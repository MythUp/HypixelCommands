package com.hypixelcommands.brigadier;

import com.mojang.brigadier.context.StringRange;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class HypixelCommandDispatcher {
    private static final CommandNode ROOT = buildRoot();

    public static void buildTree() {
        // Tree is static and ready for client-only completion.
    }

    public static Suggestions getSuggestions(String input) {
        if (input == null || input.isBlank()) {
            return null;
        }

        String raw = input.startsWith("/") ? input : "/" + input;
        String trimmed = raw.substring(1).trim();
        if (trimmed.isEmpty()) {
            return null;
        }

        String[] tokens = trimmed.split("\\s+");
        boolean endsWithSpace = raw.endsWith(" ");

        CommandNode currentNode = ROOT;
        int commandDepth = 0;
        for (String token : tokens) {
            CommandNode child = currentNode.getChild(token.toLowerCase(Locale.ROOT));
            if (child == null) {
                break;
            }
            currentNode = child;
            commandDepth++;
        }

        String currentWord = endsWithSpace ? "" : tokens[tokens.length - 1];
        int consumedArgs = endsWithSpace
                ? Math.max(0, tokens.length - commandDepth)
                : Math.max(0, tokens.length - commandDepth - 1);
        int argIndex = resolveParameterIndex(currentNode, consumedArgs, endsWithSpace);

        if (currentNode.getParameters().size() > 0 && argIndex >= 0 && argIndex < currentNode.getParameters().size()) {
            ParameterSpec parameter = currentNode.getParameters().get(argIndex);
            return buildParameterSuggestions(raw, parameter, currentWord);
        }

        if (currentNode == ROOT && !currentWord.isEmpty() && currentNode.getChildren().values().stream().noneMatch(child ->
                child.getName().toLowerCase(Locale.ROOT).startsWith(currentWord.toLowerCase(Locale.ROOT)))) {
            return null;
        }

        if (currentNode == ROOT && endsWithSpace && !tokens[tokens.length - 1].isEmpty()) {
            return null;
        }

        return buildChildSuggestions(currentNode, currentWord, raw);
    }

    private static int resolveParameterIndex(CommandNode currentNode, int consumedArgs, boolean endsWithSpace) {
        List<ParameterSpec> params = currentNode.getParameters();
        if (params.isEmpty()) {
            return -1;
        }

        if (endsWithSpace) {
            ParameterSpec lastParam = params.get(params.size() - 1);
            if (lastParam.repeatable()) {
                return Math.min(Math.max(0, consumedArgs), params.size() - 1);
            }
            return consumedArgs >= params.size() ? -1 : consumedArgs;
        }

        if (consumedArgs <= 0) {
            return 0;
        }

        int lastIndex = params.size() - 1;
        ParameterSpec lastParam = params.get(lastIndex);
        if (lastParam.repeatable()) {
            return Math.min(consumedArgs, lastIndex);
        }

        return Math.min(consumedArgs, lastIndex);
    }

    private static Suggestions buildParameterSuggestions(String raw, ParameterSpec parameter, String currentWord) {
        int start = raw.lastIndexOf(' ') + 1;
        if (start <= 0) {
            start = 1;
        }
        StringRange range = StringRange.between(start, raw.length());

        List<String> values = getParameterSuggestions(parameter, currentWord);
        List<Suggestion> suggestions = new ArrayList<>();
        for (String value : values) {
            if (value.toLowerCase(Locale.ROOT).startsWith(currentWord.toLowerCase(Locale.ROOT))) {
                suggestions.add(new Suggestion(range, value));
            }
        }
        return suggestions.isEmpty() ? null : new Suggestions(range, suggestions);
    }

    private static Suggestions buildChildSuggestions(CommandNode currentNode, String currentWord, String raw) {
        int start = raw.lastIndexOf(' ') + 1;
        if (start <= 0) {
            start = 1;
        }
        StringRange range = StringRange.between(start, raw.length());

        List<Suggestion> suggestions = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (Map.Entry<String, CommandNode> entry : currentNode.getChildren().entrySet()) {
            String childName = entry.getKey();
            if (seen.add(childName.toLowerCase(Locale.ROOT)) && childName.toLowerCase(Locale.ROOT).startsWith(currentWord.toLowerCase(Locale.ROOT))) {
                suggestions.add(new Suggestion(range, childName));
            }
        }
        return suggestions.isEmpty() ? null : new Suggestions(range, suggestions);
    }

    private static List<String> getParameterSuggestions(ParameterSpec parameter, String prefix) {
        if (parameter.type() == ParameterType.STRING && parameter.values() != null && !parameter.values().isEmpty()) {
            return parameter.values().stream()
                    .filter(value -> prefix.isEmpty() || value.toLowerCase(Locale.ROOT).startsWith(prefix.toLowerCase(Locale.ROOT)))
                    .toList();
        }

        if (parameter.type() == ParameterType.PLAYER || parameter.type() == ParameterType.PLAYER_LIST) {
            Minecraft client = Minecraft.getInstance();
            if (client == null || client.getConnection() == null) {
                return List.of();
            }

            List<String> names = new ArrayList<>();
            for (Object playerInfoObj : client.getConnection().getOnlinePlayers()) {
                if (playerInfoObj instanceof net.minecraft.client.multiplayer.PlayerInfo playerInfo) {
                    String name = getProfileName(playerInfo);
                    if (name != null && (prefix.isEmpty() || name.toLowerCase(Locale.ROOT).startsWith(prefix.toLowerCase(Locale.ROOT)))) {
                        names.add(name);
                    }
                }
            }
            names.sort(String.CASE_INSENSITIVE_ORDER);
            return names;
        }

        if (parameter.type() == ParameterType.PLAYER_OR_STRING && parameter.values() != null) {
            List<String> values = new ArrayList<>(parameter.values());
            if (parameter.type() == ParameterType.PLAYER_OR_STRING) {
                Minecraft client = Minecraft.getInstance();
                if (client != null && client.getConnection() != null) {
                    for (Object playerInfoObj : client.getConnection().getOnlinePlayers()) {
                        if (playerInfoObj instanceof net.minecraft.client.multiplayer.PlayerInfo playerInfo) {
                            String name = getProfileName(playerInfo);
                            if (name != null) {
                                values.add(name);
                            }
                        }
                    }
                }
            }
            values.sort(String.CASE_INSENSITIVE_ORDER);
            return values.stream()
                    .filter(value -> prefix.isEmpty() || value.toLowerCase(Locale.ROOT).startsWith(prefix.toLowerCase(Locale.ROOT)))
                    .distinct()
                    .toList();
        }

        return List.of();
    }

    private static String getProfileName(net.minecraft.client.multiplayer.PlayerInfo playerInfo) {
        try {
            Object profile = playerInfo.getProfile();
            if (profile == null) {
                return null;
            }
            try {
                java.lang.reflect.Method method = profile.getClass().getMethod("getName");
                Object value = method.invoke(profile);
                return value instanceof String string ? string : null;
            } catch (NoSuchMethodException ignored) {
                java.lang.reflect.Field field = profile.getClass().getDeclaredField("name");
                field.setAccessible(true);
                Object value = field.get(profile);
                return value instanceof String string ? string : null;
            }
        } catch (Exception e) {
            return null;
        }
    }

    private static CommandNode buildRoot() {
        CommandNode root = new CommandNode("root");

        CommandNode chat = new CommandNode("ch", "chat");
        chat.addChild(command("a"));
        chat.addChild(command("all"));
        chat.addChild(command("p"));
        chat.addChild(command("party"));
        chat.addChild(command("g"));
        chat.addChild(command("guild"));
        chat.addChild(command("o"));
        chat.addChild(command("officer"));
        root.addChild(chat);

        CommandNode friend = new CommandNode("friend");
        friend.addChild(command("accept", arg("player", ParameterType.PLAYER)));
        friend.addChild(command("add", arg("player", ParameterType.PLAYER)));
        friend.addChild(command("best", arg("player", ParameterType.PLAYER)));
        friend.addChild(command("deny", arg("player", ParameterType.PLAYER)));
        friend.addChild(command("help"));
        friend.addChild(command("list", arg("page", ParameterType.STRING, List.of("best"))));
        friend.addChild(command("nickname",
                arg("player", ParameterType.PLAYER),
                arg("nickname", ParameterType.STRING)));
        friend.addChild(command("notifications"));
        friend.addChild(command("remove", arg("player", ParameterType.PLAYER)));
        friend.addChild(command("removeall"));
        friend.addChild(command("requests", arg("page", ParameterType.STRING)));
        root.addChild(friend);

        CommandNode guild = new CommandNode("guild");
        guild.addChild(command("accept"));
        guild.addChild(command("chat", arg("message", ParameterType.STRING)));
        guild.addChild(command("create", arg("name", ParameterType.STRING)));
        guild.addChild(command("demote", arg("player", ParameterType.PLAYER)));
        guild.addChild(command("disband"));
        guild.addChild(command("discord"));
        guild.addChild(command("help"));
        guild.addChild(command("history"));
        guild.addChild(command("info"));
        guild.addChild(command("invite", arg("player", ParameterType.PLAYER)));
        guild.addChild(command("join", arg("guild", ParameterType.STRING)));
        guild.addChild(command("kick",
                arg("player", ParameterType.PLAYER),
                arg("reason", ParameterType.STRING)));
        guild.addChild(command("leave"));
        guild.addChild(command("log"));
        guild.addChild(command("member"));
        guild.addChild(command("members"));
        guild.addChild(command("menu"));
        guild.addChild(command("motd"));
        guild.addChild(command("mute",
                argChoice("target", ParameterType.PLAYER_OR_STRING, List.of("everyone")),
                arg("time", ParameterType.STRING)));
        guild.addChild(command("mypermissions"));
        guild.addChild(command("notifications"));
        guild.addChild(command("officerchat"));
        guild.addChild(command("online"));
        guild.addChild(command("onlinemode"));
        guild.addChild(command("party"));
        guild.addChild(command("permissions"));
        guild.addChild(command("promote", arg("player", ParameterType.PLAYER)));
        guild.addChild(command("quest"));
        guild.addChild(command("rename", arg("name", ParameterType.STRING)));
        guild.addChild(command("setrank",
                arg("player", ParameterType.PLAYER),
                arg("rank", ParameterType.STRING)));
        guild.addChild(command("settings",
                arg("setting", ParameterType.STRING),
                arg("value", ParameterType.STRING)));
        guild.addChild(command("slow"));
        guild.addChild(command("tag"));
        guild.addChild(command("tagcolor"));
        guild.addChild(command("toggle"));
        guild.addChild(command("top"));
        guild.addChild(command("transfer", arg("player", ParameterType.PLAYER)));
        guild.addChild(command("unmute", argChoice("target", ParameterType.PLAYER_OR_STRING, List.of("everyone"))));
        root.addChild(guild);

        CommandNode lang = new CommandNode("lang", "language");
        lang.addChild(command("french", "english", "german"));
        root.addChild(lang);

        CommandNode msg = new CommandNode("msg", "message", "tell", "w", "whisper");
        msg.withArgument(arg("player", ParameterType.PLAYER));
        msg.withArgument(arg("message", ParameterType.STRING));
        root.addChild(msg);

        CommandNode party = new CommandNode("party");
        party.addChild(command("accept", arg("player", ParameterType.PLAYER)));
        party.addChild(command("chat"));
        party.addChild(command("demote", arg("player", ParameterType.PLAYER)));
        party.addChild(command("disband"));
        party.addChild(command("invite", repeatableArg("player", ParameterType.PLAYER)));
        party.addChild(command("kick", arg("player", ParameterType.PLAYER)));
        party.addChild(command("kickoffline"));
        party.addChild(command("leave"));
        party.addChild(command("list"));
        party.addChild(command("mute"));
        guild.addChild(command("poll",
                arg("question", ParameterType.STRING),
                repeatableArg("answer", ParameterType.STRING)));
        party.addChild(command("private"));
        party.addChild(command("promote", arg("player", ParameterType.PLAYER)));
        party.addChild(command("settings", arg("setting", ParameterType.STRING, List.of("allinvite", "mute", "private"))));
        party.addChild(command("transfer", arg("player", ParameterType.PLAYER)));
        party.addChild(command("warp"));
        root.addChild(party);

        return root;
    }

    private static CommandNode command(String... names) {
        return new CommandNode(names);
    }

    private static CommandNode command(String name, ParameterSpec first, ParameterSpec... rest) {
        CommandNode node = new CommandNode(name);
        node.withArgument(first);
        for (ParameterSpec arg : rest) {
            node.withArgument(arg);
        }
        return node;
    }

    private static ParameterSpec arg(String name, ParameterType type) {
        return new ParameterSpec(name, type, List.of(), false);
    }

    private static ParameterSpec arg(String name, ParameterType type, List<String> values) {
        return new ParameterSpec(name, type, values, false);
    }

    private static ParameterSpec argChoice(String name, ParameterType type, List<String> values) {
        return new ParameterSpec(name, type, values, false);
    }

    private static ParameterSpec repeatableArg(String name, ParameterType type) {
        ParameterType actualType = type == ParameterType.PLAYER ? ParameterType.PLAYER_LIST : type;
        return new ParameterSpec(name, actualType, List.of(), true);
    }

    private static final class CommandNode {
        private final String name;
        private final List<String> aliases = new ArrayList<>();
        private final Map<String, CommandNode> children = new LinkedHashMap<>();
        private final List<ParameterSpec> parameters = new ArrayList<>();
        private int depth;

        private CommandNode(String... names) {
            if (names == null || names.length == 0) {
                throw new IllegalArgumentException("CommandNode requires at least one name");
            }
            this.name = names[0];
            for (String alias : names) {
                addAlias(alias);
            }
        }

        public String getName() {
            return name;
        }

        public List<String> getNames() {
            return aliases;
        }

        public Map<String, CommandNode> getChildren() {
            return children;
        }

        public List<ParameterSpec> getParameters() {
            return parameters;
        }

        public int getDepth() {
            return depth;
        }

        public CommandNode addChild(CommandNode child) {
            child.depth = this.depth + 1;
            for (String lookupName : child.getNames()) {
                children.put(lookupName.toLowerCase(Locale.ROOT), child);
            }
            return this;
        }

        public CommandNode addAlias(String alias) {
            if (alias != null && !alias.isBlank()) {
                aliases.add(alias);
            }
            return this;
        }

        public CommandNode withArgument(ParameterSpec parameter) {
            parameters.add(parameter);
            return this;
        }

        public CommandNode getChild(String key) {
            return children.get(key.toLowerCase(Locale.ROOT));
        }
    }

    private record ParameterSpec(String name, ParameterType type, List<String> values, boolean repeatable) {
    }

    private enum ParameterType {
        STRING,
        PLAYER,
        PLAYER_LIST,
        PLAYER_OR_STRING
    }
}

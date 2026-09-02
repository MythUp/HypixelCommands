package com.hypixelcommands.brigadier;

import com.mojang.brigadier.context.StringRange;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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
        int maxVisited = endsWithSpace ? tokens.length : tokens.length - 1;

        CommandNode currentNode = ROOT;
        int commandDepth = 0;
        for (int i = 0; i < maxVisited; i++) {
            CommandNode child = currentNode.getChild(tokens[i].toLowerCase(Locale.ROOT));
            if (child == null) {
                return null;
            }
            currentNode = child;
            commandDepth++;
        }

        String currentWord = endsWithSpace ? "" : tokens[tokens.length - 1];
        int argIndex = Math.max(0, maxVisited - commandDepth);

        if (currentNode.getParameters().size() > 0 && argIndex < currentNode.getParameters().size()) {
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
        for (CommandNode child : currentNode.getChildren().values()) {
            String childName = child.getName();
            if (childName.toLowerCase(Locale.ROOT).startsWith(currentWord.toLowerCase(Locale.ROOT))) {
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

        if (parameter.type() == ParameterType.PLAYER) {
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

        return root;
    }

    private static CommandNode command(String name, ParameterSpec... args) {
        CommandNode node = new CommandNode(name);
        for (ParameterSpec arg : args) {
            node.withArgument(arg);
        }
        return node;
    }

    private static ParameterSpec arg(String name, ParameterType type) {
        return new ParameterSpec(name, type, List.of());
    }

    private static ParameterSpec arg(String name, ParameterType type, List<String> values) {
        return new ParameterSpec(name, type, values);
    }

    private static final class CommandNode {
        private final String name;
        private final Map<String, CommandNode> children = new LinkedHashMap<>();
        private final List<ParameterSpec> parameters = new ArrayList<>();
        private int depth;

        private CommandNode(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
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
            children.put(child.getName().toLowerCase(Locale.ROOT), child);
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

    private record ParameterSpec(String name, ParameterType type, List<String> values) {
    }

    private enum ParameterType {
        STRING,
        PLAYER
    }
}

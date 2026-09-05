package com.hypixelcommands.brigadier;

import com.hypixelcommands.HypixelCommandsMod;
import com.hypixelcommands.commandkit.CommandArgument;
import com.hypixelcommands.commandkit.CommandCompletion;
import com.hypixelcommands.commandkit.CommandNode;
import com.mojang.brigadier.suggestion.Suggestions;

/**
 * Compatibility facade for the original dispatcher package.
 *
 * <p>The completion implementation lives in the separately publishable
 * CommandKit library; this class only declares Hypixel commands.</p>
 */
public final class HypixelCommandDispatcher {
    private HypixelCommandDispatcher() {
    }

    public static void buildTree() {
        CommandNode chat = command("ch", "chat")
                .then(command("all"))
                .then(command("party"))
                .then(command("guild"))
                .then(command("online"));
        register(chat);

        CommandNode friend = command("friend")
                .then(command("accept", player("player")))
                .then(command("add", player("player")))
                .then(command("best", player("player")))
                .then(command("deny", player("player")))
                .then(command("help"))
                .then(command("list", choices("page", "best")))
                .then(command("nickname", player("player"), string("nickname")))
                .then(command("notifications"))
                .then(command("remove", player("player")))
                .then(command("removeall"))
                .then(command("requests", string("page")));
        register(friend);

        CommandNode guild = command("g", "guild")
                .then(command("accept"))
                .then(command("chat", restMessage("message")))
                .then(command("create", string("name")))
                .then(command("demote", player("player")))
                .then(command("disband"))
                .then(command("discord"))
                .then(command("help"))
                .then(command("history"))
                .then(command("info"))
                .then(command("invite", player("player")))
                .then(command("join", string("guild")))
                .then(command("kick", player("player"), string("reason")))
                .then(command("leave"))
                .then(command("log"))
                .then(command("member"))
                .then(command("members"))
                .then(command("menu"))
                .then(command("motd"))
                .then(command("mute", playerOrLiteral("target", "everyone"), string("time")))
                .then(command("mypermissions"))
                .then(command("notifications"))
                .then(command("officerchat"))
                .then(command("online"))
                .then(command("onlinemode"))
                .then(command("party"))
                .then(command("permissions"))
                .then(command("promote", player("player")))
                .then(command("quest"))
                .then(command("rename", string("name")))
                .then(command("setrank", player("player"), string("rank")))
                .then(command("settings", string("setting"), string("value")))
                .then(command("slow"))
                .then(command("tag"))
                .then(command("tagcolor"))
                .then(command("toggle"))
                .then(command("top"))
                .then(command("transfer", player("player")))
                .then(command("unmute", playerOrLiteral("target", "everyone")));
        register(guild);

        CommandNode language = command("lang", "language")
                .then(command(
                        "chinese_simplified", "chinese_traditional", "czech", "danish",
                        "dutch", "english", "finnish", "french", "german", "hungarian",
                        "italian", "japanese", "korean", "norwegian", "pirate", "polish",
                        "portuguese_br", "portuguese_pt", "romanian", "russian", "spanish",
                        "swedish", "turkish", "ukrainian"));
        register(language);

        register(command("msg", "message", "tell", "w", "whisper")
                .argument(player("player"))
                .argument(restMessage("message")));

        register(command("party")
                .then(command("accept", player("player")))
                .then(command("chat"))
                .then(command("demote", player("player")))
                .then(command("disband"))
                .then(command("invite", repeatablePlayer("player")))
                .then(command("kick", player("player")))
                .then(command("kickoffline"))
                .then(command("leave"))
                .then(command("list"))
                .then(command("mute"))
                .then(command("poll", string("question"), repeatable(string("answer"))))
                .then(command("private"))
                .then(command("promote", player("player")))
                .then(command("settings", choices("setting", "allinvite", "mute", "private")))
                .then(command("transfer", player("player")))
                .then(command("warp")));

        register(command("particlequality", "quality", "pc")
                .then(command("off", "low", "medium", "high", "extreme")));

        register(command("tpa")
                .argument(player("player"))
                .then(command("accept", player("player"))));

        register(command("status")
                .then(command("online", "away", "busy", "offline")));

    }

    /**
     * Retains the old lookup entry point for source and binary consumers.
     */
    public static Suggestions getSuggestions(String input) {
        buildTree();
        return CommandCompletion.getSuggestions(input);
    }

    private static void register(CommandNode command) {
        command.when(context -> HypixelCommandsMod.isHypixel());
        CommandCompletion.register(command);
    }

    private static CommandNode command(String name, CommandArgument first, CommandArgument... rest) {
        CommandNode command = CommandNode.literal(name).argument(first);
        for (CommandArgument argument : rest) {
            command.argument(argument);
        }
        return command;
    }

    private static CommandNode command(String... names) {
        if (names.length == 0) {
            throw new IllegalArgumentException("A command needs a name");
        }
        return CommandNode.literal(names[0], java.util.Arrays.copyOfRange(names, 1, names.length));
    }

    private static CommandArgument player(String name) {
        return CommandArgument.player(name);
    }

    private static CommandArgument repeatablePlayer(String name) {
        return CommandArgument.repeatablePlayer(name);
    }

    private static CommandArgument repeatable(CommandArgument argument) {
        return new CommandArgument(argument.name(), argument.type(), argument.literals(), true);
    }

    private static CommandArgument playerOrLiteral(String name, String... values) {
        return CommandArgument.playerOrLiteral(name, values);
    }

    private static CommandArgument string(String name) {
        return CommandArgument.string(name);
    }

    private static CommandArgument choices(String name, String... values) {
        return CommandArgument.choices(name, values);
    }

    private static CommandArgument restMessage(String name) {
        return CommandArgument.restMessage(name);
    }
}

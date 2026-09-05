# Hypixel Commands

[![CurseForge](https://img.shields.io/curseforge/dt/1679294?logo=curseforge&label=CurseForge)](https://www.curseforge.com/minecraft/mc-mods/hypixel-commands)

Hypixel Commands is a Fabric client-only mod for Minecraft 26.2 that improves in-chat command completion on Hypixel by adding local client-side suggestions for Hypixel-specific commands and subcommands. The completion engine is published separately as the `CommandKit` Fabric library mod, so other client mods can register their own definitions.

The mod does not modify the server, does not require any server-side plugin or custom protocol, and does not execute commands automatically. It only enhances the client-side command suggestion pipeline so that the player gets better tab-completion while still sending normal Minecraft commands to Hypixel.

## Features

- Client-only implementation for Fabric 26.2
- Hypixel-only suggestion layer
- Supports command families such as `/friend`, `/guild`, `/party`, `/msg`, `/lang`, `/tpa`, and other command trees defined locally
- Supports nested subcommands and argument-aware completion
- Supports dynamic player suggestions for arguments like `<player>`
- Supports combined literal-or-player values such as `everyone` or other command-specific special values
- Supports repeatable player arguments such as `/party invite <player> <player> ...`
- Keeps native Minecraft and vanilla command completion working when no Hypixel command matches
- Avoids any custom protocol or command execution logic

## How it works

The mod hooks into Minecraft's client-side command suggestion flow through a Fabric mixin on `CommandSuggestions`.

When the player types a command in the chat box, the client updates its suggestion state. The mod intercepts that lifecycle and checks whether:

- the player is on Hypixel,
- the typed command matches a known local Hypixel command tree,
- the current word is part of a valid command path or argument slot.

If the input matches a known Hypixel command or parameter pattern, the mod injects additional `Suggestions` objects into the same Brigadier-based completion pipeline used by Minecraft itself.

The final command is still sent exactly as a normal Minecraft command by the game. The mod never bypasses the normal chat/command flow.

## Client-only architecture

The mod intentionally stays entirely on the client side:

- no Hypixel server patching,
- no plugin installation on the server,
- no custom network protocol,
- no manual command execution from the client.

This means the mod is safe to use on vanilla Hypixel as a local quality-of-life enhancement without any server-side cooperation.

## Command model

The command system is defined in code as a lightweight local command tree that mirrors common Hypixel command shapes. Each node can declare:

- command names and aliases,
- child subcommands,
- argument definitions,
- repeatable players,
- player-or-literal alternatives,
- literal values such as `best`, `everyone`, or language names.

This model is intentionally simpler than a full Brigadier command tree, because the goal is not to replace the server command system but to improve the client-side completion experience.

## Examples

The project includes support for patterns like:

- `/friend add <player>`
- `/friend list best`
- `/guild unmute <player|everyone>`
- `/party invite <player> <player> ...`
- `/msg <player> <message>`
- `/tpa <player>`
- `/tpa accept <player>`
- `/lang` and `/language`

## Dynamic player suggestions

For player arguments like `<player>`, the mod reads the current online-player list from the client connection and filters it based on the current partial text.

This is a practical client-side source because it does not require any external API and works naturally with vanilla Minecraft session data.

## Unknown commands and compatibility

The mod is deliberately careful not to override normal command completion for commands that are not part of the Hypixel command list.

If a typed command does not match the local Hypixel command tree, the mod does not inject its own suggestions, and the game continues with normal behavior. This prevents broken completions such as stale `friend` suggestions appearing after unrelated commands like `/bedwars`.

## Library API

The `CommandKit` library exposes `CommandCompletion.register(CommandNode)`
and a fluent `CommandNode`/`CommandArgument` API. Definitions can use aliases,
nested literals, player and repeatable-player arguments, player-or-literal choices,
rest-of-message arguments, and `when(Predicate<CompletionContext>)` context
predicates. The library owns the `CommandSuggestions` mixin and only supplies
suggestions for active registered definitions.

### Using the library from another Fabric mod

The library is published with the Maven coordinates:

```text
com.mythup:commandkit:1.0.0+26.2
```

Declare the dependency in the consumer mod's Gradle build:

```groovy
repositories {
    mavenLocal()
    maven {
        url = uri("https://maven.pkg.github.com/MythUp/CommandKit")
    }
}

dependencies {
    implementation "com.mythup:commandkit:1.0.0+26.2"
}
```

The library must also be installed as a separate Fabric mod jar at runtime. A
consumer registers only its own command definitions:

```java
CommandNode language = new CommandNode("lang", "language")
        .then(new CommandNode("english", "french", "german"));

language.when(context -> {
    String address = context.serverAddress();
    return address != null && address.endsWith("example.net");
});

CommandCompletion.register(language);
```

The consumer does not need to add another `CommandSuggestions` mixin. The
library owns that integration and keeps native completion intact whenever no
active local definition matches.

## Project layout

Key pieces of the implementation include:

- `CommandKit/` — separately publishable generic completion library and mixin
- `HypixelCommandsMod` — Fabric client entry point and Hypixel detection
- `HypixelCommandDispatcher` — Hypixel-only command definitions (compatibility facade)

## Development status

This project is focused on improving client-side command completion for Hypixel commands, with emphasis on correctness, compatibility, and preserving the normal Minecraft chat/Tab flow.

It is intentionally limited to client-only behavior and does not attempt to bypass Hypixel restrictions or permissions.

## License

See the [LICENSE](./LICENSE) file for full license details. The project is licensed under a custom non-commercial license that allows free distribution and modification, but prohibits commercial use or selling of the software.

## Author

[MythUp_](https://github.com/MythUp_)

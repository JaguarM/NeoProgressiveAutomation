Neo Progressive Automation
==========================

Low tech automation tools for common tasks — a ground-up reimplementation of
[Progressive Automation](https://github.com/Vanhal/ProgressiveAutomation) for modern
Minecraft and NeoForge.

| | |
|---|---|
| Minecraft | 26.1.2 |
| NeoForge | 26.1.2.95 |
| Java | 25 (Temurin recommended) |
| Mod id | `neoprogressiveautomation` |

Building
--------

```
./gradlew build
```

The finished jar lands in `build/libs/`.

Running in development
----------------------

```
./gradlew runClient   # dev client, no Minecraft account needed
./gradlew runServer   # dedicated server
./gradlew runData     # regenerate datagen output into src/generated/resources
```

If your IDE is missing libraries or something looks stale, `./gradlew --refresh-dependencies`
refreshes the local cache, and `./gradlew clean` resets build output without touching your code.

Layout
------

```
src/main/java/com/jaguarm/neoprogressiveautomation/   mod sources
src/main/resources/assets/neoprogressiveautomation/   textures, models, lang
src/main/templates/META-INF/neoforge.mods.toml        mod metadata (Gradle expands ${...} from gradle.properties)
src/generated/resources/                              datagen output — do not hand-edit
_OLD/                                                 the original 1.12.2 mod, kept as a design reference only
```

Note that `_OLD/` is **not** part of the build. It targets Minecraft 1.12.2 / Forge 14.23
and none of its code compiles against modern NeoForge; it is retained for its game design,
recipe balance (`_OLD/notes-on-recipes.txt`), and feature behaviour.

Mapping names
-------------

This project uses the official Mojang mapping names for Minecraft methods and fields. These
names are covered by a specific license that all modders should be aware of — see
https://github.com/NeoForged/NeoForm/blob/main/Mojang.md

Additional resources
--------------------

- Community documentation: https://docs.neoforged.net/
- NeoForged Discord: https://discord.neoforged.net/

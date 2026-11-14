# Netherite Anvil Upgrade (Fabric, server-side, Minecraft 26.2)

In an anvil, put a Netherite tool/armor piece in the **left** slot and the matching
Diamond piece in the **right** slot. The output is the Netherite item with merged
enchantments (vanilla merge rules), repaired durability and a vanilla-style XP cost.

- Server-side only: vanilla clients can join. No Fabric API needed.
- Requires Java 25, Fabric Loader 0.19.3+.

## Build locally
```
gradle wrapper --gradle-version 9.5.1   # once, to generate gradlew
./gradlew build
```
Jar: `build/libs/netherite-anvil-upgrade-1.0.0.jar`

Or let the included GitHub Action build it (see Actions -> artifacts).

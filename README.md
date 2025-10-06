# Better Happy Ghast

Adds a **parked** state to the vanilla **Happy Ghast** (1.21.x):
- Toggle **parked** with a **Blaze Rod** (only if the ghast has a **harness**).
- While parked and **unmounted**, it stays perfectly still.
- While parked and **mounted**, it flies **faster** (configurable multiplier).
- Optional visual feedback (glow + particles when toggling).
- Includes simple JSON config.

> Fabric mod for Minecraft **1.21.x**.

## Requirements
- **Fabric Loader** ≥ 0.15.0  
- **Fabric API** for your Minecraft 1.21.x version

## Installation
1. Install Fabric Loader and Fabric API.
2. Download the mod JAR from the [Releases](https://github.com/ivaanortega/better-happy-ghast/releases).
3. Drop the JAR into your `mods/` folder:
   - Windows: `%AppData%/.minecraft/mods`
   - Linux: `~/.minecraft/mods`
   - macOS: `~/Library/Application Support/minecraft/mods`
4. Launch the game.

## How to use
- Put a **Harness** on a Happy Ghast (vanilla behavior).
- **Right-click with a Blaze Rod** to toggle **parked** on/off.
  - Parked (unmounted): stays in place.
  - Parked (mounted): flies faster than normal.
- Right-click with empty hand to mount as usual (vanilla).

## Configuration
A JSON file is created on first run:

```
.minecraft/config/better-happy-ghast.json
```

Default fields:
```json
{
  "parkedSpeedMultiplier": 2.0,
  "sprintMultiplier": 1.25,
  "maxSpeed": 1.6,
  "debugLogs": true,
  "debugEveryTicks": 20
}
```

- `parkedSpeedMultiplier` – base speed multiplier when **parked + mounted**.  
- `sprintMultiplier` – extra multiplier if sprint/jump signal is detected (kept for future; not required if you disabled sprint logic).  
- `maxSpeed` – hard cap on velocity length to avoid runaway speeds.  
- `debugLogs` – prints diagnostic info to `latest.log`.  
- `debugEveryTicks` – how often to print periodic speed logs (20 = ~1 sec).

> After editing the JSON, **restart the game**.

## Compatibility
- Minecraft: **>= 1.21, < 1.22**
- Loader: **Fabric**
- Not tested with Forge/NeoForge.
- Should be compatible with most content mods; anything replacing Happy Ghast internals might conflict.

## Building from source
```bash
# Windows PowerShell
.\\gradlew.bat clean build
# macOS/Linux
./gradlew clean build
```
Artifacts will be in `build/libs/`.  
Publish the **remapped** jar (no `-dev` / `-sources` suffix).

## Troubleshooting
- “Incompatible mods / requires ${minecraft_version}”: make sure your `fabric.mod.json` expands placeholders (or use a fixed range like `>=1.21 <1.22`).
- If the Blaze Rod mounts you instead of toggling parked: ensure the ghast **has a harness**; the Blaze Rod action is intercepted only in that case.
- Speed feels off: adjust `parkedSpeedMultiplier` and `maxSpeed`. Enable `debugLogs` to see live speed in `latest.log`.

## License
[MIT](./LICENSE)

## Credits
- Code & idea: **Ivaanortega**

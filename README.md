# Easy Gamemode

A **client-side only** Fabric mod for **Minecraft Java 1.21.11**.

Press **G** to open a small GUI that lists every online player. Each player has four one-click gamemode buttons (Survival, Creative, Adventure, Spectator) that look like the vanilla `F3 + F4` selector. The **EVERYONE** row at the top changes the gamemode of all online players at once.

**ONE PLAYER. ONE CLICK. GAMEMODE CHANGED.**

## Features

- Press `G` to open, press `G` again to close (rebind it in Options > Controls)
- Player heads, names and a search bar
- Four icon buttons per player (sword, grass block, map, eye of ender) with hover effects and tooltips
- The player's current gamemode is highlighted, read live from the server's player list
- EVERYONE row at the top (runs `/gamemode <mode> @a`)
- List updates automatically when players join or leave
- Clear error message if the server rejects a command

## Client-side only

- Only **you** install the mod. No server mod, no server files, no server restart.
- It just sends the normal `/gamemode <mode> <player>` command for you.
- You need permission to use `/gamemode` on the server (OP). Server permission checks are never bypassed.
- Highlights are never guessed: the active button only changes after the server confirms the change.

## Build

GitHub Actions builds the jar on every push: open the **Actions** tab, open the latest run, and download the **EasyGamemode** artifact.

Local build: `gradle build` (Java 21). The jar is in `build/libs/`.

## Install

1. Install Fabric Loader for 1.21.11 and the Fabric API.
2. Drop `EasyGamemode-1.0.0.jar` into your `mods` folder.
3. Join a server, press `G`.

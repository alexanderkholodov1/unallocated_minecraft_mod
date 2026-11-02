# UNALLOCATED

{{DESIGN:DESCRIPTION}}

## Content warnings

**Flashing lights and brightness changes, loud sounds, jump scares and psychological horror.** The warnings appear before the game menu and you can read them before accepting.

{{DESIGN:WARNINGS}}

## Requirements

| Component | Requirement |
| --- | --- |
| Game | **Minecraft Java Edition {{MINECRAFT}}**, **exactly {{MINECRAFT}}**. 26.3.x hotfixes are not supported yet. You need an account with access to the game. |
| Loader | **Fabric Loader {{LOADER}} or newer** for Minecraft {{MINECRAFT}}. |
| Java | **Java 25**. The official Minecraft Launcher downloads the right runtime when it starts {{MINECRAFT}}; leave the profile's Java executable at its default. |
| Fabric API | **Included.** {{FABRIC_API}} ships inside our JAR. **Do not download or install Fabric API separately.** |
| Other mods | **None.** Use a clean profile without other mods. |

## Files

| File | What it is |
| --- | --- |
| `{{JAR}}` | The mod. SHA-256: `{{SHA256}}` |
| `{{WORLD_ZIP}}` | Optional backup map. You only need it if the title button does not work. |

You can verify the download with the SHA-256 fingerprint: on Windows use `certutil -hashfile {{JAR}} SHA256`; on macOS, `shasum -a 256 {{JAR}}`; on Linux, `sha256sum {{JAR}}`.

## Step-by-step installation

1. Open **Minecraft Launcher**, select **Java Edition** and create an installation of version **{{MINECRAFT}}**. Start it once to the main menu. Then close Minecraft and the launcher.
2. Download the [official Fabric installer](https://fabricmc.net/use/installer/) **1.1.2** and open it according to your system:

   | System | How to open the installer |
   | --- | --- |
   | Windows | Download **Download for Windows** (`.exe`) and run it. It uses the launcher's Java. |
   | macOS | Download **Universal/.JAR** and open it with Java. If you have no Java installed to open files, install [Temurin 25](https://adoptium.net/temurin/releases/?version=25) for your Mac (aarch64 for Apple Silicon; x64 for Intel). You can also run `java -jar fabric-installer-1.1.2.jar` from the download folder in Terminal. |
   | Linux | Download **Universal/.JAR**. If system Java is missing, install [Temurin 25](https://adoptium.net/temurin/releases/?version=25) for your distribution and architecture. From the download folder run `java -jar fabric-installer-1.1.2.jar`. |

3. In the installer, open the **Client** tab. Choose **Minecraft Version: {{MINECRAFT}}** and **Loader Version: {{LOADER}} or newer**. Check that the location matches your Minecraft Launcher, leave **Create Profile** ticked and press **Install**.
4. Download `{{JAR}}` from this page. Do not extract it.
5. Copy the JAR into the profile's **`mods`** folder. Create the folder if it does not exist:

   | System | Default location | How to get there |
   | --- | --- | --- |
   | Windows | `%APPDATA%\.minecraft\mods` | Press Win+R, paste the path and press Enter. |
   | macOS | `~/Library/Application Support/minecraft/mods` | Finder → Go → Go to Folder; paste the path. |
   | Linux | `~/.minecraft/mods` | Paste the path into your file manager; Ctrl+H shows hidden folders. |

   If you set a different **Game Directory** in the launcher, use the `mods` folder inside that directory. Copy only our JAR: Fabric API is already inside it, and the Fabric installer is not copied to `mods`.

6. Open the launcher and select the profile the installer created, called **`fabric-loader-{{MINECRAFT}}`** (version `fabric-loader-{{LOADER}}-{{MINECRAFT}}` or newer). If it does not appear, go to **Installations** and enable the modded-installations filter (**Modded**). Press **Play**.

**TLauncher or offline accounts.** The mod needs no online authentication. In TLauncher, choose or install a **Fabric {{MINECRAFT}}** version, launch it once, then use TLauncher's "open game folder" option and put only `{{JAR}}` in its `mods` folder. For TLauncher custom or modpack versions, the folder is `.minecraft/versions/<version name>/mods`.

## How to start

1. On the first launch, read the contract's warnings and press **I agree** (**De acuerdo** in Spanish), or type it, to enter the menu. **Read data uses** is optional. The acceptance stays only on your computer, in `<game directory>/unallocated/contract.properties`, and is not sent anywhere.
2. In the main menu press the **UNALLOCATED** button. The mod copies the included map, shows a connection screen and opens the world. You do not have to create or download anything else.
3. Play in **first person**. A **10 to 15 minute** session is recommended, with headphones, alone, and with little light.
4. If an active session gets stuck, type **`/unallocated next`** in the chat (Spanish alias **`/unallocated siguiente`**). It rescues the session and moves on to the next act; it needs no cheats and does not start another session.

The mod is fully available in English and Spanish, chosen from the game language.

## Map and configuration

The map and its configuration travel inside the JAR. The **UNALLOCATED** button creates a fresh copy in `saves/` with Adventure mode and cheats disabled; there is nothing to configure by hand. To start over after an ending, press the button again.

**Backup with the ZIP.** If the button does not work on your computer:

1. Download `{{WORLD_ZIP}}`.
2. Extract it so that the `UNALLOCATED/` folder ends up inside the `saves` folder:

   | System | `saves` folder |
   | --- | --- |
   | Windows | `%APPDATA%\.minecraft\saves` |
   | macOS | `~/Library/Application Support/minecraft/saves` |
   | Linux | `~/.minecraft/saves` |

   If you use another **Game Directory**, use the `saves` folder inside that directory.
3. Open Minecraft and go to **Singleplayer**. Choose the **UNALLOCATED** world.

## Known issues

{{KNOWN_ISSUES}}

If the mod does not load, check that the profile uses **exactly {{MINECRAFT}} and Loader {{LOADER}} or newer**, that there are no old JARs in `mods` and that Fabric API is not duplicated separately. Then check `logs/latest.log` inside the game directory and look for the lines `UNALLOCATED initialized (common).` and `UNALLOCATED initialized (client).`.

## Team

{{DESIGN:TEAM}}

## Credits

Includes seven modules of **Fabric API {{FABRIC_API}}**, by **FabricMC and its contributors**, under **Apache-2.0**: base API, lifecycle events, commands, networking, registry sync, screens and resource loading. The JAR keeps each module's licenses and includes `META-INF/NOTICE-Fabric-API.txt` and `META-INF/licenses/Fabric-API-Apache-2.0.txt`. The build structure is based on the [official Fabric template for {{MINECRAFT}}](https://github.com/FabricMC/fabric-example-mod/tree/{{MINECRAFT}}), available under CC0-1.0.

{{DESIGN:CREDITS}}

Minecraft belongs to Mojang/Microsoft. This mod is not an official Minecraft product and is not approved by Mojang or Microsoft.

## Screenshots and video

<!-- NOTE FOR THE OWNER (not visible to players): choose 3 to 5 screenshots from dist/screenshots/ and a 630×500 pixel cover. Upload dist/gameplay.mp4 or, better, a real-time recording with sound made on a real PC. See docs/release/CHECKLIST.md. -->

<!-- Page generated for version {{VERSION}} from commit {{COMMIT}}. -->

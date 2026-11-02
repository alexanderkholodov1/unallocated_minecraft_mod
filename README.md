# UNALLOCATED

A first-person horror mod for Minecraft Java 26.3 (Fabric) by NULL_Threshold Interactive, made for the USFQ jam "Terror en Primera Persona". It is a story of about 10 to 15 minutes, best played alone, in the dark, with headphones.

**Current version: 0.4.0.** The mod works completely in **English and Spanish**. Before the world, an intro tells you who you are and why you are joining (one of three arrivals, drawn per copy) and shows the server in a multiplayer list. Then you play an abandoned public server, ValleCraft Network, on real terrain: a network hub, a survival world with claimed player builds, the predecessor's base and the mine; the Heap, the Puppet state with its 3D threads, and the Kernel with three endings. The Reclaimer, the threads, every texture and every sound are original. Every dependency you need is bundled inside the JAR. The version is derived automatically from the release branch; it is never edited by hand.

## Requirements

| Component | Requirement for this version |
| --- | --- |
| Game | **Minecraft Java Edition 26.3**, **exactly 26.3** (26.3.x hotfixes are not supported yet). Verified with 26.3. |
| Loader | **Fabric Loader 0.20.1 or newer** for Minecraft 26.3. Verified with 0.20.1. |
| Fabric API | **Bundled:** the seven required modules of **0.165.0+26.3** ship inside our JAR. **You do not need to download or install Fabric API separately.** |
| Java | **Java 25**. The official Minecraft Launcher downloads the right runtime when it starts 26.3; leave the profile's Java executable at its default. |
| Mod file | **`unallocated-0.4.0.jar`** from the author's release. |
| Account | The **official Minecraft Launcher** works, and so do **TLauncher** and other launchers with **offline accounts**: the mod needs no online authentication. |
| Memory | Give Minecraft **at least 2 GB of RAM** (the official launcher's default; in TLauncher: Settings → Memory). |

Minecraft is pinned to 26.3 because the bundled modules apply mixins to its internal classes; a hotfix could change those targets. The supported version will only be widened after testing.

You do not need Simple Voice Chat, performance mods, a microphone or any artificial-intelligence service. Versions and differences between the official sources are documented, dated **2026-10-26**, in [TOOLCHAIN.md](docs/plan/TOOLCHAIN.md).

## Installation with the official launcher

1. Open **Minecraft Launcher**, select **Java Edition** and create an installation of version **26.3**. Start it once to the main menu. Then close Minecraft and the launcher.
2. Download the [official Fabric installer](https://fabricmc.net/use/installer/) **1.1.2** and open it as your system requires:

   | System | How to open the installer |
   | --- | --- |
   | Windows | Download **Download for Windows** (`.exe`) and run it. It uses the launcher's Java. |
   | macOS | Download **Universal/.JAR** and open it with Java. If you have no Java installed to open files, install [Temurin 25](https://adoptium.net/temurin/releases/?version=25) for your Mac (aarch64 for Apple Silicon; x64 for Intel). You can also run `java -jar fabric-installer-1.1.2.jar` from the download folder in Terminal. |
   | Linux | Download **Universal/.JAR**. If system Java is missing, install [Temurin 25](https://adoptium.net/temurin/releases/?version=25) for your distribution/architecture. From the download folder run `java -jar fabric-installer-1.1.2.jar`. |

   On macOS, if the system blocks the official file, see [Fabric's macOS instructions](https://docs.fabricmc.net/players/installing-fabric/macos). The Java bundled with the launcher is enough to play; it is not always available to open the universal installer from the desktop or the terminal.

3. In the installer, open the **Client** tab. Choose **Minecraft Version: 26.3** and **Loader Version: 0.20.1 or newer**; you can leave the newest Loader the installer selected. Check that the location matches your Minecraft Launcher, leave **Create Profile** ticked and press **Install**.
4. Get **`unallocated-0.4.0.jar`** from the author's release. If you received a ZIP from GitHub Actions, unzip that ZIP first: inside are the JAR and its `.sha256` check file. Keep the JAR unextracted.
5. Copy the JAR into the profile's **`mods`** folder. Create the folder if it does not exist:

   | System | Default location | How to get there |
   | --- | --- | --- |
   | Windows | `%APPDATA%\.minecraft\mods` | Press Win+R, paste the path and press Enter. |
   | macOS | `~/Library/Application Support/minecraft/mods` | Finder → Go → Go to Folder; paste the path. |
   | Linux | `~/.minecraft/mods` | Paste the path into your file manager; Ctrl+H shows hidden folders. |

   If you set a different **Game Directory** in the launcher, use the `mods` folder inside that directory. To check this build, use a clean profile without other mods. Only our JAR is copied: Fabric API is already inside it, and the Fabric installer is not copied to `mods` either. If you already have Fabric API in `mods`, it must be 0.161.0+26.3 or newer; otherwise remove it.
6. Open the launcher and select the profile the installer created, called **`fabric-loader-26.3`** (it uses the `fabric-loader-0.20.1-26.3` version, or that of the Loader you installed). If it does not appear, go to **Installations** and enable the modded-installations filter (**Modded**). Press **Play**.

Official guides consulted on 2026-10-26: [Windows](https://docs.fabricmc.net/players/installing-fabric/windows), [macOS](https://docs.fabricmc.net/players/installing-fabric/macos), [Linux](https://docs.fabricmc.net/players/installing-fabric/linux) and [installing mods](https://docs.fabricmc.net/players/installing-mods). The [switch to Java 25 and its bundled runtime](https://www.minecraft.net/en-us/article/minecraft-java-edition-26-1) is kept in the [official template for 26.3](https://github.com/FabricMC/fabric-example-mod/tree/26.3).

## Installation with TLauncher (or offline accounts)

1. In TLauncher, choose or install a **Fabric 26.3** version (a Fabric version for Minecraft 26.3 with Loader 0.20.1 or newer) and launch it once to the main menu. Then close the game.
2. Use TLauncher's **open game folder** option to open the game directory. Create a `mods` folder there if it does not exist.
3. Copy **`unallocated-0.4.0.jar`** into `mods`. Put only our JAR there: Fabric API is already inside it.
4. If you use a TLauncher custom version or modpack version, the mods folder belongs to that version: `.minecraft/versions/<version name>/mods`.
5. Select that Fabric 26.3 version and press play. No online account is needed.

## How to start

On the first launch, read the contract's warnings and either type **I agree** (**De acuerdo** in Spanish) or press the accept button. **Read data uses** is optional. The acceptance is stored only in `<game directory>/unallocated/contract.properties`: UTC date and time, method, the field's text and the contract version. It is not sent off your computer. The screen is skipped on later launches of that directory. To see it again, with Minecraft closed, delete only that file or add `-Dunallocated.contract.replay=true` to the JVM arguments; replay keeps the original agreement. If it cannot be saved you can continue, and it reappears on restart.

In the main menu, press **UNALLOCATED**, above **Singleplayer**. You will see **Connecting to the server…** while the prepared world is copied into `saves/`; it then opens automatically in Adventure mode, with no cheats. You do not need to build the map or run commands. Use the first-person view (F5 changes perspective; some keyboards need Fn+F5).

If you have an unfinished session, the button offers **Continue** or **New session**. Continue keeps the chapter and the remaining time; a new session uses another folder (`UNALLOCATED`, `UNALLOCATED (2)`, …). A finished session is kept and the next start creates another copy.

**If you get stuck:** open the pause menu and press **I'm stuck**, or type **`/unallocated next`** in the chat (Spanish alias **`/unallocated siguiente`**). It works without cheats and is available 5 seconds into each chapter. Every chapter also nudges a lost player and moves on by itself.

**When you die** inside the story, the death screen offers **I agree** (consent to another trial, back to life) or **Not yet** (stay dead as a Puppet and see the Reclaimer's threads). As a Puppet, press **R** to agree and come back whenever you want.

**Languages:** the mod is fully available in English and Spanish and picks the language from the game language (any Spanish locale shows Spanish; everything else shows English). Change it in Options → Language.

**Fallback:** download **`UNALLOCATED-world.zip`** from the same release. With Minecraft closed, extract it inside `saves/` of the game directory so that `saves/UNALLOCATED/level.dat` exists. Keep any earlier world with that name in another folder before extracting. Start Minecraft and open **UNALLOCATED** from **Singleplayer**. Keep the same JAR installed; the ZIP holds the map and the prepared session.

To confirm the mod loaded, open `logs/latest.log` inside the game directory and look for these two lines (they may carry a date and other prefixes):

```text
UNALLOCATED initialized (common).
UNALLOCATED initialized (client).
```

If a version or dependency error appears, check that the profile uses **exactly 26.3 and Loader 0.20.1 or newer** and that there are no old JARs in `mods`. If you already have Fabric API in `mods`, it must be 0.161.0+26.3 or newer; otherwise remove it. If it continues, send the author the launcher message and `logs/latest.log`.

## Content warnings

**Warnings:** brightness flicker and flashes, loud sounds, jump scares and psychological horror. They appear before the menu. The spyglass bait includes a brief black cut and a scare; the tools allow triggering visual anomalies and audio hits.

The editable text and the proposed lore winks are in [Appendix C — The contract](docs/plan/CONTRACT-TEXT.md). The list distinguishes the acceptance record from other planned uses. This integration keeps the effect and audio tools; it adds no microphone, voice cloning or inference service.

Perception changes to sensitivity/FOV/darkness live only in memory and end when you quit or restart; they never write `options.txt`. Sounds respect the volume and mute chosen by the player. Human testing at home and the complete 10-to-15-minute narrative experience are still pending. The owner's answers about real options and audio resources are kept unchanged in [DECISIONS.md](docs/plan/DECISIONS.md).

## Original content and credits

NULL_Threshold Interactive. All the art and audio in this mod are original: a **custom 3D creature model and textures**, **custom thread blocks**, and **all sounds synthesized from scratch** with [`tools/sounds/synth.py`](tools/sounds/synth.py). There is **no third-party art or audio**.

The JAR bundles seven modules of [Fabric API 0.165.0+26.3](https://github.com/FabricMC/fabric/tree/26.3), by **FabricMC and its contributors**, under **Apache-2.0**: base API, lifecycle events, commands, networking, registry sync, screens and resource loading. The JAR keeps each module's licenses and includes `META-INF/NOTICE-Fabric-API.txt` and `META-INF/licenses/Fabric-API-Apache-2.0.txt`. The build structure is based on the [official Fabric example mod for 26.3](https://github.com/FabricMC/fabric-example-mod/tree/26.3), available under CC0-1.0.

Minecraft belongs to Mojang/Microsoft. This mod is not an official Minecraft product and is not approved by or affiliated with Mojang or Microsoft.

## Reversible interface tools

The integrated `v0.1.4` package adds F3 anomalies, death messages in the entity's voice, native title/loading texts, notices for unearned advancements, language changes on a sign, and sensitivity/FOV/darkness perception changes held only in memory, without modifying saved options. None of them fires automatically. See [the API, test/video commands, restoration and CI tests](docs/plan/INTERFACE.md).

## Developers

Use **JDK 25**, **Gradle 9.8.2** (no Gradle wrapper is checked in) and pinned **Loom 1.19.3**.

```text
gradle build        # output: build/libs/unallocated-0.4.0.jar
gradle runClient    # a development client
```

On a development machine with enough RAM, `gradle --no-daemon --console=plain build` works too. CI installs Gradle using the pinned `setup-gradle` action. **Do not run Java/Gradle builds on the constrained scaffold machine.**

Every push and pull request runs [one build workflow](.github/workflows/build.yml). Its four Fabric API profiles are clean Loader + player JAR, external **0.161.0+26.3**, external **0.165.0+26.3**, and external **0.153.1+26.3**. The first three must build and pass the complete server/client checks; the oldest must be rejected by Loader before game initialization. `fail-fast: false` preserves evidence from every profile. Gradle dependency/assets caches, cached server downloads, image-keyed headless graphics packages and daemon reuse retain the faster CI path. Incomplete graphics caches fall back to official Ubuntu dependency resolution.

The dedicated-server smoke installs the exact player JAR in a flat-air custom-biome world, builds/verifies all five map sections, damages and regenerates them, verifies the bait entity, checks core commands, and stops cleanly. The clean profile starts a second process on the same save and verifies the persisted map, world settings and session. The disposable test server alone accepts the EULA. Normal worlds remain dormant until explicitly armed; a successful `build all` arms an authored world.

The production client runs under Xvfb/Mesa with null OpenAL output. One isolated helper entrypoint first checks the player JAR byte for byte and the contract before any title initialization, reads the optional data list, tests keyboard/click acceptance and saves the original typed receipt. It verifies selected API module versions/origins, then runs the real-player core persistence/rescue checks, rendered interface effects, three spyglass looks and the screamer, nine map checkpoint captures, the real ending/disconnect/terminal reload, and every audio cue through server commands, packets, registered resources and the sound engine. Audio checks include resolved gain/pitch limits, mute, spatialization, attenuation, expiry, replacement, stop and disconnect cleanup. Interface/audio/ending checks preserve saved options and the agreement. A second client process must skip the contract without rewriting its receipt. Every required marker, screenshot, clean exit and deadline is enforced. CI does not measure acoustic quality or human fear.

The helper stays outside the player's JAR and nests only `fabric-client-gametest-api-v1`; player modules must come from the player JAR on a clean install. JUnit covers the core, contract, perception primitives, bait, map layout, ending text and receipt bridge during `build`. All Python checks run without Java, including watchdog failures, audio evidence, archive tampering, JSON uniqueness, mixin registration and Appendix C synchronization (`python scripts/contract_text.py --check`).

Successful clean jobs upload `unallocated-<commit SHA>` containing the JAR, its SHA-256 and the independent include-input hash report. Every profile retains `smoke-diagnostics-<commit SHA>-<API profile>` with logs, screenshots, crash reports and test reports, including on failure. Download from [GitHub Actions](https://github.com/alexanderkholodov1/unallocated_minecraft_mod/actions/workflows/build.yml). All four jobs must pass for milestone acceptance. The private repository requires a separate direct download for judges in the eventual delivery; no publishing is performed by CI.

Common sources live in `src/main/java/org/unallocated`; client sources in `src/client/java/org/unallocated/client`. Language files are in `src/main/resources/assets/unallocated/lang/` (`en_us.json` plus the same Spanish text in every `es_*.json`). Non-remapping Loom uses ordinary `implementation`, without a mappings dependency or `remapJar`. The reviewed module lock in `fabric-api-modules.json` drives inclusion and generated dependencies. `scripts/verify_jar.py` checks exact nested files, resolved-input hashes, module identities, dependency ranges/conflicts, licenses, Java 25 entrypoints, all mixin configurations, absence of helper code and a 6 MiB budget. The final artifact receives its own SHA-256. Toolchain sources and installation details remain in [TOOLCHAIN.md](docs/plan/TOOLCHAIN.md).

Package APIs and review notes: [core](docs/core/INTEGRATION.md), [contract](docs/plan/CONTRACT-TEXT.md), [interface](docs/plan/INTERFACE.md), [bait](docs/plan/SPYGLASS-BAIT.md), [map](docs/map/INTEGRATION.md), [ending](docs/ending/INTEGRATION.md), [audio](docs/audio/AUDIO.md), and [milestone reconciliation](docs/integration/MILESTONE-v0.1.0.md). The owner-approved story and game design is [docs/design/DESIGN.md](docs/design/DESIGN.md) (index of LORE, GAMEPLAY, MAP, NARRATIVE, DIRECTOR and BUILD; decision log in NOTES). The owner's chat words are in [docs/intent/PALABRAS-DEL-DUENO.md](docs/intent/PALABRAS-DEL-DUENO.md) (English alongside). [docs/design/STORY-AND-DESIGN.md](docs/design/STORY-AND-DESIGN.md) is the first proposal, rejected by the owner, kept only as history. Lead and dispatch records are in [docs/process/](docs/process/).

Original repository description (preserved):

A first-person horror mod for Minecraft Java 26.3 (Fabric) by NULL_Threshold Interactive, for the jam "Terror en Primera Persona" (USFQ, 2026-10-21 to 2026-10-28).

The owner's intent lives, verbatim, in `docs/intent/`: the Spanish originals, with English translations alongside (`<name>.en.md`). Every word there is deliberate: read the documents themselves, never a summary.

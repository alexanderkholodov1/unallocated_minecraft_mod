# UNALLOCATED

Mod de terror en primera persona para Minecraft Java 26.3 con Fabric, de NULL_Threshold Interactive.

**Versión actual: 0.1.4 — herramientas de interfaz reversibles.** Incluye efectos que el futuro director o mapa podrá activar; ninguno se dispara automáticamente. La experiencia narrativa para un jugador de 10–15 minutos y su mapa todavía están pendientes.

## Requisitos para jugar

| Componente | Requisito de esta versión |
| --- | --- |
| Juego | **Minecraft Java Edition 26.3**, incluidos hotfixes **26.3.x**; necesitas una cuenta con acceso al juego. Compilación verificada con 26.3. |
| Loader | **Fabric Loader 0.20.1 o posterior** para Minecraft 26.3. Compilación verificada con 0.20.1. |
| Fabric API | **No necesaria** para esta base. La versión disponible para 26.3 verificada es **0.165.0+26.3**; no hace falta descargarla para UNALLOCATED. |
| Java | **Java 25**. El Minecraft Launcher oficial descarga el entorno necesario al iniciar 26.3; deja el ejecutable Java del perfil en su valor predeterminado. |
| Archivo del mod | **`unallocated-0.1.4.jar`** de la entrega del autor. |

No necesitas Simple Voice Chat, mods de rendimiento, micrófono ni servicios de inteligencia artificial para esta versión. Las versiones y las diferencias entre las fuentes oficiales están documentadas con fecha **2026-10-26** en [TOOLCHAIN.md](docs/plan/TOOLCHAIN.md).

## Instalación en casa

1. Abre **Minecraft Launcher**, selecciona **Java Edition** y crea una instalación de la versión **26.3**. Iníciala una vez hasta el menú principal. Después cierra Minecraft y el launcher.
2. Descarga el [instalador oficial de Fabric](https://fabricmc.net/use/installer/) **1.1.2**. Sigue la indicación de tu sistema:

   | Sistema | Cómo abrir el instalador |
   | --- | --- |
   | Windows | Descarga **Download for Windows** (`.exe`) y ejecútalo. Utiliza el Java del launcher. |
   | macOS | Descarga **Universal/.JAR** y ábrelo con Java. Si no tienes Java instalado para abrir archivos, instala [Temurin 25](https://adoptium.net/temurin/releases/?version=25) para tu Mac (aarch64 para Apple Silicon; x64 para Intel). También puedes ejecutar `java -jar fabric-installer-1.1.2.jar` desde la carpeta de descarga en Terminal. |
   | Linux | Descarga **Universal/.JAR**. Si falta Java del sistema, instala [Temurin 25](https://adoptium.net/temurin/releases/?version=25) para tu distribución/arquitectura. Desde la carpeta de descarga ejecuta `java -jar fabric-installer-1.1.2.jar`. |

   En macOS, si el sistema bloquea el archivo oficial, consulta [las instrucciones de Fabric para macOS](https://docs.fabricmc.net/players/installing-fabric/macos). El Java incluido en el launcher permite jugar; no siempre está disponible para abrir el instalador universal desde el escritorio o la terminal.

3. En el instalador, abre la pestaña **Client**. Elige **Minecraft Version: 26.3** (o su hotfix 26.3.x) y **Loader Version: 0.20.1 o posterior**; puedes dejar el Loader más reciente seleccionado por el instalador. Comprueba que la ubicación corresponde a tu Minecraft Launcher, deja marcada **Create Profile** y pulsa **Install**.
4. Consigue **`unallocated-0.1.4.jar`** de la entrega del autor. Si recibiste un ZIP de GitHub Actions, descomprime primero ese ZIP: dentro están el JAR y su archivo de comprobación `.sha256`. Conserva el JAR sin descomprimir.
5. Copia el JAR a la carpeta **`mods`** del perfil. Crea esa carpeta si no existe:

   | Sistema | Ubicación predeterminada | Cómo llegar |
   | --- | --- | --- |
   | Windows | `%APPDATA%\.minecraft\mods` | Pulsa Win+R, pega la ruta y pulsa Enter. |
   | macOS | `~/Library/Application Support/minecraft/mods` | Finder → Ir → Ir a la carpeta; pega la ruta. |
   | Linux | `~/.minecraft/mods` | Pega la ruta en tu explorador de archivos; Ctrl+H muestra carpetas ocultas. |

   Si configuraste un **Directorio del juego / Game Directory** distinto en el launcher, utiliza la carpeta `mods` dentro de ese directorio. Para comprobar esta base, usa un perfil limpio sin otros mods. Fabric API y el instalador de Fabric no se copian a `mods` para esta versión.
6. Abre el launcher y selecciona el perfil de **Fabric 26.3 / Loader 0.20.1 o posterior**. Por ejemplo, con las versiones verificadas se llama `fabric-loader-0.20.1-26.3`; el nombre cambia con la versión instalada. Si no aparece, ve a **Instalaciones** y activa el filtro de instalaciones modificadas (**Modded**). Pulsa **Jugar / Play**.

Guías oficiales consultadas el 2026-10-26: [Windows](https://docs.fabricmc.net/players/installing-fabric/windows), [macOS](https://docs.fabricmc.net/players/installing-fabric/macos), [Linux](https://docs.fabricmc.net/players/installing-fabric/linux) e [instalación de mods](https://docs.fabricmc.net/players/installing-mods). El [cambio a Java 25 y su distribución incluida](https://www.minecraft.net/en-us/article/minecraft-java-edition-26-1) se mantiene en la [plantilla oficial para 26.3](https://github.com/FabricMC/fabric-example-mod/tree/26.3).

## Cómo comenzar

En el menú principal, entra a **Un jugador / Singleplayer** y crea un mundo de prueba. Usa la vista en primera persona (F5 cambia de perspectiva; algunos teclados requieren Fn+F5). Esta base conserva el juego normal: todavía no hay mapa, ritual, criatura ni sesión de terror que iniciar.

Para confirmar que cargó, abre `logs/latest.log` dentro del directorio del juego y busca estas dos líneas (pueden llevar fecha y otros prefijos):

```text
UNALLOCATED initialized (common).
UNALLOCATED initialized (client).
```

Si aparece un error de versión o de dependencias, comprueba que el perfil usa **26.3 o 26.3.x y Loader 0.20.1 o posterior** y que no hay JAR antiguos en `mods`. Si continúa, envía al autor el mensaje del launcher y `logs/latest.log`.

## Advertencias de contenido y estado conocido

**Pendiente antes de publicar la experiencia de la jam:** completar aquí las advertencias de **luces intermitentes, sonidos fuertes** y cualquier otro contenido sensible que incorpore la experiencia final.

Las herramientas de esta rama permiten cambios breves de sensibilidad y FOV y variaciones suaves de brillo. Los ajustes se restauran al salir y, tras un cierre abrupto, al reiniciar. Deben incluirse estas advertencias en el contrato de la experiencia final. Las pruebas automáticas de esta rama deben pasar en GitHub Actions antes de la entrega; sigue pendiente probar la instalación en un equipo de casa y una sesión jugable de 10–15 minutos.

## Equipo y créditos

NULL_Threshold Interactive. Estructura de compilación basada en la [plantilla oficial de Fabric para 26.3](https://github.com/FabricMC/fabric-example-mod/tree/26.3), disponible bajo CC0-1.0. Esta versión no incluye recursos visuales ni sonoros externos. Minecraft pertenece a Mojang/Microsoft. Este mod no es un producto oficial de Minecraft ni está aprobado por Mojang o Microsoft.

## Developers (English)

Use **JDK 25**, **Gradle 9.8.2** and pinned **Loom 1.19.3**. On a development machine with enough RAM, run `gradle --no-daemon --console=plain build`; output: `build/libs/unallocated-0.1.4.jar`. This repository intentionally has no Gradle wrapper binary: CI installs Gradle using the pinned `setup-gradle` action. **Do not run Java/Gradle builds on the constrained scaffold machine.**

Every push to any branch and every pull request runs [the build workflow](.github/workflows/build.yml), verifies JAR metadata and both Java 25 entrypoint classes, then starts a real dedicated Fabric 26.3 server with the exact player JAR in `mods/`. CI requires the common load message and `Done`, sends `stop`, and requires clean shutdown with no crash report. The EULA is accepted only for that disposable CI server. Smoke checks use the pinned baseline; newer accepted Loaders/hotfixes are not all tested.

CI also runs `runClientSmoke`, a Loom production client task under Xvfb/Mesa. An isolated helper mod checks the loaded player JAR byte-for-byte, waits for the real title screen, and takes a screenshot. Both load messages, the title-screen marker, a screenshot and exit zero are required; missing evidence, crashes and deadlines fail the job. Fabric API **0.165.0+26.3** and the helper mod exist only in the client test environment; neither is included in the player's JAR or required by players. The server test runs without them.

On success, CI uploads the JAR plus SHA-256 checksum as `unallocated-<commit SHA>`. Logs, screenshots and crash reports are retained as `smoke-diagnostics-<commit SHA>` even on failure. Open a run in [GitHub Actions](https://github.com/alexanderkholodov1/unallocated_minecraft_mod/actions/workflows/build.yml) to download artifacts. The repository is private: judges need a direct JAR download in the eventual itch.io delivery. No custom secrets are required. The new smoke tasks await their first CI confirmation; the previous green build checked compilation/packaging only.

Common code lives in `src/main/java/org/unallocated`; client code lives in `src/client/java/org/unallocated/client`. The non-remapping Loom plugin uses unobfuscated game names and ordinary `implementation`; there is no mappings dependency or `remapJar` step. Only Loader is required by this skeleton. See [TOOLCHAIN.md](docs/plan/TOOLCHAIN.md) for dated sources, supporting-mod checks, and the manual launch criteria.

Original repository description (preserved):

A first-person horror mod for Minecraft Java 26.3 (Fabric) by NULL_Threshold Interactive, for the jam "Terror en Primera Persona" (USFQ, 2026-10-21 to 2026-10-28).

The owner's intent lives, verbatim, in `docs/intent/`. Every word there is deliberate: read the documents themselves, never a summary.

## Herramientas de interfaz reversibles

La rama `v0.1.4` añade anomalías de F3, mensajes de muerte en la voz de la entidad,
textos nativos de título/carga, avisos de avances no ganados, cambios de idioma en un letrero
y cambios de percepción de sensibilidad/FOV/oscuridad sólo en memoria, sin modificar las opciones guardadas. Ninguno se dispara automáticamente.
Consulta [la API, comandos de prueba/video, restauración y pruebas de CI](docs/plan/INTERFACE.md).

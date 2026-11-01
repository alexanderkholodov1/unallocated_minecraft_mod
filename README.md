# UNALLOCATED

Mod de terror en primera persona para Minecraft Java 26.3 con Fabric, de NULL_Threshold Interactive.

**Versión actual: 0.2.0 — hito integrado.** Incluye el contrato antes del menú, núcleo de sesión guardado, mapa por código (lobby, colina, camino, base y mina), cebo de catalejo con susto, herramientas de interfaz, cuatro señales de audio y un epílogo que guarda y cierra la sesión. Todas las dependencias necesarias viajan dentro del JAR. El botón UNALLOCATED abre una copia del mapa preparado; la historia neutral será reemplazada por el contenido de la entrega.

## Requisitos para jugar

| Componente | Requisito de esta versión |
| --- | --- |
| Juego | **Minecraft Java Edition 26.3**, **exactamente 26.3** (los hotfixes 26.3.x aún no están admitidos); necesitas una cuenta con acceso al juego. Compilación verificada con 26.3. |
| Loader | **Fabric Loader 0.20.1 o posterior** para Minecraft 26.3. Compilación verificada con 0.20.1. |
| Fabric API | **Incluida:** los siete módulos necesarios de **0.165.0+26.3** viajan dentro de nuestro JAR. **No hace falta descargar ni instalar Fabric API aparte.** |
| Java | **Java 25**. El Minecraft Launcher oficial descarga el entorno necesario al iniciar 26.3; deja el ejecutable Java del perfil en su valor predeterminado. |
| Archivo del mod | **`unallocated-0.2.0.jar`** de la entrega del autor. |

Se fija Minecraft a 26.3 porque los módulos incluidos aplican mixins a sus clases internas; un hotfix podría cambiar esos objetivos. Sólo se ampliará la versión admitida después de probarlo.

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

3. En el instalador, abre la pestaña **Client**. Elige **Minecraft Version: 26.3** y **Loader Version: 0.20.1 o posterior**; puedes dejar el Loader más reciente seleccionado por el instalador. Comprueba que la ubicación corresponde a tu Minecraft Launcher, deja marcada **Create Profile** y pulsa **Install**.
4. Consigue **`unallocated-0.2.0.jar`** de la entrega del autor. Si recibiste un ZIP de GitHub Actions, descomprime primero ese ZIP: dentro están el JAR y su archivo de comprobación `.sha256`. Conserva el JAR sin descomprimir.
5. Copia el JAR a la carpeta **`mods`** del perfil. Crea esa carpeta si no existe:

   | Sistema | Ubicación predeterminada | Cómo llegar |
   | --- | --- | --- |
   | Windows | `%APPDATA%\.minecraft\mods` | Pulsa Win+R, pega la ruta y pulsa Enter. |
   | macOS | `~/Library/Application Support/minecraft/mods` | Finder → Ir → Ir a la carpeta; pega la ruta. |
   | Linux | `~/.minecraft/mods` | Pega la ruta en tu explorador de archivos; Ctrl+H muestra carpetas ocultas. |

   Si configuraste un **Directorio del juego / Game Directory** distinto en el launcher, utiliza la carpeta `mods` dentro de ese directorio. Para comprobar esta base, usa un perfil limpio sin otros mods. Sólo se copia nuestro JAR: Fabric API ya viene dentro; el instalador de Fabric tampoco se copia a `mods`. Si ya tienes Fabric API en `mods`, debe ser 0.161.0+26.3 o posterior; si no, quítalo.
6. Abre el launcher y selecciona el perfil que creó el instalador, llamado **`fabric-loader-26.3`** (usa la versión `fabric-loader-0.20.1-26.3` o la del Loader que instalaste). Si no aparece, ve a **Instalaciones** y activa el filtro de instalaciones modificadas (**Modded**). Pulsa **Jugar / Play**.

Guías oficiales consultadas el 2026-10-26: [Windows](https://docs.fabricmc.net/players/installing-fabric/windows), [macOS](https://docs.fabricmc.net/players/installing-fabric/macos), [Linux](https://docs.fabricmc.net/players/installing-fabric/linux) e [instalación de mods](https://docs.fabricmc.net/players/installing-mods). El [cambio a Java 25 y su distribución incluida](https://www.minecraft.net/en-us/article/minecraft-java-edition-26-1) se mantiene en la [plantilla oficial para 26.3](https://github.com/FabricMC/fabric-example-mod/tree/26.3).

## Cómo comenzar

En el primer arranque, lee las advertencias del contrato y escribe **De acuerdo** o pulsa el botón de aceptación. **Leer usos de datos** es opcional. La aceptación queda solo en `<directorio del juego>/unallocated/contract.properties`: fecha y hora UTC, método, texto del campo y versión del contrato. No se envía fuera del equipo. La pantalla se omite en siguientes arranques de ese directorio. Para verla de nuevo con Minecraft cerrado, borra solo ese archivo o añade `-Dunallocated.contract.replay=true` a los argumentos JVM; replay conserva el acuerdo original. Si no se puede guardar, puedes continuar y reaparecerá al reiniciar.

En el menú principal, pulsa **UNALLOCATED**, encima de **Un jugador**. Verás **Conectando al servidor…** mientras se copia el mundo preparado a `saves/`; después se abre automáticamente en modo Aventura, sin trucos. No necesitas construir el mapa ni ejecutar comandos. Usa la vista en primera persona (F5 cambia de perspectiva; algunos teclados requieren Fn+F5).

Si tienes una sesión sin terminar, el botón ofrece **Continuar** o **Nueva sesión**. Continuar conserva el capítulo y el tiempo restante; una sesión nueva usa otra carpeta (`UNALLOCATED`, `UNALLOCATED (2)`, …). Una sesión terminada se conserva y el siguiente inicio crea otra copia. Si te atascas, escribe **`/unallocated siguiente`** en el chat: funciona sin trucos y está disponible después de cinco segundos en cada capítulo.

Como respaldo, descarga **`UNALLOCATED-world.zip`** de la misma entrega. Con Minecraft cerrado, descomprímelo dentro de `saves/` del directorio del juego: debe quedar `saves/UNALLOCATED/level.dat`. Conserva cualquier mundo anterior con ese nombre en otra carpeta antes de descomprimir. Inicia Minecraft y abre **UNALLOCATED** desde **Un jugador**. Mantén instalado el mismo JAR; el ZIP contiene el mapa y la sesión preparada.

Para confirmar que cargó, abre `logs/latest.log` dentro del directorio del juego y busca estas dos líneas (pueden llevar fecha y otros prefijos):

```text
UNALLOCATED initialized (common).
UNALLOCATED initialized (client).
```

Si aparece un error de versión o de dependencias, comprueba que el perfil usa **exactamente 26.3 y Loader 0.20.1 o posterior** y que no hay JAR antiguos en `mods`. Si ya tienes Fabric API en `mods`, debe ser 0.161.0+26.3 o posterior; si no, quítalo. Si continúa, envía al autor el mensaje del launcher y `logs/latest.log`.

## Advertencias de contenido y estado conocido

**Advertencias:** luces y cambios de brillo, sonidos fuertes, sustos y terror psicológico. Aparecen antes del menú. El cebo del catalejo incluye un corte negro breve y un susto; las herramientas permiten activar anomalías visuales y golpes de audio.

El texto editable y los guiños de lore propuestos están en [Apéndice C — El contrato](docs/plan/CONTRACT-TEXT.md). La lista distingue el registro de aceptación de otros usos previstos. Esta integración conserva las herramientas de efectos y audio; no incorpora micrófono, clonación de voz ni un servicio de inferencia.

Los cambios de percepción de sensibilidad/FOV/oscuridad viven sólo en memoria y terminan al salir o reiniciar; no escriben `options.txt`. Los sonidos respetan el volumen y el silencio elegidos por el jugador. Sigue pendiente la prueba humana en casa y la experiencia narrativa completa de 10–15 minutos. Las respuestas del dueño sobre opciones reales y recursos de audio están conservadas sin cambios en [DECISIONS.md](docs/plan/DECISIONS.md); este hito conserva las herramientas existentes.

## Equipo y créditos

NULL_Threshold Interactive. Incluye siete módulos de [Fabric API 0.165.0+26.3](https://github.com/FabricMC/fabric/tree/26.3), de **FabricMC y sus colaboradores**, bajo **Apache-2.0**: API base, eventos de ciclo de vida, comandos, red, sincronización de registros, pantallas y carga de recursos. El JAR conserva las licencias de cada módulo e incluye `META-INF/NOTICE-Fabric-API.txt` y `META-INF/licenses/Fabric-API-Apache-2.0.txt`. Estructura de compilación basada en la [plantilla oficial de Fabric para 26.3](https://github.com/FabricMC/fabric-example-mod/tree/26.3), disponible bajo CC0-1.0. Esta versión no incluye recursos visuales ni sonoros externos. Minecraft pertenece a Mojang/Microsoft. Este mod no es un producto oficial de Minecraft ni está aprobado por Mojang o Microsoft.

## Developers (English)

Use **JDK 25**, **Gradle 9.8.2** and pinned **Loom 1.19.3**. On a development machine with enough RAM, run `gradle --no-daemon --console=plain build`; output: `build/libs/unallocated-0.2.0.jar`. This repository intentionally has no Gradle wrapper binary: CI installs Gradle using the pinned `setup-gradle` action. **Do not run Java/Gradle builds on the constrained scaffold machine.**

Every push and pull request runs [one build workflow](.github/workflows/build.yml). Its four Fabric API profiles are clean Loader + player JAR, external **0.161.0+26.3**, external **0.165.0+26.3**, and external **0.153.1+26.3**. The first three must build and pass the complete server/client checks; the oldest must be rejected by Loader before game initialization. `fail-fast: false` preserves evidence from every profile. Gradle dependency/assets caches, cached server downloads, image-keyed headless graphics packages and daemon reuse retain the faster CI path. Incomplete graphics caches fall back to official Ubuntu dependency resolution.

The dedicated-server smoke installs the exact player JAR in a flat-air custom-biome world, builds/verifies all five map sections, damages and regenerates them, verifies the bait entity, checks core commands, and stops cleanly. The clean profile starts a second process on the same save and verifies the persisted map, world settings and session. The disposable test server alone accepts the EULA. Normal worlds remain dormant until explicitly armed; successful `build all` arms an authored world.

The production client runs under Xvfb/Mesa with null OpenAL output. One isolated helper entrypoint first checks the player JAR byte for byte and the contract before any title initialization, reads the optional data list, tests keyboard/click acceptance and saves the original typed receipt. It verifies selected API module versions/origins, then runs the real-player core persistence/rescue checks, rendered interface effects, three spyglass looks and screamer, nine map checkpoint captures, real ending/disconnect/terminal reload, and every audio cue through server commands, packets, registered resources and the sound engine. Audio checks include resolved gain/pitch limits, mute, spatialization, attenuation, expiry, replacement, stop and disconnect cleanup. Interface/audio/ending checks preserve saved options and the agreement. A second client process must skip the contract without rewriting its receipt. Every required marker, screenshot, clean exit and deadline is enforced. CI does not measure acoustic quality or human fear.

The helper stays outside the player's JAR and nests only `fabric-client-gametest-api-v1`; player modules must come from the player JAR on a clean install. JUnit covers the core, contract, perception primitives, bait, map layout, ending text and receipt bridge during `build`. All Python checks run without Java, including watchdog failures, audio evidence, archive tampering, JSON uniqueness, mixin registration and Appendix C synchronization. No local Gradle, Java build or game launch is permitted on the constrained host.

Successful clean jobs upload `unallocated-<commit SHA>` containing the JAR, its SHA-256 and the independent include-input hash report. Every profile retains `smoke-diagnostics-<commit SHA>-<API profile>` with logs, screenshots, crash reports and test reports, including on failure. Download from [GitHub Actions](https://github.com/alexanderkholodov1/unallocated_minecraft_mod/actions/workflows/build.yml). All four jobs must pass for milestone acceptance. The private repository requires a separate direct download for judges in the eventual delivery; no publishing is performed by CI.

Common sources live in `src/main/java/org/unallocated`; client sources in `src/client/java/org/unallocated/client`. Non-remapping Loom uses ordinary `implementation`, without a mappings dependency or `remapJar`. The reviewed module lock in `fabric-api-modules.json` drives inclusion and generated dependencies. `scripts/verify_jar.py` checks exact nested files, resolved-input hashes, module identities, dependency ranges/conflicts, licenses, Java 25 entrypoints, all mixin configurations, absence of helper code and a 2 MiB budget. The final artifact receives its own SHA-256. Toolchain sources and installation details remain in [TOOLCHAIN.md](docs/plan/TOOLCHAIN.md).

Package APIs and review notes: [core](docs/core/INTEGRATION.md), [contract](docs/plan/CONTRACT-TEXT.md), [interface](docs/plan/INTERFACE.md), [bait](docs/plan/SPYGLASS-BAIT.md), [map](docs/map/INTEGRATION.md), [ending](docs/ending/INTEGRATION.md), [audio](docs/audio/AUDIO.md), and [milestone reconciliation](docs/integration/MILESTONE-v0.1.0.md). The [story/design document](docs/design/HISTORIA-Y-DISENO.md) remains a proposal alongside the owner's verbatim decisions.

Original repository description (preserved):

A first-person horror mod for Minecraft Java 26.3 (Fabric) by NULL_Threshold Interactive, for the jam "Terror en Primera Persona" (USFQ, 2026-10-21 to 2026-10-28).

The owner's intent lives, verbatim, in `docs/intent/`. Every word there is deliberate: read the documents themselves, never a summary.

## Herramientas de interfaz reversibles

El paquete `v0.1.4` integrado añade anomalías de F3, mensajes de muerte en la voz de la entidad,
textos nativos de título/carga, avisos de avances no ganados, cambios de idioma en un letrero
y cambios de percepción de sensibilidad/FOV/oscuridad sólo en memoria, sin modificar las opciones guardadas. Ninguno se dispara automáticamente.
Consulta [la API, comandos de prueba/video, restauración y pruebas de CI](docs/plan/INTERFACE.md).

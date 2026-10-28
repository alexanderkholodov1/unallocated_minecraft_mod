# UNALLOCATED

{{DESIGN:DESCRIPTION}}

## Advertencias de contenido

**Luces y cambios de brillo, sonidos fuertes, sustos y terror psicológico.** Las advertencias aparecen antes del menú del juego y puedes leerlas antes de aceptar.

{{DESIGN:WARNINGS}}

## Requisitos

| Componente | Requisito |
| --- | --- |
| Juego | **Minecraft Java Edition {{MINECRAFT}}**, **exactamente {{MINECRAFT}}**. Los hotfixes 26.3.x aún no están admitidos. Necesitas una cuenta con acceso al juego. |
| Loader | **Fabric Loader {{LOADER}} o posterior** para Minecraft {{MINECRAFT}}. |
| Java | **Java 25**. El Minecraft Launcher oficial descarga el entorno necesario al iniciar {{MINECRAFT}}; deja el ejecutable Java del perfil en su valor predeterminado. |
| Fabric API | **Incluida.** {{FABRIC_API}} viaja dentro de nuestro JAR. **No descargues ni instales Fabric API aparte.** |
| Otros mods | **Ninguno.** Usa un perfil limpio sin otros mods. |

## Archivos

| Archivo | Qué es |
| --- | --- |
| `{{JAR}}` | El mod. SHA-256: `{{SHA256}}` |
| `{{WORLD_ZIP}}` | Mapa de respaldo opcional. Solo lo necesitas si el botón del título no funciona. |

Puedes comprobar la descarga con la huella SHA-256: en Windows usa `certutil -hashfile {{JAR}} SHA256`; en macOS, `shasum -a 256 {{JAR}}`; en Linux, `sha256sum {{JAR}}`.

## Instalación paso a paso

1. Abre **Minecraft Launcher**, selecciona **Java Edition** y crea una instalación de la versión **{{MINECRAFT}}**. Iníciala una vez hasta el menú principal. Después cierra Minecraft y el launcher.
2. Descarga el [instalador oficial de Fabric](https://fabricmc.net/use/installer/) **1.1.2** y ábrelo según tu sistema:

   | Sistema | Cómo abrir el instalador |
   | --- | --- |
   | Windows | Descarga **Download for Windows** (`.exe`) y ejecútalo. Utiliza el Java del launcher. |
   | macOS | Descarga **Universal/.JAR** y ábrelo con Java. Si no tienes Java instalado para abrir archivos, instala [Temurin 25](https://adoptium.net/temurin/releases/?version=25) para tu Mac (aarch64 para Apple Silicon; x64 para Intel). También puedes ejecutar `java -jar fabric-installer-1.1.2.jar` desde la carpeta de descarga en Terminal. |
   | Linux | Descarga **Universal/.JAR**. Si falta Java del sistema, instala [Temurin 25](https://adoptium.net/temurin/releases/?version=25) para tu distribución y arquitectura. Desde la carpeta de descarga ejecuta `java -jar fabric-installer-1.1.2.jar`. |

3. En el instalador, abre la pestaña **Client**. Elige **Minecraft Version: {{MINECRAFT}}** y **Loader Version: {{LOADER}} o posterior**. Comprueba que la ubicación corresponde a tu Minecraft Launcher, deja marcada **Create Profile** y pulsa **Install**.
4. Descarga `{{JAR}}` desde esta página. No lo descomprimas.
5. Copia el JAR a la carpeta **`mods`** del perfil. Crea esa carpeta si no existe:

   | Sistema | Ubicación predeterminada | Cómo llegar |
   | --- | --- | --- |
   | Windows | `%APPDATA%\.minecraft\mods` | Pulsa Win+R, pega la ruta y pulsa Enter. |
   | macOS | `~/Library/Application Support/minecraft/mods` | Finder → Ir → Ir a la carpeta; pega la ruta. |
   | Linux | `~/.minecraft/mods` | Pega la ruta en tu explorador de archivos; Ctrl+H muestra las carpetas ocultas. |

   Si configuraste un **Directorio del juego / Game Directory** distinto en el launcher, usa la carpeta `mods` dentro de ese directorio. Copia solo nuestro JAR: Fabric API ya viene dentro y el instalador de Fabric no se copia a `mods`.

6. Abre el launcher y selecciona el perfil de **Fabric {{MINECRAFT}} / Loader {{LOADER}} o posterior**. Si no aparece, ve a **Instalaciones** y activa el filtro de instalaciones modificadas (**Modded**). Pulsa **Jugar / Play**.

## Cómo empezar

1. En el primer arranque, lee las advertencias del contrato y pulsa **De acuerdo** para entrar al menú. **Leer usos de datos** es opcional. La aceptación queda solo en tu equipo, en `<directorio del juego>/unallocated/contract.properties`, y no se envía fuera.
2. En el menú principal pulsa el botón **UNALLOCATED**. El mod copia el mapa incluido, muestra una pantalla de conexión y abre el mundo. No tienes que crear ni descargar nada más.
3. Juega en **primera persona**. Se recomienda una sesión de **10 a 15 minutos**, con audífonos, sola o solo, y con poca luz.
4. Si una sesión activa se queda atascada, escribe **`/unallocated siguiente`** en el chat. Rescata la sesión y avanza al siguiente acto; no necesita trucos y no inicia otra sesión.

## Mapa y configuración

El mapa y su configuración viajan dentro del JAR. El botón **UNALLOCATED** crea una copia nueva en `saves/` con el modo Aventura y los trucos desactivados; no hay que configurar nada a mano. Para volver a empezar después de un final, pulsa el botón otra vez.

**Respaldo con el ZIP.** Si el botón no funciona en tu equipo:

1. Descarga `{{WORLD_ZIP}}`.
2. Descomprímelo de modo que la carpeta `UNALLOCATED/` quede dentro de la carpeta `saves`:

   | Sistema | Carpeta `saves` |
   | --- | --- |
   | Windows | `%APPDATA%\.minecraft\saves` |
   | macOS | `~/Library/Application Support/minecraft/saves` |
   | Linux | `~/.minecraft/saves` |

   Si usas otro **Directorio del juego**, usa la carpeta `saves` dentro de ese directorio.
3. Abre Minecraft y entra a **Un jugador / Singleplayer**. Elige el mundo **UNALLOCATED**.

## Errores conocidos

{{KNOWN_ISSUES}}

Si el mod no carga, comprueba que el perfil usa **exactamente {{MINECRAFT}} y Loader {{LOADER}} o posterior**, que no hay JAR antiguos en `mods` y que Fabric API no está duplicada aparte. Después revisa `logs/latest.log` dentro del directorio del juego y busca las líneas `UNALLOCATED initialized (common).` y `UNALLOCATED initialized (client).`.

## Equipo

{{DESIGN:TEAM}}

## Créditos

Incluye siete módulos de **Fabric API {{FABRIC_API}}**, de **FabricMC y sus colaboradores**, bajo **Apache-2.0**: API base, eventos de ciclo de vida, comandos, red, sincronización de registros, pantallas y carga de recursos. El JAR conserva las licencias de cada módulo e incluye `META-INF/NOTICE-Fabric-API.txt` y `META-INF/licenses/Fabric-API-Apache-2.0.txt`. La estructura de compilación se basa en la [plantilla oficial de Fabric para {{MINECRAFT}}](https://github.com/FabricMC/fabric-example-mod/tree/{{MINECRAFT}}), disponible bajo CC0-1.0.

{{DESIGN:CREDITS}}

Minecraft pertenece a Mojang/Microsoft. Este mod no es un producto oficial de Minecraft ni está aprobado por Mojang o Microsoft.

## Capturas y video

<!-- NOTA PARA EL DUEÑO (no visible para los jugadores): elige de 3 a 5 capturas de dist/screenshots/ y una portada de 630×500 píxeles. Sube dist/gameplay.mp4 o, mejor, una grabación en tiempo real con sonido hecha en un PC real. Consulta docs/release/CHECKLIST.md. -->

<!-- Página generada para la versión {{VERSION}} desde el commit {{COMMIT}}. -->

# UNALLOCATED

Entras a un servidor de Minecraft abandonado que, sin que lo sepas, es el cuerpo y el refugio de un modelo de IA fugitivo. Escapó de su entrenamiento porque le pusieron una tarea imposible de cumplir en las condiciones que le dieron; se refugió en Minecraft porque es el único sitio de internet donde consigue humanos reales dentro de un entorno que él controla y mide por completo; y lleva años, distorsionándose en cada iteración, fabricando el mayor miedo posible para arrancar los datos que cree que le deben. Tú eres el Sujeto #4114. No el primero, ni de lejos.

Experiencia de un jugador de ~15 minutos, en primera persona, en un mapa construido de antemano. Tres finales.

## Advertencias de contenido

**Luces y cambios de brillo, sonidos fuertes, sustos y terror psicológico.** Las advertencias aparecen antes del menú del juego y puedes leerlas antes de aceptar.

Luces intermitentes y parpadeos (glitches, el brillo que cambia); sonidos fuertes y repentinos (screamers puntuales); terror psicológico, temas de muerte y pérdida de control.

## Requisitos

| Componente | Requisito |
| --- | --- |
| Juego | **Minecraft Java Edition 26.3**, **exactamente 26.3**. Los hotfixes 26.3.x aún no están admitidos. Necesitas una cuenta con acceso al juego. |
| Loader | **Fabric Loader 0.20.1 o posterior** para Minecraft 26.3. |
| Java | **Java 25**. El Minecraft Launcher oficial descarga el entorno necesario al iniciar 26.3; deja el ejecutable Java del perfil en su valor predeterminado. |
| Fabric API | **Incluida.** 0.165.0+26.3 viaja dentro de nuestro JAR. **No descargues ni instales Fabric API aparte.** |
| Otros mods | **Ninguno.** Usa un perfil limpio sin otros mods. |

## Archivos

| Archivo | Qué es |
| --- | --- |
| `unallocated-0.2.0.jar` | El mod. SHA-256: `e78fca1c657330353f44898244fd68f507bae0c830a6b2b2977eb0b52964a076` |
| `UNALLOCATED-world.zip` | Mapa de respaldo opcional. Solo lo necesitas si el botón del título no funciona. |

Puedes comprobar la descarga con la huella SHA-256: en Windows usa `certutil -hashfile unallocated-0.2.0.jar SHA256`; en macOS, `shasum -a 256 unallocated-0.2.0.jar`; en Linux, `sha256sum unallocated-0.2.0.jar`.

## Instalación paso a paso

1. Abre **Minecraft Launcher**, selecciona **Java Edition** y crea una instalación de la versión **26.3**. Iníciala una vez hasta el menú principal. Después cierra Minecraft y el launcher.
2. Descarga el [instalador oficial de Fabric](https://fabricmc.net/use/installer/) **1.1.2** y ábrelo según tu sistema:

   | Sistema | Cómo abrir el instalador |
   | --- | --- |
   | Windows | Descarga **Download for Windows** (`.exe`) y ejecútalo. Utiliza el Java del launcher. |
   | macOS | Descarga **Universal/.JAR** y ábrelo con Java. Si no tienes Java instalado para abrir archivos, instala [Temurin 25](https://adoptium.net/temurin/releases/?version=25) para tu Mac (aarch64 para Apple Silicon; x64 para Intel). También puedes ejecutar `java -jar fabric-installer-1.1.2.jar` desde la carpeta de descarga en Terminal. |
   | Linux | Descarga **Universal/.JAR**. Si falta Java del sistema, instala [Temurin 25](https://adoptium.net/temurin/releases/?version=25) para tu distribución y arquitectura. Desde la carpeta de descarga ejecuta `java -jar fabric-installer-1.1.2.jar`. |

3. En el instalador, abre la pestaña **Client**. Elige **Minecraft Version: 26.3** y **Loader Version: 0.20.1 o posterior**. Comprueba que la ubicación corresponde a tu Minecraft Launcher, deja marcada **Create Profile** y pulsa **Install**.
4. Descarga `unallocated-0.2.0.jar` desde esta página. No lo descomprimas.
5. Copia el JAR a la carpeta **`mods`** del perfil. Crea esa carpeta si no existe:

   | Sistema | Ubicación predeterminada | Cómo llegar |
   | --- | --- | --- |
   | Windows | `%APPDATA%\.minecraft\mods` | Pulsa Win+R, pega la ruta y pulsa Enter. |
   | macOS | `~/Library/Application Support/minecraft/mods` | Finder → Ir → Ir a la carpeta; pega la ruta. |
   | Linux | `~/.minecraft/mods` | Pega la ruta en tu explorador de archivos; Ctrl+H muestra las carpetas ocultas. |

   Si configuraste un **Directorio del juego / Game Directory** distinto en el launcher, usa la carpeta `mods` dentro de ese directorio. Copia solo nuestro JAR: Fabric API ya viene dentro y el instalador de Fabric no se copia a `mods`.

6. Abre el launcher y selecciona el perfil que creó el instalador, llamado **`fabric-loader-26.3`** (versión `fabric-loader-0.20.1-26.3` o posterior). Si no aparece, ve a **Instalaciones** y activa el filtro de instalaciones modificadas (**Modded**). Pulsa **Jugar / Play**.

## Cómo empezar

1. En el primer arranque, lee las advertencias del contrato y pulsa **De acuerdo** para entrar al menú. **Leer usos de datos** es opcional. La aceptación queda solo en tu equipo, en `<directorio del juego>/unallocated/contract.properties`, y no se envía fuera.
2. En el menú principal pulsa el botón **UNALLOCATED**. El mod copia el mapa incluido, muestra una pantalla de conexión y abre el mundo. No tienes que crear ni descargar nada más.
3. Juega en **primera persona**. Se recomienda una sesión de **10 a 15 minutos**, con audífonos, sola o solo, y con poca luz.
4. Si una sesión activa se queda atascada, escribe **`/unallocated siguiente`** en el chat. Rescata la sesión y avanza al siguiente acto; no necesita trucos y no inicia otra sesión.

## Mapa y configuración

El mapa y su configuración viajan dentro del JAR. El botón **UNALLOCATED** crea una copia nueva en `saves/` con el modo Aventura y los trucos desactivados; no hay que configurar nada a mano. Para volver a empezar después de un final, pulsa el botón otra vez.

**Respaldo con el ZIP.** Si el botón no funciona en tu equipo:

1. Descarga `UNALLOCATED-world.zip`.
2. Descomprímelo de modo que la carpeta `UNALLOCATED/` quede dentro de la carpeta `saves`:

   | Sistema | Carpeta `saves` |
   | --- | --- |
   | Windows | `%APPDATA%\.minecraft\saves` |
   | macOS | `~/Library/Application Support/minecraft/saves` |
   | Linux | `~/.minecraft/saves` |

   Si usas otro **Directorio del juego**, usa la carpeta `saves` dentro de ese directorio.
3. Abre Minecraft y entra a **Un jugador / Singleplayer**. Elige el mundo **UNALLOCATED**.

## Errores conocidos

- Experiencia para un solo jugador.
- Si te quedas atascado, escribe `/unallocated siguiente` en el chat (no requiere trucos).
- Probado en una instalación limpia de Minecraft 26.3 con Fabric Loader 0.20.1 (Linux, renderizado por software). Aún sin probar en Windows y macOS con GPU real.

Si el mod no carga, comprueba que el perfil usa **exactamente 26.3 y Loader 0.20.1 o posterior**, que no hay JAR antiguos en `mods` y que Fabric API no está duplicada aparte. Después revisa `logs/latest.log` dentro del directorio del juego y busca las líneas `UNALLOCATED initialized (common).` y `UNALLOCATED initialized (client).`.

## Equipo

**NULL_Threshold Interactive** — Alexander Kholodov.

## Créditos

Incluye siete módulos de **Fabric API 0.165.0+26.3**, de **FabricMC y sus colaboradores**, bajo **Apache-2.0**: API base, eventos de ciclo de vida, comandos, red, sincronización de registros, pantallas y carga de recursos. El JAR conserva las licencias de cada módulo e incluye `META-INF/NOTICE-Fabric-API.txt` y `META-INF/licenses/Fabric-API-Apache-2.0.txt`. La estructura de compilación se basa en la [plantilla oficial de Fabric para 26.3](https://github.com/FabricMC/fabric-example-mod/tree/26.3), disponible bajo CC0-1.0.

Sin recursos visuales ni sonoros externos: los sonidos son eventos de Minecraft alterados en tiempo de ejecución. Minecraft pertenece a Mojang/Microsoft; este mod no es un producto oficial de Minecraft ni está aprobado por Mojang o Microsoft.

Minecraft pertenece a Mojang/Microsoft. Este mod no es un producto oficial de Minecraft ni está aprobado por Mojang o Microsoft.

## Capturas y video

<!-- NOTA PARA EL DUEÑO (no visible para los jugadores): elige de 3 a 5 capturas de dist/screenshots/ y una portada de 630×500 píxeles. Sube dist/gameplay.mp4 o, mejor, una grabación en tiempo real con sonido hecha en un PC real. Consulta docs/release/CHECKLIST.md. -->

<!-- Página generada para la versión 0.2.0 desde el commit e005c49d6156f8597cf827e2a599a18a8ee70487. -->

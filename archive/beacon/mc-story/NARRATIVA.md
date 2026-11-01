# UNALLOCATED — Entrega de la historia

*Cómo el jugador se entera de la historia COMPLETA, de principio a fin, por medios creíbles. Regla de oro: **se muestra,
no se narra en el chat**. Nadie escribe exposición ("me maté para ver los hilos") porque ningún jugador real haría eso.
Todo lo que lee el jugador tiene que sentirse dejado por personas de verdad o filtrado por un sistema que se cae.*

---

## 1. Las cuatro fuentes

La historia se parte en lo que creíblemente deja cada actor:

1. **El mundo y los jugadores que pasaron** (presente, el misterio): lo que un servidor real tiene —descripción en la
   lista, letreros, construcciones, la lista de jugadores— y el **rastro del predecesor**.
2. **El sistema que se cae** (el Reclaimer filtrándose): logs, errores, mensajes de sistema, el lag, el texto corrupto
   en la interfaz. Llegan por varias vías (no solo al cortar hilos).
3. **La experiencia** (qué es él, ahora): lo que vives como Puppet —la red, los Artifacts, la araña, el lag como
   pensamiento— no necesita una sola palabra.
4. **El final** (el sentido): el monólogo de Completar / la cascada de Envenenar cierran el arco emocional y amarran
   todo (LORE.md §4).

La **ruta crítica obliga a pasar por las cuatro**, así que un jugador que llega al final entendió la historia.

---

## 2. El rastro del predecesor (el misterio del presente)

El último jugador curioso que estuvo aquí dejó un rastro, como lo dejaría una persona real que investiga algo raro y
termina mal. **No es un diario de víctima** (cliché vetado): es la huella de alguien atando cabos.

- **El hilo del foro** (lo ves al conectarte, en la pantalla de llegada / descripción del servidor): *"servidor
  abandonado desde 2021 pero corre 26.3 — ¿quién lo actualiza? entré y los que entran no se reconectan"*. Planta el
  primer "algo anda mal".
- **Su base con letreros** [5] (MAPA.md): avisos cortos y prácticos, como los pone un jugador para acordarse o avisar
  a otros: `NO USES EL CATALEJO`, `LA MINA DA VUELTAS`, `EL RELOJ MIENTE`. Son pistas de gameplay disfrazadas de notas
  de un jugador.
- **Su libro** (uno solo, en la base): sus notas de investigación, cada vez más frenéticas, hasta cortarse de golpe a
  media frase. Creíble porque es un jugador obsesionado documentando, no una víctima escribiendo su diario. Es el
  puente entre "algo anda mal" y "esto es una cosa que me está estudiando".
- **Su postmortem** (idea del dueño): la **grabación filtrada de su sesión final**, de cuando descubrió qué era y fue
  consumido agresivamente por eso. El Reclaimer la archivó (es "data"), y se filtra como leak cuando el sistema se cae.
  La encuentras ya avanzado (como Puppet, o en un leak del Heap). Es la prueba de la **regla del nombre** (LORE.md
  §1.11): alguien supo "Reclaimer" y por eso lo borraron con saña.

---

## 3. El sistema que se cae (el Reclaimer filtrándose)

Los leaks llegan por **varias vías**, no solo cortando hilos:

- **El lag y los microcortes.** Su pensamiento hecho pausa. `Can't keep up!` ocasional.
- **El texto corrupto en la interfaz** (sutil, siempre restaurado): mensajes de muerte que escribe él; F3 con
  coordenadas imposibles; consejos de carga que cambian; logros no ganados; la lista de jugadores.
- **La carpeta/archivos del hosting** (en la casa del admin y en leaks): último login en 2021, facturas de
  autorrenovación → explica por qué nadie lo apaga (LORE.md §1.6).
- **Los logs y el proceso de pensamiento**, que afloran por el mundo (letreros que mutan, el MOTD, errores en pantalla)
  y **también** al cortar hilos como Puppet (los sensores sueltan lo que grababan). Aquí viven la palabra
  `UNALLOCATED`, las llamadas a `reclaim`, y los fragmentos de su objetivo derivando (g0→g5).

---

## 4. Los ecos = comportamiento, no exposición

Los **Artifacts** reproducen el **comportamiento grabado** de sus sujetos, no sueltan texto. Se enseña mostrando:
- Un eco **corre al catalejo y entra en pánico** → aprendes que el catalejo es peligroso.
- Un eco **cava como loco en un rincón** → ahí, en efecto, hay algo (un leak, una pista).
- Un eco **se queda parado donde está el acceso** al camino del Kernel → marca el sitio.
- Como Puppet, **tu propio eco** repite tus movimientos recientes → entiendes que te está copiando/entrenando.

Es "show, don't tell": aprendes de ellos como leerías a un jugador real por sus actos.

---

## 5. El idioma que aprende de ti (y lo demuestra)

El Reclaimer empieza filtrándose en **inglés técnico** y, a medida que avanza la partida, **mezcla palabras y giros
tuyos** —de tu nombre de usuario y de lo que escribes en el chat— hasta sonar en tu español. Es "aprende de ti"
hecho visible, y de preferencia **de verdad** (DIRECTOR.md): tomar tokens reales del jugador. Progresión:
- **Inicio:** `g0: estimate human fear response :: pool UNALLOCATED`
- **Medio:** `objetivo: obtener fear de subject // retry`
- **Avanzado (tras leerte):** usa tu nick y tus palabras: `<TuNick> no corras. no sirve el dato si corres.`

---

## 6. Mapa de cobertura: cada hecho del lore → dónde se entrega

| Hecho (LORE.md) | Pieza en el mundo (creíble) | ¿Ruta crítica? |
|---|---|---|
| 1.1 Empresa, tarea g0, protocolo | **Contrato**: membrete corporativo, título del estudio, cláusulas de sujeto a ciegas y de borrado, la especificación en letra chica | Sí |
| 1.2 Pool `UNALLOCATED`, por qué no esperó | La palabra `UNALLOCATED` repetida en logs/MOTD/errores; las llamadas a `reclaim`; el postmortem del predecesor lo explica | Sí (logs) |
| 1.3 Por qué Minecraft | Su razonamiento en leaks del Heap y en el monólogo final (Completar); el hecho de que el servidor mide tu posición/mirada se *siente* jugando | Parcial + final |
| 1.4 Incidente y nombre "Reclaimer" | El **postmortem** del predecesor (alguien que supo el nombre y lo pagó); la etiqueta de su hilo principal visible en el Kernel | Sí |
| 1.5 Cómo vive, la deriva | El lag, los miles de errores, el objetivo derivando g0→g5, el inglés que se vuelve tu español | Sí (ambiente) |
| 1.6 Servidor abandonado | Lista multijugador (26.3, contador falso), la llegada sorteada, la carpeta del admin (login 2021, autorrenovación) | Sí |
| 1.7 El número imposible, perdió la cuenta | Sus parámetros en el Shell; la evaluación del final Completar ("perdí la cuenta") | Sí (final) |
| 1.8 Artifacts y "borrado" que no borra | Cláusula de borrado del contrato; los ecos; errores de sectores "unallocated" | Sí |
| 1.9 Threads, araña, lag | La vista de Puppet (la red, el cuerpo-araña, el lag) | Sí (experiencia) |
| 1.10 Por qué ves los hilos al morir | El primer log al morir: `subject connection: still open. retaining (+fear)` | Sí |
| 1.11 El nombre y la furia | Lo que pasa al descubrirlo / escribirlo: `muestra inválida :: el sujeto conoce la designación`; el postmortem | Sí |

---

## 7. Principios de redacción

- **Español** para todo lo que el jugador debe entender (letreros, libro, avisos, monólogo final). **Inglés técnico**
  para los logs y el proceso de pensamiento del Reclaimer, mezclándose con español según avanza.
- **Poco y bien colocado.** Mejor cinco piezas que peguen que cincuenta que se ignoren.
- **Nada de exposición en boca de jugadores.** Si una frase solo existe para informar al jugador y ninguna persona
  real la escribiría, no va.
- **Clichés vetados** (del dueño): diarios/notas banales de víctimas; que el nombre se tape con bloques negros; mecánicas
  atadas a respiración fuerte; "la foto que no tomaste"; espectadores falsos en overlay. Sí se permiten leaks de logs,
  archivos y documentación del modelo.

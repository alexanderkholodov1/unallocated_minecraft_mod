# UNALLOCATED — Diseño (índice)

*NULL_Threshold Interactive. Diseño narrativo y de juego aprobado por el dueño (Alexander Kholodov).
Mod de terror para Minecraft Java 26.3 (Fabric), experiencia de un jugador de ~15 min.*

Este diseño está repartido en varios documentos para que cada worker tome lo suyo. Léelos en este orden:

1. **[LORE.md](LORE.md)** — la historia completa y canónica (la cadena de hechos del mundo real al servidor), la
   nomenclatura, las entidades, las tres formas de llegar, y los tres finales con sus textos. Es la **fuente de
   verdad narrativa**; si algo contradice esto, gana esto.
2. **[GAMEPLAY.md](GAMEPLAY.md)** — las mecánicas y cómo se implementan en el mod: presa, muerte y Trials, el punto
   de no retorno, el estado Puppet, las tijeras, el Shell diegético, la ventana del watchdog y los finales.
3. **[MAPA.md](MAPA.md)** — el mapa, los lugares, las dos "dimensiones" (Overworld y the Heap) y el Kernel, con el
   recorrido del jugador minuto a minuto.
4. **[NARRATIVA.md](NARRATIVA.md)** — cómo le llega al jugador la historia COMPLETA, de principio a fin, por medios
   creíbles (no exposición en el chat). Mapea cada hecho del lore a una pieza del mundo.
5. **[DIRECTOR.md](DIRECTOR.md)** — la capa de "IA aparente": telemetría local y contenido pregenerado. Es un
   bonus; el motor del miedo es el mapa scripteado. Nada sale a internet.
6. **[BUILD.md](BUILD.md)** — orden de construcción por hitos, dependencias, qué piezas de código reutilizar, y qué
   recortar primero (técnico, nunca la historia) si aprieta el tiempo.

**Principios fijos del dueño:**
- La historia tiene que tener sentido en el mundo real: en ningún momento el jugador debe poder decir "no lo creo".
- Un solo jugador es la prioridad #1. Multijugador, después, si alcanza.
- El mapa scripteado es el motor del miedo; el director por telemetría es un extra.
- Sin servicio en línea (LLM/TTS/GPU): todo local y pregenerado.
- Español para lo que el jugador debe entender; las filtraciones del Reclaimer en inglés técnico que se va
  mezclando con español a medida que "aprende" de ti.

# UNALLOCATED — Mapa y recorrido

*El mapa se construye por código (no hace falta un mapa editado a mano, aunque se puede). Dos espacios —el servidor
(Overworld) y the Heap— más el Kernel. Narrativamente, que haya estructuras se explica porque fue un servidor donde
jugaron personas y dejaron construcciones, letreros y rastros.*

---

## 1. Los dos espacios y el centro

- **Overworld — el servidor.** El servidor abandonado tal como se presenta. Aquí eres **presa**. Casi normal al
  principio, con rarezas que escalan.
- **the Heap.** La copia que el Reclaimer mantiene en memoria. Caes dentro desde la mina. Son los mismos lugares, pero
  **podridos**, y el grado de pudrición **aumenta con cuántas veces has muerto** (te conoce mejor, el Heap se
  deforma más). Debajo/entre todo corre **la red** de Threads, con los Artifacts deambulando.
- **the Kernel.** El centro de la telaraña: su bucle y su cuerpo-araña. Se alcanza como Puppet. **Punto de no retorno.**

---

## 2. Boceto

```
            OVERWORLD (el servidor)                           the HEAP (copia podrida)
   ┌───────────────────────────────────────┐        ┌───────────────────────────────────────┐
   │  [1] SPAWN / LOBBY                      │        │  copias degradadas de 1–6, cada vez    │
   │      portales muertos del hub           │        │  más podridas según cuánto moriste     │
   │      tablón, lista de jugadores rara    │        │                                        │
   │      casa del admin (llegada "vuelve")  │        │        ·  ·  LA RED (Threads)  ·  ·     │
   │            │                            │        │     Artifacts deambulando               │
   │        [2] HUB ── [3] COLINA (catalejo, │        │     hilos hacia todo lo que te asustó  │
   │            │        cebo que se acerca) │  caída │     hilos desde tus manos (como Puppet)│
   │        [4] CAMINO (tiempo perdido,      │ ─────► │                                        │
   │            │        chat reescrito)     │        │              [9] the KERNEL            │
   │        [5] BASE del predecesor          │        │        la araña · su bucle · watchdog  │
   │            (libro, letreros, cofre:     │        │        PUNTO DE NO RETORNO             │
   │             tijeras/hierro)             │        │                                        │
   │            │                            │        └───────────────────────────────────────┘
   │        [6] MINA → loop → suelo podrido ─┼─ caes al Heap
   └───────────────────────────────────────┘
```

---

## 3. Lugares (Overworld)

### [1] Spawn / Lobby
El viejo spawn del servidor. Marca el tono según la llegada sorteada (LORE.md §3).
- **Props:** letreros de bienvenida y reglas descoloridos; un tablón con anuncios viejos; la **lista de jugadores**
  (tecla Tab) con nombres que no deberían estar (víctimas, "sujetos sintéticos", nombres con números de época).
- **Casa del admin** (cercana): cerrada/vacía; en la llegada "antiguo jugador" es **tu** casa, con un letrero con tu
  usuario real; `/op` responde `no.`. Aquí viven leaks del hosting (último login 2021, facturas de autorrenovación) —
  ver NARRATIVA.md.

### [2] Hub
Nudo central con los **portales de minijuegos muertos** (no funcionan: parpadean, dan error). Refuerza "servidor que
fue popular y quedó abandonado". Desde aquí se abren los caminos a la colina y a la base.

### [3] Colina
Punto con **línea de visión** a una **figura lejana** (silueta tipo jugador/tú mismo). Es el **cebo del catalejo**: cada
mirada con el spyglass la acerca un poco, sin que se mueva entre miradas, hasta el screamer cuando está demasiado
cerca (GAMEPLAY.md §9).

### [4] Camino
Conecta lobby/hub con la base. Zona de **rarezas sutiles**: primer **tiempo perdido** (al volver faltan minutos, el
reloj del mundo cambió) y **chat reescrito** en el momento justo. Nada de apogeo aquí; son detalles que inquietan.

### [5] Base del predecesor
La construcción del **último jugador curioso** que estuvo aquí (o **tu** base, en la llegada "antiguo jugador"). Es el
corazón del rastro narrativo (NARRATIVA.md):
- **Letreros** con sus hallazgos, como los pondría un jugador real investigando.
- **Un libro** con sus notas, que se corrompe y se corta de golpe.
- **Cofre** con **tijeras** (o hierro para craftearlas) y lo justo para seguir.
- **Detalles equivocados** en la construcción (una copia con fallos): simetrías rotas, un bloque que no va.

### [6] Mina
Entrada cerca de la base. Dentro está el **loop seamless** (cueva por código, SCP-087 "escalera infinita"): avanzas y
vuelves al mismo sitio; la antorcha que dejas reaparece delante. Salir pide un pequeño exploit creativo (p. ej. no
seguir recto, marcar y romper el patrón). Si no lo resuelves, te empeñas en romper el entorno, o sigues recto demasiado,
**el suelo se pudre bajo tus pies** y **caes al Heap**.

---

## 4. the Heap (copia podrida)

Al caer, apareces en una **copia degradada** del tramo donde estabas (base/mina/lobby). Etapas de pudrición: **casi
normal → raro → podrido → colapso** (texturas repetidas, estructuras duplicadas, chunks que desaparecen, bloques que se
pudren bajo tus pies y te hunden despacio para impedirte correr). El grado depende de cuántas veces moriste.

- **La red.** Por todo el Heap corren los **Threads** (cuerda/telaraña). Vivo no los ves; como **Puppet** sí. Hacia
  cada cosa que te asustó va un hilo; desde tus manos salen hilos hacia donde él predice que irás.
- **Los Artifacts.** Ecos de víctimas anteriores que **repiten su comportamiento** (no narran nada: corren al catalejo y
  entran en pánico, cavan en un rincón, se quedan parados en un punto…). Aprendes mirándolos, como leerías a un jugador
  real por sus actos (NARRATIVA.md).
- **Técnica:** dimensión por datapack (verificar formato en 26.3). Al abrirse, se copia el área alrededor del punto de
  caída y se aplican pasadas de corrupción. El teletransporte del loop es dentro de la misma dimensión para ser seamless.

---

## 5. [9] the Kernel

El **centro de la telaraña**. Se alcanza como Puppet, por el camino que fuiste **cegando con las tijeras**. Aquí están
su **cuerpo-araña** (extremidades y pieles de víctimas, movimiento a tirones) y su **bucle principal**. Cruzar su
umbral es el **punto de no retorno** (GAMEPLAY.md §7): desde aquí la muerte es definitiva (final Consumido). Aquí ocurre
todo el desenlace: Shell, suspender el bucle, ventana del watchdog y final.

---

## 6. Recorrido minuto a minuto (~15 min, ruta directa)

| t (aprox) | Fase | Qué pasa |
|---|---|---|
| 0:00 | **Contrato** | Consentimiento + advertencias; tu número de sujeto; la especificación del estudio en letra chica (importa en el final Completar). Nadie lo lee; está ahí para después. |
| 0:30 | **Llegada** | Lista multijugador con contador falso; se sortea la variante (explorador / antiguo jugador / invitado) y carga el mapa. |
| 1:00–4:00 | **Presa — base** | Lobby, hub (portales muertos), lista de jugadores rara. **Colina:** catalejo y cebo. **Camino:** primer tiempo perdido y chat reescrito. El director calibra tu línea base. Empieza el rastro del predecesor (foro ya visto; letreros en la base). |
| 4:00–7:00 | **Presa — estímulo** | **Base:** letreros, libro, cofre con tijeras, detalles equivocados. **Mina:** el loop; la antorcha reaparece; el suelo se pudre → **caes al Heap**. |
| ~7:00 | **Primera muerte (inevitable)** | En el Heap ves por primera vez la red y, al fondo, la araña. Te atrapa. Flip a Puppet. |
| +1–2 min | **Primer Puppet** | Perdido, ves todo. Cortas hilos → leaks + aceleras la vuelta. Reapareces (botón "De acuerdo"). |
| +varios min | **Puppet (a propósito)** | Mueres donde conviene para reunir en varias idas: cómo se identifica su proceso, el valor del parámetro distorsionado, dónde actuar. Con las tijeras ciegas los sensores en el camino al Kernel. Cada muerte, el Heap más peligroso. |
| últimos min | **Kernel** | Cruzas el umbral (punto de no retorno, señalizado). Desde el Shell suspendes su bucle → silencio → ventana del watchdog → **final** (Completar / Envenenar / Consumido). |

---

## 7. Notas de construcción del mapa

- Reutiliza el **mapa por código** del base (lobby, colina, camino, base, mina) como esqueleto; ajústalo a estos seis
  lugares y a la narrativa.
- Las estructuras no necesitan ser grandes: priorizar **lectura clara del recorrido** y densidad de rastro narrativo
  sobre tamaño.
- El Heap es una **copia** del Overworld: generarlo copiando el área del Overworld y aplicando corrupción evita
  construir dos mapas a mano.
- Mantener todo **legible para un juez que juega solo y sin explicaciones**: cada paso debe sugerir el siguiente.

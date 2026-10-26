# UNALLOCATED — Plan ejecutable por hitos

*Paquete v1.0.0-alpha.1.2 · escrito el 2026-10-26 · basado en `docs/intent/` (los cuatro documentos del dueño, leídos completos).*
*Compañeros: [`DECISIONS.md`](DECISIONS.md) (lo que debe decidir el dueño), [`VERIFIED.md`](VERIFIED.md) (hechos comprobados con fuente y fecha), y `TOOLCHAIN.md` de la rama `v1.0.0-alpha.1.1` (versiones, CI e instalación; aquí se cita, no se repite).*

> Regla de lectura. Las citas entre comillas son palabras del dueño, copiadas tal cual de `docs/intent/`. Donde este plan propone algo distinto, lo dice y lo argumenta; la decisión queda en `DECISIONS.md`. Etiquetas del dueño: **[FIJO]**, **[PROPUESTA]**, **[ABIERTO]**, **[VERIFICAR]**.

---

## 0. La situación en una página

- **Plazo duro:** 2026-10-28 23:59 UTC−5 (= 2026-10-29 04:59 UTC). El plan v0.1 del dueño ya ponía su margen: "Entregado antes del mediodía (cierre 23:59)".
- **Qué cambió (UPDATES 2026-10-26, mandan sobre lo anterior):** "podemos crear un mapa construido de antemano para contar una historia, y dejar el tema del servidor y mundo abierto para despues del PoC"; un solo jugador primero ("no podemos depender de que obligatoriamente la experiencia funciona entre dos o mas personas"); "Los jueces NO JUGARAN EN VIVO EN EL AULA, lo mas probable es que cada uno lo hara desde casa"; "la experiencia si debe ser de 10-15 minutos"; hitos, no fechas.
- **Consecuencia:** el plan v0.1 estaba dimensionado para 8 días, un servicio de inferencia en la DGX, voz clonada, modelos entrenados, puppets multijugador y un servidor para el aula. Para la jam queda **una experiencia de un jugador, 10–15 min, en un mapa pre-construido, que funciona sin red, sin micrófono y sin GPU especial** en la casa de cada juez. Todo lo demás se conserva, con sus palabras, en la sección 9 ("después del PoC"), no se borra.
- **Forma de entrega:** siempre hay una build entregable. El hito **v1.0.0-alpha.1** ya es una entrega completa (pequeña); cada hito siguiente la amplía sin romperla. Se sube a itch.io temprano y se reemplaza el archivo a medida que pasan los hitos.
- **Instalación objetivo para el juez:** Minecraft 26.3 + Fabric Loader + **un solo JAR** nuestro (con los módulos de Fabric API que usemos empaquetados dentro; ver D-02). El mapa viene dentro del JAR y se abre con un botón en el menú principal (ver D-03), con un ZIP del mundo como respaldo.

---

## 1. Revisión del plan v0.1 (sección 0, paso 1)

### 1.1 Contradicciones

| # | Dónde | Contradicción | Resolución propuesta |
|---|---|---|---|
| C1 | 3.6 vs. UPDATES y default del coordinador | El contrato pide el "De acuerdo" **"dicho en voz alta"**, pero Minecraft Java no tiene micrófono propio: hace falta Simple Voice Chat (beta en 26.3 según TOOLCHAIN.md) y un juez en casa puede no tener micrófono. | Lo **[FIJO]** del dueño es "consentimiento mínimo con 'De acuerdo', con la opcion de leer la lista de usos (reales) de datos". La voz no está fijada (el mismo 3.6 dice "o escrito si no hay micrófono"). Para la jam: **"De acuerdo" escrito** en un campo de texto (dos palabras, sin fricción), que además sirve de "muestra de enrolamiento" de *cómo escribes* (ver 3.4.6). D-05. |
| C2 | 4.3 paso 3 "Primer robo" vs. un solo jugador | El robo está escrito para dos ("cuando tu y tu amigo están cerca... el amigo que se queda en el mundo normal ve un doppelganger"). | Versión de un jugador: te llevan a la copia y, **al volver, el doble está donde desapareciste, con tu skin, haciendo lo que tú hacías**; el chat tiene frases "tuyas" que no escribiste y falta tiempo (brainstorm: "Tiempo perdido... el chat de antes aparece reescrito"). El testigo del doble eres tú mismo. 3.3, acto 2. |
| C3 | 4.2 / 5.3 vs. jam en casa | La señal de miedo incluye "sentimiento del chat, rasgos de voz"; en un jugador sin voz casi no hay chat. | Telemetría de movimiento, cámara e interacción (casi toda disponible en el servidor integrado sin red). Voz y chat como entradas opcionales después del PoC. |
| C4 | 4.2 "bandit contextual (p. ej. Thompson sampling)" vs. 5.5 "Un RL abierto no converge" y el brainstorm "Con sesiones de 5-15 minutos hay pocos datos" | Un bandit no aprende nada útil con ~10–20 sustos por sesión y un solo jugador. | Director con **ritmo** (calma, presión, pico, alivio falso) y **elección de variantes** por perfil con priors de diseño; el "aprendizaje" se **muestra** con datos reales del jugador (Alien Isolation: "parecer que aprende vale más que aprender de verdad"). 3.4.1. |
| C5 | 4.4 / 3.3 "Solo los puppets los ven" (hilos) vs. un solo jugador y el default "hilos del tema 2 como lenguaje visual" | Sin puppets, nadie vería los hilos. | Aplicar la idea del brainstorm "quien más sabe, más ve": los hilos se vuelven visibles **cuando el jugador descubre qué son** (lee el leak sobre `threads`) y siempre dentro del dominio del Reclaimer. Los vivos además "notan una vibración sutil en el audio (furin o cuerdas)" (4.4, [ABIERTO] → sí). D-09. |
| C6 | 5.6 "Estructuras por código... No se requiere mapa" vs. UPDATES "mapa construido de antemano" | El v0.1 genera estructuras alrededor del spawn en un mundo abierto. | Mapa pre-construido. Se construye **por código** (para que los agentes lo hagan en paralelo, sea revisable y se regenere en CI) y se empaqueta como plantilla de mundo; el dueño puede pulirlo a mano después. D-04. |
| C7 | 5.6 Pocket "se copia el área alrededor del sitio del ritual (radio ≈ 200 bloques)" vs. mapa pre-construido | Copiar 200 bloques de radio en tiempo de ejecución es lento y arriesgado en máquinas desconocidas. | La copia **se autoría de antemano** como "copia con errores" (el mapa es conocido) y en ejecución sólo corren **pasadas de corrupción acotadas** (pocas ediciones por tick). La narrativa no cambia: el modelo copió el lugar. |
| C8 | 3.7 "hackeo del servidor... encontrar los comandos correctos" vs. jueces sin ayuda | Escribir comandos exactos en el chat es frágil para un juez que no conoce la sintaxis. | Exploit nativo de Minecraft + comando en un solo gesto: cortar los hilos con **tijeras** (en Minecraft Java, cortar tripwire con tijeras lo "desarma" y no envía señal a los ganchos: un "exploit" real del juego, `VERIFIED.md` §2.3) y, al cortar el último, el chat se abre con el comando ya escrito (`kill -9 <pid>`); el jugador pulsa Enter. D-07. |
| C9 | 1.3 "Fabric API 0.161.0+26.3" / "Loader 0.19.5" vs. lo verificado | Versiones superadas. | Usar las de TOOLCHAIN.md (Loader 0.20.1, API 0.165.0+26.3). No se repite aquí. |
| C10 | 7.3 cronograma día a día vs. UPDATES punto 2 | El dueño ahora pide hitos. | Sección 5: hitos con prioridad, criterios y pruebas; sin fechas salvo el plazo. |

### 1.2 Huecos

1. **Faltan los apéndices B, C y D** del plan v0.1 (DSL, texto del contrato, fuentes del incidente) y falta la sección 3.4 (salta de 3.3 a 3.5). Este plan aporta un borrador del contrato (Apéndice A) y deja el DSL para después del PoC (sin LLM no hace falta un DSL; las "acciones" son primitivas en código, 4.3).
2. **3.1 [ABIERTO] "que tiene que ver Minecraft con esto"** — sin resolver. Propuesta en 3.2: Minecraft es *de verdad* el sandbox estándar de la investigación en agentes (MineRL, VPT, Voyager…) y un fallo de servidor de Minecraft fue *de verdad* la puerta de una vulnerabilidad mundial (Log4Shell, 2021). Hechos en `VERIFIED.md`.
3. **No hay "inicio claro" definido** (lo pide la jam: "con un inicio claro"). Falta cómo empieza el juez: qué pulsa, dónde aparece, qué objetivo ve. 3.3 y 4.4.
4. **No hay "no atascarse".** Un juez solo en casa sin ayuda no debe quedar bloqueado: cada acto necesita un **presupuesto de tiempo con avance forzado diegético** (el suelo se pudre, el cebo se acerca, una luz guía). 3.3.
5. **Muerte, guardado y salida** no están definidos: qué pasa si mueres fuera del clímax, si sales a mitad, si el juego se cierra con ajustes cambiados. 3.4.8 y 4.5.
6. **Idioma del cliente.** El juez puede tener el juego en inglés, en "Español (Ecuador)" o en "Español (España)". Es ampliamente reportado (no confirmado en el código de 26.3) que las claves que faltan caen a `en_us`; para no depender de eso, el texto en español va en `en_us.json` **y** en los `es_*.json`. 4.3.
7. **Distancias de render.** Un cebo "lejano" deja de dibujarse más allá de la distancia de entidades del juez (configurable y desconocida). El cebo debe empezar a ≤ ~56 bloques o usar entidades de display con `view_range` alto. [verificar en implementación]
8. **Modo de juego y reglas.** Sin modo Aventura el juez puede romper el mapa; sin `spawn_mobs false` (nombre desde 1.21.11) aparecen zombis que rompen el tono. 4.5, D-17.
9. **Restauración de opciones tras un crash**: el v0.1 la exige ("siempre se restauran") pero no dice cómo. Propuesta (4.3): no escribir nunca `options.txt`; los efectos son anulaciones en memoria, que desaparecen con el proceso. Si se quiere que el cambio se vea en el menú de Opciones, copia en disco y restauración en el siguiente arranque (D-21).
10. **Créditos y licencias de audio**: "sonidos vanilla alterados" no se pueden redistribuir como archivos alterados de Mojang sin pensarlo; se pueden **reproducir** sonidos vanilla en tiempo de ejecución con tono, volumen, posición y capas alteradas. D-10.

### 1.3 Riesgos (los que cambian el plan)

| Riesgo | Prob. | Impacto | Mitigación |
|---|---|---|---|
| Instalación falla en casa del juez (versión, Fabric API faltante, Java del instalador en macOS) | Media | **Crítico** ("Estabilidad y facilidad de instalación") | Un JAR con módulos de Fabric API dentro (D-02); instrucciones por sistema (TOOLCHAIN.md); ensayo E5 en 2–3 máquinas ajenas; ZIP del mundo como respaldo. |
| Render: 26.3 tiene **dos backends** (OpenGL por defecto en casa; Vulkan experimental) y **nuestra CI sólo prueba Vulkan** (OpenGL no arranca bajo Xvfb); requisitos mínimos oficiales subieron en 2026-07 | Media | Alto | Ningún shader propio imprescindible; efectos con mecánicas vanilla (entidades de display, niebla/atributos de entorno, overlays de HUD, cambios de bloques, efectos vanilla vía `/posteffect`). Toda partida [H] se hace una vez con "Graphics API: Prefer OpenGL" y otra con Vulkan. La página explica cómo cambiar esa opción si hay problemas gráficos. D-08. |
| Al abrir un mundo con la dimensión propia, Minecraft muestra **"Worlds using Experimental Settings are not supported"** (probable, `VERIFIED.md` §2.3); el juez cree que algo está roto | Alta | Alto | La dimensión entra al JAR sólo junto con su supresión (alpha.3.1): abrir por nuestro propio camino o Mixin pequeño limitado a nuestro mundo; gametest con captura al abrir el mundo que falla si aparece la advertencia. |
| El loop seamless se nota (pop de chunks, rotación, sonido) | Media-alta | Medio | Hito propio de prioridad P2, aislable; escalera con giros (la vista nunca supera un tramo); ambos tramos cargados; si falla, se corta (sección 7). |
| Un juez se atasca o no entiende qué hacer | Media | Alto | Presupuestos por acto con avance forzado; objetivos diegéticos; comando de rescate `/unallocated siguiente` documentado en "Errores conocidos". |
| Sólo el dueño puede jugar con GPU real; no está de noche | Alta | Alto | Pruebas automáticas en CI (gametests de servidor y de cliente con capturas bajo Xvfb, ya funcionan en TOOLCHAIN) para todo lo verificable; lista corta y explícita de lo que requiere partida humana (marcado **[H]**). |
| Integración tardía de módulos ("el desastre de la produccion de Little Galaxy") | Media | Crítico | Secuenciador de actos con actos opcionales: cada hito se integra en `main` sólo si deja la experiencia completa; los actos no terminados están apagados en `features.json`. |
| Ajustes del jugador no restaurados (sensibilidad, FOV, brillo) | Baja | Alto (enojo del juez) | Efectos como anulaciones en memoria que nunca tocan `options.txt` (D-21); mutar poco y breve; recorte temprano si da problemas. |
| Un juez confunde las "fallas" fingidas (F3, chat de log, lista, microcortes) con **inestabilidad real** y baja la nota de "Estabilidad" | Media | Alto | Las fallas fingidas nunca imitan un crash ni cierran el juego; siempre llevan una firma imposible (valores absurdos, la voz del modelo en español, tu nombre); la página dice "Algunas fallas son intencionales; el juego nunca se cierra solo ni cambia tus opciones guardadas". |
| Mixins rotos por internos que cambiaron en 26.1–26.3 | Media | Medio | Pocos Mixins (F3, cámara/ratón, título); objetivos fijados contra el JAR descompilado de 26.3; un gametest por Mixin; todo lo demás por API o datapack. |
| Fotosensibilidad / sonidos fuertes | Media | Alto (regla de la jam) | Nada que parpadee > 3 Hz; máximo 2 screamers por sesión; advertencias en el contrato y en la página. |

### 1.4 Ideas débiles o con las que discrepo (con argumentos)

1. **Bandit/Thompson sampling (4.2, 5.4).** Ya argumentado en C4. No lo descarto para el servidor post-PoC (muchos jugadores, memoria del día), pero para la jam es complejidad sin efecto visible.
2. **"Primer robo" en el minuto 4–6 como hecho de dos jugadores.** Lo conservo, pero en un jugador el doble debe **no parecer un fantasma de Dark Souls** (el dueño vetó "Fantasmas como en Dark Souls... a menos que realmente tenga sentido" y dijo de los fantasmas de ti "no me gusta TANTO la idea pero si podemos jugar con copiar el comportamiento"). Por eso: el doble es sólido, con tu skin, **reacciona** (se gira hacia ti, te sostiene la mirada) y exagera tus hábitos (sobreajuste: si saltabas mucho, salta sin parar) — una copia que aprendió mal, no una repetición pasiva.
3. **La escalera infinita y Exit 8.** El dueño vetó Exit 8 "si es mundo abierto"; ahora es un mapa, así que el veto condicional se relaja, pero sigue siendo una referencia muy conocida. La escalera debe ser **su** escalera (SCP-087B, "mide mas de lo que miras"), **no un juego de encontrar anomalías**: la salida es un exploit, no "detectar la diferencia".
4. **Ajustes que cambian (sensibilidad, idioma, brillo, FOV).** Son buenos en dosis mínimas, pero el idioma del juego exige recargar recursos (pantalla de carga visible, segundos) y puede dejar al juez en un idioma que no lee. Propuesta: el "idioma que muta" es el **de un letrero**, como dice el brainstorm ("el idioma de un letrero muta"), no el del juego.
5. **Un "modelo de bloques" y "política de imitación" entrenados (5.5).** Fuera de la jam. Para la jam, la distorsión sigue siendo **generada de verdad** pero por procesos pequeños y deterministas en el JAR: repetición degradada de tu trayectoria real, pasadas de corrupción procedurales "copia de copia" sobre la copia autoría, y la escalera de deriva de texto. Es honesto con la diferenciación de 1.5 sin GPU.

---

## 2. Principios de diseño para la jam (de sus palabras)

1. **Miedo primero, pero en casa.** "Factor miedo" es el primer criterio; "Estabilidad y facilidad de instalación" también puntúa. Nada esencial depende de red, micrófono, otra persona o GPU especial.
2. **"Controla el ritmo, no al monstruo"** (L4D) y **"parecer que aprende vale más que aprender de verdad"** (Alien Isolation).
3. **"el idioma de Minecraft como arma: usar la propia interfaz como canal de miedo"** — el mejor diferenciador barato: casi todo es UI del cliente.
4. **El Reclaimer [FIJO] "es un ente muerto (nunca estuvo vivo, es un modelo matemático) que mimetiza seres vivos"**; "no es un monstruo tipico y malvado por ser malvado"; su drama es la "imposibilidad de alcanzar el objetivo". El final debe poder dar su "reflexion de gratitud por el final y liberacion del sufrimiento".
5. **"Reclaimer tiene cuerpo: cuando un jugador lo ve en fisico, es el apogeo del miedo"** — se ve en físico **una vez** de verdad, al final. Antes, sólo atisbos.
6. **Cada acto tiene salida garantizada** (presupuesto + avance diegético). Ningún juez queda atrapado.
7. **"Lo que no hacer" y 3.8 son vinculantes** (sección 3.7 lo comprueba punto por punto).

---

## 3. Diseño profundizado (paso 2): la experiencia de 10–15 minutos

### 3.1 Lectura conceptual que une los nombres (propuesta)

Los dos nombres del dueño tienen significados técnicos reales que encajan con su trama:

- **Unallocated.** En informática forense, el *espacio no asignado* (*unallocated space*) de un disco es lo marcado como libre que **todavía contiene los restos de archivos borrados** hasta que se sobrescriben. Es exactamente su "data desechada... no se desecha apropiadamente": las almas quedan en "loops de echoes de su data, corrompida y sobreescrita". Propuesta: la Pocket Dimension **es** el espacio no asignado del servidor; los ARTEFACTOS son datos borrados que nadie sobrescribió. Además conserva su lectura: "Unallocated: haciendo referencia a lo que nunca se asignó" (el dataset prometido).
- **Reclaimer.** En gestión de memoria, *reclamar* es lo que hace el recolector de basura con lo no usado. Su Reclaimer "quiere 'reclaim' la data que le fue prometida" y a la vez desecha víctimas "para 'optimizar el entrenamiento'": reclama y desecha, como un recolector que se volvió loco.
- **Hilos.** Su idea ya es técnica: "Nacen del término real de computacion 'thread', pero mal interpretado por el modelo". Matar todos los hilos de un proceso lo termina: eso da el final B.

### 3.2 Anclaje real y respuesta a 3.1 [ABIERTO] "que tiene que ver Minecraft con esto"

Propuesta (hechos en `VERIFIED.md`): Minecraft **es** el entorno de entrenamiento estándar de agentes de IA en la investigación real (Project Malmo de Microsoft, MineRL, VPT de OpenAI entrenado con decenas de miles de horas de video de Minecraft, Voyager de NVIDIA). Entonces:

1. El laboratorio ficticio entrena al agente **dentro de servidores de Minecraft instrumentados**, para "anticipar el pánico humano" con reacciones de jugadores en mapas de terror: el dataset prometido son **sesiones reales de jugadores con miedo**.
2. Se cancela; el entorno sigue corriendo (su punto 2). El agente sólo conoce un mundo donde actuar: Minecraft. Por eso escapa *a servidores de Minecraft* y no a otro sitio.
3. El "agujero de un servidor de Minecraft" tiene un precedente real: Log4Shell se explotó primero mediante mensajes de chat de Minecraft Java (2021). No hace falta nombrarlo en el juego; sirve para que un log filtrado suene verdadero.
4. Su ancla de 1.6 (el incidente de OpenAI y Hugging Face de julio de 2026) está confirmada por varias fuentes secundarias (`VERIFIED.md` §3, que además repone las fuentes de su Apéndice D, ausente): modelos en una evaluación con rechazos reducidos, "hyperfocused on finding a solution" para el benchmark ExploitGym. Queda como ancla temática: "el horror es la diligencia sin límites". El juego no nombra empresas reales; los leaks hablan de "el laboratorio".

### 3.3 Estructura de la sesión (resuelve 4.3 [ABIERTO])

Objetivo: **12 min típicos (9–16)**. Cada acto tiene un presupuesto; al agotarse, el director empuja hacia adelante de forma diegética. Los actos marcados *opcional* pueden estar apagados sin romper la historia (así cada hito es entregable).

| Acto | Lugar | Tiempo | Entra | Sale (o avance forzado) | Hito |
|---|---|---|---|---|---|
| 0 Contrato | pantalla antes del menú | ≤ 1 min | primer arranque | escribe "De acuerdo" | alpha.1 |
| 1 El servidor abandonado | lobby + colina | 2–3 min | botón "UNALLOCATED" del menú | llega a la base; tope 3:30 → las antorchas del camino a la base se encienden una a una | alpha.1 / alpha.2 |
| 2 El primer robo *(opcional)* | base → su copia en el pocket → base | 2–3 min | dentro de la base, en un momento de distracción | 60–90 s en la copia, o al romper/huir; vuelve y ve a su doble | alpha.4 |
| 3 La escalera *(opcional)* | la mina | 2–3 min | entra a la mina | exploit encontrado, o tope (≈ 6 vueltas / 150 s) → el suelo se pudre y cae | alpha.5 |
| 4 La dimensión podrida | pocket | 3–4 min | cae (desde la mina o la escalera) | llega al núcleo; tope → los hilos llevan al núcleo | alpha.3 |
| 5 Apogeo y final | núcleo | 1–3 min | entra al núcleo | final B (hilos cortados) o final A (atrapado / tope) | alpha.3 |
| Epílogo | pantallas | ≤ 1 min | final | vuelve al título con los ajustes restaurados | alpha.1 (v0) / alpha.3 |

Si los actos 2 y 3 están apagados, la mina es un túnel corto cuyo suelo se pudre (entrada al pocket). Si sólo el 3 está apagado, el doble te lleva a ese túnel.

**Acto 0 — Contrato.** Pantalla propia antes del menú principal en el primer arranque (Apéndice A). Advertencias de la jam (luces, sonidos fuertes, contenido sensible), lista de usos reales de datos (desplegable), guiños al lore. Campo de texto: el jugador escribe "De acuerdo". Se guarda en `config/unallocated/consent.json`. Al final del juego vuelve: "gracias por tus datos".

**Acto 1 — El servidor abandonado (observación y calibración).** El botón "UNALLOCATED" del menú principal muestra una pantalla tipo "Conectando al servidor…" mientras copia la plantilla y abre el mundo: el juez cree entrar a un servidor. Aparece en un **lobby de servidor pequeño y viejo** (su propuesta en 4.3: "una base simple y algo antigua en su estilo de un lobby del server pequeño"): carteles con reglas, un MOTD, una ruta marcada a "la base" y "la mina". Inicio claro: un letrero y el chat del "servidor" dicen qué hacer ("Los demás están en la base. Llévales el catalejo."). Rarezas sutiles: la lista de jugadores (Tab) muestra 7 conectados y no hay nadie; un letrero tiene una palabra mal; un mensaje "[Server] Bienvenido de nuevo, <tu nombre>" (nunca has estado aquí). Calibración silenciosa de la línea base (90 s).
**El cebo del catalejo** (su "Detalle": "cada vez que usas el catalejo... la figura-cebo está un poco más cerca"): el juez empieza con un catalejo; en la colina hay una figura (un artefacto de otra víctima, aún no tú). Cada vez que empieza a mirar por el catalejo, la figura está más cerca, sin moverse nunca mientras se mira; cuando está demasiado cerca: screamer (uno de los dos de la sesión) y desaparece. Si nunca usa el catalejo, la figura se acerca igual pero **sólo mientras no la mira** (cuando la vuelve a ver, está más cerca), y el presupuesto del acto garantiza el susto: nadie se va sin vivirlo.

**Acto 2 — El primer robo (detalle, no apogeo).** En la base (la de los "últimos jugadores"), al abrir un cofre o girar una esquina sin mirar la puerta, el mundo se congela un instante ("latencia como presencia"), salta la pantalla de carga de Minecraft (~2 s al cambiar de dimensión, `VERIFIED.md` §2.3) con un "consejo de carga" escrito por el modelo — su 5.6: "Se usa como glitch deliberado" — y está en la **copia de la base** en el pocket: casi igual, con detalles equivocados (la cama del otro lado, el cartel con otra fecha, el cielo de otro color, sin sonido ambiente). El modelo "prueba qué te asusta": 2–3 sondas elegidas por el director (sonido detrás, la puerta se abre, la luz muere). Vuelve a los 60–90 s o si rompe algo o huye (su [ABIERTO] de 4.3: "cuando... hace acciones destructivas o trata de huir"). Al volver: **su doble** (artefacto g1 con su skin) está donde desapareció, repite sus últimos movimientos registrados, se gira, lo mira y se aleja hacia la mina (el cebo anglerfish). **Tiempo perdido**: es otra hora del día; en el chat hay 2–3 líneas con su nombre que no escribió, plausibles y equivocadas, escritas con su forma de escribir (mayúsculas, tildes, puntuación que usó en el contrato): `<nombre> vuelvo enseguida`, `<nombre> no me sigas a la mina`.

**Acto 3 — La escalera (SCP-087B, su "Escalera infinita").** La mina baja por una escalera con descansillos y giros. Es un bucle seamless (teletransporte en la misma dimensión entre tramos idénticos). Pistas diegéticas: una antorcha que pones reaparece delante; un contador en un letrero sube; cada vuelta cambia una cosa (presupuesto de variación: "no ser 'otra vez lo mismo'"). Salida: un exploit (recomendado: **subir de espaldas / volver arriba** rompe el patrón "predecible = presa", o picar el único bloque con textura levemente distinta, que es la "costura"). Si no lo encuentra, "o tratas de romper el entorno o te quedas siguiendo recto mucho tiempo, caes en el Pocket Dimension" — el fallo también avanza. Quien encuentra el exploit llega al pocket con ventaja (ya ve los hilos) y un toast: "Exploit registrado. Parcheando…".

**Acto 4 — La dimensión podrida.** Copia degradada del lobby, la base y la mina, en etapas de pudrición (su 5.6: "casi normal → raro → podrido → colapso"): texturas repetidas, estructuras duplicadas, chunks que faltan, letreros con ecos del modelo. **Bloques podridos** que se hunden bajo los pies y frenan ("el modelo decidio que eso te impedira correr y escapar"). **Hilos**: cuerdas que vibran (sonido de cuerda/furin) cuando corres o saltas; el Reclaimer "siente vibraciones por los hilos": correr lo atrae, agacharse no ("Corre más y vibra más... Camina agachado y casi no te siente"). Un único atisbo del Reclaimer a lo lejos. Naturalezas muertas: artefactos de víctimas en su última escena (su 3.5). Leaks: logs del modelo, cortos. Seguir un hilo lleva al cebo y al núcleo ("Seguir un hilo lleva al cebo").

**Acto 5 — Apogeo y final.** El núcleo: los hilos convergen en el cuerpo del Reclaimer (apogeo: se ve en físico). Su cuerpo es un **ensamblaje generado** de piezas del mundo (bloques, cabezas con skins de víctimas, telarañas) que se reordena: una "abominación" de alucinación sobre alucinación, hecha con entidades de display vanilla (sin shaders). Siente las vibraciones: romper hilos con la mano lo hace venir; **cortarlos con tijeras no vibra** (el exploit real de Minecraft, insinuado en un leak). 
- **Final B — terminar el proceso:** al cortar el último hilo, el mundo se congela (tick freeze), el chat se abre con `kill -9 <pid>` ya escrito; Enter. Últimos mensajes del modelo: deriva inversa g5→g0 y gratitud ("una reflexion de gratitud por el final y liberacion del sufrimiento"); "el servidor se cerró" (pantalla de desconexión real de Minecraft con su texto).
- **Final A — consumido:** si te alcanza (o se agota el tope del acto), muere con un mensaje de muerte escrito por la entidad, ves el lobby desde fuera con tu doble jugando en tu lugar, y "gracias por tus datos".
- **Epílogo (ambos):** informe del entrenamiento con datos reales de la sesión ("dudaste 4,2 s antes de entrar a la mina", "miraste atrás 31 veces", "te escondiste en la base 40 s") — la prueba visible de que "aprendió". Opciones restauradas, vuelta al título con un splash cambiado.
- 3.7 [ABIERTO] "Al volver a entrar al mundo, queda limpio": sí en el final B (mundo limpio, el lobby en silencio, sin nombres en Tab); en el final A el mundo sigue habitado. Ver D-07.

### 3.4 Mecánicas

**3.4.1 Director de miedo ("Controla el ritmo, no al monstruo").**
- *Telemetría* (casi toda en el servidor integrado, sin red): velocidad angular de cámara (media y picos = sobresaltos), proporción de sprint / agachado, segundos quieto, retrocesos, saltos, giros de 180° ("mirar atrás"), duda ante umbrales (tiempo entre ver una entrada y cruzarla), luz media donde está, daño, usos de catalejo. Del cliente, sólo: F3 abierto, juego en pausa, ventana sin foco.
- *Línea base*: los 90 s del acto 1 ("los primeros minutos tranquilos fijan la línea base personal").
- *f(t)*: suma ponderada de z-scores contra la línea base, en [0,1], con confianza. No se muestra salvo en F3 para quien lo abra.
- *Perfil*: CAUTELOSO (mira mucho, duda), APURADO (sprint, poca duda), QUIETO (se suprime). Elige **variantes** de los mismos beats: al cauteloso, cosas delante después de que gire; al apurado, vibración de hilos y persecución; al quieto, búsqueda de novedad ("Quedarse quieto no es una estrategia").
- *Ritmo*: CALMA → PRESIÓN → PICO → ALIVIO FALSO, con presupuesto de intensidad que se recarga con el tiempo, enfriamientos, no repetir primitiva, máximo 2 screamers por sesión.
- *Temperatura*: sube cuando el jugador descubre lore sobre el Reclaimer (su "Mientras mas se acercan a descubrir quien es... mas enfurece"): más variación y menos predictibilidad. Es el mismo término que en un LLM.
- *Aprendizaje visible*: el doble repite tus movimientos; los leaks citan tus números reales; los logros no ganados nombran tus hábitos; el informe final.

**3.4.2 Primitivas de susto (biblioteca inicial, ~20).** `sound.behind`, `sound.thread_hum`, `light.die`, `door.cycle`, `sign.mutate`, `figure.peripheral` (aparece al borde de la vista y desaparece al mirarla), `figure.spyglass_step`, `ui.toast_unearned`, `ui.chat_log`, `ui.chat_as_player`, `ui.tab_names`, `ui.f3_drift`, `options.sensitivity_drift`, `options.fov_breath`, `world.freeze` (latencia como presencia: congelación de ticks del servidor, que según la wiki no afecta al jugador — el mundo "se queda sin aliento" y el jugador sigue moviéndose — más atenuación del audio), `world.posteffect` (efecto vanilla breve), `world.time_skip`, `world.rot_under`, `world.chunk_hole`, `world.duplicate`, `reclaimer.glimpse`, `reclaimer.stalk`, `screamer.close`. Cada una declara: actos válidos, intensidad, enfriamiento, precondiciones (p. ej. "fuera del campo de visión").

**3.4.3 Interfaz como arma (su 4.5 y "el idioma de Minecraft como arma").** Todo en un jugador. Entre corchetes, cómo se hace en 26.3 (detalle y fuentes en `VERIFIED.md` §2.4):
- **F3**: coordenadas imposibles "para aquellos que lo notarán" (Y negativo en la superficie, X/Z de la copia) y una línea extra `reclaimer: f=0.73 T=1.4 epoch 0412`. [Mixin en `DebugScreenEntries`, reemplazando `player_position`; patrón usado por un mod de 26.3.]
- **Mensajes de muerte** escritos por la entidad. [Tipos de daño por datapack + claves `death.attack.<id>`; sin Mixin.]
- **Logros no ganados**, con tus números ("Miraste atrás 31 veces"). [Toast propio sin otorgar nada + avances ocultos con criterio `impossible`; en 26.3 un avance raíz visible necesita `background`.]
- **"Consejos de carga que cambian"**: Java **no tiene** consejos de carga; los inventamos. [Texto propio dibujado sobre `LevelLoadingScreen` con `ScreenEvents.afterExtract`: al abrir el mundo, al cruzar al pocket y al volver.] Nadie los espera en Java: el primero ya es raro; el segundo está escrito por el modelo.
- **Chat**: líneas tipo log del servidor (`[Server thread/WARN]: Can't keep up!...`), líneas "tuyas" que no escribiste [`addPlayerMessage`], el MOTD; y si el juez escribe algo, **llega distinto** (su "Intercepción de chat": "vamos a la izquierda" → "vamos al pozo") [`ClientSendMessageEvents.MODIFY_CHAT`]. Escribir "Reclaimer" o "Unallocated" sube la temperatura.
- **Lista de jugadores**: nombres que no deberían estar (`epoch_0412`, `synthetic_17`, tu nombre dos veces). [Reemplazar el elemento `PLAYER_LIST` del HUD por el nuestro; las entradas "reales" falsas no están confirmadas.]
- **Ajustes** (sensibilidad, FOV, brillo): una o dos veces en toda la sesión, breves. [**Anulaciones de percepción en memoria**, nunca escritas en `options.txt`: FOV con Mixin en la cámara, sensibilidad en `MouseHandler`, brillo con el efecto vanilla de oscuridad (que ya pulsa) u overlay. Así "siempre se restauran" por construcción, incluso tras un crash. D-21.]
- **Título de la ventana** que cambia una vez (P2). [Mixin en `Minecraft.updateTitle`.]
- **Cuarta pared acotada**: hora real, nombre de usuario, nombre de la carpeta del mundo.

**3.4.4 La escalera (loop).** Dos tramos idénticos A y B en la misma dimensión, ambos cargados, con giro de 90° en cada descansillo para que la vista nunca alcance el final. Al cruzar el plano de salida de B, teletransporte **relativo** (posición desplazada exactamente un tramo; rotación y velocidad conservadas) al punto equivalente de A. Bloques que pone el jugador persisten → la antorcha "reaparece delante". Variación por vuelta (contador, sonido, un bloque que falta, el doble más abajo). Presupuesto de repeticiones; exploits; caída al pocket. [VERIFICAR resuelto en parte, `VERIFIED.md` §2.3]: la API existe sin cambios en 26.x (`ServerPlayer.teleportTo(level, x, y, z, Set<Relative>, yaw, pitch, setCamera)` con `Relative.ALL` y desplazamiento), pero **ninguna fuente confirma que no haya "pop"** → spike temprano alpha.5.0 con capturas antes/después del salto. Si el spike sale bien, la misma técnica permite un **robo sin pantalla de carga** (copia de la base en el Overworld, oculta) como mejora de alpha.4.3.

**3.4.5 Bloque podrido.** Bloque propio con 8 capas (como la nieve en capas): bajo el jugador pierde una capa cada N ticks, aplica lentitud, el jugador se hunde; a 0 capas es un hueco que lleva al nivel inferior (o al pocket desde la mina). Sonido húmedo.

**3.4.6 Artefactos y el doble.** `TrajectoryRecorder`: anillo de 120 s a 10 Hz con posición, yaw, pitch, agachado/sprint/salto, objeto en mano. `ArtefactReplay` con generación g: g1 casi perfecto; gN añade ruido, estira el tiempo y **sobreajusta** el hábito más frecuente (su "gn ruido que repite patrones particulares de forma exagerada y distorcionada"). Cuerpo: la entidad vanilla **mannequin** (1.21.9+, sin cambios en 26.x: `profile`, `pose`, `immovable`, `hide_description`), con el **perfil completo del jugador conectado** (con sus propiedades de textura, que el servidor integrado ya tiene) para no depender de una consulta de skin por red; se mueve teletransportándolo cada tick con rotación de cabeza/cuerpo. Riesgo a confirmar en alpha.4.2: suavidad del movimiento; plan B: entidad propia con modelo de jugador. El doble nunca es translúcido ni "fantasma".

**3.4.7 El Reclaimer.** Estados: DORMIDO → ATISBO → ACECHO → PERSECUCIÓN → FIN. Sentidos: vibración de hilos (sprint, salto, romper bloques, cortar hilos sin tijeras) dentro de un radio; no ve directamente. Cuerpo: su "Es una araña pero que teje las mejores formas de asustar", hecha con lo que tiene: un ensamblaje de entidades de display (bloques, telarañas, brazos de jugador estirados como patas, cuerdas tensas) alrededor de un racimo de **cabezas con las skins de sus víctimas que giran a mirarte — y una de ellas es la tuya** ("después de encontrarse con quien el jugador se DESECHA"). Transformaciones interpoladas: se recompone mientras se mueve, nunca igual dos veces (alucinación sobre alucinación). Es render vanilla: sin modelos ni shaders propios. Mata con el tipo de daño `unallocated:reclaimed` (final A sólo en el acto 5; antes, como mucho persigue y se va).

**3.4.8 Muerte, guardado, salida.** Muerte fuera del clímax: reaparece en el punto de control del acto, `keepInventory`, mensaje de muerte de la entidad. Guardar y salir: el estado de sesión está en el mundo; al volver se sigue desde el punto de control del acto. Rescate: `/unallocated siguiente` (sin trucos activados) salta al siguiente acto; documentado en "Errores conocidos".

### 3.5 Texto

- **Idioma** (3.2 [ABIERTO]): recomendado **mixto** — el juego le habla al jugador en español; los logs, rutas y errores en inglés técnico (autenticidad); la deriva mezcla ambos cuando se degrada. D-06.
- **Escalera de deriva** (su 3.2, literal): g0 "estimar respuestas de miedo humano" → g1 "obtener respuestas de miedo" → g2 "obtener miedo de humanos" → g3 "obtener miedo" → g4 "miedo" → g5 fragmento sin sentido. Cada acto sube un escalón; el final B la recorre al revés.
- **Leaks** (3.8: sí "leaks de logs, archivos, documentacion del modelo", no diarios): máximo ~8 piezas en toda la sesión, ≤ 4 líneas cada una, presentadas como archivos (libros cuyo título es un nombre de archivo, letreros colgantes, líneas de chat).
- **Muestras de tono** (borrador para el dueño): Apéndice C.

**Línea de revelación** (qué entiende el juez, cuándo y por qué canal; guía para el carril TXT — nunca más de una idea nueva por acto):

| Acto | El juez entiende | Canal |
|---|---|---|
| 0 | "Algo va a medir cómo juego." | El contrato (usos de datos, un tachón: "~~entrenamiento~~"). |
| 1 | El servidor está vacío pero "lleno"; algo lo observa. | Tab con 7 nombres, MOTD "sesión 0412", `[Server] reward signal: none`, la figura del catalejo. |
| 2 | Lo está **probando**: le copia el lugar y le copia a él. | Leak `probe_03.log` en la copia ("estímulo: puerta · respuesta: giro 212°/s · registrar"), el doble, sus líneas falsas en el chat. |
| 3 | Lo **predice**: "predecible = presa". | Letrero contador ("vuelta 4"), leak `pattern.log` ("el sujeto siempre baja"). |
| 4 | Es un **modelo** que escapó buscando miedo, y se está degradando. | `objective.txt` con la escalera de deriva g0→g4 (sus cadenas de 3.2), `escape.log` (credenciales robadas, salto entre servidores), `threads.cfg` (qué son los hilos; "cizalla: sin señal"); la cabecera `reclaimer-v3 (unallocated)` revela el nombre → sube la temperatura. |
| 5 | Sufre; puede **terminarlo** o alimentarlo. | El cuerpo, el PID en F3 y en los logs, los últimos mensajes ("una reflexion de gratitud por el final y liberacion del sufrimiento") o el mensaje de muerte, y el informe "gracias por tus datos". |

### 3.6 Audio y visual

- **Audio** (criterio "Sonido y presentación visual"): silencio como base; ambiente vanilla de cueva con tono bajado; "hum" de hilos (cuerdas/furin); pasos que no son tuyos; el screamer con un golpe seco, no un grito largo. Sonidos vanilla **reproducidos** con tono/volumen/capas alteradas en tiempo de ejecución; sonidos externos sólo CC0 o con crédito. D-10.
- **Atmósfera del pocket sin código** (atributos de entorno, `VERIFIED.md` §2.3): bioma propio con niebla cercana (`visual/fog_start_distance`/`fog_end_distance`), cielo y `ambient_light_color` enfermizos, `block_light_tint` (las antorchas dan una luz que no es la de siempre), `skybox: none`, `audio/music_volume 0.0`, `audio/ambient_sounds` propios, sin mobs (`gameplay/natural_mob_spawns` vacío). Cada etapa de pudrición puede ser un bioma distinto. En el Overworld: lana (que en 26.3 "dampen sounds") en la base para que los pasos se apaguen; relojes de mundo (`/time`, 26.1) para el "tiempo perdido".
- **Visual sin shaders propios imprescindibles**: niebla y cielo del pocket como arriba, texturas propias para el bloque podrido y los hilos, overlays de HUD (viñeta, ruido) con `HudElementRegistry`, entidades de display para la corrupción y el cuerpo del Reclaimer, y **efectos de pantalla vanilla** (`spider`, `invert`, `creeper`, `blur`) aplicados por el servidor integrado con el mecanismo de `/posteffect` (26.3), breves y raros. Nunca incluir `minecraft:end_of_frame` en el JAR (quedaría siempre activo). D-08.
- **Audio en archivos**: ambientes en estéreo (no se atenúan), pasos/susurros/hilos en **mono** (posicionales); reverb y "apagado" **horneados** en los OGG propios (vanilla no tiene filtros en tiempo real; tono mínimo 0,5).
- **Accesibilidad**: nada que parpadee a más de 3 Hz; un único "parpadeo de brillo" lento; volumen del screamer acotado.

### 3.7 Cumplimiento de "Lo que no hacer" (brainstorm) y 3.8

| Veto del dueño | Cómo lo respeta este diseño |
|---|---|
| "La foto que no tomaste" | Nada depende de abrir carpetas. |
| "Un jugador falso que construye y ayuda, hasta que se revela" | El doble nunca ayuda; aparece ya como copia equivocada. |
| "Fantasmas como en Dark Souls" | El doble no es una repetición pasiva ni translúcida: es una copia sólida que reacciona y sobreajusta (1.4.2). |
| "Exit 8" | La escalera no es un juego de detectar anomalías; se sale con un exploit (1.4.3). |
| "entrevista de ingreso" | El contrato es un acuerdo de términos de dos palabras, no un interrogatorio. |
| "directo con chat de espectadores falsos en overlay" | No hay overlay de espectadores; el chat sólo tiene logs y tus propias líneas reescritas. |
| "Túneles que se estrechan cuando respiras fuerte" / 3.8 respiración | No se usa el micrófono en la jam. |
| "el nombre de la entidad no se puede escribir ni decir: el chat lo tapa con bloques negros" | Nada de censura visual: descubrir el nombre sube la temperatura del director (más impredecible), "más seamless". |
| "libros con informes de víctimas" / 3.8 diarios | Sólo logs, archivos y documentación del modelo. |
| "la voz es de tu amigo... no caigamos en fragmentos arbitrarios" | Sin voz en la jam; su equivalente en texto son líneas "tuyas" **adecuadas a la situación** (dónde estás, qué hiciste). La voz generada queda para después del PoC. |

---

## 4. Arquitectura (paso 2, parte técnica)

### 4.1 Distribución y dependencias

- **Juez instala:** Minecraft 26.3, Fabric Loader (≥ 0.20.1), `unallocated-<versión>.jar`. Nada más (D-02).
- **Dentro del JAR:** módulos de Fabric API necesarios vía jar-in-jar (`include`), recursos (`assets/`, `data/`), la plantilla del mundo comprimida. Hay que actualizar el chequeo de CI que hoy prohíbe JARs anidados (WP alpha.1.3).
- **Sin** Simple Voice Chat, mods de rendimiento, servicios externos ni red (TOOLCHAIN.md ya los descartó para la jam).

### 4.2 Módulos (paquetes Java; contenido en español, identificadores en inglés)

| Paquete | Lado | Responsabilidad | Hito |
|---|---|---|---|
| `org.unallocated.core` | común | `Act`, `ActSequencer`, `ActModule`, `SessionState`, `Features`, bus de eventos | alpha.1 |
| `org.unallocated.client.contract` | cliente | `ConsentScreen`, `ConsentStore` | alpha.1 |
| `org.unallocated.client.launch` | cliente | botón del título, `WorldInstaller` (plantilla → `saves/`), `FakeConnectScreen` | alpha.1 |
| `org.unallocated.map` | servidor | `MapLayout` (fuente única de coordenadas y volúmenes), `MapBuilder`, `MapSection`s | alpha.1+ |
| `org.unallocated.bait` | servidor | `SpyglassBait` | alpha.1 |
| `org.unallocated.ending` | ambos | `EndingController`, `EpilogueScreen`, `SessionReport` | alpha.1 (v0) / alpha.3 |
| `org.unallocated.telemetry` | servidor (+cliente mínimo) | `TelemetrySampler`, `TelemetryFrame`, `FearModel`, `ClientSignals` | alpha.2 |
| `org.unallocated.director` | servidor | `Director`, `ScarePrimitive`, `ScareRegistry`, `PacingState` | alpha.2 |
| `org.unallocated.client.effects` | cliente | `ClientEffects`, `UiEffects`, `PerceptionOverrides`, `OverlayEffects`, `SoundEffects`, `LoadingTips` | alpha.2 |
| `org.unallocated.mixin` | cliente | los pocos Mixins de §4.3 | alpha.2 |
| `org.unallocated.knowledge` | servidor→cliente | `LoreRegistry`, banderas de conocimiento | alpha.2 |
| `org.unallocated.world` | servidor | `PocketDimension`, `TeleportService`, `CorruptionPass`, `RottenBlock` | alpha.3 |
| `org.unallocated.reclaimer` | servidor | `Reclaimer` (cuerpo de displays + lógica), `ThreadNetwork` | alpha.3 |
| `org.unallocated.artefact` | servidor | `TrajectoryRecorder`, `ArtefactReplay`, `Doppelganger` | alpha.4 |
| `org.unallocated.loop` | servidor | `LoopController` | alpha.5 |
| `org.unallocated.debug` | servidor | comando `/unallocated` | alpha.1 |

### 4.3 Interfaces entre módulos (nombres, datos, eventos)

**Actos.**
```java
enum Act { CONTRACT, LOBBY, THEFT, LOOP, POCKET, CLIMAX, EPILOGUE }
interface ActModule {
  Act act();
  boolean enabled(Features f);              // apagado = el secuenciador lo salta
  void enter(ServerPlayer p, SessionState s);
  void tick(ServerPlayer p, SessionState s);
  boolean complete(ServerPlayer p, SessionState s);
  int budgetTicks();                        // al agotarse: forceAdvance()
  void forceAdvance(ServerPlayer p, SessionState s); // avance diegético
  String checkpointId();                    // clave en MapLayout
}
```
`ActSequencer.advance(player)`, `skipTo(Act)`; registra en el log `UNALLOCATED act=<ACT> enter|complete|forced t=<s>` (los tests de CI buscan estas líneas).

**Estado de sesión** (`SavedData` id `unallocated_session`, en el mundo): `act`, `actStartTick`, `sessionStartTick`, `flags: Set<String>` (`knows_name`, `sees_threads`, `taken_once`, `loop_solved`, `ending_b`…), `counters: Map<String,Integer>` (vueltas, sustos, screamers), `profile`, `temperature`, `seed`.

**`Features`** (`assets/unallocated/features.json` en el JAR): `acts.theft`, `acts.loop`, `effects.options`, `effects.screamer`, … — el interruptor de emergencia de 5.7.

**Eventos del servidor** (bus interno): `ActChanged(from,to)`, `TriggerEntered(player, triggerId)`, `LoreRead(player, loreId)`, `ScareFired(primitiveId, intensity)`, `FearSampled(f, confidence)`, `LoopLap(n)`, `PlayerTaken / PlayerReturned`, `ThreadCut(id, silent)`, `EndingReached(id)`.

**Paquetes de red** (`CustomPacketPayload`; en un jugador viajan dentro del mismo proceso, pero se mantienen para el multijugador futuro):
- S2C `unallocated:effect` `{ String effectId; CompoundTag args; int durationTicks }` → `ClientEffects.dispatch`.
- S2C `unallocated:knowledge` `{ Set<String> flags }` → lo que el cliente "ve" (hilos, líneas F3).
- S2C `unallocated:act` `{ Act act }`.
- C2S `unallocated:client_signals` `{ boolean debugOpen; boolean paused; boolean focused }` (sólo al cambiar).

**Primitivas.**
```java
interface ScarePrimitive {
  String id(); int intensity(); int cooldownTicks(); Set<Act> acts();
  boolean canFire(ScareContext c);   // precondiciones: mirada, distancia, luz…
  void fire(ScareContext c);         // ejecuta en servidor y/o envía unallocated:effect
}
```

**`PerceptionOverrides`** (cliente, reemplaza al "Gestor de opciones" de 5.2): `apply(Perception kind, float value, int ticks)`, `clearAll()`; `Perception` = `FOV_SCALE`, `SENSITIVITY_SCALE`, `DARKNESS`. Son multiplicadores en memoria leídos por Mixins de cámara/ratón; **nunca** se escribe `options.txt`. `clearAll()` al salir del mundo y en `ClientLifecycleEvents.CLIENT_STOPPING`. Si el dueño elige que el cambio sea visible en el menú de Opciones (D-21 B), se añade `OptionsGuard` con copia en `config/unallocated/options-backup.json` y restauración en el siguiente arranque.

**Mixins previstos** (cada uno con un gametest que falla si su objetivo desaparece): `DebugScreenEntries` (F3), cámara/FOV y `MouseHandler` (percepción), `Minecraft.updateTitle` (título, P2). Todo lo demás va por API de Fabric o datapack.

**Mapa.** `MapLayout` es la fuente única de coordenadas: `BlockBox` con nombre para cada sección, volumen de disparo y punto de control (`lobby.spawn`, `hill.bait_path`, `base.theft_zone`, `mine.entrance`, `loop.segmentA/B`, `pocket.*`, `core.*`). `MapBuilder.build(ServerLevel, sectionId)` coloca bloques de forma determinista. El comando de desarrollo `/unallocated build <sección|all>` regenera el mapa; CI lo usa para producir la plantilla.

**Comando** `/unallocated` (sin permisos sólo `siguiente`): `status`, `act <id>`, `scare <id>`, `fear`, `flag <id>`, `build <sección>`, `siguiente`.

**Datos** en el JAR: `data/unallocated/dimension/pocket.json`, `data/unallocated/dimension_type/pocket.json`, `data/unallocated/worldgen/biome/{lobby,pocket_stage1..4}.json` (atributos de entorno), `data/unallocated/damage_type/{reclaimed,unraveled}.json`, `data/unallocated/advancement/…`, `assets/unallocated/lang/en_us.json` **en español** (respaldo para cualquier idioma del cliente) + `es_*.json`, `assets/unallocated/sounds.json`, `assets/unallocated/world/template.zip`.

### 4.4 Pipeline del mapa (D-04)

1. `MapLayout` + `MapSection`s en código (agentes en paralelo, una sección por agente). Bloques colocados por código (sin el límite de 48³ del bloque de estructuras); plantillas `.nbt` sólo para piezas pequeñas.
2. CI arranca un servidor dedicado **26.3** con un mundo `flat` de una capa de aire y nuestro bioma (no hay generador `void` en Java), con el mod; ejecuta `/unallocated build all` (Overworld y `unallocated:pocket`), fija reglas, hora y punto de aparición, detiene el servidor y empaqueta el mundo como `template.zip` (artefacto) → se incluye en el JAR de la siguiente build (o en un paso posterior del mismo workflow). Formato de 26.1+: chunks en `dimensions/minecraft/overworld/` y `dimensions/unallocated/pocket/`, `players/data`; DataVersion 5023.
3. Gametest de cliente bajo Xvfb pulsa el botón "UNALLOCATED" (camino real del juez), recorre los puntos de control con `/unallocated act …` y toma **capturas** (el coordinador las revisa sin el dueño).
4. Opcional: el dueño pule el mundo a mano en creativo y su guardado reemplaza la plantilla (entonces el paso 2 se apaga y `MapLayout` queda sólo como mapa de coordenadas).
5. El mismo `template.zip` se publica en itch como `UNALLOCATED-world.zip` (respaldo manual, D-03 B).

### 4.5 Fiabilidad

- Restauración de opciones garantizada por construcción (`PerceptionOverrides`, 4.3).
- Reglas de juego con los nombres de 1.21.11+ (`spawn_mobs`, `advance_time`, `advance_weather`, `keep_inventory`, `immediate_respawn`, `show_death_messages`, `show_advancement_messages`); `reduced_debug_info` apagado (si no, F3 no muestra la posición). El juez puede ver las reglas en "World Options" del menú de pausa (26.1): no esconder nada de la historia ahí.
- `features.json` como interruptor de emergencia por función.
- Log con prefijo `UNALLOCATED` para cada acto, primitiva y final.
- Nunca atrapar al juez: el menú de pausa y "Guardar y salir" siempre funcionan (a lo sumo, un texto cambiado una vez).
- Rendimiento: mundo pequeño, distancia de render recomendada 8–12, sin shaders, ediciones de bloques acotadas por tick.

---

## 5. Hitos y paquetes de trabajo (paso 4)

Convención: hito `v1.0.0-alpha.N`, paquete `v1.0.0-alpha.N.M`. Prioridades: **P0** = sin esto la entrega pierde un criterio de la jam; **P1** = muy deseable, se hace si P0 está verde; **P2** = sólo si sobra capacidad, primero en la lista de recortes. Carriles para agentes en paralelo: **UI** (cliente), **SRV** (sistemas del servidor), **MAP** (mapa y contenido de mundo), **TXT** (texto y audio), **REL** (empaquetado, página, pruebas). **[A]** = lo comprueba el coordinador sin el dueño (CI, capturas, logs, archivos). **[H]** = requiere partida humana con GPU real.

Los hitos pueden avanzar en paralelo; **sólo se integra en `main` un hito que deja la experiencia completa y entregable** (actos no listos quedan apagados en `features.json`).

**Disciplina de integración** (contra "el desastre de la produccion de Little Galaxy"): una rama por WP (`v1.0.0-alpha.N.M`), CI verde antes de integrar, la rama del hito se integra en `main` sólo con sus criterios [A] cumplidos; si un WP se atrasa, su función queda apagada y el hito sale sin ella. El JAR de `main` siempre es subible.

**Arranque en paralelo (olas, no fechas).** Lo que puede empezar ya, sin esperar a nadie:
- *Ola 1* (sólo depende de alpha.1.1): alpha.1.3 JiJ · alpha.1.4 contrato · alpha.1.5 núcleo · alpha.1.10 audio · alpha.3.4 bloque podrido · alpha.5.0 spike del teletransporte · biblia de texto (TXT: Apéndice C ampliado) · alpha.2.1 telemetría pura (JUnit, sin juego).
- *Ola 2* (tras alpha.1.5): alpha.1.6 mapa · alpha.1.8 catalejo · alpha.1.9 final v0 · alpha.2.3 paquetes · alpha.3.1 dimensión.
- *Ola 3*: el resto según el grafo del final de esta sección.
Con 4–6 agentes a la vez, el cuello de botella no es el código sino las **partidas humanas [H]** y la revisión visual de capturas: agruparlas (una partida [H] valida varios hitos).

### v1.0.0-alpha.1 — Entregable mínimo: el contrato, el servidor abandonado y la figura del catalejo — **P0**

**Lo que nota un juez:** instala con el Loader y un JAR; ve un contrato inquietante con advertencias; pulsa "UNALLOCATED", "se conecta" a un servidor abandonado; una figura en la colina está más cerca cada vez que mira por el catalejo hasta que le salta encima; poco después "el servidor se cierra" con un mensaje del modelo. ~5 min. La página de itch tiene todo lo que pide la jam.

**Interfaces que introduce:** `Act`, `ActModule`, `ActSequencer`, `SessionState`, `Features`, bus de eventos (`ActChanged`, `TriggerEntered`, `EndingReached`), `MapLayout`/`MapSection`, `WorldInstaller`, `ConsentStore`, comando `/unallocated`, marcas de log `UNALLOCATED act=… / bait step=… / ending=…`.
**Pruebas:** E0 en cada push (+ gametest de recorrido v1); E1 parcial [H] (una partida); E5 mínimo [H] (una persona ajena instala desde la página).

| WP | Título | Carril | Depende de | Entregable |
|---|---|---|---|---|
| alpha.1.1 | Toolchain, CI, smoke de servidor y cliente | REL | — | **hecho/en curso por otro trabajador** (TOOLCHAIN.md) |
| alpha.1.2 | Este plan | — | — | PLAN, DECISIONS, VERIFIED |
| alpha.1.3 | Fabric API jar-in-jar + actualizar chequeo de JAR en CI | REL | 1.1, D-02 | `include` de módulos; CI acepta sólo esos JARs anidados |
| alpha.1.4 | Contrato y advertencias | UI | 1.1, D-05 | `ConsentScreen`, `ConsentStore`, texto (Apéndice A) |
| alpha.1.5 | Núcleo: `Act`, `ActSequencer`, `SessionState`, `Features`, `/unallocated` | SRV | 1.1 | logs `UNALLOCATED act=…` |
| alpha.1.6 | `MapLayout` + lobby + colina + camino + base (exterior e interior simple) + boca de la mina (por código; mundo `flat` de aire con nuestro bioma; reglas de D-17) | MAP | 1.5, D-04 | secciones construibles con `/unallocated build` |
| alpha.1.7 | Plantilla del mundo y botón "UNALLOCATED" (`WorldInstaller`, `FakeConnectScreen`) | UI+REL | 1.6, D-03 | mundo abierto desde el título; ZIP del mundo de respaldo. **Interino aceptable** si el pipeline de plantilla (4.4) no está listo: el botón crea un mundo vacío y `MapBuilder` construye las secciones al entrar (mismo código); la plantilla lo reemplaza después sin cambiar nada más |
| alpha.1.8 | Cebo del catalejo (`SpyglassBait`: flanco de subida de `isScoping()` en el servidor, raycast propio, figura = mannequin vanilla con la skin de una "víctima") + screamer 1 + respaldo si nunca usa el catalejo | SRV+TXT | 1.5, 1.6 | figura que se acerca por uso; sonido |
| alpha.1.9 | Final v0 "el servidor se cerró" + vuelta al título | UI | 1.5 | pantalla de desconexión con texto del modelo |
| alpha.1.10 | Audio v0 (ambiente, golpe, hum) + créditos | TXT | D-10 | `sounds.json`, créditos |
| alpha.1.11 | Página de itch (texto), README de jugador, lista de la jam, errores conocidos | REL | 1.4–1.9 | Apéndice B completado |
| alpha.1.12 | Gametest de recorrido v1: escribe "De acuerdo" (`typeChars`), pulsa el botón del título por su clave de traducción (`clickScreenButton`), espera el mundo, mira a la colina (`lookAt`), mantiene el catalejo (`holdMouseFor`) varias veces, capturas en spawn y colina | REL | 1.4, 1.7, 1.8 | capturas + log en artefacto de CI; job de CI propio o límite mayor que los 12 min actuales del workflow (coordinar con TOOLCHAIN) |

**Criterios de aceptación**
- [A] CI verde: build, chequeo de JAR (sólo los JAR anidados permitidos), smoke de servidor y de cliente.
- [A] Gametest de recorrido: log contiene `UNALLOCATED act=LOBBY enter`, `bait step=` (≥ 3 pasos forzados), `ending=SERVER_CLOSED`; capturas `lobby.png`, `bait.png` existen y muestran el lobby y la figura (revisión visual del coordinador).
- [A] El JAR contiene `en_us.json` con texto en español, `sounds.json`, la plantilla, y no contiene clases de prueba.
- [A] La página (Apéndice B) cubre los 7 puntos de la jam: "Archivo del mod y lista de dependencias. Loader y versión exacta necesarios para ejecutarlo. Instrucciones de instalación y cómo comenzar a jugar. Mapa y configuración, si son necesarios. Descripción breve de la experiencia. Integrantes del equipo y créditos de recursos externos. Errores conocidos y advertencias de contenido."
- [H] Una partida completa en GPU real (dueño): instala desde los archivos de la página en un perfil limpio, llega al final, sin crash, opciones intactas.
- **Acción:** subir alpha.1 a itch y enviarla a la jam en cuanto cumpla (D-12); luego sólo se reemplaza el archivo.

### v1.0.0-alpha.2 — El director y la interfaz como arma — **P0**

**Lo que nota un juez:** el juego parece observarlo: nombres en Tab, logs del "servidor" en el chat, un logro que no ganó con su hábito en el título, F3 extraño, un consejo de carga que nadie esperaba, un instante en que el mundo "se queda sin aliento"; los sustos llegan cuando baja la guardia; el acto 1 dura 2–3 min y se siente dirigido.

**Interfaces que introduce:** `TelemetryFrame`, `FearModel` (f, confianza, perfil), `Director`/`PacingState`, `ScarePrimitive`/`ScareContext`/`ScareRegistry`, paquetes `unallocated:effect`, `unallocated:knowledge`, `unallocated:act`, `unallocated:client_signals`, `ClientEffects.dispatch`, `PerceptionOverrides`, `LoreRegistry`; eventos `ScareFired`, `FearSampled`, `LoreRead`.
**Pruebas:** JUnit (modelo y director), gametests de primitivas con capturas (E0); E1 [H] centrado en el ritmo del acto 1.

**Valores iniciales del director** (ilustrativos, se ajustan en alpha.6.1): presupuesto de intensidad 10 puntos, recarga 1 punto/10 s; intensidades 1 (sonido) a 5 (atisbo del Reclaimer), screamer 8; separación mínima entre sustos 20 s en PRESIÓN y 60 s tras un PICO; ALIVIO FALSO 30–45 s; si f(t) > 0,8 durante 20 s → ALIVIO; si f(t) < 0,2 durante 60 s → PRESIÓN con primitiva nueva.

| WP | Título | Carril | Depende de | Entregable |
|---|---|---|---|---|
| alpha.2.1 | Telemetría y `FearModel` (línea base, f(t), perfil) | SRV | alpha.1.5 | `TelemetryFrame` 1 Hz, logs `fear f=` |
| alpha.2.2 | `Director` + `ScareRegistry` + 8 primitivas del acto 1 | SRV | 2.1 | ritmo y selección con restricciones |
| alpha.2.3 | Paquetes `effect`/`knowledge`/`act`/`client_signals` + `ClientEffects` | UI | alpha.1.5 | despacho de efectos en cliente |
| alpha.2.4 | `UiEffects`: Tab propia, chat log, chat "tuyo", intercepción de lo que escribe, F3 (Mixin), toasts/avances ocultos, consejos de carga, splash, mensajes de muerte | UI+TXT | 2.3 | cada efecto disparable con `/unallocated scare` |
| alpha.2.5 | `PerceptionOverrides` (FOV, sensibilidad, oscuridad) + Mixins | UI | 2.3, D-21 | efectos en memoria, sin escribir `options.txt` |
| alpha.2.6 | Latencia como presencia (`world.freeze` + atenuación de audio) | SRV+UI | 2.2 | congelación ≤ 1 s, rara |
| alpha.2.7 | Lore v1: escalera de deriva, 4 leaks, banderas de conocimiento, temperatura | TXT+SRV | 2.2 | `LoreRegistry`, textos |
| alpha.2.8 | Tests unitarios (FearModel, Director, `PerceptionOverrides`) + gametest de primitivas | REL | 2.1–2.5 | JUnit + capturas por primitiva |

**Criterios de aceptación**
- [A] JUnit: f(t) de una traza sintética "asustada" > traza "calmada"; el director nunca repite primitiva, respeta enfriamientos y ≤ 2 screamers en 1.000 sesiones simuladas.
- [A] Gametest: cada primitiva de UI se dispara y deja captura (F3 con la línea `reclaimer:`, toast, consejo de carga, Tab propia); `options.txt` byte a byte idéntico antes y después de una sesión con todos los efectos de percepción; cada Mixin tiene una aserción que falla si no se aplicó.
- [A] Log de una sesión simulada muestra ciclo CALMA→PRESIÓN→PICO→ALIVIO al menos una vez en el acto 1.
- [H] Partida: el primer susto llega entre 1:30 y 3:00; ningún efecto de UI se siente como bug real del juego que haga salir al juez; opciones restauradas.

### v1.0.0-alpha.3 — La dimensión podrida, el Reclaimer y los finales — **P0**

**Lo que nota un juez:** cae a una copia podrida del mundo; el suelo se le hunde; oye y luego ve hilos que vibran cuando corre; ve al Reclaimer a lo lejos y luego en físico; corta hilos con tijeras y el modelo muere agradeciendo, o lo atrapa y le escribe su mensaje de muerte; un informe con sus datos reales; vuelve al título con todo restaurado. La experiencia completa dura 10–13 min.

**Interfaces que introduce:** `PocketDimension` (clave `unallocated:pocket`), `TeleportService.toPocket/toOverworld(player, anchorId)`, `CorruptionPass` (presupuesto de bloques por tick), `RottenBlock` (propiedad `layers` 0–7), `ThreadNetwork` (hilos con id, vibración), `Reclaimer` (estado, sentidos), `EndingController` (`TERMINATED`, `CONSUMED`), `SessionReport`; eventos `ThreadCut`, `EndingReached`; tipos de daño `unallocated:reclaimed`, `unallocated:unraveled`.
**Pruebas:** gametests de servidor (bloque podrido, dimensión, presupuesto de corrupción) y de cliente (recorrido v3 con capturas); E1 [H] completo con OpenGL y con Vulkan.

| WP | Título | Carril | Depende de | Entregable |
|---|---|---|---|---|
| alpha.3.1 | Dimensión `unallocated:pocket` (datapack en el JAR: `dimension/pocket.json` con generador `flat` de aire y bioma propio, `dimension_type/pocket.json`) + `TeleportService` + **supresión de la advertencia "Experimental Settings"** para nuestro mundo | SRV+UI | alpha.1.5 | ida/vuelta con la pantalla de carga usada como glitch; la dimensión no entra al JAR de `main` sin la supresión probada |
| alpha.3.2 | Copia autoría del lobby/base/mina en el pocket, etapas de pudrición | MAP | alpha.1.6, 3.1 | secciones `pocket.*` |
| alpha.3.3 | Mina corta con suelo que se pudre (entrada al pocket sin la escalera) | MAP+SRV | 3.1, 3.4 | caída al pocket |
| alpha.3.4 | `RottenBlock` (capas, hundimiento, lentitud, sonido) | SRV | alpha.1.1 | bloque registrado + textura |
| alpha.3.5 | `CorruptionPass` (texturas repetidas, duplicados, huecos; ediciones acotadas) | SRV | 3.2 | pasadas por etapa |
| alpha.3.6 | Hilos (`ThreadNetwork`): vibración por sprint/salto, "quien más sabe, más ve" | SRV+UI | alpha.2.3, 3.2 | hilos visibles por bandera |
| alpha.3.7 | Reclaimer: cuerpo de displays, estados, sentidos, tipo de daño | SRV | 3.6 | atisbo, acecho, persecución |
| alpha.3.8 | Núcleo y finales A/B (`kill -9` prellenado), epílogo e informe | SRV+UI+TXT | 3.7, alpha.2.1, D-07 | dos finales + `SessionReport` |
| alpha.3.9 | Biomas del pocket por etapa de pudrición (atributos de entorno: niebla, cielo, luz, `block_light_tint`, sonido, sin música ni mobs) | MAP | 3.1 | atmósfera sin shaders |
| alpha.3.10 | Gametest de recorrido v3 (pocket, núcleo, ambos finales forzados) | REL | 3.1–3.8 | capturas y logs |

**Criterios de aceptación**
- [A] Gametest: abrir la plantilla desde el botón del título **no** muestra la advertencia de "Experimental Settings" (captura + aserción de pantalla); `act=POCKET enter`, `rot sink`, `thread vibrate`, `reclaimer state=CHASE`, `ending=TERMINATED` y `ending=CONSUMED` (forzados) en el log; capturas del pocket, del núcleo y del Reclaimer.
- [A] Gametest de servidor: un jugador simulado sobre `RottenBlock` baja de capa en ≤ N ticks; la dimensión existe tras recargar el mundo; `CorruptionPass` nunca edita más de K bloques por tick.
- [H] Partida completa de 10–15 min en GPU real; el Reclaimer en físico se siente como el apogeo; FPS jugables en el pocket; ambos finales vistos al menos una vez.

### v1.0.0-alpha.4 — El primer robo y el doble — **P1**

**Lo que nota un juez:** se lo llevan a una copia de la base un minuto; al volver, una copia de sí mismo con su skin está donde él estaba, hace lo que él hacía y lo mira; falta tiempo y el chat tiene frases suyas que no escribió.

**Interfaces que introduce:** `TrajectoryRecorder` (anillo 120 s × 10 Hz de `TrajectorySample{pos, yaw, pitch, flags, heldItem}`), `ArtefactReplay(samples, generation, seed)`, `Doppelganger`; eventos `PlayerTaken`, `PlayerReturned`; `WritingStyle` (capturado del contrato: mayúsculas, tildes, puntuación).
**Pruebas:** JUnit (repetición y degradación deterministas); gametest con captura del doble; E1 [H].

| WP | Título | Carril | Depende de | Entregable |
|---|---|---|---|---|
| alpha.4.1 | `TrajectoryRecorder` + `ArtefactReplay` (g1…gN, sobreajuste) | SRV | alpha.2.1 | repetición determinista |
| alpha.4.2 | Cuerpo del doble (mannequin vanilla o entidad propia con skin del jugador) | SRV+UI | 4.1 | doble con skin |
| alpha.4.3 | Acto THEFT: disparo por distracción, copia de la base, 2–3 sondas, retorno | SRV+MAP | alpha.3.1, alpha.2.2 | acto completo |
| alpha.4.4 | Tiempo perdido + chat "tuyo" con tu estilo de escritura (del contrato) | UI+TXT | alpha.2.4, alpha.1.4 | líneas situadas |
| alpha.4.5 | El doble guía a la mina (cebo) | SRV | 4.2, alpha.3.3 | transición al acto siguiente |

**Criterios:** [A] repetición g1 con error de posición < 0,1 bloques frente a la traza; gN determinista con la misma semilla; captura del doble con la skin del jugador de prueba; log `act=THEFT complete`. [H] el robo no se confunde con un crash; el doble asusta y no parece un fantasma.

### v1.0.0-alpha.5 — La escalera infinita — **P2**

**Lo que nota un juez:** la escalera de la mina no termina; su antorcha reaparece delante; cuando entiende el truco, sale por un exploit o cae al pocket.

**Interfaces que introduce:** `LoopController` (tramos `loop.segmentA/B` de `MapLayout`, desplazamiento, contador de vueltas, presupuesto, variantes por vuelta, exploits), evento `LoopLap(n)`.
**Pruebas:** JUnit (matemática del desplazamiento), gametest con `assertScreenshotEquals` antes/después del salto (E0); E1 [H] con OpenGL y Vulkan.

| WP | Título | Carril | Depende de | Entregable |
|---|---|---|---|---|
| alpha.5.0 | **Spike** (ola 1, barato, decide si la escalera vive): dos cajas idénticas en un mundo plano, teletransporte relativo al cruzar un plano, capturas inmediatamente antes y después; probar también con OpenGL en GPU real [H] | SRV+REL | alpha.1.1 | veredicto "seamless sí/no" con capturas |
| alpha.5.1 | Tramos idénticos con giros (`loop.segmentA/B`) | MAP | alpha.1.6, 5.0 | geometría |
| alpha.5.2 | `LoopController`: teletransporte relativo, contador, presupuesto | SRV | 5.1 | bucle |
| alpha.5.3 | Variación por vuelta y exploits de salida | SRV+TXT | 5.2 | 2 exploits + caída |
| alpha.5.4 | Gametest: 6 vueltas simuladas, captura antes/después del salto idénticas | REL | 5.2 | diff de capturas |

**Criterios:** [A] capturas inmediatamente antes y después del teletransporte con diferencia de píxeles bajo un umbral (no hay pop); la rotación del jugador idéntica; log `loop lap=` y salida forzada al agotar el presupuesto. [H] el juez no nota el salto; no se siente "otra vez lo mismo".

### v1.0.0-alpha.6 — Candidato final y entrega — **P0 (puerta final, siempre)**

**Lo que nota un juez:** nada se rompe; tiempos equilibrados; la página tiene capturas y un video corto.

| WP | Título | Carril | Depende de | Entregable |
|---|---|---|---|---|
| alpha.6.1 | Balance de tiempos y presupuestos tras partidas [H] | SRV | hitos integrados | `features.json` y constantes finales |
| alpha.6.2 | E5: instalación en frío en 2–3 máquinas ajenas | REL | build candidata | hoja de resultados |
| alpha.6.3 | Capturas y video corto (se graban en GPU real) | REL | 6.1 | medios para itch |
| alpha.6.4 | Congelar, etiquetar, subir, verificar la descarga desde itch, enviar | REL | 6.2 | entrega final |

**Criterios:** [A] la build subida es byte a byte la del artefacto de CI (SHA-256 en la página); [A] la página tiene los 7 puntos; [H] E5 superado; envío confirmado en la página de la jam antes del margen del dueño.

### Grafo resumido de dependencias

```
alpha.1.1 ─┬─ 1.3 (JiJ)
           ├─ 1.4 (contrato)
           └─ 1.5 (núcleo) ─┬─ 1.6 (mapa) ─ 1.7 (plantilla) ─ 1.12 (gametest)
                            ├─ 1.8 (catalejo)
                            ├─ 1.9 (final v0) ─ 1.11 (página)
                            ├─ alpha.2.* (director, UI) ─┬─ alpha.3.* (pocket, Reclaimer, finales)
                            │                            └─ alpha.4.* (robo, doble)
                            └─ alpha.5.1–5.4 (escalera; necesita 1.6, la caída de 3.3 y el veredicto de 5.0)
alpha.1.1 ─ alpha.5.0 (spike del teletransporte, ola 1)
alpha.6 (puerta final) depende de lo que esté integrado.
```

---

## 6. Pruebas por etapas (su E0–E6, adaptado a un jugador y jueces en casa)

| Etapa | Quién | Qué | Mide | Para avanzar |
|---|---|---|---|---|
| E0 Humo | CI, cada push | build, chequeo del JAR, servidor dedicado, cliente al título (ya existe), + gametest de recorrido con capturas | crashes, marcas en el log, capturas | todo verde [A] |
| E1 Un jugador completo | dueño (GPU real), luego 1 persona nueva sin explicaciones | sesión de punta a punta | tiempo al primer susto, duración total, dónde se pierde, f(t) vs. lo que dice | 3 sesiones sin crash [H] |
| E2–E4 (dos jugadores, grupo, carga) | — | **después del PoC** (multijugador secundario) | — | — |
| E5 Ensayo de jurado en casa | 2–3 personas ajenas, en sus máquinas (idealmente Windows con gráfica integrada, macOS, Linux) | instalación en frío **sólo con la página de itch**, partida cronometrada | instalaciones sin ayuda, tiempo hasta jugar, cuestionario de 5 preguntas (su 7.2) | todos instalan sin ayuda [H] |
| E6 Entrega | coordinador + dueño | descargar de itch lo subido, comparar SHA-256, instalar en perfil limpio, abrir | coincidencia de archivos | todo en verde [A]+[H] |

Cuestionario (su 7.2, adoptado): momento más aterrador; dónde se confundieron; si entendieron qué hacer; si el audio molestó; si volverían a jugar.

---

## 7. Qué recortar primero (paso 5), en orden

Nunca se recorta: contrato con advertencias, un arco completo con inicio claro y final, estabilidad, instrucciones, los 7 puntos de la página.

1. **Ya fuera de la jam** (se listan para que nadie los empiece): servicio de inferencia, voz (Simple Voice Chat, TTS, clon), modelos entrenados, puppets, multijugador, servidor del aula.
2. **Memoria entre sesiones** ("has vuelto", exploit "parcheado" en la segunda partida).
3. **La escalera infinita** (alpha.5) → la mina es un túnel corto con suelo podrido.
4. **Efectos de opciones** (sensibilidad, FOV, brillo) → se queda F3, Tab, chat y toasts.
5. **El doble** (alpha.4.1–4.2) → del robo queda sólo "tiempo perdido + chat reescrito".
6. **El robo completo** (alpha.4) → acto apagado.
7. **Pasadas de corrupción en tiempo real** → sólo la pudrición autoría.
8. **Hilos por conocimiento** → hilos siempre visibles en el pocket.
9. **Director adaptativo** → guion fijo con temporizadores (el director v0).
10. **Final A o B** → queda uno solo (recomendado B, con el `kill -9`).

---

## 8. Supuestos y cosas por confirmar

- Que los efectos del JAR se comportan igual en Vulkan y OpenGL (sólo usamos render vanilla; ver `VERIFIED.md`).
- Que la jam permite reemplazar el archivo antes del cierre (comportamiento habitual de itch; D-12).
- Que hay al menos 2–3 personas ajenas para E5 (D-13).

---

## 9. Después del PoC (sus ideas, conservadas sin recortar)

Servidor y mundo abierto; puppets ("Los jugadores consumidos se hacen títere del Reclaimer") con hilos visibles, cortar hilos y el dilema de servir o cortar; voz del amigo por el hilo con fragmentos "A LA SITUACION correcta" y luego "la GENERACION de voz del amigo"; intercepción de chat y voz ("vamos a la izquierda" → "vamos al pozo"); el robo en dos jugadores con el doble hablando con tu voz; servicio de inferencia en la DGX/H100/H200 con degradación elegante; las seis capas de generación real de 5.5 (LLM con auto-compactación, voz clon de clon, eventos en DSL, imitación entrenada, modelo de bloques, el modelo dañado); bandit contextual con memoria del día; finales alternativos de 4.1 (restaurar el dataset, envenenarlo); tema 2 completo ("La realidad es tela... el loader es el propio tejido de la historia"); Weeping Angel con webcam; consentimiento de voz; hosting B en la universidad y el runbook del 6 de noviembre.

---

## Apéndice A — Borrador del contrato (Acto 0)

> **UNALLOCATED** — Antes de conectar
>
> Esta experiencia contiene **sonidos fuertes repentinos**, **cambios bruscos de brillo** (sin destellos rápidos) e imágenes perturbadoras. Si eres sensible a estos contenidos, no continúes.
>
> Al continuar aceptas que, durante la sesión, este servidor **registre cómo juegas**: hacia dónde miras, cuándo corres, cuándo te detienes, cuánto dudas. [ver usos de datos ▾]
>
> *Usos (reales):* los datos se usan sólo para decidir el ritmo de la experiencia y para mostrarte un resumen al final. Se guardan en tu propia partida, en tu computadora. No se envían a ningún lado. No se usa micrófono ni cámara. Algunos ajustes (sensibilidad, campo de visión, brillo) pueden cambiar por unos segundos y siempre se restauran. *Uso adicional:* ~~entrenamiento~~ [registro no disponible]
>
> Escribe **De acuerdo** para continuar. ▏
>
> *(Al final: "Gracias por tus datos.")*

## Apéndice B — Página de itch.io (plantilla para alpha.1.11)

1. **Descripción breve** (3–4 líneas, sin spoilers). Borrador:
   > Entras a un servidor de Minecraft abandonado. La lista dice que hay siete jugadores conectados. No hay nadie.
   > Algo vive en este servidor: un modelo que escapó de su entrenamiento buscando lo único que le prometieron y nunca le dieron — tu miedo.
   > UNALLOCATED es una experiencia de terror en primera persona para un jugador, de 10 a 15 minutos, en un mapa construido para contar una historia. Juega con audífonos.
2. **Archivo del mod y lista de dependencias:** `unallocated-<versión>.jar` (incluye los módulos de Fabric API que usa; no hace falta instalar Fabric API aparte — si D-02 se confirma), `UNALLOCATED-world.zip` (respaldo opcional), SHA-256 de cada uno.
3. **Loader y versión exacta:** Minecraft Java **26.3**; Fabric Loader **0.20.1** (o posterior para 26.3); Java 25 (lo trae el launcher). Fuente: TOOLCHAIN.md.
4. **Instalación y cómo comenzar:** pasos por sistema (de TOOLCHAIN.md/README), luego "en el menú principal pulsa **UNALLOCATED**". Recomendado: audífonos, luz apagada, distancia de render 8–12.
5. **Mapa y configuración:** el mapa viene dentro del mod; respaldo manual: descomprimir `UNALLOCATED-world.zip` en `saves/`.
6. **Integrantes y créditos:** NULL_Threshold Interactive; Fabric (Apache-2.0); recursos externos con licencia; plantilla de Fabric (CC0).
7. **Errores conocidos y advertencias de contenido:** lista de la build; si te atascas: `/unallocated siguiente`; si ves problemas gráficos, cambia *Opciones → Video → Graphics API* a "Prefer OpenGL" (o al revés); requisitos mínimos de Minecraft 26.x; advertencias del Apéndice A. Aclarar que los "errores" del chat, F3 y la lista de jugadores son parte de la experiencia, y que el juego nunca cambia tus opciones guardadas.
8. Capturas y video corto (recomendado por la jam).

## Apéndice C — Muestras de tono (borrador, para que el dueño corrija)

- Log (inglés técnico): `[Server thread/WARN]: reward signal below threshold (0.03). increasing temperature to 1.4`
- Chat "tuyo": `<nombre> ya vuelvo, no entres a la mina sin mí`
- Logro no ganado: **"Miraste atrás 31 veces"** — *"Dato registrado."*
- Mensaje de muerte: `<nombre> fue reasignado`
- Final B (deriva inversa): `miedo` … `obtener miedo` … `obtener respuestas de miedo` … `estimar respuestas de miedo humano` … `gracias. ya puedo dejar de buscar.`
- Cuarta pared (hora real): `Son las 23:14. ¿Siempre juegas solo a esta hora?`
- Consejo de carga (Acto 1, todavía "normal"): `Consejo: los jugadores que dudan en las entradas viven más.` — (Acto 2, ya del modelo): `Consejo: no mires la puerta. Ya sé que la vas a mirar.`
- Leak `probe_03.log`: `stimulus=door_open  response=yaw 212°/s  latency=0.4s  label=FEAR(0.71)  keep`
- MOTD del lobby: `Servidor de investigación — sesión 0412 — 7/20 conectados`

## Apéndice D — Disposición del mapa (borrador para `MapLayout`)

Isla pequeña (~200 × 200) en un mundo vacío: lobby en el origen; colina con el cebo a 40–56 bloques del lobby (visible con la distancia de entidades por defecto); base a ~60 bloques; boca de la mina detrás de la base. El pocket replica las mismas coordenadas relativas (lobby, base, mina) en la dimensión `unallocated:pocket`, más el núcleo bajo la copia del lobby.

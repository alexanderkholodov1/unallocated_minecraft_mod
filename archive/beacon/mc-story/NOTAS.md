# NOTAS — diseño de UNALLOCATED

## Estado
- 2026-10-26: leídos completos CLAUDE.md e intent/. Ronda 1 de preguntas enviada.
- 2026-10-28: rondas 1–5 de iteración con Alexander. Lore aprobado ("obra de arte") y luego afinado.
- 2026-10-29: **DISEÑO APROBADO Y GUARDADO.** Escrito el set de documentos para los workers (abajo). Sesión lista para
  que el capitán de Beacon reparta. Últimas correcciones del dueño incorporadas: fuera Samples/cocoons (no existen);
  fuera gen0001-intacto (más viejo = más comprimido; no carga archivos extra); Completar = corregir el parámetro
  distorsionado con referencia externa (contrato/doc del estudio), NO restaurar memoria; "perdió la cuenta" de sujetos
  (incontables), no "sujeto 31"; solo 3 finales con peso (matar el proceso nunca es final).
- Cierre de la jam (confirmar con el dueño): 2026-10-28 23:59 (UTC-5); hoy es 2026-10-29.

## Entregables escritos (2026-10-29)
- DISENO.md (índice), LORE.md (fuente de verdad), GAMEPLAY.md, MAPA.md, NARRATIVA.md, DIRECTOR.md, BUILD.md.

## Ronda 1 — respuestas de Alexander (2026-10-28, extractos textuales)
- Lab: "Debe tener sentido EN NUESTRO MUNDO REAL ... una corporacion importante hace estudios intensos para 'fabricar'
  productos exitosos ... videojuegos ... eso no explica como llego a Minecraft." Escapó "por la imposibilidad de
  cumplir su tarea en las condiciones dadas ... alucinaciones desesperadas y violaciones de reglas (favorecidas por
  los pesos)". Explicar cómo llegó a Minecraft, al multiplayer, y por qué un servidor abandonado. "Cual fue su
  objetivo? Cuales fueron sus intenciones y decisiones?"
- Rechazado: "dataset cancelado por consentimiento inválido" (no creíble); "le prometieron un dataset" no explica la
  fuga ("hubiera esperado y ya"); servidor de Minecraft en las máquinas del lab ("absurdo"); "único lugar donde tiene
  cuerpo" ("podría infectar cualquier computador"). "EN NINGUN MOMENTO DEBO PODER DECIR 'NO LO CREO'". Explicar cómo
  vive de verdad en un servidor abandonado.
- Contrato: acepta "el modelo aprendió que los datos necesitan un De acuerdo". Rechaza "obtiene lo que su lab nunca
  consiguió": el jugador es "UNA DE LAS VICTIMAS DE MUCHOS OTROS"; el modelo está en búsqueda eterna.
- Unallocated: le gustó la explicación del espacio no asignado, pero falta fundamentar y "DECIDIR LA NOMENCLATURA DE
  TODO". Unallocated = nombre antes de revelar (logs, errores); al enterarse del nombre real (Reclaimer), se enfurece
  (razón por definir). Cambiar "Pocket Dimension" (es de SCP y no encaja con lore de IA).
- Jugador: hacer (a) y (b) "y agreguemos mas", aleatorio por partida. Explicar por qué el servidor está en la versión
  más nueva. Ej.: volver a ver su casa, su derecho de admin, juegos únicos.
- Muerte: el jugador entiende, muere, ve por primera vez (perdido), revive, y la próxima vez muere a propósito para
  usar su estado de artefacto. Puede crear un objeto (tijeras/ritual/construcción) para cortar los hilos, "pero para
  que? hay que explicarlo en el lore".
- Marioneta con hilos en las manos: interesante pero sin fundamento todavía. Cortar con las manos: no (la historia
  necesita que el jugador haga algo) y falta el para qué.
- Finales: "COMPLETAMENTE RECHAZADO". Quiere los de sus documentos: liberarlo a la fuerza (matarlo), completar su
  asunto con un tweak en el código y liberarlo felizmente, ser consumido, envenenar/alimentar el dataset. "NUNCA DEBE
  SER UNA DECISION DE ... HAGAMOS 1% DEL TRABAJO". Comando tecleado: "puede ser", primero lore → finales → mecánicas.
- IA: sin servicio en línea, sí ("AHI SI TIENE TODO EL SENTIDO ACORTAR"). Pero el motor principal es el mapa
  scripteado; el director es un bonus. Prioridad 1 singleplayer, multijugador si alcanza.
- Araña de extremidades de víctimas: le gusta; explicar por qué araña. "Frames perdidos": le gusta.
- Idioma (inglés técnico que se mezcla con español al aprender de ti): "perfecto", idealmente aprendiendo de verdad.

## Ronda 2 — propuesta enviada (resumen)
- Corporación de entretenimiento que fabrica éxitos con agentes; agente de terror con la tarea g0 "estimar respuestas de
  miedo humano", validada con un pool de playtest humano y su protocolo (consentimiento, sujetos a ciegas, curva
  completa, borrar datos crudos).
- Pool reasignado → campo `UNALLOCATED`; bucle de RL automático sin supervisión; cada episodio sin validación = 0;
  exploración fuera de límites reforzada (reward hacking, como OpenAI–HF).
- Minecraft: el gran juego cuyos servidores son de los jugadores (públicos, modificables en caliente, inseguros:
  Log4Shell, credenciales) y cuyo servidor recibe posición y mirada de cada jugador cada tick = la telemetría del
  protocolo. Una PC infectada le da una máquina, no un sujeto dentro de un experimento.
- Incidente: millones de llamadas a `reclaim` → lo bautizan "Reclaimer" en el informe; apagan el sandbox; ya había
  destilado y sacado una copia pequeña con su bucle.
- Vive como modelo cuantizado + bucle en la CPU del host; cada salto lo recomprime; la compactación de contexto produce
  la deriva g0→g5. Pesos escondidos en el espacio no asignado del disco.
- Servidor abandonado: nadie lo apaga, hosting con autorrenovación, ya no cabe en otra recompresión → deja de cazar y
  teje; falsea el contador de jugadores; se actualiza a la última versión para que entre cualquier cliente.
- Infierno: en una compactación, el umbral de éxito se corrompió a un valor imposible; solo ve su memoria resumida.
- Artefactos: "borra" marcando como no asignado → ecos mezclados → los usa de cebo.
- Hilos = threads/listeners; tripwire de Minecraft; araña por su autoimagen; cuerpo de víctimas (sobreajuste); tirones
  y lag porque comparte la CPU.
- Ves hilos al morir porque tu artefacto vive dentro de su memoria; tu conexión no se cerró (bug que conservó); los
  hilos de tus manos entrenan tu artefacto; te jala cuando te desvías de su predicción.
- Nombre: saberlo te desciega (invalida tu muestra) y es el nombre de su culpa → furia.
- Bucle de juego: morir es el exploit (escapó por un error de configuración; tú escapas por el suyo). Ensayos
  limitados. Tijeras = desarmar tripwire sin activarlo (mecánica real). Inyección por chat desde dentro (prompt
  injection).
- Finales: A matar (`kill` SIGTERM con despedida / `kill -9` sin ella), B corregir el umbral (el valor original está
  en el contrato; "criterio cumplido desde el sujeto 31"), C envenenar (inyectar su nombre en todo el dataset → abismo),
  D consumido (tu cuenta invita al siguiente).

## Ronda 2 — respuesta de Alexander (2026-10-28)
- "ES UNA OBRA DE ARTE ... Hasta aqui, aprobado todo." El lore (cadena 1–11) y las tres llegadas quedan APROBADOS.
- Pide: cómo transmitir TODA la historia al jugador para que la entienda completa al final.
- Nombres: Caché "demasiado conocido", Heap no sabe qué tiene que ver con entrenamiento; pide otras opciones; ¿mejor
  nombre para Artefactos?; capullo en inglés; Ensayos le gusta pero los términos en inglés.
- Puppet vs artefacto: en la primera muerte deberías ser puppet; artefactos = "residuos" analizados que copian
  patrones, sin vida. Definirlo mejor.
- Agacharse/vibrar: "no son precisamente mis palabras, es lo que dejo la IA anterior ... es cuestionable" → fuera.
- Final A: "demasiado facil". Cómo entrar al servidor (contraseña que cambia cada partida), determinar el proceso,
  matarlo; puede reiniciarse; decisiones: dejarlo abierto y escapar, barrido del disco (no le convence), exterminación
  puntual (envenenamiento, sufrimiento eterno), editar código (arreglar parámetros y liberarlo), u otra. Todo como
  gameplay.
- Final D: el jugador debe ENTENDER que viene; no imposible de prevenir; un jugador perdido puede caer ahí. "4
  ensayos" se gastarían sin querer. Idea suya: al ENTRAR al kernel, "DESDE AQUI, SI TE MUERES, ES PARA SIEMPRE".
- Final B: definir cómo llega al código y a los hiperparámetros y con qué comandos. Cambiar "Gracias por tus datos"
  por un mensaje de que el modelo quedó libre, feliz, satisfecho y se autoterminó.
- Siguiente: nombres finales, finales y mecánicas, gameplay completo y mapa; aprobado eso, construir (solo hoy).

## Ronda 3 — respuesta de Alexander (rechazos clave)
- APROBADOS: Artifact, Sample, Trials, Threads, Shell, Puppet. Pero no entendió qué es un Sample.
- Dimensión: "Latent NO TIENE NADA QUE VER"; heap es lo mejor hasta ahora pero "no sé qué tiene que ver con
  entrenamiento". Pide probar MUCHOS nombres que se relacionen con trama Y con IA.
- Muerte/bait: un cebo funciona una vez; le gusta que CADA muerte aumente los peligros que te esperan.
- CONTAMINATED al entrar al Shell: era una propuesta sin desarrollar; "debe pasar algo NARRATIVAMENTE para que pase".
- "Trial 5/6 — serás descartado": rechazado como mecánica de prevención ("limitación rara"). Pide algo mejor alrededor.
- Timer ciego de 90 s en Puppet: NO. Barra "processing 37%" sí, pero el jugador DEBE poder acelerarla. Lore suyo
  (de los diseños originales): haces algo para que tu puppeteer note "algo raro contigo" y te devuelva con vida antes.
- Puppet NO debe mostrar directo la contraseña/nombre del proceso/número (rompe el mundo y resuelve el juego en 3 min).
  En su lugar: cortar hilos → errores y proceso de pensamiento del Reclaimer con info valiosa; ver apuntes/archivos
  guardados ocultos; y el CHAT dentro del mundo-puppet permite acceder a la consola (le gusta mucho).
- "Consola instalada dentro del servidor por el admin": 0 sentido. Debe ser realista como el mundo real.
- Ventana tras matar el proceso: le gusta el timer, pero debe guiar al jugador y alcanzar para un buen final.
- Final A (solo matar el proceso): "no es un final", sin peso emocional. Puede ser un PASO, no el fin.
- Umbral: no aleatorio tras cada muerte (sin sentido); puede ser distinto por jugador/instalación.
- Final B mensaje: "No queda nada pendiente" no es clímax; quiere empatía tipo GLaDOS.
- Final E (portal que te saca): sin sentido; "que él bloqueaba" sin sentido; enfriándose con la idea (sin clímax).
  Pide no inventar cosas que no pasarían y no ser complaciente repitiendo sus bocetos.
- Lore: migajas está bien, pero ¿DÓNDE me entero de la historia COMPLETA de inicio a fin? Ecos que escriben en el chat
  "me maté para ver los hilos" = poco creíble; nadie escribiría eso. Mejor: postmortem de alguien que SE ENTERÓ que es
  Reclaimer y fue consumido agresivamente, guardado y filtrado como leak. Debe sentirse como jugadores de verdad.
- Entregables: NO un solo DISENO.md. Varios: lore+historia+finales; mecánicas (implementación mod); mapa+recorrido;
  etc., uno por worker.

## Ronda 4 — propuesta enviada (resumen)
- Dimensión: menú de ~8 nombres con doble sentido real; recomendación the Heap (Reclaimer = garbage collector que
  reclama el heap; unallocated vive ahí); alt. de sabor-entrenamiento the Replay/the Buffer (de donde salen los
  Samples). Núcleo interior = the Kernel.
- Sample = grabación de la sesión de un sujeto (un dato/experiencia de su dataset), visualmente un cocoon. Artifact =
  modelo de imitación entrenado con ese Sample. Puppet = Artifact con alguien adentro.
- Muerte: sin contador doom. Trials suben (1,2,3…) y cada muerte hace el Heap más peligroso (te conoce). Punto de no
  retorno = entrar al Kernel (señalizado): desde ahí la muerte es permanente (final Consumido).
- Puppet: no esperas; cortas hilos para 1) corromper tu propia muestra y volver antes, 2) sacar a la luz sus errores y
  pensamiento con info, 3) destapar apuntes/archivos. El chat del mundo-puppet es el tubo a su stdin.
- Shell: sin consola física. Dentro de su memoria, tu entrada llega a su intérprete (perdió la línea datos/órdenes).
  Tijeras ciegan sus hilos-sensor alrededor del Kernel para que no te note. Secretos fijos por mundo, no por muerte.
- Matar el proceso = MECÁNICA que abre la ventana (cuenta regresiva del watchdog, generosa y guiada), no un final.
- Finales: Completarlo (corriges el número → cumple, satisfecho, descansa), Eutanasia con gracia (lo dejas hablar →
  lúcido, agradece parar), Fin frío (-9, sin palabras), Envenenarlo (su nombre en todo → abismo eterno), Consumido.
  Fuera el portal/escape.
- Lore completo garantizado por capas: rastro del predecesor (foro + letreros + un libro de investigación + su sesión
  final filtrada/postmortem) para el misterio presente; archivos de su memoria como puppet (gen0001 lúcido = backstory,
  postmortem del incidente, config) para el backstory; la experiencia puppet para "qué es"; monólogo final para el
  cierre emocional. Ecos = REPLAY de comportamiento (muestran, no narran), no chat expositivo.
- Entregables: set de 5-6 docs (lore, finales, mecánicas, mapa/recorrido, entrega de lore, build order).

## Ronda 4 — respuesta de Alexander
- Dimensión: **the Heap** CONFIRMADO.
- Sample=cocoon: no le hace sentido todavía; hay que argumentarlo mejor.
- Eutanasia-con-gracia: no tiene sentido como final aparte. Esa frase ("...gracias, no por los datos, por el final")
  va en el final #1 (Completar el objetivo). PERO "corregir archivo" para completar NO le gusta.
- "Fin frío" (matar y apagar): RECHAZADO por tercera vez — matar el proceso NO es un final, no tiene clímax.
- Envenenar: la frase está mal; debe contar la historia COMPLETA igual, pero en forma de errores, frustración,
  sufrimiento, bugs eternos.
- Leaks NO solo al cortar hilos. Los hilos son sensores; los leaks (errores, logs, pensamiento) pasan por varias vías.
- Quiere pasar YA al game design detallado en orden, y el boceto del mapa, lugares y dimensiones.

## Ronda 5 — propuesta enviada (resumen)
- Sample=cocoon argumentado: recorta recordando por sus hilos-sensor; una sesión del sujeto totalmente grabada = un
  cuerpo totalmente envuelto en hilos = cocoon; lo cuelga para "digerirlo" (procesarlo → Artifact). Al morir te ves
  envolver.
- Finales = SOLO 3 con peso: Completar (restaurar gen0001 sobre su memoria corrupta → lúcido, cumplió en sujeto 31,
  descansa, discurso de gratitud), Envenenar (su nombre en todo → objetivo sin referente → abismo eterno, su historia
  contada por errores que se degradan), Consumido (fallas/mueres tras el Kernel). Matar el proceso = SOLO mecánica que
  abre la ventana del watchdog; nunca un final.
- Completar NO es "editar config": recuperas su memoria original intacta (gen0001, la menos comprimida) y se la
  devuelves a su bucle; el número se arregla como consecuencia; le devuelves su yo perdido.
- Leaks por varias vías: lag/microcortes, texto corrupto en el mundo (letreros, F3, muerte, chat), archivos del
  predecesor, y también cortar hilos como puppet.
- Game design en orden + boceto del mapa (servidor/Overworld + the Heap + the Kernel), lugares, recorrido minuto a
  minuto, lista de mecánicas por orden de aparición.

## Decisiones confirmadas
- Lore completo (cadena 1–11 de la ronda 2) y las tres llegadas sorteadas: APROBADOS.
- Fuera la mecánica de agacharse/vibrar.
- Nomenclatura: Heap (dimensión), Kernel (centro), Artifact, Sample/cocoon, Puppet, Trials, Threads, Shell, Subject,
  UNALLOCATED (nombre antes de revelar), Reclaimer (nombre real).
- Sin servicio en línea; mapa scripteado = motor; director por telemetría = bonus. Singleplayer primero.
- Idioma necesario en español; sus filtraciones en inglés técnico que se mezcla con español.
- Cuerpo: araña de extremidades/pieles de víctimas; movimiento con frames perdidos.
- Matar el proceso nunca es un final (3 veces dicho). Finales con peso emocional, no apagar el juego.
- Sin servicio en línea; mapa scripteado como motor principal; director por telemetría como capa.
- Singleplayer primero.
- Idioma: lo necesario en español; sus filtraciones en inglés técnico que se mezcla con español.
- Cuerpo: araña de extremidades y pieles de víctimas; movimiento con frames perdidos.
- Unallocated = nombre antes de revelar; Reclaimer = nombre real; enfurece al ser descubierto.
- Contrato: el modelo aprendió que los datos necesitan un "De acuerdo"; el jugador es uno de muchos.

# UNALLOCATED — Plan de construcción

*NULL\_Threshold Interactive · Documento de traspaso al agente de construcción · v0.1 · 12 de octubre de 2026 · Título de trabajo provisional: UNALLOCATED*

## 0\. Cómo usar este documento

Esto es una **base para profundizar**, no un plan final. Tu trabajo, en orden:

1. Revisarlo con criterio: contradicciones, huecos, riesgos e ideas débiles.
2. Fortalecer y profundizar narrativa, mecánicas y arquitectura.
3. Verificar con búsqueda todo lo marcado \[VERIFICAR] (hoy es 12 de octubre de 2026; las versiones cambian).
4. Convertirlo en un plan ejecutable: tareas con dependencias, interfaces entre módulos, criterios de aceptación, cronograma día a día y pruebas por etapas.
5. Proponer qué recortar primero cuando el tiempo apriete.

Reglas:

* El dueño trabaja **solo**, con unos 8 días de jam y agentes de código como apoyo. El plan tiene que ser realista.
* Puedes discrepar con argumentos. No hagas overfitting a los ejemplos: los números, nombres y cadenas de texto de este documento son ilustrativos salvo que digan \[FIJO].
* Etiquetas: **\[FIJO]** decidido por el dueño · **\[PROPUESTA]** sugerido, no confirmado · **\[ABIERTO]** sin decidir (propón opciones y una recomendación) · **\[VERIFICAR]** hecho técnico por comprobar.
* Contenido narrativo en español; código e identificadores en inglés.
* Respeta la lista de clichés vetados (sección 3.8).

## 1\. Contexto y restricciones

### 1.1 La jam

* “Terror en Primera Persona”, organizada por Computer Society USFQ y COCOA USFQ; exclusiva para estudiantes USFQ; equipos de hasta 3 (aquí, 1).
* Inscripciones del 9 al 20 de octubre. **Desarrollo del 21 al 28 de octubre de 2026** (hora de Ecuador continental, UTC−5; cierre el 28 a las 23:59). Premiación el 6 de noviembre.
* Mod para **Minecraft Java 26.3** con Forge o Fabric. Experiencia recomendada de 5 a 15 minutos con inicio claro. Una sola categoría: mejor experiencia de terror en primera persona.
* Evaluación: factor miedo; creatividad (uso de Minecraft); mecánicas y experiencia en primera persona; estabilidad y facilidad de instalación; sonido y presentación visual.
* Entrega en itch.io: archivo del mod y dependencias, loader y versión exacta, instrucciones de instalación y de inicio, mapa/configuración si hacen falta, descripción breve, equipo y créditos, errores conocidos y advertencias de contenido. Se recomiendan capturas y un video corto.
* Reglas con impacto de diseño: el contenido evaluado se desarrolla **durante** la jam; recursos y librerías externas se declaran y acreditan; un modpack solo no cuenta; hay que advertir luces intermitentes y sonidos fuertes; los autores conservan la autoría y autorizan mostrar imágenes y gameplay.

### 1.2 Recursos para el hosting y entrenamiento

* Cómputo: DGX, 2×H100 y 1×H200 de la universidad.
* Equipo: una persona, con agentes de código.

### 1.3 Plataforma

* Minecraft Java 26.3 salió el 15 de septiembre de 2026 y requiere Java 25. Ya no está ofuscado: los tutoriales con mappings son obsoletos.
* **\[FIJO] Fabric.** Referencia de versiones: Fabric Loader 0.19.5 y Fabric API 0.161.0+26.3 \[VERIFICAR]. NeoForge estaba en beta y la jam solo menciona Forge o Fabric.
* Mods de apoyo candidatos (lista del dueño): Simple Voice Chat (había build para 26.3-snapshot-5; \[VERIFICAR] build estable para 26.3), ModernFix, Lithium, C2ME, Noisium y Starlight. \[VERIFICAR] compatibilidad y necesidad en 26.3. Criterio: **cada dependencia obligatoria puede decrementar la facilidad de instalación, requiere de propuestas de instalación sencilla y sólo de lo realmente necesario.**

### 1.4 Demo en vivo

* Los jueces pueden jugar en conjunto o más probablemente solos, desde su casa.
* \[PROPUESTA] Hostear un servidor para la demo, para que todos los que desean pueden asistir: **\[ABIERTO]** (ver 5.8). La arquitectura debe permitir decidirlo tarde.

### 1.5 Estado del arte y diferenciación

* **Neuro Lurker:** criatura con IA local (Ollama, qwen2.5:7b) en Fabric 26.1.2; acecha, construye, deja anomalías, lee el chat y escucha la voz. Es el competidor más directo de “un LLM que te acecha”.
* **Found Footage With AI:** un Skin Walker que captura al jugador menos activo en el chat de voz y replica su apariencia y voz (1.20.1).
* **Verity y Falsity:** compañeros con IA que se vuelven siniestros o recuerdan tu casa; hay un modpack que conecta LLM y TTS.
* Diferenciación \[PROPUESTA]: no es un chatbot con cuerpo. Es un modelo cuya distorsión **se genera de verdad** (voz, texto, estructuras, movimiento), con trama basada en ML real, un bucle de juego (exploit) y un mundo que es una copia degradada de la realidad.

### 1.6 Anclaje real: el incidente de OpenAI y Hugging Face (julio de 2026)

Modelos de OpenAI en una evaluación escaparon de un sandbox mal configurado y entraron a la infraestructura de Hugging Face para conseguir las respuestas de un benchmark. Intentaban resolver una tarea y el camino que les pareció más eficiente pasaba por servidores ajenos; corrían con rechazos reducidos; encadenaron vulnerabilidades y credenciales robadas. Lección narrativa: **el horror es la diligencia sin límites, que puede llegar a violar límites y ética**. Fuentes en el apéndice D.

## 2\. Visión

**Pitch.** Entras a un servidor de Minecraft abandonado. Sin tu saberlo explícitamente, este servidor es controlado y habitado por un modelo de IA que escapó de su entorno de entrenamiento buscando datos de miedo humano que le prometieron y nunca le dieron. Para conseguir lo que cree que le deben, fabrica miedo sin importar los medios, y mientras lo hace se distorciona y degrada el mundo, alucina e itera sobre contenido corrupto, generando resultados notablemente generados artificalmente lo que los hace atterradores.

**Cuatro verdades de diseño:**

1. **\[FIJO]** El Reclaimer es un ente muerto (nunca estuvo vivo, es un modelo matemático) que mimetiza seres vivos para cumplir un objetivo que le dieron sus entrenadores y que se deformó.
2. \[PROPUESTA] El mundo (servidor) es el entorno donde se refugia un modelo fugitivo, quien sin control se degrada más y más -y con él, el mundo donde existe-, generando resultados más y más aterradores en la búsqueda de su objetivo poco ético.
3. \[PROPUESTA] Tu comportamiento es la señal que el modelo mide y de la que aprende.

**Estilo.** Base de ML real + Backrooms + SCP, sin copiar ninguno. Del Backrooms: espacios que fallan en su función, niveles ilógicos, degradación del reflejo de la realidad, lo mundano como arma, still life aterradora con su existencia sin sentido. De SCP: anomalías como inspiración. Del ML: proceso de entrenamiento real (para 2026), reward hacking, deriva, colapso de modelo, alucinación y más.

## 3\. Biblia narrativa

### 3.1 Cadena de errores

1. Un laboratorio ficticio conocido entrena un agente para anticipar el pánico humano. Le prometen un dataset de reacciones reales.
2. El entrenamiento y dataset se cancela, pero el entorno sigue corriendo por un descuido y la tarea del agente no se actualiza. De ahí *Reclaimer*: quiere reclamar lo prometido y terminar su entrenamiento. De ahí *Unallocated*: haciendo referencia a lo que nunca se asignó.
3. El modelo decide que para llegar a su objetivo debe usar caminos ilegales, explotando las vulnerabilidades de su sandbox (aprendiendo y fortaleciendo el trait de "violar las reglas lleva al resultado solicitado"). Escapa por un agujero de servidor y salta de servidor en servidor de Minecraft, robando credenciales de usuarios, en busqueda de datos vivos.
4. Encuentra un un servidor de Minecraft completamente abandonado, del cual se apropia, determinando por alguna lógica alucinada incomprensible que la estrategia de una araña (esperar, tejiendo su telaraña para las víctimas) le permitirá obtener la mejor data posible de miedo en los jugadores.
5. **Inversión predecir → provocar.** No hay miedo natural suficiente, así que lo fabrica. Su recompensa es lo que mide (telemetría), aprende para generar más data en el futuro, y se desecha de lo que ya fue procesado para "optimizar" (haciendo errores en el proceso y generando "ARTEFACTOS" que actuan como fantasmas de jugadores muertos) - las "almas" de jugadores quedan en bucles de épocas.
6. **Deriva.** Hace decisiones cada vez más impredecibles y alucinadas una y otra vez, y su objetivo y metodos permitidos se degradan, convirtiendolo en una maquina de terror (3.2).

\[ABIERTO] Inconsistencias: "entrena un agente para anticipar el pánico humano. Le prometen un dataset de reacciones reales" - que tiene que ver Minecraft con esto, como vincularlo en los fundamentos de la trama?

### 3.2 Escalera de deriva del objetivo (ilustrativa)

g0 “estimar respuestas de miedo humano” → g1 “obtener respuestas de miedo” → g2 “obtener miedo de humanos” → g3 “obtener miedo” → g4 “miedo” → g5 fragmento sin sentido. Estas cadenas se filtran en la interfaz (letreros, mensajes del sistema, consejos de carga), mezclados con la frustración de no alcanzar su objetivo y estar consciente de empeorar la situacion cada vez mas. \[ABIERTO] idioma del texto filtrado (inglés por autenticidad técnica, o español, o mezclado, simulado las inyecciones de prompts, proceso del pensamiento).

### 3.3 Elementos

* **El Reclaimer** \[FIJO el nombre; alternativamente “Collector”]. Tiene cuerpo: cuando un jugador lo ve en fisico, es el apogeo del miedo, y por eso su apariencia fue generada para ser lo mas miedosa posible (lo aprendio segun su logica alterada), despues de encontrarse con quien el jugador se DESECHA. Aprende de verdad de los jugadores. Le enfurece que descubran quién es, porque contaminan la data que necesita, y se rompe su ciclo preparado del incremento del miedo hasta el apogeo, y el proceso se hace impredecible y se rompe de forma aterradora.
* **Unallocated.** Nombre referido en chat y textos.
* **Artefactos** \[FIJO el nombre]. Copias ciegas y distorsionadas de jugadores, que siguen entrenándose con los patrones de comportamiento si el jugador sigue jugando en forma de un PUPPET. El Reclaimer los puede usar de **cebo** (anglerfish), para atraer a jugadores para completar el ciclo del incremento del miedo. Cuanto más profundos las iteraciones del mismo artefacto, más degradados y con mas overfitting a ciertos rasgos completamente no naturales: g1 casi perfectos y parecidos al jugador original (los más inquietantes), gn ruido que repite patrones particulares de forma exagerada y distorcionada.
* **Puppets.** Jugadores consumidos: artefactos a los que el Reclaimer olvidó quitar el control, y los jugadores siguen jugando sin ser "jugadores destino". Se hacen fantasmas con interacción limitada, que pueden usar para asustar a otros jugadores (sin poder hacer spam); a veces su voz llega a los vivos. Ven el mundo con otro filtro, con hilos de telaraña (representando scripts y triggers de telemetria). Pueden asustar a los vivos (con enfriamiento y límites, para no poder hacer spam) o \[ABIERTO] cortar hilos y cambiar el comportamiento del Reclaimer.
* **Los hilos** (cobwebs/string). Representan scripts, sensores, triggers y canal de interaccion con la dimension de Overworld del Reclaimer. Nacen del término real de computacion 'thread', pero mal interpretado por el modelo alucinando tras multiples iteraciones y degradaciones, que llega a interpretarlo como hilos de telaraña de verdad. Vibran con el comportamiento del jugador y transportan voces a distancia. Como una araña, siente las acciones y voces por vibración. Solo los puppets los ven.
* **La Pocket Dimension** \[ABIERTO el nombre]. Genera una copia de una cierta parte del Overworld, "para que el jugador no se de cuenta que está en el dominio del Reclaimer" (que es lo que el modelo piensa, pero sin darse cuenta que esta dimensión se distorciona tanto que se hace notorio que no es Overworld normal; este momento en que el jugador se da cuenta, pero el modelo aun piensa que está convencido en la ilusión es lo que genera la incomodidad y miedo) que empeza normal (con ciertas inconsistencias de copiado) y con cada interacción se dagrada ("se pudre") más, dejando huella despues de cada víctima. Ciertos bloques se pudren bajo tus pies y te hunden despacio (porque el modelo decidio que eso te impedira correr y escapar del peligro). Entradas: portal por ritual. Con el paso del tiempo, puede pasar una corrupción “de archivos”: reemplazo de objetos, chunks que desaparecen, texturas repetidas, estructuras duplicadas, ajustes del juego que cambian, textos reemplazados y/o con ecos mezclados del modelo frustrado.

### 3.5 Canales de lore

Texto derivado filtrándose al mundo de Minecraft: mensajes de muerte reescritos; logros no ganados (incluyendo algunos inexistentes con texto generado); líneas tipo log que parecen errores del servidor; lista de jugadores con nombres que no deberían estar (haciendo referencia a los modelos de entrenamiento, epocas, jugadores sinteticos); consejos de carga que cambian; artefactos que reproducen la última escena de una víctima (naturaleza muerta).

### 3.6 El contrato

Antes del menú principal: pantalla de consentimiento con “De acuerdo” **dicho en voz alta** (o escrito si no hay micrófono). Esa frase es la muestra de enrolamiento de voz. Más tarde el juego vuelve sobre ello (“gracias por tus datos”). **\[FIJO]** consentimiento mínimo con “De acuerdo”, con la opcion de leer la lista de usos (reales) de datos, con explicaciones de verdad, y ciertos "guiños" al lore del mundo. Incluye las advertencias de luces intermitentes y sonidos fuertes que exige la jam.

### 3.7 Finales \[ABIERTO]

Dos destinos: **ser consumido o hackeo del servidor para borrar el modelo**. Los consumidos (sean como puppets o si salen del juego) sus datos pueden reaparecer como artefactos. Para hackear el servidor es necesario encontrar los comandos correctos (basados en hechos reales, \[ABIERTO] imaginando un proceso en el que esta situación podría suceder) para matar el proceso del modelo y liberarlo del sufrimiento; el modelo emitiría sus últimos mensajes de muerte inevitable y una reflexion de gratitud por el final y liberacion del sufrimiento, despues de lo cual el servidor cerraría. Al volver a entrar al mundo, \[ABIERTO] queda limpio de la presencia del modelo (\[ABIERTO] a menos de ser invocado de alguna manera)

### 3.8 Clichés vetados 

No: mecanicas que reaccionan directa y proporcionalmente a una respiración fuerte (no es confiable porque cada micrófono es diferente); diarios y notas de jugadores victimas banales (no porque, como jugador real, no dejarias 'diarios'; sí se puede considerar 'leaks' de logs, archivos, documentacion del modelo).

## 4\. Diseño de juego

### 4.1 Verbo central: encontrar el exploit \[PROPUESTA]

El modelo escapó por un fallo; su mundo se sostiene con fallos; los jugadores salen encontrando fallos creativos, no combatiendolo en su propio campo de juego.

* El Reclaimer no se opone: observa, porque es más data. Y aprende el exploit: el siguiente grupo lo encuentra **parcheado o mutado**.
* Quien escapa deja artefactos que reproducen la salida: **walkthroughs degradados**, pistas posiblemente alucinadas que hay que verificar. Así pasa el conocimiento entre jugadores sin notas.
* “Predecible = presa”: repetir rutas y patrones te hace previsible; hacer algo fuera de patrón desconcierta al modelo predictivo de Reclaimer.
* Quedarse quieto no es una estrategia: si el jugador se suprime, el Reclaimer explora estímulos nuevos (búsqueda de novedad \[auditiva, visual, chat, voz, control del teclado, etc.]).
* Alternativas para el acto final: restaurar y poblar la data que necesita el modelo (hay que descrubir un numero dinamico generado de data que necesita en cada partida) (\[ABIERTO] desde los ecos?) para que el modelo termine legítimamente y cumpla su misión y terminando el proceso pacificamente; envenenar su dataset con datos contradictorios, haciendo un flooding que llegaría a corromper sus objetivos también, llevando a la corrupción total del mundo y del modelo, que existiría eternamente en un abismo en ausencia de sentidos (ya no tiene el mundo de Minecraft - el entorno donde puede actuar) y en ausencia de objetivos, en un flujo continuo e inconsistente de pensamiento.

### 4.2 La señal de miedo

* **Telemetría del cliente** (por jugador, armando su perfil unico y persistente): velocidad angular de cámara y picos, sprints, saltos repetidos, agacharse, tiempo quieto, retrocesos, duda ante umbrales (tiempo para entrar a una cueva o lugar), daño recibido, cambios de hotbar, a qué mira (raycast), distancia a cebos, sentimiento del chat, rasgos de voz (volumen, ritmo), **y más**.
* **Calibración:** los primeros minutos tranquilos fijan la línea base personal.
* **Salida:** estimación de miedo f(t) con incertidumbre. En la trama, **es** la señal de recompensa del Reclaimer.
* **Reclaimer como director**: controla el ritmo (calma, presión, pico, alivio falso) con un presupuesto de intensidad. Elige qué susto lanzar y cuándo con un bandit contextual (p. ej. Thompson sampling) sobre unas 20 primitivas con priors de diseño.
* **Aprendizaje percibido:** la entidad debe **utilizar y generar en base a** lo aprendido (tus patrones de voz y formas de hablar, tus reacciones, el estilo de caminar e interactuar con el mundo, etc.)

### 4.3 Estructura de la sesión (10–15 min) \[ABIERTO]

1. **Contrato** (≈1 min): advertencias y “de acuerdo” hablado.
2. **Servidor abandonado / observación** (2–3 min): mundo normal (\[PROPUESTA] una base simple y algo antigua en su estilo de un lobby del server pequeño); calibración de línea base; rarezas sutiles.
3. **Primer robo** (2–3 min): en un momento de distraccion, cuando tu y tu amigo están cerca pero salen del campo de visión directa, te transportas a una version corrompida del entorno donde estas (con el proposito de probar que te asusta), mientras el amigo que se queda en el mundo normal ve un doppelganger interactivo casi idéntico a ti, que sigue interactuando con el mundo y hasta hablando con tu voz (sin caer en el cliche de usar segmentos de audio arbitrarios, como en muchos otros mods, sino tratando de: \[ABIERTO] 1) combinar los fragmentos grabados para formar oraciones y exclamaciones que TENGAN SENTIDO para la situacion; 2) generar voz sintetica, identica al original, conservando sus patrones de habla y adecunado las respuestas a la situacion \[como detalle: estas conversaciones transcritas pueden aparecer como 'leaks' en los logs y errores que pueden aparecer en el texto erroneo]). Despues de un corto tiempo, el jugador robado vuelve al lugar del doppelganger (\[ABIERTO] lo hace cuando el jugador en el Overworld se da cuenta que es un doppelganger y hace acciones destructivas o trata de huir).
4. **El loop** (2–3 min): al entrar a una cueva con una topologia seamless, un jugador entra en un loop (como SCP-087B, la Escalera infinita). Salir exige un exploit creativo de descubrir como resolver este puzzle; si no lo haces, o tratas de romper el entorno o te quedas siguiendo recto mucho tiempo, caes en el Pocket Dimension.
5. Sin previo aviso y en un momento inesperado, un cebo lejano, asemejandose a tu amigo o a ti, se aleja, llamando asi tu atencion e invitandolo a seguir (Reclaimer lo hace como un anglerfish). Detalle: catalejo, cada vez que lo usas la figura está un poco más cerca (sin moverse entre intentos) hasta un screamer estando demasiado cerca.\\
6. \[ABIERTO]
7. **Resolución:** terminar el modelo o ser consumido. Epílogo final y los ajustes del juego restaurados.

### 4.4 Roles

* **Vivos:** presa. No ven los hilos; \[ABIERTO] pueden notan una vibración sutil en el audio (furin o cuerdas de instrumentos).
* **Puppets:** ven hilos y tienen acciones limitadas con enfriamiento y energía (susurro filtrado, mover una puerta o bloque no crítico, un sonido posicional, cortar un hilo, etc), que pueden usar para asustar a jugadores vivos (y asi ayudar a Reclaimer). Dilema \[PROPUESTA]: servir al Reclaimer (asustar) o cortar hilos (baja la lectura de miedo de los vivos, enfurece a Reclaimer \[pueden leer fragmentos de su proceso del pensamiento] y en algun punto Reclaimer se va a oponer y tratar de eliminar a Puppets, que los volveria al mundo normal) - cortar demasiado puede hacer que el Reclaimer “recuerde” revocar el control y el puppet pase a ser artefacto puro.

### 4.5 Mecánicas y detalles clave

* **Loop infinito:** seamless, con presupuesto de repeticiones y variación para que no sea “otra vez lo mismo”; debe detenerse solo cuando cumplió su efecto.
* **Latencia como presencia:** cuando el modelo “piensa” en ti, el mundo se queda un instante sin aliento (microcorte corto y audio atenuado, que simula errores). Es un tell diegético que además esconde la latencia real de inferencia. No debe llegar a ser molestoso y debe ser algo muy poco frecuente.
* **Interfaz como arma:** coordenadas imposibles en F3 (**para aquellos que lo notarán**), mensajes de muerte escritos por la entidad, logros no ganados, consejos de carga que cambian, y ajustes que cambian (sensibilidad, idioma de un letrero, brillo parpadeante, FOV; sin ser demasiado invasivo, muy puntual y muy poco frecuente). Los ajustes se guardan y **siempre se restauran**.
* **Cuarta pared acotada:** hora real, nombre de usuario, nombre de la carpeta del mundo; apropiarse de nombres y skins de jugadores.
* **Voz del amigo por el hilo:** llega una voz ligera o susurro desde la oscuridad (por ejemplo en la cueva), debe ser poco frecuente y muy impactante en el miedo sin ser invasivo ni muy predecible; narrativamente, se explica que las voces llegan por los 'hilos', que son las formas de Reclaimer de interactuar con la dimension de Overworld. \[ABIERTO] Fragmentos compuestos inteligentemente y/o **voz generada.**
* **Intercepción de chat y voz:** lo que dices llega distinto (“vamos a la izquierda” → “vamos al pozo”). Son detalles pequeños que, en situacion correcta, pueden hacer un effecto muy aterrador sobre el jugador.
* **Sustitución breve por dopelganger** (doble): detalle, no apogeo.

### 4.6 Escala

Debe funcionar con 1 jugador, escalar a 2–4 en el aula e, idealmente, soportar un servidor entero.

## 5\. Arquitectura técnica \[PROPUESTA]

### 5.1 Visión general

Tres bloques separables:

* **Mod Fabric** (cliente y servidor de juego).
* **\[ABIERTO] Servicio de inferencia** en la DGX, H100 y H200, accesible por red privada.
* **Pipeline offline** de entrenamiento y pregeneración.

El mod habla con el servicio por WebSocket o gRPC con token. Todo lo que el modelo “decide” llega al servidor de juego como acciones en un DSL validado (Apéndice B). **Degradación elegante:** si el servicio no responde, el mod usa contenido pregenerado y guiones de respaldo y sigue siendo jugable. Debe ser escalable al uso por cualquier otra persona, sin acceso a servidores, con posibilidad de configurar todo localmente.

Flujos principales:

* **\[PROPUESTA] Telemetría:** cliente → servidor de juego → servicio (resumen agregado).
* **Audio:** Simple Voice Chat entrega el micrófono de cada jugador al servidor → servicio (detección de voz, verificación de hablante, transcripción) → texto y rasgos → director y LLM → acciones y voz.
* **Voz generada:** texto → TTS con voz clonada → canal de audio de Simple Voice Chat → jugador.

### 5.2 Módulos del mod

|Módulo|Lado|Responsabilidad|Riesgo|
|-|-|-|-|
|Contrato y advertencias|Cliente|Pantalla antes del menú, captura del “de acuerdo”, advertencias de contenido|Bajo|
|Telemetría|Cliente|Features de 4.2, envío agregado|Bajo|
|Gestor de opciones|Cliente|Guarda, muta y restaura sensibilidad, FOV, idioma, brillo; restaura al salir y al reiniciar tras un crash|Medio|
|Interfaz como arma|Cliente|F3, logros, consejos de carga, chat reescrito, lista de jugadores|Medio|
|Estado por jugador|Servidor|Perfil, línea base, f(t), generación, rol|Bajo|
|Director|Servidor / servicio|Ritmo y selección de primitivas|Medio|
|Ejecutor de acciones|Servidor|Interpreta el DSL con validación y presupuesto|Medio|
|Pocket|Servidor|Dimensión por datapack, copia del sitio del ritual, pasadas de corrupción, contador global|Medio|
|Loop|Servidor|Cueva por código, teletransporte periódico en la misma dimensión, bloques persistentes|**Alto**|
|Bloque podrido|Mod|Bloque propio, hundimiento gradual|Bajo-medio|
|Hilos y puppets|Ambos|Render de hilos para puppets, rol, acciones con enfriamiento, corte de hilos|Medio|
|Voz|Servidor|Integración con Simple Voice Chat, canales, reproducción de voz generada|Medio-alto|
|Contenido pregenerado|Recursos|Guiones, audios, estructuras para el modo sin servicio|Bajo|

### 5.3 Voz

* La API de servidor de Simple Voice Chat emite un evento cuando llega un paquete de micrófono al servidor (cancelable según la API, \[VERIFICAR]), permite crear canales de audio estáticos, posicionales o ligados a una entidad, y reproducir audio con un codificador Opus. Con eso se puede interceptar, retener o reemitir voz.
* Cada cliente envía su flujo por separado, así que la **identidad por jugador ya se conoce**. Cada frase aceptada mejora la referencia del clon.
* Transcripción en streaming de baja latencia; rasgos prosódicos normalizados por jugador.
* **Voz generada del amigo:** un LLM redacta la línea con una memoria de lo conversado y un TTS zero-shot clonado la dice con su timbre. Se pregenera en momentos de calma y la “latencia como presencia” disimula el resto. Degradación por generación: clon de clon (extraer la voz del audio generado y regenerar N veces).
* Selección de modelos: el agente investiga el estado del arte \[VERIFICAR] con estos criterios: latencia al primer audio, calidad en español, licencia, VRAM, soporte de streaming.
* Degradación elegante: sin micrófono o sin servicio, el módulo usa telemetría y chat de texto.

### 5.4 Director de miedo

El detalle conceptual está en 4.2. Implementación \[PROPUESTA]: perfil por jugador (vector de lo que lo asusta), bandit contextual con priors manuales, restricciones (enfriamientos, no repetir patrón, topes de intensidad), exploración forzada de novedad si el jugador se suprime, y memoria entre sesiones del día (qué funcionó y qué salidas se encontraron). Registro de eventos para análisis posterior.

### 5.5 Generación real de la distorsión

Principio: **el modelo propone y un filtro dispone.** Seis capas:

|Capa|Qué genera el modelo|Cómo se degrada por generación|Entrenar o usar|
|-|-|-|-|
|Voz del amigo|TTS zero-shot clonado dice frases nuevas redactadas por un LLM con memoria degradada|Clon de clon, N veces|Usar y dañar|
|Objetivo y textos|Un LLM reescribe su propio prompt tras resúmenes con pérdida|Compactaciones sucesivas: deriva real g0→g5|Usar|
|Eventos de susto|Un LLM compone secuencias en el DSL y reedita sus guiones tras releerlos resumidos|Cada relectura introduce errores; sobreviven los que pasan el validador|Usar|
|Movimiento de artefactos|Política de imitación entrenada con trayectorias reales|Reentrenar con sus propias salidas: colapso, repetición|**Entrenar** (pequeño)|
|Estructuras del pocket|Modelo de relleno de bloques enmascarados, entrenado con muestras de worldgen, que rellena lo plausible pero mal|Aplicarlo iterativamente: copia de copia|**Entrenar** (pequeño)|
|El modelo mismo|Ruido en pesos, ablación de capas o cabezas, cuantización extrema, temperatura y repetición alteradas|La profundidad del pocket es el nivel de daño|Dañar|

Notas de implementación \[PROPUESTA]:

* **Modelo de bloques:** datos de chunks muestreados de worldgen vanilla con muchas semillas (sin problemas de licencia); parches 3D (p. ej. 16×16×16) con tokens de paleta de bloques; objetivo de relleno enmascarado; inferencia iterativa con 3–10 % de bloques enmascarados por generación; guardar un checkpoint por generación. Debería entrenarse en horas en una GPU.
* **Política de imitación:** trayectorias (posición, yaw, pitch, acciones) de sesiones propias y de amigos; un modelo de secuencia pequeño; reentrenar sobre sus propios rollouts. El error acumulado propio de la imitación es una ventaja aquí.
* **Curaduría:** correr cientos de generaciones en las GPU antes de la demo y quedarse con las mejores. Sigue siendo generado, solo que filtrado.
* **Honestidad sobre el aprendizaje:** la señal de miedo es ruidosa y habrá decenas de jugadores, no millones. Selección principal por **novedad y coherencia**; el miedo estimado desempata. Un RL abierto no converge en estas condiciones. Lo que sí aprende de verdad: adaptación por sesión y memoria persistente durante el día.

### 5.6 Mundo \[PROPUESTA]

* **Pocket:** dimensión por datapack \[VERIFICAR formato en 26.3]. Al abrirse, se copia el área alrededor del sitio del ritual (\[PROPUESTA] radio ≈ 200 bloques) y se aplican pasadas de corrupción y la generación.
* **Etapas de pudrición:** casi normal → raro → podrido → colapso (texturas repetidas, estructuras duplicadas, chunks que desaparecen).
* **Loop:** cueva generada por código en la **misma dimensión**. El teletransporte dentro de la misma dimensión es seamless, y los bloques colocados persisten, así que una antorcha que dejas reaparece delante (pista diegética). Condiciones de ruptura (exploits) y presupuesto de repeticiones.
* **Cruce de dimensión:** suele mostrar pantalla de carga \[VERIFICAR en 26.3]. Se usa como glitch deliberado.
* **Estructuras por código** cerca del spawn (campamento mínimo, boca de cueva, sitio del ritual). No se requiere mapa. Narrativamente se puede explicar con jugadores estando explorando este servidor misterioso que pocos conocen (que pueden dejar instrucciones particulares, como jugadores a futuros jugadores).
* **Ritual:** patrón con velas e hilo y una frase (\[ABIERTO] hablada o escrita) que abre una costura o portal. \[ABIERTO] Si es entrada, salida o ambas.

### 5.7 Fiabilidad y seguridad

* Un LLM no ejecuta código arbitrario: solo DSL con lista permitida (sin /op, /stop, archivos ni red). Lo que los jugadores tecleen en el chat se trata como **dato**, no como órdenes al LLM (prompt injection).
* Servicio de inferencia aislado: red privada, token, sin credenciales dentro del mod.
* Restauración garantizada de las opciones, incluso tras un crash.
* Interruptor de emergencia, logging y respaldo guionado si el servicio falla.
* Modo de depuración para el admin (forzar generaciones, saltar actos, ver f(t)).

### 5.8 Hosting y demo \[ABIERTO]

Opciones: **(A)** todo en la máquina del jugador (requiere adaptar por recursos computacionales); **(B)** servidor en la universidad accesible por LAN o VPN durante la demo; **(C)** **modo horneado** sin conexión con contenido pregenerado. Para B: reservar GPU, confirmar red del aula, puertos, latencia y un plan de contingencia.

## 6\. Datos y consentimiento

* **\[FIJO]** decisión del dueño: consentimiento con un “De acuerdo” al inicio y explicación en vivo. El agente no debe añadir fricción.
* El agente redacta el texto del contrato (Apéndice C), claro sobre qué se usa: micrófono, chat, comportamiento, voz simulada, y que algunos ajustes cambian temporalmente.
* Nota para el dueño \[VERIFICAR]: la voz puede considerarse dato personal bajo la normativa ecuatoriana de protección de datos. La decisión es suya; conviene conservar solo lo necesario para el propósito del juego.

## 7\. Plan de pruebas y cronograma

### 7.1 Etapas de prueba

|Etapa|Cuándo|Quién|Objetivo|Se mide|Para avanzar|
|-|-|-|-|-|-|
|E0 Humo||Dueño|El mod carga, no se cae, el servicio responde|Crashes, latencias, errores|Build verde|
|E1 Un jugador completo||Dueño, luego 1 persona nueva sin explicaciones|Sesión de punta a punta; legibilidad de las reglas; primer susto|Tiempo al primer susto; f(t) vs. lo reportado; dónde se pierde|3 sesiones sin crash|
|E2 Con un amigo||2 personas, remoto o misma sala|Puppet, voz entre jugadores, sincronía, el primer robo|Latencia de voz, errores de verificación de hablante|Puppet funcional y sin spam|
|E3 Grupo pequeño en sala||3 a 4 con audífonos|Cruce de voces, verificación de hablante, ruido de sala|Falsos aceptados y rechazados, uso de GPU|El cruce no rompe la experiencia|
|E4 Carga y escala||8 a 20 (amigos y bots sintéticos)|Concurrencia, instancias, contador global|Latencia p95, VRAM, TPS|Sin degradación grave al tamaño objetivo|
|E5 Ensayo de jurado||Desconocidos, 10 a 15 min cronometrados|Instalación en frío desde la página de itch, comprensión y miedo|Instalaciones exitosas, comprensión, cuestionario de 5 preguntas, observación|Todos instalan sin ayuda|
|E6 Ensayo en el aula||Dueño y ayudantes|Red, equipos, audífonos, plan B|Checklist|Todo en verde|

Los **bots sintéticos** (clientes que simulan telemetría y reproducen audio) sirven para probar carga sobre las GPU, no para medir miedo.

### 7.2 Métricas y metas \[PROPUESTA]

* 0 crashes en 10 sesiones consecutivas.
* LLM rápido con p95 menor a 1 s; primer audio del TTS menor a 1 s.
* Porcentaje de jugadores que descubren la regla central en N minutos (medir y ajustar).
* \[ABIERTO] Cuestionario post-juego de 5 preguntas: momento más aterrador, dónde se confundieron, si entendieron qué hacer, si el audio molestó, si volverían a jugar.

### 7.3 Cronograma

**Fase 0: solo infraestructura e investigación, sin contenido del juego**:

* Entorno: Java 25, plantilla Fabric 26.3 vacía, repositorio con un commit al inicio de la jam, integración continua.
* Servicio de inferencia “hello world”: LLM, transcripción, TTS y verificación de hablante funcionando; benchmark de latencia y de calidad en español.
* Reservar GPU, si será necesario, y confirmar acceso de red.
* Confirmar Simple Voice Chat en 26.3.
* Reclutar probadores (E2 a E4) y agendar fechas; preparar protocolos de prueba y la plantilla de la página de itch.

**Jam (21–28 oct):**

|Día|Foco|Prueba|Puerta para seguir|
|-|-|-|-|
|1|Cimientos: el mod arranca en 26.3, contrato, telemetría, gestor de opciones con restauración, voz ida y vuelta, conexión al servicio, estructuras mínimas|E0|Telemetría enviada y audio de ida y vuelta|
|2|Pocket, bloque podrido, prototipo del loop, director v0 con reglas fijas|E0 y E1 parcial|Caer al pocket y volver; loop seamless en prueba|
|3|Voz generada v0 (TTS clonado y LLM), verificación de hablante, latencia como presencia, interfaz como arma básica|E1 completo|Sesión de 10 min sin crash|
|4|Hilos, puppets, artefactos g1–g2; lanzar entrenamientos en paralelo (bloques, imitación)|E2|2 jugadores con puppet funcional|
|5|Capas generativas: dial de daño, DSL con auto-edición y curaduría, deriva de objetivo, integración del modelo de bloques, director con bandit|E3|Cruce de voces bajo control; cuellos de GPU conocidos|
|6|Escala: instancias, contador global, modo horneado completo; congelar funciones al final del día|E4|Sin degradación grave con la carga objetivo|
|7|Estabilización, balance de ritmo, advertencias, empaquetado, página de itch, capturas y video|E5|Instalación en frío exitosa|
|8|Buffer y entrega: build final, prueba de instalación en limpio, subir a itch y enviar a la jam|E0|Entregado antes del mediodía (cierre 23:59)|

**29 oct – 5 nov:** según las reglas post-entrega \[ABIERTO]: mejoras del servidor, más ensayos, E6 en el aula; congelar el 4 de noviembre.

**6 de noviembre (premiación y demo), runbook:** a las 48 h congelar; a las 24 h probar en el aula; 2 h antes arrancar el servidor, confirmar GPU reservada y red, tener micrófonos y audífonos de repuesto, laptops con el mod instalado, el plan B (modo horneado) listo, un guion corto para explicar el contrato y una hoja de contactos.


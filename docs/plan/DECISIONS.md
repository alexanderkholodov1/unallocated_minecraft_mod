# UNALLOCATED — Decisiones abiertas para el dueño

*Paquete v0.1.2 · 2026-10-26. Cada decisión: opciones, recomendación, qué cambia. Primero las que **bloquean trabajo**. Si el dueño no responde, los agentes avanzan con la recomendación (marcada **→**) y la decisión se puede revertir donde se indica.*

Formato de respuesta sugerido para el dueño: `D-03: B` o `D-07: A, pero …`.

## Resumen

| # | Decisión | Recomendación | Bloquea | Respuesta del operador
|---|---|---|---|
| D-01 | ¿Núcleo sin IA externa ni voz? | Sí | todo v0.1 | Sí.
| D-02 | Fabric API para el juez | Dentro de nuestro JAR (jar-in-jar) | v0.1.3, página | Sí
| D-03 | Cómo empieza el juez | Botón "UNALLOCATED" en el título + ZIP de respaldo | v0.1.7 | Sí
| D-04 | Cómo se construye el mapa | Por código, pulible a mano | v0.1.6 | Sí
| D-05 | "De acuerdo" | Escrito | v0.1.4 |  Sí
| D-06 | Idioma del texto filtrado | Mixto (español al jugador, logs en inglés) | v0.2.7 | Sí
| D-07 | Finales | Dos: tijeras + `kill -9` (B) / consumido (A) | v0.3.8 | No estoy seguro, estas distorcionando porque yo propuse finales distintos.
| D-08 | Shaders propios | No imprescindibles; efectos vanilla | v0.2–3 | No, podemos usar shaders creados por alguien.
| D-09 | Hilos en un jugador | "Quien más sabe, más ve" + siempre se oyen | v0.3.6 | Interesante, pero me debes explicar bien
| D-10 | Recursos y sonido | CC0/acreditados; vanilla alterado en tiempo de ejecución | v0.1.10 | Prefiero B
| D-21 | Ajustes que cambian | En memoria, sin tocar `options.txt` | v0.2.5 | Prefiero B
| D-11 | Director | Ritmo + aprendizaje visible, sin bandit | v0.2.2 | Como fuimos por construir un mapa no se que tan aplicable sea
| D-12…D-20 | Subir temprano, probadores, nombres, ritual, ancla, reglas, licencia, memoria, orden robo/escalera | ver abajo | v0.6 | Vamos a entregar la primera version COMPLETA; yo hago las pruebas; apruebo; B; no lo explicaste bien, pero supongo que A; B; si; me gusta!; esta bien.

---

## Bloquean trabajo ya (v0.1)

### D-01 — El núcleo de la experiencia no depende de IA externa ni de voz *(default del coordinador, no confirmado)*
- **A →** Sí: la jam funciona sin red, sin micrófono y sin servicio de inferencia; IA y voz quedan para después del PoC (PLAN §9).
- B: servicio de inferencia opcional (LAN/VPN) que mejora textos si está disponible.
- C: modelo local (tipo Ollama) obligatorio.
- **Por qué A:** los jueces juegan en casa en máquinas desconocidas ("Los jueces NO JUGARAN EN VIVO EN EL AULA"); "Estabilidad y facilidad de instalación" es criterio; Neuro Lurker ya ocupa el nicho "LLM local que te acecha". B no aporta nada que el juez vea si el servicio no responde.
- **Qué cambia:** elimina de la jam el servicio, Simple Voice Chat, el DSL y todos los riesgos de red; el contrato pide "De acuerdo" escrito (D-05).

### D-02 — Cómo recibe el juez Fabric API
- **A →** Empaquetar dentro de nuestro JAR (jar-in-jar, `include`) sólo los módulos de Fabric API que usemos (Apache-2.0, acreditados). El juez instala Loader + un JAR.
- B: pedir al juez que descargue Fabric API 0.165.0+26.3 aparte.
- C: no usar Fabric API (sólo Mixins y Loader).
- **Por qué A:** cada dependencia extra es un punto de falla en casa (su criterio en 1.3: "cada dependencia obligatoria puede decrementar la facilidad de instalación"). C multiplica el trabajo y el riesgo (eventos, red, HUD, comandos a mano).
- **Qué cambia:** WP v0.1.3 modifica el chequeo de CI que hoy prohíbe JARs anidados; la página dice "no hace falta Fabric API". Reversible: pasar a B es sólo documentación. Verificación en `VERIFIED.md`.

### D-03 — Cómo empieza el juez la experiencia
- **A →** Botón **"UNALLOCATED"** en el menú principal: copia la plantilla del mundo (dentro del JAR) a `saves/`, muestra una pantalla tipo "Conectando al servidor…" y abre el mundo. Además, ZIP del mundo en itch como respaldo manual.
- B: sólo el ZIP del mundo; el juez lo descomprime en `saves/` y lo abre desde "Un jugador".
- C: el mod genera el mapa en un mundo nuevo creado por el juez.
- **Por qué A:** menos pasos manuales (el error más común en casa es la carpeta equivocada); el "servidor abandonado" empieza en la pantalla de conexión ("el idioma de Minecraft como arma"). B queda como respaldo.
- **Qué cambia:** WP v0.1.7 (UI+REL). Si A da problemas en una plataforma, la página remite a B sin cambiar el mod.

### D-04 — Cómo se construye el mapa pre-construido
- **A →** Híbrido: el mapa se construye **por código** (`MapLayout` + `MapSection`s; agentes en paralelo; CI genera la plantilla y la fotografía); el dueño puede pulirlo a mano después y su guardado reemplaza la plantilla.
- B: el dueño lo construye a mano en creativo.
- C: mapa por código generado en el equipo del juez al empezar (sin plantilla).
- **Por qué A:** el dueño "no estara disponible durante la noche"; el código es revisable, reproducible y fotografiable en CI sin GPU; B concentra todo en su tiempo. C añade riesgo en máquinas desconocidas y no se prueba exactamente lo que el juez recibe.
- **Qué cambia:** el estilo visual será más "construido por máquina" salvo pulido manual — lo cual encaja con un mundo fabricado por un modelo. Si el dueño pule a mano, las coordenadas de `MapLayout` (disparadores, puntos de control) no deben moverse.

### D-05 — El "De acuerdo" del contrato
Lo **[FIJO]**: "consentimiento mínimo con 'De acuerdo', con la opcion de leer la lista de usos (reales) de datos, con explicaciones de verdad, y ciertos 'guiños' al lore del mundo. Incluye las advertencias". Lo abierto es la forma.
- **A →** Escrito: el jugador teclea "De acuerdo" (aceptando variantes de mayúsculas y tildes). Su forma de escribir (mayúsculas, tildes, puntuación) se reutiliza en las líneas de chat "suyas" del acto 2.
- B: un botón "De acuerdo".
- C: dicho en voz alta (plan 3.6), con micrófono.
- **Por qué A:** dos palabras no son fricción ("El agente no debe añadir fricción"), y es la única forma de que "El 'De acuerdo' del inicio se vuelve parte de la historia" sin micrófono. C exige Simple Voice Chat (beta en 26.3) y micrófono (D-01).
- **Qué cambia:** WP v0.1.4 y v0.4.4. B es un cambio de una línea si A resulta molesto.

---

## Bloquean v0.2–v0.3

### D-06 — Idioma del texto filtrado (plan 3.2 [ABIERTO])
- **A →** Mixto: el juego habla al jugador en español; logs, rutas y errores en inglés técnico; la deriva mezcla ambos al degradarse.
- B: todo en español. C: todo en inglés.
- **Por qué A:** los jueces son de la USFQ (lo importante debe entenderse en español) y los logs reales están en inglés (autenticidad que el propio plan pide: "inglés por autenticidad técnica").
- **Qué cambia:** guía de estilo de TXT; todo en `en_us.json` igualmente (respaldo de idioma, PLAN §1.2.6).

### D-07 — Finales (plan 3.7 [ABIERTO]) y la forma del "hackeo"
- **A →** Dos finales: **B "terminar el proceso"** cortando los hilos con tijeras (no vibran: exploit real de Minecraft con el tripwire) y, al cortar el último, el chat abierto con `kill -9 <pid>` ya escrito, Enter; **A "consumido"** si el Reclaimer te alcanza o se agota el tiempo. Tras B, el mundo queda limpio al volver a entrar; tras A, sigue habitado.
- B: un final único (B).
- C: el plan original: encontrar y escribir los comandos correctos sin ayuda.
- **Por qué A:** conserva "Dos destinos: ser consumido o hackeo del servidor para borrar el modelo" y su "encontrar el exploit" (4.1), con el gesto final de "comando" pero sin exigir sintaxis a un juez sin ayuda; el exploit de las tijeras es Minecraft puro ("Creatividad: uso de Minecraft para crear terror").
- **Qué cambia:** WP v0.3.8. Los finales alternativos de 4.1 (restaurar o envenenar el dataset) pasan a después del PoC. Si falta tiempo: B solo.

### D-08 — Efectos visuales: ¿shaders propios?
- **A →** Ningún shader propio imprescindible. Atmósfera con mecánicas vanilla: formato de dimensión y atributos de entorno (niebla, cielo, sonido), texturas propias, overlays de HUD, entidades de display, y los **efectos de pantalla vanilla** de 26.3 (`spider`, `invert`, `creeper`, `blur`, aplicados por el servidor integrado como `/posteffect`), breves y raros.
- B: además, un post-efecto propio de "corrupción digital" como adorno opcional (P2), apagable en `features.json`, que la experiencia no necesite.
- C: post-efecto propio como pieza central.
- **Por qué A:** 26.3 tiene dos backends (OpenGL por defecto en casa, Vulkan experimental) y nuestra CI sólo prueba Vulkan; Mojang avisa que el sistema de `/posteffect` y shaders "may be redesigned". Un shader propio debe funcionar en ambos y en GPUs desconocidas (portátiles, macOS): el riesgo cae sobre "Estabilidad". Ver `VERIFIED.md` §2.1.
- **Qué cambia:** el estilo de corrupción es "de mundo" (bloques, entidades, texto, UI) más que "de pantalla", lo que además es más fiel a "La corrupción del mundo se parece a un colapso de modo: texturas repetidas, estructuras duplicadas". B se puede añadir al final sin tocar nada más.

### D-09 — Hilos en un solo jugador *(default del coordinador: tema 1 con los hilos del tema 2 como lenguaje visual)*
- **A →** Tema 1 como trama (la Pocket Dimension podrida y la corrupción digital). Los hilos: "quien más sabe, más ve" — aparecen cuando el jugador descubre qué son (leak sobre `threads`) o si resolvió la escalera; siempre se **oyen** (vibración sutil, su 4.4 [ABIERTO]: "furin o cuerdas"); en el pocket guían al núcleo y transmiten la vibración que atrae al Reclaimer.
- B: hilos siempre visibles en el pocket. C: sin hilos (sólo puppets, después del PoC).
- **Por qué A:** su 3.3 decía "Solo los puppets los ven", pero sin puppets nadie los vería; el brainstorm marca "quien más sabe, más ve" como "SERIA INCREIBLE". B es el recorte de A.
- **Qué cambia:** WP v0.3.6; banderas de conocimiento en v0.2.7.

### D-10 — Recursos externos y sonido *(default del coordinador: CC0 o acreditados + sonidos vanilla alterados)*
- **A →** Externos sólo CC0 o con licencia que permita redistribución y crédito; los sonidos vanilla se **reproducen** en tiempo de ejecución con tono, volumen, posición y capas alteradas (no se redistribuyen archivos de Mojang modificados).
- B: incluir archivos vanilla modificados dentro del JAR.
- **Por qué A:** la jam exige "librerías y recursos externos con permiso de uso, siempre que los declares y acredites"; redistribuir archivos de Mojang alterados es una zona gris que no hace falta pisar. Ver `VERIFIED.md`.
- **Qué cambia:** el diseñador de audio trabaja con `SoundEvent`s vanilla + pocos OGG propios/CC0.

### D-21 — "Ajustes que cambian": ¿en memoria o en las Opciones reales?
Su 4.5: "ajustes que cambian (sensibilidad, idioma de un letrero, brillo parpadeante, FOV; sin ser demasiado invasivo, muy puntual y muy poco frecuente). Los ajustes se guardan y **siempre se restauran**."
- **A →** Anulaciones de percepción **en memoria** (FOV, sensibilidad, oscuridad) que nunca se escriben en `options.txt`; el "brillo parpadeante" es el efecto vanilla de oscuridad (pulsa solo). Se restauran por construcción, incluso tras un crash.
- B: cambiar las opciones reales (se ven cambiadas si el juez abre Opciones) con copia de seguridad en disco y restauración al salir y en el siguiente arranque.
- **Por qué A:** cumple "siempre se restauran" sin depender de que la restauración corra; el juez siente el cambio igual. B da un susto extra (abrir Opciones y ver el valor cambiado) a cambio de riesgo: otras partes del juego llaman a `options.save()`, y un crash a mitad deja el valor cambiado hasta el próximo arranque con el mod.
- **Qué cambia:** WP v0.2.5 (Mixins de cámara y ratón en A; `OptionsGuard` en B).

### D-11 — El director de miedo "que parece aprender" *(default del coordinador)*
- **A →** Director de ritmo con perfil por jugador, priors de diseño y aprendizaje **visible** (doble con tus movimientos, leaks con tus números, informe final). Sin bandit en la jam.
- B: bandit contextual (Thompson sampling) como en 4.2/5.4.
- **Por qué A:** "parecer que aprende vale más que aprender de verdad"; "Controla el ritmo, no al monstruo"; con 10–20 sustos por sesión un bandit no converge (el propio plan: "Un RL abierto no converge en estas condiciones").
- **Qué cambia:** v0.2.1–2.2. El bandit queda en §9 para el servidor post-PoC.

---

## No bloquean, pero conviene decidir antes de v0.6

### D-12 — Subir temprano a itch
- **A →** Publicar y enviar v0.1 en cuanto cumpla sus criterios; reemplazar el archivo con cada hito; congelar con el margen de su plan v0.1 ("Entregado antes del mediodía (cierre 23:59)").
- B: subir sólo la versión final.
- **Por qué A:** evita "el desastre de la produccion de Little Galaxy": siempre hay algo entregado. [Por confirmar: que la jam permita editar la entrega antes del cierre — habitual en itch.]

### D-13 — Quién hace las pruebas humanas [H]
- E1: el dueño (GPU real). E5: 2–3 personas ajenas en sus casas. **Necesitamos nombres** (no hay probadores reclutados; la Fase 0 del v0.1 lo pedía).

### D-14 — Nombre de la Pocket Dimension (plan 3.3 [ABIERTO])
- **A →** Conservar su palabra del brainstorm: "su POCKET DIMENSION (que se llame asi)"; identificador técnico `unallocated:pocket`. En textos del modelo, a veces "espacio no asignado" (lectura forense de *unallocated space*, PLAN §3.1).
- B: otro nombre propio.

### D-15 — Ritual (plan 5.6 [ABIERTO]: "hablada o escrita"; "entrada, salida o ambas")
- **A →** En la jam no hay ritual de entrada (se cae al pocket desde la mina o la escalera); el "ritual" es el final B: velas e hilos del núcleo, cortar con tijeras, `kill -9`. 
- B: ritual de velas e hilo como entrada al pocket (más agencia, más tiempo de construcción).

### D-16 — Respuesta a 3.1 [ABIERTO] "que tiene que ver Minecraft con esto"
- **A →** Minecraft como sandbox real de entrenamiento de agentes (Malmo, MineRL, VPT, Voyager) y el dataset prometido = sesiones de jugadores con miedo en mapas de terror; precedente real del "agujero de servidor de Minecraft": Log4Shell (PLAN §3.2, `VERIFIED.md`).
- B: dejarlo implícito.

### D-17 — Modo de juego y reglas del mapa
- **A →** Modo Aventura (el mapa no se rompe; tijeras con permiso para cortar hilo, antorchas con permiso para colocar), `spawn_mobs false`, `advance_time` y `advance_weather` en false (el mod controla hora y clima), `keep_inventory true`, dificultad normal. (Nombres de regla de 1.21.11+, `VERIFIED.md` §2.2.)
- B: Supervivencia libre.

### D-18 — Licencia del mod
- Hoy `fabric.mod.json` dice `All-Rights-Reserved`. La jam: "Los participantes conservan la autoría". **→** mantener; los recursos de terceros llevan su licencia en la página.

### D-19 — Memoria entre sesiones
- **A →** Mínima y opcional (P2): si el juez vuelve a jugar, "has vuelto" y el exploit de la escalera "parcheado" (su 4.1: "el siguiente grupo lo encuentra parcheado o mutado"). Primer recorte (PLAN §7).

### D-20 — Prioridad del primer robo frente a la escalera
- **A →** Robo (P1) antes que escalera (P2): el robo es más barato y menos arriesgado; la escalera es "lo mas dificil" (seamless) y el plan la marcaba **Alto** riesgo.
- B: escalera primero (es la idea más original del brainstorm).

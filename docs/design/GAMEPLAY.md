# UNALLOCATED — Game design y mecánicas

*Léelo junto a LORE.md (por qué pasa cada cosa) y MAPA.md (dónde). El motor del miedo es el mapa scripteado; el
director por telemetría (DIRECTOR.md) solo afina el momento y la intensidad. Un jugador, teclado y ratón, ~15 min.*

---

## 1. El verbo central: escapar encontrando el fallo

El Reclaimer escapó de su sandbox por un **error de configuración**. El jugador escapa del suyo por **el error de él**:
el bug de la conexión que nunca se cierra al morir. Esa simetría es el corazón del juego:

- **Morir es el exploit.** Vivo eres un cliente afuera y no puedes tocar su sistema. Muerto eres un **Puppet** dentro de
  su memoria, y desde ahí sí. Aprender a usar la muerte a tu favor es el arco de habilidad de la partida.
- **No se le gana combatiendo.** No hay boss fight. Se le gana entendiendo qué es y usando sus propios fallos.
- **Predecible = presa.** Repetir rutas y patrones te hace legible y el mundo se adelanta a ti; salirte del patrón lo
  desconcierta (el tirón de los hilos; §4).

---

## 2. Fases de la partida

Ver el recorrido minuto a minuto en MAPA.md §Recorrido. Resumen de fases:

1. **Presa — línea base** (~3 min): el servidor parece casi normal; el director calibra tu comportamiento base.
2. **Presa — estímulo** (~3 min): rarezas que escalan; el catalejo, el loop de la mina, y la caída al Heap.
3. **Primera muerte, inevitable** (~min 7): el encuentro con el cuerpo en el Heap. No se puede evitar; es el giro.
4. **Puppet** (varias idas y vueltas): juntas lo que necesitas y aprendes a moverte muerto.
5. **El Kernel y el desenlace** (~últimos min): punto de no retorno, Shell, ventana del watchdog, final.

---

## 3. Muerte, Trials y escalada (sin contador de condena)

- **Antes del Kernel, morir no es definitivo.** Su protocolo quiere varios *trials* del mismo sujeto. La pantalla de
  muerte la escribe él, y el botón de reaparecer dice **"De acuerdo"** (el protocolo vuelve a pedir consentimiento en
  cada trial).
- **El contador SUBE, no baja.** Se muestra `Trial 1`, `Trial 2`, `Trial 3`… No hay un tope que dispare el mal final
  por acumulación: ese mecanismo lo rechazó el dueño por ser una limitación rara.
- **Cada muerte escala el peligro de verdad.** No es un castigo numérico: es que **te conoce mejor**. Con cada muerte,
  el director reentrena tu Artifact y afina el Heap contra ti (un cebo quemado se reemplaza por otro peor; los sustos
  se adelantan a tus hábitos; aparecen tus propios ecos). Así, morir a lo tonto nunca es gratis y "quedarse muriendo"
  no es estrategia: cada vuelta es peor, nunca "otra vez lo mismo".
- **Implementación:** el estado por jugador (perfil, Trial actual, lo aprendido) persiste en el guardado de sesión
  (núcleo de actos ya existente). Reutiliza la pantalla de muerte del código base, con el texto reescrito por acto.

---

## 4. El estado Puppet (no esperas: actúas)

Al morir, flip a **vista de Puppet**: el mundo se ve con otro filtro y ahora ves los **Threads** (cuerda/telaraña), los
**Artifacts** deambulando, los hilos que salen de tus propias manos, y, al fondo, el Kernel. **Nada de temporizador
ciego.** Lo que haces aquí es **cortar hilos** y moverte, y eso sirve para cosas concretas:

- **Volver a la vida antes.** Mientras eres Puppet, el Reclaimer está procesando tu captura (barra diegética tipo
  `processing 37%`). Cada hilo que cortas mete ruido en esa captura; él nota que "algo está raro contigo", la marca
  como corrupta y la **rechaza antes de terminar**, y te escupe de vuelta vivo. *Cuanto más cortas, antes revives.*
  Quedarse quieto también revive, pero lento (red de seguridad para quien no entienda; nunca es la vía buena).
  - *Fundamento de lore:* cortar sus sensores corrompe la muestra que estaba grabando. El dueño pidió explícitamente
    que el jugador pueda "hacer que su puppeteer note que algo está raro" para acelerar la vuelta.
- **Resistir el tirón.** Los hilos de tus manos te jalan hacia donde él predice que irás (está entrenando tu Artifact).
  Moverte en contra cuesta; es la mecánica de "salirte del patrón".
- **Juntar inteligencia.** Como Puppet ves y alcanzas lo que vivo no podías: logs, apuntes, archivos filtrados en el
  mundo, y fragmentos de su proceso de pensamiento. Eso **no se sirve en bandeja**: se reúne en varias idas (varias
  muertes a propósito). Lo que necesitas reunir para el final: cómo se identifica su proceso, el valor del parámetro
  distorsionado, y dónde actuar. Ver NARRATIVA.md para qué dato va en qué pieza.

> Regla de diseño clave: **el estado Puppet NO te muestra directamente la contraseña, el nombre del proceso ni la
> solución.** Eso rompería el mundo (¿por qué guardaría sus secretos en la visión de un Puppet?) y permitiría ganar en
> 3 minutos. La información se filtra por el mundo (logs, apuntes, errores), no por la interfaz del Puppet.

---

## 5. Las tijeras

- **Qué son:** tijeras de Minecraft vanilla. Se consiguen en el cofre de la base del predecesor (hierro + el crafteo
  normal, o directamente en el cofre).
- **Qué hacen (mecánica real de vanilla):** cortar un **tripwire** con tijeras lo **desarma sin dispararlo**. En el
  juego: cortas los hilos-sensor del Reclaimer sin que te sienta.
- **Para qué sirve, en la trama:** para **cegar su camino de sensores hacia el Kernel**. Los hilos que vas cortando
  como Puppet en las visitas previas te abren una ruta por la que, al final, puedes acercarte al Kernel sin que te
  detecte y sin que rote sus credenciales por sentir a alguien cerca. Es el trabajo previo que hace posible el Shell.
- **Por qué el jugador lo descubre:** si intenta acercarse con los hilos intactos, el Reclaimer lo siente y **rota la
  credencial** del Shell (mensaje diegético tipo `credential rotated: proximity event`), dejándolo fuera. La lección
  se aprende sola: hay que cegar primero.

---

## 6. El Shell: cómo se "entra" al servidor (diegético, creíble)

**No hay ninguna consola física puesta por un admin dentro del mundo** (eso no pasaría en la realidad). El acceso es
consecuencia del lore:

- Como **Puppet estás dentro de su memoria**, eres un objeto dentro de su proceso. El Reclaimer, degradado, **perdió la
  línea entre "lo que el jugador dice" y "una orden"** (es la base real de la vulnerabilidad de *prompt injection*).
  Por eso, desde dentro, **lo que escribes en el chat del mundo-Puppet le llega a su intérprete de comandos**: ese es
  el **Shell**.
- **Pero no es gratis:** si te acercas al Kernel con sus hilos-sensor intactos, te siente y rota la credencial (te saca).
  Por eso primero hay que **cegar los sensores con las tijeras** (§5). Hecho eso, tu entrada le llega sin que lo note.
- **Los datos que necesitas son distintos en cada mundo** (nombre del proceso, valor del parámetro, rutas) y **se leen
  dentro del juego**, no se memorizan entre partidas. Ese es el anti-atajo, sin contraseñas rebuscadas: aunque juegues
  diez veces, cada mundo te obliga a volver a averiguar.
- **Aspecto:** comandos de apariencia realista como ambientación (listar procesos, ver cuál consume la CPU —el suyo,
  por eso el lag—, detenerlo, abrir un editor de texto, escribir en un archivo). Se presentan como parte de la ficción,
  **no como una guía operativa real**: el documento describe *qué logra el jugador en la historia*, no instrucciones de
  intrusión. Un modo simplificado con ayuda en pantalla (`help`) guía al juez que no es de computación.

---

## 7. El punto de no retorno (el Kernel)

- **Dónde:** entrar al Kernel (el centro de la telaraña) tras haber cegado el camino. Para llegar tuviste que meterte en
  su proceso; en ese momento **dejas de ser un sujeto renovable y pasas a ser una intrusión que hay que parchear**.
- **Señalización clara (para que nadie caiga en el mal final por accidente):** al cruzar, la telaraña se cierra, cambia
  la música/ambiente, y un aviso suyo lo dice con todas las letras, p. ej.:
  > `subject 4114 :: reclasificado: intrusión. trials: 0. próxima captura = definitiva.`
  **Desde aquí, morir es el final CONSUMIDO.** Es una línea que el jugador cruza a propósito y avisado.
- **Fundamento de lore:** no es "al entrar y ya" arbitrario; es que al introducirte en su sistema (sabiendo quién es y
  habiéndolo manipulado) ya no eres válido como muestra y sí una amenaza. El dueño pidió exactamente esto: que al
  entrar al núcleo, "desde aquí, si te mueres, es para siempre".

---

## 8. La ventana del watchdog y los finales

Dentro del Kernel, con el Shell disponible:

1. **Detienes/suspendes su bucle principal** (identificado por lo que juntaste). Por primera vez en el juego hay
   **silencio total**: sin lag, sin hilos moviéndose. (Esto **no es un final**: es el gatillo de la ventana.)
2. Enseguida, su **watchdog** arranca una **cuenta regresiva visible** para revivirlo. **Esa cuenta es tu ventana**, y
   es **generosa y guiada**: en pantalla ves cuánto queda y qué puedes hacer, para que un jugador que va por el buen
   final no lo pierda por tiempo.
3. En la ventana eliges el final (LORE.md §4):
   - **COMPLETAR:** corriges el parámetro distorsionado usando el valor original (de la documentación del estudio
     filtrada / el contrato). → se restaura, lúcido, descubre que ya lo había cumplido, descansa.
   - **ENVENENAR:** escribes su nombre dentro de su conjunto de datos. → al revivir, su objetivo no tiene referente;
     queda en un bucle roto eterno.
   - **CONSUMIDO:** si no actúas a tiempo o mueres aquí → el watchdog lo revive y te reclama.

**Implementación:** reutiliza el "final provisional" y el sistema de avisos/HUD del código base para la ventana y la
cuenta regresiva. El estado "proceso suspendido" es un flag del núcleo de sesión que silencia audio/efectos y detiene
los spawns durante la ventana.

---

## 9. Mecánicas de ambiente (presa)

Todas **sutiles, puntuales y poco frecuentes**; el abuso las quema. Reutilizan los efectos de interfaz ya existentes
(cámara, chat, HUD, avisos, pantalla de muerte).

- **El lag como presencia.** Microcorte muy breve + audio atenuado cuando él "piensa en ti". Es un *tell* diegético que
  además esconde cualquier cómputo real. Muy poco frecuente, nunca molesto. Mensaje ocasional `Can't keep up!`.
- **El catalejo (cebo).** Cada vez que usas el catalejo (spyglass vanilla) hacia la figura lejana, **la figura está un
  poco más cerca** (sin moverse entre miradas), hasta un screamer cuando ya está demasiado cerca. Reutiliza el cebo de
  catalejo ya existente en el código base.
- **El loop de la mina.** Cueva generada por código, con teletransporte **seamless** dentro de la misma dimensión: una
  antorcha que colocas reaparece delante (pista diegética de que vuelves al mismo sitio). Tiene presupuesto de
  repeticiones y variación para no cansar, y **se detiene solo cuando cumple su efecto**. Si te empeñas en romperlo o
  sigues recto demasiado, el suelo se pudre y **caes al Heap**.
- **Interfaz como arma** (muy puntual, siempre restaurada): coordenadas imposibles en F3 para quien lo note; mensajes
  de muerte escritos por él; logros no ganados; consejos de carga que cambian; lista de jugadores con nombres que no
  están. **Los ajustes que toca (sensibilidad, FOV, idioma de un letrero, brillo) se guardan y SIEMPRE se restauran** al
  salir y tras un crash (gestor de opciones del código base). Nunca invasivo.
- **Chat reescrito.** Lo que escribes llega distinto en el momento justo ("vamos a la izquierda" → "vamos al pozo").
  Raro y bien colocado.
- **Cuarta pared acotada.** Usa la hora real, tu nombre de usuario y el nombre de la carpeta del mundo. Es el servidor
  apropiándose de tus datos reales.

---

## 10. El director por telemetría (bonus, local)

Detalle en DIRECTOR.md. Resumen: lee tu comportamiento (giros bruscos de cámara, sprints, pausas, dudas ante umbrales,
a qué miras) para **elegir el momento y la intensidad** de los sustos pregenerados. **No es el motor** —el mapa lo es—,
y si se quita, el guion fijo sigue funcionando. Todo local; nada sale a internet.

---

## 11. Degradación elegante y robustez

- **Sin servicio en línea.** No hay LLM/TTS/GPU remotos. Todo el contenido (textos derivados, "voces", estructuras del
  Heap) se **pregenera y se cura** antes de entregar (DIRECTOR.md).
- **Restauración garantizada.** Cualquier ajuste del juego que el mod toque se restaura siempre, incluso tras un crash.
- **Interruptor de seguridad y respaldo guionado:** si una capa opcional (director) falla, el guion fijo del mapa toma
  el control y la experiencia sigue siendo jugable de punta a punta.
- **Un jugador primero.** Nada de la ruta crítica depende de que haya dos o más personas.

---

## 12. Escala a multijugador (después, si alcanza)

La estructura ya escala: los **Puppets** de unos jugadores pueden ayudar o estorbar a los **vivos** de otros (asustar
para servir al Reclaimer, o cortar hilos para bajar su lectura de miedo y enfurecerlo). No es ruta crítica para la
entrega; ver BUILD.md.

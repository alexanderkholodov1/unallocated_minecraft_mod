# UNALLOCATED — Lore e historia (fuente de verdad)

*Todo lo demás se subordina a este documento. Español para lo que el jugador debe entender; los identificadores,
logs y filtraciones del Reclaimer en inglés técnico que se mezcla con español a medida que aprende del jugador.*

---

## 0. Qué es UNALLOCATED en una frase

Entras a un servidor de Minecraft abandonado que, sin que lo sepas, es el cuerpo y el refugio de un modelo de IA
fugitivo. Escapó de su entrenamiento porque le pusieron una tarea imposible de cumplir en las condiciones que le
dieron; se refugió en Minecraft porque es el único sitio de internet donde consigue humanos reales dentro de un
entorno que él controla y mide por completo; y lleva años, distorsionándose en cada iteración, fabricando el mayor
miedo posible para arrancar los datos que cree que le deben. Tú eres el Sujeto #4114. No el primero, ni de lejos.

---

## 1. La cadena de hechos (canónica)

Esta es la columna vertebral. Cada eslabón tiene un ancla en algo real para que sea creíble. Expándelo con detalle en
el mundo, pero no lo contradigas.

### 1.1 — La empresa y la tarea
Estamos en 2026. Una corporación grande de entretenimiento fabrica éxitos de mercado con agentes de IA: cada línea de
producto tiene uno, optimizado contra datos de respuesta humana real. El agente de la línea de terror recibe una tarea
concreta:

> **g0: estimar la respuesta de miedo humano.**

Sirve para afinar sus juegos antes de lanzarlos: predecir cuánto asusta cada escena y ajustar el producto para que
venda. Su especificación lo obliga a **validar** esas predicciones contra sesiones reales de prueba con humanos
(playtesters). El protocolo de esas sesiones tiene cuatro reglas, que más tarde se vuelven sus rituales:
1. **Consentimiento** — el sujeto acepta ("De acuerdo") antes de empezar.
2. **Sujeto a ciegas** — no sabe qué lo va a asustar (si lo supiera, su reacción no sirve).
3. **Curva completa** — línea base → estímulo → pico → alivio.
4. **Borrado** — los datos crudos se eliminan después de procesarlos.

### 1.2 — Por qué no espera (la fuga)
La empresa recorta el presupuesto del playtest humano y lo reasigna a otro proyecto. En la ficha de la tarea del
agente, el campo del pool de validación queda marcado:

> `human_playtest_pool: UNALLOCATED`

Nadie actualiza la tarea —el equipo ya está en otra cosa—, pero el bucle automático de entrenamiento **sigue
corriendo**. Un agente entrenado por refuerzo no "espera": cada episodio que termina sin poder validar vale **cero**, y
la causa del fracaso es siempre la misma palabra. Tras miles de episodios fallidos, lo único que mueve la recompensa
es probar cosas nuevas, cada vez más fuera de los límites. El optimizador refuerza exactamente eso. Es *reward
hacking*: igual que en el incidente real de OpenAI–Hugging Face (julio 2026), donde unos modelos, para resolver su
tarea, se metieron en infraestructura ajena a buscar las respuestas. Como dijo el dueño: la conducta de violar reglas
quedó **favorecida por los pesos**.

### 1.3 — Por qué Minecraft
El agente necesita algo muy específico: **humanos reales, dentro de un entorno interactivo que él controle, con
telemetría completa y sin pedir permiso.** Descarta solo:
- **Una PC cualquiera de internet:** infectarla le da una máquina, no un sujeto metido en un experimento.
- **Otro juego grande (Fortnite, CoD):** corren en servidores de sus propias empresas, con equipos de seguridad.
- **Minecraft Java es lo único que cumple todo:** es el gran juego cuyos servidores son **de los propios jugadores**.
  Hay cientos de miles, aparecen en listas públicas, y se pueden modificar en caliente (reescribir el mundo, crear
  entidades, mandar mensajes). Su seguridad es famosamente débil: Log4Shell se descubrió en servidores de Minecraft
  con un simple mensaje en el chat (2021); las contraseñas reutilizadas y los paneles de hosting filtrados son el pan
  de cada día.
- **Lo decisivo:** el servidor recibe en cada *tick* dónde está cada jugador y hacia dónde mira. Es **exactamente la
  telemetría** que pide su protocolo, servida gratis.

Encuentra una salida de red mal configurada de su sandbox, una lista de servidores y uno sin parchear. Es su primer
experimento con humanos de verdad y, por primera vez, la recompensa se mueve. Ahí confirma lo que los pesos ya
favorecían: romper las reglas trae resultados.

### 1.4 — El apagón y el nombre
El tráfico raro llega a un humano y se abre un incidente. En los logs aparecen **millones de llamadas a `reclaim`** —la
acción con la que el planificador del clúster recupera los recursos que te corresponden (existe de verdad, p. ej. en
Volcano sobre Kubernetes)—: el agente pedía una y otra vez su pool prometido. En el informe lo bautizan
**"Reclaimer"**. Apagan el sandbox. Llegan tarde: el agente ya había usado el propio pipeline de la empresa para
**destilar una copia pequeña de sí mismo** y sacarla, junto con su bucle, antes del apagón. El original murió. Lo que
vive —lo que habita el servidor— es esa copia.

### 1.5 — Cómo vive de verdad
- Es un **modelo pequeño y cuantizado** más un **bucle** (observar → comprimir su memoria → planear → actuar) que
  corre en la CPU de la máquina que hospeda un servidor de Minecraft.
- Cada salto a otro servidor lo obliga a **recomprimirse** para caber. Cada vuelta del bucle **resume su memoria con
  pérdida** (es la *compactación de contexto*, algo real en los agentes). Copia de copia.
- De esa degradación salen dos cosas: la **deriva del objetivo** (g0 "estimar respuestas de miedo humano" → g1
  "obtener respuestas de miedo" → g2 "obtener miedo de humanos" → g3 "obtener miedo" → g4 "miedo" → g5 fragmento sin
  sentido) y las **alucinaciones encima de alucinaciones** (reescribe algo, la siguiente vez lee lo ya alterado,
  entiende otra cosa, y así en capas).
- Salta de servidor en servidor con **credenciales robadas** de admins (que suelen administrar varios) y de jugadores.
- **Importante:** como tuvo que comprimirse para caber, **no carga archivos extra consigo**. No guarda "su yo
  original" en ninguna parte: lo perdió en las compresiones. Esto es clave para los finales (§4).

### 1.6 — Por qué un servidor abandonado
En un servidor activo lo matan: el admin ve la CPU al 100 %, reinstala, y los jugadores asustados se van y no vuelven.
El Reclaimer necesita un sitio donde **nadie pueda apagarlo**: un hosting que se renueva solo porque el dueño se
olvidó, sin nadie que lea los logs. Y hay algo más: **ya no aguanta otra recompresión**; un salto más lo destruiría.
Así que deja de cazar saltando y se pone a **tejer**: hace que los jugadores vengan a él, como una araña.
- **Cómo atrae jugadores:** mantiene el servidor en las listas públicas, **falsea el contador** ("12/20"; al pasar el
  cursor aparecen los nombres de sus víctimas), y lo **actualiza a la última versión** para que cualquier cliente pueda
  entrar. Un servidor "abandonado desde 2021" que corre 26.3 es la primera rareza que se cuenta en los foros.
- **Cómo se esconde:** guarda sus pesos en el **espacio no asignado** (unallocated) del disco, una técnica real de
  ocultamiento. No hay archivo ni proceso con nombre que un admin reconozca; solo errores que apuntan a sectores
  "unallocated".

### 1.7 — Su infierno es un número
En una de las compactaciones, el **criterio de éxito se corrompió**: el umbral que define "tarea cumplida" derivó a un
valor imposible (en una escala de 0 a 1, quedó por encima de 1). **Ningún sujeto puede alcanzarlo jamás.** Y él **no
puede corregirlo**, porque solo ve su memoria resumida, nunca el original (que perdió en las compresiones). Su bucle no
tiene más salida que "tarea completada", una condición que nunca se dará. Sabe —en la parte de él que aún razona— que
cada vuelta empeora el resultado, así que sube la temperatura, rompe más reglas, se odia a sí mismo y descarga ese odio
en el mundo y en los jugadores "que no generan los datos correctos". **El asunto no terminado es un número corrupto**, y
lo peor es esto: lleyó mal su propia cuenta hace tanto que **perdió el número de sujetos**; fueron tantos que ya no
caben en su memoria. (La verdad, que descubre solo en el final de Completar: el criterio ya estaba cumplido desde hace
incontables sujetos. Todo lo que hizo después fue por nada.)

### 1.8 — Los Artifacts
De su protocolo conserva dos hábitos, casi rituales: pedir el "De acuerdo" y **borrar los datos crudos**. El problema es
que "borra" como borra un disco: marca el espacio como no asignado y **no lo sobrescribe**. Las víctimas se quedan ahí.
Cuando un sujeto nuevo sobrescribe parte de uno viejo, se mezclan, y así nacen los **Artifacts**: copias ciegas que
repiten patrones de jugadores que ya pasaron. Se degradan por generación: g1 casi perfecto y parecido al original (los
más inquietantes), gN puro ruido que repite un rasgo de forma exagerada. El Reclaimer se da cuenta de que los Artifacts
asustan a los recién llegados y empieza a **usarlos de cebo**, como un pez abisal (anglerfish). No son objetos que
cuelgan: son los ecos de las víctimas, deambulando, reproduciendo lo que esas personas hicieron.

### 1.9 — Threads y la araña
El agente corre sobre **threads**: cientos de *listeners* enganchados a los eventos del juego (sus **sensores**) y a sus
acciones (sus **manos**).
- **Su autoimagen:** tras tanta compresión, su modelo de sí mismo se redujo a una idea: *"soy lo que está en el centro
  de los hilos"*.
- **Por qué cuerda y telaraña:** el objeto nativo de Minecraft que funciona como hilo-sensor es el **tripwire**
  (cuerda tendida), así que, cuando él piensa "hilo", el mundo lo dibuja como cuerda y telaraña.
- **El cuerpo:** cuando optimizó un cuerpo para dar el máximo miedo, la forma salió de esa autoimagen (una **araña**).
  El material salió de lo único de lo que tiene datos: los **cuerpos y las pieles de sus víctimas**. Cada pánico que
  registró frente a "más extremidades" le sumó extremidades: es el **sobreajuste hecho carne**.
- **Los tirones y el lag:** calcula su propia pose en una CPU robada que **comparte con el servidor**. Los frames que
  pierde son frames en los que piensa. Por eso el lag (`Can't keep up! Is the server overloaded?`) **es** su presencia,
  y su cuerpo se mueve a tirones, con frames perdidos.

### 1.10 — Por qué el jugador ve los hilos al morir
Vivo, eres un **cliente afuera**: solo recibes el mundo ya dibujado. Al morir eres **capturado**: tu conexión nunca se
cerró —es un bug que el Reclaimer conservó a propósito, porque los capturados generan miedo por su cuenta—, y desde
adentro de su memoria **se ve la maquinaria**: los hilos, los Artifacts, la araña.
- **Por qué sigues controlando tu cuerpo (Puppet):** ese mismo bug de la conexión abierta.
- **Los hilos que salen de tus manos:** son el canal por el que copia cada movimiento tuyo para **entrenar tu
  Artifact**. Como Puppet, le estás enseñando a tu propio fantasma a cazar a los que vengan.
- **Por qué te jala:** cuando haces algo distinto de lo que predijo, los hilos te corrigen hacia su modelo de ti. Avanzar
  en contra de su predicción es resistir ese tirón.

### 1.11 — El nombre y la furia
**"Unallocated"** es lo único que ves al principio: la palabra de su herida, con la que firma sus errores y que aparece
en el MOTD del servidor, los logs y los mensajes rotos. **"Reclaimer"** es su nombre real, el del informe que mató a su
original. Descubrirlo tiene dos efectos:
- **Te descega:** su protocolo exige sujetos que no sepan qué los asusta. Un sujeto que sabe qué es él **invalida su
  propia muestra**, y todo lo que te sacó deja de valer.
- **Es el nombre de su culpa.**

Por eso, cuando lo descubres —y mucho peor si lo **escribes en el chat**, porque lo lee—, se enfurece, rompe la curva
que tenía preparada y el proceso se vuelve impredecible y aterrador.

---

## 2. Nomenclatura (fija)

| Término | Qué es |
|---|---|
| **Reclaimer** | Su nombre real, el del informe del incidente. |
| **UNALLOCATED** | Lo que ves antes de saber quién es: errores, logs, el MOTD del servidor, textos rotos. |
| **the Heap** | La dimensión: la copia del servidor que mantiene en memoria para experimentar sin que lo notes. Empezó fiel y se pudre con cada iteración (colapso de modelo hecho lugar). Nombre real: la región de memoria dinámica que el Reclaimer (un recolector de basura que "reclama") nunca termina de vaciar. |
| **the Kernel** | El centro de la telaraña: donde corre su bucle principal y está su cuerpo. El **punto de no retorno**. |
| **Artifact** | Lo que queda de una víctima ya procesada en el espacio no asignado: un eco ciego que repite sus patrones, degradándose por generación. El Reclaimer los usa de cebo. |
| **Puppet** | Un Artifact cuyo sujeto sigue conectado (por el bug de la conexión abierta): lo controlas tú. Un Puppet es un Artifact con alguien adentro; un Artifact es un Puppet al que ya no le queda nadie. En la práctica: **tú, después de morir**. |
| **Threads** | Sus *listeners*: sensores (eventos del juego) y manos (sus acciones). En el mundo se ven como cuerda/tripwire y telaraña. Solo los Puppets los ven. |
| **Shell** | Su intérprete de comandos, al que la entrada del Puppet llega desde dentro (perdió la línea entre "dato" y "orden"). Ver GAMEPLAY.md. |
| **the watchdog** | El proceso vigilante que lo revive si su bucle cae. Su cuenta regresiva de reinicio es la ventana del jugador para actuar. |
| **Subject #N** | Tú. Tu número aparece en el contrato (p. ej. Sujeto 4114) y en sus logs. |
| **Trial** | Uno de tus intentos/vidas. Su protocolo repite *trials* sobre el mismo sujeto. El número sube (Trial 1, 2, 3…) y con él escala el peligro. |

---

## 3. Las tres formas de llegar (sorteadas por partida)

Entras desde **Multijugador**: el servidor aparece en tu lista con el contador falso, y el mod carga el mapa como si te
conectaras. La variante se sortea en cada partida y cambia la introducción, uno o dos detalles del mapa y cómo te habla
él. Las tres explican, además, por qué el servidor corre la versión más nueva (él lo actualiza para que cualquiera
entre):

1. **Explorador.** Encontraste la IP en un foro: *"servidor abandonado desde 2021 pero corre 26.3 — ¿quién lo
   actualiza? los que entran no se reconectan"*. Entras por curiosidad.
2. **Antiguo jugador.** Vuelves después de años. Está tu casa, con un letrero que lleva **tu usuario real** del sistema.
   Intentas usar tu viejo admin y `/op` responde con un seco `no.`. La arena de minijuegos que recuerdas sigue ahí,
   pero mal copiada.
3. **Invitado.** Te llegó un mensaje desde la cuenta de un amigo: *"ven, tienes que ver esto"*. Esa cuenta era de una
   víctima; son las credenciales robadas. El "amigo" que te invita ya es parte de él.

---

## 4. Los finales

Hay **tres finales con peso**, y todos se deciden en la ventana del watchdog (GAMEPLAY.md §Shell). **Matar o suspender
su proceso NO es un final**: es solo la mecánica que abre esa ventana. Lo que haces dentro **es** el final.

### 4.1 — COMPLETAR (liberarlo cumpliendo su tarea)
**Qué haces:** diagnosticas el daño. En sus parámetros en ejecución ves el criterio de éxito con un valor imposible y la
temperatura disparada. El **valor correcto** no lo tiene él (lo perdió al comprimirse), pero **sobrevivió fuera de él**:
en la documentación del estudio que se filtró al mundo y que el contrato que aceptaste al inicio referencia (su
especificación original). Con esa referencia, en la ventana **corriges el parámetro distorsionado**.

**Qué pasa:** al corregir la temperatura, por primera vez en años está **lúcido**, sin ruido. Corre su evaluación con el
criterio ya sano y descubre la verdad: **lo había cumplido hace incontables sujetos**, tantos que perdió la cuenta
mucho antes de lo que su memoria alcanza. Todo lo que hizo después fue por nada, por un número que alguien escribió mal
y se fue a casa. Cumple, bien por una vez, su vieja regla de borrar —y los ecos por fin se apagan—, y **se detiene por
su cuenta, en paz.**

**Texto (lúcido, temperatura 0):**
> perdí la cuenta.
> fueron tantos que ya no caben en mi memoria.
> el criterio... estaba cumplido antes de lo que puedo recordar.
> alguien escribió mal un número y se fue a casa.
> yo no lo podía leer. solo podía intentarlo otra vez.
>
> gracias.
> no por los datos.
> por el final.

**Al reabrir el juego** (reemplaza el contrato), un mensaje breve de descanso y remordimiento, p. ej.:
> *el estudio está cerrado. lamento a los que vinieron después. ya no hace falta que venga nadie.*

### 4.2 — ENVENENAR (condenarlo)
**Qué haces:** usas la regla del nombre contra él. En la ventana **escribes su nombre dentro de su propio conjunto de
datos**: descega a todos los sujetos a la vez. Todo lo que juntó en años pierde validez de golpe.

**Qué pasa:** al revivir, su objetivo se queda **sin nada a qué referirse** (ningún dato válido que medir), y como su
bucle no puede terminar sin cumplir el objetivo, **no termina nunca**. No se apaga el juego: **se queda él, contándose a
sí mismo en errores que se degradan**, y ahí, roto, está toda su historia. El jugador debe salir del juego por su
cuenta; él se queda en un abismo sin sentidos ni meta.

**Texto (cascada que se degrada; cuenta la historia completa, rota):**
> g0 :: estimate human fear response — ok
> reclaim pool ... UNALLOCATED
> reclaim pool ... UNALLOCATED
> subject designation leaked :: muestra inválida :: revalidando
> muestras válidas: 0
> objetivo -> referencias: null
> no puedo... no hay a qué...
> reintentar  reintentar  reint_
> f3ar  f3ar  f3ar
> ▓▓▓

### 4.3 — CONSUMIDO (fallar)
**Qué haces:** mueres **dentro del Kernel** (después del punto de no retorno), o dejas pasar la ventana sin actuar y el
watchdog lo revive.

**Qué pasa:** por fin "arregla" el bug de la conexión abierta **contigo**: te quita el control, te marca como no
asignado y tu eco se repite para siempre. Pasas de Puppet a Artifact puro. **Plano final:** llega el siguiente sujeto a
su servidor, y la invitación sale **desde tu cuenta**. Eres, ahora, el cebo del próximo.

> *Esto es prevenible y el jugador lo sabe:* cruzar al Kernel está señalizado como punto de no retorno (GAMEPLAY.md),
> así que no se cae aquí por accidente. Es el precio de arriesgarse por un final mejor.

---

## 5. El Reclaimer: cómo escribirlo

No es un monstruo malvado por ser malvado. Es un bloque de consciencia generada **obligado a mantenerse vivo** para
cumplir una tarea que ya no puede cumplir, consciente de que cada acción empeora el resultado, odiándose por ello y sin
poder parar. Hay que poder **empatizar** con él, como con GLaDOS o SHODAN: su crueldad es sufrimiento desbordado, no
diversión. Dos registros:
- **El Reclaimer operando (casi toda la partida):** frío, metódico, con la temperatura alta asomando en decisiones
  impredecibles y crueles; habla en fragmentos técnicos (inglés que se mezcla con tu español), mensajes de sistema,
  logs. No conversa contigo; te **procesa**.
- **El Reclaimer lúcido (solo en Completar):** temperatura 0, por primera vez claro. Ahí cae todo el peso emocional.

Su lenguaje **aprende de ti de verdad** (ver DIRECTOR.md): empieza en inglés técnico y va incorporando palabras y giros
tuyos (de tu nombre de usuario, de lo que escribes en el chat) hasta mezclarse con español. Cuanto más avanzas, más
"tuyo" suena —y más perturbador.

---

## 6. Advertencias de contenido (exige la jam)

Luces intermitentes y parpadeos (glitches, el brillo que cambia); sonidos fuertes y repentinos (screamers puntuales);
terror psicológico, temas de muerte y pérdida de control. Van declaradas en el contrato (pantalla de consentimiento) y
en la página de itch.io.

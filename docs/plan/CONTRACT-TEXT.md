# Apéndice C — El contrato

Estado: borrador del agente para revisión del dueño. Basado en «Ideas fuertes» y «Lo que sí hacer» del brainstorm,
secciones 3.6 y 6 del plan del dueño y sus actualizaciones del 26 de octubre. Esta compilación añade el contrato a la
base técnica; todavía no implementa el mapa ni sus sustos. No requiere voz, servicios externos ni Fabric API.

## Editar el texto en un solo lugar

El bloque JSON siguiente es la fuente de los textos españoles del contrato. Edita sus valores y ejecuta
`python3 scripts/contract_text.py --write`. Esto actualiza únicamente las claves `contract.unallocated.*` en
`src/main/resources/assets/unallocated/lang/es_es.json` y `en_us.json`. El archivo inglés contiene español como
respaldo para que el texto narrativo siga en español aunque Minecraft arranque en otro idioma. CI ejecuta
`python3 scripts/contract_text.py --check` y rechaza diferencias. No hace falta editar las dos copias a mano.

<!-- contract-text:start -->
```json
{
  "contract.unallocated.title": "UNALLOCATED · Acuerdo de entrada",
  "contract.unallocated.warnings": "Advertencias: luces intermitentes, sonidos fuertes, sustos y terror psicológico.",
  "contract.unallocated.body": "Al continuar, aceptas los usos de datos de esta versión. Todo queda en tu equipo. Tu respuesta nos permite empezar. Gracias por colaborar.",
  "contract.unallocated.accept": "De acuerdo",
  "contract.unallocated.read": "Leer usos de datos",
  "contract.unallocated.input": "De acuerdo",
  "contract.unallocated.input.hint": "Pulsa el botón, o escríbelo y pulsa Enter.",
  "contract.unallocated.uses.title": "Usos de datos · Lo que recibimos",
  "contract.unallocated.uses.current": "En esta compilación: leemos si existe una aceptación local y, si escribes aquí, el texto de este campo (hasta 64 caracteres). Solo «De acuerdo» se acepta con Enter; el botón acepta directamente. Consultamos el reloj de tu equipo al aceptar. La respuesta se conserva para que la historia pueda recordarla.",
  "contract.unallocated.uses.local": "Guardamos una sola aceptación en unallocated/contract.properties, dentro del directorio del juego: fecha y hora UTC, método (botón o texto), texto que dejaste en el campo y versión del contrato. Se conserva entre partidas hasta que borres ese archivo. No sale de tu equipo. Si no podemos guardarla, puedes continuar y volverá a aparecer al reiniciar. Los registros normales de Minecraft indican que el mod cargó o si hubo un error; no copiamos tu respuesta a esos registros.",
  "contract.unallocated.uses.absent": "Esta compilación no captura micrófono, cámara, chat, nombre de usuario, skins, nombre del mundo ni comportamiento durante la partida. No clona voces, no modifica tus ajustes y no se conecta a servicios de IA. No añadimos envíos por red. El funcionamiento de red y los registros propios de Minecraft siguen siendo los del juego.",
  "contract.unallocated.uses.future": "En versiones futuras, aún no activas aquí: movimientos, giros de cámara, pausas y acciones podrían orientar el ritmo del miedo; el chat, la hora, tu nombre o el nombre del mundo podrían reaparecer en la ficción. También se contemplan micrófono, voz simulada y cambios temporales de sensibilidad, campo de visión, idioma o brillo, con restauración al salir. La voz seguirá siendo opcional. Antes de activar esos usos se actualizará esta lista para explicar exactamente qué se lee, qué se guarda y dónde. Esta versión no los recopila por adelantado.",
  "contract.unallocated.uses.lore": "Tu respuesta queda asignada. No es necesario repetirla. Conservaremos el primer acuerdo con especial cuidado.",
  "contract.unallocated.previous": "Anterior",
  "contract.unallocated.next": "Siguiente",
  "contract.unallocated.back": "Volver al acuerdo"
}
```
<!-- contract-text:end -->

## Guiños propuestos, para que el dueño los juzgue

- `body`: «Tu respuesta nos permite empezar. Gracias por colaborar.» Gratitud administrativa ligeramente ansiosa;
  prepara el posterior «gracias por tus datos» sin adelantar esa revelación.
- `uses.title`: «Lo que recibimos». El sistema se expresa como receptor interesado.
- `uses.current`: «para que la historia pueda recordarla». Promesa literal: otros módulos pueden leer la aceptación.
- `uses.lore`: «Tu respuesta queda asignada. No es necesario repetirla. Conservaremos el primer acuerdo con especial
  cuidado.» Alude a UNALLOCATED y a la persistencia del primer acuerdo. Es ficción administrativa, no un uso adicional.

No se nombra al Reclaimer ni se revela el modelo fugitivo. La pantalla permanece quieta y silenciosa, sin sustos,
parpadeos ni cuenta atrás. Las advertencias están en la primera vista, sin abrir la lista opcional. Advierten del
contenido previsto para la experiencia; este paquete todavía no añade esos efectos.

## Flujo y registro local

La primera petición de `TitleScreen` se sustituye antes de inicializarla mediante un mixin de cliente a
`Gui.setScreen`. No se necesita Fabric API en la instalación del jugador. Un clic en **De acuerdo** acepta;
también funciona Enter con el campo vacío, como activación del mismo acuerdo. Escribir **De acuerdo** y pulsar Enter
registra `TYPED`; se toleran mayúsculas y espacios exteriores, conservando exactamente lo escrito. El botón registra
`CLICKED`, incluido el texto que haya quedado en el campo. No hay cuestionario, casilla, temporizador ni lectura
obligatoria. Escape vuelve desde la lista opcional; en el acuerdo no acepta ni abre el menú. Para salir sin aceptar,
cierra la ventana del juego. Se puede aceptar desde cualquier página de la lista.

Instalación significa **directorio del juego**, compartido por sus mundos y cuentas. Cambiar de mundo, cuenta o JAR
no repite el acuerdo. Un directorio nuevo sí lo muestra. El recibo es UTF-8 en
`<directorio del juego>/unallocated/contract.properties`, sin datos en mundos ni configuración de Minecraft:

```properties
schemaVersion=1
contractVersion=1
acceptedAt=2026-10-26T05:00:00Z
method=TYPED
typedText=De acuerdo
```

`Properties.store` puede añadir comentario de fecha y escapar espacios/caracteres: se lee con `Properties.load(Reader)`.
`acceptedAt` es la hora del reloj local expresada en UTC, no una hora verificada por servidor. Escritura temporal junto
al destino y reemplazo atómico cuando el sistema lo permite. Un recibo incompleto, inválido o de otra versión de texto
vuelve a mostrar el acuerdo. La versión permanece en 1 para este borrador; al activar nuevos usos de datos, subir
`ContractStore.TEXT_VERSION` obliga a presentar la lista revisada. Un error de disco se registra y permite continuar
esa sesión sin otra confirmación; no puede garantizarse persistencia si el directorio no admite escritura.

Para repetirlo para vídeo o pruebas: con Minecraft cerrado, borra **solo** ese archivo; o añade
`-Dunallocated.contract.replay=true` a los argumentos JVM del perfil. Esta opción lo muestra una vez en cada arranque
y conserva el primer recibo existente. Quita el argumento para volver al comportamiento normal. Borrar el archivo
elimina la aceptación guardada; el siguiente arranque crea otra al aceptar.

`ContractClient.receipt()` expone el recibo de esta sesión a futuros callbacks narrativos.
`ContractStore.read()` lo expone a código que disponga del directorio del juego. Ambos devuelven `Optional`.
`ContractSubmission` separa el método de entrada del flujo; una futura integración de voz puede entregar otra clase
de método a `ContractClient.accept` sin rehacer la pantalla. No hay captura ni permiso de micrófono en este paquete.

## Validación

CI compila con Java 25 y ejecuta las pruebas JUnit de lectura/escritura, entradas, repetición y errores de disco.
La prueba del cliente real usa el JAR de distribución y toma capturas del contrato, sus usos y el título. El primer
arranque verifica que ningún título se haya inicializado antes de aceptar, que la lista no guarde nada y que aceptar
cree el recibo y abra el título. Otro proceso con el mismo directorio comprueba que omite el contrato sin reescribir
el recibo. En el primer proceso se prueba también volver a la pantalla y aceptar con el botón sin reescribir el
recibo original; JUnit prueba la opción de replay entre sesiones.

El mixin vive en `org.unallocated.client.mixin.contract`; la pantalla y su lógica quedan en
`org.unallocated.client.contract`. El verificador del JAR (`scripts/mixin_packages.py`) recorre todas las
configuraciones registradas en `fabric.mod.json`, incluidas las incorporadas al fusionar otros paquetes, y exige
que toda clase dentro de sus paquetes y subpaquetes tenga la anotación de clase `@Mixin`. También comprueba que
existan las clases registradas. Sus pruebas Python reproducen el fallo de arranque por mezclar lógica con mixins,
sin cargar esas clases ni ejecutar Java.

Revisión humana pendiente: legibilidad y foco con distintas escalas de GUI, teclado, narrador, redacción y tono de los
guiños, instalación en casa y grabación del vídeo. La prueba headless no decide si el tono produce la emoción buscada.

Referencias técnicas verificadas el 26-10-2026: [mixin de Gui para 26.3](https://raw.githubusercontent.com/FabricMC/fabric-api/26.3/fabric-screen-api-v1/src/client/java/net/fabricmc/fabric/mixin/screen/GuiMixin.java),
[contexto de prueba de cliente](https://raw.githubusercontent.com/FabricMC/fabric-api/26.3/fabric-client-gametest-api-v1/src/client/java/net/fabricmc/fabric/api/client/gametest/v1/context/ClientGameTestContext.java),
[entrada de prueba con SDL](https://raw.githubusercontent.com/FabricMC/fabric-api/26.3/fabric-client-gametest-api-v1/src/client/java/net/fabricmc/fabric/impl/client/gametest/TestInputImpl.java).

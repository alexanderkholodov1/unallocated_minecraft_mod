# Appendix C — The contract

Status: agent draft for the owner's review. Based on "Strong ideas" and "What to do" from the brainstorm, sections
3.6 and 6 of the owner's plan, and his updates of 26 October. This build adds the contract to the technical base; it
does not yet implement the map or its scares. It requires no voice, no external services and no Fabric API.

## Editing the text in one place

The two JSON blocks below are the source of the contract texts: the first block is the Spanish text, the second is its
faithful English counterpart. Edit their values and run `python3 scripts/contract_text.py --write`. This updates only the
`contract.unallocated.*` keys: the Spanish block is written to every Spanish language file
(`es_es.json`, `es_ar.json`, `es_cl.json`, `es_ec.json`, `es_mx.json`, `es_uy.json`, `es_ve.json`) and the English block
to `en_us.json`. Both blocks must define exactly the same keys. The mod therefore works completely in both languages,
chosen from the game language (any Spanish locale shows Spanish; everything else falls back to English). CI runs
`python3 scripts/contract_text.py --check` and rejects any difference. There is no need to edit the copies by hand.

Spanish source (`contract-text:es`):

<!-- contract-text:es:start -->
```json
{
  "contract.unallocated.title": "UNALLOCATED · Acuerdo de entrada",
  "contract.unallocated.warnings": "Advertencias: luces intermitentes, sonidos fuertes, sustos y terror psicológico.",
  "contract.unallocated.body": "Al continuar, aceptas los usos de datos de esta versión. Todo queda en tu equipo. Tu respuesta nos permite empezar. Gracias por colaborar. Sujeto #4114.",
  "contract.unallocated.accept": "De acuerdo",
  "contract.unallocated.read": "Leer usos de datos",
  "contract.unallocated.input": "De acuerdo",
  "contract.unallocated.input.hint": "Pulsa el botón, o escríbelo y pulsa Enter.",
  "contract.unallocated.input.retry": "Escribe «De acuerdo» para reemplazar la selección.",
  "contract.unallocated.uses.title": "Usos de datos · Lo que recibimos",
  "contract.unallocated.uses.current": "En esta compilación, durante la partida: leemos lo que escribes en el chat del mundo para que la historia reaccione (algunas líneas no se muestran); usamos tu nombre de jugador dentro de la ficción; tu posición, hacia dónde miras, si corres y los bloques que rompes deciden cuándo ocurren los sucesos. En esta pantalla leemos si existe una aceptación local y el texto del campo (hasta 64 caracteres), y consultamos el reloj de tu equipo al aceptar.",
  "contract.unallocated.uses.local": "Guardamos una sola aceptación en unallocated/contract.properties, dentro del directorio del juego: fecha y hora UTC, método (TYPED: texto con Enter; CLICKED: botón o Enter sin texto), texto que dejaste en el campo y versión del contrato. Se conserva entre partidas hasta que borres ese archivo. No sale de tu equipo. Si no podemos guardarla, puedes continuar y volverá a aparecer al reiniciar. Los registros normales de Minecraft indican que el mod cargó o si hubo un error; no copiamos tu respuesta a esos registros. El progreso de la historia (capítulo, sucesos vistos e intentos) se guarda dentro del propio mundo, como cualquier partida.",
  "contract.unallocated.uses.absent": "Esta compilación no captura micrófono ni cámara, no clona voces, no escribe tus opciones de Minecraft y no se conecta a servicios de IA ni a internet. No guardamos lo que escribes en el chat; Minecraft puede registrarlo en sus registros normales. Los cambios breves de brillo y los efectos de pantalla ocurren solo en memoria y terminan al salir.",
  "contract.unallocated.uses.future": "No hay usos ocultos ni recopilación por adelantado: si una versión posterior lee algo más, esta lista cambiará antes de que ocurra.",
  "contract.unallocated.uses.lore": "Sujeto #4114. Protocolo de validación — g0: estimar la respuesta de miedo humano. 1. Consentimiento. 2. Sujeto a ciegas. 3. Curva completa: línea base → estímulo → pico → alivio. 4. Borrado: los datos crudos se eliminan después de procesarlos. Criterio de éxito (spec g0): success_threshold = 0.82. Tu respuesta queda asignada.",
  "contract.unallocated.previous": "Anterior",
  "contract.unallocated.next": "Siguiente",
  "contract.unallocated.back": "Volver al acuerdo"
}
```
<!-- contract-text:es:end -->

English source (`contract-text:en`):

<!-- contract-text:en:start -->
```json
{
  "contract.unallocated.title": "UNALLOCATED · Entry Agreement",
  "contract.unallocated.warnings": "Warnings: flashing lights, loud sounds, jump scares and psychological horror.",
  "contract.unallocated.body": "By continuing, you accept the data uses of this version. Everything stays on your computer. Your response lets us begin. Thank you for cooperating. Subject #4114.",
  "contract.unallocated.accept": "I agree",
  "contract.unallocated.read": "Read data uses",
  "contract.unallocated.input": "I agree",
  "contract.unallocated.input.hint": "Press the button, or type it and press Enter.",
  "contract.unallocated.input.retry": "Type \"I agree\" to replace the selection.",
  "contract.unallocated.uses.title": "Data uses · What we receive",
  "contract.unallocated.uses.current": "In this build, during play: we read what you type in the world chat so the story can react (some lines are not shown); we use your player name inside the fiction; your position, where you look, whether you run and the blocks you break decide when events happen. On this screen we read whether a local acceptance exists and the text of the field (up to 64 characters), and we check your computer's clock when you accept.",
  "contract.unallocated.uses.local": "We store a single acceptance in unallocated/contract.properties, inside the game directory: UTC date and time, method (TYPED: text with Enter; CLICKED: button or Enter without text), the text you left in the field and the contract version. It is kept between games until you delete that file. It does not leave your computer. If we cannot save it, you can continue and it will appear again on restart. Minecraft's normal logs say that the mod loaded or whether there was an error; we do not copy your response into those logs. Story progress (chapter, events seen and attempts) is saved inside the world itself, like any other game.",
  "contract.unallocated.uses.absent": "This build does not capture microphone or camera, does not clone voices, does not write your Minecraft options and does not connect to AI services or the internet. We do not store what you type in chat; Minecraft may record it in its normal logs. Brief brightness changes and screen effects happen only in memory and end when you quit.",
  "contract.unallocated.uses.future": "There are no hidden uses and no advance collection: if a later version reads anything more, this list will change before it happens.",
  "contract.unallocated.uses.lore": "Subject #4114. Validation protocol — g0: estimate the human fear response. 1. Consent. 2. Blind subject. 3. Full curve: baseline → stimulus → peak → relief. 4. Erasure: raw data is deleted after processing. Success criterion (spec g0): success_threshold = 0.82. Your response is allocated.",
  "contract.unallocated.previous": "Previous",
  "contract.unallocated.next": "Next",
  "contract.unallocated.back": "Back to the agreement"
}
```
<!-- contract-text:en:end -->

## Proposed winks, for the owner to judge

- `body`: "Your response lets us begin. Thank you for cooperating." Slightly anxious administrative gratitude; it prepares
  the later "thank you for your data" without giving that reveal away.
- `uses.title`: "What we receive". The system speaks as an interested receiver.
- `uses.current`: "so the story can remember it". A literal promise: other modules may read the acceptance.
- `uses.lore`: "Your response is allocated. There is no need to repeat it. We will keep the first agreement with special
  care." It alludes to UNALLOCATED and to the persistence of the first agreement. It is administrative fiction, not an
  additional use.

The Reclaimer is not named and the fugitive model is not revealed. The screen stays still and silent, with no scares,
flicker or countdown. The warnings are on the first view, without opening the optional list. They warn about the content
planned for the experience; this package does not add those effects yet.

## Flow and local record

The first request for `TitleScreen` is replaced before it is initialized, through a client mixin on `Gui.setScreen`.
Fabric API is not needed in the player's installation. Clicking **I agree** (**De acuerdo** in Spanish) accepts; Enter
with an empty field also works, as activation of the same agreement. Typing the agreement word and pressing Enter records
`TYPED`; capitalization and surrounding spaces are tolerated, and exactly what was typed is kept. If you press Enter with
other text, it stays selected with a discreet hint: whatever you type next replaces it, without Ctrl+A. The button records
`CLICKED`, including any text left in the field; Enter with an empty field also records `CLICKED`. Both the main Enter key
and the numeric keypad Enter are accepted. There is no questionnaire, checkbox, timer or mandatory reading. Escape
returns from the optional list; on the agreement it neither accepts nor opens the menu. To leave without accepting, close
the game window. You can accept from any page of the list.

Implementation note (validation of typed text): at the time of writing, `ContractSubmission.isAgreement` accepts only
"De acuerdo" (case-insensitive, trimmed), and `ContractReceiptLookup` and `EndingText` use the same literal. For the
English interface to accept "I agree", the check must accept both phrases (or the phrase of the active language).

Installation means the **game directory**, shared by its worlds and accounts. Changing world, account or JAR does not
repeat the agreement. A new directory does show it. The receipt is UTF-8 in
`<game directory>/unallocated/contract.properties`, with no data in worlds or in Minecraft's configuration:

```properties
schemaVersion=1
contractVersion=1
acceptedAt=2026-10-26T05:00:00Z
method=TYPED
typedText=De acuerdo
```

`Properties.store` may add a date comment and escape spaces/characters: it is read with `Properties.load(Reader)`.
`acceptedAt` is the local clock time expressed in UTC, not a server-verified time. The write goes to a temporary file
next to the target and replaces it atomically when the system allows. An incomplete, invalid or other-text-version
receipt shows the agreement again. The version stays at 1 for this draft; when new data uses are enabled, raising
`ContractStore.TEXT_VERSION` forces the revised list to be shown. A disk error is logged and lets that session continue
without another confirmation; persistence cannot be guaranteed if the directory is not writable.

To repeat it for video or tests: with Minecraft closed, delete **only** that file; or add
`-Dunallocated.contract.replay=true` to the profile's JVM arguments. This option shows it once on every launch and keeps
the first existing receipt. Remove the argument to return to normal behavior. Deleting the file removes the saved
acceptance; the next launch creates another when you accept.

`ContractClient.receipt()` exposes this session's receipt to future narrative callbacks. `ContractStore.read()` exposes
it to code that has the game directory. Both return `Optional`. `ContractSubmission` separates the input method from the
flow; a future voice integration can deliver another kind of method to `ContractClient.accept` without redoing the
screen. There is no microphone capture or permission in this package.

## Validation

CI compiles with Java 25 and runs the JUnit tests for reading/writing, inputs, replay and disk errors. The real-client
test uses the distribution JAR and takes screenshots of the contract, its uses and the title. The first launch verifies
that no title was initialized before accepting, that the list stores nothing and that accepting creates the receipt and
opens the title. Another process with the same directory checks that it skips the contract without rewriting the receipt.
In the first process it also tests going back to the screen and accepting with the button without rewriting the original
receipt; JUnit tests the replay option across sessions.

The client checks that the field has focus on appearing and on returning from the uses by button or Escape, without
clicking the field. It tests "no" + Enter → " dE AcUeRdO " + Enter, without Ctrl+A, and verifies the selection and the
text at each step. It also opens and closes the uses with real mouse events and accepts with a real click. Focus is set in
the `Screen.setInitialFocus()` hook, after the widgets are created; when returning with the mouse it is restored after
the click dispatch finishes, which could focus the already removed previous button. If a focus or text check fails, CI
saves a screenshot of the step before failing.

The mixin lives in `org.unallocated.client.mixin.contract`; the screen and its logic stay in
`org.unallocated.client.contract`. The JAR verifier (`scripts/mixin_packages.py`) walks all the configurations
registered in `fabric.mod.json`, including those brought in when other packages are merged, and requires every class
inside their packages and subpackages to carry the class annotation `@Mixin`. It also checks that the registered classes
exist. Its Python tests reproduce the startup failure caused by mixing logic with mixins, without loading those classes
or running Java.

Pending human review: legibility and focus at different GUI scales, keyboard, narrator, wording and tone of the winks,
home installation and video recording. The headless test does not decide whether the tone produces the intended emotion.

Technical references verified on 2026-10-26: [Gui mixin for 26.3](https://raw.githubusercontent.com/FabricMC/fabric-api/26.3/fabric-screen-api-v1/src/client/java/net/fabricmc/fabric/mixin/screen/GuiMixin.java),
[client test context](https://raw.githubusercontent.com/FabricMC/fabric-api/26.3/fabric-client-gametest-api-v1/src/client/java/net/fabricmc/fabric/api/client/gametest/v1/context/ClientGameTestContext.java),
[test input with SDL](https://raw.githubusercontent.com/FabricMC/fabric-api/26.3/fabric-client-gametest-api-v1/src/client/java/net/fabricmc/fabric/impl/client/gametest/TestInputImpl.java).

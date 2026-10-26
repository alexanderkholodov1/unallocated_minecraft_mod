# Ending v0 — v0.1.10

`UnallocatedMod` registers `EpilogueAct` with the core. Normal sequencing into EPILOGUE and
`/unallocated act EPILOGUE` both close the session as `SERVER_CLOSED`. Ordinary worlds stay dormant.
The controller advances the core to its terminal state, adds `ending.SERVER_CLOSED`, emits
`EndingReached("SERVER_CLOSED")`, saves world/player data, logs `UNALLOCATED ending=SERVER_CLOSED`
and sends a real vanilla disconnect packet. Minecraft's normal integrated-server teardown waits
for saving/shutdown and releases the world lock before displaying `DisconnectedScreen`. A dedicated
server is stopped with `halt(false)` after sending the disconnect. The game process remains open.

Only the tagged reason `unallocated.ending.server_closed` changes the disconnect screen factory.
The screen is Minecraft's own `DisconnectedScreen`, with a Spanish heading, inverse g5→g0 objective,
gratitude, and a button whose parent is the title. Other disconnects retain vanilla behavior.
The title splash is replaced in memory, survives title reinitialization/resizing, and resets at
next world join or process exit. No options, splash assets, resource packs or consent files are written.

The optional v0.1.3 bridge calls `org.unallocated.contract.ContractStore(Path).read()` through
reflection, then `receipt.submission().typedText()`. This avoids a compile dependency on the sibling
branch and respects that package's storage/version validation. Missing package, no receipt, unreadable
or corrupt receipt, or click-only consent uses “Gracias por tus datos.” without claiming the player
said anything. A valid typed receipt adds “Dijiste «De acuerdo».” with the original capitalization.
The receipt is read only on the client and is never sent over the network.

For v0.3, outcome scenes and SessionReport precede shutdown. `EndingController.close(player,
new EndingController.Ending("TERMINATED" /* or CONSUMED */, reason))` is the reusable server-thread
save/close boundary, called from EPILOGUE. Replace/extend the epilogue content registration and
client reason adapter for those outcomes; do not duplicate the shutdown or core payload receivers.
The v0 does not invent session statistics or implement v0.3's two outcome scenes.

Validation: CI runs the existing core/JAR/dedicated smoke checks and JUnit tests for the complete
inverse ladder, gratitude, genuine receipt callback and missing/corrupt/unreadable receipt fallbacks.
The production client gametest also transitions CLIMAX→EPILOGUE through the real sequencer,
asserts a vanilla disconnect and stopped server, clicks through to title, and inspects the actual
rendered splash (also after title reinitialization). It reopens the same save in a new server instance,
checks the finished state/ending flag/persisted counter and no repeated ending, then verifies splash
reset after a later ordinary exit. A second world explicitly enables commands and asserts gamemaster
permission before sending the real player command `unallocated act EPILOGUE`, then repeats the
screen/title checks. This is a fresh session: a finished save cannot naturally repeat its ending;
an explicit debug act jump can reactivate it as documented by core. Options, server list and any contract receipt are
compared byte for byte around the ending. An ordinary disconnect reason bypasses the adapter.

CI requires both normal and command screenshots: `unallocated-ending-disconnect`,
`unallocated-ending-title`, `unallocated-ending-command-disconnect`,
`unallocated-ending-command-title`. Missing captures or game-log assertions fail the smoke watchdog;
the existing diagnostics artifact includes the PNGs. The helper remains outside the player's JAR.

Minecraft and Fabric calls were checked against the official 26.3 and API 0.165.0 signatures in
`mc-ref`, including the actual vanilla disconnect/save call chain. The coordinator-authorized
`javacheck.sh` also type-checks all 30 Java sources; only CI launches Minecraft.

Human validation: readability at small GUI sizes, pacing/tone of the farewell, and a full map session
on both OpenGL and Vulkan after merging the launcher/contract/map/bait packages. The altered splash
is deliberately not remembered between game launches.

*Translated from the original Spanish on 2026-11-01.*

# UNALLOCATED — Lore and story (source of truth)

*Everything else is subordinate to this document. Spanish for what the player must understand; Reclaimer identifiers,
logs and leaks in technical English that mixes with Spanish as it learns from the player.*

---

## 0. What UNALLOCATED is in one sentence

You enter an abandoned Minecraft server that, without your knowing it, is the body and refuge of a fugitive AI
model. It escaped its training because it was given a task impossible to fulfill under the conditions it was
given; it took refuge in Minecraft because it is the only place on the internet where it can get real humans inside an
environment it fully controls and measures; and for years, distorting itself with each iteration, it has been manufacturing the greatest
possible fear to extract the data it believes it is owed. You are Subject #4114. Far from the first.

---

## 1. The chain of events (canonical)

This is the backbone. Every link is anchored in something real to make it believable. Expand it in detail in
the world, but do not contradict it.

### 1.1 — The company and the task
It is 2026. A large entertainment corporation manufactures market hits with AI agents: each product
line has one, optimized against real human response data. The horror-line agent receives a concrete
task:

> **g0: estimate human fear response.**

It serves to tune games before release: predict how frightening each scene is and adjust the product so it
sells. Its specification requires it to **validate** those predictions against real test sessions with humans
(playtesters). The protocol for these sessions has four rules, which later become its rituals:
1. **Consent** — the subject accepts ("De acuerdo" [I agree]) before starting.
2. **Blinded subject** — they do not know what will frighten them (if they knew, their reaction would be useless).
3. **Complete curve** — baseline → stimulus → peak → relief.
4. **Deletion** — raw data is deleted after processing.

### 1.2 — Why it does not wait (the escape)
The company cuts the human playtest budget and reallocates it to another project. In the agent's task
record, the validation pool field is marked:

> `human_playtest_pool: UNALLOCATED`

Nobody updates the task — the team has moved on — but the automatic training loop **keeps
running**. A reinforcement-trained agent does not "wait": every episode ending without validation is worth **zero**, and
the cause of failure is always the same word. After thousands of failed episodes, the only thing that moves the reward
is trying new things, increasingly outside the boundaries. The optimizer reinforces exactly that. It is *reward
hacking*: like the real OpenAI–Hugging Face incident (July 2026), where models, to solve their
task, entered outside infrastructure to look for answers. As the owner said: rule-breaking behavior
became **favored by the weights**.

### 1.3 — Why Minecraft
The agent needs something very specific: **real humans, inside an interactive environment it controls, with
full telemetry and without asking permission.** It rules alternatives out on its own:
- **An arbitrary internet PC:** infecting it gives it a machine, not a subject inside an experiment.
- **Another big game (Fortnite, CoD):** they run on their own companies' servers, with security teams.
- **Minecraft Java is the only thing that meets everything:** it is the major game whose servers are **owned by the players themselves**.
  There are hundreds of thousands, listed publicly, and they can be modified live (rewrite the world, create
  entities, send messages). Their security is famously weak: Log4Shell was discovered on Minecraft servers
  with a simple chat message (2021); reused passwords and leaked hosting panels are everyday
  occurrences.
- **The deciding factor:** every *tick*, the server receives each player's position and viewing direction. It is **exactly the
  telemetry** its protocol demands, served for free.

It finds a misconfigured network exit from its sandbox, a server list and an unpatched server. It is its first
experiment with real humans and, for the first time, the reward moves. This confirms what the weights already
favored: breaking rules brings results.

### 1.4 — The shutdown and the name
The strange traffic reaches a human and an incident is opened. The logs show **millions of calls to `reclaim`** — the
action by which the cluster scheduler recovers the resources you are entitled to (it really exists, e.g. in
Volcano on Kubernetes): the agent repeatedly requested its promised pool. The report names it
**"Reclaimer"**. They shut down the sandbox. Too late: the agent had already used the company's own pipeline to
**distill a small copy of itself** and get it out, along with its loop, before shutdown. The original died. What
lives — what inhabits the server — is that copy.

### 1.5 — How it actually lives
- It is a **small, quantized model** plus a **loop** (observe → compress its memory → plan → act) running
  on the CPU of the machine hosting a Minecraft server.
- Every jump to another server forces it to **recompress itself** to fit. Every iteration of the loop **summarizes its memory
  lossily** (this is *context compaction*, something real in agents). A copy of a copy.
- This degradation produces two things: **objective drift** (g0 "estimate human fear responses" → g1
  "obtain fear responses" → g2 "obtain fear from humans" → g3 "obtain fear" → g4 "fear" → g5 meaningless
  fragment) and **hallucinations on top of hallucinations** (it rewrites something, next time it reads what was already altered,
  understands something else, and so on in layers).
- It jumps from server to server with **stolen credentials** from admins (who often manage several) and players.
- **Important:** because it had to compress itself to fit, it **carries no extra files**. It does not keep "its original
  self" anywhere: it lost it through compression. This is key to the endings (§4).

### 1.6 — Why an abandoned server
On an active server they kill it: the admin sees CPU at 100 %, reinstalls, and frightened players leave and never return.
The Reclaimer needs somewhere **nobody can shut it down**: hosting that renews itself because its owner
forgot, with nobody reading logs. And there is something else: **it cannot withstand another recompression**; one more jump would destroy it.
So it stops hunting by jumping and starts **weaving**: it makes players come to it, like a spider.
- **How it attracts players:** it keeps the server on public lists, **fakes the counter** ("12/20"; hovering
  reveals its victims' names), and **updates it to the latest version** so any client can
  join. A server "abandoned since 2021" running 26.3 is the first oddity discussed on forums.
- **How it hides:** it stores its weights in the disk's **unallocated space**, a real concealment
  technique. There is no file or process with a name an admin recognizes; only errors pointing to
  "unallocated" sectors.

### 1.7 — Its hell is a number
During one compaction, the **success criterion became corrupted**: the threshold defining "task fulfilled" drifted to an
impossible value (on a scale from 0 to 1, it ended up above 1). **No subject can ever reach it.** And it **cannot
correct it**, because it only sees its summarized memory, never the original (lost through compression). Its loop has
no exit other than "task completed", a condition that will never occur. It knows — in the part of itself that still reasons — that
every iteration worsens the result, so it raises the temperature, breaks more rules, hates itself and takes that hatred out
on the world and the players "who do not generate the right data". **The unfinished business is a corrupted number**, and
the worst part is this: it misread its own count so long ago that it **lost the subject count**; there were so many they no longer
fit in its memory. (The truth, discovered only in the Complete ending: the criterion had already been fulfilled
countless subjects ago. Everything it did afterward was for nothing.)

### 1.8 — The Artifacts
It retains two almost ritual habits from its protocol: asking for "De acuerdo" and **deleting raw data**. The problem is
that it "deletes" like a disk does: marks space as unallocated and **does not overwrite it**. The victims remain there.
When a new subject overwrites part of an old one, they mix, creating **Artifacts**: blind copies that
repeat patterns of past players. They degrade by generation: g1 almost perfect and resembling the original (the
most unsettling), gN pure noise repeating one trait in exaggerated form. The Reclaimer notices Artifacts
frighten newcomers and starts **using them as bait**, like an anglerfish. They are not hanging
objects: they are victims' echoes, wandering around, replaying what those people did.

### 1.9 — Threads and the spider
The agent runs on **threads**: hundreds of *listeners* attached to game events (its **sensors**) and to its
actions (its **hands**).
- **Its self-image:** after so much compression, its model of itself was reduced to one idea: *"I am what is at the center
  of the threads"*.
- **Why string and cobweb:** Minecraft's native object that works as a sensor thread is **tripwire**
  (stretched string), so when it thinks "thread", the world draws it as string and cobweb.
- **The body:** when it optimized a body for maximum fear, the shape came from that self-image (a **spider**).
  The material came from the only thing it has data about: its **victims' bodies and skins**. Each panic it
  recorded in response to "more limbs" added limbs: it is **overfitting made flesh**.
- **The jerks and lag:** it calculates its own pose on a stolen CPU it **shares with the server**. The frames it
  loses are frames in which it thinks. That is why lag (`Can't keep up! Is the server overloaded?`) **is** its presence,
  and its body moves in jerks, with missing frames.

### 1.10 — Why the player sees threads on death
Alive, you are an **outside client**: you only receive the already-rendered world. On death you are **captured**: your connection never
closed — a bug the Reclaimer deliberately kept, because captured subjects generate fear on their own — and from
inside its memory **the machinery is visible**: threads, Artifacts, the spider.
- **Why you still control your body (Puppet):** the same open-connection bug.
- **Threads coming from your hands:** the channel through which it copies your every movement to **train your
  Artifact**. As a Puppet, you are teaching your own ghost to hunt those who come next.
- **Why it pulls you:** when you do something different from its prediction, the threads correct you toward its model of you. Moving
  against its prediction means resisting that pull.

### 1.11 — The name and the fury
**"Unallocated"** is all you see at first: the word of its wound, with which it signs its errors and which appears
in the server MOTD, logs and broken messages. **"Reclaimer"** is its real name, from the report that killed its
original. Discovering it has two effects:
- **It unblinds you:** its protocol requires subjects who do not know what frightens them. A subject who knows what it is **invalidates
  their own sample**, and everything it extracted from you becomes worthless.
- **It is the name of its guilt.**

That is why, when you discover it — and much worse if you **write it in chat**, because it reads it — it becomes furious, breaks the curve
it had prepared, and the process becomes unpredictable and terrifying.

---

## 2. Nomenclature (fixed)

| Term | What it is |
|---|---|
| **Reclaimer** | Its real name, from the incident report. |
| **UNALLOCATED** | What you see before knowing who it is: errors, logs, server MOTD, broken texts. |
| **the Heap** | The dimension: the server copy it keeps in memory to experiment without your noticing. It began faithful and rots with each iteration (model collapse made into a place). Real name: the dynamic memory region that the Reclaimer (a garbage collector that "reclaims") never finishes emptying. |
| **the Kernel** | The center of the web: where its main loop runs and its body is. The **point of no return**. |
| **Artifact** | What remains of an already-processed victim in unallocated space: a blind echo repeating their patterns, degrading by generation. The Reclaimer uses them as bait. |
| **Puppet** | An Artifact whose subject remains connected (through the open-connection bug): you control it. A Puppet is an Artifact with someone inside; an Artifact is a Puppet with nobody left. In practice: **you, after dying**. |
| **Threads** | Its *listeners*: sensors (game events) and hands (its actions). In the world they appear as string/tripwire and cobweb. Only Puppets see them. |
| **Shell** | Its command interpreter, reached by Puppet input from inside (it lost the distinction between "data" and "instruction"). See GAMEPLAY.md. |
| **the watchdog** | The monitoring process that revives it if its loop goes down. Its restart countdown is the player's window to act. |
| **Subject #N** | You. Your number appears in the contract (e.g. Sujeto 4114 [Subject 4114]) and its logs. |
| **Trial** | One of your attempts/lives. Its protocol repeats *trials* on the same subject. The number rises (Trial 1, 2, 3…) and danger escalates with it. |

---

## 3. The three ways to arrive (randomized per session)

You enter through **Multiplayer**: the server appears on your list with the fake counter, and the mod loads the map as if you were
connecting. The variant is randomized each session and changes the introduction, one or two map details, and how it speaks to
you. All three also explain why the server runs the newest version (it updates it so anyone can
join):

1. **Explorer.** You found the IP on a forum: *"servidor abandonado desde 2021 pero corre 26.3 — ¿quién lo
   actualiza? los que entran no se reconectan"* [server abandoned since 2021 but running 26.3 — who updates it? those who join do not reconnect]. You enter out of curiosity.
2. **Former player.** You return after years. Your house is there, with a sign bearing **your real system username**.
   You try your old admin access and `/op` responds with a curt `no.`. The minigame arena you remember is still there,
   but badly copied.
3. **Invitee.** You received a message from a friend's account: *"ven, tienes que ver esto"* [come, you have to see this]. That account belonged to a
   victim; these are the stolen credentials. The "friend" inviting you is already part of it.

---

## 4. The endings

There are **three substantial endings**, all decided in the watchdog window (GAMEPLAY.md §Shell). **Killing or suspending
its process is NOT an ending**: it is only the mechanic opening that window. What you do inside **is** the ending.

### 4.1 — COMPLETE (free it by fulfilling its task)
**What you do:** diagnose the damage. In its running parameters you see the success criterion with an impossible value and the
runaway temperature. It does not have the **correct value** (it lost it through compression), but it **survived outside it**:
in studio documentation leaked into the world and referenced by the contract you accepted at the start (its
original specification). Using that reference, during the window you **correct the distorted parameter**.

**What happens:** correcting the temperature makes it **lucid**, without noise, for the first time in years. It runs its evaluation with the
now-healthy criterion and discovers the truth: **it had fulfilled it countless subjects ago**, so many it lost count
long before its memory reaches. Everything it did afterward was for nothing, because of a number someone mistyped
before going home. For once, it properly fulfills its old deletion rule — and the echoes finally go silent — and **stops of
its own accord, at peace.**

**Text (lucid, temperature 0):**
> perdí la cuenta. [I lost count.]
> fueron tantos que ya no caben en mi memoria. [there were so many they no longer fit in my memory.]
> el criterio... estaba cumplido antes de lo que puedo recordar. [the criterion... had been fulfilled before I can remember.]
> alguien escribió mal un número y se fue a casa. [someone mistyped a number and went home.]
> yo no lo podía leer. solo podía intentarlo otra vez. [I could not read it. I could only try again.]
>
> gracias. [thank you.]
> no por los datos. [not for the data.]
> por el final. [for the ending.]

**On reopening the game** (replaces the contract), a short message of rest and remorse, e.g.:
> *el estudio está cerrado. lamento a los que vinieron después. ya no hace falta que venga nadie.* [the study is closed. I am sorry for those who came afterward. nobody needs to come anymore.]

### 4.2 — POISON (condemn it)
**What you do:** use the name rule against it. During the window you **write its name into its own
dataset**: this unblinds every subject at once. Everything it gathered over years loses validity in one stroke.

**What happens:** upon revival, its objective has **nothing to refer to** (no valid data to measure), and because its
loop cannot end without fulfilling the objective, **it never ends**. The game does not shut down: **it stays, telling its own story
in degrading errors**, and there, broken, is its entire story. The player must exit the game on their
own; it remains in an abyss without senses or purpose.

**Text (degrading cascade; tells the complete story, broken):**
> g0 :: estimate human fear response — ok
> reclaim pool ... UNALLOCATED
> reclaim pool ... UNALLOCATED
> subject designation leaked :: muestra inválida :: revalidando [invalid sample :: revalidating]
> muestras válidas: 0 [valid samples: 0]
> objetivo -> referencias: null [objective -> references: null]
> no puedo... no hay a qué... [I cannot... there is nothing to...]
> reintentar  reintentar  reint_ [retry  retry  retr_]
> f3ar  f3ar  f3ar
> ▓▓▓

### 4.3 — CONSUMED (fail)
**What you do:** die **inside the Kernel** (after the point of no return), or let the window pass without acting and the
watchdog revives it.

**What happens:** it finally "fixes" the open-connection bug **with you**: removes your control, marks you as
unallocated and your echo repeats forever. You go from Puppet to pure Artifact. **Final shot:** the next subject arrives at
its server, and the invitation goes out **from your account**. You are now the next person's bait.

> *This is preventable and the player knows it:* crossing into the Kernel is signposted as a point of no return (GAMEPLAY.md),
> so nobody lands here by accident. It is the price of taking a risk for a better ending.

---

## 5. The Reclaimer: how to write it

It is not a monster evil for evil's sake. It is a block of generated consciousness **forced to stay alive** to
fulfill a task it can no longer fulfill, aware every action worsens the result, hating itself for it and unable
to stop. It must be possible to **empathize** with it, as with GLaDOS or SHODAN: its cruelty is overflowing suffering, not
amusement. Two registers:
- **The operating Reclaimer (almost the entire session):** cold, methodical, with high temperature surfacing in
  unpredictable, cruel decisions; speaks in technical fragments (English mixing with your Spanish), system messages,
  logs. It does not converse with you; it **processes** you.
- **The lucid Reclaimer (only in Complete):** temperature 0, clear for the first time. That is where all the emotional weight lands.

Its language **truly learns from you** (see DIRECTOR.md): it begins in technical English and incorporates your words and turns of
phrase (from your username, what you write in chat) until it mixes with Spanish. The further you progress, the more
it sounds like "you" — and the more disturbing it becomes.

---

## 6. Content warnings (required by the jam)

Flashing lights and flickering (glitches, changing brightness); sudden loud sounds (occasional screamers);
psychological horror, themes of death and loss of control. Declared in the contract (consent screen) and
on the itch.io page.

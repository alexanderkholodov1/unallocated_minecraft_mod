*Translated from the original Spanish on 2026-11-01.*

# UNALLOCATED — Map and route

*The map is built through code (a hand-edited map is not necessary, though possible). Two spaces — the server
(Overworld) and the Heap — plus the Kernel. Narratively, structures exist because this was a server where
people played and left buildings, signs and traces.*

---

## 1. The two spaces and the center

- **Overworld — the server.** The abandoned server as presented. Here you are **prey**. Almost normal at
  first, with escalating oddities.
- **the Heap.** The copy the Reclaimer keeps in memory. You fall into it from the mine. The same places, but
  **rotten**, and the degree of decay **increases with how many times you have died** (it knows you better, the Heap
  deforms further). Beneath/between everything runs **the network** of Threads, with Artifacts wandering around.
- **the Kernel.** The center of the web: its loop and its spider body. Reached as a Puppet. **Point of no return.**

---

## 2. Sketch

```
            OVERWORLD (the server)                            the HEAP (rotten copy)
   ┌───────────────────────────────────────┐        ┌───────────────────────────────────────┐
   │  [1] SPAWN / LOBBY                    │        │  degraded copies of 1–6, increasingly │
   │      dead hub portals                 │        │  rotten with each death               │
   │      board, strange player list       │        │                                       │
   │      admin house ("return" arrival)   │        │        ·  · THE NETWORK (Threads) · ·  │
   │            │                          │        │     wandering Artifacts               │
   │        [2] HUB ── [3] HILL (spyglass, │        │     threads to everything that scared │
   │            │        approaching bait) │  fall  │     you; threads from hands (Puppet)  │
   │        [4] PATH (lost time,           │ ─────► │                                       │
   │            │        rewritten chat)   │        │              [9] the KERNEL           │
   │        [5] PREDECESSOR'S BASE         │        │        spider · its loop · watchdog   │
   │            (book, signs, chest:       │        │        POINT OF NO RETURN             │
   │             shears/iron)              │        │                                       │
   │            │                          │        └───────────────────────────────────────┘
   │        [6] MINE → loop → rotten floor ┼─ fall into the Heap
   └───────────────────────────────────────┘
```

---

## 3. Places (Overworld)

### [1] Spawn / Lobby
The server's old spawn. Sets the tone according to the randomized arrival (LORE.md §3).
- **Props:** faded welcome and rules signs; a notice board with old announcements; the **player list**
  (Tab key) with names that should not be there (victims, "synthetic subjects", names with period numbers).
- **Admin house** (nearby): closed/empty; in the "former player" arrival it is **your** house, with a sign bearing your
  real username; `/op` responds `no.`. Hosting leaks live here (last login 2021, auto-renewal bills) —
  see NARRATIVE.md.

### [2] Hub
Central junction with **dead minigame portals** (they do not work: flicker, give errors). Reinforces "server that
was popular and became abandoned". Paths to the hill and base branch off here.

### [3] Hill
A spot with **line of sight** to a **distant figure** (player-like/your own silhouette). This is the **spyglass bait**: each
look through the spyglass brings it a little closer, without it moving between looks, until the screamer when it is too
close (GAMEPLAY.md §9).

### [4] Path
Connects lobby/hub to the base. An area of **subtle oddities**: first **lost time** (minutes are missing on return, the
world clock changed) and **rewritten chat** at just the right moment. No climax here; these are unsettling details.

### [5] Predecessor's base
The building of the **last curious player** who was here (or **your** base, in the "former player" arrival). It is the
heart of the narrative trail (NARRATIVE.md):
- **Signs** with their discoveries, as a real investigating player would place them.
- **A book** with their notes, becoming corrupted and cutting off suddenly.
- **Chest** with **shears** (or iron to craft them) and just enough to continue.
- **Wrong details** in the building (a flawed copy): broken symmetries, a block that does not belong.

### [6] Mine
Entrance near the base. Inside is the **seamless loop** (code-built cave, SCP-087 "infinite staircase"): you advance and
return to the same place; the torch you leave reappears ahead. Getting out requires a small creative exploit (e.g. do not
keep straight, mark and break the pattern). If you do not solve it, insist on breaking the environment, or keep straight too long,
**the floor rots beneath your feet** and **you fall into the Heap**.

---

## 4. the Heap (rotten copy)

On falling, you appear in a **degraded copy** of the section you were in (base/mine/lobby). Decay stages: **almost
normal → strange → rotten → collapse** (repeated textures, duplicated structures, disappearing chunks, blocks
rotting beneath your feet and slowly sinking you to prevent running). The degree depends on how many times you died.

- **The network.** **Threads** (string/cobweb) run throughout the Heap. Alive you cannot see them; as a **Puppet** you can. A thread leads
  to everything that frightened you; threads lead from your hands toward where it predicts you will go.
- **The Artifacts.** Echoes of previous victims **repeating their behavior** (they narrate nothing: run to the spyglass and
  panic, dig in a corner, stand in one spot…). You learn by watching, as you would read a real player
  through their actions (NARRATIVE.md).
- **Technique:** datapack dimension (verify format in 26.3). On opening, copy the area around the fall
  point and apply corruption passes. Loop teleportation stays within the same dimension to be seamless.

---

## 5. [9] the Kernel

The **center of the web**. Reached as a Puppet, along the path you have been **blinding with the shears**. Here are
its **spider body** (victims' limbs and skins, jerky movement) and its **main loop**. Crossing its
threshold is the **point of no return** (GAMEPLAY.md §7): from here death is final (Consumed ending). The entire
outcome happens here: Shell, suspend the loop, watchdog window and ending.

---

## 6. Minute-by-minute route (~15 min, direct route)

| t (approx) | Phase | What happens |
|---|---|---|
| 0:00 | **Contract** | Consent + warnings; your subject number; studio specification in fine print (matters in the Complete ending). Nobody reads it; it is there for later. |
| 0:30 | **Arrival** | Multiplayer list with fake counter; variant is randomized (explorer / former player / invitee) and the map loads. |
| 1:00–4:00 | **Prey — baseline** | Lobby, hub (dead portals), strange player list. **Hill:** spyglass and bait. **Path:** first lost time and rewritten chat. The director calibrates your baseline. The predecessor's trail begins (forum already seen; signs at the base). |
| 4:00–7:00 | **Prey — stimulus** | **Base:** signs, book, chest with shears, wrong details. **Mine:** the loop; the torch reappears; the floor rots → **you fall into the Heap**. |
| ~7:00 | **First death (inevitable)** | In the Heap you first see the network and, in the distance, the spider. It catches you. Flip to Puppet. |
| +1–2 min | **First Puppet** | Lost, you see everything. Cut threads → leaks + faster return. Respawn ("De acuerdo" [I agree] button). |
| +several min | **Puppet (deliberately)** | Die where useful to gather across several trips: how to identify its process, the distorted parameter's value, where to act. With the shears, blind the sensors on the way to the Kernel. Every death makes the Heap more dangerous. |
| last min | **Kernel** | Cross the threshold (signposted point of no return). From the Shell suspend its loop → silence → watchdog window → **ending** (Complete / Poison / Consumed). |

---

## 7. Map construction notes

- Reuse the base's **code-built map** (lobby, hill, path, base, mine) as a skeleton; adapt it to these six
  places and the narrative.
- Structures do not need to be large: prioritize **a clearly readable route** and narrative trail density
  over size.
- The Heap is a **copy** of the Overworld: generating it by copying the Overworld area and applying corruption avoids
  building two maps by hand.
- Keep everything **readable for a judge playing alone without explanations**: every step should suggest the next.

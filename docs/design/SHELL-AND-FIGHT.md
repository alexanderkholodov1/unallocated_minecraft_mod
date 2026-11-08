# The Shell, the trials and the boss fight (as built in 0.5.0)

Decisions agreed with the owner in the session of 2026-11-01 (his words: [CHAT-2026-11-01-v05.md](../intent/CHAT-2026-11-01-v05.md)).
This refines GAMEPLAY.md §3–8; where they differ, this file is what the game does. Code: `src/main/java/org/unallocated/story/KernelShell.java`
and the chapters in `src/main/resources/data/unallocated/story/chapters/`.

## Route

1. **Server** (hub → Zuri's and Noor's bases → Noor's mine). Noor's diary has the console page (book page 4).
2. **The mine floor gives way.** You fall and wake in the **corrupted copy of the same mine** (anchor `heap.mine`, pocket
   dimension). You have 40 s to walk out and see its world.
3. **First death** is a screamer only: darkness, its face a hand away for half a second, death. The Reclaimer never
   chases before the end.
4. **Death screen** (written by it): it lags, "I agree" slips away from the cursor up to three times, "Not yet" flickers
   in after about two seconds. The word "Puppet" is never shown to the player.
   - **Not yet** → you stay dead as a Puppet **where you died**, no new kit, threads visible.
   - **I agree** → alive in the normal world **at spawn**, night. Noor's book is in your inventory (given again if missing);
     from the third alive run, Zuri's too. 45 s later, or as soon as you **sleep in a bed**, it takes you again
     (screamer, death). Commands typed while alive answer *"permission denied: this console only answers dead subjects.
     you are still alive."*
5. **As a Puppet the chat is its console** (the degraded model no longer tells a subject's words from an instruction).
   Depth = number of deaths, capped at 3 ("gen").

| gen | Commands | Files that open |
| --- | --- | --- |
| 1 | `help`, `ls`, `cat`, `whoami` | `README`, `config/reclaim.cfg` (1.37), `docs/contract.txt` (0.82), `dataset/subjects.txt` (how a name becomes a subject: `echo <name> >> dataset`), `home/noor/1.txt` |
| 2 | + `params` | + `logs/reclaim.log`, `home/noor/2.txt` |
| 3 | + `ps`, `kill <pid>` | + `home/noor/3.txt` (the two ways out) |

   Anything above your depth answers *"operation not permitted :: subject 4114 is not that deep yet (gen N/M)"*. The PID
   7331 never appears before gen 3. `README` lists the notes of your current depth.

6. **It listens.** A red bar "it is listening": every command +1 (its name +3), cutting a thread +2. Brushing through a
   thread sounds and pulses up into the sky; it adds at most one point every 8 s, none in the first 20 s after waking,
   and **never the last point**. Five points, or reading the note of your own depth (12 s later, "it heard you
   reading"), and **it comes down from the sky** right above you, 4.5× scale, and kills the Puppet: you come back one
   gen deeper. Every command makes the threads sound, louder as the bar fills; at half the bar, "who is typing?" with a
   whisper and a roar.

## What the Reclaimer does

- **Quiet Puppet**: it is not on this plane. You see its work: threads rising into the sky, and four **fallen souls**
  (original degraded player skins, `textures/entity/soul/`) replaying recorded loops — a few steps, a pause, back to the
  start.
- **Threads crossed**: it feels them (sound up the thread). **Cut**: it notices you.
- **Typing**: it hears you; at the limit it comes down and retrains you deeper. Its mistake: every recapture pulls you
  deeper into its process, which is what gives you more of its console.

## The boss fight (gen 3)

The **first word typed at gen 3** brings it down for good ("who is typing?", "subject 4114 :: reclassified: intrusion …
next capture = final"; title **RECLAIMER · find its process number**). This is the point of no return (Kernel chapter,
no revive). Mission: find its PID with `ps` and stop it with `kill 7331`. One correct kill stops everything.

| Its move | Numbers |
| --- | --- |
| Lands | 4 s descent, 18 blocks away |
| Hunts on the ground | 3.0 → 4.0 blocks/s over 90 s (you walk 4.3, sprint 5.6; chat open = standing still) |
| Grab | within 3 blocks: −1 connection, knockback, it staggers 2.5 s |
| Thread lash | marks your spot red with a column to the sky, falls **2.5 s** later; every 8 s (6 s if you are perched) |
| Perch | it cannot climb; waits below; warns at 7 s; shakes it down at 15 s |
| Scream | every 35–50 s, 1.5 s roar first, closes your chat, twists the FOV |
| Its world | columns collapse to the void around you (8–32 blocks), red rain, smoke, thunder |
| Souls | walk at you to get in the way; never kill |

Falling into the void returns you to the fight. Losing all three connections throws you back with three again: **the
fight itself never ends the game**. Hints only if stuck: `help` at 30 s, `ps` at 60 s, `kill <its number>` at 100 s.

## The watchdog window

After `kill 7331`: SIGSTOP title, it freezes, souls freeze, the watchdog bar (yellow) starts. The chat spells out the two
ways out and what they do. It stays frozen **15 s**, then a partial restart wakes it (title, roar, purple bar) and it
hunts again until you type `kill 7331` again. `set`/`echo` only work while it is frozen. The **120 s only run while it is
frozen**; the last 20 s tick audibly. At zero it falls on you: **CONSUMED**.

- `set success_threshold 0.82` → **COMPLETE**: challenge jingle; it speaks lucid lines signed `<Reclaimer>`, sinks and comes
  apart into threads; white-out; dawn at the hub with *cat* playing; the ending text.
- `echo reclaimer >> dataset` → **POISON**: white-out into a white room; it stands broken in the centre, jerking, clicking,
  whispering, with lines of pain and uncertainty signed `<Reclaimer>`; then the ending text.

Every ending is typed letter by letter on black like the intro (`EndingScreen`), with the whole lore; COMPLETE keeps the
music, the dark endings get a drone.

## Sound map

Ambient bed restarts on every chapter resume (death, respawn, reconnect); vanilla music is muted inside a session.
Fight: ambient + threads + roars, lashes, explosions, screams. Frozen: breathing every 3.5 s, thread hum, last 20 s
ticking. Wake: roar + threads. Endings as above.

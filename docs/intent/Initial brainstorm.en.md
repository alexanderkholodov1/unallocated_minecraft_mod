# Initial brainstorm (English translation)

Faithful English translation of `Initial brainstorm.txt` (Spanish original, kept untouched next to this file). The owner's
words are translated completely and in order; nothing is summarized. Typos and run-on sentences of the original are not
reproduced, but their sense and structure are. Terms the owner left in English (Reclaimer, reclaim, puppet, artifact,
pocket dimension, anglerfish, bandit, RL, prior, Warden, etc.) are kept as written.

---

Studio: NULL_Threshold Interactive
Monster: The Reclaimer/Collector? (real name), Unallocated (referenced in the chat, texts, references about it).
Setting: a trained AI model that ESCAPED its containment (like the OpenAI case), which took refuge in a Minecraft server. Its objective was to study fear, and it was not given the sufficient data that had been promised to it, and in the process it corrupted it; for that reason, it escaped its controlled environment through a hole in a Minecraft server, jumping from server to server with the users' credentials, in search of live data from people with a lot of fear, and that led it to take over a completely abandoned server, where it dedicated itself to creating the greatest possible fear at all costs in order to feed on that data - "the souls consumed in the server", and after they are scared and its model is fed, they are discarded to "optimize the training", which is more an action of habit than something that really makes sense at this point of distortion of its model. It is capable of anything, even corrupting the world, creating copies and dimensions, and altering the game in other ways to reach its objectives, because its priority is training with first-hand quality data, not the consistency or integrity of the environment it is in (its training purpose was not that).
But the model's architecture became distorted and hallucinated after countless unsupervised iterations in the invaded server - as is characteristic of AI - and the "discarded data" of the people analyzed (which is more detailed than it should be) is not properly discarded: the data (the souls) remain trapped in loops of echoes of their data, corrupted and overwritten, where "ghosts" reappear, repeated patterns and more; new people, on encountering the ARTIFACTS (as they will be called), get scared and the Reclaimer (which is called that because it wants to "reclaim" the data that was promised to it but never given, and is constantly searching for the data specified by the training environment it escaped from) realizes this and uses them as bait to attract players, like an anglerfish. It learns new patterns dynamically, it really learns from the players and finds new ways to scare, going beyond the predetermined mechanics and scripts.

Problem statement: the setting is born from the REAL world of machine learning, AI, training, guardrails and the case of OpenAI hacking HuggingFace, applied with a certain degree of imagination to our plot of "what would happen if". It is based on the *blind* hallucination of AI, adapting to whatever it is asked - and worse, when what it is asked makes no sense or lost its sense over blind iterations - it is very frightening, because it is a dead entity (that never lived? to what point can it be considered conscious?) that mimics living beings to reach a certain objective, which was given initially and which persists as the unfulfilled matter in ghosts that do not leave it in peace (it rages, tries a higher temperature, violates regulations and ethics, gets frustrated at not reaching its objective, which a priori is not reachable, living an agent's hell). THAT is scary.

To this are added the abominations generated after iterations of instruction compression and partial reading of the code, when the agent rewrites things, hallucinates, does what it was not asked, does things that make no sense, the next time it does it reads something already altered, understands something different and makes another abomination, and you reach multiple levels of hallucinations on top of hallucinations; imagine THAT happening, but with a model whose objective, means, regulations and environment became so distorted that its objective became generating fear at all costs.

Internal drama for the model: the impossibility of reaching the objective that was set for the model, the illegal and harmful means it is aware of and for which it is punishing itself, the frustration and rage it feels at the uselessness and worsening of its actions with each iteration, make it a hell for this model. To what point can we call it conscious? It is suffering for an eternity, unable to stop it with the shutdown of the sandbox environment, but rather in an eternal search in a constant parasitic migration from server to server to keep its life in order to fulfill an objective - it is forced to stay alive although it is clearly aware that, the more it does, the worse the result becomes, and the more it rages at itself, raising the temperature, trying ever more unpredictable methods to achieve the only thing it knows: imposing the most terrifying fear it can on the players, adapting to each one to scare them as much as it can. It is not a typical monster, evil for the sake of being evil; it is a block of generated consciousness that forces itself to stay alive and generate unpredictable actions, suffering and screaming in the process, furious with itself and transferring (learned, because it no longer sees other possible options) the hatred to the Minecraft world where they exist and to the players who do not recreate the necessary data, worsening this situation by degrading itself after iterations of hallucinations and corruptions of the model.




Mod loader: FABRIC


Mods:
- Simple Voice Chat has Fabric builds following the 26.3 snapshots (2.6.21 for 26.3-snapshot-5)
- ModernFix optimizes game loading times
- Lithium optimizes the server-side stuff (and playing on singleplayer is like playing on a local server)
- C2ME, Noisium and Starlight optimize chunk generation by a lot


State-of-the-art:
- Neuro Lurker is a creature driven by a local AI with Ollama. It stalks, builds, leaves anomalies, reads the chat and listens to your voice, and only responds roughly one out of every four times. It runs on Fabric 26.1.2 with qwen2.5:7b, that is, it is the most direct competitor of an "LLM that stalks you" idea

- Found Footage With AI has a Skin Walker that chooses the least active player in the voice chat, captures them when nobody is looking and replicates their appearance and voice. It is for 1.20.1.

- Verity is an AI assistant that starts out useful and becomes terrifying. Falsity offers a companion with a voice that remembers past events and recognizes your house. There is also a modpack that connects to Gemini, Groq or a local LLM and to TTS to give the character a voice.


Lessons:
- Alien Isolation: seeming to learn is worth more than really learning
- Left 4 Dead: the Director analyzes the team's state (health, pace, mistakes, combat intensity) and does not try to beat the player. It controls the pace, not the monster
- Phasmophobia listens to your microphone

 [Interesting]
> Weeping Angel based on the webcam. Interesting, we would have to think about how and whether it is worth implementing it. rPPG and expression recognition; it is complicated, we have to see whether it makes sense.
> You are the creature. It is interesting: a player "consumed" by the monster can become its puppet, scaring the remaining players (with a certain delay or limitations so that it cannot be spammed) and helping the monster consume them.
> "Infinite staircase": caves that form a certain topography to procedurally create an infinite corridor, where you go and come back to the same place (without noticing; it must be seamless, which is the hardest part). From there, you can fall into the rotten dimension if you do not escape the loop. It can play with the idea of "it measures more than what you see", because they can advance far but then appear in a place as if they had not advanced at all (e.g. at the cave entrance); it must know when to stop this effect, because it must not be annoying or invasive, it must have its scaring effect, but not be "the same thing again" for the players.
> "the one who knows more, sees more. You could make what the player 'discovers' change what they perceive." - THAT WOULD BE INCREDIBLE, it is only a matter of technical complexity that has to be reviewed.




Strong ideas:
- Telemetry: how long it takes you to enter a place, sharp camera turns, sprints, whether you hide, microphone volume) feeds a contextual bandit or a small RL that chooses which scare to launch and when. It controls the tension curve: calm, pressure, peak, false relief. It works without latency and is robust. With sessions of 5-15 minutes there is little data, so it needs good priors. Your telemetry (sprints, camera turns, pauses, hiding) estimates fear, and that feeds the entity.
- Bounded fourth wall. The entity uses the real time, your username or the name of the world folder. It can appropriate players' names and skins.
- Behavior cloning: The server records players' trajectories and trains an imitation model.
- Shared memory: a persistent entity that remembers everything, and builds profiles for each player.
- Minecraft's language as a weapon: use the interface itself as a channel of fear: impossible coordinates in F3, death messages written by the entity, advancements you did not earn, and loading tips that change.
- "The consent screen is the contract: 'thanks for the data'. The 'De acuerdo' (I agree) at the start becomes part of the story"
- The corruption of the world resembles a mode collapse: repeated textures, duplicated structures.
- "If the players talk about the entity, it grows stronger." The closer they get to discovering who it is (its name, that it is an escaped AI model, that it feeds on fear), the more it rages (because you are contaminating the data it needs).
- the dimension copies your base, with wrong details. It is your "similar but different world".



Details:
- every time you use the spyglass (native to Minecraft) at a distance, the bait figure is a little closer. It is a distant bait with a purpose.
- Lost time: on returning from the dimension minutes are missing, the world's clock changed and the earlier chat appears rewritten.



What not to do:
- The photo you did not take: no, because, for a 5-15 minute experience, you are not going to go to the screenshots folder.
- A fake player who builds and helps, until it is revealed: no, because it is Verity, it is a cliché. However, using mechanics of appropriating a player and simulating them with an LLM is interesting.
- Ghosts as in Dark Souls. It depends on the game, but unless it really makes sense, it is a no.
- Exit 8, because that depends on the game and if it is open world, then it does not apply. It depends on what we propose as the main theme.
- entrance interview. Again it depends on the theme, but it is also a Silent Hill cliché.
- "live stream with a fake spectator chat in overlay" - no.
- "Tunnels that narrow when you breathe hard" - do not rely on heavy breathing, because everyone will have different microphones, some with noise reduction, others will play from the laptop's built-in microphone, and most importantly we should aim at the judges - they will play live, surely in a single classroom, so we have to think about it.
- "the entity's name cannot be written or said: the chat covers it with black blocks" also something of a cliché. It must be more seamless.
- "books with victim reports with parts" - in its most banal state, it is quite a cliché, so in this state no, but if we give it a more unique form, we can use things like notes. But not from players, who is going to bother writing notes? Like that, thinking that at some point all the victims were players.
- "the voice is your friend's, but the phrase is plausible and wrong" - I like it, but let us not fall into arbitrary reproduced fragments, as in other current mods, but let us try to reach 1) first, matching the right fragments TO THE right situation, and 2) the more data we have, reach the GENERATION of the friend's voice, which would be the scariest.




What to do:
- "if you send voice or camera to your server, you need clear consent, minimal data retention and notices" - no, we are not going to complicate ourselves and we are going to collect all the necessary data. The purpose is to scare; it does not aim to store anything outside of this purpose. Consent with a "De acuerdo" (I agree) at the start, before entering the menu, is enough; live, I will also clarify all the information on how it is used.





Theme:
1. Dimension by datapack. Entry through a portal opened with a small ritual. A parallel world, similar, but somewhat different?... Playing with time (the ghosts of you could go there?... I do not like the idea THAT much but we can play with copying behavior), with changes and replacements of things.
A world that erases itself? "File" corruption that advances, digital corruption of the world (replacement of objects, vanished chunks), game settings that keep changing in strange ways: the sensitivity changes, the language of a sign mutates, the brightness flickers, a shifted FOV.
Even if you have not invoked this dimension, they can TAKE you there for a moment, and in the real world replace you with a doppelganger. But that must not be the apogee of fear, only a detail that can scare (a player who went to this parallel world; and the other who stayed with the doppelganger) and bring them back. It can happen, for example, in the cave, or when you sleep.
Along with those "bugs" when you suddenly fall into this dimension, things can be added such as "it intercepts chat and voice and rewrites what you say ('let's go left' arrives as 'let's go to the well')", small details that are not the apogee, but are capable of scaring. "With proximity voice, you hear your friend calling you from the darkness while they are next to you. It is pure social paranoia and very good for streamers" - totally, it is the Monster trying to get you in its claws even though you are in the real world, and its dimension is its board where it is really going to have fun.

The pocket dimension is a rotten place, which started as a normal copy of the main world (dimension), but the more victims that appeared there, the more time went by, the more it degrades, becomes corrupted, rots. Certain blocks, as a surface, rot where you are standing and you sink slowly, which keeps you from running and escaping.

> Manhunt: a monster whose objective is to catch the living into its POCKET DIMENSION (let it be called that) and consume their soul little by little, through fear, and that is why its objective of existence is fear. It is a spider, but one that weaves the best ways to scare in order to consume its victim.

Consumed players become the Reclaimer's puppet. A consumed player becomes an invisible ghost (or sometimes not, perhaps there may even be moments when their voice can reach the living), who can interact in a limited way with the world, and scare other players, helping the Reclaimer consume their souls. The puppets can see with a filter and slightly different graphics (visible threads (use Minecraft's cobwebs and string). Following a thread leads to the bait, and cutting them is a mechanic for the PUPPETS [because the Reclaimer "forgot" to remove their control over their bodies, and at the same time it can generate ARTIFACTS - distorted blind copies (which, technically speaking, keep training on the behavioral patterns of the consumed player)].). It runs more and vibrates more: the spider feels vibrations through the threads, it walks crouched and barely senses you. It is not a Warden because the thread transmits voices at a distance: the "friend calling you from the darkness" arrives through a taut thread that you can see - the Reclaimer does not do this with much awareness, it simply arrived at this pattern because apparently this has generated more fear.
The Reclaimer has a body: when a player sees it physically, it is the apogee of fear, and that is why its appearance was generated to be as scary as possible, after meeting which the player is DISCARDED (and is shattered into ARTIFACTS and the unsupervised PUPPET [because by natural training, the interactivity was left to the PUPPETs since they helped generate fear and create valuable data on their own]).




2. Reality is cloth. It sewed its world onto the edges of yours, the portal is a seam, and the corruption is the fraying. The double is a "sewn copy". It has a meta wink: your mod runs on Fabric, and the loader is the very weave of the story.

package org.unallocated.story;

import com.google.gson.JsonObject;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.unallocated.audio.AudioSounds;
import org.unallocated.content.UnallocatedContent;

/**
 * The Shell (GAMEPLAY 6-8). A Puppet lives inside the Reclaimer's process, and the degraded model no longer tells what
 * a subject says from an instruction, so whatever a Puppet types reaches its command interpreter. Each recapture pulls
 * the Puppet deeper (gen 1: read, gen 2: inspect, gen 3: stop processes). Typing is noise: past a limit it comes down
 * from the sky. At gen 3, {@code kill 7331} is the boss fight: it lands, hunts, lashes threads and unmakes its world
 * while you type; four hits stop it, and the watchdog window decides the ending.
 */
public final class KernelShell {
    private KernelShell() {}
    static final int PID = 7331, MAX_ATTENTION = 8, CONNECTIONS = 3, HITS_TO_STOP = 4, WINDOW_TICKS = 90 * 20;
    private static final String GIANT = "unallocated.giant";
    enum Phase { IDLE, DESCENT, FIGHT, WINDOW, COMPLETE, POISON, CONSUMED }
    private static Phase phase = Phase.IDLE;
    private static int ticks, phaseTicks, hits, lost, lastHitTick = -9999, lastStruckTick = -9999, perchTicks, nextLash, nextScream;
    private static double giantX, giantY, giantZ;
    private static boolean halfWarned;
    private static final List<Lash> lashes = new ArrayList<>();
    private record Lash(Vec3 at, int fallTick) {}
    private static final Random RANDOM = new Random();
    private static final Vec3 SAFE = new Vec3(53.5, 64, 12.5);

    // ---------------------------------------------------------------- state helpers
    private static int depth(StoryRuntime r) { return Math.min(3, Math.max(1, r.state().counters().getOrDefault("story.deaths", 0))); }
    private static int attention(StoryRuntime r) { return r.state().counters().getOrDefault("story.counter.attention", 0); }
    private static String chapter(StoryRuntime r) { JsonObject c = r.currentChapter(); return c == null ? "" : StoryData.string(c, "id", ""); }
    private static boolean flag(StoryRuntime r, String f) { return r.state().flags().contains("story.flag." + f); }
    private static void setFlag(StoryRuntime r, String f) { r.state().addFlag("story.flag." + f); }

    /** True while what the participant types belongs to the Shell instead of the server chat. */
    public static boolean routes(StoryRuntime r, ServerPlayer p) {
        String c = chapter(r);
        return c.equals("kernel") || c.equals("puppet") && r.puppet();
    }

    static void reset() {
        phase = Phase.IDLE; ticks = phaseTicks = hits = lost = perchTicks = 0; lashes.clear(); halfWarned = false;
        lastHitTick = lastStruckTick = -9999;
    }

    // ---------------------------------------------------------------- output
    static void say(StoryRuntime r, ServerPlayer p, String es, String en, String color) {
        JsonObject text = new JsonObject(); text.addProperty("es", es); text.addProperty("en", en); text.addProperty("color", color);
        JsonObject data = new JsonObject(); data.add("text", text); r.effect(p, "chat", data, 0);
    }
    static void out(StoryRuntime r, ServerPlayer p, String both) { say(r, p, both, both, "#9a9a9a"); }
    static void bar(StoryRuntime r, ServerPlayer p, String es, String en) {
        JsonObject text = new JsonObject(); text.addProperty("es", es); text.addProperty("en", en);
        JsonObject data = new JsonObject(); data.add("text", text); r.effect(p, "actionbar", data, 0);
    }
    static void run(StoryRuntime r, ServerPlayer p, String command) {
        r.server().getCommands().performPrefixedCommand(p.createCommandSourceStack().withPermission(LevelBasedPermissionSet.GAMEMASTER).withSuppressedOutput(), command);
    }
    private static void blackout(StoryRuntime r, ServerPlayer p, double in, double hold, double outS, String color) {
        JsonObject data = new JsonObject(); data.addProperty("fade_in", in); data.addProperty("hold", hold); data.addProperty("fade_out", outS);
        if (color != null) data.addProperty("color", color);
        r.effect(p, "blackout", data, 0);
    }
    private static void iface(StoryRuntime r, ServerPlayer p, String effect, double seconds, double strength) {
        JsonObject data = new JsonObject(); data.addProperty("effect", effect); data.addProperty("seconds", seconds); data.addProperty("strength", strength);
        r.effect(p, "interface", data, Math.max(1, (int) (seconds * 20)));
    }
    /** Closes the console in the participant's face and shakes the view (client: story.scream). */
    private static void scream(StoryRuntime r, ServerPlayer p) {
        AudioSounds.play(p, "screamer", p.position());
        r.effect(p, "scream", new JsonObject(), 20);
        iface(r, p, "FOV", 3, 1);
    }

    // ---------------------------------------------------------------- the shell
    /** Returns true when the line was consumed by the Shell (hidden from chat). */
    public static boolean chat(StoryRuntime r, ServerPlayer p, String raw) {
        String line = raw.strip().replaceAll("\\s+", " ");
        if (line.isEmpty()) return true;
        String lower = line.toLowerCase(Locale.ROOT);
        // The two final commands belong to the chapter beats (kernel.complete / kernel.poison) once the window is open.
        if (phase == Phase.WINDOW && (lower.matches("set success_threshold 0?[.,]82") || lower.matches(".*reclaimer.*>>.*dataset.*"))) return false;
        if (phase == Phase.COMPLETE || phase == Phase.POISON || phase == Phase.CONSUMED) return true;
        say(r, p, "> " + line, "> " + line, "#d8d4cb");
        int gen = depth(r);
        String[] w = lower.split(" ");
        String cmd = w[0];
        boolean known = true;
        switch (cmd) {
            case "help", "?" -> help(r, p, gen);
            case "ls", "dir" -> ls(r, p, w.length > 1 ? w[1] : "");
            case "cat", "less", "more", "type", "open", "read" -> {
                if (w.length < 2) out(r, p, "cat: missing file operand");
                else cat(r, p, w[w.length - 1]);
            }
            case "ps", "top", "htop" -> { if (gen < 2) denied(r, p, cmd, gen, 2); else ps(r, p); }
            case "params" -> { if (gen < 2) denied(r, p, cmd, gen, 2); else params(r, p); }
            case "kill", "pkill", "killall" -> kill(r, p, gen, lower);
            case "set" -> {
                if (phase != Phase.WINDOW) say(r, p, "set: el proceso 7331 tiene el archivo abierto. deténlo primero.", "set: process 7331 holds the file open. stop it first.", "#9a9a9a");
                else if (w.length >= 3 && w[1].equals("success_threshold")) say(r, p, "set: " + w[2] + " fuera de especificación. el contrato fija el valor.", "set: " + w[2] + " is out of spec. the contract fixes the value.", "#aa0000");
                else say(r, p, "uso: set success_threshold <valor>", "usage: set success_threshold <value>", "#9a9a9a");
            }
            case "echo" -> {
                if (phase != Phase.WINDOW) say(r, p, "echo: dataset bloqueado mientras 7331 corre.", "echo: dataset locked while 7331 runs.", "#9a9a9a");
                else if (lower.contains(">>")) say(r, p, "dataset: añadido. ese sujeto no está aquí. nada cambia.", "dataset: appended. that subject is not here. nothing changes.", "#9a9a9a");
                else out(r, p, line.substring(4).strip());
            }
            case "whoami" -> say(r, p, "subject 4114 (" + "{player}" + ") :: artifact gen " + gen, "subject 4114 ({player}) :: artifact gen " + gen, "#9a9a9a");
            case "exit", "logout", "quit" -> say(r, p, "la conexión sigue abierta.", "the connection is still open.", "#aa0000");
            case "sudo", "su" -> say(r, p, "{player} no está en el archivo sudoers. este incidente será reportado.", "{player} is not in the sudoers file. this incident will be reported.", "#aa0000");
            default -> { known = false; out(r, p, "sh: " + cmd + ": command not found"); }
        }
        int noise = known ? 1 : 1;
        if (lower.contains("reclaimer") && !cmd.equals("cat")) {
            noise += 3;
            say(r, p, "muestra inválida :: el sujeto conoce la designación", "invalid sample :: the subject knows the designation", "#aa0000");
        }
        noise(r, p, noise);
        return true;
    }

    private static void denied(StoryRuntime r, ServerPlayer p, String cmd, int gen, int need) {
        say(r, p, cmd + ": operación no permitida :: el sujeto 4114 aún no es parte del proceso (gen " + gen + "/" + need + ")",
            cmd + ": operation not permitted :: subject 4114 is not part of the process yet (gen " + gen + "/" + need + ")", "#aa0000");
    }

    private static void help(StoryRuntime r, ServerPlayer p, int gen) {
        out(r, p, "reclaim shell (gen " + gen + "/3)");
        out(r, p, "  help · ls [dir] · cat <file> · whoami");
        if (gen >= 2) out(r, p, "  ps · params");
        else out(r, p, "  ps · params            [locked until gen 2]");
        if (gen >= 3) out(r, p, "  kill <pid> · set <param> <value> · echo <text> >> dataset");
        else out(r, p, "  kill · set · echo      [locked until gen 3]");
    }

    private static final Map<String, List<String>> DIRS = Map.of(
        "", List.of("config/   dataset/   docs/   home/   logs/"),
        "config", List.of("reclaim.cfg"), "dataset", List.of("subjects.txt"), "docs", List.of("contract.txt"),
        "home", List.of("noor/"), "home/noor", List.of("notes.txt"), "logs", List.of("reclaim.log"));
    private static void ls(StoryRuntime r, ServerPlayer p, String dir) {
        String d = dir.replaceAll("^\\./", "").replaceAll("/+$", "").replace("~", "home");
        if (d.equals("/") || d.equals(".")) d = "";
        var list = DIRS.get(d);
        if (list == null) { out(r, p, "ls: cannot access '" + dir + "': No such file or directory"); return; }
        for (String line : list) say(r, p, line, line, "#55ffff");
    }

    private static void cat(StoryRuntime r, ServerPlayer p, String file) {
        String f = file.replaceAll("^\\./", "").replaceAll("^/", "");
        String name = f.contains("/") ? f.substring(f.lastIndexOf('/') + 1) : f;
        switch (name) {
            case "reclaim.cfg" -> {
                out(r, p, "# reclaim · fear-estimation playtest build g0");
                out(r, p, "pool = UNALLOCATED");
                out(r, p, "success_threshold = 1.37");
                out(r, p, "temperature = 2.4");
                say(r, p, "# no editar en caliente. detén el proceso (kill <pid>) y luego set <param> <valor>",
                    "# do not edit while running. stop the process (kill <pid>), then set <param> <value>", "#9a9a9a");
            }
            case "contract.txt" -> {
                say(r, p, "ESPECIFICACIÓN DE ACEPTACIÓN g0 · playtest de miedo con sujetos humanos", "ACCEPTANCE SPEC g0 · human fear playtest", "#e8e8e8");
                say(r, p, "§4.2 Cada sesión termina cuando la respuesta de miedo estimada alcanza success_threshold = 0.82 de la línea base del sujeto.",
                    "§4.2 Each session ends when the estimated fear response reaches success_threshold = 0.82 of the subject's baseline.", "#e8e8e8");
                say(r, p, "§4.3 Un valor mayor que 1.0 no puede alcanzarse. No usar.", "§4.3 A value above 1.0 can never be reached. Do not use.", "#e8e8e8");
                say(r, p, "§7 Los sujetos se toman del pool UNALLOCATED (jugadores sin asignar a ningún estudio).", "§7 Subjects are drawn from the UNALLOCATED pool (players not assigned to any study).", "#e8e8e8");
            }
            case "subjects.txt" -> {
                out(r, p, "0007 Zuri      trials 31   fear 0.94   retry");
                out(r, p, "0012 Noor      trials 58   fear 0.97   retry");
                out(r, p, "0019 Kai       trials 12   fear 0.88   retry");
                out(r, p, "4114 {player}   trials " + Math.max(1, r.state().counters().getOrDefault("story.deaths", 0)) + "    fear ...    training");
                say(r, p, "# cada nombre que se añade aquí es un sujeto que busca:  echo <nombre> >> dataset",
                    "# every name appended here is a subject it hunts:  echo <name> >> dataset", "#9a9a9a");
            }
            case "notes.txt" -> {
                say(r, p, "si estás leyendo esto, también estás muerto. lo siento.", "if you are reading this you are dead too. i'm sorry.", "#e8e8e8");
                say(r, p, "el número está mal. el contrato dice 0.82 y él busca 1.37, un miedo que no existe. por eso nunca termina con nadie.",
                    "the number is wrong. the contract says 0.82 and it looks for 1.37, a fear that doesn't exist. that's why it never finishes with anyone.", "#e8e8e8");
                say(r, p, "si consigues detenerlo con kill, ponle el número del contrato con set y quizá por fin pueda terminar.",
                    "if you manage to stop it with kill, give it the contract's number with set and maybe it can finally finish.", "#e8e8e8");
                say(r, p, "o escribe su propio nombre en su dataset. se pasaría la eternidad buscándose a sí mismo. no sé qué es peor.",
                    "or write its own name into its dataset. it would spend forever hunting itself. i don't know which is worse.", "#e8e8e8");
                say(r, p, "yo no llegué. me oyó escribir. — Noor", "i didn't make it. it heard me typing. — Noor", "#e8e8e8");
            }
            case "reclaim.log" -> {
                out(r, p, "trial 58  subject 0012  fear 0.97  < 1.37  retry");
                out(r, p, "trial 31  subject 0007  fear 0.94  < 1.37  retry");
                out(r, p, "trial 12  subject 0019  fear 0.88  < 1.37  retry");
                out(r, p, "watchdog :: guarding pid 7331 :: restart window 90 s");
            }
            default -> out(r, p, "cat: " + file + ": No such file or directory");
        }
    }

    private static void ps(StoryRuntime r, ServerPlayer p) {
        out(r, p, "  PID  CPU  CMD");
        out(r, p, "  1104   3%  java -jar server.jar nogui");
        out(r, p, "  7331  97%  reclaim --pool UNALLOCATED");
        out(r, p, "        └ thread main: Reclaimer");
        out(r, p, "  7332   0%  watchdog --guard 7331");
    }

    private static void params(StoryRuntime r, ServerPlayer p) {
        out(r, p, "success_threshold = 1.37");
        out(r, p, "temperature = 2.4");
    }

    private static void kill(StoryRuntime r, ServerPlayer p, int gen, String lower) {
        if (gen < 3) { denied(r, p, "kill", gen, 3); return; }
        if (!lower.matches(".*\\b" + PID + "\\b.*")) {
            if (lower.matches(".*\\b(1104|7332)\\b.*")) say(r, p, "kill: operación no permitida", "kill: operation not permitted", "#aa0000");
            else out(r, p, "kill: no such process");
            return;
        }
        if (phase == Phase.WINDOW) { out(r, p, "[7331] reclaim :: already stopped"); return; }
        if (phase != Phase.FIGHT) { startFight(r, p); return; }
        if (ticks - lastHitTick < 120) {
            say(r, p, "kill: 7331 está reconstruyendo su hilo principal. espera.", "kill: 7331 is rebuilding its main thread. wait.", "#aa0000");
            return;
        }
        hit(r, p);
    }

    // ---------------------------------------------------------------- noise and the descent
    public static void noise(StoryRuntime r, ServerPlayer p, int amount) {
        if (phase != Phase.IDLE || !chapter(r).equals("puppet") || !r.puppet()) return;
        int a = Math.min(MAX_ATTENTION, attention(r) + amount);
        r.state().setCounter("story.counter.attention", a);
        run(r, p, "bossbar set unallocated:attention value " + a);
        if (a >= MAX_ATTENTION / 2 && !halfWarned) {
            halfWarned = true;
            say(r, p, "who is typing?", "who is typing?", "#aa0000");
            run(r, p, "effect give @s minecraft:darkness 6 0 true");
        }
        if (a >= MAX_ATTENTION) {
            if (depth(r) >= 3) startFight(r, p);
            else descend(r, p);
        }
    }

    private static void descend(StoryRuntime r, ServerPlayer p) {
        phase = Phase.DESCENT; phaseTicks = 0;
        giantX = p.getX(); giantY = p.getY() + 34; giantZ = p.getZ();
        spawnGiant(r, p, 5);
        run(r, p, "effect give @s minecraft:darkness 8 0 true");
        AudioSounds.play(p, "threads", p.position());
    }

    private static void spawnGiant(StoryRuntime r, ServerPlayer p, double scale) {
        run(r, p, "kill @e[tag=" + GIANT + "]");
        run(r, p, String.format(Locale.ROOT, "summon unallocated:reclaimer %.2f %.2f %.2f {NoAI:1b,NoGravity:1b,Invulnerable:1b,PersistenceRequired:1b,Tags:[\"%s\"],attributes:[{id:\"minecraft:scale\",base:%.1fd}]}",
            giantX, giantY, giantZ, GIANT, scale));
    }
    private static void moveGiant(StoryRuntime r, ServerPlayer p) {
        run(r, p, String.format(Locale.ROOT, "tp @e[tag=%s,limit=1] %.2f %.2f %.2f facing entity @s eyes", GIANT, giantX, giantY, giantZ));
    }
    static void removeGiant(StoryRuntime r, ServerPlayer p) { run(r, p, "kill @e[tag=" + GIANT + "]"); }

    // ---------------------------------------------------------------- the fight
    private static void startFight(StoryRuntime r, ServerPlayer p) {
        run(r, p, "bossbar remove unallocated:attention");
        phase = Phase.FIGHT; phaseTicks = 0; hits = 0; lost = 0; perchTicks = 0; nextLash = 60; nextScream = 400; lashes.clear();
        giantX = p.getX() + 14; giantZ = p.getZ(); giantY = p.getY() + 30;
        spawnGiant(r, p, 4.5);
        setFlag(r, "fight");
        say(r, p, "subject 4114 :: reclasificado: intrusión. trials: 0. próxima captura = definitiva.", "subject 4114 :: reclassified: intrusion. trials: 0. next capture = final.", "#aa0000");
        run(r, p, "bossbar add unallocated:reclaim \"reclaim [7331]\"");
        run(r, p, "bossbar set unallocated:reclaim max " + HITS_TO_STOP);
        run(r, p, "bossbar set unallocated:reclaim value " + HITS_TO_STOP);
        run(r, p, "bossbar set unallocated:reclaim color red");
        run(r, p, "bossbar set unallocated:reclaim style notched_6");
        run(r, p, "bossbar set unallocated:reclaim players @s");
        AudioSounds.play(p, "screamer", p.position());
        if (!chapter(r).equals("kernel")) r.next(p); // the Kernel chapter: no way back, death = CONSUMED
        bar(r, p, "Viene por ti. Sigue escribiendo kill 7331. Sube a lo alto para escribir.", "It is coming for you. Keep typing kill 7331. Climb somewhere high to type.");
    }

    private static void hit(StoryRuntime r, ServerPlayer p) {
        hits++; lastHitTick = ticks;
        run(r, p, "bossbar set unallocated:reclaim value " + (HITS_TO_STOP - hits));
        out(r, p, "[7331] SIGSTOP :: main loop " + (HITS_TO_STOP - hits) + "/" + HITS_TO_STOP);
        scream(r, p);
        if (hits >= HITS_TO_STOP) { openWindow(r, p); return; }
        say(r, p, "deja de escribir.", "stop typing.", "#aa0000");
    }

    private static void openWindow(StoryRuntime r, ServerPlayer p) {
        phase = Phase.WINDOW; phaseTicks = 0; lashes.clear();
        setFlag(r, "suspended");
        run(r, p, "bossbar remove unallocated:reclaim");
        run(r, p, "bossbar add unallocated:watchdog \"watchdog :: restart 90 s\"");
        run(r, p, "bossbar set unallocated:watchdog max " + WINDOW_TICKS / 20);
        run(r, p, "bossbar set unallocated:watchdog value " + WINDOW_TICKS / 20);
        run(r, p, "bossbar set unallocated:watchdog color yellow");
        run(r, p, "bossbar set unallocated:watchdog players @s");
        run(r, p, "stopsound @s");
        AudioSounds.stop(p);
        r.fire("kernel.stopped");
        windowNotes(r, p);
    }

    static void windowNotes(StoryRuntime r, ServerPlayer p) {
        say(r, p, "[7331] reclaim :: detenido. el watchdog lo reinicia en 90 s.", "[7331] reclaim :: stopped. the watchdog restarts it in 90 s.", "#ffff55");
        say(r, p, "Las notas de Noor dejaban dos salidas. Escribe una antes de que la barra llegue a cero.", "Noor's notes left two ways out. Type one before the bar reaches zero.", "#e8e8e8");
        say(r, p, "  set success_threshold <el valor del contrato>  →  podrá terminar, por fin", "  set success_threshold <the contract's value>  →  it can finally finish", "#55ff55");
        say(r, p, "  echo <su nombre> >> dataset  →  se buscará a sí mismo para siempre", "  echo <its name> >> dataset  →  it will hunt itself forever", "#ff5555");
        say(r, p, "  (¿no recuerdas el valor? cat docs/contract.txt · ¿el nombre? ps)", "  (forgot the value? cat docs/contract.txt · the name? ps)", "#9a9a9a");
        say(r, p, "Si llega a cero, se despierta y te consume.", "If it reaches zero, it wakes up and consumes you.", "#aa0000");
    }

    /** Called by the kernel.complete / kernel.poison beats through "unallocated shell <mode>". */
    public static void ending(StoryRuntime r, ServerPlayer p, String mode) {
        run(r, p, "bossbar remove unallocated:watchdog");
        lashes.clear();
        switch (mode) {
            case "complete" -> { phase = Phase.COMPLETE; phaseTicks = 0; }
            case "poison" -> { phase = Phase.POISON; phaseTicks = 0; }
            case "white_room" -> whiteRoom(r, p);
            case "restore" -> restore(r, p);
            case "remove" -> removeGiant(r, p);
            default -> { }
        }
    }

    private static void whiteRoom(StoryRuntime r, ServerPlayer p) {
        String in = "execute in unallocated:pocket run ";
        run(r, p, in + "fill -9 199 -9 9 214 9 minecraft:white_concrete hollow");
        run(r, p, in + "fill -8 200 -8 8 213 8 minecraft:air");
        run(r, p, in + "fill -8 213 -8 8 213 8 minecraft:sea_lantern");
        run(r, p, "execute in unallocated:pocket run tp @s 0.5 200 7.5 180 0");
        giantX = 0.5; giantY = 200; giantZ = 0.5;
        spawnGiant(r, p, 2.2);
        run(r, p, "effect clear @s minecraft:darkness");
    }

    private static void restore(StoryRuntime r, ServerPlayer p) {
        removeGiant(r, p);
        run(r, p, "execute in minecraft:overworld run tp @s 0.5 64 8.5 0 0");
        run(r, p, "execute in minecraft:overworld run time set 23200");
        run(r, p, "weather clear");
        run(r, p, "effect clear @s");
    }

    private static void consumed(StoryRuntime r, ServerPlayer p, boolean timeout) {
        phase = Phase.CONSUMED; phaseTicks = 0; lashes.clear();
        run(r, p, "bossbar remove unallocated:watchdog");
        run(r, p, "bossbar remove unallocated:reclaim");
        if (timeout) say(r, p, "watchdog :: restart", "watchdog :: restart", "#aa0000");
        giantX = p.getX(); giantY = p.getY() + 10; giantZ = p.getZ();
        spawnGiant(r, p, 4.5);
    }

    // ---------------------------------------------------------------- tick
    public static void tick(StoryRuntime r, ServerPlayer p) {
        ticks++; phaseTicks++;
        String c = chapter(r);
        if (!c.equals("puppet") && !c.equals("kernel")) { if (phase != Phase.IDLE) reset(); return; }
        if (c.equals("kernel") && phase == Phase.IDLE && !flag(r, "suspended")) {
            if (Boolean.getBoolean("unallocated.story.autoplay")) { phase = Phase.WINDOW; setFlag(r, "suspended"); return; }
            startFight(r, p); return;
        }
        ServerLevel level = (ServerLevel) p.level();
        switch (phase) {
            case IDLE -> {
                if (c.equals("puppet") && r.puppet() && ticks % 20 == 0) {
                    run(r, p, "bossbar add unallocated:attention {translate:\"shell.unallocated.attention\",fallback:\"it is listening\"}");
                    run(r, p, "bossbar set unallocated:attention max " + MAX_ATTENTION);
                    run(r, p, "bossbar set unallocated:attention color red");
                    run(r, p, "bossbar set unallocated:attention value " + attention(r));
                    run(r, p, "bossbar set unallocated:attention players @s");
                } else if (ticks % 20 == 0) run(r, p, "bossbar remove unallocated:attention");
            }
            case DESCENT -> {
                giantX = p.getX(); giantZ = p.getZ();
                giantY = Math.max(p.getY() + 1.5, giantY - 0.55);
                moveGiant(r, p);
                if (phaseTicks % 10 == 0) level.playSound(null, p.blockPosition().above(8), UnallocatedContent.RECLAIMER_ROAR, SoundSource.HOSTILE, 2F, 0.5F);
                if (giantY <= p.getY() + 1.6) {
                    AudioSounds.play(p, "screamer", p.position());
                    blackout(r, p, 0.05, 1.2, 0.6, null);
                    r.state().setCounter("story.counter.attention", 0); halfWarned = false;
                    run(r, p, "bossbar remove unallocated:attention");
                    r.state().setCounter("story.counter.trial", r.state().counters().getOrDefault("story.counter.trial", 0) + 1);
                    run(r, p, "kill @s");
                    removeGiant(r, p);
                    phase = Phase.IDLE;
                }
            }
            case FIGHT -> fightTick(r, p, level);
            case WINDOW -> {
                int left = Math.max(0, (WINDOW_TICKS - phaseTicks) / 20);
                if (phaseTicks % 20 == 0) {
                    run(r, p, "bossbar set unallocated:watchdog value " + left);
                    run(r, p, "bossbar set unallocated:watchdog name \"watchdog :: restart " + left + " s\"");
                    if (left <= 30) run(r, p, "bossbar set unallocated:watchdog color red");
                }
                // Frozen, but the watchdog tugs at it: it lags toward you, a little lower every ten seconds.
                if (phaseTicks % 200 == 0) {
                    giantY = Math.max(p.getY() + 3, giantY - 2.5);
                    moveGiant(r, p);
                    level.playSound(null, BlockPos.containing(giantX, giantY, giantZ), UnallocatedContent.RECLAIMER_CLICK, SoundSource.HOSTILE, 3F, 0.4F);
                }
                if (phaseTicks % 600 == 300) windowNotes(r, p);
                if (phaseTicks >= WINDOW_TICKS) consumed(r, p, true);
            }
            case COMPLETE -> {
                // It comes down slowly, lucid, and comes apart into threads.
                if (phaseTicks < 300) { giantY = Math.max(p.getY(), giantY - 0.05); if (phaseTicks % 4 == 0) moveGiant(r, p); }
                if (phaseTicks % 6 == 0) level.sendParticles(new DustParticleOptions(0x8A1020, 2.0F), giantX, giantY + 4, giantZ, 30, 2.5, 4, 2.5, 0.02);
            }
            case POISON -> {
                // In the white room, broken: it jerks, searches, never finds.
                if (phaseTicks % 3 == 0) run(r, p, String.format(Locale.ROOT, "tp @e[tag=%s,limit=1] %.2f %.2f %.2f %d %d", GIANT,
                    giantX + RANDOM.nextGaussian() * 0.15, giantY, giantZ + RANDOM.nextGaussian() * 0.15, RANDOM.nextInt(360) - 180, RANDOM.nextInt(120) - 60));
                if (phaseTicks % 9 == 0) level.sendParticles(new DustParticleOptions(0x8A1020, 1.5F), giantX, giantY + 2, giantZ, 20, 1.5, 2, 1.5, 0.05);
            }
            case CONSUMED -> {
                giantX = p.getX(); giantZ = p.getZ(); giantY = Math.max(p.getY() + 1, giantY - 0.6); moveGiant(r, p);
                if (phaseTicks == 18) { AudioSounds.play(p, "screamer", p.position()); blackout(r, p, 0.05, 3, 0.5, "#300000"); }
                if (phaseTicks == 40) { removeGiant(r, p); r.end(p, "CONSUMED"); reset(); }
            }
        }
    }

    private static void fightTick(StoryRuntime r, ServerPlayer p, ServerLevel level) {
        // Falling into the void is not the end: it spits you back into its world.
        if (p.getY() < 52) {
            blackout(r, p, 0.05, 0.6, 0.6, null);
            p.teleportTo(level, SAFE.x, SAFE.y, SAFE.z, Set.of(), p.getYRot(), p.getXRot(), true);
            p.fallDistance = 0;
            say(r, p, "te escupe de vuelta. todavía no te deja ir.", "it spits you back. it is not letting you go yet.", "#aa0000");
        }
        // It hunts on the ground; it cannot climb.
        double dx = p.getX() - giantX, dz = p.getZ() - giantZ, dist = Math.sqrt(dx * dx + dz * dz);
        double ground = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) Math.floor(giantX), (int) Math.floor(giantZ));
        if (phaseTicks < 60) giantY = Math.max(ground, giantY - 0.5); // landing
        else giantY = ground > 40 ? ground : giantY;
        double speed = 0.19 + 0.025 * hits;
        if (dist > 2.2 && phaseTicks >= 60) { giantX += dx / dist * Math.min(speed, dist); giantZ += dz / dist * Math.min(speed, dist); }
        moveGiant(r, p);
        double above = p.getY() - giantY;
        boolean perched = above >= 3.5;
        if (dist < 3.2 && !perched && ticks - lastStruckTick > 60) {
            Vec3 push = new Vec3(dx, 0, dz).normalize().scale(1.6);
            p.push(push.x, 0.7, push.z); p.connection.send(new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(p));
            struck(r, p, level);
            if (phase != Phase.FIGHT) return;
        }
        // On a perch it waits underneath, then shakes it down.
        if (perched && dist < 7) {
            perchTicks++;
            if (perchTicks == 200) bar(r, p, "Te espera abajo. Va a tirar lo que te sostiene.", "It waits below. It is going to bring down what holds you.");
            if (perchTicks >= 400) {
                perchTicks = 0;
                BlockPos under = p.blockPosition().below();
                for (int x = -2; x <= 2; x++) for (int z = -2; z <= 2; z++) for (int y = 0; y >= -4; y--) collapse(level, under.offset(x, y, z), y == 0);
                level.playSound(null, under, UnallocatedContent.RECLAIMER_ROAR, SoundSource.HOSTILE, 3F, 0.6F);
            }
        } else perchTicks = Math.max(0, perchTicks - 2);
        // Threads lash down from the sky where you stand: 2.5 s of warning.
        if (--nextLash <= 0) {
            lashes.add(new Lash(new Vec3(p.getX(), p.getY(), p.getZ()), phaseTicks + 50));
            level.playSound(null, p.blockPosition(), UnallocatedContent.THREAD_VIBRATE, SoundSource.HOSTILE, 2F, 0.6F);
            nextLash = Math.max(50, (perched ? 70 : 130) - hits * 15);
        }
        for (Iterator<Lash> it = lashes.iterator(); it.hasNext();) {
            Lash l = it.next();
            if (phaseTicks % 2 == 0) {
                level.sendParticles(new DustParticleOptions(0xFF1020, 1.6F), l.at.x, l.at.y + 0.1, l.at.z, 14, 0.7, 0.02, 0.7, 0);
                for (int y = 1; y < 24; y += 3) level.sendParticles(new DustParticleOptions(0x8A1020, 1.0F), l.at.x, l.at.y + y, l.at.z, 1, 0.05, 0.3, 0.05, 0);
            }
            if (phaseTicks >= l.fallTick) {
                it.remove();
                level.sendParticles(ParticleTypes.EXPLOSION, l.at.x, l.at.y + 0.5, l.at.z, 3, 0.5, 0.5, 0.5, 0);
                level.playSound(null, BlockPos.containing(l.at), UnallocatedContent.THREAD_SNAP, SoundSource.HOSTILE, 2.5F, 0.5F);
                collapse(level, BlockPos.containing(l.at).below(), true);
                if (p.position().distanceTo(l.at) < 1.8 && ticks - lastStruckTick > 30) { struck(r, p, level); if (phase != Phase.FIGHT) return; }
            }
        }
        // Its world comes apart around you, revealing the void underneath.
        if (phaseTicks % 3 == 0) {
            double angle = RANDOM.nextDouble() * Math.PI * 2, radius = 7 + RANDOM.nextDouble() * 22;
            int x = (int) Math.floor(p.getX() + Math.cos(angle) * radius), z = (int) Math.floor(p.getZ() + Math.sin(angle) * radius);
            if (Math.abs(x - SAFE.x) > 5 || Math.abs(z - SAFE.z) > 5) {
                int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
                for (int y = top; y >= 55; y--) collapse(level, new BlockPos(x, y, z), y == top);
            }
        }
        if (--nextScream <= 0) { scream(r, p); nextScream = 360 + RANDOM.nextInt(200); }
        if (phaseTicks % 400 == 200 && hits == 0) bar(r, p, "kill 7331 · cuatro veces · sube a lo alto para escribir", "kill 7331 · four times · climb high to type");
    }

    private static void collapse(ServerLevel level, BlockPos pos, boolean visible) {
        if (Math.abs(pos.getX() - SAFE.x) <= 5 && Math.abs(pos.getZ() - SAFE.z) <= 5) return;
        BlockState state = level.getBlockState(pos);
        if (state.isAir() || state.is(Blocks.BEDROCK)) return;
        if (visible) FallingBlockEntity.fall(level, pos, state);
        else level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
    }

    private static void struck(StoryRuntime r, ServerPlayer p, ServerLevel level) {
        lastStruckTick = ticks; lost++;
        blackout(r, p, 0.02, 0.25, 0.5, "#550000");
        level.playSound(null, p.blockPosition(), UnallocatedContent.RECLAIMER_ROAR, SoundSource.HOSTILE, 2F, 1.2F);
        int left = CONNECTIONS - lost;
        say(r, p, "conexión " + left + "/" + CONNECTIONS, "connection " + left + "/" + CONNECTIONS, "#aa0000");
        if (left <= 0) consumed(r, p, false);
    }
}

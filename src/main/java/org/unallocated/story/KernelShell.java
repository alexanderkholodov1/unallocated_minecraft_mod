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
 * the Puppet deeper (gen 1: read files, gen 2: params and logs, gen 3: processes). Typing is noise: past a limit it
 * comes down from the sky and retrains you. At gen 3 the first command brings it down for good: the boss fight. While
 * it hunts you, your mission is to find its process number; one {@code kill <pid>} stops everything, and the watchdog
 * window decides the ending.
 */
public final class KernelShell {
    private KernelShell() {}
    static final int PID = 7331, MAX_ATTENTION = 5, CONNECTIONS = 3, WINDOW_TICKS = 120 * 20;
    private static final String GIANT = "unallocated.giant";
    enum Phase { IDLE, DESCENT, FIGHT, WINDOW, COMPLETE, POISON, CONSUMED }
    private static Phase phase = Phase.IDLE;
    private static int ticks, phaseTicks, lost, lastStruckTick = -9999, perchTicks, nextLash, nextScream, screamWarn = -1, stagger;
    private static double giantX, giantY, giantZ;
    private static boolean halfWarned, gen3Warned, typedPs, typedHelp, awake;
    private static int pendingDescent = -1, frozenTicks, sinceFreeze;
    private static final Set<String> SHELL_WORDS = Set.of("help", "?", "ls", "dir", "cat", "less", "more", "type", "open", "read", "ps", "top", "htop",
        "params", "kill", "pkill", "killall", "set", "echo", "whoami", "sudo", "su", "cd", "pwd", "exit");
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
        return c.equals("kernel") || c.equals("puppet") || c.equals("heap");
    }

    static void reset() {
        phase = Phase.IDLE; ticks = phaseTicks = lost = perchTicks = stagger = frozenTicks = sinceFreeze = 0; awake = false; pendingDescent = -1; lashes.clear(); halfWarned = gen3Warned = typedPs = typedHelp = false;
        lastStruckTick = -9999; screamWarn = -1;
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
    /** Closes the console in the participant's face and twists the view (client: story.scream). */
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
        if (!chapter(r).equals("kernel") && !r.puppet()) {
            if (!SHELL_WORDS.contains(lower.split(" ")[0])) return false; // ordinary chat while alive
            say(r, p, "> " + line, "> " + line, "#d8d4cb");
            say(r, p, "sh: permiso denegado: esta consola solo le contesta a los sujetos muertos. sigues vivo.", "sh: permission denied: this console only answers dead subjects. you are still alive.", "#aa0000");
            return true;
        }
        // The two final commands belong to the chapter beats (kernel.complete / kernel.poison) once the window is open.
        if (phase == Phase.WINDOW && !awake && (lower.matches("set success_threshold 0?[.,]82") || lower.matches(".*reclaimer.*>>.*dataset.*"))) return false;
        if (phase == Phase.COMPLETE || phase == Phase.POISON || phase == Phase.CONSUMED || phase == Phase.DESCENT) return true;
        say(r, p, "> " + line, "> " + line, "#d8d4cb");
        int gen = depth(r);
        String[] w = lower.split(" ");
        String cmd = w[0];
        switch (cmd) {
            case "help", "?" -> { typedHelp = true; help(r, p, gen); }
            case "ls", "dir" -> ls(r, p, w.length > 1 ? w[1] : "");
            case "cat", "less", "more", "type", "open", "read" -> {
                if (w.length < 2) out(r, p, "cat: missing file operand");
                else cat(r, p, w[w.length - 1], gen);
            }
            case "params" -> { if (gen < 2) denied(r, p, cmd, gen, 2); else params(r, p); }
            case "ps", "top", "htop" -> { if (gen < 3) denied(r, p, cmd, gen, 3); else { typedPs = true; ps(r, p); } }
            case "kill", "pkill", "killall" -> kill(r, p, gen, lower);
            case "set" -> {
                if (phase == Phase.WINDOW && awake) say(r, p, "set: el proceso está corriendo otra vez. detenlo primero: kill 7331", "set: the process is running again. stop it first: kill 7331", "#aa0000");
                else if (phase != Phase.WINDOW) say(r, p, "set: el proceso tiene el archivo abierto. detén el proceso primero.", "set: the process holds the file open. stop the process first.", "#9a9a9a");
                else if (w.length >= 3 && w[1].equals("success_threshold")) say(r, p, "set: " + w[2] + " fuera de especificación. el contrato fija el valor.", "set: " + w[2] + " is out of spec. the contract fixes the value.", "#aa0000");
                else say(r, p, "uso: set success_threshold <valor>", "usage: set success_threshold <value>", "#9a9a9a");
            }
            case "echo" -> {
                if (phase == Phase.WINDOW && awake) say(r, p, "echo: el proceso está corriendo otra vez. detenlo primero: kill 7331", "echo: the process is running again. stop it first: kill 7331", "#aa0000");
                else if (phase != Phase.WINDOW) say(r, p, "echo: dataset bloqueado mientras el proceso corre.", "echo: dataset locked while the process runs.", "#9a9a9a");
                else if (lower.contains(">>")) say(r, p, "dataset: añadido. ese sujeto no está aquí. nada cambia.", "dataset: appended. that subject is not here. nothing changes.", "#9a9a9a");
                else out(r, p, line.substring(4).strip());
            }
            case "whoami" -> say(r, p, "subject 4114 ({player}) :: artifact gen " + gen, "subject 4114 ({player}) :: artifact gen " + gen, "#9a9a9a");
            case "exit", "logout", "quit" -> say(r, p, "la conexión sigue abierta.", "the connection is still open.", "#aa0000");
            case "sudo", "su" -> say(r, p, "{player} no está en el archivo sudoers. este incidente será reportado.", "{player} is not in the sudoers file. this incident will be reported.", "#aa0000");
            default -> out(r, p, "sh: " + cmd + ": command not found");
        }
        if (phase == Phase.WINDOW) return true;
        ServerLevel lvl = (ServerLevel) p.level();
        float loud = 0.35F + 0.12F * attention(r) + (gen >= 3 ? 0.6F : 0F);
        lvl.playSound(null, p.blockPosition().above(2), UnallocatedContent.THREAD_VIBRATE, SoundSource.HOSTILE, loud, 0.6F + 0.08F * attention(r));
        lvl.playSound(null, p.blockPosition().above(14), UnallocatedContent.THREAD_VIBRATE, SoundSource.HOSTILE, loud * 0.7F, 1.4F);
        int noise = 1;
        if (lower.contains("reclaimer") && !cmd.equals("cat")) {
            noise += 3;
            say(r, p, "muestra inválida :: el sujeto conoce la designación", "invalid sample :: the subject knows the designation", "#aa0000");
        }
        // Third time inside: the first word you type is the one it was waiting for.
        if (phase == Phase.IDLE && gen >= 3 && chapter(r).equals("puppet")) { startFight(r, p); return true; }
        noise(r, p, noise);
        return true;
    }

    private static void denied(StoryRuntime r, ServerPlayer p, String cmd, int gen, int need) {
        say(r, p, cmd + ": operación no permitida :: el sujeto 4114 aún no llega tan hondo (gen " + gen + "/" + need + ")",
            cmd + ": operation not permitted :: subject 4114 is not that deep yet (gen " + gen + "/" + need + ")", "#aa0000");
    }

    private static void help(StoryRuntime r, ServerPlayer p, int gen) {
        out(r, p, "reclaim shell (gen " + gen + "/3) · empieza / start: cat README");
        out(r, p, "  help · ls [dir] · cat <file> · whoami");
        out(r, p, gen >= 2 ? "  params" : "  params                 [gen 2]");
        out(r, p, gen >= 3 ? "  ps · kill <pid>" : "  ps · kill <pid>        [gen 3]");
        out(r, p, "  set <param> <value> · echo <text> >> dataset   [process stopped]");
    }

    private static final Map<String, List<String>> DIRS = Map.of(
        "", List.of("README   config/   dataset/   docs/   home/   logs/"),
        "config", List.of("reclaim.cfg"), "dataset", List.of("subjects.txt"), "docs", List.of("contract.txt"),
        "home", List.of("noor/"), "home/noor", List.of("1.txt   2.txt   3.txt"), "logs", List.of("reclaim.log"));
    private static void ls(StoryRuntime r, ServerPlayer p, String dir) {
        String d = dir.replaceAll("^\\./", "").replaceAll("^/", "").replaceAll("/+$", "").replace("~", "home");
        if (d.equals(".")) d = "";
        var list = DIRS.get(d);
        if (list == null) { out(r, p, "ls: cannot access '" + dir + "': No such file or directory"); return; }
        for (String line : list) say(r, p, line, line, "#55ffff");
    }

    private static void cat(StoryRuntime r, ServerPlayer p, String file, int gen) {
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
                out(r, p, "4114 {player}   trials " + depth(r) + "    fear ...    training");
                say(r, p, "# cada nombre que se añade aquí es un sujeto que busca:  echo <nombre> >> dataset",
                    "# every name appended here is a subject it hunts:  echo <name> >> dataset", "#9a9a9a");
            }
            case "readme", "readme.txt" -> {
                say(r, p, "esta consola solo le contesta a los sujetos muertos. tú lo estás.", "this console only answers dead subjects. you are one.", "#e8e8e8");
                String notes = gen == 1 ? "cat home/noor/1.txt" : gen == 2 ? "cat home/noor/1.txt · cat home/noor/2.txt" : "cat home/noor/3.txt";
                say(r, p, "Noor dejó una nota por cada vez que la atrapó. Ahora puedes leer: " + notes, "Noor left a note for every time it caught her. You can read now: " + notes, "#e8e8e8");
                say(r, p, "cada vez que te atrapa de nuevo llegas más hondo y puedes leer más (gen " + gen + "/3).", "every time it catches you again you get deeper and can read more (gen " + gen + "/3).", "#e8e8e8");
            }
            case "1.txt" -> {
                if (gen == 1) scheduleDescent(r, p);
                say(r, p, "nota 1. lo que escribes aquí, lo oye. la barra de arriba es cuánto te está escuchando.", "note 1. what you type here, it hears. the bar at the top is how closely it is listening.", "#e8e8e8");
                say(r, p, "si haces ruido baja del cielo y te mata. no pasa nada: vuelves, pero más hondo. elige «Todavía no» para seguir aquí.", "if you make noise it comes down from the sky and kills you. that is fine: you come back, deeper. choose \"Not yet\" to stay here.", "#e8e8e8");
                say(r, p, "quieres llegar más hondo, así que haz ruido. mientras, lee config/reclaim.cfg y docs/contract.txt. algo no cuadra.", "you want to go deeper, so make noise. meanwhile read config/reclaim.cfg and docs/contract.txt. something doesn't add up.", "#e8e8e8");
            }
            case "2.txt" -> {
                if (gen < 2) { denied(r, p, "cat home/noor/2.txt", gen, 2); return; }
                scheduleDescent(r, p);
                say(r, p, "nota 2. 1.37 contra 0.82. busca un miedo que no existe, por eso nunca termina con nadie. mira logs/reclaim.log.", "note 2. 1.37 against 0.82. it looks for a fear that doesn't exist, so it never finishes with anyone. see logs/reclaim.log.", "#e8e8e8");
                say(r, p, "la próxima vez que te atrape verás sus procesos (ps). pero en cuanto escribas algo, bajará y no se irá. ten pensado qué escribir.", "next time it catches you, you will see its processes (ps). but the moment you type, it will come down and not leave. know what you will type.", "#e8e8e8");
            }
            case "3.txt", "notes.txt" -> {
                if (gen < 3) { denied(r, p, "cat home/noor/3.txt", gen, 3); return; }
                say(r, p, "si estás leyendo esto, también estás muerto. lo siento.", "if you are reading this you are dead too. i'm sorry.", "#e8e8e8");
                say(r, p, "el número está mal. el contrato dice 0.82 y él busca 1.37, un miedo que no existe. por eso nunca termina con nadie.",
                    "the number is wrong. the contract says 0.82 and it looks for 1.37, a fear that doesn't exist. that's why it never finishes with anyone.", "#e8e8e8");
                say(r, p, "a la tercera vez ya ves sus procesos. pero en cuanto escribes, baja. busca su número y detenlo con kill, rápido.",
                    "the third time you can see its processes. but the moment you type, it comes down. find its number and stop it with kill, fast.", "#e8e8e8");
                say(r, p, "detenido, ponle el número del contrato con set y quizá por fin pueda terminar.",
                    "once it is stopped, give it the contract's number with set and maybe it can finally finish.", "#e8e8e8");
                say(r, p, "o escribe su propio nombre en su dataset. se pasaría la eternidad buscándose a sí mismo. no sé qué es peor.",
                    "or write its own name into its dataset. it would spend forever hunting itself. i don't know which is worse.", "#e8e8e8");
                say(r, p, "yo no llegué. me oyó escribir. — Noor", "i didn't make it. it heard me typing. — Noor", "#e8e8e8");
            }
            case "reclaim.log" -> {
                if (gen < 2) { denied(r, p, "cat logs/reclaim.log", gen, 2); return; }
                out(r, p, "trial 58  subject 0012  fear 0.97  < 1.37  retry");
                out(r, p, "trial 31  subject 0007  fear 0.94  < 1.37  retry");
                out(r, p, "trial 12  subject 0019  fear 0.88  < 1.37  retry");
                out(r, p, "watchdog :: guarding reclaim :: restart window 90 s");
            }
            default -> out(r, p, "cat: " + file + ": No such file or directory");
        }
    }

    private static void ps(StoryRuntime r, ServerPlayer p) {
        out(r, p, "  PID   CPU  CMD");
        out(r, p, "     1   0%  init");
        out(r, p, "   412   0%  sshd: listening");
        out(r, p, "  1104   3%  java -jar server.jar nogui");
        out(r, p, "  1187   0%  backup --daily (failed 1594 times)");
        say(r, p, "  7331  97%  reclaim --pool UNALLOCATED   └ thread main: Reclaimer", "  7331  97%  reclaim --pool UNALLOCATED   └ thread main: Reclaimer", "#ff5555");
        out(r, p, "  7332   0%  watchdog --guard reclaim");
    }

    private static void params(StoryRuntime r, ServerPlayer p) {
        out(r, p, "success_threshold = 1.37");
        out(r, p, "temperature = 2.4");
    }

    private static void kill(StoryRuntime r, ServerPlayer p, int gen, String lower) {
        if (gen < 3) { denied(r, p, "kill", gen, 3); return; }
        if (phase == Phase.WINDOW) {
            if (!awake) { out(r, p, "[7331] reclaim :: already stopped"); return; }
            if (lower.matches(".*\\b" + PID + "\\b.*")) { refreeze(r, p); return; }
            out(r, p, "kill: no such process"); return;
        }
        if (phase == Phase.IDLE) startFight(r, p);
        if (lower.matches(".*\\b" + PID + "\\b.*")) { stop(r, p); return; }
        if (lower.matches(".*\\b(1|412|1104|1187|7332)\\b.*")) say(r, p, "kill: operación no permitida", "kill: operation not permitted", "#aa0000");
        else out(r, p, "kill: no such process");
        nextLash = Math.min(nextLash, 10); // it heard the wrong number
    }

    /** Reading the note of your own depth is loud enough: it comes for you a few seconds later. */
    private static void scheduleDescent(StoryRuntime r, ServerPlayer p) {
        if (pendingDescent < 0 && phase == Phase.IDLE && depth(r) < 3) {
            pendingDescent = ticks + 240;
            say(r, p, "te oyó leer.", "it heard you reading.", "#aa0000");
        }
    }

    // ---------------------------------------------------------------- noise and the descent
    public static void noise(StoryRuntime r, ServerPlayer p, int amount) {
        if (phase != Phase.IDLE || !chapter(r).equals("puppet") || !r.puppet()) return;
        if (depth(r) >= 3) return; // at gen 3 typing alone brings it down (see chat)
        int a = Math.min(MAX_ATTENTION, attention(r) + amount);
        r.state().setCounter("story.counter.attention", a);
        run(r, p, "bossbar set unallocated:attention value " + a);
        if (a >= MAX_ATTENTION / 2 && !halfWarned) {
            halfWarned = true;
            say(r, p, "who is typing?", "who is typing?", "#aa0000");
            run(r, p, "effect give @s minecraft:darkness 6 0 true");
            ((ServerLevel) p.level()).playSound(null, p.blockPosition(), UnallocatedContent.RECLAIMER_WHISPER, SoundSource.HOSTILE, 2F, 0.7F);
            ((ServerLevel) p.level()).playSound(null, p.blockPosition().above(20), UnallocatedContent.RECLAIMER_ROAR, SoundSource.HOSTILE, 1.5F, 0.4F);
        }
        if (a >= MAX_ATTENTION) descend(r, p);
    }

    private static void descend(StoryRuntime r, ServerPlayer p) {
        phase = Phase.DESCENT; phaseTicks = 0;
        giantX = p.getX(); giantY = p.getY() + 34; giantZ = p.getZ();
        spawnGiant(r, p, 4.5);
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
        phase = Phase.FIGHT; phaseTicks = 0; lost = 0; perchTicks = 0; stagger = 0; nextLash = 140; nextScream = 600; screamWarn = -1; lashes.clear();
        double angle = RANDOM.nextDouble() * Math.PI * 2;
        giantX = p.getX() + Math.cos(angle) * 18; giantZ = p.getZ() + Math.sin(angle) * 18; giantY = p.getY() + 36;
        spawnGiant(r, p, 4.5);
        setFlag(r, "fight");
        say(r, p, "who is typing?", "who is typing?", "#aa0000");
        say(r, p, "subject 4114 :: reclasificado: intrusión. trials: 0. próxima captura = definitiva.", "subject 4114 :: reclassified: intrusion. trials: 0. next capture = final.", "#aa0000");
        run(r, p, "bossbar add unallocated:reclaim \"RECLAIMER\"");
        run(r, p, "bossbar set unallocated:reclaim color red");
        run(r, p, "bossbar set unallocated:reclaim value 100");
        run(r, p, "bossbar set unallocated:reclaim players @s");
        run(r, p, "effect give @s minecraft:darkness 4 0 true");
        run(r, p, "weather thunder");
        AudioSounds.play(p, "ambient", p.position());
        AudioSounds.play(p, "threads", p.position());
        title(r, p, "RECLAIMER", "encuentra su número de proceso", "find its process number");
        if (!chapter(r).equals("kernel")) r.next(p); // the Kernel chapter: no way back, death = CONSUMED
        bar(r, p, "Viene por ti. Encuentra su número de proceso y detenlo.", "It is coming for you. Find its process number and stop it.");
    }

    /** One kill on the right number: everything stops. */
    private static void stop(StoryRuntime r, ServerPlayer p) {
        phase = Phase.WINDOW; phaseTicks = 0; lashes.clear(); awake = false; frozenTicks = 0; sinceFreeze = 0;
        setFlag(r, "suspended");
        run(r, p, "bossbar remove unallocated:reclaim");
        run(r, p, "bossbar add unallocated:watchdog \"watchdog :: restart 120 s\"");
        run(r, p, "bossbar set unallocated:watchdog max " + WINDOW_TICKS / 20);
        run(r, p, "bossbar set unallocated:watchdog value " + WINDOW_TICKS / 20);
        run(r, p, "bossbar set unallocated:watchdog color yellow");
        run(r, p, "bossbar set unallocated:watchdog players @s");
        run(r, p, "stopsound @s");
        run(r, p, "effect clear @s minecraft:darkness");
        AudioSounds.stop(p);
        ServerLevel level = (ServerLevel) p.level();
        level.playSound(null, BlockPos.containing(giantX, giantY, giantZ), UnallocatedContent.RECLAIMER_CLICK, SoundSource.HOSTILE, 3F, 0.3F);
        out(r, p, "[7331] reclaim :: SIGSTOP");
        r.fire("kernel.stopped");
        run(r, p, "weather clear");
        title(r, p, "SIGSTOP", "el watchdog lo reinicia en 120 s", "the watchdog restarts it in 120 s");
        windowNotes(r, p);
    }

    /** The watchdog restarts a thread: it hunts again until you stop it again. */
    private static void wake(StoryRuntime r, ServerPlayer p) {
        awake = true; lashes.clear(); nextLash = 60; stagger = 0;
        title(r, p, "watchdog", "reinicio parcial · kill 7331", "partial restart · kill 7331");
        out(r, p, "watchdog :: restarting thread main ... [7331] reclaim :: running");
        run(r, p, "bossbar set unallocated:watchdog color purple");
        AudioSounds.play(p, "threads", p.position());
        ((ServerLevel) p.level()).playSound(null, BlockPos.containing(giantX, giantY + 6, giantZ), UnallocatedContent.RECLAIMER_ROAR, SoundSource.HOSTILE, 4F, 0.6F);
    }

    private static void refreeze(StoryRuntime r, ServerPlayer p) {
        awake = false; sinceFreeze = 0; lashes.clear();
        out(r, p, "[7331] reclaim :: SIGSTOP");
        run(r, p, "bossbar set unallocated:watchdog color yellow");
        ((ServerLevel) p.level()).playSound(null, BlockPos.containing(giantX, giantY, giantZ), UnallocatedContent.RECLAIMER_CLICK, SoundSource.HOSTILE, 3F, 0.3F);
        title(r, p, "SIGSTOP", "rápido: set o echo", "quick: set or echo");
        windowNotes(r, p);
    }

    static void windowNotes(StoryRuntime r, ServerPlayer p) {
        say(r, p, "[7331] reclaim :: detenido. el watchdog lo despierta a ratos: tienes unos 15 s cada vez.", "[7331] reclaim :: stopped. the watchdog wakes it now and then: you have about 15 s each time.", "#ffff55");
        say(r, p, "Las notas de Noor dejaban dos salidas. Escribe una antes de que la barra llegue a cero.", "Noor's notes left two ways out. Type one before the bar reaches zero.", "#e8e8e8");
        say(r, p, "  set success_threshold <el valor del contrato>  →  podrá terminar, por fin, y soltarlos a todos", "  set success_threshold <the contract's value>  →  it can finally finish, and let everyone go", "#55ff55");
        say(r, p, "  echo <su nombre> >> dataset  →  se buscará a sí mismo para siempre, roto", "  echo <its name> >> dataset  →  it will hunt itself forever, broken", "#ff5555");
        say(r, p, "  (¿el valor? cat docs/contract.txt · ¿su nombre? ps)", "  (the value? cat docs/contract.txt · its name? ps)", "#9a9a9a");
        say(r, p, "Si la barra llega a cero, despierta y te consume.", "If the bar reaches zero, it wakes and consumes you.", "#aa0000");
    }

    static void title(StoryRuntime r, ServerPlayer p, String main, String es, String en) {
        JsonObject t = new JsonObject(); t.addProperty("es", main); t.addProperty("en", main);
        JsonObject sub = new JsonObject(); sub.addProperty("es", es); sub.addProperty("en", en);
        JsonObject data = new JsonObject(); data.add("title", t); data.add("subtitle", sub);
        data.addProperty("fade_in", 0.3); data.addProperty("stay", 4); data.addProperty("fade_out", 1);
        r.effect(p, "title", data, 0);
    }

    /** Called by the kernel.complete / kernel.poison beats through "unallocated shell <mode>". */
    public static void ending(StoryRuntime r, ServerPlayer p, String mode) {
        switch (mode) {
            case "complete" -> { run(r, p, "bossbar remove unallocated:watchdog"); lashes.clear(); awake = false; phase = Phase.COMPLETE; phaseTicks = 0; }
            case "poison" -> { run(r, p, "bossbar remove unallocated:watchdog"); lashes.clear(); awake = false; phase = Phase.POISON; phaseTicks = 0; }
            case "white_room" -> whiteRoom(r, p);
            case "restore" -> restore(r, p);
            case "remove" -> removeGiant(r, p);
            case "alive" -> aliveRun(r, p);
            default -> { }
        }
    }

    /**
     * Every time you come back alive: Noor's notes are always with you (given again if missing), and from the third
     * run on, Zuri's, who got further and prepares you for what happens when you type at gen 3.
     */
    private static void aliveRun(StoryRuntime r, ServerPlayer p) {
        int runs = r.state().counters().getOrDefault("story.counter.runs", 0) + 1;
        r.state().setCounter("story.counter.runs", runs);
        boolean noor = giveBook(r, p, "Noor", "puppetbook", 3);
        boolean zuri = runs >= 3 && giveBook(r, p, "Zuri", "zuribook", 4);
        if (zuri) bar(r, p, "Alguien más dejó algo para ti. Un libro firmado por Zuri.", "Someone else left something for you. A book signed by Zuri.");
        else if (noor && runs == 1) bar(r, p, "Hay un libro en tu inventario que no estaba antes.", "There is a book in your inventory that was not there before.");
        else bar(r, p, "Los apuntes de Noor siguen contigo.", "Noor's notes are still with you.");
    }

    private static boolean giveBook(StoryRuntime r, ServerPlayer p, String title, String key, int pages) {
        var inventory = p.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            var content = inventory.getItem(i).get(net.minecraft.core.component.DataComponents.WRITTEN_BOOK_CONTENT);
            if (content != null && content.title().raw().equals(title)) return false;
        }
        StringBuilder list = new StringBuilder();
        for (int i = 0; i < pages; i++) list.append(i == 0 ? "" : ",").append("{raw:{translate:\"story.unallocated.").append(key).append(".").append(i).append("\"}}");
        run(r, p, "give @s minecraft:written_book[minecraft:written_book_content={title:\"" + title + "\",author:\"" + title + "\",pages:[" + list + "]}]");
        return true;
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
                boolean puppet = c.equals("puppet") && r.puppet();
                if (!puppet) pendingDescent = -1;
                if (puppet && pendingDescent >= 0 && ticks >= pendingDescent) { pendingDescent = -1; descend(r, p); return; }
                // alive after the first trial, sleeping is consenting again: it takes you in your sleep
                if (c.equals("puppet") && !r.puppet() && p.isSleeping() && !flag(r, "slept")) setFlag(r, "slept");
                if (puppet) replaySouls(p, level);
                if (puppet && depth(r) >= 3 && !gen3Warned && phaseTicks > 100) {
                    gen3Warned = true;
                    bar(r, p, "Más hondo que nunca. Ahora puedes ver sus procesos. Si escribes, bajará.", "Deeper than ever. Now you can see its processes. If you type, it will come down.");
                }
                if (puppet && depth(r) < 3 && ticks % 20 == 0) {
                    run(r, p, "bossbar add unallocated:attention {translate:\"shell.unallocated.attention\",fallback:\"it is listening\"}");
                    run(r, p, "bossbar set unallocated:attention max " + MAX_ATTENTION);
                    run(r, p, "bossbar set unallocated:attention color red");
                    run(r, p, "bossbar set unallocated:attention value " + attention(r));
                    run(r, p, "bossbar set unallocated:attention players @s");
                } else if (ticks % 20 == 0) run(r, p, "bossbar remove unallocated:attention");
            }
            case DESCENT -> {
                giantX = p.getX(); giantZ = p.getZ();
                giantY = Math.max(p.getY() + 1.5, giantY - 0.45);
                moveGiant(r, p);
                if (phaseTicks % 12 == 0) level.playSound(null, p.blockPosition().above(8), UnallocatedContent.RECLAIMER_ROAR, SoundSource.HOSTILE, 2F, 0.5F);
                if (giantY <= p.getY() + 1.6) {
                    AudioSounds.play(p, "screamer", p.position());
                    blackout(r, p, 0.05, 1.2, 0.6, null);
                    r.state().setCounter("story.counter.attention", 0); halfWarned = false;
                    r.state().setCounter("story.counter.trial", r.state().counters().getOrDefault("story.counter.trial", 0) + 1);
                    run(r, p, "bossbar remove unallocated:attention");
                    run(r, p, "kill @s");
                    removeGiant(r, p);
                    phase = Phase.IDLE; phaseTicks = 0;
                }
            }
            case FIGHT -> fightTick(r, p, level);
            case WINDOW -> {
                if (awake) {
                    fightTick(r, p, level);
                    if (phase != Phase.WINDOW) return;
                    if (phaseTicks % 100 == 0) bar(r, p, "¡Despertó! Detenlo otra vez: kill 7331", "It woke up! Stop it again: kill 7331");
                    return;
                }
                frozenTicks++; sinceFreeze++;
                // Frozen, it still breathes; near the end the seconds tick out loud.
                if (sinceFreeze % 70 == 0) level.playSound(null, BlockPos.containing(giantX, giantY + 4, giantZ), UnallocatedContent.RECLAIMER_BREATH, SoundSource.HOSTILE, 2.5F, 0.6F);
                if (sinceFreeze % 160 == 80) level.playSound(null, p.blockPosition().above(10), UnallocatedContent.THREAD_VIBRATE, SoundSource.HOSTILE, 0.6F, 0.5F);
                if ((WINDOW_TICKS - frozenTicks) <= 400 && frozenTicks % 20 == 0)
                    level.playSound(null, p.blockPosition(), net.minecraft.sounds.SoundEvents.NOTE_BLOCK_HAT.value(), SoundSource.MASTER, 1F, 0.8F);
                int left = Math.max(0, (WINDOW_TICKS - frozenTicks) / 20);
                if (frozenTicks % 20 == 0) {
                    run(r, p, "bossbar set unallocated:watchdog value " + left);
                    run(r, p, "bossbar set unallocated:watchdog name \"watchdog :: restart " + left + " s\"");
                    if (left <= 30) run(r, p, "bossbar set unallocated:watchdog color red");
                }
                if (sinceFreeze % 200 == 40) bar(r, p, "⏱ " + left + " s · set success_threshold <valor>  ·  echo <nombre> >> dataset", "⏱ " + left + " s · set success_threshold <value>  ·  echo <name> >> dataset");
                if (sinceFreeze == 220) level.playSound(null, BlockPos.containing(giantX, giantY + 6, giantZ), UnallocatedContent.RECLAIMER_ROAR, SoundSource.HOSTILE, 3F, 0.5F);
                if (sinceFreeze >= 300) wake(r, p);
                if (frozenTicks >= WINDOW_TICKS) consumed(r, p, true);
            }
            case COMPLETE -> {
                if (phaseTicks % 60 == 0 && phaseTicks < 360) level.playSound(null, BlockPos.containing(giantX, giantY + 4, giantZ), UnallocatedContent.THREAD_SNAP, SoundSource.HOSTILE, 1.5F, 1.6F);
                // It sinks slowly, lucid, and comes apart into threads.
                if (phaseTicks < 360 && phaseTicks % 4 == 0) { giantY -= 0.2; moveGiant(r, p); }
                if (phaseTicks % 5 == 0) level.sendParticles(new DustParticleOptions(0x8A1020, 2.0F), giantX, giantY + 5, giantZ, 40, 2.5, 5, 2.5, 0.02);
            }
            case POISON -> {
                // In the white room, broken: it jerks, searches, never finds.
                if (phaseTicks % 3 == 0) run(r, p, String.format(Locale.ROOT, "tp @e[tag=%s,limit=1] %.2f %.2f %.2f %d %d", GIANT,
                    giantX + RANDOM.nextGaussian() * 0.15, giantY, giantZ + RANDOM.nextGaussian() * 0.15, RANDOM.nextInt(360) - 180, RANDOM.nextInt(120) - 60));
                if (phaseTicks % 9 == 0) level.sendParticles(new DustParticleOptions(0x8A1020, 1.5F), giantX, giantY + 2, giantZ, 20, 1.5, 2, 1.5, 0.05);
                if (phaseTicks % 13 == 0) level.playSound(null, BlockPos.containing(giantX, giantY + 2, giantZ), RANDOM.nextBoolean() ? UnallocatedContent.RECLAIMER_CLICK : UnallocatedContent.RECLAIMER_WHISPER,
                    SoundSource.HOSTILE, 1.6F, 0.5F + RANDOM.nextFloat());
                if (phaseTicks % 90 == 0) level.playSound(null, BlockPos.containing(giantX, giantY + 2, giantZ), UnallocatedContent.THREAD_SNAP, SoundSource.HOSTILE, 1.2F, 0.4F);
            }
            case CONSUMED -> {
                giantX = p.getX(); giantZ = p.getZ(); giantY = Math.max(p.getY() + 1, giantY - 0.6); moveGiant(r, p);
                if (phaseTicks == 18) { AudioSounds.play(p, "screamer", p.position()); blackout(r, p, 0.05, 3, 0.5, "#300000"); }
                if (phaseTicks == 40) { removeGiant(r, p); r.end(p, "CONSUMED"); reset(); }
            }
        }
    }

    /*
     * Balance (blocks/s): player walks 4.3, sprints 5.6; it lands 18 away and walks 3.0, rising to 4.0 after 90 s, so
     * sprinting always gains ground and standing still to type costs ~3-4 blocks per second. Lashes give 2.5 s of
     * warning every 8 s (6 s on a perch, 5.5 s after two minutes). A grab or a lash costs one of three connections and
     * staggers it 2.5 s. A perch buys 15 s before it shakes it down (warning at 7 s). The scream that closes the
     * console comes every 35-50 s with a 1.5 s roar first. Hints: help (30 s), ps (60 s), kill <pid> (100 s).
     */
    /** The fallen souls repeat what was recorded of them: a few steps, a pause, and back to the start, forever. */
    private static final Map<String, Vec3> ORIGINS = new HashMap<>();
    private static void replaySouls(ServerPlayer p, ServerLevel level) {
        for (int i = 1; i <= 4; i++) {
            var e = org.unallocated.creature.Creatures.entity(p, "soul" + i);
            if (e == null || e.level() != level) continue;
            Vec3 o = ORIGINS.computeIfAbsent("soul" + i, k -> e.position());
            double angle = i * Math.PI / 2 + 0.6, t = (ticks + i * 53) % 220;
            double along = t < 170 ? t / 170.0 * 7 : 7;
            double x = o.x + Math.cos(angle) * along, z = o.z + Math.sin(angle) * along;
            double y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) Math.floor(x), (int) Math.floor(z));
            float yaw = (float) Math.toDegrees(Math.atan2(-Math.cos(angle), Math.sin(angle)));
            e.teleportTo(x, y, z); e.setYRot(yaw); e.setYHeadRot(yaw); e.setYBodyRot(yaw);
        }
    }

    private static void fightTick(StoryRuntime r, ServerPlayer p, ServerLevel level) {
        // Falling into the void is not the end: it spits you back into its world.
        if (p.getY() < 50) {
            blackout(r, p, 0.05, 0.6, 0.6, null);
            p.teleportTo(level, SAFE.x, SAFE.y, SAFE.z, Set.of(), p.getYRot(), p.getXRot(), true);
            p.fallDistance = 0;
            say(r, p, "te escupe de vuelta. todavía no te deja ir.", "it spits you back. it is not letting you go yet.", "#aa0000");
        }
        // It hunts on the ground; it cannot climb.
        double dx = p.getX() - giantX, dz = p.getZ() - giantZ, dist = Math.sqrt(dx * dx + dz * dz);
        double ground = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) Math.floor(giantX), (int) Math.floor(giantZ));
        if (phaseTicks < 80) giantY = Math.max(ground, giantY - 0.45); // the descent
        else if (ground > 40) giantY = ground;
        if (phaseTicks == 80) { level.playSound(null, BlockPos.containing(giantX, giantY, giantZ), UnallocatedContent.RECLAIMER_ROAR, SoundSource.HOSTILE, 4F, 0.5F); run(r, p, "effect give @s minecraft:nausea 3 0 true"); }
        double speed = Math.min(0.20, 0.15 + phaseTicks / 1800.0 * 0.05);
        if (stagger > 0) stagger--;
        else if (dist > 2.4 && phaseTicks >= 80) { giantX += dx / dist * Math.min(speed, dist); giantZ += dz / dist * Math.min(speed, dist); }
        moveGiant(r, p);
        double above = p.getY() - giantY;
        boolean perched = above >= 3.5;
        if (dist < 3.0 && !perched && phaseTicks >= 80 && ticks - lastStruckTick > 60) {
            Vec3 push = new Vec3(dx, 0, dz).normalize().scale(1.4);
            p.push(push.x, 0.6, push.z); p.connection.send(new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(p));
            stagger = 50;
            if (struck(r, p, level)) return;
        }
        // On a perch it waits underneath, then shakes it down.
        if (perched && dist < 8) {
            perchTicks++;
            if (perchTicks == 140) bar(r, p, "Te espera abajo. Va a tirar lo que te sostiene.", "It waits below. It is going to bring down what holds you.");
            if (perchTicks >= 300) {
                perchTicks = 0;
                BlockPos under = p.blockPosition().below();
                for (int x = -2; x <= 2; x++) for (int z = -2; z <= 2; z++) for (int y = 0; y >= -5; y--) collapse(level, under.offset(x, y, z), y == 0);
                level.playSound(null, under, UnallocatedContent.RECLAIMER_ROAR, SoundSource.HOSTILE, 3F, 0.6F);
            }
        } else perchTicks = Math.max(0, perchTicks - 2);
        // Threads lash down from the sky where you stand: 2.5 s of warning.
        if (--nextLash <= 0 && phaseTicks >= 80) {
            lashes.add(new Lash(new Vec3(p.getX(), p.getY(), p.getZ()), phaseTicks + 50));
            level.playSound(null, p.blockPosition(), UnallocatedContent.THREAD_VIBRATE, SoundSource.HOSTILE, 2F, 0.6F);
            nextLash = perched ? 120 : phaseTicks > 2400 ? 110 : 160;
        }
        for (Iterator<Lash> it = lashes.iterator(); it.hasNext();) {
            Lash l = it.next();
            if (phaseTicks % 2 == 0) {
                level.sendParticles(new DustParticleOptions(0xFF1020, 1.6F), l.at.x, l.at.y + 0.1, l.at.z, 14, 0.8, 0.02, 0.8, 0);
                for (int y = 1; y < 24; y += 3) level.sendParticles(new DustParticleOptions(0x8A1020, 1.0F), l.at.x, l.at.y + y, l.at.z, 1, 0.05, 0.3, 0.05, 0);
            }
            if (phaseTicks >= l.fallTick) {
                it.remove();
                level.sendParticles(ParticleTypes.EXPLOSION, l.at.x, l.at.y + 0.5, l.at.z, 3, 0.5, 0.5, 0.5, 0);
                level.playSound(null, BlockPos.containing(l.at), UnallocatedContent.THREAD_SNAP, SoundSource.HOSTILE, 2.5F, 0.5F);
                collapse(level, BlockPos.containing(l.at).below(), true);
                if (p.position().distanceTo(l.at) < 1.6 && ticks - lastStruckTick > 30) { if (struck(r, p, level)) return; }
            }
        }
        // Its world comes apart around you, revealing the void underneath.
        for (int burst = 0; burst < 2 && phaseTicks >= 80; burst++) {
            double angle = RANDOM.nextDouble() * Math.PI * 2, radius = 8 + RANDOM.nextDouble() * 24;
            int x = (int) Math.floor(p.getX() + Math.cos(angle) * radius), z = (int) Math.floor(p.getZ() + Math.sin(angle) * radius);
            int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
            for (int y = top; y >= level.getMinY(); y--) collapse(level, new BlockPos(x, y, z), y == top);
            level.sendParticles(ParticleTypes.LARGE_SMOKE, x + .5, top + 1, z + .5, 6, 0.4, 0.6, 0.4, 0.02);
            if (RANDOM.nextInt(4) == 0) level.playSound(null, new BlockPos(x, top, z), net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 0.6F, 0.5F);
        }
        if (phaseTicks % 2 == 0) for (int i = 0; i < 6; i++)
            level.sendParticles(new DustParticleOptions(0xB01020, 1.4F), p.getX() + RANDOM.nextGaussian() * 10, p.getY() + 8 + RANDOM.nextDouble() * 6, p.getZ() + RANDOM.nextGaussian() * 10, 1, 0, -0.5, 0, 0.0);
        // The scream: a roar first, then it closes your console.
        if (--nextScream == 30) level.playSound(null, BlockPos.containing(giantX, giantY + 6, giantZ), UnallocatedContent.RECLAIMER_ROAR, SoundSource.HOSTILE, 4F, 0.7F);
        if (nextScream <= 0) { scream(r, p); nextScream = 700 + RANDOM.nextInt(300); }
        // Dummy-proof hints, only if the player is stuck.
        if (phase == Phase.FIGHT && phaseTicks == 600 && !typedHelp && !typedPs) bar(r, p, "La consola sigue abierta. help.", "The console is still open. help.");
        if (phase == Phase.FIGHT && phaseTicks == 1200 && !typedPs) bar(r, p, "ps muestra sus procesos. El suyo es el que más consume.", "ps shows its processes. Its own is the one eating everything.");
        if (phase == Phase.FIGHT && phaseTicks == 2000) bar(r, p, "kill <su número>", "kill <its number>");
        if (phaseTicks % 200 == 0 && phaseTicks >= 200) bar(r, p, "conexión " + (CONNECTIONS - lost) + "/" + CONNECTIONS, "connection " + (CONNECTIONS - lost) + "/" + CONNECTIONS);
    }

    private static void collapse(ServerLevel level, BlockPos pos, boolean visible) {
        if (Math.abs(pos.getX() - SAFE.x) <= 5 && Math.abs(pos.getZ() - SAFE.z) <= 5) return;
        BlockState state = level.getBlockState(pos);
        if (state.isAir() || state.is(Blocks.BEDROCK)) return;
        if (visible) FallingBlockEntity.fall(level, pos, state);
        else level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
    }

    /** One connection lost; returns true when it was the last one. */
    private static boolean struck(StoryRuntime r, ServerPlayer p, ServerLevel level) {
        lastStruckTick = ticks; lost++;
        blackout(r, p, 0.02, 0.25, 0.5, "#550000");
        level.playSound(null, p.blockPosition(), UnallocatedContent.RECLAIMER_ROAR, SoundSource.HOSTILE, 2F, 1.2F);
        int left = CONNECTIONS - lost;
        say(r, p, "conexión " + left + "/" + CONNECTIONS, "connection " + left + "/" + CONNECTIONS, "#aa0000");
        if (left <= 0) {
            lost = 0; lashes.clear(); stagger = 80;
            blackout(r, p, 0.05, 1.0, 0.8, "#300000");
            p.teleportTo(level, SAFE.x, SAFE.y, SAFE.z, Set.of(), p.getYRot(), p.getXRot(), true);
            p.fallDistance = 0;
            say(r, p, "te soltó. todavía no termina contigo. conexión 3/3.", "it let you go. it is not done with you. connection 3/3.", "#aa0000");
            return true;
        }
        return false;
    }
}

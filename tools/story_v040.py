"""Generate the v0.4 story (story.json + chapters) from one readable place.

Run: python3 tools/story_v040.py   (then python3 scripts/story_check.py)
Flow (docs/design/*, docs/intent/PLAYTEST-2026-11-01.md): server (hub -> survival -> hill bait -> path -> predecessor's
base -> mine) -> heap (first, inevitable death) -> puppet (stay dead, cut the three guard threads) -> kernel (shell,
watchdog, endings). Every chapter guides the player, nudges a lost one, and never traps anyone.
"""
import json
from pathlib import Path

D = Path(__file__).resolve().parents[1] / 'src/main/resources/data/unallocated/story'
GRAY, SERVER, YELLOW, RED, WHITE = '#8a8a8a', '#55ffff', '#ffff55', '#aa0000', '#e8e8e8'


def T(es, en=None, color=None):
    t = {'es': es, 'en': en if en is not None else es}
    if color: t['color'] = color
    return t


def chat(at, es, en=None, color=None, sender=None):
    a = {'at': at, 'do': 'chat', 'text': T(es, en, color)}
    if sender: a['from'] = T(sender)
    return a


def bar(at, es, en):
    return {'at': at, 'do': 'actionbar', 'text': T(es, en)}


def cmd(at, run):
    return {'at': at, 'do': 'command', 'run': run}


def flag(at, name):
    return {'at': at, 'do': 'flag', 'set': name}


def blackout(at, fade_in=0.2, hold=1.0, fade_out=0.6):
    return {'at': at, 'do': 'blackout', 'fade_in': fade_in, 'hold': hold, 'fade_out': fade_out}


def creature(at, op, **keys):
    a = {'at': at, 'do': 'creature', 'op': op, 'id': 'reclaimer'}
    a.update(keys)
    return a


FACE = 'tp @s ~ ~ ~ facing entity @e[type=unallocated:reclaimer,limit=1,sort=nearest] eyes'
LEAK_G0 = 'g0: estimate human fear response :: pool UNALLOCATED'

# ---------------------------------------------------------------- chapter 1: the server (alive, prey)
server = {
    'id': 'server', 'budget_seconds': 420, 'checkpoint': 'lobby.spawn',
    'on_enter': [
        {'at': 0, 'do': 'audio', 'cue': 'ambient'},
        cmd(0, 'unallocated tab frame'),
        cmd(0, 'scoreboard objectives add vc dummy {translate:"score.unallocated.title"}'),
        cmd(0.1, 'scoreboard objectives setdisplay sidebar vc'),
        cmd(0.1, 'scoreboard objectives modify vc numberformat blank'),
        cmd(0.2, 'scoreboard players set mode vc 5'),
        cmd(0.2, 'scoreboard players display name mode vc {translate:"score.unallocated.mode"}'),
        cmd(0.2, 'scoreboard players set online vc 4'),
        cmd(0.2, 'scoreboard players display name online vc {translate:"score.unallocated.online"}'),
        cmd(0.2, 'scoreboard players set rank vc 3'),
        cmd(0.2, 'scoreboard players display name rank vc {translate:"score.unallocated.rank"}'),
        cmd(0.2, 'scoreboard players set address vc 1'),
        cmd(0.2, 'scoreboard players display name address vc {translate:"score.unallocated.address"}'),
        cmd(0.3, 'give @s minecraft:compass[minecraft:custom_name={translate:"item.unallocated.selector",italic:false},'
                 'minecraft:lore=[{translate:"item.unallocated.selector.lore",italic:false}]]'),
        cmd(0.3, 'give @s minecraft:written_book[minecraft:written_book_content={title:"ValleCraft",author:"Staff",'
                 'pages:[{raw:{translate:"item.unallocated.rules.0"}},{raw:{translate:"item.unallocated.rules.1"}}]},'
                 'minecraft:custom_name={translate:"item.unallocated.rules",italic:false}]'),
        cmd(0.3, 'give @s minecraft:spyglass[minecraft:custom_name={translate:"item.unallocated.spyglass",italic:false},'
                 'minecraft:lore=[{translate:"item.unallocated.spyglass.lore",italic:false}]]'),
        {'at': 0.5, 'do': 'title', 'title': T('ValleCraft Network'),
         'subtitle': T('Bienvenido de nuevo, {player}', 'Welcome back, {player}'), 'fade_in': 0.5, 'stay': 3, 'fade_out': 1},
        chat(1.5, '[ValleCraft] Bienvenido a ValleCraft Network · Survival · Minijuegos · Plots',
             '[ValleCraft] Welcome to ValleCraft Network · Survival · Minigames · Plots', SERVER),
        chat(2.5, '[ValleCraft] Usa la brújula para elegir un modo. Lee las reglas antes de jugar.',
             '[ValleCraft] Use the compass to choose a mode. Read the rules before you play.', SERVER),
        bar(4, 'Explora el hub. La salida a SURVIVAL está al este del spawn →', 'Explore the hub. The way to SURVIVAL is east of spawn →'),
    ],
    'on_resume': [{'at': 0, 'do': 'audio', 'cue': 'ambient'}, cmd(0, 'unallocated tab frame')],
    'escape': [
        chat(0, 'el suelo de la mina cede.', 'the mine floor gives way.', GRAY),
        blackout(0, 0.3, 1.2, 0.6),
    ],
    'beats': [
        {'id': 'server.explorer', 'when': {'type': 'chapter_start'}, 'if': {'flags': ['arrival.explorer']}, 'actions': [
            chat(6, '[ValleCraft] Última actividad registrada: 14/06/2021.', '[ValleCraft] Last recorded activity: 06/14/2021.', SERVER),
            chat(8, '[ValleCraft] Jugadores conectados: 1 (tú).', '[ValleCraft] Players online: 1 (you).', SERVER)]},
        {'id': 'server.former', 'when': {'type': 'chapter_start'}, 'if': {'flags': ['arrival.former']}, 'actions': [
            chat(6, '[ValleCraft] Rango restaurado: Ayudante.', '[ValleCraft] Rank restored: Helper.', SERVER),
            cmd(6, 'scoreboard players display name rank vc {translate:"score.unallocated.rank_helper"}'),
            cmd(0, 'unallocated arrival sign -30 66 11'),
            chat(8, '[ValleCraft] Tu casa sigue protegida a tu nombre. Nadie la ha tocado.',
                 '[ValleCraft] Your house is still claimed in your name. Nobody touched it.', SERVER)]},
        {'id': 'server.invited', 'when': {'type': 'chapter_start'}, 'if': {'flags': ['arrival.invited']}, 'actions': [
            cmd(0, 'unallocated tab add Kai'),
            chat(7, 'viniste.', 'you came.', sender='Kai'),
            chat(11, 'estoy en survival. sal por la puerta grande. te espero en la base.',
                 'i\'m in survival. go out the big gate. i\'ll wait at the base.', sender='Kai')]},
        {'id': 'server.selector', 'when': {'type': 'use_item', 'item': 'minecraft:compass'},
         'repeat': {'max': 6, 'cooldown_seconds': 3}, 'actions': [
            chat(0, 'SURVIVAL — en línea', 'SURVIVAL — online', '#55ff55'),
            chat(0, 'MINIJUEGOS — fuera de línea', 'MINIGAMES — offline', GRAY),
            chat(0, 'PLOTS — fuera de línea', 'PLOTS — offline', GRAY),
            chat(0, 'CREATIVO — fuera de línea', 'CREATIVE — offline', GRAY),
            chat(0.6, '▓▓▓ — UNALLOCATED', '▓▓▓ — UNALLOCATED', RED)]},
        {'id': 'server.leave_hub', 'when': {'type': 'zone', 'zone': 'lobby.exit'}, 'actions': [
            chat(0, '[ValleCraft] Mundo Survival. Las zonas protegidas tienen dueño: respétalas.',
                 '[ValleCraft] Survival world. Claimed land has an owner: respect it.', SERVER),
            cmd(4, 'unallocated tab add Noor'),
            chat(4, 'Noor se unió a la partida', 'Noor joined the game', YELLOW),
            {'at': 6, 'do': 'bait', 'op': 'place', 'scare': True, 'seed': 4114, 'lure': 'hill.bait', 'fallback_after': 40},
            bar(8, 'Hay alguien de pie en la colina, al norte.', 'Someone is standing on the hill to the north.')],
         'capture': {'name': 'hub', 'at': 0.5}},
        {'id': 'server.bait_scream', 'when': {'type': 'bait', 'event': 'screamer'}, 'actions': [
            chat(1.5, 'Can\'t keep up! Is the server overloaded? Running 2047ms or 40 ticks behind', None, GRAY),
            {'at': 1.5, 'do': 'audio', 'cue': 'muffle'},
            cmd(3, 'unallocated tab remove Noor'),
            chat(3, 'Noor salió de la partida', 'Noor left the game', YELLOW),
            bar(5, 'Sigue el camino de antorchas hacia la base.', 'Follow the torch path to the base.')],
         'capture': {'name': 'colina', 'at': 0.5}, 'autoplay': {'wait_seconds': 30}},
        {'id': 'server.bait_force', 'when': {'type': 'after', 'seconds': 75, 'beat': 'server.leave_hub'},
         'if': {'not_beats': ['server.bait_scream']}, 'actions': [{'at': 0, 'do': 'bait', 'op': 'scare'}]},
        {'id': 'server.lost_time', 'when': {'type': 'zone', 'zone': 'presa.path'}, 'actions': [
            blackout(0, 0.05, 0.4, 0.3), {'at': 0, 'do': 'audio', 'cue': 'muffle'},
            cmd(0.1, 'time add 2500'),
            chat(1.2, '[Server] Lag detectado: 2500 ticks omitidos. Reloj resincronizado.',
                 '[Server] Lag detected: 2500 ticks skipped. Clock resynchronized.', GRAY),
            bar(2.5, '¿Ya es de noche? Hace un momento era de día.', 'Night already? It was daytime a moment ago.')], 'autoplay': {'wait_seconds': 90}},
        {'id': 'server.sprint', 'when': {'type': 'sprint', 'seconds': 4}, 'actions': [
            chat(0, 'no corras. no sirve el dato si corres.', 'don\'t run. the data is useless if you run.', sender='{player}')]},
        {'id': 'server.rewrite', 'when': {'type': 'chat', 'pattern': '\\S', 'hide': True}, 'actions': [
            chat(0, 'vamos al pozo', 'let\'s go to the pit', sender='{player}')], 'autoplay': {'input': 'vamos a la izquierda'}},
        {'id': 'server.fury', 'when': {'type': 'chat', 'pattern': '(?i)reclaimer'}, 'actions': [
            chat(0, 'muestra inválida :: el sujeto conoce la designación', 'invalid sample :: the subject knows the designation', RED),
            {'at': 0.3, 'do': 'audio', 'cue': 'muffle'},
            {'at': 0.3, 'do': 'interface', 'effect': 'GAMMA', 'seconds': 4, 'strength': 1}], 'autoplay': {'input': 'reclaimer'}},
        {'id': 'server.base', 'when': {'type': 'zone', 'zone': 'base.theft_zone'}, 'actions': [
            bar(0.5, 'La base de alguien. Letreros, un cofre. Alguien investigaba algo aquí.',
                'Someone\'s base. Signs, a chest. Someone was investigating something here.'),
            {'at': 8, 'do': 'interface', 'effect': 'SIGN', 'seconds': 5, 'generation': 2, 'strength': 0.5, 'leak': 'mixed',
             'seed': 4114, 'sign': {'pos': [53, 64, 10]}, 'text': T('objetivo: obtener fear de subject // retry',
                                                                    'objective: obtain fear from subject // retry')},
            bar(14, 'Detrás de la base está la mina.', 'Behind the base is the mine.')],
         'capture': {'name': 'base', 'at': 1}, 'autoplay': {'wait_seconds': 100}},
        {'id': 'server.mine', 'when': {'type': 'zone', 'zone': 'mine.entrance'}, 'actions': [
            bar(0, 'La mina. Un letrero decía que da vueltas.', 'The mine. A sign said it loops.')], 'autoplay': {'wait_seconds': 110}},
        {'id': 'server.loop1', 'when': {'type': 'zone', 'zone': 'presa.mine_end'}, 'actions': [
            cmd(0, 'tp @s ~-14 ~ ~'), flag(0.5, 'loop1')], 'capture': {'name': 'mina', 'at': 0.6}, 'autoplay': {'wait_seconds': 120}},
        {'id': 'server.loop2', 'when': {'type': 'zone', 'zone': 'presa.mine_end'}, 'if': {'flags': ['loop1']}, 'actions': [
            cmd(0, 'tp @s ~-14 ~ ~'), flag(0.5, 'loop2'),
            bar(1, 'Otra vez el mismo tramo.', 'The same stretch again.')]},
        {'id': 'server.fall', 'when': {'type': 'zone', 'zone': 'presa.mine_end'}, 'if': {'flags': ['loop2']}, 'actions': [
            cmd(0, 'fill ~-1 ~-1 ~-1 ~1 ~-1 ~1 unallocated:rot'), {'at': 0, 'do': 'audio', 'cue': 'muffle'},
            chat(0.6, LEAK_G0, None, GRAY), blackout(0.8, 0.4, 1.2, 0.6), {'at': 1.3, 'do': 'next_chapter'}]},
        {'id': 'server.nudge_hub', 'when': {'type': 'after', 'seconds': 90}, 'if': {'not_beats': ['server.leave_hub']}, 'actions': [
            bar(0, 'La puerta de SURVIVAL está al este del spawn. Sigue el camino →', 'The SURVIVAL gate is east of spawn. Follow the road →')]},
        {'id': 'server.nudge_base', 'when': {'type': 'after', 'seconds': 200}, 'if': {'not_beats': ['server.base']}, 'actions': [
            bar(0, 'Sigue el camino de antorchas hasta la base, al este.', 'Follow the torch path east to the base.')]},
        {'id': 'server.nudge_mine', 'when': {'type': 'after', 'seconds': 300}, 'if': {'not_beats': ['server.loop1']}, 'actions': [
            bar(0, 'La mina está detrás de la base. Entra y sigue hasta el fondo.', 'The mine is behind the base. Go in, all the way down.')]},
    ],
}

# ---------------------------------------------------------------- chapter 2: the Heap (alive; first, inevitable death)
HUNT = [creature(0, 'spawn', body={'summon': 'unallocated:reclaimer'}, where='heap.spider', facing='player'),
        creature(0.1, 'stalk', step=1.2, min_distance=8)]
heap = {
    'id': 'heap', 'budget_seconds': 150, 'checkpoint': 'heap.arrival',
    'on_enter': [
        {'at': 0, 'do': 'audio', 'cue': 'ambient'},
        cmd(0, 'scoreboard objectives modify vc displayname {translate:"score.unallocated.heap_title"}'),
        cmd(0, 'scoreboard players display name online vc {translate:"score.unallocated.online_heap"}'),
        cmd(0, 'scoreboard players display name rank vc {translate:"score.unallocated.rank_subject"}'),
        chat(1, LEAK_G0, None, GRAY),
        bar(3, 'Es el mismo servidor. Pero no es el mismo.', 'It is the same server. But it is not the same.'),
    ] + [dict(a, at=a['at'] + 2) for a in HUNT],
    'on_resume': [{'at': 0, 'do': 'audio', 'cue': 'ambient'}],
    'escape': [{'at': 0, 'do': 'counter', 'id': 'trial', 'add': 1},
               {'at': 0, 'do': 'interface', 'effect': 'DEATH', 'seconds': 30, 'text': T('subject connection: still open. retaining (+fear)')},
               cmd(0.4, 'kill @s')],
    'beats': [
        {'id': 'heap.see', 'when': {'type': 'chapter_start'}, 'actions': [
            {'at': 3, 'do': 'audio', 'cue': 'threads', 'where': 'heap.spider'},
            bar(6, 'Algo grande está quieto en el camino. Se mueve cuando no lo miras.',
                'Something big stands still on the path. It moves when you are not looking.')],
         'capture': {'name': 'heap', 'at': 6}},
        {'id': 'heap.hunt', 'when': {'type': 'after', 'seconds': 16}, 'actions': [
            cmd(0, FACE), creature(0.1, 'chase', speed=5.5, catch_distance=1.5)], 'capture': {'name': 'cazar', 'at': 0.6}},
        {'id': 'heap.caught', 'when': {'type': 'creature', 'creature': 'reclaimer', 'event': 'caught'},
         'repeat': {'max': 20, 'cooldown_seconds': 2}, 'actions': [
            {'at': 0, 'do': 'counter', 'id': 'trial', 'add': 1}, creature(0, 'hold'),
            {'at': 0.1, 'do': 'interface', 'effect': 'DEATH', 'seconds': 30, 'text': T('subject connection: still open. retaining (+fear)')},
            {'at': 0.2, 'do': 'audio', 'cue': 'screamer'}, cmd(0.5, 'kill @s')], 'autoplay': {'wait_seconds': 20}},
        {'id': 'heap.to_puppet', 'when': {'type': 'respawn'}, 'if': {'flags': ['puppet']}, 'actions': [
            creature(0, 'despawn'), {'at': 0.2, 'do': 'next_chapter'}], 'autoplay': {'wait_seconds': 200}},
        {'id': 'heap.alive_again', 'when': {'type': 'respawn'}, 'if': {'not_flags': ['puppet']},
         'repeat': {'max': 20, 'cooldown_seconds': 1}, 'actions': [
            {'at': 0.3, 'do': 'title', 'title': T('Trial {counter:trial}'), 'fade_in': 0.3, 'stay': 2, 'fade_out': 1},
            chat(1, 'subject connection: still open.', None, GRAY),
            bar(2, 'Al morir, elige «Todavía no» para quedarte como Puppet: sólo así verás sus hilos.',
                'When you die, choose "Not yet" to stay as a Puppet: only then will you see its threads.')]
            + [dict(a, at=a['at'] + 4) for a in HUNT] + [cmd(10, FACE), creature(10.1, 'chase', speed=6, catch_distance=1.5)]},
    ],
}

# ---------------------------------------------------------------- chapter 3: Puppet (dead, sees the threads)
def cut(letter, leak1, leak2):
    return {'id': f'puppet.cut_{letter}', 'when': {'type': 'break', 'block': 'unallocated:thread', 'zone': f'heap.cut_{letter}'},
            'if': {'not_flags': [f'cut_{letter}']}, 'actions': [
                chat(0, leak1, None, GRAY), chat(1.5, leak2, None, GRAY), {'at': 0, 'do': 'audio', 'cue': 'threads'},
                flag(0.2, f'cut_{letter}'), {'at': 0.2, 'do': 'counter', 'id': 'cuts', 'add': 1},
                bar(2.5, 'Hilos cortados: {counter:cuts}/3', 'Threads cut: {counter:cuts}/3')],
            **({'capture': {'name': 'hilos', 'at': 0.3}} if letter == 'a' else {})}


puppet = {
    'id': 'puppet', 'budget_seconds': 300, 'checkpoint': 'heap.arrival',
    'on_enter': [
        cmd(0, 'unallocated puppet now'),
        {'at': 0, 'do': 'title', 'title': T('PUPPET'), 'subtitle': T('subject connection: still open'), 'fade_in': 0.3, 'stay': 3, 'fade_out': 1},
        cmd(0.5, 'give @s minecraft:shears[minecraft:can_break={blocks:"unallocated:thread"},'
                 'minecraft:custom_name={translate:"item.unallocated.shears",italic:false}]'),
        chat(1, 'subject connection: still open. retaining (+fear)', None, GRAY),
        bar(3, 'Ahora ves sus hilos. Tres hilos rojos custodian el túnel negro: córtalos con las tijeras.',
            'Now you see its threads. Three red threads guard the black tunnel: cut them with the shears.'),
        {'at': 2, 'do': 'creature', 'op': 'spawn', 'id': 'echo', 'body': {'mannequin': {'profile': 'player', 'scale': 1}},
         'where': 'heap.echo', 'facing': 'player'},
        {'at': 2.1, 'do': 'creature', 'op': 'stalk', 'id': 'echo', 'step': 1, 'min_distance': 5},
        chat(4, 'artifact[subject 4114] :: training (gen 1)', None, GRAY),
    ],
    'on_resume': [cmd(0, 'unallocated tab frame')],
    'escape': [flag(0, 'cut_a'), flag(0, 'cut_b'), flag(0, 'cut_c'), flag(0, 'path_open'),
               {'at': 0, 'do': 'creature', 'op': 'despawn', 'id': 'echo'}, creature(0, 'despawn'),
               chat(0, 'sensors around the Kernel: offline', None, GRAY)],
    'beats': [
        cut('a', 'human_playtest_pool: UNALLOCATED', 'reclaim pool ... UNALLOCATED'),
        cut('b', 'reclaim[7331] :: cpu 97% :: Can\'t keep up! Is the server overloaded?', 'watchdog :: guarding pid 7331'),
        cut('c', 'success_threshold: 1.37 (spec g0: 0.82)', 'the Kernel :: main loop'),
        {'id': 'puppet.open', 'when': {'type': 'flag', 'flag': 'cut_c'}, 'if': {'flags': ['cut_a', 'cut_b']}, 'actions': [
            flag(0, 'path_open'), chat(1, 'sensors around the Kernel: blind', None, GRAY),
            bar(2, 'El túnel negro está abierto. Al fondo está el Kernel.', 'The black tunnel is open. The Kernel lies at its end.')]},
        {'id': 'puppet.open_b', 'when': {'type': 'flag', 'flag': 'cut_b'}, 'if': {'flags': ['cut_a', 'cut_c'], 'not_beats': ['puppet.open']},
         'actions': [flag(0, 'path_open'), chat(1, 'sensors around the Kernel: blind', None, GRAY),
                     bar(2, 'El túnel negro está abierto. Al fondo está el Kernel.', 'The black tunnel is open. The Kernel lies at its end.')]},
        {'id': 'puppet.open_a', 'when': {'type': 'flag', 'flag': 'cut_a'}, 'if': {'flags': ['cut_b', 'cut_c'], 'not_beats': ['puppet.open', 'puppet.open_b']},
         'actions': [flag(0, 'path_open'), chat(1, 'sensors around the Kernel: blind', None, GRAY),
                     bar(2, 'El túnel negro está abierto. Al fondo está el Kernel.', 'The black tunnel is open. The Kernel lies at its end.')]},
        {'id': 'puppet.blocked', 'when': {'type': 'zone', 'zone': 'kernel.mouth'}, 'if': {'not_flags': ['path_open']},
         'repeat': {'max': 30, 'cooldown_seconds': 4}, 'actions': [
            chat(0, 'credential rotated: proximity event', None, RED), {'at': 0, 'do': 'audio', 'cue': 'muffle'},
            {'at': 0.3, 'do': 'teleport', 'to': 'heap.arrival', 'blackout': True},
            bar(1.5, 'Sus hilos te sintieron. Corta los tres hilos rojos primero ({counter:cuts}/3).',
                'Its threads felt you. Cut the three red threads first ({counter:cuts}/3).')]},
        {'id': 'puppet.alive_block', 'when': {'type': 'zone', 'zone': 'kernel.mouth'}, 'if': {'flags': ['path_open'], 'not_flags': ['puppet']},
         'repeat': {'max': 30, 'cooldown_seconds': 4}, 'actions': [
            chat(0, 'credential rotated: subject alive', None, RED),
            {'at': 0.3, 'do': 'teleport', 'to': 'heap.arrival', 'blackout': True},
            bar(1.5, 'Sólo un Puppet entra al Kernel. Vivo, te siente.', 'Only a Puppet enters the Kernel. Alive, it feels you.')]},
        {'id': 'puppet.enter', 'when': {'type': 'zone', 'zone': 'kernel.mouth'}, 'if': {'flags': ['path_open', 'puppet']}, 'actions': [
            {'at': 0, 'do': 'creature', 'op': 'despawn', 'id': 'echo'}, creature(0, 'despawn'), {'at': 0.2, 'do': 'next_chapter'}]},
        {'id': 'puppet.revived', 'when': {'type': 'respawn'}, 'if': {'not_flags': ['puppet']}, 'repeat': {'max': 20, 'cooldown_seconds': 1},
         'actions': [{'at': 0.3, 'do': 'title', 'title': T('Trial {counter:trial}'), 'fade_in': 0.3, 'stay': 2, 'fade_out': 1},
                     bar(2, 'Vivo otra vez, y te caza. Lo que cortaste sigue cortado.', 'Alive again, and it hunts you. What you cut stays cut.')]
            + [dict(a, at=a['at'] + 3) for a in HUNT] + [cmd(9, FACE), creature(9.1, 'chase', speed=6, catch_distance=1.5)]},
        {'id': 'puppet.caught', 'when': {'type': 'creature', 'creature': 'reclaimer', 'event': 'caught'},
         'repeat': {'max': 20, 'cooldown_seconds': 2}, 'actions': [
            {'at': 0, 'do': 'counter', 'id': 'trial', 'add': 1}, creature(0, 'hold'),
            {'at': 0.2, 'do': 'audio', 'cue': 'screamer'}, cmd(0.5, 'kill @s')]},
        {'id': 'puppet.again', 'when': {'type': 'respawn'}, 'if': {'flags': ['puppet']}, 'repeat': {'max': 20, 'cooldown_seconds': 1},
         'actions': [creature(0, 'despawn'), bar(1, 'Hilos cortados: {counter:cuts}/3. Luego, el túnel negro.',
                                                'Threads cut: {counter:cuts}/3. Then, the black tunnel.')]},
        {'id': 'puppet.fury', 'when': {'type': 'chat', 'pattern': '(?i)reclaimer'}, 'actions': [
            chat(0, 'muestra inválida :: el sujeto conoce la designación', 'invalid sample :: the subject knows the designation', RED),
            {'at': 0.3, 'do': 'audio', 'cue': 'muffle'}, {'at': 0.3, 'do': 'interface', 'effect': 'GAMMA', 'seconds': 4, 'strength': 1}],
         'autoplay': {'input': 'reclaimer'}},
        {'id': 'puppet.nudge', 'when': {'type': 'after', 'seconds': 120}, 'if': {'not_flags': ['path_open']}, 'actions': [
            bar(0, 'Los hilos rojos brillan junto al camino hacia el túnel negro, al este. Golpéalos con las tijeras.',
                'The red threads glow by the path to the black tunnel, to the east. Hit them with the shears.')]},
    ],
}

# ---------------------------------------------------------------- chapter 4: the Kernel (reused shell and endings)
old = json.loads((D / 'chapters/kernel.json').read_text(encoding='utf-8'))
keep = {b['id']: b for b in old['beats']}
kernel = {
    'id': 'kernel', 'budget_seconds': 600, 'checkpoint': 'kernel.gate',
    'on_enter': [
        flag(0, 'no_revive'),
        cmd(0.1, 'unallocated revive lock'),
        {'at': 0, 'do': 'audio', 'cue': 'ambient'},
        {'at': 0, 'do': 'title', 'title': T('the Kernel'), 'fade_in': 0.3, 'stay': 2.5, 'fade_out': 1},
        creature(1, 'spawn', body={'summon': 'unallocated:reclaimer{attributes:[{id:"minecraft:scale",base:1.15d}]}'},
                 where='kernel.heart', facing='player'),
        creature(1.1, 'face_player'),
        {'at': 2, 'do': 'audio', 'cue': 'threads', 'where': 'kernel.heart'},
        bar(4, 'Al fondo del túnel, el centro de la telaraña. Desde aquí no se vuelve.',
            'At the end of the tunnel, the centre of the web. There is no coming back from here.'),
    ],
    'on_resume': old['on_resume'],
    'escape': old['escape'],
    'beats': [],
}
for bid in ('kernel.cross', 'kernel.help', 'kernel.ps', 'kernel.params', 'kernel.suspend', 'kernel.complete', 'kernel.poison',
            'kernel.timeout', 'kernel.wakes', 'kernel.caught', 'kernel.death', 'kernel.expire'):
    b = json.loads(json.dumps(keep[bid]))
    if bid == 'kernel.cross':
        b.pop('if', None)
    for a in b.get('actions', []):
        if a.get('do') == 'creature': a['id'] = 'reclaimer'
    if b['when'].get('type') == 'creature': b['when']['creature'] = 'reclaimer'
    kernel['beats'].append(b)

# ---------------------------------------------------------------- story.json
story = json.loads((D / 'story.json').read_text(encoding='utf-8'))
story['chapters'] = ['server', 'heap', 'puppet', 'kernel']
story['anchors'].pop('vuelta.spider', None)
story['anchors']['heap.arrival'] = {'pos': [53, 64, 12], 'dimension': 'pocket', 'yaw': 90, 'pitch': 0}
story['anchors']['heap.spider'] = {'pos': [34, 64, 3], 'dimension': 'pocket', 'yaw': -90, 'pitch': 0}
story['zones']['heap.cut_c'] = {'box': [88, 64, 18, 90, 67, 22], 'dimension': 'pocket'}
story['zones']['kernel.mouth'] = {'box': [96, 63, 18, 104, 67, 22], 'dimension': 'pocket'}
run = [c for c in story['build']['run'] if 'unallocated:thread[' not in c]
copy_at = next(i for i, c in enumerate(run) if c.startswith('unallocated story copy'))
run.insert(copy_at + 1, 'unallocated story corrupt heap.rot 4114 heap')
guards = [(84.5, 20.5), (89.5, 20.5), (94.5, 20.5)]
for x, z in guards:
    run.append(f'execute in unallocated:pocket run summon unallocated:thread {x} 65.3 {z} {{dx:0.0f,dy:0.0f,dz:5.0f}}')
# The network: threads from the places where its scripts run, all leaning toward the Kernel.
network = [(20, 66, 4, 18, 2, 6), (45, 68, -14, 16, 4, 10), (60, 67, 6, 12, 1, 8), (72, 67, 14, 10, 1, 6),
           (30, 66, 14, 20, 0, 6), (8, 67, -4, 18, 2, 8), (80, 67, 26, 8, 0, -6), (66, 70, 20, 14, 3, 0)]
for x, y, z, dx, dy, dz in network:
    run.append(f'execute in unallocated:pocket run summon unallocated:thread {x}.5 {y} {z}.5 {{dx:{dx}.0f,dy:{dy}.0f,dz:{dz}.0f}}')
for dx, dz in ((10, 0), (0, 10), (7, 7), (7, -7)):
    run.append(f'execute in unallocated:pocket run summon unallocated:thread 141.5 70 20.5 {{dx:{dx}.0f,dy:6.0f,dz:{dz}.0f}}')
story['build']['run'] = run
story['autoplay'] = {'time_scale': 0.1, 'runs': [
    {'ending': e, 'skip': ['server.rewrite', 'server.fury', 'puppet.fury', 'heap.alive_again', 'puppet.revived', 'puppet.caught',
                           'puppet.blocked', 'puppet.alive_block', 'puppet.again', 'kernel.death', 'kernel.caught'] + extra}
    for e, extra in (('COMPLETE', ['kernel.poison', 'kernel.expire']), ('POISON', ['kernel.complete', 'kernel.expire']),
                     ('CONSUMED', ['kernel.complete', 'kernel.poison']))]}

for old_file in (D / 'chapters').glob('*.json'):
    old_file.unlink()
for chapter in (server, heap, puppet, kernel):
    (D / 'chapters' / f"{chapter['id']}.json").write_text(json.dumps(chapter, ensure_ascii=False, indent=1) + '\n', encoding='utf-8')
(D / 'story.json').write_text(json.dumps(story, ensure_ascii=False, indent=1) + '\n', encoding='utf-8')
print('story v0.4 written:', [c['id'] for c in (server, heap, puppet, kernel)])

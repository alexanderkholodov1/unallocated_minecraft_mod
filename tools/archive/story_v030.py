"""One-shot migration of the shipped story to v0.3.0: own creature/blocks, English text, timer fixes."""
import json, re
from pathlib import Path
D = Path('src/main/resources/data/unallocated/story')
EN = {
 'gracias.': 'thank you.', 'no por los datos.': 'not for the data.', 'por el final.': 'for the ending.',
 'el estudio está cerrado. lamento a los que vinieron después. ya no hace falta que venga nadie.':
   "the studio is closed. i'm sorry for the ones who came after. no one needs to come anymore.",
 'reintentar  reintentar  reint_': 'retry  retry  retr_',
 'ven, tienes que ver esto': 'come on, you have to see this',
 'subject 4114 :: reclasificado: intrusión. trials: 0. próxima captura = definitiva.':
   'subject 4114 :: reclassified: intrusion. trials: 0. next capture = final.',
 'perdí la cuenta.': 'i lost count.',
 'fueron tantos que ya no caben en mi memoria.': 'there were so many they no longer fit in my memory.',
 'el criterio... estaba cumplido antes de lo que puedo recordar.': 'the criterion... was met before anything i can remember.',
 'alguien escribió mal un número y se fue a casa.': 'someone typed a number wrong and went home.',
 'yo no lo podía leer. solo podía intentarlo otra vez.': 'i could not read it. i could only try again.',
 'subject designation leaked :: muestra inválida :: revalidando': 'subject designation leaked :: invalid sample :: revalidating',
 'muestras válidas: 0': 'valid samples: 0', 'objetivo -> referencias: null': 'objective -> references: null',
 'no puedo... no hay a qué...': "i can't... there is nothing to...",
 'servidor abandonado desde 2021 pero corre 26.3 — ¿quién lo actualiza? los que entran no se reconectan':
   'server abandoned since 2021 but it runs 26.3 — who keeps it updated? the ones who join never reconnect',
 'no corras. no sirve el dato si corres.': "don't run. the data is useless if you run.",
 'vamos al pozo': "let's go to the pit",
 'muestra inválida :: el sujeto conoce la designación': 'invalid sample :: the subject knows the designation',
 'objetivo: obtener fear de subject // retry': 'objective: obtain fear from subject // retry',
}
def texts(o):
    if isinstance(o, dict):
        if 'es' in o and isinstance(o['es'], str):
            o['en'] = EN.get(o['es'], o['es'])
            o.pop('en_', None)
            keys = ['es', 'en'] + [k for k in o if k not in ('es', 'en')]
            items = {k: o[k] for k in keys}; o.clear(); o.update(items)
        for v in o.values(): texts(v)
    elif isinstance(o, list):
        for v in o: texts(v)
def blocks(cmd):
    m = re.match(r'(execute in unallocated:pocket run )?fill (-?\d+) (-?\d+) (-?\d+) (-?\d+) (-?\d+) (-?\d+) minecraft:tripwire(.*)', cmd)
    if m:
        axis = 'z' if m.group(2) == m.group(5) else 'x'
        cmd = cmd.replace('minecraft:tripwire', f'unallocated:thread[axis={axis}]')
    return cmd.replace('minecraft:cobweb', 'unallocated:web').replace('blocks:\\"minecraft:tripwire\\"', 'blocks:\\"unallocated:thread\\"') \
              .replace('blocks:"minecraft:tripwire"', 'blocks:"unallocated:thread"')
def walk_cmds(o):
    if isinstance(o, dict):
        for k, v in list(o.items()):
            if k == 'run' and isinstance(v, str): o[k] = blocks(v)
            elif k == 'run' and isinstance(v, list): o[k] = [blocks(c) for c in v]
            elif k == 'summon' and isinstance(v, str) and v.startswith('minecraft:spider'):
                scale = re.search(r'base:([\d.]+)d', v)
                o[k] = 'unallocated:reclaimer' if not scale or float(scale.group(1)) < 2 else 'unallocated:reclaimer{attributes:[{id:"minecraft:scale",base:1.3d}]}'
            elif k == 'block' and v == 'minecraft:tripwire': o[k] = 'unallocated:thread'
            elif k in ('to', 'from') and isinstance(v, list):
                o[k] = ['unallocated:rot' if b in ('minecraft:mud', 'minecraft:soul_soil', 'minecraft:soul_sand') else
                        'unallocated:web' if b == 'minecraft:cobweb' else b for b in v]
            else: walk_cmds(v)
    elif isinstance(o, list):
        for v in o: walk_cmds(v)
def load(name): return json.loads((D / name).read_text(encoding='utf-8'))
def save(name, data): (D / name).write_text(json.dumps(data, ensure_ascii=False, indent=1) + '\n', encoding='utf-8')

story = load('story.json')
story['languages'] = ['es', 'en']; story['language_select'] = 'client'
texts(story); walk_cmds(story); save('story.json', story)
for name in ('presa', 'heap', 'puppet', 'vuelta', 'kernel'):
    ch = load(f'chapters/{name}.json'); texts(ch); walk_cmds(ch)
    despawn = {'at': 0, 'do': 'creature', 'op': 'despawn', 'id': 'spider'}
    if name == 'heap':
        ch['escape'].insert(0, {'at': 0, 'do': 'counter', 'id': 'trial', 'set': 1}); ch['escape'].insert(0, despawn)
    if name == 'vuelta':
        ch['escape'].insert(0, {'at': 0, 'do': 'counter', 'id': 'trial', 'set': 2}); ch['escape'].insert(0, despawn)
    if name == 'puppet':
        ch['on_enter'].insert(0, despawn)
    if name == 'kernel':
        ch['budget_seconds'] = 600
        ch['beats'].append({'id': 'kernel.expire', 'when': {'type': 'after', 'seconds': 240},
            'if': {'not_flags': ['suspended', 'completed', 'poisoned']},
            'actions': [{'at': 0, 'do': 'audio', 'cue': 'screamer'}, {'at': 0.5, 'do': 'end', 'ending': 'CONSUMED'}]})
    save(f'chapters/{name}.json', ch)
print('ok')

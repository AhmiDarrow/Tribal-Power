"""Check en_us.json is valid JSON without duplicate keys and that every registered block, item and entity has a name."""
import json, re, sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
LANG = ROOT / 'src/main/resources/assets/tribalpower/lang/en_us.json'
JAVA = ROOT / 'src/main/java'

def load_lang():
    dupes = []
    def hook(pairs):
        seen = set()
        for k, _ in pairs:
            if k in seen: dupes.append(k)
            seen.add(k)
        return dict(pairs)
    data = json.loads(LANG.read_text(encoding='utf-8'), object_pairs_hook=hook)
    return data, dupes

# (regex over Java source, lang prefix)
# Block items fall back to their block's name, so an item id may be named by the block key instead.
PATTERNS = [
    (r'BLOCKS\.register(?:Block)?\(\s*"([a-z0-9_]+)"', 'block'),
    (r'\bregisterBlock\(\s*"([a-z0-9_]+)"', 'block'),
    (r'ITEMS\.register(?:Item|SimpleBlockItem|SimpleItem)?\(\s*"([a-z0-9_]+)"', 'item'),
    (r'\bregister(?:SimpleBlockItem|SimpleItem|Item)\(\s*"([a-z0-9_]+)"', 'item'),
    (r'(?<!BLOCK_)ENTITIES\.register\(\s*"([a-z0-9_]+)"', 'entity'),
    (r'ENTITY_TYPES\.register\(\s*"([a-z0-9_]+)"', 'entity'),
    (r'\.build\(\s*"tribalpower:([a-z0-9_]+)"', 'entity'),
]
# Ids built at runtime from enums: creatures (CreatureProfile) and the four Kin spawn eggs (KinRole).
def dynamic():
    ids = set()
    profile = (JAVA / 'tk/darrow/tribalpower/entity/CreatureProfile.java').read_text(encoding='utf-8')
    for m in re.finditer(r'^\s*[A-Z_]+\("([a-z_]+)",.*?"([a-z_]+)", 0x', profile, re.M):
        ids.add(('entity', m.group(1))); ids.add(('item', m.group(2))); ids.add(('item', m.group(1) + '_spawn_egg'))
    role = (JAVA / 'tk/darrow/tribalpower/tribe/KinRole.java').read_text(encoding='utf-8')
    for m in re.finditer(r'^\s*[A-Z]+\("([a-z]+)"', role, re.M):
        ids.add(('item', 'tribal_kin_' + m.group(1) + '_spawn_egg'))
    return ids

def registered():
    found = {}
    for path in JAVA.rglob('*.java'):
        text = path.read_text(encoding='utf-8')
        for pattern, kind in PATTERNS:
            if kind == 'entity' and not re.search(r'(?<!Block)EntityType\.Builder', text): continue  # a BlockEntityType register
            for m in re.finditer(pattern, text, re.S):
                if m.group(1).endswith('_'): continue  # id completed at runtime (see dynamic())
                found.setdefault((kind, m.group(1)), path.relative_to(ROOT))
    return found

def main():
    data, dupes = load_lang()
    if dupes:
        print('DUPLICATE KEYS:', sorted(set(dupes))); sys.exit(1)
    found = registered()
    for key in dynamic(): found.setdefault(key, 'enum')
    def named(kind, ident):
        if f'{kind}.tribalpower.{ident}' in data: return True
        return kind == 'item' and f'block.tribalpower.{ident}' in data
    missing = [(k, i, str(p)) for (k, i), p in sorted(found.items()) if not named(k, i)]
    for k, i, p in missing: print(f'missing {k}.tribalpower.{i}  ({p})')
    if missing: sys.exit(1)
    print(f'PASS: {len(data)} lang keys, no duplicates, every registered id named.')

if __name__ == '__main__': main()

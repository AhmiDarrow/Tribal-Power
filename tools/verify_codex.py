"""Check generated teachings and every image consumed by the native reader."""
import json
from pathlib import Path
from generate_guide import CHAPTERS
ROOT=Path(__file__).resolve().parents[1]
rows=json.loads((ROOT/'tools/guide_catalog.json').read_text(encoding='utf-8'))
roster=json.loads((ROOT/'art/creatures/roster.json').read_text(encoding='utf-8'))
assert {r['title'] for r in rows}=={t for t,_ in CHAPTERS}|{r['name'] for r in roster}
assert len({r['id'] for r in rows})==len(rows)
assert not rows[0]['spoiler']
for row in rows:
    if row['picture']:
        assert row['spoiler'] or row.get('safe_picture'), row['id']
    if row.get('safe_picture'):
        assert row['picture'] and not row['spoiler'], row['id']
        image=ROOT/'src/main/resources/assets/tribalpower/textures/gui/codex'/f"{row['picture']}.png"
        assert image.read_bytes().startswith(b'\x89PNG\r\n\x1a\n'),image
screen=(ROOT/'src/main/java/tk/darrow/tribalpower/client/SpiritCodexScreen.java').read_text(encoding='utf-8')
assert 'super.render(g,mx,my,partial)' not in screen, 'Screen.render would blur already rendered content'
assert 'Button.DEFAULT_NARRATION' not in screen, 'Protected narration supplier must not be accessed externally'
assert 'if(!spoilers){confirmSpoilers(()->openRecipes(id));return;}' in screen
assert 'getRecipeManager().getRecipes()' in screen
assert any(r['category']=='Walkthroughs' for r in rows)
print(f'PASS: {len(rows)} teachings, {sum(1 for r in rows if r["picture"])} pictured, spoiler guard and live recipe source.')

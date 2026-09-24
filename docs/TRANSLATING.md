# Translating Tribal Power

Every line a player reads lives in one file: `src/main/resources/assets/tribalpower/lang/en_us.json` (about 2,500 keys). Nothing is hard-coded; `tools/verify_lang.py` fails the build if a registered block, item, entity or effect has no name, and the GameTests fail if a Codex entry points at a missing key.

## How to add a language

1. Copy `en_us.json` to `<language>_<region>.json` beside it (for example `de_de.json`, `pt_br.json`). Keep every key; translate only the values. Keys you leave out fall back to English in game.
2. `%s` and `%1$s` are placeholders and must stay, in the same order as English unless you use the numbered form.
3. Formatting codes are not used in this file; colour and style come from code, so a value is plain text. `**bold**`, `[text](entry_id)` and `{item:namespace:id}` in Codex text (`assets/tribalpower/codex/en_us/entries/**`) are Codex markup and must keep their brackets.
4. The Codex's own pages are per language too: copy `assets/tribalpower/codex/en_us/` to `codex/<language>_<region>/` and translate `name`, `title`, `description` and every `text` line. Entry ids, page types, links and item ids stay as they are.

## What the key families are

| Prefix | What it names |
|---|---|
| `block.`, `item.`, `entity.`, `effect.`, `biome.` | Registered things; `.desc` and `.hint` are their tooltips |
| `dialogue.tribalpower.<tribe>.*` | What each tribe's Elder says; `dialogue.tribalpower.choice.*` is what you can say back |
| `request.`, `questline.` | Requests and the seven-step stories |
| `lore.tribalpower.tablet.*`, `lore.tribalpower.chronicle.*`, `lore.tribalpower.spirit.*` | Lore tablets, the Chronicle's sixteen fragments, wandering spirits' lines |
| `message.` | Chat and action-bar messages |
| `gui.`, `screen.`, `jei.`, `jade.`, `emi.category.` | Screens and recipe viewers |
| `guide.tribalpower.*` | The Codex's "Next step" guidance |
| `subtitles.` | Sound subtitles |
| `painting.`, `block.minecraft.banner.tribalpower.*` | Paintings and the tribe crests on banners (one line per dye) |

## Voice

The mod is written the way the tribes speak: short sentences, plain words, a little dry. Elders are gruff or warm by tribe (the Pad-keepers warm, the Grit-singers dry, the Pattern-weavers exact); keep that in the target language rather than translating word for word. Tribe names are compound nouns (Pad-keepers, Grit-singers) and may be rendered as the language's natural equivalent.

## Checking your work

`python tools/verify_lang.py` checks `en_us.json` only. For another language, a quick check is `python -c "import json; json.load(open('src/main/resources/assets/tribalpower/lang/xx_xx.json', encoding='utf-8'))"` to prove the file parses, and then playing with the language selected; missing keys show as their key name in game.

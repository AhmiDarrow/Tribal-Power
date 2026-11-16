# Translating Tribal Power

Player-facing text lives in Minecraft's language files. The game's own language setting switches it. There is no language picker in the mod.

English (`en_us`) is the source of truth and the runtime fallback. A missing key shows the English line (Codex pages fall back file by file; config rows and song titles use a fallback string). A missing key does not crash and does not show a blank line. `tools/verify_locales.py` fails the build if a shipped locale drifts from English, so the files stay complete.

## Files

| Path | What it is |
|---|---|
| `src/main/resources/assets/tribalpower/lang/en_us.json` | Every lang key, in English |
| `src/main/resources/assets/tribalpower/lang/<locale>.json` | The same keys in another language |
| `src/main/resources/assets/tribalpower/codex/en_us/` | Spirit Codex pages, in English |
| `src/main/resources/assets/tribalpower/codex/<locale>/` | The same pages, prose translated |
| `src/main/resources/data/tribalpower/advancement/` | Advancement titles and descriptions, as `translate` keys |
| `tools/verify_locales.py` | Key set, JSON, placeholders, brands, Codex structure |

Shipped locales: `es_es`, `es_mx`, `fr_fr`, `de_de`, `pt_br`, `it_it`, `ru_ru`, `pl_pl`, `ja_jp`, `ko_kr`, `zh_cn`.

`es_mx` matches `es_es` on purpose. The Spanish is Latin-American neutral: *tú* and *ustedes*, never *vosotros*, and never *coger*.

## How a player switches language

Esc, Options, Language, and pick the language. Minecraft reloads language files on its own. Resource packs are not required.

## How to add or edit a language

1. Copy `en_us.json` to `<locale>.json` beside it. Keep every key. Translate only the values.
2. `%s` and `%1$s` are arguments. The translation must use the same argument numbers and the same types (`s`, `d`, `i`, `f`). Order may change when the numbered form is used. Do not mix numbered and unnumbered arguments. `%%` is an escaped percent and must occur the same number of times. A bare percent that is not a conversion, such as `60%` or `+40%`, is ordinary text. Keep the percent; do not turn it into `%%`.
3. These names stay exactly as written, wherever English has them: `Tribal Power`, `Remedy`, `Ninjacat`, `Chocobos Reborn`, and the line `Hope this helps`.
4. Copy `codex/en_us/` to `codex/<locale>/`. Translate only `text`, `title`, `name`, `caption`, `description` and `landing`. Leave ids, icons, item ids, coordinates, page types and link targets alone. Keep `**bold**`, `[words](entry_id)` and `{item:namespace:id}` markup. The words inside the brackets may be translated; the id in the parentheses may not.
5. Run `python tools/verify_locales.py`. Gradle runs it as `verifyLocales`, and `check` (and therefore `build`) depends on it.

## Key families

| Prefix | What it names |
|---|---|
| `block.`, `item.`, `entity.`, `effect.`, `biome.` | Registered things. `.desc` and `.hint` are tooltips |
| `tribalpower.configuration.` | Config screen labels. `.tooltip` is the comment under the control |
| `advancement.tribalpower.` | Advancement title and description |
| `songkeeper.tribalpower.` | Song titles and album names shown in the Songkeeper and in chat |
| `dialogue.tribalpower.` | What an elder says, and `dialogue.tribalpower.choice.*` for the replies |
| `request.`, `questline.` | Requests and the story steps |
| `lore.tribalpower.` | Lore tablets, chronicle fragments, wandering spirits |
| `message.` | Chat and the action bar |
| `gui.`, `screen.`, `jei.`, `jade.`, `emi.category.` | Screens and recipe viewers |
| `guide.tribalpower.*` | The Codex "next step" lines |
| `subtitles.` | Sound subtitles |
| `key.`, `key.categories.tribalpower` | Key bindings |
| `painting.`, `block.minecraft.banner.tribalpower.*` | Paintings and tribe crests on banners |

## Voice

Short sentences, plain words, a little dry. Elders stay gruff or warm by tribe (the Pad-keepers warm, the Grit-singers dry, the Pattern-weavers exact). Tribe names may be the language's natural equivalent. Do not translate the mod's name.

## Layout and fonts

The mod does not ship a font. Text uses Minecraft's font, which draws Japanese, Korean and Chinese. Codex pages and item hints wrap on the font's measured width. The Songkeeper shortens a title with an ellipsis once it no longer fits. Buttons that stay a fixed width hold a symbol or a digit, not a sentence.

## Checking

`python tools/verify_locales.py` checks every shipped locale. `python tools/verify_locales.py --lang-only` skips the Codex trees.

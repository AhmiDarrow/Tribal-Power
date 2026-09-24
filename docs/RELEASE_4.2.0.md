# Tribal Power 4.2.0 - Nine agreeing

The tribes talk now, and the March keeps guardians. This release gives every Elder a voice, every tribe a story that ends at a trial, every March biome a boss of its own, and one finale that ties the nine together.

## Tribes that talk

- **Elders speak.** Right-click a tribe's Elder and a conversation opens: what they say, and what you can say back. Trading is one of the things you can ask for. Strangers get a gruff word and a hint about the hearth; friends get work; those who finish the story get a warmer welcome. Every line is in the tribe's own voice, and every tree is data, so a pack can rewrite it.
- **Requests.** Ask an Elder for work and they hand you one of the tribe's requests: fetch what they prize, deliver their dish cooked, turn back the dark's creatures near their fires, perform a rite in their camp, or raise a small build with Builder's Chalk. Three are open a day and they change with the days. Each pays standing, and every third finished request for a tribe pays a **Tribe Mark**, so you no longer wait for Voice to hold one.
- **Nine stories.** Ask an Elder to tell you their story and a seven-step road begins: become their Friend, bring the one thing they prize, hunt near their fires, sing a rite in their camp, cook their dish, and go where they send you. The last step is a **trial** against a guardian.
- **Nine relics.** Finish a story and the Elder hands you the tribe's relic: Kept Ember, Ringing Stone, First Root, Bridge Nail, Steady Drum, Silent Gear, Hive Seal, Polite Seal, Ninth Thread. Carried anywhere in your pack, a relic keeps that tribe's boon on you whatever your standing.
- **The Codex keeps track.** Every tribe's page ends with the request they gave you, how far along it is, and where their story stands; a new chapter explains the Elders, the requests and the stories.

## The guardians

- **Eight guardians**, one per March biome, each the end of a tribe's story: the **Slag Titan** in the Ember Wastes, the **Bog Matriarch** in the Reed Fen, the **Cairn Wight** in the Snow Fields, the **Prism Serpent** in the Crystal Fields, the **Vault Sentinel** on Glimmer Ridge, the **Tide Drummer** in the Shallows, the **Stampede Spirit** on the Steppe and the **Storm Roc** on the Highlands. The Loom-stitchers' story ends at The Unsung.
- **Guardian grounds.** Each biome now generates a ring of its own stone with a **Guardian Altar** at its centre. A guardian rises only from its altar: on polished deepslate, between four lit candles, with open air above, and it rests twenty minutes between calls. Sneak-use the altar with an empty hand and it tells you what it wants: a fistful of that biome's reagent.
- **Two phases.** Every guardian has a signature attack (slams, snares, frost breath, light bolts, sweeps, tides, charges, swoops) that sharpens at half health, when it also calls its biome's creatures to its side. Those creatures drop nothing and vanish when it falls. Leave it alone half a minute and it goes back to sleep.
- **Bosses in every sense:** a boss bar, no knockback, no potion effects, no leash, no Soul Urn, no rest while one stands. When a guardian falls, everyone who fought it finishes that tribe's trial, and it drops its **core** with a heap of its biome's reagents. A core and a Loom Thread make six Resonant Cores.
- **The Unsung** keeps the same standards now: its Echo Weavers drop nothing, it needs open sky above the drum to rise, and its fall is the Loom-stitchers' trial.

## The Ninth Agreement

- **One finale.** Craft the tablet from the eight cores and an Unsung Heart, draw a Loom circle, and perform the rite with all nine relics on you. Nine lights rise, one in each tribe's colour, and the Loom agrees again.
- **What changes:** every tribe treats the one who made the agreement as Kin at the least, all nine boons settle on them for a while, and the March's aurora burns full and steady every night they stand under it. The world remembers the agreement for good.

## Configuration

- New `[quests]` and `[guardians]` sections: request standing, requests per Mark, whether Elders talk; guardian health and damage scales, the call's cost, the altar's rest, attack and wave timings, add caps, sleep time, and the last rite's Pulse cost and boon length.

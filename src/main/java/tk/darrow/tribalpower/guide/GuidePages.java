package tk.darrow.tribalpower.guide;

import java.util.List;

/**
 * Spirit Codex chapters — keep aligned with live Song Bench / Echo / rites / Spiritgear.
 */
public final class GuidePages {
    private GuidePages() {}

    public static List<String> allPages() {
        return List.of(
                "SPIRIT CODEX\n\nShamanic Technomancy\n\nAlong the Loom, steward-tribes learned to braid spirit and craft without furnace vanity. Tribal Power is their craft: Pulse, totems, and Echo.",
                "LOOM-BORN ORIGIN\n\nSteward-tribes of the Loom kept rhythm as law. They drummed Spirit Pulse into stone, raised Resonance Totems as attuned posts, and walked ore through Echo stages until metal remembered the tribe. No outside pantheon — only drum, chalk, and honest work.",
                "WHAT YOU BUILD\n\nDrumheart and Ley for Pulse. Totem ring and Song Bench for songs. Echo shrine markers for camp layout. Pedestal for seals. Pulse Cells for Spiritgear. Gate Drum for The March.",
                "SPIRIT PULSE\n\nSpirit Pulse (SP) is measured in beats. Machines implement Pulse handlers. Early power comes from the Drumheart — strike it, or feed it redstone tempo. Ley Collectors sip ambient Pulse for steadier songs.",
                "READING PULSE\n\nDrumheart and Ley chat feedback show buffer fill. Song Bench status reports Pulse stalls. Nothing here speaks FE or RF natively.",
                "DRUMHEART\n\nEmpty-hand right-click to drum beats into the buffer. Redstone edges also add beats. Idle placement trickles a little Pulse. Keep one near the Song Bench.",
                "CHARGING CELLS\n\nHold a Pulse Cell and use it on a Drumheart. Up to 25 Pulse transfers per click. Cells hold 200, stack to one, and show a cyan fill bar.",
                "PULSE CELLS\n\nPortable Pulse buffers. Spiritgear scans offhand, mainhand, then inventory. Gate Drums and Deep Caches can also drink cell charge when rites demand it.",
                "LEY COLLECTOR\n\nGathers ambient Pulse (~2 every 40 ticks). Right-click to read its buffer. Song Bench drinks from Drumhearts and Ley Collectors first, then Totem buffers.",
                "TOTEM LATTICE\n\nResonance Totems hold Attunements: Earth, Fire, Water, Air, Spirit. Place the stage's totem within 8 blocks of the Song Bench. Ritual Chalk seals lasting links out to 16 blocks.",
                "ATTUNEMENTS\n\nEach totem answers one voice. Match Echo stages to nearby attunements. Wrong harmonic stalls the song without wasting grit.",
                "RITUAL CHALK\n\nRight-click one Resonance Totem to mark it, then another within 16 blocks to seal a bidirectional lattice link. Linked attunements count even a little farther out.",
                "LATTICE CONDUCTOR\n\nCopper-bone scaffolding for longer songs. Helps the lattice feel continuous between distant posts and echo shrines.",
                "SONG BENCH\n\nLive Echo refinement. Right-click with raw ore, cobble, iron-like metal, or an Echo intermediate to seat one item. Empty-hand click starts the song. Shift-click removes the item.",
                "SONG STATUS\n\nStatus text reports stalls: missing Pulse, no totems in range, or wrong attunement for the current Echo stage.",
                "ECHO LOOP\n\nAt the Song Bench:\n1 Shatter (Earth) → Echo Shard\n2 Attune (Fire) → Attuned Echo\n3 Bind (Water) → Bound Echo\n4 Manifest (Spirit) → Manifested Ingot\n\nEach stage costs Pulse every tick and needs its totem.",
                "SHATTER\n\nFeeds: raw iron/gold/copper, their ores, ingots, iron blocks, cobble/stone, March cobble/ore/stone. Earth totem required. Output: Echo Shard.",
                "ATTUNE\n\nEcho Shard → Attuned Echo under Fire. Wrong attunement stalls without wasting the grit.",
                "BIND\n\nAttuned Echo → Bound Echo under Water. Binding drinks Pulse from the lattice every tick.",
                "MANIFEST\n\nBound Echo → Manifested Ingot under Spirit — Loom-born metal for Spiritgear and seals. The song stops when Manifest completes.",
                "ECHO STATIONS\n\nEcho Shatter / Attune / Bind / Manifest blocks are camp shrine markers. They do not process ores. All live refinement runs on the Song Bench.",
                "ANCESTRAL CACHE\n\nLocal storage bound to your camp. Opens like a chest. Keep reagents, seals, and Pulse Cells close to the lattice.",
                "DEEP CACHE\n\nBulk storage linked through The March. Visiting The March marks a spirit link; Pulse in a cell can form a temporary link when needed.",
                "SEALS & RITES\n\nCraft Blank Seals, then attune them. On a Rite Pedestal, offer a finished seal. The seal is consumed; the spirit working remains.",
                "EARTH RITE\n\nResistance, absorption, haste. Roots nearby hostiles. Firms soft ground into paths under the pedestal.",
                "FIRE RITE\n\nFire resistance. Ignites nearby hostiles. Melts snow/ice and lights cold campfires in range.",
                "WATER RITE\n\nWater breathing and dolphin's grace. Extinguishes fires, moistens farmland, and may coax crops when amplified.",
                "AIR RITE\n\nSlow falling, speed, jump. Gusts shove hostiles outward. Clears rain when the sky is heavy.",
                "SPIRIT RITE\n\nRegeneration, night vision, cleanse. Heals nearby players, weakens and reveals hostiles. Amplified rites can refund Pulse into a cell.",
                "AMPLIFIED RITES\n\nIf you carry at least 8 Pulse in cells, the pedestal drinks it to deepen the working — longer buffs, stronger area effects.",
                "SPIRITGEAR\n\nPickaxe, axe, shovel, and blade fueled by Pulse Cells. Fueled swings spare durability; starved swings wear the tool twice as hard.",
                "SPIRITGEAR COSTS\n\nMining costs 2 Pulse per block. Stripping or pathing costs 1. Blade hits cost 3 and, when fueled, deal bonus magic damage and brief Glow.",
                "PULSE ATTUNEMENT\n\nEnchantment for mining tools / Spiritgear. Raises mining efficiency like a spirit-tuned Efficiency.",
                "ECHO EDGE\n\nWeapon enchantment. Extra strike damage plus Glow on direct hits.",
                "GATE DRUM\n\nCharge with Pulse (sneak-drum or Pulse Cell), then strike to cross into The March. Use it again to return.",
                "THE MARCH\n\nDimension tribalpower:the_march. March Stone, soil, grass, cobble, logs, leaves, Spirit Reeds, Echo Blooms. Spirit Wisps and March Walkers dwell there.",
                "MARCH MATERIALS\n\nBring March blocks home for builds that remember the otherworld. Deep Cache logic leans on March spirit links.",
                "CRAFTING NOTES\n\nBone Chimes, Copper Resonators, and Spirit Shards unlock Drumhearts, Song Benches, Totems, and Ley Collectors. Echo intermediates come from the Song Bench loop, not crafting tables.",
                "CAMP LAYOUT\n\nDrumheart and Ley near Song Bench. Totems in a ring by attunement. Echo shrines in Shatter→Manifest order. Pedestal and caches at the hearth. Gate Drum at the edge.",
                "SAFETY\n\nPulse is not Redstone Flux. Do not expect FE cables to feed a Song Bench. Native rhythm remains Pulse.",
                "CREDITS\n\nTribal Power\nAhmi & Risika Darrow\n\nShamanic Technomancy for steward-tribes of the Loom. Keep the drum honest."
        );
    }
}

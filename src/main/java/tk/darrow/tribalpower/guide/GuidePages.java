package tk.darrow.tribalpower.guide;

import java.util.List;

/**
 * Extensive Spirit Codex chapters describing shamanic technomancy.
 * Keep these aligned with live Song Bench / EchoStage behavior.
 */
public final class GuidePages {
    private GuidePages() {}

    public static List<String> allPages() {
        return List.of(
                "SPIRIT CODEX\n\nShamanic Technomancy\n\nAlong the Loom, steward-tribes learned to braid spirit and craft without furnace vanity. Tribal Power is their craft: Pulse, totems, and Echo.",
                "LOOM-BORN ORIGIN\n\nSteward-tribes of the Loom kept rhythm as law. They drummed Spirit Pulse into stone, raised Resonance Totems as attuned posts, and walked ore through Echo stages until metal remembered the tribe. This is not anime pageantry and needs no outside pantheon — only drum, chalk, and honest work.",
                "SPIRIT PULSE\n\nSpirit Pulse (SP) is measured in beats. Machines implement Pulse handlers. Early power comes from the Drumheart — strike it, or feed it redstone tempo. Ley Collectors sip ambient Pulse for steadier songs.",
                "DRUMHEART\n\nPlace a Drumheart and right-click to drum. Each beat stores Pulse. Redstone edges also add beats. Keep one near the Song Bench so Shatter and Manifest never starve.",
                "LEY COLLECTOR\n\nThe Ley Collector gathers ambient Pulse (~2 every 40 ticks). Right-click to read its buffer. Song Bench drinks from Drumhearts and Ley Collectors first, then Totem buffers.",
                "TOTEM LATTICE\n\nResonance Totems hold Attunements: Earth, Fire, Water, Air, Spirit. Place the stage's totem within 8 blocks of the Song Bench. Ritual Chalk seals lasting links out to 16 blocks.",
                "RITUAL CHALK\n\nRight-click one Resonance Totem to mark it, then another within 16 blocks to seal a bidirectional lattice link. Linked attunements count for Echo work even a little farther out.",
                "SONG BENCH\n\nLive Echo refinement. Right-click with raw ore, cobble, iron-like metal, or an Echo intermediate to seat one item. Empty-hand click starts the song. Shift-click removes the item. Status text reports stalls (Pulse, totems, wrong attunement).",
                "ECHO LOOP\n\nOre is not smelted in a magic furnace. At the Song Bench:\n1 Shatter (Earth) → Echo Shard\n2 Attune (Fire) → Attuned Echo\n3 Bind (Water) → Bound Echo\n4 Manifest (Spirit) → Manifested Ingot\n\nEach stage costs Pulse every tick and needs its totem. The song can chain stages if attunements stay present.",
                "SHATTER\n\nFeeds: raw iron/gold/copper, their ores, ingots, iron blocks, cobble/stone, March cobble/ore/stone. Earth totem required. Output: Echo Shard.",
                "ATTUNE\n\nEcho Shard → Attuned Echo under Fire. Wrong attunement stalls the song without wasting the grit.",
                "BIND\n\nAttuned Echo → Bound Echo under Water. Binding drinks Pulse from the lattice every tick.",
                "MANIFEST\n\nBound Echo → Manifested Ingot under Spirit — Loom-born metal for Spiritgear and seals. The song stops when Manifest completes.",
                "ECHO STATIONS\n\nEcho Shatter / Attune / Bind / Manifest blocks are camp shrine markers. They do not process ores. All live refinement runs on the Song Bench.",
                "ANCESTRAL CACHE\n\nLocal storage bound to your camp. Opens like a chest. Keep reagents, seals, and pulse cells close to the lattice.",
                "DEEP CACHE\n\nBulk storage linked through The March. Visiting The March marks a spirit link; authorized players open a dimension-backed vault.",
                "SEALS & RITES\n\nCraft Blank Seals, then attune them. On a Rite Pedestal, offer a seal to receive a short spirit boon — earth skin, fire ward, water breath, soft fall, or spirit sight.",
                "SPIRITGEAR\n\nTools that lean on Pulse and seals. Pulse Cells in inventory feed Spiritgear strain so the edge lasts longer.",
                "GATE DRUM\n\nCharge the Gate Drum with Pulse (or a Pulse Cell), then strike it to cross into The March. Use it again to return.",
                "THE MARCH\n\nDimension tribalpower:the_march. March Stone, cobble, logs, leaves, Spirit Reeds, and Echo Blooms define its palette. Spirit Wisps and March Walkers dwell there.",
                "CRAFTING NOTES\n\nBone Chimes, Copper Resonators, and Spirit Shards unlock Drumhearts, Song Benches, Totems, and Ley Collectors. Echo intermediates come from the Song Bench loop, not crafting tables.",
                "SAFETY\n\nPulse is not Redstone Flux. Do not expect FE cables to feed a Song Bench. Native rhythm remains Pulse.",
                "CREDITS\n\nTribal Power\nAhmi & Risika Darrow\n\nShamanic Technomancy for steward-tribes of the Loom. Keep the drum honest."
        );
    }
}

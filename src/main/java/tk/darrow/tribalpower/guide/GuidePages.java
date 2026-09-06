package tk.darrow.tribalpower.guide;

import java.util.List;

/**
 * Extensive Spirit Codex chapters describing shamanic technomancy.
 */
public final class GuidePages {
    private GuidePages() {}

    public static List<String> allPages() {
        return List.of(
                "SPIRIT CODEX\n\nShamanic Technomancy\n\nTribal Power binds spirit and machine. Power is not furnace heat — it is Pulse: rhythmic beats that travel totem lattices.",
                "SPIRIT PULSE\n\nSpirit Pulse (SP) is measured in beats. Machines implement Pulse handlers. Early power comes from the Drumheart — strike it, or feed it redstone tempo.",
                "DRUMHEART\n\nPlace a Drumheart and right-click to drum. Each beat stores Pulse. Redstone edges also add beats. Later, Ley Collectors gather ambient Pulse from the land.",
                "TOTEM LATTICE\n\nResonance Totems hold Attunements: Earth, Fire, Water, Air, Spirit. Place them near a Song Bench. The bench starts a song that routes materials along harmonics.",
                "SONG BENCH\n\nThe Song Bench (or Lattice Conductor) begins a lattice song. Linked totems within range answer. Full chalk-line linking and item routing arrive in later rites.",
                "ECHO STAGES\n\nOre is not smelted in a magic furnace. It walks Echo Stages:\n1 Shatter\n2 Attune\n3 Bind\n4 Manifest\n\nEach stage is a station on the lattice path.",
                "SHATTER\n\nBreak raw ore into Shattered Ore at an Echo Shatter block. Fragments open to spirit pressure — the first harmonic step.",
                "ATTUNE\n\nAttuned Ore takes on an elemental voice matching nearby totems. Wrong attunement wastes the song; match the lattice.",
                "BIND\n\nBound Ore locks the chosen harmonic. Binding costs Pulse from the lattice. Without Pulse, the song stalls.",
                "MANIFEST\n\nManifest Ingot is the finished voice of the ore — metal remembered by spirit. Use it for Spiritgear and seals.",
                "ANCESTRAL CACHE\n\nLocal storage bound to your camp. Opens like a chest. Keep reagents, seals, and pulse cells close to the lattice.",
                "DEEP CACHE\n\nBulk storage linked through The March. Spirit links reach a dimension-backed vault. UI and persistence expand later.",
                "SEALS & RITES\n\nCraft Blank Seals, then attune them. On a Rite Pedestal, offer a seal to receive a short spirit boon — earth skin, fire ward, water breath, soft fall, or spirit sight.",
                "SPIRITGEAR\n\nTools and armor that lean on Pulse and seals. The Spiritgear Pickaxe already wears with spirit strain; full Pulse drain wiring comes next.",
                "GATE DRUM\n\nStrike the Gate Drum to cross into The March — a shamanic otherworld of teal stone, pale woods, and quiet walkers. Use it again to return.",
                "THE MARCH\n\nDimension tribalpower:the_march. March Stone, cobble, logs, leaves, and Spirit Reeds define its palette. Spirit Wisps and March Walkers dwell there.",
                "LEY & MID GAME\n\nLey Collectors sip ambient Pulse. Pair them with lattices for steady songs without constant drumming.",
                "RITUAL CHALK\n\nChalk marks planned links between totems. Line-of-sight still works for crude songs; chalk makes lasting paths.",
                "PULSE CELLS\n\nPortable Pulse buffers for Spiritgear and portable rites. Fill them at a Drumheart or Ley Collector.",
                "COPPER RESONATORS\n\nConductive bones of the lattice. Craft conductors, benches, and echo frames from resonators and bone chimes.",
                "SAFETY\n\nPulse is not Redstone Flux. Do not expect FE cables to feed a Song Bench. A future bridge block may translate — native rhythm remains Pulse.",
                "CREDITS\n\nTribal Power\nAhmi & Risika Darrow\n\nShamanic Technomancy rewrite for modern Minecraft. Keep the drum honest."
        );
    }
}

package tk.darrow.tribalpower.pattern;

import net.minecraft.tags.BlockTags;
import tk.darrow.tribalpower.block.ModBlocks;
import tk.darrow.tribalpower.gate.GateRegistry;

/**
 * Every placement rite in the mod (design 3.1 sections 5 to 8).
 *
 * <p>Written as static finals: the predicates hold block suppliers, so nothing here resolves a
 * registry object until a pattern is first matched against the world.
 */
public final class ModPatterns {
    private ModPatterns() {}

    /**
     * The Stone Font, and the first pattern anyone builds (section 6). Tier 1 is a font and four chalk
     * marks; tier 2 braces the corners with anchor stones. Which stone the font can ask for beyond that
     * is a question of voices, not of shape, so the obsidian tier is not a third layout.
     */
    public static final RitualPattern STONE_FONT = RitualPattern.builder("stone_font")
            .layer(0,
                    ".m.",
                    "mFm",
                    ".m.")
            .where('F', Predicates.anchor(ModBlocks.STONE_FONT))
            .where('m', Predicates.ritualMark())
            .where('.', Predicates.anything())
            .tier(2)
            .layer(0,
                    "A...A",
                    "..m..",
                    ".mFm.",
                    "..m..",
                    "A...A")
            .where('A', Predicates.block(ModBlocks.ANCHOR_STONE))
            .build();

    /**
     * The Listening Pit (section 7.2). Tier 1 is the 5x5 written in the design; tier 2 is the 7x7 Deep
     * Listening, which doubles the corner bracing and seats a totem at each cardinal edge.
     *
     * <p>Design note: the design text gives tier 2 both eight anchor stones "corners + edge midpoints"
     * and totems in "the four cardinal edge slots", which cannot both hold the same four cells. Resolved
     * here as eight anchors on the two corner rings and the totems on the true cardinal midpoints, which
     * keeps both counts and leaves the shape symmetrical.
     */
    public static final RitualPattern LISTENING_PIT = RitualPattern.builder("listening_pit")
            .layer(-1,
                    "SSSSS",
                    "SSSSS",
                    "SSSSS",
                    "SSSSS",
                    "SSSSS")
            .layer(0,
                    "A.m.A",
                    "..m..",
                    "mmMmm",
                    "..m..",
                    "A.m.A")
            .where('S', Predicates.tag(BlockTags.BASE_STONE_OVERWORLD))
            .where('A', Predicates.block(ModBlocks.ANCHOR_STONE))
            .where('m', Predicates.ritualMark())
            .where('M', Predicates.anchor(ModBlocks.RESONANCE_MESH))
            .where('.', Predicates.anything())
            .tier(2)
            .layer(-1,
                    "SSSSSSS",
                    "SSSSSSS",
                    "SSSSSSS",
                    "SSSSSSS",
                    "SSSSSSS",
                    "SSSSSSS",
                    "SSSSSSS")
            .layer(0,
                    "A..T..A",
                    ".A.m.A.",
                    "...m...",
                    "TmmMmmT",
                    "...m...",
                    ".A.m.A.",
                    "A..T..A")
            .where('T', Predicates.anyTotem())
            .build();

    /**
     * The Rite Circle (section 5): a brazier, four Rite Pedestals on the diagonals, chalk joining them.
     * Tier 2 seats four totems on the cardinals; whether they match the seated seal is a question for the
     * rite, not the shape, so the cells only ask for totems.
     */
    public static final RitualPattern RITE_CIRCLE = RitualPattern.builder("rite_circle")
            .layer(0,
                    "P...P",
                    ".m.m.",
                    "..B..",
                    ".m.m.",
                    "P...P")
            .where('B', Predicates.anchor(ModBlocks.RITUAL_BRAZIER))
            .where('P', Predicates.block(ModBlocks.RITE_PEDESTAL))
            .where('m', Predicates.ritualMark())
            .where('.', Predicates.anything())
            .tier(2)
            .layer(0,
                    "P.T.P",
                    ".m.m.",
                    "T.B.T",
                    ".m.m.",
                    "P.T.P")
            .where('T', Predicates.anyTotem())
            .build();

    /**
     * The Voice Ring (section 5): totems at radius 3 around a Pulse Resonator, on its own level. Eight
     * slots rather than six, because six evenly spaced totems do not exist on a square grid -- six-voice
     * resonance then means six distinct voices among the eight arranged totems.
     */
    public static final RitualPattern VOICE_RING = RitualPattern.builder("voice_ring")
            .layer(0,
                    "T..T..T",
                    ".......",
                    ".......",
                    "T..R..T",
                    ".......",
                    ".......",
                    "T..T..T")
            .where('R', Predicates.anchor(ModBlocks.PULSE_RESONATOR))
            .where('T', Predicates.anyTotem())
            .where('.', Predicates.anything())
            .build();

    /**
     * The Shatter Array (section 5): an Echo station centred in a 5x5 with four totems at the corners and
     * an Ancestral Cache beneath. Whether the totems match the recipe's attunement is checked by the
     * station, which already knows which recipe it is running.
     */
    public static final RitualPattern SHATTER_ARRAY = RitualPattern.builder("shatter_array")
            .layer(-1,
                    ".....",
                    ".....",
                    "..C..",
                    ".....",
                    ".....")
            .layer(0,
                    "T...T",
                    ".....",
                    "..S..",
                    ".....",
                    "T...T")
            .where('S', Predicates.anchor(ModBlocks.ECHO_SHATTER))
            .where('T', Predicates.anyTotem())
            .where('C', Predicates.block(ModBlocks.ANCESTRAL_CACHE))
            .where('.', Predicates.anything())
            .build();

    /**
     * The Way Gate (section 8): a 5x5 frame ring without its corners -- twelve cells, one of which the
     * keystone replaces -- around a 3x3 interior. Written on one axis; the matcher's four rotations cover
     * the other.
     */
    public static final RitualPattern WAY_GATE = RitualPattern.builder("way_gate")
            .layer(0, ".FKF.")
            .layer(1, "FiiiF")
            .layer(2, "FiiiF")
            .layer(3, "FiiiF")
            .layer(4, ".FFF.")
            .where('K', Predicates.anchor(GateRegistry.GATE_KEYSTONE))
            .where('F', Predicates.block(GateRegistry.GATE_FRAME))
            .where('i', Predicates.airOr(GateRegistry.GATE_PORTAL))
            .where('.', Predicates.anything())
            .build();

    /**
     * The Far Gate (section 8): a taller frame around a 3x4 interior, braced by four anchor stones at the
     * corners of its footprint. The Loom voice is checked by the keystone, not by the shape.
     */
    public static final RitualPattern FAR_GATE = RitualPattern.builder("far_gate")
            .layer(0,
                    "A...A",
                    ".FKF.",
                    "A...A")
            .layer(1,
                    ".....",
                    "FiiiF",
                    ".....")
            .layer(2,
                    ".....",
                    "FiiiF",
                    ".....")
            .layer(3,
                    ".....",
                    "FiiiF",
                    ".....")
            .layer(4,
                    ".....",
                    "FiiiF",
                    ".....")
            .layer(5,
                    ".....",
                    ".FFF.",
                    ".....")
            .where('K', Predicates.anchor(GateRegistry.GATE_KEYSTONE))
            .where('F', Predicates.block(GateRegistry.GATE_FRAME))
            .where('A', Predicates.block(ModBlocks.ANCHOR_STONE))
            .where('i', Predicates.airOr(GateRegistry.GATE_PORTAL))
            .where('.', Predicates.anything())
            .build();
}

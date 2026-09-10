package tk.darrow.tribalpower.pattern;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A placement rite: layers of characters, one predicate per character, one anchor cell marking the
 * block entity's own position (design 3.1 section 4).
 *
 * <p>Written bottom layer first; each layer's rows run north to south and each row's characters run
 * west to east, so the written shape reads as it looks from above. Layer indices are relative to the
 * anchor's own layer.
 *
 * <p>Tiers are complete alternative shapes rather than additions, because the real content changes
 * footprint between tiers (the Listening Pit goes 5x5 to 7x7). A tier declared after another inherits
 * its predicates, so only the changed keys need restating. Matching tries the highest tier first.
 */
public final class RitualPattern {
    private final String id;
    private final List<Tier> tiers;

    private RitualPattern(String id, List<Tier> tiers) {
        this.id = id;
        this.tiers = tiers;
    }

    public String id() { return id; }

    /** Translation key for the pattern's own name, e.g. {@code pattern.tribalpower.listening_pit}. */
    public String translationKey() { return "pattern.tribalpower." + id; }

    public Component displayName() { return Component.translatable(translationKey()); }

    /** Tiers from highest to lowest -- the order matching walks them. */
    public List<Tier> tiers() { return tiers; }

    public Tier tier(int number) {
        for (Tier tier : tiers) if (tier.number() == number) return tier;
        return null;
    }

    public int maxTier() { return tiers.getFirst().number(); }

    /** The largest bounding box any tier occupies, used to decide whether a neighbour change matters. */
    public BoundingBox boundsAround(BlockPos anchor) {
        BoundingBox box = tiers.getFirst().relativeBounds();
        for (Tier tier : tiers) box = encompass(box, tier.relativeBounds());
        // Rotation swaps X and Z, so the invalidation box has to cover both orientations.
        int horizontal = Math.max(
                Math.max(Math.abs(box.minX()), Math.abs(box.maxX())),
                Math.max(Math.abs(box.minZ()), Math.abs(box.maxZ())));
        return new BoundingBox(
                anchor.getX() - horizontal, anchor.getY() + box.minY(), anchor.getZ() - horizontal,
                anchor.getX() + horizontal, anchor.getY() + box.maxY(), anchor.getZ() + horizontal);
    }

    private static BoundingBox encompass(BoundingBox a, BoundingBox b) {
        return new BoundingBox(
                Math.min(a.minX(), b.minX()), Math.min(a.minY(), b.minY()), Math.min(a.minZ(), b.minZ()),
                Math.max(a.maxX(), b.maxX()), Math.max(a.maxY(), b.maxY()), Math.max(a.maxZ(), b.maxZ()));
    }

    /** One cell of one tier, as an offset from the anchor. */
    public record Cell(Vec3i offset, BlockPredicate predicate) {
        /** This cell's world position for a given anchor and rotation. */
        public BlockPos at(BlockPos anchor, Rotation rotation) {
            int x = offset.getX();
            int z = offset.getZ();
            int rx = switch (rotation) {
                case NONE -> x;
                case CLOCKWISE_90 -> -z;
                case CLOCKWISE_180 -> -x;
                case COUNTERCLOCKWISE_90 -> z;
            };
            int rz = switch (rotation) {
                case NONE -> z;
                case CLOCKWISE_90 -> x;
                case CLOCKWISE_180 -> -z;
                case COUNTERCLOCKWISE_90 -> -x;
            };
            return anchor.offset(rx, offset.getY(), rz);
        }
    }

    /** One complete shape. {@code cells} excludes the anchor itself; the anchor is checked separately. */
    public record Tier(int number, List<Cell> cells, Cell anchor, BoundingBox relativeBounds) {}

    public static Builder builder(String id) { return new Builder(id); }

    public static final class Builder {
        private final String id;
        private final List<TierDraft> drafts = new ArrayList<>();
        private TierDraft current;

        private Builder(String id) {
            this.id = id;
            tier(1);
        }

        /** Opens a tier. Its predicates start as a copy of the previous tier's, so only changes need restating. */
        public Builder tier(int number) {
            TierDraft draft = new TierDraft(number);
            if (current != null) draft.keys.putAll(current.keys);
            drafts.add(draft);
            current = draft;
            return this;
        }

        /** One horizontal course, at {@code y} relative to the anchor's layer. */
        public Builder layer(int y, String... rows) {
            current.layers.put(y, rows);
            return this;
        }

        public Builder where(char key, BlockPredicate predicate) {
            current.keys.put(key, predicate);
            return this;
        }

        public RitualPattern build() {
            List<Tier> built = new ArrayList<>(drafts.size());
            for (TierDraft draft : drafts) built.add(draft.build(id));
            // Highest tier first: matching wants the best shape a build satisfies.
            built.sort((a, b) -> Integer.compare(b.number(), a.number()));
            return new RitualPattern(id, List.copyOf(built));
        }
    }

    private static final class TierDraft {
        final int number;
        final Map<Integer, String[]> layers = new LinkedHashMap<>();
        final Map<Character, BlockPredicate> keys = new HashMap<>();

        TierDraft(int number) { this.number = number; }

        Tier build(String id) {
            List<Cell> cells = new ArrayList<>();
            Cell anchorCell = null;
            int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
            int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
            for (Map.Entry<Integer, String[]> layer : layers.entrySet()) {
                int y = layer.getKey();
                String[] rows = layer.getValue();
                for (int z = 0; z < rows.length; z++) {
                    String row = rows[z];
                    for (int x = 0; x < row.length(); x++) {
                        BlockPredicate predicate = keys.get(row.charAt(x));
                        if (predicate == null)
                            throw new IllegalStateException("Pattern " + id + " tier " + number
                                    + " uses " + row.charAt(x) + " with no where() clause");
                        Cell cell = new Cell(new Vec3i(x, y, z), predicate);
                        if (predicate instanceof Predicates.Anchor) {
                            if (anchorCell != null)
                                throw new IllegalStateException("Pattern " + id + " tier " + number + " has more than one anchor");
                            anchorCell = cell;
                        } else {
                            cells.add(cell);
                        }
                        minX = Math.min(minX, x); maxX = Math.max(maxX, x);
                        minY = Math.min(minY, y); maxY = Math.max(maxY, y);
                        minZ = Math.min(minZ, z); maxZ = Math.max(maxZ, z);
                    }
                }
            }
            if (anchorCell == null)
                throw new IllegalStateException("Pattern " + id + " tier " + number + " has no anchor cell");
            // Re-origin every cell on the anchor: written coordinates are grid indices, stored ones are offsets.
            Vec3i origin = anchorCell.offset();
            List<Cell> relative = new ArrayList<>(cells.size());
            for (Cell cell : cells) relative.add(new Cell(cell.offset().subtract(origin), cell.predicate()));
            BoundingBox bounds = new BoundingBox(
                    minX - origin.getX(), minY - origin.getY(), minZ - origin.getZ(),
                    maxX - origin.getX(), maxY - origin.getY(), maxZ - origin.getZ());
            return new Tier(number, List.copyOf(relative), new Cell(Vec3i.ZERO, anchorCell.predicate()), bounds);
        }
    }
}

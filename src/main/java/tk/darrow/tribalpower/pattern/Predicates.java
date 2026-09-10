package tk.darrow.tribalpower.pattern;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import tk.darrow.tribalpower.api.pulse.Attunement;
import tk.darrow.tribalpower.block.ResonanceTotemBlock;
import tk.darrow.tribalpower.blockentity.ResonanceTotemBlockEntity;

import java.util.function.Supplier;

/**
 * The cell predicates a {@link RitualPattern} can be written with (design 3.1 §4).
 * Blocks are taken as suppliers so patterns can be static finals declared before registration runs.
 */
public final class Predicates {
    private Predicates() {}

    /** Marks the cell the block entity itself occupies. Exactly one cell of a tier must be an anchor. */
    public static BlockPredicate anchor(Supplier<? extends Block> block) {
        return new Anchor(block);
    }

    public static BlockPredicate block(Supplier<? extends Block> block) {
        return new Simple(
                (level, pos) -> level.getBlockState(pos).is(block.get()),
                () -> block.get().getName());
    }

    public static BlockPredicate tag(TagKey<Block> tag) {
        return new Simple(
                (level, pos) -> level.getBlockState(pos).is(tag),
                () -> Component.translatable("pattern.tribalpower.expect.tag",
                        Component.translatable(net.minecraft.Util.makeDescriptionId("tag", tag.location()))));
    }

    public static BlockPredicate anything() {
        return ANYTHING;
    }

    public static BlockPredicate air() {
        return new Simple(
                (level, pos) -> level.getBlockState(pos).isAir(),
                () -> Component.translatable("pattern.tribalpower.expect.air"));
    }

    public static BlockPredicate solid() {
        return new Simple(
                (level, pos) -> {
                    BlockState state = level.getBlockState(pos);
                    return !state.isAir() && state.isSolidRender(level, pos);
                },
                () -> Component.translatable("pattern.tribalpower.expect.solid"));
    }

    public static BlockPredicate container() {
        return new Simple(
                (level, pos) -> {
                    BlockEntity be = level.getBlockEntity(pos);
                    return be instanceof net.minecraft.world.Container;
                },
                () -> Component.translatable("pattern.tribalpower.expect.container"));
    }

    /** A Resonance Totem carrying one particular voice. */
    public static BlockPredicate totem(Attunement voice) {
        return new Simple(
                (level, pos) -> level.getBlockEntity(pos) instanceof ResonanceTotemBlockEntity totem
                        && totem.getAttunement() == voice,
                () -> Component.translatable("pattern.tribalpower.expect.totem",
                        Component.translatable("attunement.tribalpower." + voice.getSerializedName())));
    }

    /** Any Resonance Totem, whatever its voice. */
    public static BlockPredicate anyTotem() {
        return new Simple(
                (level, pos) -> level.getBlockState(pos).getBlock() instanceof ResonanceTotemBlock,
                () -> Component.translatable("pattern.tribalpower.expect.any_totem"));
    }

    /** Air, or the block itself -- how a portal frame's interior is written before it is lit. */
    public static BlockPredicate airOr(Supplier<? extends Block> block) {
        return new Simple(
                (level, pos) -> {
                    BlockState state = level.getBlockState(pos);
                    return state.isAir() || state.is(block.get());
                },
                () -> Component.translatable("pattern.tribalpower.expect.air_or", block.get().getName()));
    }

    /** A Ritual Mark: the chalk line joining the pattern together. */
    public static BlockPredicate ritualMark() {
        return block(tk.darrow.tribalpower.block.ModBlocks.RITUAL_MARK);
    }

    // ---- implementations -------------------------------------------------------------------

    private static final BlockPredicate ANYTHING = new BlockPredicate() {
        @Override public boolean test(Level level, BlockPos pos) { return true; }
        @Override public Component description() { return Component.translatable("pattern.tribalpower.expect.anything"); }
        @Override public boolean trivial() { return true; }
    };

    private interface Check { boolean test(Level level, BlockPos pos); }

    private record Simple(Check check, Supplier<Component> describe) implements BlockPredicate {
        @Override public boolean test(Level level, BlockPos pos) { return check.test(level, pos); }
        @Override public Component description() { return describe.get(); }
    }

    /** The pattern's own position. Matching it is what proves the cached match still belongs to this device. */
    record Anchor(Supplier<? extends Block> block) implements BlockPredicate {
        @Override public boolean test(Level level, BlockPos pos) { return level.getBlockState(pos).is(block.get()); }
        @Override public Component description() { return block.get().getName(); }
    }
}

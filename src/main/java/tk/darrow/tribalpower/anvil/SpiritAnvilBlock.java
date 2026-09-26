package tk.darrow.tribalpower.anvil;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The Spirit Anvil: an anvil in every way (it falls, it lands, it renames, combines and repairs) that also mends
 * Spiritgear with Manifested Ingots ({@link SpiritAnvil#repair}). It is not in {@code minecraft:anvil}, which is
 * the tag vanilla reads to chip an anvil after use or a fall, so it never wears down.
 */
public class SpiritAnvilBlock extends AnvilBlock {
    public static final MapCodec<SpiritAnvilBlock> CODEC = simpleCodec(SpiritAnvilBlock::new);
    private static final Component TITLE = Component.translatable("container.tribalpower.spirit_anvil");

    public SpiritAnvilBlock(Properties properties) {
        super(properties);
    }

    @Override
    @SuppressWarnings("unchecked")
    public MapCodec<AnvilBlock> codec() {
        return (MapCodec<AnvilBlock>) (MapCodec<?>) CODEC;
    }

    @Override
    protected MenuProvider getMenuProvider(BlockState state, Level level, BlockPos pos) {
        return new SimpleMenuProvider((id, inventory, player) -> new SpiritAnvilMenu(id, inventory, ContainerLevelAccess.create(level, pos)), TITLE);
    }
}

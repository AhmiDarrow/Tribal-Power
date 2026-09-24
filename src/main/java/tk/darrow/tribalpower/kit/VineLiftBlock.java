package tk.darrow.tribalpower.kit;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** A pad of living vine that carries whoever stands on it to the next pad above or below (see {@link VineLifts}). */
public class VineLiftBlock extends Block {
    public static final MapCodec<VineLiftBlock> CODEC = simpleCodec(VineLiftBlock::new);

    public VineLiftBlock(Properties properties) {
        super(properties);
    }

    @Override protected MapCodec<? extends Block> codec() { return CODEC; }

    @Override
    public void appendHoverText(net.minecraft.world.item.ItemStack stack, net.minecraft.world.item.Item.TooltipContext context,
                                java.util.List<net.minecraft.network.chat.Component> lines, net.minecraft.world.item.TooltipFlag flag) {
        lines.add(net.minecraft.network.chat.Component.translatable("block.tribalpower.vine_lift.desc").withStyle(net.minecraft.ChatFormatting.GRAY));
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(4) != 0) return;
        level.addParticle(ParticleTypes.SPORE_BLOSSOM_AIR, pos.getX() + random.nextDouble(), pos.getY() + 1.05,
                pos.getZ() + random.nextDouble(), 0, 0.02, 0);
    }
}

package tk.darrow.tribalpower.item;

import net.minecraft.core.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import tk.darrow.tribalpower.api.pulse.Attunement;
import tk.darrow.tribalpower.effect.SpiritEffects;
import java.util.List;

/** Deliberate 3x3 excavation, routed through vanilla player breaking and protection events. */
public class ResonanceMaulItem extends PickaxeItem {
    public ResonanceMaulItem(Properties properties) { super(Tiers.DIAMOND, properties.attributes(PickaxeItem.createAttributes(Tiers.DIAMOND, 3, -3.1F))); }
    @Override public InteractionResult useOn(UseOnContext context) {
        if (context.getHand() != net.minecraft.world.InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (!(context.getPlayer() instanceof ServerPlayer player)) return InteractionResult.SUCCESS;
        if (!player.isShiftKeyDown()) return InteractionResult.PASS;
        if (player.getCooldowns().isOnCooldown(this)) return InteractionResult.FAIL;
        Direction.Axis axis = context.getClickedFace().getAxis();
        BlockPos center = context.getClickedPos();
        int broken = 0;
        for (int a = -1; a <= 1; a++) for (int b = -1; b <= 1; b++) {
            BlockPos pos = switch(axis) { case X -> center.offset(0, a, b); case Y -> center.offset(a, 0, b); case Z -> center.offset(a, b, 0); };
            var state = player.level().getBlockState(pos);
            if (context.getItemInHand().isEmpty() || !state.is(BlockTags.MINEABLE_WITH_PICKAXE) || state.getDestroySpeed(player.level(), pos) < 0
                    || !context.getItemInHand().isCorrectToolForDrops(state) || !player.level().mayInteract(player, pos)
                    || !player.mayUseItemAt(pos, context.getClickedFace(), context.getItemInHand())) continue;
            if (!player.isCreative() && SpiritgearHelper.availablePulse(player) < 8) break;
            if (player.gameMode.destroyBlock(pos)) { SpiritgearHelper.tryConsumePulse(player, 8); broken++; }
        }
        if (broken > 0) {
            SpiritEffects.ring(player.serverLevel(), center.getCenter(), Attunement.EARTH, 1.3, 16);
            player.getCooldowns().addCooldown(this, 20);
        } else SpiritgearHelper.notifyStarved(player);
        return InteractionResult.CONSUME;
    }
    @Override public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("item.tribalpower.resonance_maul.desc"));
    }
}

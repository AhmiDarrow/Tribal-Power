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
    public ResonanceMaulItem(Properties properties) { super(tk.darrow.tribalpower.item.SpiritGear.TIER, properties.attributes(PickaxeItem.createAttributes(tk.darrow.tribalpower.item.SpiritGear.TIER, 3, -3.1F))); }
    /** The 3x3 swing is paid in Pulse, block by block, so it leaves the maul's edge alone (see SpiritGear.wear). */
    @Override public <T extends net.minecraft.world.entity.LivingEntity> int damageItem(ItemStack stack, int amount, @org.jetbrains.annotations.Nullable T entity,
            java.util.function.Consumer<Item> onBroken) {
        return SpiritGear.wear(stack, super.damageItem(stack, amount, entity, onBroken));
    }
    @Override public InteractionResult useOn(UseOnContext context) {
        if (context.getHand() != net.minecraft.world.InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (context.getPlayer() == null || !context.getPlayer().isShiftKeyDown()) return InteractionResult.PASS;
        if (!(context.getPlayer() instanceof ServerPlayer player)) return InteractionResult.SUCCESS;
        if (player.getCooldowns().isOnCooldown(this)) return InteractionResult.FAIL;
        Direction.Axis axis = context.getClickedFace().getAxis();
        BlockPos center = context.getClickedPos();
        int broken = 0;
        int perBlock = tk.darrow.tribalpower.config.TribalConfig.maulBlockPulse();
        for (int a = -1; a <= 1; a++) for (int b = -1; b <= 1; b++) {
            BlockPos pos = switch(axis) { case X -> center.offset(0, a, b); case Y -> center.offset(a, 0, b); case Z -> center.offset(a, b, 0); };
            var state = player.level().getBlockState(pos);
            if (context.getItemInHand().isEmpty() || !state.is(BlockTags.MINEABLE_WITH_PICKAXE) || state.getDestroySpeed(player.level(), pos) < 0
                    || !context.getItemInHand().isCorrectToolForDrops(state) || !player.level().mayInteract(player, pos)
                    || !player.mayUseItemAt(pos, context.getClickedFace(), context.getItemInHand())) continue;
            if (!player.isCreative() && GearCell.available(player, context.getItemInHand()) < perBlock) break;
            ItemStack maul = context.getItemInHand();
            int[] wear = {0};
            if (SpiritGear.held(maul, wear, () -> player.gameMode.destroyBlock(pos))) {
                // Paid, the block wears nothing; a block the Pulse somehow could not cover wears as any pick's would.
                if (!GearCell.spend(player, maul, perBlock) && wear[0] > 0) maul.hurtAndBreak(wear[0], player, net.minecraft.world.entity.EquipmentSlot.MAINHAND);
                broken++;
            } else if (wear[0] > 0) maul.hurtAndBreak(wear[0], player, net.minecraft.world.entity.EquipmentSlot.MAINHAND);
        }
        if (broken > 0) {
            SpiritEffects.ring(player.serverLevel(), center.getCenter(), Attunement.EARTH, 1.3, 16);
            player.getCooldowns().addCooldown(this, tk.darrow.tribalpower.config.TribalConfig.maulCooldownTicks());
        } else if (!player.isCreative() && GearCell.available(player, context.getItemInHand()) < perBlock) SpiritgearHelper.notifyStarved(player);
        else player.displayClientMessage(Component.translatable("message.tribalpower.maul.nothing"), true);
        return InteractionResult.CONSUME;
    }
    @Override public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("item.tribalpower.resonance_maul.desc", tk.darrow.tribalpower.config.TribalConfig.maulBlockPulse()));
    }
}

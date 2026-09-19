package tk.darrow.tribalpower.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import tk.darrow.tribalpower.api.pulse.Attunement;

import java.util.List;

/** Spiritgear axe — Pulse spares chops and stripping; a linked totem voice adds a wood perk. */
public class SpiritgearAxeItem extends AxeItem {
    public SpiritgearAxeItem(Properties properties) {
        super(Tiers.DIAMOND, properties.attributes(AxeItem.createAttributes(Tiers.DIAMOND, 5.0F, -3.0F))
                .durability(SpiritGear.TOOL_DURABILITY));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return SpiritGear.foil(stack) || super.isFoil(stack);
    }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        return SpiritGear.destroySpeed(stack, super.getDestroySpeed(stack, state));
    }

    @Override
    public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity entity) {
        if (level.isClientSide || !(entity instanceof ServerPlayer player) || player.getAbilities().instabuild
                || state.getDestroySpeed(level, pos) == 0.0F) {
            return super.mineBlock(stack, level, state, pos, entity);
        }
        SpiritGear.Swing parent = SpiritGear.SWING.get();
        boolean aoe = parent != null && parent.aoe();
        boolean paid = parent != null ? parent.pulsePaid() : SpiritGear.consumeForMine(player, stack);
        if (parent == null) SpiritGear.beginSwing(player, stack, paid, false);
        boolean ok = super.mineBlock(stack, level, state, pos, entity);
        SpiritGear.finishDurability(player, stack, paid);
        if (ok && paid && !aoe && state.is(BlockTags.LOGS)) {
            Attunement voice = SpiritGear.voice(stack).orElse(null);
            if (voice == Attunement.EARTH && SpiritGear.chance(player.getRandom(), stack, 0.25F)) {
                extraLog(player, stack, pos);
            } else if (voice == Attunement.AIR) {
                extraLog(player, stack, pos);
                extraLog(player, stack, pos.relative(player.getDirection()));
            } else if (voice == Attunement.WATER && SpiritGear.chance(player.getRandom(), stack, 0.15F)) {
                net.minecraft.world.level.block.Block.popResource(level, pos, new ItemStack(Blocks.OAK_SAPLING));
            } else if (voice == Attunement.SPIRIT && level instanceof ServerLevel server) {
                for (LivingEntity mob : server.getEntitiesOfClass(LivingEntity.class,
                        player.getBoundingBox().inflate(8), tk.darrow.tribalpower.familiar.FamiliarRoster::hostile)) {
                    mob.addEffect(new MobEffectInstance(MobEffects.GLOWING, 80, 0));
                }
            }
        }
        return ok;
    }

    private static void extraLog(ServerPlayer player, ItemStack tool, BlockPos origin) {
        for (Direction direction : Direction.values()) {
            BlockPos pos = origin.relative(direction);
            BlockState state = player.level().getBlockState(pos);
            if (!state.is(BlockTags.LOGS) || !tool.isCorrectToolForDrops(state)
                    || !player.level().mayInteract(player, pos)) continue;
            SpiritGear.beginSwing(player, tool, true, true);
            try {
                player.gameMode.destroyBlock(pos);
            } finally {
                SpiritGear.beginSwing(player, tool, true, false);
            }
            return;
        }
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        InteractionResult result = super.useOn(context);
        if (result.consumesAction() && player != null && !player.level().isClientSide && !player.getAbilities().instabuild) {
            ItemStack stack = context.getItemInHand();
            if (GearCell.spend(player, stack, SpiritGear.useCost(stack))) {
                stack.setDamageValue(Math.max(0, stack.getDamageValue() - 1));
            } else {
                if (!SpiritGear.skipStarveHurt(stack)) stack.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
                SpiritgearHelper.notifyStarved(player);
            }
        }
        return result;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.tribalpower.spiritgear.desc"));
        SpiritGear.appendTooltip(stack, tooltip, flag);
    }
}

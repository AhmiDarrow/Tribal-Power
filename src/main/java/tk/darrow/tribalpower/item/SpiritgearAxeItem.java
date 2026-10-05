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
        super(tk.darrow.tribalpower.item.SpiritGear.TIER, properties.attributes(AxeItem.createAttributes(tk.darrow.tribalpower.item.SpiritGear.TIER, 5.0F, -3.0F)));
    }

    @Override
    public <T extends LivingEntity> int damageItem(ItemStack stack, int amount, @org.jetbrains.annotations.Nullable T entity,
            java.util.function.Consumer<net.minecraft.world.item.Item> onBroken) {
        return SpiritGear.wear(stack, super.damageItem(stack, amount, entity, onBroken));
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
        SpiritGear.Break mined = SpiritGear.mine(stack, level, state, pos, entity, () -> super.mineBlock(stack, level, state, pos, entity));
        boolean ok = mined.ok();
        if (mined.perks() && entity instanceof ServerPlayer player && state.is(BlockTags.LOGS)) {
            Attunement voice = SpiritGear.voice(stack).orElse(null);
            if (voice == Attunement.EARTH) {
                TreeFelling.fell(player, stack, pos, state);
            } else if (voice == Attunement.AIR) {
                extraLog(player, stack, pos);
                extraLog(player, stack, pos.relative(player.getDirection()));
            } else if (voice == Attunement.WATER && SpiritGear.chance(player, player.getRandom(), stack, 0.15F)) {
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
        if (player == null) return super.useOn(context);
        // Stripping is a paid use: the cell's Pulse instead of the edge, or a starved strip that wears.
        ItemStack stack = context.getItemInHand();
        int[] wear = {0};
        InteractionResult result = SpiritGear.held(stack, wear, () -> super.useOn(context));
        SpiritGear.settleUse(player, stack, LivingEntity.getSlotForHand(context.getHand()), result.consumesAction(), wear[0]);
        return result;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.tribalpower.spiritgear.desc"));
        SpiritGear.appendTooltip(stack, tooltip, flag);
        if (SpiritGear.voice(stack).orElse(null) == Attunement.EARTH)
            tooltip.add(Component.translatable("item.tribalpower.spiritgear_axe.earth").withStyle(net.minecraft.ChatFormatting.DARK_GREEN));
    }
}

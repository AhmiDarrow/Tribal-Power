package tk.darrow.tribalpower.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import tk.darrow.tribalpower.api.pulse.Attunement;

import java.util.List;

/** Spiritgear pickaxe — Pulse spares the edge; a linked totem voice adds a mining perk. */
public class SpiritgearPickaxeItem extends PickaxeItem {
    public SpiritgearPickaxeItem(Properties properties) {
        super(Tiers.DIAMOND, properties.attributes(PickaxeItem.createAttributes(Tiers.DIAMOND, 1.0F, -2.8F))
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
        if (ok && paid && !aoe) {
            Attunement voice = SpiritGear.voice(stack).orElse(null);
            if (voice == Attunement.EARTH && SpiritGearHooks.stoneLike(state) && state.is(BlockTags.MINEABLE_WITH_PICKAXE)) {
                Direction.Axis axis = Direction.orderedByNearest(player)[0].getAxis();
                SpiritGearHooks.aoe(player, stack, pos, axis, SpiritGearHooks::stoneLike);
            }
        }
        return ok;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, level, entity, slot, selected);
        if (level.isClientSide || !selected || !(entity instanceof Player player)) return;
        if (SpiritGear.voice(stack).orElse(null) == Attunement.AIR && level.getGameTime() % 40 == 0) {
            player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED,
                    60, SpiritGear.rank(stack) >= 3 ? 1 : 0, true, false, true));
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.tribalpower.spiritgear.desc"));
        SpiritGear.appendTooltip(stack, tooltip, flag);
    }
}

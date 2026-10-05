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
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import tk.darrow.tribalpower.api.pulse.Attunement;

import java.util.List;

/** Spiritgear pickaxe — Pulse spares the edge; a linked totem voice adds a mining perk. */
public class SpiritgearPickaxeItem extends PickaxeItem {
    public SpiritgearPickaxeItem(Properties properties) {
        super(tk.darrow.tribalpower.item.SpiritGear.TIER, properties.attributes(PickaxeItem.createAttributes(tk.darrow.tribalpower.item.SpiritGear.TIER, 1.0F, -2.8F)));
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
        if (mined.perks() && entity instanceof ServerPlayer player) {
            Attunement voice = SpiritGear.voice(stack).orElse(null);
            if (voice == Attunement.EARTH && SpiritGearHooks.stoneLike(state) && state.is(BlockTags.MINEABLE_WITH_PICKAXE)) {
                Direction.Axis axis = Direction.orderedByNearest(player)[0].getAxis();
                SpiritGearHooks.aoe(player, stack, pos, axis, SpiritGearHooks::stoneLike);
            }
        }
        return mined.ok();
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

package tk.darrow.tribalpower.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FrostedIceBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import tk.darrow.tribalpower.api.pulse.Attunement;

import java.util.List;
import java.util.Map;

public class SpiritweaveArmor extends ArmorItem {
    public static final DeferredRegister<ArmorMaterial> MATERIALS = DeferredRegister.create(Registries.ARMOR_MATERIAL, "tribalpower");
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> MATERIAL = MATERIALS.register("spiritweave", () ->
            new ArmorMaterial(Map.of(Type.HELMET, 3, Type.CHESTPLATE, 8, Type.LEGGINGS, 6, Type.BOOTS, 3, Type.BODY, 8),
                    20, SoundEvents.ARMOR_EQUIP_DIAMOND, () -> Ingredient.of(ModItems.SPIRITWEAVE.get()),
                    List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath("tribalpower", "spiritweave"))),
                    2F, 0.05F));
    static final ResourceLocation STEP = ResourceLocation.fromNamespaceAndPath("tribalpower", "spirit_step");
    /** Ticks one upkeep payment keeps every perk of a piece going: the four seconds between its timed boons. */
    public static final int UPKEEP_TICKS = 80;
    /** When each piece's paid upkeep runs out. Weak, so a piece that is gone is forgotten with it. */
    private static final Map<ItemStack, Long> PAID_UNTIL = new java.util.WeakHashMap<>();

    public SpiritweaveArmor(Type type, Properties properties) {
        super(MATERIAL, type, properties.durability(type.getDurability(33)));
    }

    /** Sneak-use on a hood that has ley goggles switches them off, or back on. A plain click still wears it. */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (getType() == Type.HELMET && player.isShiftKeyDown() && SpiritGear.goggles(stack)) {
            if (!level.isClientSide) SpiritGear.toggleGoggles(player, stack);
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        return super.use(level, player, hand);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return SpiritGear.foil(stack) || super.isFoil(stack);
    }

    @Override
    public <T extends LivingEntity> int damageItem(ItemStack stack, int amount, @org.jetbrains.annotations.Nullable T entity,
            java.util.function.Consumer<net.minecraft.world.item.Item> onBroken) {
        GearCell.armorDamaged(stack, entity);
        return super.damageItem(stack, amount, entity, onBroken);
    }

    /**
     * Whether a worn piece's perks work right now, timed and reactive alike: switched on in the Gear screen, and its
     * upkeep paid. One payment of {@link SpiritGear#armorCost} keeps every perk of the piece going for four seconds;
     * once that runs out, the next perk wanted pays again, from the piece's own cell first. The hood, robe and
     * leggings pay it every four seconds anyway for their timed boons, so their reactive perks ride on that; the
     * boots pay only when one of theirs is used. The client pays nothing, so there a piece counts as powered while
     * enough Pulse is within its reach.
     */
    public static boolean powered(Player player, ItemStack stack) {
        if (!(stack.getItem() instanceof SpiritweaveArmor armor) || SpiritGear.abilitiesOff(stack)) return false;
        if (player.getAbilities().instabuild) return true;
        int cost = SpiritGear.armorCost(stack);
        if (player.level().isClientSide) return GearCell.available(player, stack) >= cost;
        long now = player.level().getGameTime();
        Long until = PAID_UNTIL.get(stack);
        if (until != null && now < until) return true;
        if (!GearCell.spend(player, stack, cost)) return false;
        PAID_UNTIL.put(stack, now + UPKEEP_TICKS);
        // The Loom robe sometimes threads what it spent back into the cell.
        if (armor.getType() == Type.CHESTPLATE && SpiritGear.voice(stack).orElse(null) == Attunement.LOOM
                && player.getRandom().nextFloat() < (SpiritGear.rank(stack) >= 3 ? 0.50F : 0.30F))
            GearCell.refund(player, stack, cost);
        return true;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, level, entity, slot, selected);
        if (level.isClientSide || !(entity instanceof Player player)) return;

        boolean off = SpiritGear.abilitiesOff(stack);
        var step = player.getAttribute(Attributes.STEP_HEIGHT);
        if (getType() == Type.LEGGINGS && step != null) {
            ItemStack worn = player.getItemBySlot(getEquipmentSlot());
            boolean spiritLegs = worn == stack && !off && SpiritGear.voice(stack).orElse(null) == Attunement.SPIRIT
                    && powered(player, stack);
            if (spiritLegs) {
                if (!step.hasModifier(STEP)) step.addTransientModifier(new AttributeModifier(STEP, 1.0, AttributeModifier.Operation.ADD_VALUE));
            } else if (worn == stack || SpiritGear.voice(worn).orElse(null) != Attunement.SPIRIT
                    || !(worn.getItem() instanceof SpiritweaveArmor) || SpiritGear.abilitiesOff(worn)) {
                step.removeModifier(STEP);
            }
        }
        if (player.getItemBySlot(getEquipmentSlot()) != stack) return;
        if (getType() == Type.HELMET && SpiritGear.goggles(stack) && player instanceof net.minecraft.server.level.ServerPlayer wearer)
            tk.darrow.tribalpower.ley.LeyRopes.sync(wearer);
        // Switched off in the Gear screen: the goggles keep their own switch, everything else waits.
        if (off) return;

        // Frost only has to answer where the wearer walks, so it looks every few ticks rather than every one.
        if (getType() == Type.BOOTS && level.getGameTime() % 5 == 0
                && SpiritGear.voice(stack).orElse(null) == Attunement.WATER) {
            freeze(player, stack, SpiritGear.rank(stack) >= 3 ? 3 : 2);
        }

        if (getType() == Type.BOOTS) {
            // Unlinked and Air boots catch a fall with slow falling, paying only when they do.
            Attunement voice = SpiritGear.voice(stack).orElse(null);
            if ((voice == null || voice == Attunement.AIR) && player.fallDistance > 1
                    && !player.hasEffect(MobEffects.SLOW_FALLING) && powered(player, stack))
                player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 80, 0, true, false, true));
            return;
        }

        if (level.getGameTime() % UPKEEP_TICKS != 0) return;
        if (!powered(player, stack)) return;
        apply(player, stack, SpiritGear.voice(stack).orElse(null));
    }

    private void apply(Player player, ItemStack stack, Attunement voice) {
        int rankAmp = SpiritGear.rank(stack) >= 3 ? 1 : 0;
        if (voice == null) {
            var effect = switch (getType()) {
                case HELMET -> MobEffects.NIGHT_VISION;
                case CHESTPLATE -> MobEffects.DAMAGE_RESISTANCE;
                case LEGGINGS -> MobEffects.MOVEMENT_SPEED;
                default -> null;
            };
            if (effect != null) player.addEffect(new MobEffectInstance(effect, getType() == Type.HELMET ? 300 : 100,
                    0, true, false, true));
            return;
        }
        switch (voice) {
            case EARTH -> {
                if (getType() == Type.CHESTPLATE)
                    effect(player, MobEffects.DAMAGE_RESISTANCE, 100, rankAmp);
            }
            case FIRE -> {
                if (getType() == Type.HELMET || getType() == Type.CHESTPLATE)
                    effect(player, MobEffects.FIRE_RESISTANCE, 120, 0);
                if (getType() == Type.LEGGINGS && (player.isInLava() || player.level().dimensionType().ultraWarm()))
                    effect(player, MobEffects.MOVEMENT_SPEED, 100, 0);
            }
            case WATER -> {
                if (getType() == Type.HELMET) effect(player, MobEffects.WATER_BREATHING, 220, 0);
                if (getType() == Type.LEGGINGS) effect(player, MobEffects.DOLPHINS_GRACE, 100, 0);
            }
            case AIR -> {
                if (getType() == Type.HELMET) effect(player, MobEffects.NIGHT_VISION, 300, 0);
                if (getType() == Type.LEGGINGS) effect(player, MobEffects.MOVEMENT_SPEED, 100, rankAmp);
            }
            case SPIRIT -> {
                if (getType() == Type.HELMET) {
                    effect(player, MobEffects.NIGHT_VISION, 300, 0);
                    glowHostiles(player, 12);
                }
                if (getType() == Type.CHESTPLATE) effect(player, MobEffects.DAMAGE_RESISTANCE, 100, 0);
                if (getType() == Type.LEGGINGS) effect(player, MobEffects.MOVEMENT_SPEED, 100, 0);
            }
            case LOOM -> {
                if (getType() == Type.HELMET) effect(player, MobEffects.LUCK, 120, 0);
            }
        }
    }

    private static void effect(Player player, net.minecraft.core.Holder<MobEffect> effect, int duration, int amp) {
        player.addEffect(new MobEffectInstance(effect, duration, amp, true, false, true));
    }

    private static void glowHostiles(Player player, int range) {
        for (LivingEntity mob : player.level().getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(range), tk.darrow.tribalpower.familiar.FamiliarRoster::hostile)) {
            mob.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100, 0));
        }
    }


    /** Water boots freeze still water underfoot, paying their upkeep only when there is water to freeze. */
    private static void freeze(Player player, ItemStack boots, int radius) {
        BlockPos origin = player.blockPosition();
        Level level = player.level();
        List<BlockPos> water = new java.util.ArrayList<>();
        BlockPos.betweenClosed(origin.offset(-radius, -1, -radius), origin.offset(radius, -1, radius)).forEach(pos -> {
            if (pos.distManhattan(origin) > radius) return;
            BlockState state = level.getBlockState(pos);
            if (state.getFluidState().is(Fluids.WATER) && state.getFluidState().isSource()
                    && level.getBlockState(pos.above()).isAir()) {
                water.add(pos.immutable());
            }
        });
        if (water.isEmpty() || !powered(player, boots)) return;
        for (BlockPos pos : water)
            level.setBlockAndUpdate(pos, Blocks.FROSTED_ICE.defaultBlockState().setValue(FrostedIceBlock.AGE, 0));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<net.minecraft.network.chat.Component> lines, TooltipFlag flag) {
        lines.add(net.minecraft.network.chat.Component.translatable("item.tribalpower.spiritweave_armor.desc"));
        if (SpiritGear.abilitiesOff(stack))
            lines.add(net.minecraft.network.chat.Component.translatable("item.tribalpower.spiritweave_armor.off").withStyle(net.minecraft.ChatFormatting.RED));
        SpiritGear.appendTooltip(stack, lines, flag);
    }
}

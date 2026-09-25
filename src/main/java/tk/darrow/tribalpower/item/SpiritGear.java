package tk.darrow.tribalpower.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.Ingredient;
import tk.darrow.tribalpower.TribalPower;
import tk.darrow.tribalpower.api.pulse.Attunement;
import tk.darrow.tribalpower.echo.LatticeRecipe;
import tk.darrow.tribalpower.echo.ProcessingRecipes;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Rank and totem voice for Spiritgear tools and Spiritweave armor, stored on
 * {@link DataComponents#CUSTOM_DATA} as {@code GearRank} (0–3) and {@code GearVoice}.
 */
public final class SpiritGear {
    public static final String RANK_KEY = "GearRank";
    public static final String VOICE_KEY = "GearVoice";
    public static final int MAX_RANK = 3;
    public static final int LINK_COST = 40;
    public static final int TOOL_DURABILITY = 1024;

    public record Swing(Player player, ItemStack tool, boolean pulsePaid, boolean aoe) {}

    static final ThreadLocal<Swing> SWING = new ThreadLocal<>();

    private SpiritGear() {}

    public static boolean isTool(ItemStack stack) {
        Item item = stack.getItem();
        return item instanceof SpiritgearPickaxeItem || item instanceof SpiritgearAxeItem
                || item instanceof SpiritgearShovelItem || item instanceof SpiritgearBladeItem
                || item instanceof SpiritgearShearsItem || item instanceof SpiritgearHoeItem
                || item instanceof SpiritgearRattleItem;
    }

    public static boolean isArmor(ItemStack stack) {
        return stack.getItem() instanceof SpiritweaveArmor;
    }

    public static boolean isGear(ItemStack stack) {
        return isTool(stack) || isArmor(stack);
    }

    /**
     * Read the piece's tag without copying it. {@code copyTag()} deep-copies the whole compound, and
     * these two are read several times per tick per worn piece and again for every frame the piece is
     * drawn, so the copy dominated. Nothing here mutates what it reads; the writers still go through
     * {@link CustomData#update}.
     */
    private static net.minecraft.nbt.CompoundTag tag(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).getUnsafe();
    }

    public static int rank(ItemStack stack) {
        if (!isGear(stack)) return 0;
        return Math.clamp(tag(stack).getInt(RANK_KEY), 0, MAX_RANK);
    }

    public static void setRank(ItemStack stack, int rank) {
        int clamped = Math.clamp(rank, 0, MAX_RANK);
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            if (clamped <= 0) tag.remove(RANK_KEY);
            else tag.putInt(RANK_KEY, clamped);
        });
        model(stack);
    }

    private static final String GOGGLES_KEY = "Goggles";

    /** Ley goggles fitted to a Spiritweave Hood. Worn, they show the flowing ropes. */
    public static boolean goggles(ItemStack stack) {
        return stack.is(ModItems.SPIRITWEAVE_HOOD.get()) && tag(stack).getBoolean(GOGGLES_KEY);
    }

    public static void setGoggles(ItemStack stack, boolean on) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, data -> {
            if (on) data.putBoolean(GOGGLES_KEY, true);
            else data.remove(GOGGLES_KEY);
        });
        model(stack);
    }

    private static final String GOGGLES_OFF = "GogglesOff";

    /** Fitted goggles that have not been switched off. */
    public static boolean gogglesOpen(ItemStack stack) {
        return goggles(stack) && !tag(stack).getBoolean(GOGGLES_OFF);
    }

    /** Switches fitted goggles between ley sight and off. Returns whether they are open now. */
    public static boolean toggleGoggles(Player player, ItemStack stack) {
        if (!goggles(stack)) return false;
        boolean open = !gogglesOpen(stack);
        CustomData.update(DataComponents.CUSTOM_DATA, stack, data -> {
            if (open) data.remove(GOGGLES_OFF);
            else data.putBoolean(GOGGLES_OFF, true);
        });
        player.displayClientMessage(Component.translatable(open
                ? "message.tribalpower.goggles.on" : "message.tribalpower.goggles.off"), true);
        return open;
    }

    private static final String OFF_KEY = "GearOff";

    /** A worn Spiritweave piece the player switched off: it protects like armor but gives no effect and draws no Pulse. */
    public static boolean abilitiesOff(ItemStack stack) {
        return tag(stack).getBoolean(OFF_KEY);
    }

    public static void setAbilitiesOff(ItemStack stack, boolean off) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, data -> {
            if (off) data.putBoolean(OFF_KEY, true);
            else data.remove(OFF_KEY);
        });
    }

    /** Rank uses 1–3. Goggles add 4, so a manifested hood with goggles is 7 and both models survive. */
    private static void model(ItemStack stack) {
        int cmd = rank(stack) + (goggles(stack) ? 4 : 0);
        if (cmd <= 0) stack.remove(DataComponents.CUSTOM_MODEL_DATA);
        else stack.set(DataComponents.CUSTOM_MODEL_DATA, new net.minecraft.world.item.component.CustomModelData(cmd));
    }

    public static boolean foil(ItemStack stack) {
        return rank(stack) >= 3;
    }

    private static final java.util.Map<String, Optional<Attunement>> VOICES = voiceLookup();

    private static java.util.Map<String, Optional<Attunement>> voiceLookup() {
        java.util.Map<String, Optional<Attunement>> map = new java.util.HashMap<>();
        for (Attunement attunement : Attunement.values()) map.put(attunement.getSerializedName(), Optional.of(attunement));
        return java.util.Map.copyOf(map);
    }

    public static Optional<Attunement> voice(ItemStack stack) {
        if (!isGear(stack)) return Optional.empty();
        String name = tag(stack).getString(VOICE_KEY);
        if (name == null || name.isEmpty()) return Optional.empty();
        return VOICES.getOrDefault(name, Optional.empty());
    }

    public static void setVoice(ItemStack stack, Attunement attunement) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag ->
                tag.putString(VOICE_KEY, attunement.getSerializedName()));
    }

    public static boolean linked(ItemStack stack) {
        return voice(stack).isPresent();
    }

    public static int mineCost(ItemStack stack) {
        if (stack.getItem() instanceof SpiritgearPickaxeItem && voice(stack).orElse(null) == Attunement.LOOM) {
            return 0;
        }
        return Math.max(1, SpiritgearHelper.MINE_COST - (rank(stack) >= 1 ? 1 : 0));
    }

    public static int hitCost(ItemStack stack) {
        return Math.max(1, SpiritgearHelper.HIT_COST - (rank(stack) >= 1 ? 1 : 0));
    }

    public static int useCost(ItemStack stack) {
        return Math.max(1, SpiritgearHelper.USE_COST - (rank(stack) >= 1 ? 1 : 0));
    }

    public static int armorCost(ItemStack stack) {
        return linked(stack) ? 3 : 2;
    }

    public static boolean skipStarveHurt(ItemStack stack) {
        return rank(stack) >= 2;
    }

    public static float destroySpeed(ItemStack stack, float base) {
        return base > 1.0F ? base * MINING[rank(stack)] : base;
    }

    public static void beginSwing(Player player, ItemStack tool, boolean pulsePaid, boolean aoe) {
        SWING.set(new Swing(player, tool, pulsePaid, aoe));
    }

    public static void endSwing() {
        SWING.remove();
    }

    /**
     * The swing in progress for {@code player}, or null. The swing is only cleared on the next player tick, so a
     * stale one (another player's, or an earlier break's) must never be picked up by a different break.
     */
    @org.jetbrains.annotations.Nullable
    public static Swing swingFor(Player player) {
        Swing swing = SWING.get();
        return swing != null && swing.player() == player ? swing : null;
    }

    public static Optional<Swing> swing() {
        return Optional.ofNullable(SWING.get());
    }

    /**
     * Spend Pulse for a mine swing. Creative and zero-cost Loom picks count as paid so perks still fire.
     */
    public static boolean consumeForMine(Player player, ItemStack stack) {
        if (player.getAbilities().instabuild) return true;
        int cost = mineCost(stack);
        if (cost <= 0) return true;
        return GearCell.spend(player, stack, cost);
    }

    public static void finishDurability(Player player, ItemStack stack, boolean paid) {
        if (player.getAbilities().instabuild) return;
        RandomSource random = player.getRandom();
        if (paid) {
            stack.setDamageValue(Math.max(0, stack.getDamageValue() - 1));
            SpiritgearHelper.notifyFueled(player);
            return;
        }
        boolean refund = (rank(stack) >= 3 && random.nextFloat() < 0.5F)
                || (stack.getItem() instanceof SpiritgearPickaxeItem
                && voice(stack).orElse(null) == Attunement.LOOM && random.nextFloat() < 0.75F);
        if (refund) stack.setDamageValue(Math.max(0, stack.getDamageValue() - 1));
        else if (!skipStarveHurt(stack)) {
            stack.hurtAndBreak(1, player, net.minecraft.world.entity.EquipmentSlot.MAINHAND);
        }
        SpiritgearHelper.notifyStarved(player);
    }

    public static boolean tryLink(Player player, ItemStack stack, Attunement attunement) {
        if (!isGear(stack)) return false;
        if (!player.getAbilities().instabuild && !GearCell.spend(player, stack, LINK_COST)) {
            SpiritgearHelper.notifyStarved(player);
            return false;
        }
        setVoice(stack, attunement);
        player.displayClientMessage(Component.translatable("message.tribalpower.spiritgear.linked",
                Component.translatable("attunement.tribalpower." + attunement.getSerializedName())), true);
        return true;
    }

    public static ItemStack withRank(ItemStack stack, int rank) {
        ItemStack copy = stack.copyWithCount(1);
        setRank(copy, rank);
        return copy;
    }

    /**
     * Echo Attune 0→1 (Fire), Bind 1→2 (Water), Manifest 2→3 (Spirit). Bind refuses rank 0.
     * Snapshots the input so voice and damage survive the station.
     */
    public static ProcessingRecipes.Formula rankFormula(String station, ItemStack stack) {
        if (stack.isEmpty() || !isGear(stack)) return null;
        int from = rank(stack);
        int to;
        Attunement attunement;
        int seconds;
        int pulse;
        String step;
        switch (station) {
            case "echo_attune" -> {
                if (from != 0) return null;
                to = 1; attunement = Attunement.FIRE; seconds = 45; pulse = 48; step = "attune";
            }
            case "echo_bind" -> {
                if (from != 1) return null;
                to = 2; attunement = Attunement.WATER; seconds = 90; pulse = 64; step = "bind";
            }
            case "echo_manifest" -> {
                if (from != 2) return null;
                to = 3; attunement = Attunement.SPIRIT; seconds = 180; pulse = 96; step = "manifest";
            }
            default -> { return null; }
        }
        ItemStack output = withRank(stack, to);
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(TribalPower.MOD_ID,
                "spiritgear/" + step + "/" + BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath());
        LatticeRecipe recipe = new LatticeRecipe(station, Ingredient.of(stack.getItem()), output,
                seconds, pulse, attunement);
        return new ProcessingRecipes.Formula(id, recipe, catalysts(to));
    }

    /**
     * What each rank consumes besides Pulse and time, per piece: the next crystal for Attuned and Bound, and for
     * Manifested two Resonant Cores and Loom Thread, which only The Unsung gives. Top gear waits on that fight.
     */
    public static List<ItemStack> catalysts(int toRank) {
        return switch (toRank) {
            case 1 -> List.of(new ItemStack(ModItems.ATTUNED_ECHO.get(), 4));
            case 2 -> List.of(new ItemStack(ModItems.BOUND_ECHO.get(), 4));
            case 3 -> List.of(new ItemStack(ModItems.RESONANT_CORE.get(), 2), new ItemStack(ModItems.LOOM_THREAD.get(), 4));
            default -> List.of();
        };
    }

    public static boolean isCatalyst(ItemStack stack) {
        return stack.is(ModItems.ATTUNED_ECHO.get()) || stack.is(ModItems.BOUND_ECHO.get())
                || stack.is(ModItems.RESONANT_CORE.get()) || stack.is(ModItems.LOOM_THREAD.get());
    }

    // ---- what a rank is worth -------------------------------------------------------------------

    /** Extra armour per piece by rank, per slot: a full Manifested set is 29, one short of the cap. */
    private static final int[][] ARMOR_BONUS = {
            {0, 1, 1, 2},   // head
            {0, 1, 2, 3},   // chest
            {0, 1, 2, 2},   // legs
            {0, 1, 1, 2}};  // feet
    private static final double[] TOUGHNESS_BONUS = {0, 1, 2, 3};
    private static final double[] KNOCKBACK_BONUS = {0, 0.05, 0.10, 0.15};
    private static final double[] HEALTH_BONUS = {0, 1, 2, 4};
    private static final double[] BLADE_DAMAGE = {0, 2, 5, 10};
    private static final double[] BLADE_SPEED = {0, 0, 0.1, 0.2};
    private static final double[] TOOL_DAMAGE = {0, 1, 2, 3};
    private static final float[] MINING = {1.0F, 1.2F, 1.45F, 1.8F};
    /** Incoming damage taken with a whole set of Bound (or better) and of Manifested pieces. */
    public static final float BOUND_SET = 0.9F, MANIFESTED_SET = 0.8F;
    /** A Manifested blade hits bosses harder and gives some of the blow back as health. */
    public static final float BOSS_BONUS = 1.25F, LIFESTEAL = 0.1F;

    /** Adds rank bonuses to a piece's attributes. Everything here scales with rank alone; voices add perks. */
    public static void rankAttributes(net.neoforged.neoforge.event.ItemAttributeModifierEvent event) {
        ItemStack stack = event.getItemStack();
        int rank = rank(stack);
        if (rank <= 0) return;
        if (stack.getItem() instanceof SpiritweaveArmor armor) {
            var slot = armor.getEquipmentSlot();
            var group = net.minecraft.world.entity.EquipmentSlotGroup.bySlot(slot);
            int row = switch (slot) { case HEAD -> 0; case CHEST -> 1; case LEGS -> 2; default -> 3; };
            String name = slot.getName();
            add(event, net.minecraft.world.entity.ai.attributes.Attributes.ARMOR, "spiritweave_armor_" + name, ARMOR_BONUS[row][rank], group);
            add(event, net.minecraft.world.entity.ai.attributes.Attributes.ARMOR_TOUGHNESS, "spiritweave_toughness_" + name, TOUGHNESS_BONUS[rank], group);
            add(event, net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE, "spiritweave_knockback_" + name, KNOCKBACK_BONUS[rank], group);
            add(event, net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH, "spiritweave_health_" + name, HEALTH_BONUS[rank], group);
        } else if (stack.getItem() instanceof SpiritgearBladeItem) {
            add(event, net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE, "spiritgear_blade_damage", BLADE_DAMAGE[rank],
                    net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND);
            add(event, net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_SPEED, "spiritgear_blade_speed", BLADE_SPEED[rank],
                    net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND);
        } else if (isTool(stack)) {
            add(event, net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE, "spiritgear_tool_damage", TOOL_DAMAGE[rank],
                    net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND);
        }
    }

    private static void add(net.neoforged.neoforge.event.ItemAttributeModifierEvent event,
                            net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute, String id, double amount,
                            net.minecraft.world.entity.EquipmentSlotGroup group) {
        if (amount == 0) return;
        event.addModifier(attribute, new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                ResourceLocation.fromNamespaceAndPath(TribalPower.MOD_ID, id), amount,
                net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE), group);
    }

    /** The lowest rank among four worn Spiritweave pieces, or -1 when the set is not complete. */
    public static int setRank(Player player) {
        int lowest = MAX_RANK;
        for (var slot : new net.minecraft.world.entity.EquipmentSlot[]{net.minecraft.world.entity.EquipmentSlot.HEAD,
                net.minecraft.world.entity.EquipmentSlot.CHEST, net.minecraft.world.entity.EquipmentSlot.LEGS,
                net.minecraft.world.entity.EquipmentSlot.FEET}) {
            ItemStack worn = player.getItemBySlot(slot);
            if (!isArmor(worn)) return -1;
            lowest = Math.min(lowest, rank(worn));
        }
        return lowest;
    }

    public static List<ProcessingRecipes.Formula> allRankFormulae() {
        List<Item> pieces = List.of(
                ModItems.SPIRITGEAR_PICKAXE.get(), ModItems.SPIRITGEAR_AXE.get(),
                ModItems.SPIRITGEAR_SHOVEL.get(), ModItems.SPIRITGEAR_BLADE.get(),
                ModItems.SPIRITGEAR_SHEARS.get(), ModItems.SPIRITGEAR_HOE.get(),
                ModItems.SPIRITWEAVE_HOOD.get(), ModItems.SPIRITWEAVE_ROBE.get(),
                ModItems.SPIRITWEAVE_LEGGINGS.get(), ModItems.SPIRITWEAVE_BOOTS.get());
        pieces = new ArrayList<>(pieces);
        for (var weapon : ModItems.SPIRITGEAR_WEAPONS.values()) pieces.add(weapon.get());
        pieces.add(ModItems.SPIRITGEAR_RATTLE.get());
        String[] stations = {"echo_attune", "echo_bind", "echo_manifest"};
        List<ProcessingRecipes.Formula> out = new ArrayList<>();
        for (Item piece : pieces) {
            for (int rank = 0; rank < MAX_RANK; rank++) {
                ItemStack input = new ItemStack(piece);
                setRank(input, rank);
                ProcessingRecipes.Formula formula = rankFormula(stations[rank], input);
                if (formula != null) out.add(formula);
            }
        }
        return out;
    }

    public static void appendTooltip(ItemStack stack, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("item.tribalpower.spiritgear.rank",
                Component.translatable("item.tribalpower.spiritgear.rank." + rank(stack))));
        voice(stack).ifPresentOrElse(
                attunement -> lines.add(Component.translatable("item.tribalpower.spiritgear.voice",
                        Component.translatable("attunement.tribalpower." + attunement.getSerializedName()))),
                () -> lines.add(Component.translatable("item.tribalpower.spiritgear.unlinked")));
        int rank = rank(stack);
        if (rank < MAX_RANK) {
            String[] stations = {"echo_attune", "echo_bind", "echo_manifest"};
            List<Component> parts = new ArrayList<>();
            for (ItemStack want : catalysts(rank + 1)) parts.add(Component.literal(want.getCount() + " ").append(want.getHoverName()));
            lines.add(Component.translatable("item.tribalpower.spiritgear.next_rank",
                    Component.translatable("block.tribalpower." + stations[rank]),
                    net.minecraft.network.chat.ComponentUtils.formatList(parts, Component.literal(", ")))
                    .withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
        }
        if (isArmor(stack) && rank >= 2)
            lines.add(Component.translatable(rank >= 3 ? "item.tribalpower.spiritweave.set_manifested" : "item.tribalpower.spiritweave.set_bound")
                    .withStyle(net.minecraft.ChatFormatting.DARK_AQUA));
        if (stack.getItem() instanceof SpiritgearBladeItem && rank >= 3)
            lines.add(Component.translatable("item.tribalpower.spiritgear_blade.manifested").withStyle(net.minecraft.ChatFormatting.DARK_AQUA));
        if (goggles(stack))
            lines.add(Component.translatable(gogglesOpen(stack)
                    ? "item.tribalpower.spiritweave.goggles" : "item.tribalpower.spiritweave.goggles_off")
                    .withStyle(net.minecraft.ChatFormatting.AQUA));
    }

    public static boolean chance(ItemStack stack, float base) {
        return RandomSource.create().nextFloat() < (rank(stack) >= 3 ? base * 2 : base);
    }

    public static boolean chance(RandomSource random, ItemStack stack, float base) {
        return random.nextFloat() < (rank(stack) >= 3 ? Math.min(1.0F, base * 2) : base);
    }
}

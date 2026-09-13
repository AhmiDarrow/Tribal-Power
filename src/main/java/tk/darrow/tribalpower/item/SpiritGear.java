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
                || item instanceof SpiritgearShovelItem || item instanceof SpiritgearBladeItem;
    }

    public static boolean isArmor(ItemStack stack) {
        return stack.getItem() instanceof SpiritweaveArmor;
    }

    public static boolean isGear(ItemStack stack) {
        return isTool(stack) || isArmor(stack);
    }

    public static int rank(ItemStack stack) {
        if (!isGear(stack)) return 0;
        return Math.clamp(stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                .copyTag().getInt(RANK_KEY), 0, MAX_RANK);
    }

    public static void setRank(ItemStack stack, int rank) {
        int clamped = Math.clamp(rank, 0, MAX_RANK);
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            if (clamped <= 0) tag.remove(RANK_KEY);
            else tag.putInt(RANK_KEY, clamped);
        });
        if (clamped <= 0) stack.remove(DataComponents.CUSTOM_MODEL_DATA);
        else stack.set(DataComponents.CUSTOM_MODEL_DATA, new net.minecraft.world.item.component.CustomModelData(clamped));
    }

    public static boolean foil(ItemStack stack) {
        return rank(stack) >= 3;
    }

    public static Optional<Attunement> voice(ItemStack stack) {
        if (!isGear(stack)) return Optional.empty();
        String name = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                .copyTag().getString(VOICE_KEY);
        if (name == null || name.isEmpty()) return Optional.empty();
        for (Attunement attunement : Attunement.values()) {
            if (attunement.getSerializedName().equals(name)) return Optional.of(attunement);
        }
        return Optional.empty();
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
        if (base > 1.0F && rank(stack) >= 3) {
            return base * (Tiers.NETHERITE.getSpeed() / Tiers.DIAMOND.getSpeed());
        }
        return base;
    }

    public static void beginSwing(Player player, ItemStack tool, boolean pulsePaid, boolean aoe) {
        SWING.set(new Swing(player, tool, pulsePaid, aoe));
    }

    public static void endSwing() {
        SWING.remove();
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
        return SpiritgearHelper.tryConsumePulse(player, cost);
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
        if (!player.getAbilities().instabuild && !SpiritgearHelper.tryConsumePulse(player, LINK_COST)) {
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
        if (!isGear(stack)) return null;
        int from = rank(stack);
        int to;
        Attunement attunement;
        int seconds;
        int pulse;
        String step;
        switch (station) {
            case "echo_attune" -> {
                if (from != 0) return null;
                to = 1; attunement = Attunement.FIRE; seconds = 8; pulse = 24; step = "attune";
            }
            case "echo_bind" -> {
                if (from != 1) return null;
                to = 2; attunement = Attunement.WATER; seconds = 10; pulse = 32; step = "bind";
            }
            case "echo_manifest" -> {
                if (from != 2) return null;
                to = 3; attunement = Attunement.SPIRIT; seconds = 12; pulse = 40; step = "manifest";
            }
            default -> { return null; }
        }
        ItemStack output = withRank(stack, to);
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(TribalPower.MOD_ID,
                "spiritgear/" + step + "/" + BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath());
        LatticeRecipe recipe = new LatticeRecipe(station, Ingredient.of(stack.getItem()), output,
                seconds, pulse, attunement);
        return new ProcessingRecipes.Formula(id, recipe);
    }

    public static List<ProcessingRecipes.Formula> allRankFormulae() {
        List<Item> pieces = List.of(
                ModItems.SPIRITGEAR_PICKAXE.get(), ModItems.SPIRITGEAR_AXE.get(),
                ModItems.SPIRITGEAR_SHOVEL.get(), ModItems.SPIRITGEAR_BLADE.get(),
                ModItems.SPIRITWEAVE_HOOD.get(), ModItems.SPIRITWEAVE_ROBE.get(),
                ModItems.SPIRITWEAVE_LEGGINGS.get(), ModItems.SPIRITWEAVE_BOOTS.get());
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
    }

    public static boolean chance(ItemStack stack, float base) {
        return RandomSource.create().nextFloat() < (rank(stack) >= 3 ? base * 2 : base);
    }

    public static boolean chance(RandomSource random, ItemStack stack, float base) {
        return random.nextFloat() < (rank(stack) >= 3 ? Math.min(1.0F, base * 2) : base);
    }
}

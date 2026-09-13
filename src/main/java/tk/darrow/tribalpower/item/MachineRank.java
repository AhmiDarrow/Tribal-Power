package tk.darrow.tribalpower.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import tk.darrow.tribalpower.TribalPower;
import tk.darrow.tribalpower.api.pulse.Attunement;
import tk.darrow.tribalpower.echo.LatticeRecipe;
import tk.darrow.tribalpower.echo.ProcessingRecipes;

import java.util.ArrayList;
import java.util.List;

/**
 * Workshop machines rank the same way Spiritgear does — Echo Attune, Bind, Manifest —
 * but the station asks for twice the time and Pulse. Rank lives on the item as
 * {@code MachineRank} and on the placed block entity as persistent NBT.
 */
public final class MachineRank {
    public static final String KEY = "MachineRank";
    public static final int MAX = 3;
    public static final TagKey<Item> UPGRADEABLE = TagKey.create(Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(TribalPower.MOD_ID, "upgradeable_machines"));

    private MachineRank() {}

    public static boolean isMachine(ItemStack stack) {
        return !stack.isEmpty() && stack.is(UPGRADEABLE);
    }

    public static int rank(ItemStack stack) {
        if (!isMachine(stack)) return 0;
        return Math.clamp(stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                .copyTag().getInt(KEY), 0, MAX);
    }

    public static int rank(BlockEntity be) {
        if (be == null) return 0;
        return Math.clamp(be.getPersistentData().getInt(KEY), 0, MAX);
    }

    public static void setRank(ItemStack stack, int rank) {
        int clamped = Math.clamp(rank, 0, MAX);
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            if (clamped <= 0) tag.remove(KEY);
            else tag.putInt(KEY, clamped);
        });
        if (clamped <= 0) stack.remove(DataComponents.CUSTOM_MODEL_DATA);
        else stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(clamped));
    }

    public static void apply(BlockEntity be, int rank) {
        if (be == null) return;
        int clamped = Math.clamp(rank, 0, MAX);
        if (clamped <= 0) be.getPersistentData().remove(KEY);
        else be.getPersistentData().putInt(KEY, clamped);
        be.setChanged();
    }

    public static void copyToItem(BlockEntity be, ItemStack stack) {
        if (be == null || !isMachine(stack)) return;
        setRank(stack, rank(be));
    }

    public static void copyToBlock(ItemStack stack, BlockEntity be) {
        if (be == null || !isMachine(stack)) return;
        apply(be, rank(stack));
    }

    /** Rank 1/2/3: 85% / 70% / 55% duration. Extra speed is paid 1:1 in Pulse. */
    public static float timeFactor(int rank) {
        return switch (Math.clamp(rank, 0, MAX)) {
            case 1 -> 0.85F;
            case 2 -> 0.70F;
            case 3 -> 0.55F;
            default -> 1.0F;
        };
    }

    /** Inverse of {@link #timeFactor}: 118% / 143% / 182% draw or cargo. */
    public static float pulseFactor(int rank) {
        float time = timeFactor(rank);
        return time <= 0F ? 1.0F : 1.0F / time;
    }

    /** Rank 1/2/3: 118% / 143% / 182% Pulse a second (or items / mB per beat). */
    public static int scalePulse(BlockEntity be, int pulse) {
        return Math.max(1, Math.round(pulse * pulseFactor(rank(be))));
    }

    /** Rank 1/2/3: 85% / 70% / 55% duration. */
    public static int scaleTime(BlockEntity be, int units) {
        return Math.max(1, Math.round(units * timeFactor(rank(be))));
    }

    /** Workshop / loom beat period. Rank 0 stays 20 ticks. */
    public static int beatTicks(BlockEntity be) {
        return scaleTime(be, 20);
    }

    public static boolean due(Level level, BlockPos pos, BlockEntity be) {
        return Math.floorMod(level.getGameTime() + pos.asLong(), beatTicks(be)) == 0;
    }

    /** Extra Pulse a second for generators. */
    public static int bonusGain(BlockEntity be, int gain) {
        if (gain <= 0) return 0;
        return Math.round(gain * 0.15F * rank(be));
    }

    public static int itemBurst(BlockEntity be, int base) {
        return scalePulse(be, base);
    }

    public static ItemStack withRank(ItemStack stack, int rank) {
        ItemStack copy = stack.copyWithCount(1);
        setRank(copy, rank);
        return copy;
    }

    /**
     * Twice Spiritgear's station cost: Attune 16s/48, Bind 20s/64, Manifest 24s/80.
     */
    public static ProcessingRecipes.Formula rankFormula(String station, ItemStack stack) {
        if (!isMachine(stack)) return null;
        int from = rank(stack);
        int to;
        Attunement attunement;
        int seconds;
        int pulse;
        String step;
        switch (station) {
            case "echo_attune" -> {
                if (from != 0) return null;
                to = 1; attunement = Attunement.FIRE; seconds = 16; pulse = 48; step = "attune";
            }
            case "echo_bind" -> {
                if (from != 1) return null;
                to = 2; attunement = Attunement.WATER; seconds = 20; pulse = 64; step = "bind";
            }
            case "echo_manifest" -> {
                if (from != 2) return null;
                to = 3; attunement = Attunement.SPIRIT; seconds = 24; pulse = 80; step = "manifest";
            }
            default -> { return null; }
        }
        ItemStack output = withRank(stack, to);
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(TribalPower.MOD_ID,
                "machine/" + step + "/" + BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath());
        LatticeRecipe recipe = new LatticeRecipe(station, Ingredient.of(stack.getItem()), output,
                seconds, pulse, attunement);
        return new ProcessingRecipes.Formula(id, recipe);
    }

    public static List<ProcessingRecipes.Formula> allRankFormulae() {
        List<ProcessingRecipes.Formula> out = new ArrayList<>();
        String[] stations = {"echo_attune", "echo_bind", "echo_manifest"};
        for (Item item : BuiltInRegistries.ITEM) {
            ItemStack stack = new ItemStack(item);
            if (!isMachine(stack)) continue;
            for (int rank = 0; rank < MAX; rank++) {
                setRank(stack, rank);
                ProcessingRecipes.Formula formula = rankFormula(stations[rank], stack);
                if (formula != null) out.add(formula);
            }
        }
        return out;
    }

    public static void appendTooltip(ItemStack stack, List<Component> lines) {
        if (!isMachine(stack)) return;
        lines.add(Component.translatable("item.tribalpower.machine.rank",
                Component.translatable("item.tribalpower.spiritgear.rank." + rank(stack))));
    }

    private static final java.util.Map<java.util.UUID, ItemStack> PLACING = new java.util.concurrent.ConcurrentHashMap<>();

    public static void beforePlace(net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide()) return;
        if (isMachine(event.getItemStack()) && event.getEntity() != null)
            PLACING.put(event.getEntity().getUUID(), event.getItemStack().copy());
    }

    public static void placed(net.neoforged.neoforge.event.level.BlockEvent.EntityPlaceEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof net.minecraft.world.entity.player.Player player)) return;
        ItemStack stack = PLACING.remove(player.getUUID());
        if (stack == null) return;
        if (stack.getItem() instanceof net.minecraft.world.item.BlockItem blockItem
                && event.getPlacedBlock().getBlock() != blockItem.getBlock()) return;
        copyToBlock(stack, event.getLevel().getBlockEntity(event.getPos()));
    }

    /** Drop a leftover snapshot if the click never placed a block. */
    public static void playerTick(net.neoforged.neoforge.event.tick.PlayerTickEvent.Post event) {
        if (!event.getEntity().level().isClientSide) PLACING.remove(event.getEntity().getUUID());
    }

    public static void dropped(net.neoforged.neoforge.event.level.BlockDropsEvent event) {
        if (event.getLevel().isClientSide) return;
        var be = event.getBlockEntity();
        if (be == null || rank(be) <= 0) return;
        for (var drop : event.getDrops()) copyToItem(be, drop.getItem());
    }
}

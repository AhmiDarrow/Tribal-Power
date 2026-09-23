package tk.darrow.tribalpower.item;

import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/**
 * A Pulse Cell seated inside a piece of equipment. The piece spends from its own cell first and only reaches
 * for the cells in the player's inventory once its own is empty. Right-clicking a cell onto a piece in an
 * inventory seats it, swaps in a bigger one (handing the smaller back), or tops the seated one up.
 */
public final class GearCell {
    private static final String CELL = "GearCell", PULSE = "GearCellPulse";

    private GearCell() {}

    /** Equipment that can carry a cell: Spiritgear, Spiritweave, the Resonance Maul and the Sixfold Staff. */
    public static boolean accepts(ItemStack stack) {
        return SpiritGear.isGear(stack) || stack.getItem() instanceof ResonanceMaulItem
                || stack.getItem() instanceof SpiritStaffItem
                || stack.getItem() instanceof tk.darrow.tribalpower.song.SongbookItem
                || stack.getItem() instanceof tk.darrow.tribalpower.song.PulseBowItem;
    }

    /** The seated cell's item, or null. */
    public static Item cell(ItemStack gear) {
        String id = gear.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString(CELL);
        if (id.isEmpty()) return null;
        var key = ResourceLocation.tryParse(id);
        return key != null && BuiltInRegistries.ITEM.containsKey(key) && BuiltInRegistries.ITEM.get(key) instanceof PulseCellItem cell ? cell : null;
    }

    public static int capacity(ItemStack gear) {
        Item cell = cell(gear);
        return cell == null ? 0 : PulseCellItem.capacity(new ItemStack(cell));
    }

    public static int pulse(ItemStack gear) {
        return Math.clamp(gear.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getInt(PULSE), 0, capacity(gear));
    }

    private static void set(ItemStack gear, Item cell, int pulse) {
        CustomData.update(DataComponents.CUSTOM_DATA, gear, tag -> {
            if (cell == null) { tag.remove(CELL); tag.remove(PULSE); return; }
            tag.putString(CELL, BuiltInRegistries.ITEM.getKey(cell).toString());
            tag.putInt(PULSE, pulse);
        });
    }

    /**
     * A cell clicked onto a piece. Returns what should be left on the cursor: empty when the cell went in,
     * the old smaller cell after a swap, or the same cell (lighter) after a top-up. Null when nothing happened.
     */
    public static ItemStack offer(ItemStack gear, ItemStack carried) {
        if (!accepts(gear) || !(carried.getItem() instanceof PulseCellItem)) return null;
        int offered = PulseCellItem.capacity(carried);
        Item seated = cell(gear);
        if (seated == null) {
            set(gear, carried.getItem(), PulseCellItem.getPulse(carried));
            return ItemStack.EMPTY;
        }
        if (offered > capacity(gear)) {
            ItemStack old = new ItemStack(seated);
            PulseCellItem.setPulse(old, pulse(gear));
            set(gear, carried.getItem(), PulseCellItem.getPulse(carried));
            return old;
        }
        int room = capacity(gear) - pulse(gear);
        int moved = PulseCellItem.extractPulse(carried, room, false);
        if (moved <= 0) return null;
        set(gear, seated, pulse(gear) + moved);
        return carried;
    }

    /** The seated cell as an item with its charge, or empty. The piece itself is left alone. */
    public static ItemStack asStack(ItemStack gear) {
        Item cell = cell(gear);
        if (cell == null) return ItemStack.EMPTY;
        ItemStack stack = new ItemStack(cell);
        PulseCellItem.setPulse(stack, pulse(gear));
        return stack;
    }

    /** Puts Pulse back: into the piece's own cell first, then any carried cell with room. Returns what fit. */
    public static int refund(Player player, ItemStack gear, int amount) {
        int room = capacity(gear) - pulse(gear), own = Math.min(room, amount);
        if (own > 0) set(gear, cell(gear), pulse(gear) + own);
        int rest = amount - own;
        // Spend reaches the offhand before the hotbar. That cell is not in inventory.items.
        if (rest > 0 && player.getOffhandItem().getItem() instanceof PulseCellItem)
            rest -= PulseCellItem.insertPulse(player.getOffhandItem(), rest, false);
        for (ItemStack stack : player.getInventory().items) {
            if (rest <= 0) break;
            if (stack.getItem() instanceof PulseCellItem) rest -= PulseCellItem.insertPulse(stack, rest, false);
        }
        return amount - rest;
    }

    /** Pulse the piece can reach: its own cell, then the player's cells. */
    public static int available(Player player, ItemStack gear) {
        return pulse(gear) + SpiritgearHelper.availablePulse(player);
    }

    /**
     * Spends {@code amount} for this piece: from its own cell first, and from the player's cells only for what
     * the seated cell could not cover once it runs dry. All or nothing.
     */
    public static boolean spend(Player player, ItemStack gear, int amount) {
        if (amount <= 0 || player.getAbilities().instabuild) return true;
        int own = pulse(gear);
        int fromOwn = Math.min(own, amount);
        int rest = amount - fromOwn;
        if (rest > 0 && !SpiritgearHelper.tryConsumePulse(player, rest)) return false;
        if (fromOwn > 0) set(gear, cell(gear), own - fromOwn);
        return true;
    }

    /** Right-clicking a carried cell onto a piece in any inventory slot seats, swaps or tops up its cell. */
    public static void stackedOn(net.neoforged.neoforge.event.ItemStackedOnOtherEvent event) {
        if (event.getClickAction() != net.minecraft.world.inventory.ClickAction.SECONDARY) return;
        ItemStack gear = event.getStackedOnItem(), carried = event.getCarriedItem();
        if (gear.getCount() != 1 || carried.getCount() != 1 || !event.getSlot().allowModification(event.getPlayer())) return;
        ItemStack left = offer(gear, carried);
        if (left == null) return;
        // Hand the slot its stack back: some modded slots only give out copies.
        event.getSlot().set(gear);
        if (left != carried) event.getCarriedSlotAccess().set(left);
        event.getPlayer().playSound(net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_CHIME, 0.7F, 1.4F);
        event.setCanceled(true);
    }

    /** A piece that breaks gives its seated cell back rather than taking it with it. */
    public static void broken(net.neoforged.neoforge.event.entity.player.PlayerDestroyItemEvent event) {
        ItemStack cell = asStack(event.getOriginal());
        if (cell.isEmpty() || event.getEntity().level().isClientSide) return;
        SpiritgearHelper.give(event.getEntity(), cell);
    }

    // Worn armor breaks through hurtAndBreak, which never fires PlayerDestroyItemEvent. The last piece damaged is
    // remembered with its cell; the ITEM_BROKEN stat that follows a real break (that stack now empty) returns it.
    private record Damaged(ItemStack gear, Item item, ItemStack cell) {}
    private static final java.util.Map<java.util.UUID, Damaged> DAMAGED = new java.util.HashMap<>();

    static void armorDamaged(ItemStack gear, net.minecraft.world.entity.LivingEntity entity) {
        if (!(entity instanceof net.minecraft.server.level.ServerPlayer player)) return;
        ItemStack cell = asStack(gear);
        if (cell.isEmpty()) DAMAGED.remove(player.getUUID());
        else DAMAGED.put(player.getUUID(), new Damaged(gear, gear.getItem(), cell));
    }

    public static void armorBroken(net.neoforged.neoforge.event.StatAwardEvent event) {
        if (!(event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player)
                || event.getStat().getType() != net.minecraft.stats.Stats.ITEM_BROKEN) return;
        Damaged damaged = DAMAGED.get(player.getUUID());
        if (damaged == null || !damaged.gear().isEmpty() || event.getStat().getValue() != damaged.item()) return;
        DAMAGED.remove(player.getUUID());
        SpiritgearHelper.give(player, damaged.cell());
    }

    public static void tooltip(net.neoforged.neoforge.event.entity.player.ItemTooltipEvent event) {
        if (!accepts(event.getItemStack())) return;
        appendTooltip(event.getItemStack(), event.getToolTip());
    }

    public static void appendTooltip(ItemStack gear, List<Component> lines) {
        Item cell = cell(gear);
        if (cell == null) lines.add(Component.translatable("item.tribalpower.gear_cell.none").withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
        else lines.add(Component.translatable("item.tribalpower.gear_cell.seated", cell.getDescription(), pulse(gear), capacity(gear))
                .withStyle(net.minecraft.ChatFormatting.AQUA));
    }
}

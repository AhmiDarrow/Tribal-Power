package tk.darrow.tribalpower.healing;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import tk.darrow.tribalpower.TribalPower;
import tk.darrow.tribalpower.api.pulse.Attunement;
import tk.darrow.tribalpower.block.ModBlocks;
import tk.darrow.tribalpower.blockentity.ResonanceTotemBlockEntity;
import tk.darrow.tribalpower.config.TribalConfig;
import tk.darrow.tribalpower.entity.CreatureProfile;
import tk.darrow.tribalpower.lattice.LatticeNetwork;
import tk.darrow.tribalpower.song.Reagents;

/**
 * The Spirit Kettle. A raw reagent, a March herb and a base brew over heat into a remedy: a glass bottle makes
 * tinctures, honeycomb (the wax) makes salves, charcoal makes incense. The reagent picks the remedy; the voice of
 * the nearest totem shapes it. Each batch takes time and Pulse.
 */
public class SpiritKettleBlockEntity extends BlockEntity implements WorldlyContainer, MenuProvider,
        tk.darrow.tribalpower.api.Diagnosable {
    public static final int REAGENT = 0, HERB = 1, BASE = 2, OUTPUT = 3, SIZE = 4;
    public static final TagKey<Item> HERBS = ItemTags.create(ResourceLocation.fromNamespaceAndPath(TribalPower.MOD_ID, "kettle_herbs"));
    public static final int IDLE = 0, BREWING = 1, NO_PULSE = 2, NO_HEAT = 3, BLOCKED = 4;
    private static final int[] INPUTS = {REAGENT, HERB, BASE}, OUTPUTS = {OUTPUT};

    private final NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
    private int progress;
    private int state;
    /** The voice found on the last beat; the menu reads this rather than scanning the lattice every frame. */
    private int voiceShown;

    private final ContainerData data = new ContainerData() {
        @Override public int get(int index) {
            return switch (index) {
                case 0 -> progress;
                case 1 -> TribalConfig.kettleSeconds();
                case 2 -> state;
                default -> voiceShown;
            };
        }
        @Override public void set(int index, int value) {}
        @Override public int getCount() { return 4; }
    };

    public SpiritKettleBlockEntity(BlockPos pos, BlockState state) {
        super(HealingRegistry.KETTLE_ENTITY.get(), pos, state);
    }

    // ---- what a batch is --------------------------------------------------------------------------------------

    public static @Nullable Remedies.Form form(ItemStack base) {
        if (base.is(Items.GLASS_BOTTLE)) return Remedies.Form.TINCTURE;
        if (base.is(Items.HONEYCOMB)) return Remedies.Form.SALVE;
        if (base.is(Items.CHARCOAL)) return Remedies.Form.INCENSE;
        return null;
    }

    public static int batchSize(Remedies.Form form) {
        return switch (form) {
            case TINCTURE -> TribalConfig.tinctureYield();
            case SALVE -> TribalConfig.salveYield();
            case INCENSE -> TribalConfig.incenseYield();
        };
    }

    /** What the seated inputs brew into, or empty when they brew nothing. */
    public static ItemStack result(ItemStack reagent, ItemStack herb, ItemStack base, @Nullable Attunement voice) {
        CreatureProfile profile = Reagents.of(reagent.getItem());
        Remedies.Form form = form(base);
        if (profile == null || form == null || !herb.is(HERBS)) return ItemStack.EMPTY;
        return Remedies.make(form, profile, voice, batchSize(form));
    }

    /** A lit campfire, fire, magma, lava or an Ember Bowl under the kettle. */
    public static boolean heated(Level level, BlockPos pos) {
        BlockState below = level.getBlockState(pos.below());
        if (below.is(BlockTags.CAMPFIRES)) return below.getValue(CampfireBlock.LIT);
        return below.is(BlockTags.FIRE) || below.is(Blocks.MAGMA_BLOCK) || below.is(ModBlocks.EMBER_BOWL.get())
                || level.getFluidState(pos.below()).is(FluidTags.LAVA);
    }

    /** The voice of the nearest totem in lattice range, if any. */
    public static @Nullable Attunement voice(Level level, BlockPos pos) {
        Attunement best = null;
        double bestDistance = Double.MAX_VALUE;
        for (BlockEntity be : LatticeNetwork.blockEntitiesAround(level, pos, LatticeNetwork.DEFAULT_RADIUS)) {
            if (!(be instanceof ResonanceTotemBlockEntity totem)) continue;
            double distance = be.getBlockPos().distSqr(pos);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = totem.getAttunement();
            }
        }
        return best;
    }

    // ---- brewing ----------------------------------------------------------------------------------------------

    public static void tick(Level level, BlockPos pos, BlockState blockState, SpiritKettleBlockEntity kettle) {
        if ((level.getGameTime() + pos.asLong()) % 20 != 0) return;
        int was = kettle.state;
        kettle.state = kettle.step((ServerLevel) level);
        boolean brewing = kettle.state == BREWING || kettle.state == NO_PULSE;
        if (blockState.getValue(SpiritKettleBlock.BREWING) != brewing)
            level.setBlock(pos, blockState.setValue(SpiritKettleBlock.BREWING, brewing), 3);
        if (was != kettle.state) kettle.setChanged();
    }

    /** One second of brewing. Returns the kettle's state. */
    private int step(ServerLevel level) {
        Attunement voice = voice(level, worldPosition);
        voiceShown = voice == null ? 0 : voice.ordinal() + 1;
        ItemStack made = result(items.get(REAGENT), items.get(HERB), items.get(BASE), voice);
        if (made.isEmpty()) {
            progress = 0;
            return IDLE;
        }
        if (TribalConfig.kettleNeedsHeat() && !heated(level, worldPosition)) return NO_HEAT;
        ItemStack output = items.get(OUTPUT);
        if (!output.isEmpty() && (!ItemStack.isSameItemSameComponents(output, made) || output.getCount() + made.getCount() > output.getMaxStackSize()))
            return BLOCKED;
        if (progress < TribalConfig.kettleSeconds()) {
            progress++;
            setChanged();
            if (progress < TribalConfig.kettleSeconds()) return BREWING;
        }
        int price = TribalConfig.kettlePulse();
        if (price > 0 && LatticeNetwork.extractPulseNearby(level, worldPosition, LatticeNetwork.DEFAULT_RADIUS, price, true) < price) return NO_PULSE;
        if (price > 0) LatticeNetwork.extractPulseNearby(level, worldPosition, LatticeNetwork.DEFAULT_RADIUS, price, false);
        finish(made);
        level.playSound(null, worldPosition, net.minecraft.sounds.SoundEvents.BREWING_STAND_BREW, net.minecraft.sounds.SoundSource.BLOCKS, 0.8F, 0.8F);
        return BREWING;
    }

    private void finish(ItemStack made) {
        items.get(REAGENT).shrink(1);
        items.get(HERB).shrink(1);
        items.get(BASE).shrink(1);
        ItemStack output = items.get(OUTPUT);
        if (output.isEmpty()) items.set(OUTPUT, made);
        else output.grow(made.getCount());
        progress = 0;
        setChanged();
    }

    /** Runs a whole batch at once, for tests and diagnosis: true when it made something. */
    public boolean brewNow(ServerLevel level) {
        progress = TribalConfig.kettleSeconds();
        ItemStack before = items.get(OUTPUT).copy();
        state = step(level);
        return !ItemStack.matches(before, items.get(OUTPUT));
    }

    public int state() { return state; }

    // ---- container --------------------------------------------------------------------------------------------

    @Override public int[] getSlotsForFace(Direction side) { return side == Direction.DOWN ? OUTPUTS : INPUTS; }
    @Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) { return canPlaceItem(slot, stack); }
    @Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) { return slot == OUTPUT; }
    @Override public int getContainerSize() { return SIZE; }
    @Override public boolean isEmpty() { return items.stream().allMatch(ItemStack::isEmpty); }
    @Override public ItemStack getItem(int slot) { return items.get(slot); }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack removed = ContainerHelper.removeItem(items, slot, amount);
        if (!removed.isEmpty()) setChanged();
        return removed;
    }

    @Override public ItemStack removeItemNoUpdate(int slot) { return ContainerHelper.takeItem(items, slot); }

    @Override
    public void setItem(int slot, ItemStack stack) {
        items.set(slot, stack);
        setChanged();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return switch (slot) {
            case REAGENT -> Reagents.of(stack.getItem()) != null;
            case HERB -> stack.is(HERBS);
            case BASE -> form(stack) != null;
            default -> false;
        };
    }

    @Override public boolean stillValid(Player player) { return Container.stillValidBlockEntity(this, player); }
    @Override public void clearContent() { items.clear(); }
    public NonNullList<ItemStack> items() { return items; }

    @Override public Component getDisplayName() { return Component.translatable("block.tribalpower.spirit_kettle"); }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new KettleMenu(id, inventory, this, data);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, items, registries);
        tag.putInt("Progress", progress);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items.clear();
        ContainerHelper.loadAllItems(tag, items, registries);
        progress = tag.getInt("Progress");
    }

    @Override
    public List<Component> diagnose(ServerLevel server, BlockPos pos) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("gui.tribalpower.kettle.state." + state));
        lines.add(Component.translatable(heated(server, pos) ? "gui.tribalpower.kettle.heat.on" : "gui.tribalpower.kettle.heat.off"));
        Attunement voice = voice(server, pos);
        lines.add(voice == null ? Component.translatable("gui.tribalpower.kettle.no_voice")
                : Component.translatable("gui.tribalpower.kettle.voice", Component.translatable("attunement.tribalpower." + voice.getSerializedName())));
        return lines;
    }
}

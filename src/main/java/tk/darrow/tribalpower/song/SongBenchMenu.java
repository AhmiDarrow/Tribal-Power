package tk.darrow.tribalpower.song;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import tk.darrow.tribalpower.api.pulse.Attunement;
import tk.darrow.tribalpower.blockentity.SongBenchBlockEntity;
import tk.darrow.tribalpower.echo.ModMenus;
import tk.darrow.tribalpower.entity.CreatureProfile;

/** Paper, chalk, a songbook, the written page and a weapon to anoint, plus the buttons that spend the pouch. */
public class SongBenchMenu extends AbstractContainerMenu {
    public static final int WIDTH = 248;
    public static final int HEIGHT = 236;
    public static final int INVENTORY_X = 44;
    public static final int INVENTORY_Y = 154;
    public static final int CLEAR = 1, POP = 2, WRITE = 3, LIFT = 4, CYCLE = 5;
    public static final int APPEND = 1000, EMPOWER = 2000, WITHDRAW = 3000, FLETCH = 4000, ANOINT = 5000;
    public static final int WEAPON_X = 180, WEAPON_Y = 126;

    private final Container container;
    private final ContainerData data;
    private final Inventory inventory;
    private final @Nullable SongBenchBlockEntity bench;

    public SongBenchMenu(int id, Inventory inventory) {
        this(id, inventory, new SimpleContainer(SongBenchBlockEntity.SIZE), new SimpleContainerData(9), null);
    }

    public SongBenchMenu(int id, Inventory inventory, SongBenchBlockEntity bench) {
        this(id, inventory, bench, reader(bench), bench);
    }

    private SongBenchMenu(int id, Inventory inventory, Container container, ContainerData data, @Nullable SongBenchBlockEntity bench) {
        super(ModMenus.SONG_BENCH.get(), id);
        this.container = container;
        this.data = data;
        this.inventory = inventory;
        this.bench = bench;
        checkContainerDataCount(data, 9);
        addSlot(new Slot(container, SongBenchBlockEntity.PAPER, 8, 126) {
            @Override public boolean mayPlace(ItemStack stack) { return stack.is(net.minecraft.world.item.Items.PAPER); }
        });
        addSlot(new Slot(container, SongBenchBlockEntity.CHALK, 30, 126) {
            @Override public int getMaxStackSize() { return 1; }
            @Override public boolean mayPlace(ItemStack stack) { return stack.getItem() instanceof tk.darrow.tribalpower.item.RitualChalkItem; }
        });
        addSlot(new Slot(container, SongBenchBlockEntity.BOOK, 52, 126) {
            @Override public int getMaxStackSize() { return 1; }
            @Override public boolean mayPlace(ItemStack stack) { return stack.getItem() instanceof SongbookItem; }
        });
        addSlot(new Slot(container, SongBenchBlockEntity.OUTPUT, 214, 126) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
        });
        addSlot(new Slot(container, SongBenchBlockEntity.WEAPON, WEAPON_X, WEAPON_Y) {
            @Override public int getMaxStackSize() { return 1; }
            @Override public boolean mayPlace(ItemStack stack) { return Anointing.canAnoint(stack); }
        });
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++)
            addSlot(new Slot(inventory, col + row * 9 + 9, INVENTORY_X + col * 18, INVENTORY_Y + row * 18));
        for (int col = 0; col < 9; col++)
            addSlot(new Slot(inventory, col, INVENTORY_X + col * 18, INVENTORY_Y + 58));
        addDataSlots(data);
    }

    public int sequenceLength() { return data.get(0); }

    public @Nullable CreatureProfile reagentAt(int index) {
        if (index < 0 || index >= sequenceLength()) return null;
        int ordinal = data.get(1 + index) - 1;
        CreatureProfile[] values = CreatureProfile.values();
        return ordinal < 0 || ordinal >= values.length ? null : values[ordinal];
    }

    public @Nullable Attunement voice() {
        int ordinal = data.get(8) - 1;
        Attunement[] values = Attunement.values();
        return ordinal < 0 || ordinal >= values.length ? null : values[ordinal];
    }

    private static ContainerData reader(SongBenchBlockEntity bench) {
        return new ContainerData() {
            @Override public int get(int index) {
                if (index == 0) return bench.sequence().size();
                if (index >= 1 && index <= 7) {
                    var sequence = bench.sequence();
                    if (index - 1 >= sequence.size()) return 0;
                    CreatureProfile profile = Reagents.byId(sequence.get(index - 1));
                    return profile == null ? 0 : profile.ordinal() + 1;
                }
                Attunement voice = bench.voice();
                return voice == null ? 0 : voice.ordinal() + 1;
            }
            @Override public void set(int index, int value) {}
            @Override public int getCount() { return 9; }
        };
    }

    @Override
    public boolean stillValid(Player player) {
        return container.stillValid(player);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (bench == null || player.level().isClientSide || !(player.level() instanceof net.minecraft.server.level.ServerLevel level)) return false;
        if (bench.voice() == null) {
            var voices = SongBenchLogic.voices(level, bench.getBlockPos());
            if (!voices.isEmpty()) bench.setVoice(voices.get(0));
        }
        ItemStack pouch = Reagents.hotbarPouch(player);
        SongBenchLogic.Attempt attempt = null;
        if (id == CLEAR) bench.clearSequence();
        else if (id == POP) bench.pop();
        else if (id == CYCLE) cycleVoice();
        else if (id == LIFT) lift(player);
        else if (id == WRITE) attempt = write(level, player, pouch);
        else if (id >= ANOINT) attempt = anoint(level, pouch, id - ANOINT);
        else if (id >= FLETCH) attempt = act(level, player, pouch, id - FLETCH, true);
        else if (id >= WITHDRAW) attempt = withdraw(player, pouch, id - WITHDRAW);
        else if (id >= EMPOWER) attempt = act(level, player, pouch, id - EMPOWER, false);
        else if (id >= APPEND) attempt = append(pouch, id - APPEND);
        if (attempt != null) player.displayClientMessage(attempt.message(), true);
        return true;
    }

    private SongBenchLogic.Attempt append(ItemStack pouch, int ordinal) {
        CreatureProfile profile = profile(ordinal);
        if (profile == null) return SongBenchLogic.Attempt.fail("message.tribalpower.song_bench.need_item");
        if (pouch == null || ReagentPouch.empowered(pouch, profile) <= 0) {
            return SongBenchLogic.Attempt.fail("message.tribalpower.song_bench.need_empowered",
                    net.minecraft.network.chat.Component.translatable("item.tribalpower." + profile.reagent));
        }
        if (!bench.append(profile.reagent)) return SongBenchLogic.Attempt.fail("message.tribalpower.song_bench.sequence_full");
        return null;
    }

    private SongBenchLogic.Attempt act(net.minecraft.server.level.ServerLevel level, Player player, ItemStack pouch, int ordinal, boolean fletch) {
        CreatureProfile profile = profile(ordinal);
        if (profile == null) return SongBenchLogic.Attempt.fail("message.tribalpower.song_bench.need_item");
        if (!fletch) return SongBenchLogic.empower(level, bench.getBlockPos(), bench, pouch, profile);
        var attempt = SongBenchLogic.fletch(level, bench.getBlockPos(), bench, pouch, profile, bench.voice(), bench.getItem(SongBenchBlockEntity.OUTPUT));
        if (attempt.ok() && !attempt.made().isEmpty()) bench.setItem(SongBenchBlockEntity.OUTPUT, attempt.made());
        return attempt;
    }

    private SongBenchLogic.Attempt anoint(net.minecraft.server.level.ServerLevel level, ItemStack pouch, int ordinal) {
        CreatureProfile profile = profile(ordinal);
        if (profile == null) return SongBenchLogic.Attempt.fail("message.tribalpower.song_bench.need_item");
        var attempt = SongBenchLogic.anoint(level, bench.getBlockPos(), bench, pouch, profile, bench.getItem(SongBenchBlockEntity.WEAPON));
        if (attempt.ok()) bench.setChanged();
        return attempt;
    }

    /** With nothing sequenced, Write binds the sheet in the output into the seated book: Lift, undone. */
    private SongBenchLogic.Attempt bindSheet() {
        ItemStack sheet = bench.getItem(SongBenchBlockEntity.OUTPUT);
        ItemStack book = bench.getItem(SongBenchBlockEntity.BOOK);
        SongVerse verse = SongSheetItem.verse(sheet);
        if (verse == null || !(book.getItem() instanceof SongbookItem songbook)) return null;
        if (!SongPages.add(book, songbook.tier(), verse)) return SongBenchLogic.Attempt.fail("message.tribalpower.song_bench.book_full");
        sheet.shrink(1);
        if (sheet.isEmpty()) bench.setItem(SongBenchBlockEntity.OUTPUT, ItemStack.EMPTY);
        bench.setChanged();
        return SongBenchLogic.Attempt.done("message.tribalpower.song_bench.bound", verse.name());
    }

    private SongBenchLogic.Attempt write(net.minecraft.server.level.ServerLevel level, Player player, ItemStack pouch) {
        if (bench.sequence().isEmpty()) {
            SongBenchLogic.Attempt bound = bindSheet();
            if (bound != null) return bound;
        }
        var attempt = SongBenchLogic.write(level, bench.getBlockPos(), bench, player, pouch, bench.sequence(), bench.voice(),
                bench.getItem(SongBenchBlockEntity.PAPER), bench.getItem(SongBenchBlockEntity.CHALK),
                bench.getItem(SongBenchBlockEntity.BOOK), bench.getItem(SongBenchBlockEntity.OUTPUT));
        if (attempt.ok()) {
            if (!attempt.made().isEmpty()) bench.setItem(SongBenchBlockEntity.OUTPUT, attempt.made());
            bench.clearSequence();
        }
        return attempt;
    }

    private SongBenchLogic.Attempt withdraw(Player player, ItemStack pouch, int ordinal) {
        CreatureProfile profile = profile(ordinal);
        if (profile == null || pouch == null) return SongBenchLogic.Attempt.fail("message.tribalpower.song_bench.no_pouch");
        int room = Reagents.inventoryRoom(player, profile);
        if (room <= 0) {
            return SongBenchLogic.Attempt.fail(ReagentPouch.raw(pouch, profile) > 0
                    ? "message.tribalpower.song_bench.no_room"
                    : "message.tribalpower.song_bench.no_raw");
        }
        int taken = ReagentPouch.takeRaw(pouch, profile, room);
        if (taken <= 0) return SongBenchLogic.Attempt.fail("message.tribalpower.song_bench.no_raw");
        PouchMenu.give(player, new ItemStack(Reagents.item(profile), taken));
        return SongBenchLogic.Attempt.done("message.tribalpower.song_bench.taken", taken,
                net.minecraft.network.chat.Component.translatable("item.tribalpower." + profile.reagent));
    }

    private void lift(Player player) {
        ItemStack book = bench.getItem(SongBenchBlockEntity.BOOK);
        if (!(book.getItem() instanceof SongbookItem)) return;
        if (!bench.getItem(SongBenchBlockEntity.OUTPUT).isEmpty()) {
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.tribalpower.song_bench.output_full"), true);
            return;
        }
        SongVerse verse = SongPages.removeOpen(book);
        if (verse == null) return;
        bench.setItem(SongBenchBlockEntity.OUTPUT, SongSheetItem.create(verse));
        bench.setChanged();
    }

    private void cycleVoice() {
        var voices = SongBenchLogic.voices(bench.getLevel(), bench.getBlockPos());
        if (voices.isEmpty()) {
            bench.setVoice(null);
            return;
        }
        int index = bench.voice() == null ? -1 : voices.indexOf(bench.voice());
        bench.setVoice(voices.get((index + 1) % voices.size()));
    }

    private static CreatureProfile profile(int ordinal) {
        CreatureProfile[] values = CreatureProfile.values();
        return ordinal < 0 || ordinal >= values.length ? null : values[ordinal];
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index < SongBenchBlockEntity.SIZE) {
            if (!moveItemStackTo(stack, SongBenchBlockEntity.SIZE, slots.size(), true)) return ItemStack.EMPTY;
        } else if (Anointing.canAnoint(stack) && !slots.get(SongBenchBlockEntity.WEAPON).hasItem()) {
            if (!moveItemStackTo(stack, SongBenchBlockEntity.WEAPON, SongBenchBlockEntity.WEAPON + 1, false)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(stack, 0, SongBenchBlockEntity.OUTPUT, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        return original;
    }
}

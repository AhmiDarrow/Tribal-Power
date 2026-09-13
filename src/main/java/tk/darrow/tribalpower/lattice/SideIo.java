package tk.darrow.tribalpower.lattice;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

/**
 * Per-face insert/extract for Tribal machines. Hoppers and relays honour this; the station UI cycles it.
 */
public final class SideIo {
    public enum Mode {
        BOTH, INPUT, OUTPUT, NONE;

        public boolean insert() { return this == BOTH || this == INPUT; }
        public boolean extract() { return this == BOTH || this == OUTPUT; }
        public Mode next() { return values()[(ordinal() + 1) % values().length]; }
        public String key() { return "gui.tribalpower.io." + name().toLowerCase(); }
        public Component label() { return Component.translatable(key()); }
    }

    private static final int[] EMPTY = new int[0];
    private int packed;

    public SideIo(Mode fill) {
        int bits = fill.ordinal();
        for (int i = 0; i < 6; i++) packed |= bits << (i * 2);
    }

    public static SideIo station() {
        SideIo io = new SideIo(Mode.INPUT);
        io.set(Direction.DOWN, Mode.OUTPUT);
        return io;
    }

    public static SideIo mesh() {
        SideIo io = new SideIo(Mode.INPUT);
        io.set(Direction.DOWN, Mode.OUTPUT);
        return io;
    }

    public Mode get(Direction face) {
        return Mode.values()[(packed >> (face.get3DDataValue() * 2)) & 3];
    }

    public void set(Direction face, Mode mode) {
        int shift = face.get3DDataValue() * 2;
        packed = (packed & ~(3 << shift)) | (mode.ordinal() << shift);
    }

    public Mode cycle(Direction face) {
        Mode next = get(face).next();
        set(face, next);
        return next;
    }

    public int pack() { return packed; }

    public void unpack(int value) { packed = value; }

    public void save(CompoundTag tag) { tag.putInt("SideIo", packed); }

    public void load(CompoundTag tag) { if (tag.contains("SideIo")) packed = tag.getInt("SideIo"); }

    public static int[] slots(HasSideIo io, Direction face) {
        Mode mode = io.sideIo().get(face);
        return switch (mode) {
            case NONE -> EMPTY;
            case INPUT -> io.inputSlots(face);
            case OUTPUT -> io.outputSlots(face);
            case BOTH -> concat(io.inputSlots(face), io.outputSlots(face));
        };
    }

    private static int[] concat(int[] a, int[] b) {
        if (a.length == 0) return b;
        if (b.length == 0 || a == b) return a;
        int max = 0;
        for (int v : a) max = Math.max(max, v);
        for (int v : b) max = Math.max(max, v);
        boolean[] seen = new boolean[max + 1];
        int[] tmp = new int[a.length + b.length];
        int n = 0;
        for (int v : a) if (v >= 0 && !seen[v]) { seen[v] = true; tmp[n++] = v; }
        for (int v : b) if (v >= 0 && !seen[v]) { seen[v] = true; tmp[n++] = v; }
        return java.util.Arrays.copyOf(tmp, n);
    }
}

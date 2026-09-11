package tk.darrow.tribalpower.blockentity;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import tk.darrow.tribalpower.api.pulse.Attunement;
import tk.darrow.tribalpower.config.TribalConfig;
import tk.darrow.tribalpower.effect.SpiritEffects;
import tk.darrow.tribalpower.lattice.LatticeNetwork;
import tk.darrow.tribalpower.pattern.ModPatterns;
import tk.darrow.tribalpower.pattern.PatternMatcher;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * "Stone is not made. Stone is asked to stay." The starter rite, and the tutorial for patterns
 * (design 3.1 section 6).
 *
 * <p>Runs on a hand-drummed Drumheart at tier 1, so a player learns chalk, shape and Pulse draw before
 * they own anything. Which tier it can reach is a question of shape first and voices second.
 *
 * <p>Obsidian has two doors. Fire and Water voices together keep the Pulse-taught path (24 Pulse/s).
 * A braced font that is fed 250 mB water and 250 mB lava instead asks at 6 Pulse/s — the grounded
 * alternative from design 3.1 section 6, so fluid transport has a job here too.
 *
 * <p>Design note: the design gives the cobble tier "deepslate below y 0". Read here as the deepslate band
 * producing the deepslate analogue of each tier's stone, so tier 1 yields cobbled deepslate and tier 2
 * deepslate -- otherwise tier 2 would be a downgrade underground.
 */
public class StoneFontBlockEntity extends LatticeDeviceBlockEntity {
    public static final int SLOTS = 4;

    /** What the font can ask for, in the order it is asked. */
    public enum Ask {
        COBBLE(1, 2, 4),
        STONE(2, 3, 8),
        OBSIDIAN(2, 12, 24);

        private final int patternTier;
        private final int seconds;
        private final int pulsePerSecond;

        Ask(int patternTier, int seconds, int pulsePerSecond) {
            this.patternTier = patternTier;
            this.seconds = seconds;
            this.pulsePerSecond = pulsePerSecond;
        }

        public int patternTier() { return patternTier; }
        public int seconds() { return Math.max(1, (int) Math.round(seconds / TribalConfig.pitSpeedMultiplier())); }
        public int pulsePerSecond() { return pulsePerSecond(false); }
        public int pulsePerSecond(boolean grounded) {
            return this == OBSIDIAN && grounded ? 6 : pulsePerSecond;
        }
        public String key() { return name().toLowerCase(java.util.Locale.ROOT); }

        /** The block this tier settles, in the stone band the font stands in. */
        public ItemStack result(BlockPos pos) {
            boolean deep = pos.getY() < 0;
            return switch (this) {
                case COBBLE -> new ItemStack(deep ? Blocks.COBBLED_DEEPSLATE : Blocks.COBBLESTONE);
                case STONE -> new ItemStack(deep ? Blocks.DEEPSLATE : Blocks.STONE);
                case OBSIDIAN -> new ItemStack(Blocks.OBSIDIAN);
            };
        }
    }

    public static final int TANK_CAPACITY = 4000;
    public static final int GROUND_COST = 250;

    private int work;
    private String state = "idle";
    private Ask asking = Ask.COBBLE;
    private boolean grounded;

    public final FluidTank water = new FluidTank(TANK_CAPACITY, stack -> stack.getFluid() == Fluids.WATER) {
        @Override public int fill(FluidStack resource, FluidAction action) {
            return stilled() ? 0 : super.fill(resource, action);
        }
        @Override protected void onContentsChanged() { setChanged(); }
    };
    public final FluidTank lava = new FluidTank(TANK_CAPACITY, stack -> stack.getFluid() == Fluids.LAVA) {
        @Override public int fill(FluidStack resource, FluidAction action) {
            return stilled() ? 0 : super.fill(resource, action);
        }
        @Override protected void onContentsChanged() { setChanged(); }
    };
    public final IFluidHandler fluids = new IFluidHandler() {
        @Override public int getTanks() { return 2; }
        @Override public FluidStack getFluidInTank(int tank) {
            return tank == 0 ? water.getFluid() : lava.getFluid();
        }
        @Override public int getTankCapacity(int tank) { return TANK_CAPACITY; }
        @Override public boolean isFluidValid(int tank, FluidStack stack) {
            return tank == 0 ? stack.getFluid() == Fluids.WATER : stack.getFluid() == Fluids.LAVA;
        }
        @Override public int fill(FluidStack resource, FluidAction action) {
            if (resource.getFluid() == Fluids.WATER) return water.fill(resource, action);
            if (resource.getFluid() == Fluids.LAVA) return lava.fill(resource, action);
            return 0;
        }
        @Override public FluidStack drain(FluidStack resource, FluidAction action) {
            if (resource.getFluid() == Fluids.WATER) return water.drain(resource, action);
            if (resource.getFluid() == Fluids.LAVA) return lava.drain(resource, action);
            return FluidStack.EMPTY;
        }
        @Override public FluidStack drain(int maxDrain, FluidAction action) {
            if (!water.isEmpty()) return water.drain(maxDrain, action);
            return lava.drain(maxDrain, action);
        }
    };

    public StoneFontBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.STONE_FONT.get(), pos, state, SLOTS, ModPatterns.STONE_FONT);
    }

    @Override protected boolean isOutputSlot(int slot) { return true; }
    @Override public boolean canPlaceItem(int slot, ItemStack stack) { return false; }
    @Override public int[] getSlotsForFace(Direction face) { return new int[]{0, 1, 2, 3}; }

    public int work() { return work; }
    public Ask asking() { return asking; }
    public boolean grounded() { return grounded; }

    /** Comparator output: progress through the current cycle, 0 when nothing is being asked. */
    public int progressSignal() {
        int seconds = asking.seconds();
        if (!"working".equals(state) || seconds <= 0) return 0;
        return Math.max(1, Math.min(15, 1 + 14 * work / seconds));
    }

    public Component status() {
        return Component.translatable("message.tribalpower.font." + state,
                Component.translatable("message.tribalpower.font.ask." + asking.key()), work, asking.seconds());
    }

    /** The best tier this font can reach right now, given its shape and the voices around it. */
    public Ask best(Level level, BlockPos pos) {
        int tier = pattern.tier(level, pos);
        if (tier <= 0) {
            grounded = false;
            return null;
        }
        Set<Attunement> voices = LatticeNetwork.collectAttunements(level, pos, LatticeNetwork.DEFAULT_RADIUS);
        boolean earth = voices.contains(Attunement.EARTH);
        boolean voicesOpen = tier >= Ask.OBSIDIAN.patternTier() && earth
                && voices.contains(Attunement.FIRE) && voices.contains(Attunement.WATER);
        boolean tanksOpen = tier >= Ask.OBSIDIAN.patternTier() && earth
                && water.getFluidAmount() >= GROUND_COST && lava.getFluidAmount() >= GROUND_COST;
        grounded = tanksOpen && !voicesOpen;
        if (voicesOpen || tanksOpen) return Ask.OBSIDIAN;
        if (tier >= Ask.STONE.patternTier() && earth) return Ask.STONE;
        return Ask.COBBLE;
    }

    public static void tick(Level level, BlockPos pos, BlockState blockState, StoneFontBlockEntity be) {
        if ((level.getGameTime() + pos.asLong()) % 20 != 0) return;
        if (be.stilled()) { be.state = "paused"; return; }

        Ask ask = be.best(level, pos);
        if (ask == null) { be.reset("pattern"); return; }
        if (ask != be.asking) { be.asking = ask; be.work = 0; }

        ItemStack result = ask.result(pos);
        if (!be.placeOutput(result, true)) { be.state = "full"; return; }

        int cost = ask.pulsePerSecond(be.grounded);
        if (LatticeNetwork.extractPulseNearby(level, pos, LatticeNetwork.DEFAULT_RADIUS, cost, true) < cost) {
            be.state = "pulse";
            return;
        }
        LatticeNetwork.extractPulseNearby(level, pos, LatticeNetwork.DEFAULT_RADIUS, cost, false);
        be.state = "working";
        be.work++;
        SpiritEffects.ring((ServerLevel) level, pos.getCenter().add(0, 0.6, 0), Attunement.EARTH, 0.4, 8);
        if (be.work >= ask.seconds()) {
            if (ask == Ask.OBSIDIAN && be.grounded) {
                if (be.water.getFluidAmount() < GROUND_COST || be.lava.getFluidAmount() < GROUND_COST) {
                    be.state = "fluid";
                    return;
                }
                be.water.drain(GROUND_COST, IFluidHandler.FluidAction.EXECUTE);
                be.lava.drain(GROUND_COST, IFluidHandler.FluidAction.EXECUTE);
            }
            be.placeOutput(result, false);
            be.work = 0;
            tk.darrow.tribalpower.camp.CampHooks.award((ServerLevel) level, be.owner(), "journey/circle");
            tk.darrow.tribalpower.camp.CampHooks.award((ServerLevel) level, be.owner(), "journey/stone_stays");
            tk.darrow.tribalpower.sound.ModSounds.play(level, pos,
                    tk.darrow.tribalpower.sound.ModSounds.FONT_FORM, 0.5F, 1.05F);
        }
        be.setChanged();
        level.updateNeighbourForOutputSignal(pos, blockState.getBlock());
    }

    private void reset(String why) {
        work = 0;
        state = why;
        setChanged();
    }

    @Override
    public List<Component> diagnose(ServerLevel server, BlockPos pos) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("diag.tribalpower.font.state", status()));
        PatternMatcher.Match match = pattern.get(server, pos);
        lines.addAll(match.report(3));
        if (!match.found()) return lines;
        Ask ask = best(server, pos);
        lines.add(Component.translatable("diag.tribalpower.font.tanks",
                water.getFluidAmount(), lava.getFluidAmount()));
        if (ask != null) {
            lines.add(Component.translatable("diag.tribalpower.font.ask",
                    Component.translatable("message.tribalpower.font.ask." + ask.key()),
                    ask.seconds(), ask.pulsePerSecond(grounded)));
            if (ask == Ask.OBSIDIAN && grounded)
                lines.add(Component.translatable("diag.tribalpower.font.grounded").withStyle(ChatFormatting.GRAY));
            if (ask != Ask.OBSIDIAN)
                lines.add(Component.translatable("diag.tribalpower.font.next." + ask.key()).withStyle(ChatFormatting.GRAY));
        }
        return lines;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Work", work);
        tag.putString("Ask", asking.key());
        tag.putString("State", state);
        tag.putBoolean("Grounded", grounded);
        tag.put("Water", water.writeToNBT(registries, new CompoundTag()));
        tag.put("Lava", lava.writeToNBT(registries, new CompoundTag()));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        work = Math.max(0, tag.getInt("Work"));
        if (tag.contains("State")) state = tag.getString("State");
        grounded = tag.getBoolean("Grounded");
        for (Ask ask : Ask.values()) if (ask.key().equals(tag.getString("Ask"))) asking = ask;
        water.readFromNBT(registries, tag.getCompound("Water"));
        lava.readFromNBT(registries, tag.getCompound("Lava"));
    }
}

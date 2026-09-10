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
 * <p>Design note: the design offers a grounded obsidian variant costing water and lava instead of Pulse.
 * Taken here as the Pulse version, because the Font's whole job is to teach patterns and Pulse draw to a
 * player who has no fluid transport yet; fluid arrives with the Mesh and the Wave Drum.
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
        public int pulsePerSecond() { return pulsePerSecond; }
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

    private int work;
    private String state = "idle";
    private Ask asking = Ask.COBBLE;

    public StoneFontBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.STONE_FONT.get(), pos, state, SLOTS, ModPatterns.STONE_FONT);
    }

    @Override protected boolean isOutputSlot(int slot) { return true; }
    @Override public boolean canPlaceItem(int slot, ItemStack stack) { return false; }
    @Override public int[] getSlotsForFace(Direction face) { return new int[]{0, 1, 2, 3}; }

    public int work() { return work; }
    public Ask asking() { return asking; }

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
        if (tier <= 0) return null;
        Set<Attunement> voices = LatticeNetwork.collectAttunements(level, pos, LatticeNetwork.DEFAULT_RADIUS);
        if (tier >= Ask.OBSIDIAN.patternTier() && voices.contains(Attunement.EARTH)
                && voices.contains(Attunement.FIRE) && voices.contains(Attunement.WATER)) return Ask.OBSIDIAN;
        if (tier >= Ask.STONE.patternTier() && voices.contains(Attunement.EARTH)) return Ask.STONE;
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

        int cost = ask.pulsePerSecond();
        if (LatticeNetwork.extractPulseNearby(level, pos, LatticeNetwork.DEFAULT_RADIUS, cost, true) < cost) {
            be.state = "pulse";
            return;
        }
        LatticeNetwork.extractPulseNearby(level, pos, LatticeNetwork.DEFAULT_RADIUS, cost, false);
        be.state = "working";
        be.work++;
        SpiritEffects.ring((ServerLevel) level, pos.getCenter().add(0, 0.6, 0), Attunement.EARTH, 0.4, 8);
        if (be.work >= ask.seconds()) {
            be.placeOutput(result, false);
            be.work = 0;
            tk.darrow.tribalpower.camp.CampHooks.award((ServerLevel) level, be.owner(), "journey/circle");
            tk.darrow.tribalpower.camp.CampHooks.award((ServerLevel) level, be.owner(), "journey/stone_stays");
            level.playSound(null, pos, net.minecraft.sounds.SoundEvents.STONE_PLACE,
                    net.minecraft.sounds.SoundSource.BLOCKS, 0.5F, 1.1F);
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
        if (ask != null) {
            lines.add(Component.translatable("diag.tribalpower.font.ask",
                    Component.translatable("message.tribalpower.font.ask." + ask.key()),
                    ask.seconds(), ask.pulsePerSecond()));
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
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        work = Math.max(0, tag.getInt("Work"));
        for (Ask ask : Ask.values()) if (ask.key().equals(tag.getString("Ask"))) asking = ask;
    }
}

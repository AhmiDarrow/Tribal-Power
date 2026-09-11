package tk.darrow.tribalpower.blockentity;

import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import tk.darrow.tribalpower.api.pulse.Attunement;
import tk.darrow.tribalpower.effect.SpiritEffects;
import tk.darrow.tribalpower.item.ModItems;
import tk.darrow.tribalpower.item.PulseCellItem;
import tk.darrow.tribalpower.lattice.LatticeNetwork;

public class RitualBrazierBlockEntity extends BlockEntity implements tk.darrow.tribalpower.api.Diagnosable {
    /** Where the Rite Circle seats its pedestals, as offsets from the brazier. Rotation-invariant as a set. */
    private static final int[][] PEDESTALS = {{2, 2}, {2, -2}, {-2, 2}, {-2, -2}};

    private ItemStack seal = ItemStack.EMPTY;
    private boolean active;
    private boolean lastSignal;
    public RitualBrazierBlockEntity(BlockPos pos, BlockState state) { super(ModBlockEntities.RITUAL_BRAZIER.get(), pos, state); }
    public ItemStack seal() { return seal; }
    public void setSeal(ItemStack stack) { seal = stack; active = false; setChanged(); }
    public Component status() {
        if (active && element(seal) == Attunement.LOOM) return Component.translatable("message.tribalpower.brazier.tension");
        return Component.translatable("message.tribalpower.brazier." + (seal.isEmpty() ? "empty" : active ? "active" : "waiting"));
    }
    public static Attunement element(ItemStack seal) {
        if (seal.is(ModItems.EARTH_SEAL.get())) return Attunement.EARTH;
        if (seal.is(ModItems.FIRE_SEAL.get())) return Attunement.FIRE;
        if (seal.is(ModItems.WATER_SEAL.get())) return Attunement.WATER;
        if (seal.is(ModItems.AIR_SEAL.get())) return Attunement.AIR;
        if (seal.is(ModItems.SPIRIT_SEAL.get())) return Attunement.SPIRIT;
        if (seal.is(ModItems.LOOM_SEAL.get())) return Attunement.LOOM;
        return null;
    }
    /**
     * A struck signal fires the seated rite once (design 3.1 section 2). It looks for a Rite Tablet on one
     * of the circle's pedestals matching the seated seal, and consumes it on success -- which is what makes
     * the whole loop buildable: a relay restocks a pedestal, a clock strikes the brazier, the rite fires,
     * and a comparator reports the pedestal empty.
     */
    /**
     * A struck signal is an edge. A brazier placed beside a lit redstone torch has to start from the
     * signal that is already there, or the first unrelated neighbour update fires the seated rite and
     * eats a Rite Tablet nobody asked for.
     */
    public void seedSignal(net.minecraft.world.level.Level level) {
        lastSignal = level.hasNeighborSignal(worldPosition);
        setChanged();
    }

    public void onRedstoneChanged(ServerLevel level) {
        boolean signal = level.hasNeighborSignal(worldPosition);
        boolean rising = signal && !lastSignal;
        lastSignal = signal;
        setChanged();
        if (rising) strike(level);
    }

    /** Fires the seated rite from a stocked pedestal. Returns null on success, else why not. */
    public Component strike(ServerLevel level) {
        Attunement element = element(seal);
        if (element == null) return Component.translatable("message.tribalpower.brazier.no_seal");
        for (int[] offset : PEDESTALS) {
            BlockPos pedestalPos = worldPosition.offset(offset[0], 0, offset[1]);
            if (!level.hasChunkAt(pedestalPos)) continue;
            if (!(level.getBlockEntity(pedestalPos) instanceof tk.darrow.tribalpower.blockentity.RitePedestalBlockEntity pedestal)) continue;
            ItemStack held = pedestal.held();
            if (!(held.getItem() instanceof tk.darrow.tribalpower.rite.world.RiteTabletItem tablet)) continue;
            if (tablet.rite().element() != element) continue;
            Component failure = tk.darrow.tribalpower.rite.world.RiteTabletItem.perform(level, worldPosition, null, tablet.rite());
            if (failure != null) return failure;
            pedestal.removeItem(tk.darrow.tribalpower.blockentity.RitePedestalBlockEntity.SLOT, 1);
            return null;
        }
        return Component.translatable("message.tribalpower.brazier.no_tablet");
    }

    public static void tick(Level level, BlockPos pos, BlockState state, RitualBrazierBlockEntity be) {
        if ((level.getGameTime() + pos.asLong()) % 40 != 0) return;
        Attunement element = element(be.seal);
        be.active = false;
        if (element == null || level.hasNeighborSignal(pos)) return;
        var players = level.getEntitiesOfClass(Player.class, new AABB(pos).inflate(6), p -> p.isAlive() && !p.isSpectator());
        if (players.isEmpty() || !LatticeNetwork.hasAttunement(level, pos, 8, element)
                || LatticeNetwork.extractPulseNearby(level, pos, 8, 8, true) < 8) return;
        LatticeNetwork.extractPulseNearby(level, pos, 8, 8, false);
        be.active = true;
        var effect = switch(element) {
            case EARTH -> MobEffects.DIG_SPEED;
            case FIRE -> MobEffects.FIRE_RESISTANCE;
            case WATER -> MobEffects.REGENERATION;
            case AIR -> MobEffects.SLOW_FALLING;
            case SPIRIT -> MobEffects.NIGHT_VISION;
            case LOOM -> MobEffects.LUCK;
        };
        for (Player player : players) {
            player.addEffect(new MobEffectInstance(effect, element == Attunement.SPIRIT ? 300 : 100, 0, true, false, true));
            if (element == Attunement.LOOM) tension(player);
        }
        SpiritEffects.ring((ServerLevel)level, pos.getCenter().add(0, 0.35, 0), element, 1.2, 16);
    }
    /** Loom blessing "Tension": every beat, thread 2 Pulse back into one carried cell with room. */
    public static void tension(Player player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() instanceof PulseCellItem && PulseCellItem.insertPulse(stack, TENSION_PULSE, false) > 0) return;
        }
    }
    public static final int TENSION_PULSE = 2;
    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!seal.isEmpty()) tag.put("Seal", seal.save(registries));
        tag.putBoolean("LastSignal", lastSignal);
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        seal = ItemStack.parseOptional(registries, tag.getCompound("Seal")); active = false;
        lastSignal = tag.getBoolean("LastSignal");
    }

    @Override public java.util.List<Component> diagnose(ServerLevel server, BlockPos pos) {
        java.util.List<Component> lines = new java.util.ArrayList<>();
        lines.add(Component.translatable("diag.tribalpower.brazier.state", status()));
        Attunement element = element(seal);
        if (element == null) { lines.add(Component.translatable("diag.tribalpower.brazier.no_seal").withStyle(net.minecraft.ChatFormatting.YELLOW)); return lines; }
        lines.add(Component.translatable("diag.tribalpower.brazier.seal", seal.getHoverName(), Component.translatable("attunement.tribalpower." + element.getSerializedName())));
        if (!LatticeNetwork.hasAttunement(server, pos, 8, element))
            lines.add(Component.translatable("diag.tribalpower.station.missing_attunement", Component.translatable("attunement.tribalpower." + element.getSerializedName())).withStyle(net.minecraft.ChatFormatting.YELLOW));
        int players = server.getEntitiesOfClass(Player.class, new AABB(pos).inflate(6), p -> p.isAlive() && !p.isSpectator()).size();
        lines.add(Component.translatable("diag.tribalpower.brazier.players", players));
        var circle = tk.darrow.tribalpower.rite.world.RiteCircle.evaluate(server, pos, element);
        lines.addAll(circle.report());
        if (circle.complete() && circle.tier() >= 2)
            lines.add(Component.translatable("diag.tribalpower.brazier.circle_tier2").withStyle(net.minecraft.ChatFormatting.GREEN));
        for (var rite : tk.darrow.tribalpower.rite.world.WorldRite.values())
            if (rite.element() == element) lines.add(Component.translatable("diag.tribalpower.brazier.rite",
                    Component.translatable("item.tribalpower." + rite.tabletId()), circle.cost(rite.cost())));
        return lines;
    }
}

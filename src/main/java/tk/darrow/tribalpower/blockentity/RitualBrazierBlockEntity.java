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

public class RitualBrazierBlockEntity extends BlockEntity implements tk.darrow.tribalpower.api.Diagnosable, tk.darrow.tribalpower.api.pulse.PulseSpend {
    /** Where the Rite Circle seats its pedestals, as offsets from the brazier. Rotation-invariant as a set. */
    private static final int[][] PEDESTALS = {{2, 2}, {2, -2}, {-2, 2}, {-2, -2}};

    private ItemStack seal = ItemStack.EMPTY;
    /** Incense seated to burn, and how many beats the stick now burning has given. */
    private ItemStack incense = ItemStack.EMPTY;
    private int burned;
    private boolean active;
    private boolean lastSignal;
    public RitualBrazierBlockEntity(BlockPos pos, BlockState state) { super(ModBlockEntities.RITUAL_BRAZIER.get(), pos, state); }
    /** Eight Pulse every two seconds while a blessing is up, reported as four a second. */
    @Override
    public int spendPerSecond() {
        if (level == null || tk.darrow.tribalpower.familiar.SpiritClickBlock.hearsRealSignal(level, worldPosition)) return 0;
        Attunement voice = element(seal);
        if (voice == null) return 0;
        if (level.getEntitiesOfClass(Player.class, new AABB(worldPosition).inflate(6), p -> p.isAlive() && !p.isSpectator()).isEmpty()) return 0;
        if (!LatticeNetwork.hasAttunement(level, worldPosition, 8, voice)) return 0;
        return 4;
    }

    public ItemStack seal() { return seal; }
    public ItemStack incense() { return incense; }

    /** Seats incense: a stack of one brew at a time, up to sixteen sticks. Returns how many were taken. */
    public int addIncense(ItemStack stack) {
        if (!incense.isEmpty() && !ItemStack.isSameItemSameComponents(incense, stack)) return 0;
        int room = 16 - incense.getCount();
        int taken = Math.min(room, stack.getCount());
        if (taken <= 0) return 0;
        if (incense.isEmpty()) incense = stack.copyWithCount(taken);
        else incense.grow(taken);
        setChanged();
        return taken;
    }

    public ItemStack takeIncense() {
        ItemStack out = incense;
        incense = ItemStack.EMPTY;
        burned = 0;
        setChanged();
        return out;
    }

    /**
     * One beat of burning incense: its remedy reaches every player and bonded familiar within the incense radius,
     * and familiars are mended a little too. Each stick burns for a set number of beats. Redstone silences it.
     */
    public void burnIncense(ServerLevel level) {
        if (incense.isEmpty() || tk.darrow.tribalpower.familiar.SpiritClickBlock.hearsRealSignal(level, worldPosition)) return;
        int radius = tk.darrow.tribalpower.config.TribalConfig.incenseRadius();
        for (var body : level.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class, new AABB(worldPosition).inflate(radius),
                e -> e.isAlive() && !e.isSpectator() && (e instanceof Player
                        || e instanceof tk.darrow.tribalpower.familiar.Familiar familiar && familiar.isBonded()))) {
            tk.darrow.tribalpower.healing.Remedies.breathe(body, incense);
            if (body instanceof tk.darrow.tribalpower.familiar.Familiar)
                body.heal((float) tk.darrow.tribalpower.config.TribalConfig.incenseFamiliarHeal());
        }
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.CAMPFIRE_COSY_SMOKE, worldPosition.getX() + 0.5, worldPosition.getY() + 1.1,
                worldPosition.getZ() + 0.5, 3, 0.15, 0.1, 0.15, 0.01);
        if (++burned >= tk.darrow.tribalpower.config.TribalConfig.incenseBeats()) {
            burned = 0;
            incense.shrink(1);
            if (incense.isEmpty()) incense = ItemStack.EMPTY;
        }
        setChanged();
    }
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
        lastSignal = tk.darrow.tribalpower.familiar.SpiritClickBlock.hearsRealSignal(level, worldPosition);
        setChanged();
    }

    public void onRedstoneChanged(ServerLevel level) {
        boolean signal = tk.darrow.tribalpower.familiar.SpiritClickBlock.hearsRealSignal(level, worldPosition);
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
        be.burnIncense((ServerLevel) level);
        Attunement element = element(be.seal);
        be.active = false;
        if (element == null || tk.darrow.tribalpower.familiar.SpiritClickBlock.hearsRealSignal(level, pos)) return;
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
        if (!incense.isEmpty()) tag.put("Incense", incense.save(registries));
        tag.putInt("Burned", burned);
        tag.putBoolean("LastSignal", lastSignal);
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        seal = ItemStack.parseOptional(registries, tag.getCompound("Seal")); active = false;
        incense = ItemStack.parseOptional(registries, tag.getCompound("Incense"));
        burned = tag.getInt("Burned");
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

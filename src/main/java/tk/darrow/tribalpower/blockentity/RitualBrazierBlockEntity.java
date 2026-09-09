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

public class RitualBrazierBlockEntity extends BlockEntity {
    private ItemStack seal = ItemStack.EMPTY;
    private boolean active;
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
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        seal = ItemStack.parseOptional(registries, tag.getCompound("Seal")); active = false;
    }
}

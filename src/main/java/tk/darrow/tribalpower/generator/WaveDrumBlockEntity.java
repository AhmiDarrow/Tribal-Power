package tk.darrow.tribalpower.generator;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

import java.util.List;

/**
 * The Rootbinders' craft (design 3.1 section 9.1), and the second job for fluid transport.
 *
 * <p>Three a second standing beside water and sharing it with any drum within eight blocks; ten a second
 * drinking {@link WaveMath#PIPED_COST} mB a second from its own tank, which nothing crowds because the
 * water supply is already the limit. Doubled in the rain either way.
 */
public class WaveDrumBlockEntity extends GeneratorBlockEntity {
    public static final int CAPACITY = 1000;
    public static final int TANK_CAPACITY = 4000;

    public final FluidTank tank = new FluidTank(TANK_CAPACITY, stack -> stack.getFluid() == Fluids.WATER) {
        @Override public int fill(FluidStack resource, FluidAction action) {
            return stilled() ? 0 : super.fill(resource, action);
        }
        @Override protected void onContentsChanged() { changed(); }
    };

    public WaveDrumBlockEntity(BlockPos pos, BlockState state) {
        super(GeneratorRegistry.WAVE_DRUM_TYPE.get(), pos, state, CAPACITY);
    }

    @Override public tk.darrow.tribalpower.api.pulse.Attunement voice() { return tk.darrow.tribalpower.api.pulse.Attunement.WATER; }

    public boolean piped() { return tank.getFluidAmount() >= WaveMath.PIPED_COST; }

    @Override
    protected int rawOutput(Level level, BlockPos pos) {
        return WaveMath.factors(level, pos, piped()).gain();
    }

    @Override
    protected void afterProduce(Level level, BlockPos pos, int produced) {
        // The piped tier drinks only when it actually delivered: a full buffer wastes no water.
        if (produced > 0 && piped()) tank.drain(WaveMath.PIPED_COST, IFluidHandler.FluidAction.EXECUTE);
    }

    @Override
    public List<Component> breakdown() {
        if (level == null) return List.of();
        List<Component> lines = new java.util.ArrayList<>(WaveMath.breakdown(level, worldPosition, piped()));
        lines.add(Component.translatable("wave.tribalpower.tank", tank.getFluidAmount(), TANK_CAPACITY));
        return lines;
    }

    public Component tankStatus() {
        return Component.translatable("message.tribalpower.cistern.status", tank.getFluidAmount(), TANK_CAPACITY);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Tank", tank.writeToNBT(registries, new CompoundTag()));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        tank.readFromNBT(registries, tag.getCompound("Tank"));
    }
}

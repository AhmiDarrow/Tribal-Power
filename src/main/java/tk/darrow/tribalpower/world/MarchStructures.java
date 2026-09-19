package tk.darrow.tribalpower.world;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import tk.darrow.tribalpower.TribalPower;

/** Code-driven structure types for the March. */
public final class MarchStructures {
    public static final DeferredRegister<StructureType<?>> TYPES = DeferredRegister.create(Registries.STRUCTURE_TYPE, TribalPower.MOD_ID);
    public static final DeferredRegister<StructurePieceType> PIECES = DeferredRegister.create(Registries.STRUCTURE_PIECE, TribalPower.MOD_ID);

    public static final DeferredHolder<StructureType<?>, StructureType<WillowGroveStructure>> WILLOW_GROVE =
            TYPES.register("willow_grove", () -> () -> WillowGroveStructure.CODEC);
    public static final DeferredHolder<StructurePieceType, StructurePieceType> WILLOW =
            PIECES.register("willow", () -> (StructurePieceType.ContextlessType) WillowGroveStructure.Willow::new);

    private MarchStructures() {}

    public static void register(IEventBus modBus) {
        TYPES.register(modBus);
        PIECES.register(modBus);
    }
}

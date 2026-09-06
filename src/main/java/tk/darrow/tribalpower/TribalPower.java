package tk.darrow.tribalpower;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;
import tk.darrow.tribalpower.block.ModBlocks;
import tk.darrow.tribalpower.blockentity.ModBlockEntities;
import tk.darrow.tribalpower.entity.ModEntities;
import tk.darrow.tribalpower.entity.ModEntityAttributes;
import tk.darrow.tribalpower.item.ModCreativeTabs;
import tk.darrow.tribalpower.item.ModItems;
import tk.darrow.tribalpower.world.ModDimensions;

@Mod(TribalPower.MOD_ID)
public final class TribalPower {
    public static final String MOD_ID = "tribalpower";
    public static final Logger LOGGER = LogUtils.getLogger();

    public TribalPower(IEventBus modBus) {
        ModBlocks.BLOCKS.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modBus);
        ModEntities.ENTITIES.register(modBus);
        ModCreativeTabs.TABS.register(modBus);

        modBus.addListener(this::onCommonSetup);
        modBus.addListener(ModEntityAttributes::onAttributes);
        modBus.addListener(ModEntityAttributes::onSpawnPlacements);
        NeoForge.EVENT_BUS.register(ModDimensions.class);
    }

    private void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> LOGGER.info("Tribal Power — shamanic technomancy online"));
    }
}

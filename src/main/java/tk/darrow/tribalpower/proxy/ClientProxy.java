package tk.darrow.tribalpower.proxy;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import tk.darrow.tribalpower.handlers.CraftingHandler;
import tk.darrow.tribalpower.init.ModBlocks;
import tk.darrow.tribalpower.init.ModItems;
import tk.darrow.tribalpower.init.TribalAltar;

public class ClientProxy extends CommonProxy {
	
	@Override
	public void PreInit(FMLPreInitializationEvent preEvent) {

	}
	@Override
	public void Init(FMLInitializationEvent event) {
		ModItems.registerRenders();
		ModBlocks.registerRenders();
		MinecraftForge.EVENT_BUS.register(new CraftingHandler());
		MinecraftForge.EVENT_BUS.register(new TribalAltar());
	}
	@Override
	public void PostInit(FMLPostInitializationEvent postEvent) {

	}

}

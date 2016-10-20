package tk.darrow.tribalpower.proxy;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import tk.darrow.tribalpower.handlers.CraftingHandler;
import tk.darrow.tribalpower.init.TribalAltar;

public class CommonProxy {
	
	public void PreInit(FMLPreInitializationEvent preEvent) {
		
	}
	
	public void Init(FMLInitializationEvent event) {
		MinecraftForge.EVENT_BUS.register(new CraftingHandler());
		MinecraftForge.EVENT_BUS.register(new TribalAltar());
	}
	
	public void PostInit(FMLPostInitializationEvent postEvent) {
		
	}

	
	
	
}

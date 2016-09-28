package tk.darrow.tribalpower;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.FMLLog;
import net.minecraftforge.fml.common.IWorldGenerator;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;
import tk.darrow.tribalpower.handlers.ConfigHandler;
import tk.darrow.tribalpower.handlers.OreHandler;
import tk.darrow.tribalpower.handlers.TextHandler;
import tk.darrow.tribalpower.init.ModBlocks;
import tk.darrow.tribalpower.init.ModCrafting;
import tk.darrow.tribalpower.init.ModCrops;
import tk.darrow.tribalpower.init.ModItems;
import tk.darrow.tribalpower.init.worldgen.OreGen;
import tk.darrow.tribalpower.proxy.CommonProxy;


@Mod(modid = Reference.MOD_ID, name = Reference.NAME, version = Reference.VERSION, acceptedMinecraftVersions = Reference.ACCEPTED_VERSIONS)
public class TribalPower {

	@Instance
	public static TribalPower instance;
	
	@SidedProxy(clientSide = Reference.CLIENT_PROXY_CLASS, serverSide = Reference.SERVER_PROXY_CLASS)
	public static CommonProxy proxy;
	
	public static final CreativeTabs CREATIVE_TAB = new TribalTab();

	// Console Spam
	public static Logger Log = FMLLog.getLogger();
	
	@EventHandler
	public void PreInit(FMLPreInitializationEvent event)
	{
		// PreIntialization Confirmation
		Log.info("[Tribal Power] - The clans are gathering!");
		
		//Handlers 
		ConfigHandler.init(event.getSuggestedConfigurationFile());
		
		//Gen
		GameRegistry.registerWorldGenerator(new OreGen(), 2);
		
		//Items
		ModItems.init();
		ModItems.register();
		
		//Blocks
		ModBlocks.init();
		ModBlocks.register();
		
		/*/Crops
		ModCrops.init();
		ModCrops.register();
		*/
		// Name & Registration Confirmation
		Log.info("[Tribal Power] - Fates are being spun...");
	}
	
	@EventHandler
	public void Init(FMLInitializationEvent event)
	{
		// Initialization Start
		Log.info("[Tribal Power] - Runes are being cast...");
		
	
		proxy.init();
		ModCrafting.register();
		ModCrafting.orereg();
		
		
		//Initialization Finish
		Log.info("[Tribal Power] - The elders have spoken... ");
	}
	
	@EventHandler
	public void PostInit(FMLPostInitializationEvent event)
	{
		// Post Confirmation
		Log.info("[Tribal Power] - It is now time, The Tribes have awoken!");
	}
}

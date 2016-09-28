package tk.darrow.tribalpower.init;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraftforge.fml.common.registry.GameRegistry;
import tk.darrow.tribalpower.blocks.BlockAcoomaLog;
import tk.darrow.tribalpower.blocks.BlockAcoomaPlanks;
import tk.darrow.tribalpower.blocks.BlockAmazonite;
import tk.darrow.tribalpower.blocks.BlockAmazoniteOre;
import tk.darrow.tribalpower.blocks.BlockChromiteOre;
import tk.darrow.tribalpower.blocks.BlockFluoriteOre;




public class ModBlocks {
	//Base Blocks
	public static Block acoomaplanks;
	public static Block acoomalog;
	
	//Metals and such
	public static Block amazoniteore;
	public static Block amazoniteblock;
	public static Block fluoriteore;
	public static Block chromiteore;
	
	
	
	public static void init() {
		//Base Blocks
		acoomaplanks = new BlockAcoomaPlanks();
		acoomalog = new BlockAcoomaLog();
		
		//Metals and such
		amazoniteore = new BlockAmazoniteOre();
		amazoniteblock = new BlockAmazonite();
		fluoriteore = new BlockFluoriteOre();
		chromiteore = new BlockChromiteOre();
		
	}
	
	public static void register() {
		//Base Blocks
		registerBlock(acoomaplanks);
		registerBlock(acoomalog);
		
		//Metals and such
		registerBlock(amazoniteore);
		registerBlock(amazoniteblock);
		registerBlock(fluoriteore);
		registerBlock(chromiteore);
		
	}
	
	private static void registerBlock(Block block) {
		GameRegistry.register(block);
		ItemBlock item = new ItemBlock(block);
		item.setRegistryName(block.getRegistryName());
		GameRegistry.register(item);
	}
	
	public static void registerRenders() {
		//Base Blocks
		registerRender(acoomaplanks);
		registerRender(acoomalog);
		
		//Metals and such
		registerRender(amazoniteore);
		registerRender(amazoniteblock);
		registerRender(fluoriteore);
		registerRender(chromiteore);
		
	}
	
	public static void registerRender(Block block) {
		Minecraft.getMinecraft().getRenderItem().getItemModelMesher().register(Item.getItemFromBlock(block), 0, new ModelResourceLocation(block.getRegistryName(), "inventory"));
	}
	
}

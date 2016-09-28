package tk.darrow.tribalpower.init;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraftforge.fml.common.registry.GameRegistry;
import tk.darrow.tribalpower.blocks.NasaiPlant;

public class ModCrops {
	
	public static Block nasaiplant;
	
	public static void init() {
		
		nasaiplant = new NasaiPlant();
	}
	
	public static void register() {
		
		GameRegistry.register(nasaiplant);
	}
	
	public static void registerRenders() {
		
		registerRender(nasaiplant);
		
	}
	
	public static void registerRender(Block block) {
		Minecraft.getMinecraft().getRenderItem().getItemModelMesher().register(Item.getItemFromBlock(block), 0, new ModelResourceLocation(block.getRegistryName(), "inventory"));
	}
}

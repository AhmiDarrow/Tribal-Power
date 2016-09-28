package tk.darrow.tribalpower.init;

import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.oredict.OreDictionary;

public class ModCrafting {
	
	// OreDictionary Registration
	public static OreDictionary OreReg;
	
	public static void register() {
		
		//Shaped
		GameRegistry.addShapedRecipe(new ItemStack(ModBlocks.amazoniteblock), "AAA", "AAA", "AAA", 'A', ModItems.amazoniteingot);
		GameRegistry.addShapedRecipe(new ItemStack(ModItems.spiritdiamond),
				new Object[] { "SSS", "SDS", "SSS", 'S', ModItems.spiritessence, 'D', Items.DIAMOND });
		//Shapeless
		
		
		//Smelting
		GameRegistry.addSmelting(ModBlocks.amazoniteore, new ItemStack(ModItems.amazoniteingot), 4.0F);
		GameRegistry.addSmelting(ModBlocks.chromiteore, new ItemStack(ModItems.chromiteingot), 4.0F);
		
		
		
	}
	
	
	// OreDictionary Registration
		public static void orereg() {

			// Items
			OreReg.registerOre("stickWood", ModItems.acoomastick);
			OreReg.registerOre("ingotAmazonite", ModItems.amazoniteingot);
			OreReg.registerOre("nuggetAmazonite", ModItems.amazonitenugget);
			OreReg.registerOre("gemDiamond", ModItems.spiritdiamond);

			// Blocks
			OreReg.registerOre("plankWood", ModBlocks.acoomaplanks);
			OreReg.registerOre("blockAmazonite", ModBlocks.amazoniteblock);

			// Actual Ore
			OreReg.registerOre("oreAmazonite", ModBlocks.amazoniteore);
			OreReg.registerOre("oreFluorite", ModBlocks.fluoriteore);
			OreReg.registerOre("oreChromite", ModBlocks.chromiteore);

		}
}

package tk.darrow.tribalpower.init;

import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraftforge.oredict.ShapelessOreRecipe;

public class ModCrafting {

	// OreDictionary Registration
	public static OreDictionary OreReg;

	public static void register() {

		// Shaped Items General
		GameRegistry.addShapedRecipe(new ItemStack(ModBlocks.amazoniteblock), "AAA", "AAA", "AAA", 'A',
				ModItems.amazoniteingot);
		GameRegistry.addShapedRecipe(new ItemStack(ModItems.amazoniteingot), "AAA", "AAA", "AAA", 'A',
				ModItems.amazonitenugget);
		GameRegistry.addShapedRecipe(new ItemStack(ModItems.spiritdiamond),
				new Object[] { "SSS", "SDS", "SSS", 'S', ModItems.spiritessence, 'D', Items.DIAMOND });
		GameRegistry.addShapedRecipe(new ItemStack(ModItems.runetip),
				new Object[] { "   ", "ASA", "AAA", 'S', ModItems.spiritessence, 'A', ModItems.amazonitenugget });
		GameRegistry.addShapedRecipe(new ItemStack(ModItems.tribalseal),
				new Object[] { "SES", "SDS", "LLL", 'S', ModItems.acoomastick, 'E', Items.EMERALD, 'A',
						ModItems.amazoniteingot, 'D', ModItems.spiritdiamond, 'L', ModBlocks.acoomaplanks });
		GameRegistry.addShapedRecipe(new ItemStack(ModItems.acoomastick, 4),
				new Object[] {"P", "P", 'P', ModBlocks.acoomaplanks });

		// Shaped Tools

		// Amazonite
		GameRegistry.addShapedRecipe(new ItemStack(ModItems.amazonite_axe),
				new Object[] { "AA ", "AS ", " S ", 'A', ModItems.amazoniteingot, 'S', ModItems.acoomastick });
		GameRegistry.addShapedRecipe(new ItemStack(ModItems.amazonite_hoe),
				new Object[] { "AA ", " S ", " S ", 'A', ModItems.amazoniteingot, 'S', ModItems.acoomastick });
		GameRegistry.addShapedRecipe(new ItemStack(ModItems.amazonite_shovel),
				new Object[] { " A ", " S ", " S ", 'A', ModItems.amazoniteingot, 'S', ModItems.acoomastick });
		GameRegistry.addShapedRecipe(new ItemStack(ModItems.amazonite_pickaxe),
				new Object[] { "AAA", " S ", " S ", 'A', ModItems.amazoniteingot, 'S', ModItems.acoomastick });
		GameRegistry.addShapedRecipe(new ItemStack(ModItems.amazonite_sword),
				new Object[] { " A ", " A ", " S ", 'A', ModItems.amazoniteingot, 'S', ModItems.acoomastick });
		// Chromite
		GameRegistry.addShapedRecipe(new ItemStack(ModItems.chromite_axe),
				new Object[] { "AA ", "AS ", " S ", 'A', ModItems.chromiteingot, 'S', ModItems.acoomastick });
		GameRegistry.addShapedRecipe(new ItemStack(ModItems.chromite_hoe),
				new Object[] { "AA ", " S ", " S ", 'A', ModItems.chromiteingot, 'S', ModItems.acoomastick });
		GameRegistry.addShapedRecipe(new ItemStack(ModItems.chromite_shovel),
				new Object[] { " A ", " S ", " S ", 'A', ModItems.chromiteingot, 'S', ModItems.acoomastick });
		GameRegistry.addShapedRecipe(new ItemStack(ModItems.chromite_pickaxe),
				new Object[] { "AAA", " S ", " S ", 'A', ModItems.chromiteingot, 'S', ModItems.acoomastick });
		GameRegistry.addShapedRecipe(new ItemStack(ModItems.chromite_sword),
				new Object[] { " A ", " A ", " S ", 'A', ModItems.chromiteingot, 'S', ModItems.acoomastick });
		// Fluorite
		GameRegistry.addShapedRecipe(new ItemStack(ModItems.fluorite_axe),
				new Object[] { "AA ", "AS ", " S ", 'A', ModItems.fluorite, 'S', ModItems.acoomastick });
		GameRegistry.addShapedRecipe(new ItemStack(ModItems.fluorite_hoe),
				new Object[] { "AA ", " S ", " S ", 'A', ModItems.fluorite, 'S', ModItems.acoomastick });
		GameRegistry.addShapedRecipe(new ItemStack(ModItems.fluorite_shovel),
				new Object[] { " A ", " S ", " S ", 'A', ModItems.fluorite, 'S', ModItems.acoomastick });
		GameRegistry.addShapedRecipe(new ItemStack(ModItems.fluorite_pickaxe),
				new Object[] { "AAA", " S ", " S ", 'A', ModItems.fluorite, 'S', ModItems.acoomastick });
		GameRegistry.addShapedRecipe(new ItemStack(ModItems.fluorite_sword),
				new Object[] { " A ", " A ", " S ", 'A', ModItems.fluorite, 'S', ModItems.acoomastick });
		// Shapeless
		GameRegistry.addShapelessRecipe(new ItemStack(ModItems.amazonitenugget, 9),
				new Object[] { ModItems.amazoniteingot });
		GameRegistry.addShapelessRecipe(new ItemStack(ModItems.blankrune, 4),
				new Object[] { Blocks.STONE, new ItemStack(ModItems.runecarver, 1, OreDictionary.WILDCARD_VALUE) });
		GameRegistry.addShapelessRecipe(new ItemStack(ModItems.runecarver),
				new Object[] { ModItems.acoomastick, ModItems.runetip });
		GameRegistry.addShapelessRecipe(new ItemStack(ModBlocks.acoomaplanks, 4),
				new Object[] { ModBlocks.acoomalog });
		

		// Smelting
		GameRegistry.addSmelting(ModBlocks.amazoniteore, new ItemStack(ModItems.amazoniteingot), 4.0F);
		GameRegistry.addSmelting(ModBlocks.chromiteore, new ItemStack(ModItems.chromiteingot), 4.0F);

	}

	// OreDictionary Registration
	public static void orereg() {

		// Items
		OreReg.registerOre("stickWood", ModItems.acoomastick);
		OreReg.registerOre("ingotAmazonite", ModItems.amazoniteingot);
		OreReg.registerOre("ingotChromite", ModItems.chromiteingot);
		OreReg.registerOre("nuggetAmazonite", ModItems.amazonitenugget);
		OreReg.registerOre("gemFluorite", ModItems.fluorite);
		OreReg.registerOre("gemDiamond", ModItems.spiritdiamond);

		// Blocks
		OreReg.registerOre("plankWood", ModBlocks.acoomaplanks);
		OreReg.registerOre("blockAmazonite", ModBlocks.amazoniteblock);

		// Actual Ore
		OreReg.registerOre("oreAmazonite", ModBlocks.amazoniteore);
		OreReg.registerOre("oreFluorite", ModBlocks.fluoriteore);
		OreReg.registerOre("oreChromite", ModBlocks.chromiteore);

	}

	public static void orerecipe() {
		// Ore Shapeless
		/*
		 * GameRegistry.addRecipe(new ShapelessOreRecipe(new
		 * ItemStack(ModItems.runecarver), new Object[] { ModItems.runetip,
		 * "stickWood" }));
		 */
	}

}

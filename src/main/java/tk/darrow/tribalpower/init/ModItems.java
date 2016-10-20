package tk.darrow.tribalpower.init;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraftforge.fml.common.registry.GameRegistry;
import tk.darrow.tribalpower.items.ItemAcoomaFruit;
import tk.darrow.tribalpower.items.ItemAcoomaStick;
import tk.darrow.tribalpower.items.ItemAirEssence;
import tk.darrow.tribalpower.items.ItemAirRune;
import tk.darrow.tribalpower.items.ItemAmazoniteAxe;
import tk.darrow.tribalpower.items.ItemAmazoniteHoe;
import tk.darrow.tribalpower.items.ItemAmazoniteIngot;
import tk.darrow.tribalpower.items.ItemAmazoniteNugget;
import tk.darrow.tribalpower.items.ItemAmazonitePickaxe;
import tk.darrow.tribalpower.items.ItemAmazoniteShovel;
import tk.darrow.tribalpower.items.ItemAmazoniteSword;
import tk.darrow.tribalpower.items.ItemBlankRune;
import tk.darrow.tribalpower.items.ItemChromiteAxe;
import tk.darrow.tribalpower.items.ItemChromiteHoe;
import tk.darrow.tribalpower.items.ItemChromiteIngot;
import tk.darrow.tribalpower.items.ItemChromitePickaxe;
import tk.darrow.tribalpower.items.ItemChromiteShovel;
import tk.darrow.tribalpower.items.ItemChromiteSword;
import tk.darrow.tribalpower.items.ItemEarthRune;
import tk.darrow.tribalpower.items.ItemFireRune;
import tk.darrow.tribalpower.items.ItemFluorite;
import tk.darrow.tribalpower.items.ItemFluoriteAxe;
import tk.darrow.tribalpower.items.ItemFluoriteHoe;
import tk.darrow.tribalpower.items.ItemFluoritePickaxe;
import tk.darrow.tribalpower.items.ItemFluoriteShovel;
import tk.darrow.tribalpower.items.ItemFluoriteSword;
import tk.darrow.tribalpower.items.ItemInfusedAmazoniteIngot;
import tk.darrow.tribalpower.items.ItemRuneCarver;
import tk.darrow.tribalpower.items.ItemRunetip;
import tk.darrow.tribalpower.items.ItemSpiritDiamond;
import tk.darrow.tribalpower.items.ItemSpiritEssence;
import tk.darrow.tribalpower.items.ItemTribalSeal;
import tk.darrow.tribalpower.items.ItemWaterRune;

public class ModItems {

	// Base Items
	public static Item acoomafruit;
	public static Item acoomastick;

	// Crops and such

	// Metals and such
	public static Item amazoniteingot;
	public static Item amazonitenugget;
	public static Item fluorite;
	public static Item chromiteingot;
	public static Item spiritdiamond;

	// Runes and such
	public static Item tribalseal;
	public static Item spiritessence;
	public static Item airessence;
	public static Item airrune;
	public static Item blankrune;
	public static Item earthrune;
	public static Item firerune;
	public static Item waterrune;

	// Tools and such
	public static Item amazonite_pickaxe;
	public static Item amazonite_axe;
	public static Item amazonite_hoe;
	public static Item amazonite_shovel;
	public static Item chromite_pickaxe;
	public static Item chromite_axe;
	public static Item chromite_hoe;
	public static Item chromite_shovel;
	public static Item fluorite_pickaxe;
	public static Item fluorite_axe;
	public static Item fluorite_hoe;
	public static Item fluorite_shovel;
	public static Item runecarver;
	public static Item runetip;

	// Weapons and such
	public static Item amazonite_sword;
	public static Item chromite_sword;
	public static Item fluorite_sword;

	public static void init() {
		// Base Items
		acoomafruit = new ItemAcoomaFruit();
		acoomastick = new ItemAcoomaStick();

		// Crops and such

		// Metals and such
		amazoniteingot = new ItemAmazoniteIngot();
		amazonitenugget = new ItemAmazoniteNugget();
		fluorite = new ItemFluorite();
		chromiteingot = new ItemChromiteIngot();
		spiritdiamond = new ItemSpiritDiamond();

		// Runes and such
		tribalseal = new ItemTribalSeal();
		spiritessence = new ItemSpiritEssence();
		airessence = new ItemAirEssence();
		airrune = new ItemAirRune();
		blankrune = new ItemBlankRune();
		earthrune = new ItemEarthRune();
		firerune = new ItemFireRune();
		waterrune = new ItemWaterRune();

		// Tools and such
		amazonite_pickaxe = new ItemAmazonitePickaxe(TribalToolMaterials.AMAZONITE);
		amazonite_axe = new ItemAmazoniteAxe(TribalToolMaterials.AMAZONITE, 4.0F, 3.0F);
		amazonite_hoe = new ItemAmazoniteHoe(TribalToolMaterials.AMAZONITE);
		amazonite_shovel = new ItemAmazoniteShovel(TribalToolMaterials.AMAZONITE);
		chromite_pickaxe = new ItemChromitePickaxe(TribalToolMaterials.CHROMITE);
		chromite_axe = new ItemChromiteAxe(TribalToolMaterials.CHROMITE, 3.8F, 3.2F);
		chromite_hoe = new ItemChromiteHoe(TribalToolMaterials.CHROMITE);
		chromite_shovel = new ItemChromiteShovel(TribalToolMaterials.CHROMITE);
		fluorite_pickaxe = new ItemFluoritePickaxe(TribalToolMaterials.FLUORITE);
		fluorite_axe = new ItemFluoriteAxe(TribalToolMaterials.FLUORITE, 4.5F, 1.5F);
		fluorite_hoe = new ItemFluoriteHoe(TribalToolMaterials.FLUORITE);
		fluorite_shovel = new ItemFluoriteShovel(TribalToolMaterials.FLUORITE);
		runecarver = new ItemRuneCarver();
		runetip = new ItemRunetip();

		// Weapons and such
		amazonite_sword = new ItemAmazoniteSword(TribalToolMaterials.CHROMITE);
		chromite_sword = new ItemChromiteSword(TribalToolMaterials.CHROMITE);
		fluorite_sword = new ItemFluoriteSword(TribalToolMaterials.FLUORITE);
	}

	public static void register() {
		// Base Items
		GameRegistry.register(acoomafruit);
		GameRegistry.register(acoomastick);

		// Crops and such

		// Metals and such
		GameRegistry.register(amazoniteingot);
		GameRegistry.register(amazonitenugget);
		GameRegistry.register(fluorite);
		GameRegistry.register(chromiteingot);
		GameRegistry.register(spiritdiamond);

		// Runes and such
		GameRegistry.register(tribalseal);
		GameRegistry.register(spiritessence);
		GameRegistry.register(airessence);
		GameRegistry.register(airrune);
		GameRegistry.register(blankrune);
		GameRegistry.register(earthrune);
		GameRegistry.register(firerune);
		GameRegistry.register(waterrune);

		// Tools and such
		GameRegistry.register(amazonite_pickaxe);
		GameRegistry.register(amazonite_axe);
		GameRegistry.register(amazonite_hoe);
		GameRegistry.register(amazonite_shovel);
		GameRegistry.register(chromite_pickaxe);
		GameRegistry.register(chromite_axe);
		GameRegistry.register(chromite_hoe);
		GameRegistry.register(chromite_shovel);
		GameRegistry.register(fluorite_pickaxe);
		GameRegistry.register(fluorite_axe);
		GameRegistry.register(fluorite_hoe);
		GameRegistry.register(fluorite_shovel);
		GameRegistry.register(runecarver);
		GameRegistry.register(runetip);

		// Weapons and such
		GameRegistry.register(amazonite_sword);
		GameRegistry.register(chromite_sword);
		GameRegistry.register(fluorite_sword);
	}

	public static void registerRenders() {

		// Base Items
		registerRender(acoomafruit);
		registerRender(acoomastick);

		// Crops and such

		// Metals and such
		registerRender(amazoniteingot);
		registerRender(amazonitenugget);
		registerRender(fluorite);
		registerRender(chromiteingot);
		registerRender(spiritdiamond);

		// Runes
		registerRender(tribalseal);
		registerRender(spiritessence);
		registerRender(airessence);

		registerRender(airrune);
		registerRender(blankrune);
		registerRender(earthrune);
		registerRender(firerune);
		registerRender(waterrune);

		// Tools and such
		registerRender(amazonite_pickaxe);
		registerRender(amazonite_axe);
		registerRender(amazonite_hoe);
		registerRender(amazonite_shovel);
		registerRender(runecarver);
		registerRender(runetip);
		registerRender(chromite_pickaxe);
		registerRender(chromite_axe);
		registerRender(chromite_hoe);
		registerRender(chromite_shovel);
		registerRender(fluorite_pickaxe);
		registerRender(fluorite_axe);
		registerRender(fluorite_hoe);
		registerRender(fluorite_shovel);

		// Weapons and such
		registerRender(amazonite_sword);
		registerRender(chromite_sword);
		registerRender(fluorite_sword);

	}

	public static void registerRender(Item item) {
		Minecraft.getMinecraft().getRenderItem().getItemModelMesher().register(item, 0,
				new ModelResourceLocation(item.getRegistryName(), "inventory"));
	}
}

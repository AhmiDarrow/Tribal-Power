package tk.darrow.tribalpower.init;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraftforge.fml.common.registry.GameRegistry;
import tk.darrow.tribalpower.items.ItemAcoomaFruit;
import tk.darrow.tribalpower.items.ItemAcoomaStick;
import tk.darrow.tribalpower.items.ItemAirRune;
import tk.darrow.tribalpower.items.ItemAmazoniteAxe;
import tk.darrow.tribalpower.items.ItemAmazoniteHoe;
import tk.darrow.tribalpower.items.ItemAmazoniteIngot;
import tk.darrow.tribalpower.items.ItemAmazoniteNugget;
import tk.darrow.tribalpower.items.ItemAmazonitePickaxe;
import tk.darrow.tribalpower.items.ItemAmazoniteShovel;
import tk.darrow.tribalpower.items.ItemAmazoniteSword;
import tk.darrow.tribalpower.items.ItemBlankRune;
import tk.darrow.tribalpower.items.ItemChromiteIngot;
import tk.darrow.tribalpower.items.ItemEarthRune;
import tk.darrow.tribalpower.items.ItemFireRune;
import tk.darrow.tribalpower.items.ItemFluorite;
import tk.darrow.tribalpower.items.ItemInfusedAmazoniteIngot;
import tk.darrow.tribalpower.items.ItemSpiritDiamond;
import tk.darrow.tribalpower.items.ItemSpiritEssence;
import tk.darrow.tribalpower.items.ItemTribalSeal;
import tk.darrow.tribalpower.items.ItemWaterRune;

public class ModItems {
	
	 

	//Base Items
	public static Item acoomafruit;
	public static Item acoomastick;
	
	//Metals and such
	public static Item amazoniteingot;
	public static Item amazonitenugget;
	public static Item infusedamazoniteingot;
	public static Item fluorite;
	public static Item chromiteingot;
	public static Item spiritdiamond;
	
	//Runes and such
	public static Item tribalseal;
	public static Item spiritessence;
	public static Item airrune;
	public static Item blankrune;
	public static Item earthrune;
	public static Item firerune;
	public static Item waterrune;
	
	//Tools and such
	public static Item amazonite_pickaxe;
	public static Item amazonite_axe;
	public static Item amazonite_hoe;
	public static Item amazonite_shovel;
	
	//Weapons and such
	public static Item amazonite_sword;
	
	public static void init() {
		//Base Items
		acoomafruit = new ItemAcoomaFruit();
		acoomastick = new ItemAcoomaStick();
		
		//Metals and such
		amazoniteingot = new ItemAmazoniteIngot();
		amazonitenugget = new ItemAmazoniteNugget();
		infusedamazoniteingot = new ItemInfusedAmazoniteIngot();
		fluorite = new ItemFluorite();
		chromiteingot = new ItemChromiteIngot();
		spiritdiamond = new ItemSpiritDiamond();
		
		//Runes and such
		tribalseal = new ItemTribalSeal();
		spiritessence = new ItemSpiritEssence();
		airrune = new ItemAirRune();
		blankrune = new ItemBlankRune();
		earthrune = new ItemEarthRune();
		firerune = new ItemFireRune();
		waterrune = new ItemWaterRune();
		
		//Tools and such
		amazonite_pickaxe = new ItemAmazonitePickaxe(TribalToolMaterials.AMAZONITE);
		amazonite_axe = new ItemAmazoniteAxe(TribalToolMaterials.AMAZONITE, 4.0F, 3.0F);
		amazonite_hoe = new ItemAmazoniteHoe(TribalToolMaterials.AMAZONITE);
		amazonite_shovel = new ItemAmazoniteShovel(TribalToolMaterials.AMAZONITE);
		
		//Weapons and such
		amazonite_sword = new ItemAmazoniteSword(TribalToolMaterials.AMAZONITE);
	}
	
	public static void register() {
		//Base Items
		GameRegistry.register(acoomafruit);
		GameRegistry.register(acoomastick);
		
		
		//Metals and such
		GameRegistry.register(amazoniteingot);
		GameRegistry.register(amazonitenugget);
		GameRegistry.register(infusedamazoniteingot);
		GameRegistry.register(fluorite);
		GameRegistry.register(chromiteingot);
		GameRegistry.register(spiritdiamond);
		
		//Runes and such
		GameRegistry.register(tribalseal);
		GameRegistry.register(spiritessence);
		GameRegistry.register(airrune);
		GameRegistry.register(blankrune);
		GameRegistry.register(earthrune);
		GameRegistry.register(firerune);
		GameRegistry.register(waterrune);
		
		//Tools and such
		GameRegistry.register(amazonite_pickaxe);
		GameRegistry.register(amazonite_axe);
		GameRegistry.register(amazonite_hoe);
		GameRegistry.register(amazonite_shovel);
		
		//Weapons and such
		GameRegistry.register(amazonite_sword);
	}
	
	public static void registerRenders() {
		
		//Base Items
		registerRender(acoomafruit);
		registerRender(acoomastick);
		
		//Metals and such
		registerRender(amazoniteingot);
		registerRender(amazonitenugget);
		registerRender(infusedamazoniteingot);
		registerRender(fluorite);
		registerRender(chromiteingot);
		registerRender(spiritdiamond);
		
		//Runes
		registerRender(tribalseal);
		registerRender(spiritessence);
		registerRender(airrune);
		registerRender(blankrune);
		registerRender(earthrune);
		registerRender(firerune);
		registerRender(waterrune);
		
		//Tools and such
		registerRender(amazonite_pickaxe);
		registerRender(amazonite_axe);
		registerRender(amazonite_hoe);
		registerRender(amazonite_shovel);
		
		//Weapons and such
		registerRender(amazonite_sword);
		
	}
	
	public static void registerRender(Item item) {
		Minecraft.getMinecraft().getRenderItem().getItemModelMesher().register(item, 0, new ModelResourceLocation(item.getRegistryName(), "inventory"));
	}
}

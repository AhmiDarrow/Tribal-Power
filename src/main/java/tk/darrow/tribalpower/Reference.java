package tk.darrow.tribalpower;

public class Reference {
	
	public static final String MOD_ID = "TribalPower";
	public static final String NAME = "Tribal Power";
	public static final String VERSION = "1.0";
	public static final String ACCEPTED_VERSIONS = "[1.9.4]";
	
	public static final String CLIENT_PROXY_CLASS = "tk.darrow.tribalpower.proxy.ClientProxy";
	public static final String SERVER_PROXY_CLASS = "tk.darrow.tribalpower.proxy.ServerProxy";
	
	public static enum TribalPowerItems {
		//Base Items
		ACOOMAFRUIT("acoomafruit", "ItemAcoomaFruit"),
		ACOOMASTICK("acoomastick", "ItemAcoomaStick"),
		
		//Metals and such
		AMAZONITEINGOT("amazoniteingot", "ItemAmazoniteIngot"),
		AMAZONITENUGGET("amazonitenugget", "ItemAmazoniteNugget"),
		INFUSEDAMAZONITEINGOT("infusedamazoniteingot", "ItemInfusedAmazoniteIngot"),
		FLUORITE("fluorite", "ItemFluorite"),
		CHROMITEINGOT("chromiteingot", "ItemChromiteIngot"),
		SPIRITDIAMOND("spiritdiamond", "ItemSpiritDiamond"),
		
		
		//Runes and such
		TRIBALSEAL("tribalseal", "ItemTribalSeal"),
		SPIRITESSENCE("spiritessence", "ItemSpiritEssence"),
		AIRRUNE("airrune", "ItemAirRune"),
		BLANKRUNE("blankrune", "ItemBlankRune"),
		EARTHRUNE("earthrune", "ItemEarthRune"),
		FIRERUNE("firerune", "ItemFireRune"),
		WATERRUNE("waterrune", "ItemWaterRune"),
		
		//Tools and such
		AMAZONITEPICKAXE("amazonite_pickaxe", "ItemAmazonitePickaxe"),
		AMAZONITEAXE("amazonite_axe", "ItemAmazoniteAxe"),
		AMAZONITEHOE("amazonite_hoe", "ItemAmazoniteHoe"),
		AMAZONITESHOVEL("amazonite_shovel", "ItemAmazoniteShovel"),
		
		//Weapons and such
		AMAZONITESWORD("amazonite_sword", "ItemAmazoniteSword");
		
		private String unlocalizedName;
		private String registryName;
		
		TribalPowerItems(String unlocalizedName, String registryName) {
			this.unlocalizedName = unlocalizedName;
			this.registryName = registryName;
		}
		
		public String getUnlocalizedName() {
			return unlocalizedName;
		}
		
		public String getRegistryName() {
			return registryName;
		}
	}
	
	public static enum TribalPowerBlocks {
		//Base Blocks
		ACOOMAPLANKS("acoomaplanks", "BlockAcoomaPlanks"),
		ACOOMALOG("acoomalog", "BlockAcoomaLog"),
		
		//Metals and such
		AMAZONITEORE("amazoniteore", "BlockAmazoniteOre"),
		AMAZONITEBLOCK("amazoniteblock", "BlockAmazonite"),
		FLUORITEORE("fluoriteore", "BlockFluoriteOre"),
		CHROMITEORE("chromiteore", "BlockChromiteOre");
				
		private String unlocalizedName;
		private String registryName;
		
		TribalPowerBlocks(String unlocalizedName, String registryName) {
			this.unlocalizedName = unlocalizedName;
			this.registryName = registryName;
		}
		
		public String getUnlocalizedName() {
			return unlocalizedName;
		}
		
		public String getRegistryName() {
			return registryName;
		}
		
	}
}

package tk.darrow.tribalpower.handlers;

import java.io.File;

import net.minecraftforge.common.config.Configuration;

public class ConfigHandler {

	    public static Configuration config;

	    // Sections to add to the config
	    public static String oregeneration = "Ore Generation";

	    // Options in the config
	    public static boolean enableAmazoniteGeneration;
	    public static boolean enableFluoriteGeneration;
	    public static boolean enableChromiteGeneration;

	    public static void init(File file) {
	        config = new Configuration(file);
	        syncConfig();
	    }

	    public static void syncConfig() {
	        config.addCustomCategoryComment(oregeneration, "This section contains all settings regarding ore generation.");

	        
	        enableAmazoniteGeneration = config.get(oregeneration, "enableAmazoniteGeneration", true, "Enable Amazonite ore generation off/on, on by default").getBoolean(enableAmazoniteGeneration);
	        enableFluoriteGeneration = config.get(oregeneration, "enableFluoriteGeneration", true, "Enable Fluorite ore generation off/on, on by default").getBoolean(enableFluoriteGeneration);
	        config.save();
	        enableChromiteGeneration = config.get(oregeneration, "enableChromiteGeneration", true, "Turn Chromite ore generation off/on, on by default").getBoolean(enableChromiteGeneration);
	        config.save();
	    }
	}


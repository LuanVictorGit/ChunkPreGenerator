package me.chunkpregenerator.utils;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import lombok.Getter;
import me.chunkpregenerator.Core;

@Getter
public class ConfigManager {

	private final int chunks_per_tick;
	private final int max_border_size;
	
	public ConfigManager() {
		
		FileConfiguration config = Core.getInstance().getConfig();
		ConfigurationSection section = config.getConfigurationSection("Config");
		
		max_border_size = section.getInt("max_border_size");
		chunks_per_tick = section.getInt("chunks_per_tick");
		
	}

	public static ConfigManager get() {
		return Core.getInstance().getConfigManager();
	}
	
}

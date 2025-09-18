package com.infinity3113.contractrpg.managers;

import com.infinity3113.contractrpg.ContractRPG;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class ConfigUpdater {

    public static void updateConfig(ContractRPG plugin) throws IOException {
        File configFile = new File(plugin.getDataFolder(), "config.yml");
        FileConfiguration config = plugin.getConfig();

        InputStream defaultConfigStream = plugin.getResource("config.yml");
        if (defaultConfigStream != null) {
            YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defaultConfigStream));
            config.addDefaults(defaultConfig);
            config.options().copyDefaults(true);
            plugin.saveConfig();
        }
    }
}
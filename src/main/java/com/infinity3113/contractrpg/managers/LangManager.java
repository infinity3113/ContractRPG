package com.infinity3113.contractrpg.managers;

import com.infinity3113.contractrpg.ContractRPG;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class LangManager {

    private final ContractRPG plugin;
    private final Map<String, FileConfiguration> langConfigs = new HashMap<>();
    private String defaultLang;

    public LangManager(ContractRPG plugin) {
        this.plugin = plugin;
        this.defaultLang = plugin.getConfig().getString("default-language", "en");
    }

    public void loadLanguages() {
        langConfigs.clear();
        this.defaultLang = plugin.getConfig().getString("default-language", "en");
        File langFolder = new File(plugin.getDataFolder(), "lang");
        if (!langFolder.exists()) {
            langFolder.mkdirs();
        }

        saveDefaultLangFile("en.yml");
        saveDefaultLangFile("es.yml");

        File[] langFiles = langFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (langFiles != null) {
            for (File file : langFiles) {
                String langName = file.getName().replace(".yml", "");
                langConfigs.put(langName, YamlConfiguration.loadConfiguration(file));
            }
        }
    }

    private void saveDefaultLangFile(String fileName) {
        File file = new File(plugin.getDataFolder(), "lang/" + fileName);
        if (!file.exists()) {
            plugin.saveResource("lang/" + fileName, false);
        }
    }

    public String getMessage(String path) {
        FileConfiguration config = langConfigs.get(defaultLang);
        if (config == null || !config.contains(path)) {
            config = langConfigs.get("en");
            if(config == null) {
                return "<red>Language file for '" + defaultLang + "' or 'en' not found!";
            }
        }
        return config.getString(path, "<red>Missing message: " + path);
    }
}
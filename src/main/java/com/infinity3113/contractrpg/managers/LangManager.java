package com.infinity3113.contractrpg.managers;

import com.infinity3113.contractrpg.ContractRPG;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LangManager {

    private final ContractRPG plugin;
    private final Map<String, FileConfiguration> langConfigs = new HashMap<>();
    private String defaultLang;
    private String prefix;

    public LangManager(ContractRPG plugin) {
        this.plugin = plugin;
    }

    public void loadLanguages() {
        langConfigs.clear();
        this.defaultLang = plugin.getConfig().getString("language", "en");

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
        this.prefix = getMessageFromFile("prefix");
    }

    private void saveDefaultLangFile(String fileName) {
        File file = new File(plugin.getDataFolder(), "lang/" + fileName);
        if (!file.exists()) {
            plugin.saveResource("lang/" + fileName, false);
        }
    }

    private String getMessageFromFile(String path) {
        FileConfiguration config = langConfigs.get(defaultLang);
        if (config == null || !config.contains(path)) {
            config = langConfigs.get("en");
            if (config == null) {
                return "<red>Language file for '" + defaultLang + "' or 'en' not found!";
            }
        }
        return config.getString(path, "<red>Missing message: " + path);
    }
    
    // ¡NUEVO MÉTODO! Para leer listas de texto correctamente.
    public List<String> getMessageList(String path) {
        FileConfiguration config = langConfigs.get(defaultLang);
        if (config == null || !config.isList(path)) {
            config = langConfigs.get("en");
             if (config == null || !config.isList(path)) {
                return Collections.singletonList("<red>Missing message list: " + path);
            }
        }
        return config.getStringList(path);
    }

    public String getMessage(String path) {
        String message = getMessageFromFile(path);
        return message.replace("%prefix%", this.prefix);
    }
}
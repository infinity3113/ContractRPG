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
    private String prefix;

    public LangManager(ContractRPG plugin) {
        this.plugin = plugin;
    }

    public void loadLanguages() {
        langConfigs.clear();
        // CORRECCIÓN: Usar la clave "language" para que coincida con config.yml
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
        // Cargar el prefijo después de cargar los archivos
        this.prefix = getMessageFromFile("prefix");
    }

    private void saveDefaultLangFile(String fileName) {
        File file = new File(plugin.getDataFolder(), "lang/" + fileName);
        if (!file.exists()) {
            plugin.saveResource("lang/" + fileName, false);
        }
    }

    // Método privado para obtener el string crudo del archivo sin procesar el prefijo
    private String getMessageFromFile(String path) {
        FileConfiguration config = langConfigs.get(defaultLang);
        if (config == null || !config.contains(path)) {
            config = langConfigs.get("en"); // Fallback a inglés
            if (config == null) {
                return "<red>Language file for '" + defaultLang + "' or 'en' not found!";
            }
        }
        return config.getString(path, "<red>Missing message: " + path);
    }

    public String getMessage(String path) {
        String message = getMessageFromFile(path);
        // CORRECCIÓN: Reemplazar el placeholder %prefix% automáticamente
        return message.replace("%prefix%", this.prefix);
    }
}
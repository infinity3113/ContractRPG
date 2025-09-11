package com.example.contractrpg.managers;

import com.example.contractrpg.ContractRPG;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;

public class LangManager {

    private final ContractRPG plugin;
    private FileConfiguration langConfig;

    public LangManager(ContractRPG plugin) {
        this.plugin = plugin;
        loadLangFile();
    }

    private void loadLangFile() {
        // LEEMOS LA OPCIÓN DEL CONFIG.YML
        String lang = plugin.getConfig().getString("language", "en");
        File langFile = new File(plugin.getDataFolder(), "lang/" + lang + ".yml");
        if (!langFile.exists()) {
            plugin.getLogger().warning("¡El archivo de idioma '" + lang + ".yml' no se encontró! Usando 'en.yml' por defecto.");
            plugin.saveResource("lang/en.yml", false);
            plugin.saveResource("lang/es.yml", false);
            langFile = new File(plugin.getDataFolder(), "lang/en.yml"); // Fallback a inglés
        }
        langConfig = YamlConfiguration.loadConfiguration(langFile);
    }

    public String getMessage(String path) {
        return langConfig.getString("prefix", "") + langConfig.getString(path, "Message not found: " + path);
    }

    public void sendMessage(Player player, String path) {
        String message = getMessage(path);
        Component component = MiniMessage.miniMessage().deserialize(message);
        String legacyMessage = LegacyComponentSerializer.legacySection().serialize(component);
        player.sendMessage(legacyMessage);
    }
}
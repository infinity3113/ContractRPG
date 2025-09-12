package com.example.contractrpg.managers;

import com.example.contractrpg.ContractRPG;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.List;

public class LangManager {

    private final ContractRPG plugin;
    private FileConfiguration langConfig;

    public LangManager(ContractRPG plugin) {
        this.plugin = plugin;
        loadLanguageFile();
    }

    private void loadLanguageFile() {
        String lang = plugin.getConfig().getString("language", "es");
        File langFile = new File(plugin.getDataFolder(), "lang/" + lang + ".yml");
        if (!langFile.exists()) {
            plugin.getLogger().warning("No se encontró el archivo de idioma '" + lang + ".yml'. Usando 'es.yml' por defecto.");
            langFile = new File(plugin.getDataFolder(), "lang/es.yml");
        }
        langConfig = YamlConfiguration.loadConfiguration(langFile);
    }

    public String getMessage(String path) {
        String message = langConfig.getString(path, "<#FF0000>Error: Mensaje no encontrado: " + path);
        String prefix = langConfig.getString("prefix", "");
        return message.replace("%prefix%", prefix);
    }

    // Método sobrecargado para aceptar un valor por defecto
    public String getMessage(String path, String defaultValue) {
        String message = langConfig.getString(path, defaultValue);
        String prefix = langConfig.getString("prefix", "");
        return message.replace("%prefix%", prefix);
    }


    public List<String> getStringList(String path) {
        return langConfig.getStringList(path);
    }

    public void sendMessage(Player player, String path) {
        String message = getMessage(path);
        Component component = MiniMessage.miniMessage().deserialize(message);
        String legacyMessage = LegacyComponentSerializer.legacySection().serialize(component);
        player.sendMessage(legacyMessage);
    }
}
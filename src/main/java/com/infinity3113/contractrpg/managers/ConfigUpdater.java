package com.example.contractrpg.managers;

import com.example.contractrpg.ContractRPG;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class ConfigUpdater {

    /**
     * Compara un archivo .yml del servidor con el de por defecto del plugin.
     * Si faltan claves en el archivo del servidor, las añade desde el archivo por defecto.
     *
     * @param plugin   La instancia de tu plugin.
     * @param fileName El nombre del archivo a actualizar (ej: "config.yml").
     */
    public static void update(ContractRPG plugin, String fileName) {
        File configFile = new File(plugin.getDataFolder(), fileName);
        FileConfiguration serverConfig = YamlConfiguration.loadConfiguration(configFile);

        // Cargar el archivo por defecto desde dentro del .jar del plugin
        InputStream defaultConfigStream = plugin.getResource(fileName);
        if (defaultConfigStream == null) {
            // No hay un archivo por defecto en el plugin, no hay nada que hacer.
            return;
        }
        YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defaultConfigStream));

        boolean modified = false;
        // Iteramos sobre todas las claves del archivo por defecto
        for (String key : defaultConfig.getKeys(true)) {
            // Si la clave no existe en el archivo del servidor, la añadimos.
            if (!serverConfig.contains(key)) {
                serverConfig.set(key, defaultConfig.get(key));
                modified = true;
            }
        }

        // Si hemos añadido nuevas claves, guardamos el archivo del servidor.
        if (modified) {
            try {
                serverConfig.save(configFile);
                plugin.getLogger().info("¡'" + fileName + "' ha sido actualizado con nuevos valores por defecto!");
            } catch (IOException e) {
                plugin.getLogger().severe("No se pudo guardar la configuración actualizada en '" + fileName + "'");
                e.printStackTrace();
            }
        }
    }
}
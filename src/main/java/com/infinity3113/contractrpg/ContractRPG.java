package com.example.contractrpg;

import com.example.contractrpg.commands.ContractCommand;
import com.example.contractrpg.contracts.ContractManager;
import com.example.contractrpg.data.SqliteStorage;
import com.example.contractrpg.data.StorageManager;
import com.example.contractrpg.data.YamlStorage;
import com.example.contractrpg.listeners.NPCListener;
import com.example.contractrpg.listeners.PlayerListener;
import com.example.contractrpg.managers.ConfigUpdater;
import com.example.contractrpg.managers.LangManager;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class ContractRPG extends JavaPlugin {

    private static ContractRPG instance;
    private ContractManager contractManager;
    private LangManager langManager;
    private StorageManager storageManager;
    private FileConfiguration contractsConfig;
    private Economy economy = null;

    @Override
    public void onEnable() {
        instance = this;
        
        if (!setupEconomy()) {
            getLogger().severe("¡Vault no encontrado o no hay un plugin de economía! Desactivando ContractRPG.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        setupConfigFiles();
        
        langManager = new LangManager(this);
        contractManager = new ContractManager(this);
        
        String storageType = getConfig().getString("storage.type", "yaml").toLowerCase();
        if (storageType.equals("sqlite")) {
            storageManager = new SqliteStorage(this, contractManager);
        } else {
            storageManager = new YamlStorage(this, contractManager);
        }
        storageManager.init();
        
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        if (getServer().getPluginManager().getPlugin("Citizens") != null) {
            getServer().getPluginManager().registerEvents(new NPCListener(this), this);
            getLogger().info("Integración con Citizens cargada exitosamente.");
        } else {
             getLogger().warning("Citizens no encontrado, las misiones de entrega a NPCs estarán desactivadas.");
        }
        
        getCommand("contracts").setExecutor(new ContractCommand(this));

        // Cargar datos de jugadores ya conectados (en caso de /reload)
        for (Player player : Bukkit.getOnlinePlayers()) {
            storageManager.loadPlayerData(player);
        }

        getLogger().info("ContractRPG ha sido enabled! Usando almacenamiento: " + storageType);
    }
    
    @Override
    public void onDisable() {
        getLogger().info("Guardando datos de los jugadores...");
        for (Player player : Bukkit.getOnlinePlayers()) {
            storageManager.savePlayerData(player);
        }
        storageManager.shutdown();
        getLogger().info("¡Datos guardados! ContractRPG ha sido desactivado.");
    }
    
    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        economy = rsp.getProvider();
        return economy != null;
    }

    private void setupConfigFiles() {
        saveDefaultConfig();
        ConfigUpdater.update(this, "config.yml");
        reloadConfig();
        
        saveResourceIfNotExists("contracts.yml");
        ConfigUpdater.update(this, "contracts.yml");
        createContractsConfig();
        
        saveResourceIfNotExists("lang/es.yml");
        ConfigUpdater.update(this, "lang/es.yml");
        saveResourceIfNotExists("lang/en.yml");
        ConfigUpdater.update(this, "lang/en.yml");
    }

    private void saveResourceIfNotExists(String resourcePath) {
        File resourceFile = new File(getDataFolder(), resourcePath);
        if (!resourceFile.exists()) {
            saveResource(resourcePath, false);
        }
    }
    
    private void createContractsConfig() {
        File contractsFile = new File(getDataFolder(), "contracts.yml");
        contractsConfig = YamlConfiguration.loadConfiguration(contractsFile);
    }
    
    public static ContractRPG getInstance() { return instance; }
    public ContractManager getContractManager() { return contractManager; }
    public LangManager getLangManager() { return langManager; }
    public StorageManager getStorageManager() { return storageManager; }
    public FileConfiguration getContractsConfig() { return this.contractsConfig; }
    public Economy getEconomy() { return economy; }
}
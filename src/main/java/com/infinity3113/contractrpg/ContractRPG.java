package com.infinity3113.contractrpg;

import com.infinity3113.contractrpg.commands.ContractCommand;
import com.infinity3113.contractrpg.contracts.ContractManager;
import com.infinity3113.contractrpg.data.SqliteStorage;
import com.infinity3113.contractrpg.data.StorageManager;
import com.infinity3113.contractrpg.data.YamlStorage;
import com.infinity3113.contractrpg.listeners.MythicMobListener;
import com.infinity3113.contractrpg.listeners.NPCListener;
import com.infinity3113.contractrpg.listeners.PlayerListener;
import com.infinity3113.contractrpg.managers.ConfigUpdater;
import com.infinity3113.contractrpg.managers.LangManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;

public final class ContractRPG extends JavaPlugin {

    private static ContractRPG instance;
    private ContractManager contractManager;
    private LangManager langManager;
    private StorageManager storageManager;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();
        try {
            ConfigUpdater.updateConfig(this);
        } catch (IOException e) {
            getLogger().severe("Could not update config.yml!");
            e.printStackTrace();
        }

        this.langManager = new LangManager(this);
        this.langManager.loadLanguages();

        this.contractManager = new ContractManager(this);

        String storageType = getConfig().getString("storage-type", "yaml").toLowerCase();
        if (storageType.equals("sqlite")) {
            this.storageManager = new SqliteStorage(this);
            getLogger().info("Using SQLite for data storage.");
        } else {
            this.storageManager = new YamlStorage(this);
            getLogger().info("Using YAML for data storage.");
        }

        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        getServer().getPluginManager().registerEvents(new NPCListener(this), this);

        if (Bukkit.getPluginManager().getPlugin("MythicMobs") != null) {
            getServer().getPluginManager().registerEvents(new MythicMobListener(this), this);
            getLogger().info("Successfully hooked into MythicMobs.");
        }

        getCommand("contract").setExecutor(new ContractCommand(this));

        getLogger().info("ContractRPG has been enabled!");
    }

    @Override
    public void onDisable() {
        if (storageManager != null) {
            storageManager.onDisable();
        }
        getLogger().info("ContractRPG has been disabled!");
    }

    public static ContractRPG getInstance() {
        return instance;
    }

    public ContractManager getContractManager() {
        return contractManager;
    }

    public LangManager getLangManager() {
        return langManager;
    }

    public StorageManager getStorageManager() {
        return storageManager;
    }
}
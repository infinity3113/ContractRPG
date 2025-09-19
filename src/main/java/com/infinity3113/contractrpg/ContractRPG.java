package com.infinity3113.contractrpg;

import com.infinity3113.contractrpg.commands.ContractCommand;
import com.infinity3113.contractrpg.contracts.Contract;
import com.infinity3113.contractrpg.contracts.ContractManager;
import com.infinity3113.contractrpg.contracts.ContractType;
import com.infinity3113.contractrpg.data.PlayerData;
import com.infinity3113.contractrpg.data.SqliteStorage;
import com.infinity3113.contractrpg.data.StorageManager;
import com.infinity3113.contractrpg.data.YamlStorage;
import com.infinity3113.contractrpg.listeners.GUIListener;
import com.infinity3113.contractrpg.listeners.MythicMobListener;
import com.infinity3113.contractrpg.listeners.NPCListener;
import com.infinity3113.contractrpg.listeners.PlayerListener;
import com.infinity3113.contractrpg.managers.*;
import com.infinity3113.contractrpg.util.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.IOException;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;

public final class ContractRPG extends JavaPlugin {

    private static ContractRPG instance;
    private ContractManager contractManager;
    private LangManager langManager;
    private StorageManager storageManager;
    private GUIManager guiManager;

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
        this.guiManager = new GUIManager(this);

        String storageType = getConfig().getString("storage.type", "yaml").toLowerCase();
        if (storageType.equals("sqlite")) {
            this.storageManager = new SqliteStorage(this);
            getLogger().info("Using SQLite for data storage.");
        } else {
            this.storageManager = new YamlStorage(this);
            getLogger().info("Using YAML for data storage.");
        }

        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        getServer().getPluginManager().registerEvents(new NPCListener(this), this);
        getServer().getPluginManager().registerEvents(new GUIListener(this), this);

        if (Bukkit.getPluginManager().getPlugin("MythicMobs") != null) {
            getServer().getPluginManager().registerEvents(new MythicMobListener(this), this);
            getLogger().info("Successfully hooked into MythicMobs.");
        }
        
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new PlaceholderManager(this).register();
            getLogger().info("Successfully hooked into PlaceholderAPI.");
        }

        getCommand("contract").setExecutor(new ContractCommand(this));
        startMissionResetTimer();
        getLogger().info("ContractRPG has been enabled!");
    }

    @Override
    public void onDisable() {
        if (storageManager != null) {
            storageManager.onDisable();
        }
        getLogger().info("ContractRPG has been disabled!");
    }

    public void completeContract(Player player, Contract contract) {
        PlayerData playerData = getStorageManager().getPlayerDataFromCache(player.getUniqueId());
        if (playerData == null) return;

        playerData.addExperience(contract.getExperienceReward());

        String rewardsString = String.join("<gray>, <white>", contract.getDisplayRewards());
        if(contract.getExperienceReward() > 0) {
            rewardsString += "<gray>, <white>" + contract.getExperienceReward() + " EXP";
        }
        
        String completionMessage = getLangManager().getMessage("contract_completed")
                .replace("%reward%", rewardsString);
        MessageUtils.sendMessage(player, completionMessage);

        String title = getLangManager().getMessage("contract_completed_title");
        String subtitle = getLangManager().getMessage("contract_completed_subtitle").replace("%mission%", contract.getDisplayName());
        player.sendTitle(title, subtitle, 10, 70, 20);

        for (String rewardCommand : contract.getRewards()) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), rewardCommand.replace("%player%", player.getName()));
        }

        if (contract.getContractType() == ContractType.DAILY) {
            playerData.addCompletedDailyContract(contract.getId());
        } else if (contract.getContractType() == ContractType.WEEKLY) {
            playerData.addCompletedWeeklyContract(contract.getId());
        }
        
        playerData.removeContract(contract.getId());
    }

    public void performMissionReset() {
        boolean resetDaily = getConfig().getBoolean("daily-missions.reset-unfinished-on-new-offer", false);
        boolean resetWeekly = getConfig().getBoolean("weekly-missions.reset-unfinished-on-new-offer", false);
        Calendar checkTime = Calendar.getInstance();
        boolean isWeeklyResetDay = (checkTime.get(Calendar.DAY_OF_WEEK) == Calendar.MONDAY);

        getLogger().info("Executing mission reset task...");

        for (Player player : Bukkit.getOnlinePlayers()) {
            PlayerData playerData = getStorageManager().getPlayerDataFromCache(player.getUniqueId());
            if (playerData == null) continue;

            boolean dailyProgressLost = false;
            boolean weeklyProgressLost = false;

            playerData.clearCompletedDailyContracts();

            if (isWeeklyResetDay) {
                playerData.clearCompletedWeeklyContracts();
            }

            Set<String> activeContracts = new HashSet<>(playerData.getActiveContracts().keySet());
            for (String contractId : activeContracts) {
                Contract contract = getContractManager().getContract(contractId);
                if (contract == null) continue;

                if (resetDaily && contract.getContractType() == ContractType.DAILY) {
                    playerData.removeContract(contractId);
                    dailyProgressLost = true;
                }

                if (isWeeklyResetDay && resetWeekly && contract.getContractType() == ContractType.WEEKLY) {
                    playerData.removeContract(contractId);
                    weeklyProgressLost = true;
                }
            }

            if (dailyProgressLost) {
                MessageUtils.sendMessage(player, getLangManager().getMessage("daily_mission_progress_lost"));
            }
            if (weeklyProgressLost) {
                MessageUtils.sendMessage(player, getLangManager().getMessage("weekly_mission_progress_lost"));
            }
            
            MessageUtils.sendMessage(player, getLangManager().getMessage("contracts_offered"));
        }
        getLogger().info("Mission reset task finished.");
    }

    private void startMissionResetTimer() {
        Calendar now = Calendar.getInstance();
        Calendar resetTime = (Calendar) now.clone();
        resetTime.set(Calendar.HOUR_OF_DAY, 0);
        resetTime.set(Calendar.MINUTE, 0);
        resetTime.set(Calendar.SECOND, 0);
        resetTime.set(Calendar.MILLISECOND, 0);
        if (now.after(resetTime)) {
            resetTime.add(Calendar.DATE, 1);
        }
        long delayInTicks = (resetTime.getTimeInMillis() - now.getTimeInMillis()) / 50;
        long periodInTicks = 24L * 60L * 60L * 20L;
        new BukkitRunnable() {
            @Override
            public void run() {
                performMissionReset();
            }
        }.runTaskTimer(this, delayInTicks, periodInTicks);
        getLogger().info("Mission reset timer has been scheduled successfully.");
    }

    public static ContractRPG getInstance() { return instance; }
    public ContractManager getContractManager() { return contractManager; }
    public LangManager getLangManager() { return langManager; }
    public StorageManager getStorageManager() { return storageManager; }
    public GUIManager getGuiManager() { return guiManager; }
}
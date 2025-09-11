package com.example.contractrpg.contracts;

import com.example.contractrpg.ContractRPG;
import com.example.contractrpg.data.PlayerData;
import com.example.contractrpg.managers.LangManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class ContractManager {

    private final ContractRPG plugin;
    private final Map<UUID, PlayerData> playerDataMap = new HashMap<>();

    private final List<Contract> easyDailyContracts = new ArrayList<>();
    private final List<Contract> mediumDailyContracts = new ArrayList<>();
    private final List<Contract> hardDailyContracts = new ArrayList<>();
    private final List<Contract> weeklyContracts = new ArrayList<>();

    public ContractManager(ContractRPG plugin) {
        this.plugin = plugin;
        loadContractsFromConfig();
    }

    public void loadPlayerDataIntoMap(Player player, PlayerData data) {
        playerDataMap.put(player.getUniqueId(), data);
    }

    public void removePlayerDataFromMap(Player player) {
        playerDataMap.remove(player.getUniqueId());
    }

    public PlayerData getPlayerData(Player player) {
        return playerDataMap.get(player.getUniqueId());
    }

    public void loadContractsFromConfig() {
        FileConfiguration contractsConfig = plugin.getContractsConfig();
        plugin.getLogger().info("Cargando contratos desde contracts.yml...");
        easyDailyContracts.clear();
        mediumDailyContracts.clear();
        hardDailyContracts.clear();
        weeklyContracts.clear();

        contractsConfig.getStringList("contracts.daily.EASY").forEach(s -> easyDailyContracts.add(parseContract(s, ContractType.DAILY)));
        contractsConfig.getStringList("contracts.daily.MEDIUM").forEach(s -> mediumDailyContracts.add(parseContract(s, ContractType.DAILY)));
        contractsConfig.getStringList("contracts.daily.HARD").forEach(s -> hardDailyContracts.add(parseContract(s, ContractType.DAILY)));
        
        contractsConfig.getStringList("contracts.weekly").forEach(s -> weeklyContracts.add(parseContract(s, ContractType.WEEKLY)));
        plugin.getLogger().info("¡Se cargaron " + (easyDailyContracts.size() + mediumDailyContracts.size() + hardDailyContracts.size()) + " contratos diarios y " + weeklyContracts.size() + " semanales!");
    }

    public void offerDailyContracts(Player player) {
        PlayerData data = getPlayerData(player);
        if (data == null || System.currentTimeMillis() < data.getNextDailyReset()) {
            return;
        }

        boolean shouldReset = plugin.getConfig().getBoolean("daily-missions.reset-unfinished-on-new-offer", true);
        if (shouldReset) {
            boolean progressLost = data.resetDailyContracts();
            if (progressLost) {
                plugin.getLangManager().sendMessage(player, "daily_mission_progress_lost");
            }
        } else {
            data.clearAvailableDaily();
        }
        
        int amountToOffer = plugin.getConfig().getInt("daily-missions.offered-amount", 4);
        
        List<Contract> allDaily = new ArrayList<>();
        allDaily.addAll(easyDailyContracts);
        allDaily.addAll(mediumDailyContracts);
        allDaily.addAll(hardDailyContracts);
        Collections.shuffle(allDaily);

        allDaily.stream().limit(amountToOffer).forEach(data::addAvailableDaily);

        data.setNextDailyReset(System.currentTimeMillis() + TimeUnit.HOURS.toMillis(24));
        plugin.getLangManager().sendMessage(player, "contracts_offered");
    }

    public void offerWeeklyContracts(Player player) {
        PlayerData data = getPlayerData(player);
        if (data == null || System.currentTimeMillis() < data.getNextWeeklyReset()) {
            return;
        }
    
        boolean shouldReset = plugin.getConfig().getBoolean("weekly-missions.reset-unfinished-on-new-offer", true);
        if (shouldReset) {
            boolean progressLost = data.resetWeeklyContracts();
            if (progressLost) {
                plugin.getLangManager().sendMessage(player, "weekly_mission_progress_lost");
            }
        } else {
            data.clearAvailableWeekly();
        }
        
        int amountToOffer = plugin.getConfig().getInt("weekly-missions.offered-amount", 3);
        
        Collections.shuffle(weeklyContracts);
        weeklyContracts.stream().limit(amountToOffer).forEach(data::addAvailableWeekly);
    
        data.setNextWeeklyReset(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(7));
    }
    
    public boolean acceptContract(Player player, Contract contract) {
        PlayerData data = getPlayerData(player);
        if (data == null) return false;

        int maxDaily = plugin.getConfig().getInt("daily-missions.selectable-amount", 2);
        
        long currentDailyAccepted = data.getActiveContracts().stream()
                                        .filter(c -> c.getContractType() == ContractType.DAILY)
                                        .count();

        if (currentDailyAccepted >= maxDaily) {
            plugin.getLangManager().sendMessage(player, "contract_limit_reached");
            return false;
        }

        if (data.getAvailableDailyContracts().contains(contract)) {
            data.acceptContract(contract);
            plugin.getLangManager().sendMessage(player, "contract_accepted");
            return true;
        }
        
        return false;
    }

    public void completeContract(Player player, Contract contract) {
        plugin.getEconomy().depositPlayer(player, contract.getReward());

        LangManager lang = plugin.getLangManager();
        String message = lang.getMessage("contract_completed")
                             .replace("%reward%", String.valueOf(contract.getReward()));
        
        Component component = MiniMessage.miniMessage().deserialize(message);
        String legacyMessage = LegacyComponentSerializer.legacySection().serialize(component);
        player.sendMessage(legacyMessage);
    }

    public void updatePlayerProgress(Player player, Contract contract) {
        if (contract.isCompleted()) {
            completeContract(player, contract);
        } else {
            String missionKey = contract.getMissionType().name();
            String missionFormat = plugin.getLangManager().getMessage("mission-formats." + missionKey);
            String targetKey = "translation-keys." + contract.getTarget().replace(":", ".");
            String translatedTarget = plugin.getLangManager().getMessage(targetKey);
            String missionName = missionFormat.replace("%amount%", "").replace("%target%", translatedTarget).trim();

            String actionBarFormat = plugin.getLangManager().getMessage("actionbar_progress");
            String message = actionBarFormat
                    .replace("%mission%", missionName)
                    .replace("%current%", String.valueOf(contract.getCurrentAmount()))
                    .replace("%required%", String.valueOf(contract.getRequiredAmount()));
            
            Component component = MiniMessage.miniMessage().deserialize(message);
            String legacyMessage = LegacyComponentSerializer.legacySection().serialize(component);
            
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(legacyMessage));
        }
    }
    
    public Contract parseContract(String contractString) {
        return parseContract(contractString, ContractType.DAILY); 
    }

    private Contract parseContract(String contractString, ContractType contractType) {
        try {
            String[] parts = contractString.split(":");
            MissionType type = MissionType.valueOf(parts[0].toUpperCase());
            String target = parts[1];
            int amountIndex = 2;
            int rewardIndex = 3;

            if (type == MissionType.DELIVER_MMOITEM) {
                target += ":" + parts[2];
                amountIndex = 3;
                rewardIndex = 4;
            }
            int amount = Integer.parseInt(parts[amountIndex]);
            double reward = Double.parseDouble(parts[rewardIndex]);

            return new Contract(contractType, type, target, amount, reward);
        } catch (Exception e) {
            plugin.getLogger().warning("Error al parsear el contrato: '" + contractString + "'. Formato incorrecto.");
            return null;
        }
    }
}
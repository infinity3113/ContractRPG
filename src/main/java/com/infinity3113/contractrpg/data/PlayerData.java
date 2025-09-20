package com.infinity3113.contractrpg.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.infinity3113.contractrpg.ContractRPG;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerData {
    private final UUID uuid;
    private int level;
    private int experience;
    private int contractPoints; // <-- AÑADIDO
    private Map<String, Integer> activeContracts;
    private Set<String> completedDailyContracts;
    private Set<String> completedWeeklyContracts;
    private Map<String, Long> purchasedShopItems; // <-- AÑADIDO PARA LA TIENDA
    private static final Gson gson = new GsonBuilder().create();

    public PlayerData(UUID uuid) {
        this.uuid = uuid;
        this.level = 1;
        this.experience = 0;
        this.contractPoints = 0; // <-- AÑADIDO
        this.activeContracts = new ConcurrentHashMap<>();
        this.completedDailyContracts = new HashSet<>();
        this.completedWeeklyContracts = new HashSet<>();
        this.purchasedShopItems = new ConcurrentHashMap<>(); // <-- AÑADIDO PARA LA TIENDA
    }
    //GETTERS
    public UUID getUuid() { return uuid; }
    public int getLevel() { return level; }
    public int getExperience() { return experience; }
    public int getContractPoints() { return contractPoints; } // <-- AÑADIDO
    public Map<String, Integer> getActiveContracts() { return activeContracts; }
    public Set<String> getCompletedDailyContracts() { return completedDailyContracts; }
    public Set<String> getCompletedWeeklyContracts() { return completedWeeklyContracts; }
    public Map<String, Long> getPurchasedShopItems() { return purchasedShopItems; } // <-- AÑADIDO PARA LA TIENDA

    //SETTERS
    public void setLevel(int level) { this.level = level; }
    public void setExperience(int experience) { this.experience = experience; }
    public void setContractPoints(int points) { this.contractPoints = points; } // <-- AÑADIDO
    public void setActiveContracts(Map<String, Integer> activeContracts) { this.activeContracts = activeContracts; }
    public void setCompletedDailyContracts(Set<String> contracts) { this.completedDailyContracts = contracts; }
    public void setCompletedWeeklyContracts(Set<String> contracts) { this.completedWeeklyContracts = contracts; }
    public void setPurchasedShopItems(Map<String, Long> items) { this.purchasedShopItems = items; } // <-- AÑADIDO PARA LA TIENDA

    // LOGICA
    public void addExperience(int amount) {
        this.experience += amount;
        checkLevelUp();
    }

    public void addContractPoints(int amount) { // <-- AÑADIDO
        this.contractPoints += amount;
    }

    public void addPurchasedShopItem(String itemId) { // <-- AÑADIDO PARA LA TIENDA
        this.purchasedShopItems.put(itemId, System.currentTimeMillis());
    }

    private void checkLevelUp() {
        int requiredExp = getRequiredExperience();
        while (this.experience >= requiredExp) {
            this.level++;
            this.experience -= requiredExp;
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                // Aquí podrías enviar un mensaje de subida de nivel, efectos, etc.
            }
            requiredExp = getRequiredExperience();
        }
    }

    public int getRequiredExperience() {
        return level * ContractRPG.getInstance().getConfig().getInt("leveling.experience-base", 250);
    }

    public int getContractProgress(String contractId) {
        return activeContracts.getOrDefault(contractId, 0);
    }

    public void setContractProgress(String contractId, int progress) {
        activeContracts.put(contractId, progress);
    }

    public void incrementContractProgress(String contractId, int amount) {
        int currentProgress = getContractProgress(contractId);
        setContractProgress(contractId, currentProgress + amount);
    }
    public void removeContract(String contractId) {
        activeContracts.remove(contractId);
    }

    public void addCompletedDailyContract(String contractId) {
        completedDailyContracts.add(contractId);
    }

    public void addCompletedWeeklyContract(String contractId) {
        completedWeeklyContracts.add(contractId);
    }

    public void clearCompletedDailyContracts() {
        completedDailyContracts.clear();
    }

    public void clearCompletedWeeklyContracts() {
        completedWeeklyContracts.clear();
    }

    //SERIALIZADORES
    public String serializeActiveContracts() {
        return gson.toJson(activeContracts);
    }

    public void deserializeActiveContracts(String json) {
        if (json == null || json.isEmpty()) {
            this.activeContracts = new ConcurrentHashMap<>();
            return;
        }
        Type type = new TypeToken<ConcurrentHashMap<String, Integer>>() {}.getType();
        this.activeContracts = gson.fromJson(json, type);
    }

    public String serializeCompletedDaily() {
        return String.join(",", completedDailyContracts);
    }

    public void deserializeCompletedDaily(String data) {
        if (data != null && !data.isEmpty()) {
            for (String id : data.split(",")) {
                completedDailyContracts.add(id);
            }
        }
    }

    public String serializeCompletedWeekly() {
        return String.join(",", completedWeeklyContracts);
    }

    public void deserializeCompletedWeekly(String data) {
        if (data != null && !data.isEmpty()) {
            for (String id : data.split(",")) {
                completedWeeklyContracts.add(id);
            }
        }
    }
    
    // ===== SERIALIZADORES PARA LA TIENDA AÑADIDOS =====
    public String serializePurchasedShopItems() {
        return gson.toJson(purchasedShopItems);
    }

    public void deserializePurchasedShopItems(String json) {
        if (json == null || json.isEmpty()) {
            this.purchasedShopItems = new ConcurrentHashMap<>();
            return;
        }
        Type type = new TypeToken<ConcurrentHashMap<String, Long>>() {}.getType();
        this.purchasedShopItems = gson.fromJson(json, type);
    }
}
package com.infinity3113.contractrpg.data;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

public class PlayerData {

    private static final Gson gson = new Gson();
    private static final Type CONTRACT_MAP_TYPE = new TypeToken<Map<String, Integer>>() {}.getType();
    private static final Type CONTRACT_SET_TYPE = new TypeToken<Set<String>>() {}.getType();

    private final UUID uuid;
    private int level;
    private int experience;

    private Map<String, Integer> activeContracts;
    private Set<String> completedDailyContracts;
    private Set<String> completedWeeklyContracts;

    public PlayerData(UUID uuid) {
        this.uuid = uuid;
        this.level = 1;
        this.experience = 0;
        this.activeContracts = new ConcurrentHashMap<>();
        this.completedDailyContracts = new CopyOnWriteArraySet<>();
        this.completedWeeklyContracts = new CopyOnWriteArraySet<>();
    }

    // --- Getters y Setters ---

    public UUID getUuid() { return uuid; }
    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }
    public int getExperience() { return experience; }
    public void setExperience(int experience) { this.experience = experience; }

    // --- Lógica de Contratos Activos ---

    public Map<String, Integer> getActiveContracts() { return activeContracts; }
    public void addContract(String contractId) { activeContracts.put(contractId, 0); }
    public void removeContract(String contractId) { activeContracts.remove(contractId); }
    public int getContractProgress(String contractId) { return activeContracts.getOrDefault(contractId, 0); }
    public void setContractProgress(String contractId, int progress) { activeContracts.put(contractId, progress); }

    // --- Lógica de Contratos Completados ---

    public Set<String> getCompletedDailyContracts() { return completedDailyContracts; }
    public void addCompletedDailyContract(String contractId) { this.completedDailyContracts.add(contractId); }
    public void clearCompletedDailyContracts() { this.completedDailyContracts.clear(); }

    public Set<String> getCompletedWeeklyContracts() { return completedWeeklyContracts; }
    public void addCompletedWeeklyContract(String contractId) { this.completedWeeklyContracts.add(contractId); }
    public void clearCompletedWeeklyContracts() { this.completedWeeklyContracts.clear(); }

    // --- Serialización y Deserialización (Guardado y Carga) ---

    public String serializeActiveContracts() {
        return gson.toJson(activeContracts);
    }

    public void deserializeActiveContracts(String data) {
        if (data == null || data.isEmpty()) {
            this.activeContracts = new ConcurrentHashMap<>();
            return;
        }
        Map<String, Integer> deserializedMap = gson.fromJson(data, CONTRACT_MAP_TYPE);
        this.activeContracts = (deserializedMap != null) ? new ConcurrentHashMap<>(deserializedMap) : new ConcurrentHashMap<>();
    }
    
    public String serializeCompletedDaily() {
        return gson.toJson(completedDailyContracts);
    }

    public void deserializeCompletedDaily(String data) {
        if (data == null || data.isEmpty()) {
            this.completedDailyContracts = new CopyOnWriteArraySet<>();
            return;
        }
        Set<String> deserializedSet = gson.fromJson(data, CONTRACT_SET_TYPE);
        this.completedDailyContracts = (deserializedSet != null) ? new CopyOnWriteArraySet<>(deserializedSet) : new CopyOnWriteArraySet<>();
    }

    public String serializeCompletedWeekly() {
        return gson.toJson(completedWeeklyContracts);
    }

    public void deserializeCompletedWeekly(String data) {
        if (data == null || data.isEmpty()) {
            this.completedWeeklyContracts = new CopyOnWriteArraySet<>();
            return;
        }
        Set<String> deserializedSet = gson.fromJson(data, CONTRACT_SET_TYPE);
        this.completedWeeklyContracts = (deserializedSet != null) ? new CopyOnWriteArraySet<>(deserializedSet) : new CopyOnWriteArraySet<>();
    }
}
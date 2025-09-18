package com.infinity3113.contractrpg.data;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.infinity3113.contractrpg.contracts.Contract;
import com.infinity3113.contractrpg.contracts.MissionType;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerData {

    // CORRECCIÓN: Se utiliza Gson para una serialización/deserialización segura a JSON.
    // Esto evita por completo los errores causados por comas o dos puntos en los IDs de los contratos.
    private static final Gson gson = new Gson();
    private static final Type CONTRACT_MAP_TYPE = new TypeToken<Map<String, Integer>>() {}.getType();

    private final UUID uuid;
    private int level;
    private int experience;
    private Map<String, Integer> activeContracts;

    public PlayerData(UUID uuid) {
        this.uuid = uuid;
        this.level = 1;
        this.experience = 0;
        this.activeContracts = new ConcurrentHashMap<>(); // Usar ConcurrentHashMap para seguridad en entornos asíncronos
    }

    public UUID getUuid() {
        return uuid;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public int getExperience() {
        return experience;
    }

    public void setExperience(int experience) {
        this.experience = experience;
    }

    public Map<String, Integer> getActiveContracts() {
        return activeContracts;
    }

    public void addContract(String contractId) {
        activeContracts.put(contractId, 0);
    }

    public void removeContract(String contractId) {
        activeContracts.remove(contractId);
    }

    public int getContractProgress(String contractId) {
        return activeContracts.getOrDefault(contractId, 0);
    }

    public void setContractProgress(String contractId, int progress) {
        activeContracts.put(contractId, progress);
    }

    // --- Métodos de Serialización/Deserialización con JSON ---

    public String serializeContracts() {
        if (activeContracts == null || activeContracts.isEmpty()) {
            return "{}"; // Devuelve un objeto JSON vacío en lugar de "null"
        }
        return gson.toJson(activeContracts);
    }

    public void deserializeContracts(String data) {
        if (data == null || data.isEmpty() || data.equals("{}")) {
            this.activeContracts = new ConcurrentHashMap<>();
            return;
        }
        Map<String, Integer> deserializedMap = gson.fromJson(data, CONTRACT_MAP_TYPE);
        // Asegurarse de que el mapa no sea nulo después de la deserialización
        this.activeContracts = (deserializedMap != null) ? new ConcurrentHashMap<>(deserializedMap) : new ConcurrentHashMap<>();
    }
}
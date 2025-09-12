package com.example.contractrpg.data;

import com.example.contractrpg.contracts.Contract;
import com.example.contractrpg.contracts.ContractType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class PlayerData {

    private final UUID playerUUID;
    private final List<Contract> activeContracts = new ArrayList<>();
    private final List<Contract> availableDailyContracts = new ArrayList<>();
    private final List<Contract> availableWeeklyContracts = new ArrayList<>();

    private long nextDailyReset;
    private long nextWeeklyReset;

    public PlayerData(UUID playerUUID) {
        this.playerUUID = playerUUID;
    }

    // --- Getters ---
    public UUID getPlayerUUID() { return playerUUID; }
    public List<Contract> getActiveContracts() { return activeContracts; }
    public List<Contract> getAvailableDailyContracts() { return availableDailyContracts; }
    public List<Contract> getAvailableWeeklyContracts() { return availableWeeklyContracts; }
    public long getNextDailyReset() { return nextDailyReset; }
    public long getNextWeeklyReset() { return nextWeeklyReset; }

    // --- Setters ---
    public void setNextDailyReset(long nextDailyReset) { this.nextDailyReset = nextDailyReset; }
    public void setNextWeeklyReset(long nextWeeklyReset) { this.nextWeeklyReset = nextWeeklyReset; }

    // --- Métodos de Contratos ---
    public void addAvailableDaily(Contract contract) { availableDailyContracts.add(contract); }
    public void addAvailableWeekly(Contract contract) { availableWeeklyContracts.add(contract); }
    
    // ** MÉTODO AÑADIDO QUE FALTABA **
    public void addActiveContract(Contract contract) { activeContracts.add(contract); }

    public void acceptContract(Contract contract) {
        if (availableDailyContracts.contains(contract)) {
            availableDailyContracts.remove(contract);
            activeContracts.add(contract);
        } else if (availableWeeklyContracts.contains(contract)) {
            availableWeeklyContracts.remove(contract);
            activeContracts.add(contract);
        }
    }

    public void clearAvailableDaily() { availableDailyContracts.clear(); }
    public void clearAvailableWeekly() { availableWeeklyContracts.clear(); }

    public boolean resetDailyContracts() {
        List<Contract> unfinished = activeContracts.stream()
                .filter(c -> c.getContractType() == ContractType.DAILY && c.getCurrentAmount() > 0)
                .collect(Collectors.toList());
        activeContracts.removeIf(c -> c.getContractType() == ContractType.DAILY);
        availableDailyContracts.clear();
        return !unfinished.isEmpty();
    }

    public boolean resetWeeklyContracts() {
        boolean progressLost = activeContracts.stream()
                .anyMatch(c -> c.getContractType() == ContractType.WEEKLY && c.getCurrentAmount() > 0);
        activeContracts.removeIf(c -> c.getContractType() == ContractType.WEEKLY);
        availableWeeklyContracts.clear();
        return progressLost;
    }
}
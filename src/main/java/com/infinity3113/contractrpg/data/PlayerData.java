package com.example.contractrpg.data;

import com.example.contractrpg.contracts.Contract;
import com.example.contractrpg.contracts.ContractType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PlayerData {

    private final UUID uuid;
    private long nextDailyReset;
    private long nextWeeklyReset;

    private final List<Contract> availableDailyContracts = new ArrayList<>();
    private final List<Contract> availableWeeklyContracts = new ArrayList<>();
    private final List<Contract> activeContracts = new ArrayList<>();

    public PlayerData(UUID uuid) {
        this.uuid = uuid;
    }

    public UUID getUuid() { return uuid; }
    public long getNextDailyReset() { return nextDailyReset; }
    public void setNextDailyReset(long nextDailyReset) { this.nextDailyReset = nextDailyReset; }
    public long getNextWeeklyReset() { return nextWeeklyReset; }
    public void setNextWeeklyReset(long nextWeeklyReset) { this.nextWeeklyReset = nextWeeklyReset; }
    public List<Contract> getAvailableDailyContracts() { return availableDailyContracts; }
    public List<Contract> getAvailableWeeklyContracts() { return availableWeeklyContracts; }
    public List<Contract> getActiveContracts() { return activeContracts; }

    public void clearAvailableDaily() { this.availableDailyContracts.clear(); }
    public void clearAvailableWeekly() { this.availableWeeklyContracts.clear(); }

    public boolean resetDailyContracts() {
        this.availableDailyContracts.clear();
        long activeDailyCount = this.activeContracts.stream()
                .filter(contract -> contract.getContractType() == ContractType.DAILY)
                .count();
        if (activeDailyCount > 0) {
            this.activeContracts.removeIf(contract -> contract.getContractType() == ContractType.DAILY);
            return true;
        }
        return false;
    }

    public boolean resetWeeklyContracts() {
        this.availableWeeklyContracts.clear();
        long activeWeeklyCount = this.activeContracts.stream()
                .filter(contract -> contract.getContractType() == ContractType.WEEKLY)
                .count();
        if (activeWeeklyCount > 0) {
            this.activeContracts.removeIf(contract -> contract.getContractType() == ContractType.WEEKLY);
            return true;
        }
        return false;
    }

    public void addAvailableDaily(Contract contract) { this.availableDailyContracts.add(contract); }
    public void addAvailableWeekly(Contract contract) { this.availableWeeklyContracts.add(contract); }

    public void acceptContract(Contract contract) {
        if (availableDailyContracts.remove(contract) || availableWeeklyContracts.remove(contract)) {
            activeContracts.add(contract);
        }
    }
}
package com.example.contractrpg.contracts;

public class Contract {

    private final ContractType contractType;
    private final MissionType missionType;
    private final String target;
    private final int requiredAmount;
    private final double reward; // <-- NUEVO CAMPO
    private int currentAmount;
    private boolean completed; // <-- NUEVO CAMPO para evitar recompensas duplicadas

    public Contract(ContractType contractType, MissionType missionType, String target, int requiredAmount, double reward) {
        this.contractType = contractType;
        this.missionType = missionType;
        this.target = target;
        this.requiredAmount = requiredAmount;
        this.reward = reward; // <-- NUEVO
        this.currentAmount = 0;
        this.completed = false; // <-- NUEVO
    }

    // --- Getters ---
    public ContractType getContractType() { return contractType; }
    public MissionType getMissionType() { return missionType; }
    public String getTarget() { return target; }
    public int getRequiredAmount() { return requiredAmount; }
    public double getReward() { return reward; } // <-- NUEVO GETTER
    public int getCurrentAmount() { return currentAmount; }
    public boolean isCompleted() { return completed; }

    // --- Setters ---
    public void setCurrentAmount(int currentAmount) {
        this.currentAmount = currentAmount;
        if (this.currentAmount >= this.requiredAmount) {
            this.completed = true;
        }
    }
}
package com.infinity3113.contractrpg.contracts;

import java.util.List;

public class Contract {

    private final String id;
    private final String displayName;
    private final List<String> description;
    private final ContractType contractType;
    private final MissionType missionType;
    private final String missionObjective;
    private final int missionRequirement;
    private final List<String> rewards;
    private final List<String> displayRewards;
    private final int experienceReward;
    private final int contractPointsReward; // <-- AÑADIDO
    private final int levelRequirement;

    public Contract(String id, String displayName, List<String> description, ContractType contractType, MissionType missionType, String missionObjective, int missionRequirement, List<String> rewards, List<String> displayRewards, int experienceReward, int contractPointsReward, int levelRequirement) {
        this.id = id;
        this.displayName = displayName;
        this.description = description;
        this.contractType = contractType;
        this.missionType = missionType;
        this.missionObjective = missionObjective;
        this.missionRequirement = missionRequirement;
        this.rewards = rewards;
        this.displayRewards = displayRewards;
        this.experienceReward = experienceReward;
        this.contractPointsReward = contractPointsReward; // <-- AÑADIDO
        this.levelRequirement = levelRequirement;
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public List<String> getDescription() { return description; }
    public ContractType getContractType() { return contractType; }
    public MissionType getMissionType() { return missionType; }
    public String getMissionObjective() { return missionObjective; }
    public int getMissionRequirement() { return missionRequirement; }
    public List<String> getRewards() { return rewards; }
    public List<String> getDisplayRewards() { return displayRewards; }
    public int getLevelRequirement() { return levelRequirement; }
    public int getExperienceReward() { return experienceReward; }
    public int getContractPointsReward() { return contractPointsReward; } // <-- AÑADIDO
}
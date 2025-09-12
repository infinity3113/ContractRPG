package com.example.contractrpg.data;

import com.example.contractrpg.ContractRPG;
import com.example.contractrpg.contracts.Contract;
import com.example.contractrpg.contracts.ContractManager;
import org.bukkit.entity.Player;

public abstract class StorageManager {

    protected final ContractRPG plugin;
    protected final ContractManager contractManager;

    public StorageManager(ContractRPG plugin, ContractManager contractManager) {
        this.plugin = plugin;
        this.contractManager = contractManager;
    }

    public abstract void init();

    public abstract void shutdown();

    public abstract void loadPlayerData(Player player);

    public abstract void savePlayerData(Player player);

    /**
     * Convierte un objeto Contract a un String para guardarlo.
     * Formato: MISSION_TYPE:TARGET:REQUIRED_AMOUNT:REWARD
     * @param contract El contrato a convertir.
     * @return Una cadena de texto que representa el contrato.
     */
    protected String contractToString(Contract contract) {
        return contract.getMissionType().name() + ":" +
               contract.getTarget() + ":" +
               contract.getRequiredAmount() + ":" +
               contract.getReward();
    }
}
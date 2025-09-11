package com.example.contractrpg.data;

import org.bukkit.entity.Player;

public interface StorageManager {
    void init();
    void loadPlayerData(Player player);
    void savePlayerData(Player player);
    void shutdown();
}
package com.infinity3113.contractrpg.data;

import com.infinity3113.contractrpg.ContractRPG;
import org.bukkit.Bukkit;

import java.io.File;
import java.sql.*;
import java.util.UUID;

public class SqliteStorage extends StorageManager {

    private Connection connection;

    public SqliteStorage(ContractRPG plugin) {
        super(plugin);
        connect();
    }

    private void connect() {
        try {
            File dbFile = new File(plugin.getDataFolder(), "playerdata.db");
            String url = "jdbc:sqlite:" + dbFile.getAbsolutePath();
            connection = DriverManager.getConnection(url);

            try (Statement stmt = connection.createStatement()) {
                // Actualizar la tabla para incluir los nuevos campos
                String sql = "CREATE TABLE IF NOT EXISTS player_data (" +
                        "uuid TEXT PRIMARY KEY NOT NULL," +
                        "level INTEGER NOT NULL," +
                        "experience INTEGER NOT NULL," +
                        "active_contracts TEXT," +
                        "completed_daily TEXT," +
                        "completed_weekly TEXT," +
                        "purchased_shop_items TEXT" + // <-- MODIFICADO
                        ");";
                stmt.execute(sql);
                
                // Asegurarse de que las columnas existan si la tabla ya fue creada
                addColumnIfNotExists("completed_daily", "TEXT");
                addColumnIfNotExists("completed_weekly", "TEXT");
                addColumnIfNotExists("purchased_shop_items", "TEXT"); // <-- MODIFICADO
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Could not connect to SQLite database!");
            e.printStackTrace();
        }
    }

    @Override
    public void loadPlayerDataAsync(UUID uuid) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            PlayerData playerData = null;
            String sql = "SELECT level, experience, active_contracts, completed_daily, completed_weekly, purchased_shop_items FROM player_data WHERE uuid = ?"; // <-- MODIFICADO
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, uuid.toString());
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    playerData = new PlayerData(uuid);
                    playerData.setLevel(rs.getInt("level"));
                    playerData.setExperience(rs.getInt("experience"));
                    
                    playerData.deserializeActiveContracts(rs.getString("active_contracts"));
                    playerData.deserializeCompletedDaily(rs.getString("completed_daily"));
                    playerData.deserializeCompletedWeekly(rs.getString("completed_weekly"));
                    playerData.deserializePurchasedShopItems(rs.getString("purchased_shop_items")); // <-- MODIFICADO
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Error loading data for " + uuid.toString());
                e.printStackTrace();
            }

            final PlayerData finalPlayerData = (playerData != null) ? playerData : new PlayerData(uuid);

            Bukkit.getScheduler().runTask(plugin, () -> {
                addToCache(uuid, finalPlayerData);
            });
        });
    }


    @Override
    public void savePlayerDataAsync(PlayerData playerData) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            savePlayerDataSync(playerData);
        });
    }

    @Override
    public void savePlayerDataSync(PlayerData playerData) {
        String sql = "INSERT OR REPLACE INTO player_data (uuid, level, experience, active_contracts, completed_daily, completed_weekly, purchased_shop_items) VALUES (?, ?, ?, ?, ?, ?, ?)"; // <-- MODIFICADO
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, playerData.getUuid().toString());
            pstmt.setInt(2, playerData.getLevel());
            pstmt.setInt(3, playerData.getExperience());
            pstmt.setString(4, playerData.serializeActiveContracts());
            pstmt.setString(5, playerData.serializeCompletedDaily());
            pstmt.setString(6, playerData.serializeCompletedWeekly());
            pstmt.setString(7, playerData.serializePurchasedShopItems()); // <-- MODIFICADO
            pstmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Error saving data for " + playerData.getUuid().toString());
            e.printStackTrace();
        }
    }
    
    private void addColumnIfNotExists(String columnName, String columnType) {
        try {
            DatabaseMetaData md = connection.getMetaData();
            ResultSet rs = md.getColumns(null, null, "player_data", columnName);
            if (!rs.next()) {
                try (Statement stmt = connection.createStatement()) {
                    stmt.execute("ALTER TABLE player_data ADD COLUMN " + columnName + " " + columnType);
                }
            }
        } catch (SQLException e) {
            if (!e.getMessage().contains("duplicate column name")) {
                 plugin.getLogger().severe("Could not update SQLite table schema!");
                 e.printStackTrace();
            }
        }
    }
}
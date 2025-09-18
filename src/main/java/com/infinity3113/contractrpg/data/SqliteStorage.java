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
                String sql = "CREATE TABLE IF NOT EXISTS player_data (" +
                        "uuid TEXT PRIMARY KEY NOT NULL," +
                        "level INTEGER NOT NULL," +
                        "experience INTEGER NOT NULL," +
                        "contracts TEXT" +
                        ");";
                stmt.execute(sql);
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Could not connect to SQLite database!");
            e.printStackTrace();
        }
    }

    @Override
    public void loadPlayerDataAsync(UUID uuid) {
        // CORRECCIÓN: La operación de base de datos se ejecuta en un hilo secundario.
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            PlayerData playerData = null;
            String sql = "SELECT level, experience, contracts FROM player_data WHERE uuid = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, uuid.toString());
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    playerData = new PlayerData(uuid);
                    playerData.setLevel(rs.getInt("level"));
                    playerData.setExperience(rs.getInt("experience"));
                    playerData.deserializeContracts(rs.getString("contracts"));
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Error loading data for " + uuid.toString());
                e.printStackTrace();
            }

            final PlayerData finalPlayerData = (playerData != null) ? playerData : new PlayerData(uuid);

            // Una vez cargados los datos, los añadimos a la caché en el hilo principal.
            Bukkit.getScheduler().runTask(plugin, () -> {
                addToCache(uuid, finalPlayerData);
            });
        });
    }


    @Override
    public void savePlayerDataAsync(PlayerData playerData) {
        // CORRECCIÓN: Guardado asíncrono.
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            savePlayerDataSync(playerData);
        });
    }

    @Override
    public void savePlayerDataSync(PlayerData playerData) {
        String sql = "INSERT OR REPLACE INTO player_data (uuid, level, experience, contracts) VALUES (?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, playerData.getUuid().toString());
            pstmt.setInt(2, playerData.getLevel());
            pstmt.setInt(3, playerData.getExperience());
            pstmt.setString(4, playerData.serializeContracts());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Error saving data for " + playerData.getUuid().toString());
            e.printStackTrace();
        }
    }
}
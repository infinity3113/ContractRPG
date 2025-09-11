package com.example.contractrpg.data;

import com.example.contractrpg.ContractRPG;
import com.example.contractrpg.contracts.Contract;
import com.example.contractrpg.contracts.ContractManager;
import org.bukkit.entity.Player;

import java.io.File;
import java.sql.*;
import java.util.Locale;

public class SqliteStorage implements StorageManager {

    private final ContractRPG plugin;
    private final ContractManager contractManager;
    private Connection connection;

    public SqliteStorage(ContractRPG plugin, ContractManager contractManager) {
        this.plugin = plugin;
        this.contractManager = contractManager;
    }

    @Override
    public void init() {
        File dbFile = new File(plugin.getDataFolder(), "playerdata.db");
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
            plugin.getLogger().info("Conexión con SQLite establecida.");
            createTables();
        } catch (Exception e) {
            plugin.getLogger().severe("No se pudo conectar a la base de datos SQLite.");
            e.printStackTrace();
        }
    }
    
    private void createTables() throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE IF NOT EXISTS player_data (" +
                                "uuid TEXT PRIMARY KEY, " +
                                "next_daily_reset BIGINT NOT NULL, " +
                                "next_weekly_reset BIGINT NOT NULL);");
            statement.execute("CREATE TABLE IF NOT EXISTS active_contracts (" +
                                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                                "player_uuid TEXT NOT NULL, " +
                                "contract_string TEXT NOT NULL, " +
                                "progress INTEGER NOT NULL, " +
                                "FOREIGN KEY (player_uuid) REFERENCES player_data(uuid));");
        }
    }

    @Override
    public void loadPlayerData(Player player) {
        PlayerData data = new PlayerData(player.getUniqueId());
        String uuid = player.getUniqueId().toString();

        String playerDataSQL = "SELECT * FROM player_data WHERE uuid = ?";
        String contractsSQL = "SELECT * FROM active_contracts WHERE player_uuid = ?";
        
        try {
            try (PreparedStatement ps = connection.prepareStatement(playerDataSQL)) {
                ps.setString(1, uuid);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    data.setNextDailyReset(rs.getLong("next_daily_reset"));
                    data.setNextWeeklyReset(rs.getLong("next_weekly_reset"));
                }
            }
            try (PreparedStatement ps = connection.prepareStatement(contractsSQL)) {
                ps.setString(1, uuid);
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    Contract contract = contractManager.parseContract(rs.getString("contract_string"));
                    if (contract != null) {
                        contract.setCurrentAmount(rs.getInt("progress"));
                        data.getActiveContracts().add(contract);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        contractManager.loadPlayerDataIntoMap(player, data);
    }

    @Override
    public void savePlayerData(Player player) {
        PlayerData data = contractManager.getPlayerData(player);
        if (data == null) return;
        String uuid = player.getUniqueId().toString();
        
        String playerDataSQL = "INSERT OR REPLACE INTO player_data (uuid, next_daily_reset, next_weekly_reset) VALUES (?, ?, ?)";
        String deleteContractsSQL = "DELETE FROM active_contracts WHERE player_uuid = ?";
        String insertContractSQL = "INSERT INTO active_contracts (player_uuid, contract_string, progress) VALUES (?, ?, ?)";

        try {
            connection.setAutoCommit(false);

            try (PreparedStatement ps = connection.prepareStatement(playerDataSQL)) {
                ps.setString(1, uuid);
                ps.setLong(2, data.getNextDailyReset());
                ps.setLong(3, data.getNextWeeklyReset());
                ps.executeUpdate();
            }
            
            try (PreparedStatement ps = connection.prepareStatement(deleteContractsSQL)) {
                ps.setString(1, uuid);
                ps.executeUpdate();
            }

            for (Contract contract : data.getActiveContracts()) {
                 try (PreparedStatement ps = connection.prepareStatement(insertContractSQL)) {
                    String contractString = String.format(Locale.US, "%s:%s:%d:%f",
                        contract.getMissionType().name(), contract.getTarget(),
                        contract.getRequiredAmount(), contract.getReward());
                    ps.setString(1, uuid);
                    ps.setString(2, contractString);
                    ps.setInt(3, contract.getCurrentAmount());
                    ps.executeUpdate();
                }
            }
            connection.commit();
        } catch (SQLException e) {
            e.printStackTrace();
            try {
                connection.rollback();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        } finally {
            try {
                connection.setAutoCommit(true);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void shutdown() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
package com.example.contractrpg.data;

import com.example.contractrpg.ContractRPG;
import com.example.contractrpg.contracts.Contract;
import com.example.contractrpg.contracts.ContractManager;
import com.example.contractrpg.contracts.ContractType;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.sql.*;

public class SqliteStorage extends StorageManager {

    private Connection connection;

    public SqliteStorage(ContractRPG plugin, ContractManager contractManager) {
        super(plugin, contractManager);
    }

    @Override
    public void init() {
        File dbFile = new File(plugin.getDataFolder(), "playerdata.db");
        if (!dbFile.exists()) {
            try {
                dbFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
            plugin.getLogger().info("Conexión con SQLite establecida.");
            createTables();
        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
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

    @Override
    public void loadPlayerData(Player player) {
        PlayerData playerData = new PlayerData(player.getUniqueId());
        
        try (PreparedStatement ps = connection.prepareStatement("SELECT * FROM player_contracts WHERE uuid = ?")) {
            ps.setString(1, player.getUniqueId().toString());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String contractString = rs.getString("contract_string");
                ContractType type = ContractType.valueOf(rs.getString("contract_type"));
                int progress = rs.getInt("progress");

                Contract contract = contractManager.parseContract(contractString, type);
                if (contract != null) {
                    contract.setCurrentAmount(progress);
                    playerData.addActiveContract(contract);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        contractManager.loadPlayerDataIntoMap(player, playerData);
    }

    @Override
    public void savePlayerData(Player player) {
        PlayerData playerData = contractManager.getPlayerData(player);
        if (playerData == null) return;
        
        String deleteSql = "DELETE FROM player_contracts WHERE uuid = ?";
        try (PreparedStatement psDelete = connection.prepareStatement(deleteSql)) {
            psDelete.setString(1, player.getUniqueId().toString());
            psDelete.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        String insertSql = "INSERT INTO player_contracts(uuid, contract_type, contract_string, progress) VALUES(?,?,?,?)";
        try (PreparedStatement psInsert = connection.prepareStatement(insertSql)) {
            for (Contract contract : playerData.getActiveContracts()) {
                psInsert.setString(1, player.getUniqueId().toString());
                psInsert.setString(2, contract.getContractType().name());
                psInsert.setString(3, contractToString(contract));
                psInsert.setInt(4, contract.getCurrentAmount());
                psInsert.addBatch();
            }
            psInsert.executeBatch();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    private void createTables() {
        String sql = "CREATE TABLE IF NOT EXISTS player_contracts (" +
                     "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                     "uuid TEXT NOT NULL," +
                     "contract_type TEXT NOT NULL," +
                     "contract_string TEXT NOT NULL," +
                     "progress INTEGER NOT NULL" +
                     ");";
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
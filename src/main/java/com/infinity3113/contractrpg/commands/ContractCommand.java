package com.infinity3113.contractrpg.commands;

import com.infinity3113.contractrpg.ContractRPG;
import com.infinity3113.contractrpg.data.PlayerData;
import com.infinity3113.contractrpg.gui.ContractGUI;
import com.infinity3113.contractrpg.managers.LangManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;

public class ContractCommand implements CommandExecutor {

    private final ContractRPG plugin;
    private final LangManager langManager;

    public ContractCommand(ContractRPG plugin) {
        this.plugin = plugin;
        this.langManager = plugin.getLangManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be run by a player.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length > 0) {
            // Subcomando de recarga
            if (args[0].equalsIgnoreCase("reload")) {
                if (!player.hasPermission("contractrpg.reload")) {
                    player.sendMessage(langManager.getMessage("no-permission"));
                    return true;
                }
                plugin.reloadConfig();
                plugin.getLangManager().loadLanguages();
                plugin.getContractManager().loadContracts();

                for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                    PlayerData playerData = plugin.getStorageManager().getPlayerDataFromCache(onlinePlayer.getUniqueId());
                    if (playerData != null) {
                        Set<String> activeContractsCopy = new HashSet<>(playerData.getActiveContracts().keySet());
                        for (String contractId : activeContractsCopy) {
                            if (plugin.getContractManager().getContract(contractId) == null) {
                                playerData.removeContract(contractId);
                                onlinePlayer.sendMessage(langManager.getMessage("contract-removed-on-reload").replace("%contract%", contractId));
                            }
                        }
                    }
                }
                player.sendMessage(langManager.getMessage("plugin-reloaded"));
                return true;
            }

            // ¡NUEVO! Subcomando de reseteo
            if (args[0].equalsIgnoreCase("reset")) {
                if (!player.hasPermission("contractrpg.admin.reset")) {
                    player.sendMessage(langManager.getMessage("no-permission"));
                    return true;
                }
                plugin.performMissionReset();
                player.sendMessage(langManager.getMessage("missions-manually-reset"));
                return true;
            }
        }

        // Si no hay argumentos o no coinciden, abre la GUI
        new ContractGUI(plugin, player).open();
        return true;
    }
}
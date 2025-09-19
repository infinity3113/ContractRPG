package com.infinity3113.contractrpg.commands;

import com.infinity3113.contractrpg.ContractRPG;
import com.infinity3113.contractrpg.data.PlayerData;
import com.infinity3113.contractrpg.managers.LangManager;
import com.infinity3113.contractrpg.util.MessageUtils;
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
            if (args[0].equalsIgnoreCase("reload")) {
                if (!player.hasPermission("contractrpg.reload")) {
                    MessageUtils.sendMessage(player, langManager.getMessage("no-permission"));
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
                                MessageUtils.sendMessage(onlinePlayer, langManager.getMessage("contract-removed-on-reload").replace("%contract%", contractId));
                            }
                        }
                    }
                }
                MessageUtils.sendMessage(player, langManager.getMessage("plugin-reloaded"));
                return true;
            }

            if (args[0].equalsIgnoreCase("reset")) {
                if (!player.hasPermission("contractrpg.admin.reset")) {
                    MessageUtils.sendMessage(player, langManager.getMessage("no-permission"));
                    return true;
                }
                plugin.performMissionReset();
                MessageUtils.sendMessage(player, langManager.getMessage("missions-manually-reset"));
                return true;
            }
        }

        // Abrir el nuevo menú principal
        plugin.getGuiManager().openMainMenu(player);
        return true;
    }
}
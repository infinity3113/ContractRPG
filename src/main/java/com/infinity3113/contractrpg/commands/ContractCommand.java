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

        if (args.length == 0) {
            new ContractGUI(plugin, player).open();
            return true;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            if (!player.hasPermission("contractrpg.reload")) {
                player.sendMessage(langManager.getMessage("no-permission"));
                return true;
            }

            plugin.reloadConfig();
            plugin.getLangManager().loadLanguages();
            plugin.getContractManager().loadContracts();

            // CORRECCIÓN: Se valida los contratos de los jugadores en línea después de una recarga.
            // Esto evita errores si un contrato que un jugador tenía activo es eliminado.
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

        return true;
    }
}
package com.infinity3113.contractrpg.commands;

import com.infinity3113.contractrpg.ContractRPG;
import com.infinity3113.contractrpg.data.PlayerData;
import com.infinity3113.contractrpg.managers.LangManager;
import com.infinity3113.contractrpg.util.MessageUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

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
                plugin.getShopManager().reloadConfig();
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

            if (args[0].equalsIgnoreCase("placeholder") || args[0].equalsIgnoreCase("placeholders")) {
                if (!player.hasPermission("contractrpg.command.placeholder")) {
                    MessageUtils.sendMessage(player, langManager.getMessage("no-permission"));
                    return true;
                }
                if (plugin.getPlaceholderManager() == null) {
                    player.sendMessage(MessageUtils.parse("&cPlaceholderAPI is not enabled on the server."));
                    return true;
                }
                List<String> placeholders = plugin.getPlaceholderManager().getPlaceholders();
                MessageUtils.sendMessage(player, langManager.getMessage("placeholder-list-header"));
                for (String placeholder : placeholders) {
                    String formattedPlaceholder = langManager.getMessage("placeholder-list-format")
                            .replace("%placeholder%", placeholder);
                    player.sendMessage(MessageUtils.parse(formattedPlaceholder));
                }
                return true;
            }

            if (args[0].equalsIgnoreCase("stats")) {
                PlayerData playerData = plugin.getStorageManager().getPlayerDataFromCache(player.getUniqueId());
                if (playerData == null) return true;

                List<String> statsMessage = langManager.getMessageList("stats_message");
                for (String line : statsMessage) {
                    player.sendMessage(MessageUtils.parse(line
                            .replace("%level%", String.valueOf(playerData.getLevel()))
                            .replace("%current_exp%", String.valueOf(playerData.getExperience()))
                            .replace("%required_exp%", String.valueOf(playerData.getRequiredExperience()))
                            .replace("%contract_points%", String.valueOf(playerData.getContractPoints()))
                    ));
                }
                return true;
            }

            if (args[0].equalsIgnoreCase("shop") || args[0].equalsIgnoreCase("tienda")) {
                if (!player.hasPermission("contractrpg.shop.open")) {
                    MessageUtils.sendMessage(player, langManager.getMessage("no-permission"));
                    return true;
                }
                // ===== CORREGIDO: Se abre la página 0 =====
                plugin.getShopGUI().openShop(player, 0);
                return true;
            }

            if (args[0].equalsIgnoreCase("shopedit")) {
                if (!player.hasPermission("contractrpg.admin.shopedit")) {
                    MessageUtils.sendMessage(player, langManager.getMessage("no-permission"));
                    return true;
                }
                // ===== CORREGIDO: Se abre la página 0 =====
                plugin.getShopGUI().openEditor(player, 0);
                return true;
            }
        }

        plugin.getGuiManager().openMainGUI(player);
        return true;
    }
}
package com.example.contractrpg.commands;

import com.example.contractrpg.ContractRPG;
import com.example.contractrpg.gui.ContractGUI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class ContractCommand implements CommandExecutor {

    private final ContractRPG plugin;

    public ContractCommand(ContractRPG plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Este comando solo puede ser usado por jugadores.");
            return true;
        }

        Player player = (Player) sender;
        
        // Abrimos el nuevo menú principal de la GUI
        new ContractGUI(plugin, player).openMainMenu();

        return true;
    }
}
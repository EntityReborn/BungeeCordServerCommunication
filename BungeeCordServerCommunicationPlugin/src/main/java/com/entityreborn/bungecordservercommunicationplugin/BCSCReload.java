/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.entityreborn.bungecordservercommunicationplugin;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.plugin.Command;

/**
 *
 * @author LewsTherin
 */
public class BCSCReload extends Command
{
    BCSCPlugin plugin;
    public BCSCReload()
    {
        super( "bcscreload", "bungeecord.command.reload" );
    }
    
    public void setPlugin(BCSCPlugin plug) {
        this.plugin = plug;
    }

    @Override
    public void execute(CommandSender sender, String[] args)
    {
        plugin.onDisable();
        plugin.onEnable();
        sender.sendMessage( ChatColor.BOLD.toString() + ChatColor.RED.toString() + "BCSC has been reloaded."
                + " This is NOT advisable and you will not be supported with any issues that arise! Please restart BCSC ASAP." );
    }
}

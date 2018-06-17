/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.entityreborn.bungecordservercommunicationplugin;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.md_5.bungee.api.event.LoginEvent;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.event.PreLoginEvent;
import net.md_5.bungee.api.event.ServerConnectEvent;
import net.md_5.bungee.api.event.ServerConnectedEvent;
import net.md_5.bungee.api.event.ServerDisconnectEvent;
import net.md_5.bungee.api.event.ServerSwitchEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;
import net.md_5.bungee.event.EventHandler;
import org.json.simple.JSONObject;
import org.zeromq.ZAuth;
import org.zeromq.ZCertStore;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

public class BCSCPlugin extends Plugin implements Listener
{
    public static ZContext context;
    public static Publisher publisher;
    public static ZAuth authentication;
    public static BCSCReload reloadCommand;
    Config configuration;
    public HashMap<UUID, String> previousServers = new HashMap<>();
    
    public void WriteConsole(String text) {
        WriteConsole(text, false);
    }
    
    public void WriteConsole(String text, boolean debug) {
        if (!debug || (debug && configuration.IsDebugMode())) {
            System.out.println("[BCSC] " + text);
        }
    }
    
    @Override
    public void onEnable()
    {
        if (!getDataFolder().exists())
            getDataFolder().mkdir();
        
        File file = new File(getDataFolder(), "config.yml");
        File certdir = new File(getDataFolder(), "certs");
        
        if (!certdir.exists())
            certdir.mkdir();
        
        if (!file.exists()) {
            WriteConsole("Config file doesn't exist. Creating with defaults to listen on all IPs on port 5556.");
            
            try {
                file.createNewFile();
                
                try (InputStream in = getResourceAsStream("config.yml")) {
                    OutputStream os = new FileOutputStream(file);
                    
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    //read from is to buffer
                    while((bytesRead = in.read(buffer)) !=-1){
                        os.write(buffer, 0, bytesRead);
                    }
                    in.close();
                    //flush OutputStream to write any buffered data to file
                    os.flush();
                    os.close();
                } catch (IOException e) {
                    WriteConsole("Could not create default config file. Bailing!");
                    WriteConsole("!!! Error: " + e.getMessage());

                    return;
                }
            } catch (IOException ex) {
                WriteConsole("Could not create default config file. Bailing!");
                WriteConsole("!!! Error: " + ex.getMessage());
                
                return;
            }
        }
        Configuration config;
        try {
            config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(new File(getDataFolder(), "config.yml"));
        } catch (IOException ex) {
            WriteConsole("Could not load config file. Bailing!");
            WriteConsole("!!! Error: " + ex.getMessage());
                    
            return;
        }
        configuration = new Config(config);
        
        WriteConsole("Publisher ID: " + configuration.PublisherID());
        WriteConsole("Listening mode: " + configuration.IsListenMode());
        WriteConsole("Endpoint: " + configuration.Endpoint());
        WriteConsole("Datatype: " + configuration.DataType());
        WriteConsole("Channel prefix: " + configuration.ChannelPrefix());
        WriteConsole("Debug mode: " + configuration.IsDebugMode());
        WriteConsole("Using cert auth: " + configuration.UseCertAuth());
        WriteConsole("IP Filtering: " + configuration.IPFilterType());
        
        if (configuration.DataType() == Config.DataType.JSON) {
            NodePoint.DataStructureType = NodePoint.DataType.Json;
        }
        
        getProxy().getPluginManager().registerListener(this, this);
        if (reloadCommand == null) {
            reloadCommand = new BCSCReload();
            reloadCommand.setPlugin(this);
            
            getProxy().getPluginManager().registerCommand(this, reloadCommand);
        }
        
        context = new ZContext(1);
        
        if (configuration.UseCertAuth()) {
            publisher.socket.setZAPDomain("global");
            
            authentication = new ZAuth(context, new ZCertStore.Hasher());
            authentication.setVerbose(configuration.VerboseAuth());
            authentication.configureCurve(certdir.getAbsolutePath());
        }
        
        publisher = new Publisher(configuration.PublisherID());
        publisher.init(context);
        
        if (configuration.UseCertAuth()) {
            publisher.socket.setCurveServer(true);
            publisher.socket.setCurvePublicKey(configuration.CertPublicKey().getBytes());
            publisher.socket.setCurveSecretKey(configuration.CertSecretKey().getBytes());
        }
        
        publisher.start();
        
        if (configuration.IsListenMode()) {
            publisher.listen(configuration.Endpoint());
        } else {
            publisher.connect(configuration.Endpoint());
        }
    }

    @Override
    public void onDisable() {
        if (configuration == null) {
            return;
        }
        
        publisher.stop();
        publisher = null;
        
        if (configuration.UseCertAuth() && authentication != null) {
            authentication.destroy();
            authentication = null;
        }
        
        context.destroy();
        context = null;
    }
    
    @EventHandler
    public void onEvent(LoginEvent event) {
        WriteConsole("Login", true);
    }
    
    @EventHandler
    public void onEvent(PlayerDisconnectEvent event) {
        WriteConsole("PlayerDisconnect", true);
        
        try {
            JSONObject obj = new JSONObject();
            obj.put("state", "disconnected");
            obj.put("username", event.getPlayer().getName());
            obj.put("uuid", event.getPlayer().getUniqueId().toString());
            obj.put("server", previousServers.get(event.getPlayer().getUniqueId()));
            
            previousServers.remove(event.getPlayer().getUniqueId());
            
            publisher.publish(configuration.ChannelPrefix() + "disconnected", obj.toJSONString());
        } catch (Exceptions.InvalidChannelException ex) {
            Logger.getLogger(BCSCPlugin.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    @EventHandler
    public void onEvent(PostLoginEvent event) {
        WriteConsole("PostLogin", true);
    }
    
    @EventHandler
    public void onEvent(PreLoginEvent event) {
        WriteConsole("PreLogin", true);
    }
    
    @EventHandler
    public void onEvent(ServerConnectEvent event) {
        WriteConsole("ServerConnect", true);
    }
    
    @EventHandler
    public void onEvent(ServerConnectedEvent event) {
        WriteConsole("ServerConnected", true);
        
        if (previousServers.containsKey(event.getPlayer().getUniqueId())) {
            return;
        }
        
        try {
            JSONObject obj = new JSONObject();
            obj.put("state", "connected");
            obj.put("username", event.getPlayer().getName());
            obj.put("uuid", event.getPlayer().getUniqueId().toString());
            obj.put("server", event.getServer().getInfo().getName());
            
            publisher.publish(configuration.ChannelPrefix() + "connected", obj.toJSONString());
        } catch (Exceptions.InvalidChannelException ex) {
            Logger.getLogger(BCSCPlugin.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    @EventHandler
    public void onEvent(ServerDisconnectEvent event) {
        WriteConsole("ServerDisconnect", true);
    }
    
    @EventHandler
    public void onEvent(ServerSwitchEvent event) {
        WriteConsole("ServerSwitch", true);
        
        if (!previousServers.containsKey(event.getPlayer().getUniqueId())) {
            previousServers.put(event.getPlayer().getUniqueId(), event.getPlayer().getServer().getInfo().getName());
            return;
        }
        
        try {
            JSONObject obj = new JSONObject();
            obj.put("state", "switched");
            obj.put("username", event.getPlayer().getName());
            obj.put("uuid", event.getPlayer().getUniqueId().toString());
            obj.put("oldserver", previousServers.get(event.getPlayer().getUniqueId()));
            obj.put("newserver", event.getPlayer().getServer().getInfo().getName());
            
            previousServers.put(event.getPlayer().getUniqueId(), event.getPlayer().getServer().getInfo().getName());
            publisher.publish(configuration.ChannelPrefix() + "switched", obj.toJSONString());
        } catch (Exceptions.InvalidChannelException ex) {
            Logger.getLogger(BCSCPlugin.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}

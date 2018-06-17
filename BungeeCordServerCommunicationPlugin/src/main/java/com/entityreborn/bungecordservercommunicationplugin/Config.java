/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.entityreborn.bungecordservercommunicationplugin;

import java.util.List;
import net.md_5.bungee.config.Configuration;

/**
 *
 * @author LewsTherin
 */
public class Config {
    Configuration config;
    
    public enum DataType {
        NULLSEPARATED,
        JSON
    }
    
    public enum IPFilterType {
        WHITELIST,
        BLACKLIST,
        DISABLED
    }
    
    public Config(Configuration conf) {
        config = conf;
    }
    
    public String PublisherID() {
        return config.getString("publisher-id", "bungeecord");
    }
    
    public boolean IsListenMode() {
        return config.getBoolean("listen", true);
    }
    
    public String Endpoint() {
        return config.getString("endpoint", "tcp://*:5556");
    }
    
    public DataType DataType() {
        return DataType.valueOf(config.getString("data-type", DataType.NULLSEPARATED.toString()).toUpperCase());
    }
    
    public String ChannelPrefix() {
        return config.getString("channel-prefix", "bungeeplayer_");
    }
    
    public boolean IsDebugMode() {
        return config.getBoolean("debug-mode", false);
    }
    
    public boolean UseCertAuth() {
        return config.getBoolean("use-cert-auth", false);
    }
    
    public boolean VerboseAuth() {
        return config.getBoolean("verbose-auth", true);
    }
    
    public String CertPublicKey() {
        return config.getString("cert-public-key", "*!!G^&m%iDVXWGB>V7g$j$>9%C%JmXTefszSnXyU");
    }
    
    public String CertSecretKey() {
        return config.getString("cert-secret-key", "Om]m<Khew8d*5[bML4R5St$Gkmt2LN*UM4TTXKjL");
    }
    
    public IPFilterType IPFilterType() {
        return IPFilterType.valueOf(config.getString("ip-filter-type", IPFilterType.WHITELIST.toString()).toUpperCase());
    }
    
    public List<String> IPFilter() {
        return config.getStringList("ip-filter");
    }
}

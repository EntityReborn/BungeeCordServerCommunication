package com.entityreborn.bungecordservercommunicationplugin;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.zeromq.ZAuth;
import org.zeromq.ZCertStore;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Context;

public class Subscriber extends NodePoint implements Runnable {
    public static interface MessageCallback {
        public void process(String subscriber, String channel, String publisher, String message);
    }
    
    private final Set<MessageCallback> callbacks = Collections.synchronizedSet(new HashSet<MessageCallback>());
    private final String name;

    public Subscriber(String name) {
        owningThread = new Thread(this, "subscriber-" + name);
        this.name = name;
    }
    
    public void init(Context context) {
        super.init(context, ZMQ.SUB);
    }
    
    public void init(ZContext context) {
        super.init(context, ZMQ.SUB);
    }
    
    public void addCallback(MessageCallback toRun) {
        callbacks.add(toRun);
    }
    
    public void remCallback(MessageCallback toRun) {
        callbacks.remove(toRun);
    }
    
    private String sanitizeChannel(String channel) {
        String chan = channel.trim();
        
        if (!channel.equals("*")) {
            chan += '\0';
        } else {
            chan = "";
        }
        
        return chan;
    }
    
    public void subscribe(String channel) throws Exceptions.InvalidChannelException {
        if (!Util.isValidChannel(channel)) {
            throw new Exceptions.InvalidChannelException(channel);
        }
        
        String chan = sanitizeChannel(channel);
        socket.subscribe(chan.getBytes());
    }
    
    public void unsubscribe(String channel) throws Exceptions.InvalidChannelException {
        if (!Util.isValidChannel(channel)) {
            throw new Exceptions.InvalidChannelException(channel);
        }
        
        String chan = sanitizeChannel(channel);
        socket.unsubscribe(chan.getBytes());
    }

    public void run() {
        while (!Thread.currentThread().isInterrupted() && alive) {
            String recv;
            
            try {
                // UGLY HACK WARNING!
                // Unrecoverable exception from ZMQ, if we let recvStr
                // block and the thread gets terminated.
                String raw = socket.recvStr(ZMQ.DONTWAIT);
                
                if (raw == null) {
                    Thread.sleep(5);
                    continue;
                } else {
                    recv = raw.trim();
                }
            } catch (Exception e) {
                break;
            }
            
            if (recv.isEmpty()) {
                continue;
            }
            
            String channel;
            String identifier;
            String message;
            boolean processed = false;
            
            if (recv.contains("\0")) {
                String[] split = recv.split("\0", 3);
            
                if (split.length != 3) {
                    Logger.getLogger(Subscriber.class.getName()).log(Level.WARNING, 
                                "Malformed packet received. Skipping.");
                    continue;
                }
            
                channel = split[0];
                identifier = split[1];
                message = split[2];
                
                processed = true;
            } else {
                JSONParser parser = new JSONParser();
                Object obj;
                try {
                    obj = parser.parse(recv);
                } catch (ParseException ex) {
                    Logger.getLogger(Subscriber.class.getName()).log(Level.WARNING, 
                                "Malformed packet received. Skipping.");
                    continue;
                }
                JSONObject data = (JSONObject)obj;
                
                if (!data.containsKey("channel") || !data.containsKey("publisherid") || !data.containsKey("message")) {
                    Logger.getLogger(Subscriber.class.getName()).log(Level.WARNING, 
                                "Malformed packet received. JSON object missing channel, publisherid or message component. Skipping.");
                    continue;
                }
                
                channel = data.get("channel").toString();
                identifier = data.get("publisherid").toString();
                message = data.get("message").toString();
            }
            
            for (MessageCallback toRun : callbacks) {
                try {
                    // Let the callback figure out threading issues.
                    toRun.process(this.name, channel, identifier, message);
                } catch (Exception ex) {
                    Logger.getLogger(Subscriber.class.getName()).log(Level.SEVERE, 
                            "Error processing callback", ex);
                }
            }
        }
        
        cleanup();
    }
        
    public static void main (String[] args) throws InterruptedException, Exceptions.InvalidChannelException {
        ZContext context = new ZContext(1);
        
        ZAuth auth = new ZAuth(context, new ZCertStore.Hasher());
        auth.setVerbose(true);
        auth.configureCurve("C:\\temp\\certs");
        
        Subscriber sub = new Subscriber("testing");
        sub.init(context);
        
        sub.socket.setCurvePublicKey("*!!G^&m%iDVXWGB>V7g$j$>9%C%JmXTefszSnXyU".getBytes());
        sub.socket.setCurveSecretKey("Om]m<Khew8d*5[bML4R5St$Gkmt2LN*UM4TTXKjL".getBytes());
        sub.socket.setCurveServerKey("lN*r8I=:FuY@FZ&Mdn]HoOE!v@jx7kJ##Pf{S9>l".getBytes());
        
        sub.subscribe("*");
        sub.connect("tcp://svr-home:5556");
        
        sub.addCallback(new MessageCallback() {
            public void process(String subscriber, String channel, String publisher, String message) {
                String msg = "Received %s from %s on channel %s";
                System.out.println(String.format(msg, message, publisher, channel));
            }
        });
        
        sub.start();
        
        Thread.sleep(100000);
        
        System.out.println("Stopping");
        sub.stop();
        
        System.out.println("Terminating");
        auth.destroy();
        context.destroy();
    }
}
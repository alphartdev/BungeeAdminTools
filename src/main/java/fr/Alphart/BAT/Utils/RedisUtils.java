package fr.Alphart.BAT.Utils;

import java.util.UUID;

import com.imaginarycode.minecraft.redisbungee.RedisBungee;
import com.imaginarycode.minecraft.redisbungee.events.PubSubMessageEvent;

import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import fr.Alphart.BAT.BAT;
import fr.Alphart.BAT.Modules.Core.PermissionManager.Action;

public class RedisUtils implements Listener {
    
    private boolean redis = false;
    private static String channel = "BungeeAdminTools";
    private static String split = "######";

    public RedisUtils() {
	if (BAT.getInstance().getProxy().getPluginManager().getPlugin("RedisBungee") != null) {
	    BAT.getInstance().getLogger().info("Detected RedisBungee.  Enabling experimental RedisBungee support.  This currently only supports RedisBungee 0.3.3 or higher (but not 0.4).");
		BAT.getInstance().getProxy().getPluginManager()
			.registerListener(BAT.getInstance(), this);
	    	RedisBungee.getApi().registerPubSubChannels(channel);
	    	redis = true;
	} else {
	    redis = false;
	}
    }
    
    @EventHandler
    public void onPubSubMessage(final PubSubMessageEvent e) {
	if (!e.getChannel().equals(channel)) return;
	
	String[] message = e.getMessage().split(split);
	
	if (message[0].equalsIgnoreCase(RedisBungee.getApi().getServerId()) || message.length < 3) return;
	
	String messageType = message[1];
	
	switch (messageType) {
	case "gkick":
	    recieveGKickPlayer(message[2], message[3]);
	    break;
	case "message":
	    recieveMessagePlayer(message[2], message[3]);
	    break;
	case "broadcast":
	    recieveBroadcast(message[2], message[3]);
	    break;
	default:
	    BAT.getInstance().getLogger().warning("Undeclared BungeeAdminTool redis message recieved: " + messageType);
	    break;
	}
    }
    
    public Boolean isRedisEnabled() {
	return redis;
    }
    
    public void sendMessagePlayer(UUID pUUID, String message) {
	if (!redis) return;
	sendMessage("message", pUUID.toString() + split + message);
    }
    private void recieveMessagePlayer(String sUUID, String message) {
	ProxiedPlayer player = BAT.getInstance().getProxy().getPlayer(UUID.fromString(sUUID));
	if (player != null) {
	    player.sendMessage(TextComponent.fromLegacyText(message));
	}
    }
    
    public void sendGKickPlayer(UUID pUUID, String reason) {
	if (!redis) return;
	sendMessage("gkick", pUUID.toString() + split + reason);
    }
    private void recieveGKickPlayer(String sUUID, String reason) {
	ProxiedPlayer player = BAT.getInstance().getProxy().getPlayer(UUID.fromString(sUUID));
	if (player != null) {
	    BAT.kick(player, reason);
	}
    }
    
    public void sendBroadcast(Action action, String broadcast) {
	if (!redis) return;
	sendMessage("broadcast", action.getText() + split + broadcast);
    }
    private void recieveBroadcast(String action, String broadcast) {
	BAT.broadcast(broadcast, Action.fromText(action).getPermission());
    }
    
    
    void sendMessage(String messageType, String messageBody) {
	if (!redis) return;
	
        if (messageBody.trim().length() == 0) return;
        
        final String message = RedisBungee.getApi().getServerId() + split + messageType + split + messageBody;
        
        BAT.getInstance().getProxy().getScheduler().runAsync(BAT.getInstance(), new Runnable() {
            @Override
            public void run() {
                RedisBungee.getApi().sendChannelMessage(channel, message);
            }
        });
    }
	
    public void destroy() {
	if (!redis) return;
	RedisBungee.getApi().unregisterPubSubChannels("BungeeAdminTools");
	BAT.getInstance().getProxy().getPluginManager()
		.unregisterListener(this);
    }
}

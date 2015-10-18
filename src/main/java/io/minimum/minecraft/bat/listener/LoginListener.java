package io.minimum.minecraft.bat.listener;

import io.minimum.minecraft.bat.BungeeAdminTools;
import io.minimum.minecraft.bat.database.data.Ban;
import io.minimum.minecraft.bat.utils.UUIDUtils;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.event.LoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.util.List;

public class LoginListener implements Listener {
    @EventHandler
    public void onLogin(final LoginEvent event) {
        event.registerIntent(BungeeAdminTools.get());
        ProxyServer.getInstance().getScheduler().runAsync(BungeeAdminTools.get(), new Runnable() {
            @Override
            public void run() {
                // Verify the user has not been banned.
                List<Ban> banList = BungeeAdminTools.get().getBanHandler().getMostRecentBans(event.getConnection().getUniqueId());

                for (Ban ban : banList) {
                    if (ban.getServer() == null) {
                        // Globally banned.
                        event.setCancelled(true);
                        event.setCancelReason("You were banned globally from the network by " + ChatColor.GOLD + UUIDUtils.getName(ban.getCreator())
                                + ChatColor.RESET + ".\r\n" + "Reason: " + ChatColor.GOLD + ban.getReason() + ChatColor.RESET + "\r\n"
                                + "Expires " + (ban.getExpiresAt() == null ? ChatColor.RED + "never" : ban.getExpiresAt().toInstant().toString()));
                        event.completeIntent(BungeeAdminTools.get());
                        return;
                    }
                }

                // Update the user data.
                BungeeAdminTools.get().getPlayerHandler().updateData(event.getConnection());

                // Finished!
                event.completeIntent(BungeeAdminTools.get());
            }
        });
    }
}

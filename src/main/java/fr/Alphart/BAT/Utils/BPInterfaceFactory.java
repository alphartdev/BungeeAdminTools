package fr.Alphart.BAT.Utils;

import java.util.Collection;

import net.alpenblock.bungeeperms.BungeePerms;
import net.alpenblock.bungeeperms.platform.bungee.BungeePlugin;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.plugin.Plugin;

/**
 * Provide a clean way to support both version of BungeePerms
 */
public class BPInterfaceFactory{
    
    public static PermissionProvider getBPInterface(final Plugin bpPlugin){
        if(bpPlugin instanceof net.alpenblock.bungeeperms.BungeePerms){
            return new BungeePermsV2((net.alpenblock.bungeeperms.BungeePerms) bpPlugin);
        }else if(bpPlugin instanceof net.alpenblock.bungeeperms.platform.bungee.BungeePlugin){
            return new BungeePermsV3((net.alpenblock.bungeeperms.platform.bungee.BungeePlugin) bpPlugin);
        }
        throw new RuntimeException("BungeePerms version not supported !");
    }
    
    public interface PermissionProvider{
        public Collection<String> getPermissions(final CommandSender sender);
    }
    
    static class BungeePermsV3 implements PermissionProvider{
        private net.alpenblock.bungeeperms.platform.bungee.BungeePlugin bpPlugin;

        public BungeePermsV3(BungeePlugin bPlugin) {
            this.bpPlugin = bPlugin;
        }
        
        @Override
        public Collection<String> getPermissions(CommandSender sender) {
            return bpPlugin.getBungeeperms().getPermissionsManager().getUser(sender.getName()).getEffectivePerms();
        }
        
    }

    static class BungeePermsV2 implements PermissionProvider{
        private net.alpenblock.bungeeperms.BungeePerms bpPlugin;
        
        public BungeePermsV2(BungeePerms bPlugin) {
            this.bpPlugin = bPlugin;
        }

        @Override
        public Collection<String> getPermissions(CommandSender sender) {
            return bpPlugin.getPermissionsManager().getUser(sender.getName()).getEffectivePerms();
        }
        
    }
    
}
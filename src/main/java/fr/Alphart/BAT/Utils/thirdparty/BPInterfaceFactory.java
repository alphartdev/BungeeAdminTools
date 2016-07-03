package fr.Alphart.BAT.Utils.thirdparty;

import java.util.Collection;

import lombok.RequiredArgsConstructor;
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
    
    @RequiredArgsConstructor
    static class BungeePermsV3 implements PermissionProvider{
        private final net.alpenblock.bungeeperms.platform.bungee.BungeePlugin bpPlugin;
        
        @Override
        public Collection<String> getPermissions(CommandSender sender) {
            return bpPlugin.getBungeeperms().getPermissionsManager().getUser(sender.getName()).getEffectivePerms();
        }
        
    }
    
    @RequiredArgsConstructor
    static class BungeePermsV2 implements PermissionProvider{
        private final net.alpenblock.bungeeperms.BungeePerms bpPlugin;
        
        @Override
        public Collection<String> getPermissions(CommandSender sender) {
            return bpPlugin.getPermissionsManager().getUser(sender.getName()).getEffectivePerms();
        }
        
    }
    
}
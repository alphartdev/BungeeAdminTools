package io.minimum.minecraft.bat;

import com.google.gson.Gson;
import lombok.Getter;
import net.md_5.bungee.api.plugin.Plugin;

public class BungeeAdminTools extends Plugin {
    private static BungeeAdminTools plugin;
    @Getter
    private final Gson gson = new Gson();

    public static BungeeAdminTools get() {
        return plugin;
    }

    @Override
    public void onEnable() {
        plugin = this;
    }
}

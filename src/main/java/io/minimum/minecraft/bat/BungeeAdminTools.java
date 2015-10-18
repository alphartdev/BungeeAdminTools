package io.minimum.minecraft.bat;

import com.google.gson.Gson;
import io.minimum.minecraft.bat.database.daos.BanHandler;
import io.minimum.minecraft.bat.database.daos.PlayerHandler;
import io.minimum.minecraft.bat.database.daos.impl.JsonBanHandler;
import io.minimum.minecraft.bat.database.daos.impl.JsonPlayerHandler;
import lombok.Getter;
import net.md_5.bungee.api.plugin.Plugin;

public class BungeeAdminTools extends Plugin {
    private static BungeeAdminTools plugin;
    @Getter
    private final Gson gson = new Gson();
    @Getter
    private BanHandler banHandler;
    @Getter
    private PlayerHandler playerHandler;

    public static BungeeAdminTools get() {
        return plugin;
    }

    @Override
    public void onEnable() {
        plugin = this;
        banHandler = new JsonBanHandler();
        playerHandler = new JsonPlayerHandler();
    }
}

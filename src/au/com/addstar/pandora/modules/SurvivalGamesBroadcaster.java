package au.com.addstar.pandora.modules;

import au.com.addstar.pandora.AutoConfig;
import au.com.addstar.pandora.ConfigField;
import au.com.addstar.pandora.MasterPlugin;
import au.com.addstar.pandora.Module;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.mcsg.survivalgames.api.PlayerWinEvent;

import java.io.File;

public class SurvivalGamesBroadcaster implements Module, Listener {
    private MasterPlugin mPlugin;
    private Config mConfig;

    @Override
    public void onEnable() {
        if (mConfig.load())
            mConfig.save();
    }

    @Override
    public void onDisable() {
    }

    @Override
    public void setPandoraInstance(MasterPlugin plugin) {
        mConfig = new Config(new File(plugin.getDataFolder(), "SurvivalGamesBroadcaster.yml"));
        mPlugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onSurvivalGamesWin(PlayerWinEvent event) {
        mPlugin.sendChatControlMessage(Bukkit.getConsoleSender(), mConfig.channel, event.getMessage());
    }

    private class Config extends AutoConfig {
        public Config(File file) {
            super(file);
        }

        @ConfigField(comment = "The Chat Control channel to broadcast on.")
        public String channel = "GamesBCast";
    }
}

package au.com.addstar.pandora.modules;

import au.com.addstar.pandora.AutoConfig;
import au.com.addstar.pandora.ConfigField;
import au.com.addstar.pandora.MasterPlugin;
import au.com.addstar.pandora.Module;
import au.com.mineauz.minigames.events.MinigamesBroadcastEvent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.io.File;

public class MinigameBroadcaster implements Module, Listener {
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
        mConfig = new Config(new File(plugin.getDataFolder(), "MinigameBroadcast.yml"));
        mPlugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMinigameBroadcast(MinigamesBroadcastEvent event) {
        mPlugin.sendChatControlMessage(Bukkit.getConsoleSender(), mConfig.channel, event.getMessageWithPrefix());

        // Cancel event to prevent double broadcasts on local server
        event.setCancelled(true);
    }

    private class Config extends AutoConfig {
        public Config(File file) {
            super(file);
        }

        @ConfigField(comment = "The Chat Control channel to broadcast on.")
        public String channel = "GamesBCast";
    }
}

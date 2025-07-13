package au.com.addstar.pandora.modules;

import au.com.addstar.pandora.AutoConfig;
import au.com.addstar.pandora.ConfigField;
import au.com.addstar.pandora.MasterPlugin;
import au.com.addstar.pandora.Module;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.spigotmc.event.player.PlayerSpawnLocationEvent;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;

public class SpawnFixer implements Module, Listener {
    private MasterPlugin plugin;
    private Config config;

    @Override
    public void onEnable() {
        if (config.load())
            config.save();
    }

    @Override
    public void onDisable() {
    }

    @Override
    public void setPandoraInstance(MasterPlugin plugin) {
        this.plugin = plugin;
        this.config = new Config(new File(plugin.getDataFolder(), "SpawnFixer.yml"));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onSpawn(PlayerSpawnLocationEvent event) {
        Location spawn = event.getSpawnLocation();
        if (spawn == null)
            return;
        String worldName = spawn.getWorld().getName();
        if (config.worlds.contains(worldName)) {
            event.setSpawnLocation(spawn.getWorld().getSpawnLocation());
        }
    }

    private static class Config extends AutoConfig {
        public Config(File file) {
            super(file);
        }

        @ConfigField(name = "whitelisted-worlds", comment = "Worlds that should always spawn players at world spawn")
        public HashSet<String> worlds = new HashSet<>(Arrays.asList("hub"));
    }
}

package au.com.addstar.pandora.modules;

import au.com.addstar.pandora.AbstractModule;
import au.com.addstar.pandora.AutoConfig;
import au.com.addstar.pandora.ConfigField;
import au.com.addstar.pandora.MasterPlugin;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.world.DimensionType;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerJoinGame;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerRespawn;
import org.bukkit.entity.Player;
import java.io.File;

public class OverworldHijack extends AbstractModule implements PacketListener {

    private MasterPlugin plugin;
    private Config config;

    @Override
    public void onEnable() {
        if (config != null && config.load()) {
            config.save();
            debug = config.debug;
        }
        PacketEvents.get().getEventManager().registerListener(this);
    }

    @Override
    public void onDisable() {
        PacketEvents.get().getEventManager().unregisterListener(this);
        plugin = null;
        config = null;
    }

    @Override
    public void setPandoraInstance(MasterPlugin plugin) {
        this.plugin = plugin;
        this.config = new Config(new File(plugin.getDataFolder(), "OverworldHijack.yml"));
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (event.getPacketType() == PacketType.Play.Server.JOIN_GAME) {
            WrapperPlayServerJoinGame packet = new WrapperPlayServerJoinGame(event);
            handle(packet, event.getPlayer());
        } else if (event.getPacketType() == PacketType.Play.Server.RESPAWN) {
            WrapperPlayServerRespawn packet = new WrapperPlayServerRespawn(event);
            handle(packet, event.getPlayer());
        }
    }

    private void handle(WrapperPlayServerJoinGame packet, Player player) {
        DimensionType dim = packet.getDimensionType();
        if (dim != DimensionType.NETHER && dim != DimensionType.END) {
            debugLog("Spoofing join dimension for " + player.getName());
            packet.setDimensionType(DimensionType.OVERWORLD);
            packet.setDimension(DimensionType.OVERWORLD);
        }
    }

    private void handle(WrapperPlayServerRespawn packet, Player player) {
        DimensionType dim = packet.getDimensionType();
        if (dim != DimensionType.NETHER && dim != DimensionType.END) {
            debugLog("Spoofing respawn dimension for " + player.getName());
            packet.setDimensionType(DimensionType.OVERWORLD);
            packet.setDimension(DimensionType.OVERWORLD);
        }
    }

    private class Config extends AutoConfig {
        public Config(File file) { super(file); }

        @ConfigField(comment = "Enable debugging")
        public boolean debug = false;
    }
}

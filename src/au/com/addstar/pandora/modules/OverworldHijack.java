package au.com.addstar.pandora.modules;

import au.com.addstar.pandora.MasterPlugin;
import au.com.addstar.pandora.Module;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.world.DimensionType;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerJoinGame;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerRespawn;

public class OverworldHijack implements Module, PacketListener {

    @Override
    public void onEnable() {
        PacketEvents.get().getEventManager().registerListener(this);
    }

    @Override
    public void onDisable() {
        PacketEvents.get().getEventManager().unregisterListener(this);
    }

    @Override
    public void setPandoraInstance(MasterPlugin plugin) {
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (event.getPacketType() == PacketType.Play.Server.JOIN_GAME) {
            WrapperPlayServerJoinGame packet = new WrapperPlayServerJoinGame(event);
            handle(packet);
        } else if (event.getPacketType() == PacketType.Play.Server.RESPAWN) {
            WrapperPlayServerRespawn packet = new WrapperPlayServerRespawn(event);
            handle(packet);
        }
    }

    private void handle(WrapperPlayServerJoinGame packet) {
        DimensionType dim = packet.getDimensionType();
        if (dim != DimensionType.NETHER && dim != DimensionType.END) {
            packet.setDimensionType(DimensionType.OVERWORLD);
            packet.setDimension(DimensionType.OVERWORLD);
        }
    }

    private void handle(WrapperPlayServerRespawn packet) {
        DimensionType dim = packet.getDimensionType();
        if (dim != DimensionType.NETHER && dim != DimensionType.END) {
            packet.setDimensionType(DimensionType.OVERWORLD);
            packet.setDimension(DimensionType.OVERWORLD);
        }
    }
}

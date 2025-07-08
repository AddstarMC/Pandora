package au.com.addstar.pandora.modules;

import au.com.addstar.pandora.MasterPlugin;
import au.com.addstar.pandora.Module;
import dev.simplix.protocolize.api.Protocolize;
import dev.simplix.protocolize.api.event.PacketListener;
import dev.simplix.protocolize.api.event.PacketSendEvent;
import dev.simplix.protocolize.api.mapping.MappingData;
import dev.simplix.protocolize.data.packets.PlayServerJoinGame;
import dev.simplix.protocolize.data.packets.PlayServerRespawn;

public class OverworldHijack implements Module, PacketListener {

    @Override
    public void onEnable() {
        Protocolize.listenerProvider().registerListener(this);
    }

    @Override
    public void onDisable() {
        Protocolize.listenerProvider().unregisterListener(this);
    }

    @Override
    public void setPandoraInstance(MasterPlugin plugin) {
    }

    @Override
    public void packetSend(PacketSendEvent event) {
        Object packet = event.packet();
        if (packet instanceof PlayServerJoinGame) {
            handle((PlayServerJoinGame) packet);
        } else if (packet instanceof PlayServerRespawn) {
            handle((PlayServerRespawn) packet);
        }
    }

    private void handle(PlayServerJoinGame packet) {
        MappingData.Dimension dim = packet.getDimension();
        if (dim != MappingData.Dimension.NETHER && dim != MappingData.Dimension.END) {
            packet.setDimension(MappingData.Dimension.OVERWORLD);
            packet.setDimensionType(MappingData.Dimension.OVERWORLD);
        }
    }

    private void handle(PlayServerRespawn packet) {
        MappingData.Dimension dim = packet.getDimension();
        if (dim != MappingData.Dimension.NETHER && dim != MappingData.Dimension.END) {
            packet.setDimension(MappingData.Dimension.OVERWORLD);
            packet.setDimensionType(MappingData.Dimension.OVERWORLD);
        }
    }
}

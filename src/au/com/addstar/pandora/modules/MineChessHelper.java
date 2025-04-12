package au.com.addstar.pandora.modules;

import au.com.addstar.pandora.AutoConfig;
import au.com.addstar.pandora.ConfigField;
import au.com.addstar.pandora.MasterPlugin;
import au.com.addstar.pandora.Module;
import mcp.ajneb97.api.ArenaEndEvent;
import mcp.ajneb97.api.ArenaStartEvent;
import mcp.ajneb97.model.Arena;
import mcp.ajneb97.model.game.GameEndsReason;
import mcp.ajneb97.model.game.GamePlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.io.File;

public class MineChessHelper implements Module, Listener {
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
        mConfig = new Config(new File(plugin.getDataFolder(), "MineChess.yml"));
        mPlugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMineChessArenaStart(ArenaStartEvent event) {
        Arena arena = event.getArena();
        mPlugin.getLogger().info("[DEBUG] MineChess arena started: " + arena.getName());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMineChessArenaEnd(ArenaEndEvent event) {
        Arena arena = event.getArena();
        GamePlayer winner = event.getWinner(); // null if tie
        GamePlayer loser = event.getLoser(); // null if tie
        GameEndsReason endReason = arena.getEndReason();
        mPlugin.getLogger().info("[DEBUG] MineChess arena ended: " + arena.getName() + " with reason " + endReason
                + " and winner " + (winner == null ? "null" : winner.getPlayer().getName())
                + " and loser " + (loser == null ? "null" : loser.getPlayer().getName()));
        //mPlugin.sendChatControlMessage(Bukkit.getConsoleSender(), mConfig.channel, event.getMessageWithPrefix());
    }

    private class Config extends AutoConfig {
        public Config(File file) {
            super(file);
        }

        @ConfigField(comment = "The Chat Control channel to broadcast on.")
        public String channel = "GamesBCast";
    }
}

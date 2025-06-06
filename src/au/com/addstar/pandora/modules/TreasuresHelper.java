package au.com.addstar.pandora.modules;

import au.com.addstar.pandora.AutoConfig;
import au.com.addstar.pandora.ConfigField;
import au.com.addstar.pandora.MasterPlugin;
import au.com.addstar.pandora.Module;
import com.google.common.base.Strings;
import me.robifoxx.treasures.api.TreasuresChestOpenEvent;
import me.robifoxx.treasures.api.TreasuresChestOpenResult;
import me.robifoxx.treasures.api.TreasuresFindRewardEvent;
import me.robifoxx.treasures.api.TreasuresKeys;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.io.File;

public class TreasuresHelper implements Module, Listener {
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
        mConfig = new Config(new File(plugin.getDataFolder(), "TreasuresHelper.yml"));
        mPlugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTreasureOpen(TreasuresChestOpenEvent event) {
        // Ignore the event unless the event is successful (only successful events use up keys or give rewards)
        if (event.getResult() != TreasuresChestOpenResult.SUCCESS)
            return;

        Player p = event.getPlayer();
        PlayerInventory inv = p.getInventory();
        if (mConfig.debug) System.out.println("[DEBUG] TreasuresChestOpenEvent called for " + p.getName());

        // Check how many (normal) inventory slots are free (excluding armour)
        int free = 0;
        for (int x = 0; x < 35; x++) {
            ItemStack is = inv.getItem(x);
            if (is == null) {
                free++;
            }
        }
        if (mConfig.debug) System.out.println("[DEBUG] " + p.getName() + " has " + free + " inventory slots free.");

        // Check if the player has required number of free slots
        if (free < mConfig.min_free_slots) {
            if (mConfig.debug) System.out.println("[DEBUG] Cancelling event for " + p.getName());
            p.sendMessage(ChatColor.translateAlternateColorCodes('&', mConfig.not_enough_room_msg).replace("%slots%", String.valueOf(mConfig.min_free_slots)));
            event.setCancelled(true);
            event.getPlayer().closeInventory();
            TreasuresKeys.addKey(event.getPlayer().getUniqueId().toString(), event.getChestName(), 1);
        }
        if (mConfig.verboseChests)
            mPlugin.getLogger().info(event.getPlayer().getDisplayName() + " opened a  " + event.getChestName());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTreasureReward(TreasuresFindRewardEvent event) {
        if (mConfig.debug)
            System.out.println("[DEBUG] TreasuresFindRewardEvent called for " + event.getPlayer().getName());

        // Look for a message associated with the reward
        String msg = event.getBroadcastMessage();
        if (Strings.isNullOrEmpty(msg) || msg.equals("null")) return;

        if (mConfig.debug) System.out.println("[DEBUG] " + msg);

        // Broadcast the message across other servers
        mPlugin.sendChatControlMessage(Bukkit.getConsoleSender(), mConfig.broadcast_channel, msg);
        if (mConfig.verboseChests)
            mPlugin.getLogger().info(event.getPlayer().getDisplayName() + " received a  " + event.getRewardName() + "( " + event.getRarity() + ")");
    }

    private class Config extends AutoConfig {
        public Config(File file) {
            super(file);
        }

        @ConfigField(comment = "Enable debug messages")
        public boolean debug = false;
        @ConfigField(comment = "Verbose reporting of chest opens")
        public boolean verboseChests = false;
        @ConfigField(comment = "The Chat Control channel to broadcast on.")
        public String broadcast_channel = "GamesBCast";

        @ConfigField(comment = "The minimum number of slots required to be free to use a key")
        public int min_free_slots = 3;

        @ConfigField(comment = "The message players see when they don't have enough slots free")
        public String not_enough_room_msg = "&cSorry, you must have %slots% inventory slots free to use this.";
    }
}
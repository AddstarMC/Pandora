package au.com.addstar.pandora.modules;

import au.com.addstar.pandora.MasterPlugin;
import au.com.addstar.pandora.Module;
import com.destroystokyo.paper.MaterialTags;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.Event.Result;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class AntiSpawnerChange implements Module, Listener {
    @Override
    public void onEnable() {
    }

    @Override
    public void onDisable() {
    }

    @Override
    public void setPandoraInstance(MasterPlugin plugin) {
    }

    @EventHandler(ignoreCancelled = true)
    private void onSpawnerChange(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.hasItem()) {
            Block block = event.getClickedBlock();
            ItemStack item = event.getItem();
            if (block == null || item == null) return;

            if (block.getType() == Material.SPAWNER && MaterialTags.SPAWN_EGGS.isTagged(item.getType())) {
                // Allow the change if the spawn egg item is enchanted with unbreaking 10
                if (item.getEnchantments().containsKey(org.bukkit.enchantments.Enchantment.UNBREAKING)
                        && item.getEnchantments().get(org.bukkit.enchantments.Enchantment.UNBREAKING) == 10) {
                    return;
                }

                // Deny the change if the player does not have the permission
                if (!event.getPlayer().hasPermission("pandora.spawner.change")) {
                    event.getPlayer().sendMessage(ChatColor.RED + "You do not have permission to change spawners.");
                    event.setUseInteractedBlock(Result.DENY);
                }
            }
        }
    }
}

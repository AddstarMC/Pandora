package au.com.addstar.pandora.modules;

import au.com.addstar.pandora.AutoConfig;
import au.com.addstar.pandora.ConfigField;
import au.com.addstar.pandora.MasterPlugin;
import au.com.addstar.pandora.Module;
import io.github.thebusybiscuit.slimefun4.api.events.AutoDisenchantEvent;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.Map;

public class SlimefunTweaks implements Module, Listener {

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
        mConfig = new Config(new File(plugin.getDataFolder(), "SlimefunTweaks.yml"));
        mPlugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void disenchantEvent(AutoDisenchantEvent event) {
        ItemStack stack = event.getItem();
        if (stack == null) return;
        String name = stack.getItemMeta().getDisplayName() != null ? stack.getItemMeta().getDisplayName() : "";

        for (Map.Entry<Enchantment, Integer> entry : stack.getEnchantments().entrySet()) {
            int level = entry.getValue();
            Enchantment enchant = entry.getKey();

            if (mConfig.allowSlimefunEnchants) {
                // Allow disenchanting of Reinforced Armour
                if (
                        ((enchant == Enchantment.PROTECTION) && (level == 9)) ||
                        ((enchant == Enchantment.PROTECTION) && (level == 9))) {
                    break;
                }
            }

            // If the max level for this enchant is too high, cancel the event
            if (level > mConfig.maxDisenchantLevel) {
                event.setCancelled(true);
                if (mConfig.debug)
                    mPlugin.getLogger().warning("Slimefun disenchanting denied: "
                        + stack.getType() + " (\"" + name + "\":" + enchant + ":" + level + ")");
                break;
            }
        }
        // Event wasn't cancelled so all enchants are ok
        if (mConfig.debug) {
            if (!event.isCancelled()) {
                mPlugin.getLogger().info("Slimefun disenchanting allowed: " + stack.getType() + " (\"" + name + "\")");
            }
        }
    }

    private class Config extends AutoConfig {
        public Config(File file) {
            super(file);
        }

        @ConfigField(comment = "Enable debugging for troubleshooting - Note this can be VERY spammy")
        public boolean debug = false;

        @ConfigField(comment = "Max allowed enchant level for the Slimefun Disenchanter")
        public int maxDisenchantLevel = 6;

        @ConfigField(comment = "Allow specific enchants found on Slimefun items (Prot IX/Dura IX)")
        public boolean allowSlimefunEnchants = true;
    }
}
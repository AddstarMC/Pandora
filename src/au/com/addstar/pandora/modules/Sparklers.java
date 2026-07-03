package au.com.addstar.pandora.modules;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitTask;

import com.google.common.base.Functions;

import au.com.addstar.pandora.MasterPlugin;
import au.com.addstar.pandora.Module;

public class Sparklers implements Module, Listener, CommandExecutor {
    private static final String mItemName = ChatColor.translateAlternateColorCodes('&', "&e\u274A &fSparkler &e\u274A");
    private static final String mUsedItemName = ChatColor.translateAlternateColorCodes('&', "&e\u274A &cSparkler &e\u274A");

    private static final long mSparklerLength = 30000;

    public enum SparklerEffect {
        Fire,
        Smoke,
        Colour,
        Ender,
        Purple,
        Emerald
    }

    public static class Sparkler {
        public Sparkler(SparklerEffect effect) {
            this.effect = effect;
            endTime = System.currentTimeMillis() + mSparklerLength;
        }

        public SparklerEffect effect;
        public long endTime;
    }

    private MasterPlugin mPlugin;

    private HashMap<Player, Sparkler> mActive;

    private BukkitTask mTask;

    public Sparklers() {
        mActive = new HashMap<>();
    }

    @Override
    public void onEnable() {
        mPlugin.getCommand("sparkler").setExecutor(this);

        mTask = Bukkit.getScheduler().runTaskTimer(mPlugin, this::doSparklers, 2, 2);
    }

    @Override
    public void onDisable() {
        mPlugin.getCommand("sparkler").setExecutor(null);

        mTask.cancel();
    }

    @Override
    public void setPandoraInstance(MasterPlugin plugin) {
        mPlugin = plugin;
    }

    public void doSparklers() {
        Iterator<Entry<Player, Sparkler>> it = mActive.entrySet().iterator();

        Location temp = new Location(null, 0, 0, 0);

        while (it.hasNext()) {
            Entry<Player, Sparkler> entry = it.next();
            Player player = entry.getKey();

            Sparkler sparkler = entry.getValue();
            if (System.currentTimeMillis() >= sparkler.endTime) {
                ItemStack held = player.getInventory().getItemInMainHand();
                if (isSparkler(held)) {
                    if (held.getAmount() > 1) {
                        ItemStack item = makeSparkler(sparkler.effect, held.getAmount() - 1);
                        player.getInventory().setItemInMainHand(item);
                    } else
                        player.getInventory().setItemInMainHand(null);
                }
                it.remove();
            }

            World world = player.getWorld();

            // Get the front location
            player.getLocation(temp);
            temp.add(0, player.getEyeHeight(true) - 0.1, 0);
            temp.add(temp.getDirection().multiply(0.3));

            switch (sparkler.effect) {
                case Colour:
                    world.spawnParticle(Particle.DUST, temp, 3, 0.15, 0.4, 0.15, 3);
                    break;
                case Fire:
                    world.spawnParticle(Particle.FLAME, temp, 3, 0.15, 0.4, 0.15, 0.03);
                    break;
                case Smoke:
                    world.spawnParticle(Particle.FIREWORK, temp, 3, 0.15, 0.4, 0.15, 0.03);
                    break;
                case Ender:
                    world.spawnParticle(Particle.PORTAL, temp, 6, 0.15, 0.4, 0.15, 0.2);
                    break;
                case Purple:
                    world.spawnParticle(Particle.WITCH, temp, 3, 0.15, 0.4, 0.15, 0.0);
                    break;
                case Emerald:
                    world.spawnParticle(Particle.HAPPY_VILLAGER, temp, 3, 0.15, 0.4, 0.15, 0.6);
                    break;
            }

            if ((System.currentTimeMillis() / 60) % 2 == 0)
                player.getWorld().playSound(temp, Sound.BLOCK_FIRE_EXTINGUISH, 0.4f, 1f);
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 0 || args.length > 3)
            return false;

        SparklerEffect effect = SparklerEffect.Fire;
        int count = 1;

        if (args.length >= 2) {
            effect = null;
            for (SparklerEffect e : SparklerEffect.values()) {
                if (e.name().equalsIgnoreCase(args[1])) {
                    effect = e;
                    break;
                }
            }

            if (effect == null) {
                sender.sendMessage(ChatColor.RED + "Invalid effect. Valid effects are: " + StringUtils.join(Arrays.stream(SparklerEffect.values()).map(Functions.toStringFunction()::apply).collect(Collectors.toList()), ' '));
                return true;
            }

            if (args.length == 3) {
                try {
                    count = Integer.parseInt(args[2]);
                    if (count <= 0) {
                        sender.sendMessage(ChatColor.RED + "Invalid stack size. Must be 1 or more");
                        return true;
                    }
                } catch (NumberFormatException e) {
                    sender.sendMessage(ChatColor.RED + "Invalid stack size. Must be 1 or more");
                    return true;
                }
            }
        }

        if (args[0].equalsIgnoreCase("all")) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getInventory().addItem(makeSparkler(effect, count)).isEmpty())
                    player.sendMessage(ChatColor.GOLD + "You have been given a sparkler");
                else
                    player.sendMessage(ChatColor.RED + "You would have been given a sparkler but your inventory was full");
            }

            Bukkit.broadcastMessage(ChatColor.GOLD + "Everyone has been given a sparkler by " + ChatColor.RED + sender.getName());
        } else {
            Player player = Bukkit.getPlayer(args[0]);
            if (player == null)
                sender.sendMessage(ChatColor.RED + "That player does not exist. Use 'ALL' to give to all");
            else {
                if (player.getInventory().addItem(makeSparkler(effect, count)).isEmpty()) {
                    sender.sendMessage(ChatColor.GOLD + "You gave " + player.getDisplayName() + " a sparkler");
                    player.sendMessage(ChatColor.GOLD + "You have been given a sparkler by " + ChatColor.RED + sender.getName());
                } else
                    sender.sendMessage(ChatColor.RED + "That players inventory was full.");
            }
        }
        return true;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onRightClick(PlayerInteractEvent event) {
        if (!event.hasItem() || event.getAction() == Action.PHYSICAL)
            return;

        ItemStack sparkler = event.getItem();
        if (!isSparkler(sparkler))
            return;

        event.setCancelled(true);

        if (event.getAction() != Action.RIGHT_CLICK_AIR || isUsed(sparkler))
            return;

        SparklerEffect effect = getEffect(sparkler);

        setUsed(sparkler);

        mActive.put(event.getPlayer(), new Sparkler(effect));
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onChangeHeld(PlayerItemHeldEvent event) {
        ItemStack item = event.getPlayer().getInventory().getItem(event.getPreviousSlot());

        if (isSparkler(item) && isUsed(item)) {
            if (mActive.containsKey(event.getPlayer()))
                event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player))
            return;

        Player player = (Player) event.getWhoClicked();

        Sparkler sparkler = mActive.get(player);
        if (sparkler != null)
            event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onItemThrow(PlayerDropItemEvent event) {
        Player player = event.getPlayer();

        Sparkler sparkler = mActive.get(player);
        if (sparkler != null)
            event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerLeave(PlayerQuitEvent event) {
        mActive.remove(event.getPlayer());
    }

    public boolean isSparkler(ItemStack item) {
        if (item == null || item.getType() != Material.LEVER)
            return false;

        if (!item.hasItemMeta())
            return false;

        ItemMeta meta = item.getItemMeta();
        if (!meta.hasDisplayName())
            return false;

        return meta.getDisplayName().startsWith(mItemName) || meta.getDisplayName().equals(mUsedItemName);
    }

    public SparklerEffect getEffect(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        String name = meta.getDisplayName().substring(mItemName.length());
        name = ChatColor.stripColor(name);
        name = name.substring(3);

        for (SparklerEffect e : SparklerEffect.values()) {
            if (e.name().equalsIgnoreCase(name))
                return e;
        }

        return null;
    }

    public void setUsed(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        meta.setLore(Collections.emptyList());

        meta.setDisplayName(mUsedItemName);
        item.setItemMeta(meta);
    }

    public boolean isUsed(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (!meta.hasDisplayName())
            return false;

        return meta.getDisplayName().equals(mUsedItemName);
    }

    public ItemStack makeSparkler(SparklerEffect effect, int count) {
        ItemStack item = new ItemStack(Material.LEVER, count);

        item.addUnsafeEnchantment(Enchantment.FLAME, 1);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(mItemName + ChatColor.GRAY + " - " + ChatColor.RED + effect.name());
        meta.setLore(Collections.singletonList(ChatColor.GRAY + "Right click this to create a shower of sparks"));
        item.setItemMeta(meta);

        return item;
    }
}

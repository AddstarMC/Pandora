package au.com.addstar.pandora.modules;

import au.com.addstar.pandora.AutoConfig;
import au.com.addstar.pandora.ConfigField;
import au.com.addstar.pandora.MasterPlugin;
import au.com.addstar.pandora.Module;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.protection.events.DisallowedPVPEvent;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.AreaEffectCloudApplyEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class PVPHandler implements Module, Listener {
    private HashSet<PotionEffectType> mBadEffects = new HashSet<>();
    private RegionContainer worldGaurdRegions;

    private Config mConfig;

    private int mLavaRange;
    private int mFireRange;
    private int mSearchRadius;

    public PVPHandler() {
        mBadEffects.add(PotionEffectType.BLINDNESS);
        mBadEffects.add(PotionEffectType.NAUSEA);
        mBadEffects.add(PotionEffectType.INSTANT_DAMAGE);
        mBadEffects.add(PotionEffectType.HUNGER);
        mBadEffects.add(PotionEffectType.POISON);
        mBadEffects.add(PotionEffectType.SLOWNESS);
        mBadEffects.add(PotionEffectType.MINING_FATIGUE);
        mBadEffects.add(PotionEffectType.WEAKNESS);
        mBadEffects.add(PotionEffectType.WITHER);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    private void onSplashPotion(PotionSplashEvent event) {
        if (!(event.getPotion().getShooter() instanceof Player) || ((Player) event.getPotion().getShooter()).hasMetadata("NPC"))
            return;

        boolean bad = false;
        for (PotionEffect effect : event.getPotion().getEffects()) {
            if (mBadEffects.contains(effect.getType()))
                bad = true;
        }

        if (!bad)
            return;

        Player thrower = (Player) event.getPotion().getShooter();

        if (thrower.hasPermission("pandora.pvphandler.bypass"))
            return;

        boolean warned = false;
        RegionManager manager =
                worldGaurdRegions.get(BukkitAdapter.adapt(event.getPotion().getWorld()));
        if (manager == null)
            return;

        for (LivingEntity ent : event.getAffectedEntities()) {
            if (!(ent instanceof Player) || ent.hasMetadata("NPC"))
                continue;

            if (ent.equals(thrower))
                continue;

            if (ent.hasPermission("pandora.pvphandler.ignore"))
                continue;

            ApplicableRegionSet regions =
                    manager.getApplicableRegions(BukkitAdapter.asBlockVector(ent.getLocation()));
            if (!regions.testState(null, Flags.PVP)) {
                if (!warned) {
                    thrower.sendMessage(ChatColor.RED + "PVP is not allowed in this area");
                    warned = true;
                }
                event.setIntensity(ent, 0D);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    private void onLingerPotion(AreaEffectCloudApplyEvent event) {
        if (!(event.getEntity().getSource() instanceof Player) || ((Player) event.getEntity().getSource()).hasMetadata("NPC")) {
            return;
        }
        boolean bad = false;
        for (PotionEffect effect : event.getEntity().getCustomEffects()) {
            if (mBadEffects.contains(effect.getType()))
                bad = true;
        }
        if (!bad) return;
        Player thrower = (Player) event.getEntity().getSource();
        if (thrower.hasPermission("pandora.pvphandler.bypass"))
            return;

        boolean warned = false;

        RegionManager manager =
                worldGaurdRegions.get(BukkitAdapter.adapt((event.getEntity().getWorld())));
        if (manager == null)
            return;
        List<LivingEntity> removals = new ArrayList<>();
        for (LivingEntity ent : event.getAffectedEntities()) {
            if (!(ent instanceof Player) || ent.hasMetadata("NPC"))
                continue;

            if (ent.equals(thrower))
                continue;

            if (ent.hasPermission("pandora.pvphandler.ignore"))
                continue;

            ApplicableRegionSet regions =
                    manager.getApplicableRegions(BukkitAdapter.asBlockVector(ent.getLocation()));
            if (!regions.testState(null, Flags.PVP)) {
                if (!warned) {
                    thrower.sendMessage(ChatColor.RED + "PVP is not allowed in this area");
                    warned = true;
                }
                removals.add(ent);
            }
        }
        for (LivingEntity ent : removals) {
            event.getAffectedEntities().remove(ent);
        }
    }


    private boolean handleBlockPlace(Block block, Player player, Material placeMaterial) {
        RegionManager manager = worldGaurdRegions.get(BukkitAdapter.adapt(block.getWorld()));
        if (manager == null)
            return false;

        if (player.hasPermission("pandora.pvphandler.bypass"))
            return false;

        Location playerLoc = new Location(null, 0, 0, 0);
        Location blockLoc = block.getLocation();

        ApplicableRegionSet regions =
                manager.getApplicableRegions(BukkitAdapter.asBlockVector(blockLoc));
        if (regions.testState(null, Flags.PVP))
            return false;

        List<Entity> entities = player.getNearbyEntities(mSearchRadius, mSearchRadius, mSearchRadius);

        int dist = mLavaRange;
        if (placeMaterial == Material.FIRE)
            dist = mFireRange;

        for (Entity entity : entities) {
            if (!(entity instanceof Player) || entity.hasMetadata("NPC"))
                continue;

            if (entity.hasPermission("pandora.pvphandler.ignore"))
                continue;

            entity.getLocation(playerLoc);
            if (playerLoc.distanceSquared(blockLoc) < dist) {
                player.sendMessage(ChatColor.RED + "PVP is not allowed in this area!");
                player.sendMessage(ChatColor.GRAY + "You placed " + placeMaterial.toString() + " too close to another player.");
                return true;
            }
        }

        return false;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    private void onPlaceFire(BlockPlaceEvent event) {
        if (event.getBlock().getType() != Material.FIRE && event.getBlock().getType() != Material.LAVA)
            return;

        event.setCancelled(handleBlockPlace(event.getBlock(), event.getPlayer(), event.getBlock().getType()));
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    private void onPlaceLava(PlayerBucketEmptyEvent event) {
        if (event.getBucket() != Material.LAVA_BUCKET)
            return;

        event.setCancelled(handleBlockPlace(event.getBlockClicked(), event.getPlayer(), Material.LAVA));
    }

    @EventHandler
    private void onPVPCheck(DisallowedPVPEvent event) {
        if (event.getDefender().hasMetadata("NPC") || event.getAttacker().hasMetadata("NPC"))
            event.setCancelled(true);
    }

    @Override
    public void onEnable() {
        worldGaurdRegions = WorldGuard.getInstance().getPlatform().getRegionContainer();
        if (mConfig.load())
            mConfig.save();
        else
            throw new RuntimeException("The config failed to load");

        mLavaRange = mConfig.lavaRadius * mConfig.lavaRadius;
        mFireRange = mConfig.fireRadius * mConfig.fireRadius;

        mSearchRadius = Math.max(mConfig.fireRadius, mConfig.lavaRadius);
    }

    @Override
    public void onDisable() {
    }

    @Override
    public void setPandoraInstance(MasterPlugin plugin) {
        mConfig = new Config(new File(plugin.getDataFolder(), "PVPHandler.yml"));
    }

    private class Config extends AutoConfig {
        public Config(File file) {
            super(file);
        }

        @ConfigField
        public int lavaRadius = 10;
        @ConfigField
        public int fireRadius = 5;
    }
}

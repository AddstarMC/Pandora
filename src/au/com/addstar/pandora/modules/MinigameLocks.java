package au.com.addstar.pandora.modules;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import net.md_5.bungee.api.ChatColor;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import au.com.addstar.pandora.MasterPlugin;
import au.com.addstar.pandora.Module;
import au.com.mineauz.minigames.MinigamePlayer;
import au.com.mineauz.minigames.Minigames;
import au.com.mineauz.minigames.events.EndMinigameEvent;
import au.com.mineauz.minigames.events.QuitMinigameEvent;
import au.com.mineauz.minigames.events.StartMinigameEvent;
import au.com.mineauz.minigames.minigame.Minigame;

public class MinigameLocks implements Module, Listener, CommandExecutor
{
	private boolean Debug = false;
	private MasterPlugin mPlugin;
	private File mFile;
	private FileConfiguration mConfig;
	private HashMap<Minigame, HashMap<Material, Boolean>> Lockables = new HashMap<Minigame, HashMap<Material, Boolean>>();
	private HashMap<Location, Lockable> Locks = new HashMap<Location, Lockable>();
	private ArrayList<Material> LockableMaterials = new ArrayList<Material>();
	private HashMap<Minigame, Boolean> DisabledMinigames = new HashMap<Minigame, Boolean>();

	@Override
	public void onEnable() {
		// Delay start up so Minigames are all loaded before we try to validate stuff
		System.out.println("Delaying MinigameLocks initialisation...");
		Bukkit.getScheduler().runTaskLater(mPlugin, new Runnable() {
            @Override
            public void run() {
            	// Load and validate config
				loadConfig();
            }
        }, 40);
	}

	@Override
	public void onDisable() {
		mConfig = null;
		mFile = null;
		Lockables.clear();
		Locks.clear();
		LockableMaterials.clear();
		DisabledMinigames.clear();
	}

	@Override
	public void setPandoraInstance(MasterPlugin plugin) {
		mPlugin = plugin;
		plugin.getCommand("mgl").setExecutor(this);
	}

	@Override
	public boolean onCommand( CommandSender sender, Command command, String label, String[] args ) {
		if (args.length == 0)
			return false;

		if (args[0].equalsIgnoreCase("disable") && args.length == 2) {
			if (sender.hasPermission("pandora.minigamelocks.admin")) {
				Minigame mg = Minigames.plugin.mdata.getMinigame(args[1]);
				if (mg != null) {
					DisableLocks(mg);
					sender.sendMessage(ChatColor.RED + "MinigameLocks have been cleared and disabled for: " + mg.getName(true));
				}
			} else {
				sender.sendMessage(ChatColor.RED + "You do not have permission for this.");
			}
			return true;
		}
		else if (args[0].equalsIgnoreCase("enable") && args.length == 2) {
			if (sender.hasPermission("pandora.minigamelocks.admin")) {
				Minigame mg = Minigames.plugin.mdata.getMinigame(args[1]);
				if (mg != null) {
					EnableLocks(mg);
					sender.sendMessage(ChatColor.GREEN + "MinigameLocks have been enabled for: " + mg.getName(true));
				}
			} else {
				sender.sendMessage(ChatColor.RED + "You do not have permission for this.");
			}
			return true;
		}
		return false;
	}

	public class Lockable {
		Material material;
		UUID owner;
		String ownername;
		Minigame minigame;
		Location location;

		public Lockable(Location l, Material m, UUID u, Minigame mg) {
			location = l;
			material = m;
			owner = u;
			minigame = mg;
		}
	}

	private boolean loadConfig() {
		try
		{
			mFile = new File(mPlugin.getDataFolder(), "MinigameLocks.yml");
			if (!mFile.exists())
				mPlugin.saveResource("MinigameLocks.yml", false);

			mConfig = YamlConfiguration.loadConfiguration(mFile);
			if (mFile.exists())
				mConfig.load(mFile);

			Debug = mConfig.getBoolean("debug", false);

			// Reset all the data (in case we're reloading)
			LockableMaterials.clear();
			Lockables.clear();
			Locks.clear();
			DisabledMinigames.clear();

			if ((mConfig != null) && (mConfig.isConfigurationSection("minigames"))) {
				Set<String> minigames = (Set<String>) mConfig.getConfigurationSection("minigames").getKeys(false);
				System.out.println("Loading MinigameLocks config...");
				for (String mgname : minigames) {
					// Grab all the settings for this minigame
					Minigame mg = Minigames.plugin.mdata.getMinigame(mgname);
					if (mg != null) {
						HashMap<Material, Boolean> types = new HashMap<Material, Boolean>();
						ConfigurationSection mgsection = mConfig.getConfigurationSection("minigames." + mgname);

						for (String locktype : mgsection.getKeys(false)) {
							Material mat = Material.getMaterial(locktype);
							if (mat == null) {
								System.out.println("WARNING: Unknown Material \"" + locktype + "\"!");
								continue;
							}
							Boolean autolock = ( mgsection.getString(locktype).toUpperCase().equals("AUTOLOCK") ? true : false);
							types.put(mat, autolock);

							// Keep a list of all lockable materials configured (for performance)
							if (!LockableMaterials.contains(mat)) {
								LockableMaterials.add(mat);
							}
						}
						Lockables.put(mg, types);
					} else {
						System.out.println("WARNING: Unknown Minigame \"" + mgname + "\"!");
					}
				}

				if (Debug) {
					System.out.println("==============================================");
					for (Map.Entry<Minigame, HashMap<Material, Boolean>> entry : Lockables.entrySet()) {
						Minigame m = entry.getKey();
						System.out.println("Minigame: " + m.getName(false) + " (" + m.getName(true) + ")");
						for (Map.Entry<Material, Boolean> opt : entry.getValue().entrySet()) {
							System.out.println("  " + opt.getKey() + ": " + opt.getValue());
						}
					}
					System.out.println("==============================================");
				}
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
			return false;
		}
		return true;
	}

	public Lockable GetLockable(Location loc) {
		// TODO: logic for double chests (normal and trapped)
		Location newloc = new Location(loc.getWorld(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
		return Locks.get(newloc);
	}

	public boolean HasBlockAccess(Lockable lock, Player player) {
		if (lock != null) {
			if (lock.owner.equals(player.getUniqueId())) {
				// Allow owner to access chest
				return true;
			} else {
				return false;
			}
		} else {
			return true;
		}
	}

	public boolean CanLock(Minigame mg, Material mat) {
		HashMap<Material, Boolean> types = Lockables.get(mg);
		if (types != null) {
			if (types.get(mat)) {
				return true;
			}
		}
		return false;
	}

	public String NiceItemName(Material mat) {
		return mat.name().toLowerCase().replace("_", " ");
	}

	public boolean AddLock(Minigame mg, Block b, Player p) {
		Location newloc = new Location(
				b.getLocation().getWorld(),
				b.getLocation().getBlockX(),
				b.getLocation().getBlockY(),
				b.getLocation().getBlockZ());
		Lockable lock = new Lockable(newloc, b.getType(), p.getUniqueId(), mg);
		lock.ownername = (p.getCustomName() != null) ? p.getCustomName() : p.getName();
		Locks.put(newloc, lock);
		p.sendMessage(ChatColor.GREEN + "You have created a locked " + NiceItemName(b.getType()));
		return true;
	}

	// Clear all the locks for a given minigame
	public void ClearMinigameLocks(Minigame mg) {
		for(Iterator<Map.Entry<Location, Lockable>> it = Locks.entrySet().iterator(); it.hasNext(); ) {
			Map.Entry<Location, Lockable> entry = it.next();
			if (entry.getValue().minigame.equals(mg)) {
				// Remove the entry if part of the specified Minigame
				it.remove();
			}
		}
	}

	// Clear all the locks for a given player
	public void ClearPlayerLocks(MinigamePlayer mgp) {
		if (mgp != null) {
			for(Iterator<Map.Entry<Location, Lockable>> it = Locks.entrySet().iterator(); it.hasNext(); ) {
				Map.Entry<Location, Lockable> entry = it.next();
				if (entry.getValue().owner.equals(mgp.getUUID())) {
					// Remove the entry if owned by the MinigamePlayer
					it.remove();
				}
			}
		}
	}

	public void DisableLocks(Minigame mg) {
		DisabledMinigames.put(mg, true);
		ClearMinigameLocks(mg);
	}

	public void EnableLocks(Minigame mg) {
		DisabledMinigames.remove(mg);
	}

	// Clear locks when a game finishes
	@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
	public void onMinigameEnd(EndMinigameEvent event) {
		Minigame mg = event.getMinigame();
		ClearMinigameLocks(mg);
		EnableLocks(mg);
	}

	// Clear locks when a game finishes
	@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
	public void onMinigameStart(StartMinigameEvent event) {
		Minigame mg = event.getMinigame();
		ClearMinigameLocks(mg);
		EnableLocks(mg);
	}

	// Lock an item when the player places it
	@EventHandler(ignoreCancelled=true)
	public void onBlockPlace(BlockPlaceEvent event) {
		// Exit early if this block can't be locked
		if (!LockableMaterials.contains(event.getBlock().getType())) return; 

		MinigamePlayer mgp = Minigames.plugin.pdata.getMinigamePlayer(event.getPlayer());
		if ((mgp != null) && (mgp.isInMinigame())) {
			Minigame mg = mgp.getMinigame();
			if (DisabledMinigames.containsKey(mg)) return;	// ignore locks if game is disabled

			if (CanLock(mg, event.getBlock().getType())) {
				AddLock(mg, event.getBlock(), event.getPlayer());
			}
		}
	}

	// Don't allow breaking locked things
	@EventHandler(ignoreCancelled=true)
	public void onBlockBreak(BlockBreakEvent event) {
		// Early exit if there are no locks at all or this isnt a lockable type
		if (Locks.isEmpty()) return;
		if (!LockableMaterials.contains(event.getBlock().getType())) return; 

		Lockable lock = GetLockable(event.getBlock().getLocation());
		if (lock == null) return;									// block is not locked 
		if (DisabledMinigames.containsKey(lock.minigame)) return;	// ignore locks if game is disabled

		if (!HasBlockAccess(lock, event.getPlayer())) {
			event.getPlayer().sendMessage(ChatColor.RED  + "That is locked by " + lock.ownername);
			event.setCancelled(true);
		}
	}

	// Don't allow players to interact with locked things they don't own
	@EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
	public void onPlayerInteract(PlayerInteractEvent event) {
		// Right clicking crafting table (create personal workbench for each player)
		Block b = event.getClickedBlock();
		if ((event.getAction() == Action.RIGHT_CLICK_BLOCK) && (b != null) && (b.getType() == Material.WORKBENCH)) {
			Player p = event.getPlayer();
			p.openWorkbench(p.getLocation(), true);
			event.setCancelled(true);
			return;
		}

		// Early exit if there are no locks at all or this isnt a lockable type
		if (Locks.isEmpty()) return;
		if (!LockableMaterials.contains(event.getClickedBlock().getType())) return; 

		Lockable lock = GetLockable(event.getClickedBlock().getLocation());
		if (lock == null) return;									// block is not locked 
		if (DisabledMinigames.containsKey(lock.minigame)) return;	// ignore locks if game is disabled

		if (!HasBlockAccess(lock, event.getPlayer())) {
			event.getPlayer().sendMessage(ChatColor.RED  + "That is locked by " + lock.ownername);
			event.setCancelled(true);
		}
	}

	// Clear locks of a player when they quit the game
	@EventHandler(ignoreCancelled=true)
	public void onPlayerQuitMinigame(QuitMinigameEvent event) {
		// Early exit if there are no locks at all
		if (Locks.isEmpty()) return;
		ClearPlayerLocks(Minigames.plugin.pdata.getMinigamePlayer(event.getPlayer()));
	}

	// Clear locks of a player when they disconnect
	@EventHandler(ignoreCancelled=true)
	public void onPlayerDisconnect(PlayerQuitEvent event) {
		// Early exit if there are no locks at all
		if (Locks.isEmpty()) return;
		ClearPlayerLocks(Minigames.plugin.pdata.getMinigamePlayer(event.getPlayer()));
	}
}
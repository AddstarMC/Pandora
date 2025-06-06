package au.com.addstar.pandora;

import au.com.addstar.monolith.ItemMetaBuilder;
import au.com.addstar.monolith.lookup.Lookup;


import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredListener;
import org.bukkit.potion.PotionEffect;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utilities {
    public static long parseDateDiff(String dateDiff) {
        if (dateDiff == null)
            return 0;

        Pattern dateDiffPattern = Pattern.compile("^\\s*(\\-|\\+)?\\s*(?:([0-9]+)y)?\\s*(?:([0-9]+)mo)?\\s*(?:([0-9]+)w)?\\s*(?:([0-9]+)d)?\\s*(?:([0-9]+)h)?\\s*(?:([0-9]+)m)?\\s*(?:([0-9]+)s)?\\s*$");
        dateDiff = dateDiff.toLowerCase();

        Matcher m = dateDiffPattern.matcher(dateDiff);

        if (m.matches()) {
            int years, months, weeks, days, hours, minutes, seconds;
            boolean negative;

            if (m.group(1) != null)
                negative = (m.group(1).compareTo("-") == 0);
            else
                negative = false;

            if (m.group(2) != null)
                years = Integer.parseInt(m.group(2));
            else
                years = 0;

            if (m.group(3) != null)
                months = Integer.parseInt(m.group(3));
            else
                months = 0;

            if (m.group(4) != null)
                weeks = Integer.parseInt(m.group(4));
            else
                weeks = 0;

            if (m.group(5) != null)
                days = Integer.parseInt(m.group(5));
            else
                days = 0;

            if (m.group(6) != null)
                hours = Integer.parseInt(m.group(6));
            else
                hours = 0;

            if (m.group(7) != null)
                minutes = Integer.parseInt(m.group(7));
            else
                minutes = 0;

            if (m.group(8) != null)
                seconds = Integer.parseInt(m.group(8));
            else
                seconds = 0;

            // Now calculate the time
            long time = 0;
            time += seconds * 1000L;
            time += minutes * 60000L;
            time += hours * 3600000L;
            // Use standard real world values when converting larger time units
            // into milliseconds rather than the old shortened versions
            time += days * 86400000L;
            time += weeks * 604800000L;
            time += months * 2592000000L;
            time += years * 31536000000L;

            if (negative)
                time *= -1;

            return time;
        }

        return 0;
    }

    public static boolean safeTeleport(Player player, Location loc) {
        int horRange = 30;

        double closestDist = Double.MAX_VALUE;
        Location closest = null;

        for (int y = 0; y < loc.getWorld().getMaxHeight(); ++y) {
            for (int x = loc.getBlockX() - horRange; x < loc.getBlockX() + horRange; ++x) {
                for (int z = loc.getBlockZ() - horRange; z < loc.getBlockZ() + horRange; ++z) {
                    for (int i = 0; i < 2; ++i) {
                        int yy = loc.getBlockY();

                        if (i == 0) {
                            yy -= y;
                            if (yy < 0)
                                continue;
                        } else {
                            yy += y;
                            if (yy >= loc.getWorld().getMaxHeight())
                                continue;
                        }

                        Location l = new Location(loc.getWorld(), x, yy, z);
                        double dist = loc.distanceSquared(l);

                        if (dist < closestDist && isSafeLocation(l)) {
                            closest = l;
                            closestDist = dist;
                        }
                    }
                }
            }

            if (y * y > closestDist)
                break;
        }

        if (closest == null)
            return false;

        closest.setPitch(loc.getPitch());
        closest.setYaw(loc.getYaw());

        return player.teleport(closest.add(0.5, 0, 0.5));
    }

    public static boolean isSafeLocation(Location loc) {
        Block feet = loc.getBlock();
        Block ground = feet.getRelative(BlockFace.DOWN);
        Block head = feet.getRelative(BlockFace.UP);

        return (isSafe(feet) && isSafe(head) && (head.getType() != Material.WATER) && ground.getType().isSolid());
    }

    private static boolean isSafe(Block block) {
        switch (block.getType()) {
            case AIR:
            case SUGAR_CANE:
            case WATER:
            case TALL_GRASS:
            case WHEAT:
            case CARROT:
            case POTATO:
            case RED_MUSHROOM:
            case POPPY:
            case BROWN_MUSHROOM:
            case SUNFLOWER:

            case DEAD_BUSH:
            case SPRUCE_SIGN:
            case SPRUCE_WALL_SIGN:
            case ACACIA_WALL_SIGN:
            case ACACIA_SIGN:
            case BIRCH_SIGN:
            case BIRCH_WALL_SIGN:
            case DARK_OAK_SIGN:
            case DARK_OAK_WALL_SIGN:
            case JUNGLE_SIGN:
            case JUNGLE_WALL_SIGN:
            case OAK_SIGN:
            case OAK_WALL_SIGN:
                return true;
            default:
                return false;
        }
    }

    public static void adjustEventHandlerPosition(HandlerList list, Listener listener, String beforePlugin) {
        Plugin plugin = Bukkit.getPluginManager().getPlugin(beforePlugin);
        if (plugin == null || !plugin.isEnabled())
            return;

        ArrayList<RegisteredListener> theirs = new ArrayList<>();
        RegisteredListener mine = null;

        for (RegisteredListener regListener : list.getRegisteredListeners()) {
            if (regListener.getListener() == listener)
                mine = regListener;
            if (regListener.getPlugin().equals(plugin))
                theirs.add(regListener);
        }

        if (mine == null)
            return;

        list.unregister(mine);
        for (RegisteredListener regListener : theirs)
            list.unregister(regListener);

        // Register in the order we want them in
        list.register(mine);
        list.registerAll(theirs);
        list.bake();

        MasterPlugin.getInstance().getLogger().info("NOTE: Listener " + listener + " injected before that of " + beforePlugin + " listener");
    }

    public static List<String> matchStrings(String str, Collection<String> values) {
        str = str.toLowerCase();
        ArrayList<String> matches = new ArrayList<>();

        for (String value : values) {
            if (value.toLowerCase().startsWith(str))
                matches.add(value);
        }

        if (matches.isEmpty())
            return null;
        return matches;
    }

    public static Material getMaterial(String name) {
        // Bukkit name
        Material mat = Material.getMaterial(name.toUpperCase());
        if (mat != null)
            return mat;
        // ItemDB
        return Lookup.findItemByName(name);
    }

    public static ItemStack getItem(String[] args, int start) throws IllegalArgumentException {
        // Attempt to match an item by exact Minecraft key
        Material mat = null;
        try {
            mat = Lookup.findByMinecraftName(args[start]);
        } catch (Exception e) {
            throw new IllegalArgumentException("Attempted to pass an illegal string to the minecraft find by key command");
        }

        int index = start;
        if (mat == null) {
            // No match by Minecraft name - attempt MATERIAL match
            mat = getMaterial(args[index]);
            if (mat == null)
                throw new IllegalArgumentException("Unknown material " + args[index]);
        }
        index++;

        // Parse amount (if given)
        int amount = mat.getMaxStackSize();
        if (args.length > index) {
            try {
                amount = Integer.parseInt(args[index]);
                if (amount < 0)
                    throw new IllegalArgumentException("Amount value cannot be less than 0");
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Unable to parse amount value " + args[index]);
            }
            ++index;
        }
        ItemStack item = new ItemStack(mat, amount);

        // Parse Meta (if given)
        if (args.length > index) {
            ItemMetaBuilder builder = new ItemMetaBuilder(item);
            for (int i = index; i < args.length; ++i) {
                String definition = args[i].replace('_', ' ');
                builder.accept(definition);
            }
            item.setItemMeta(builder.build());
        }

        return item;
    }

    public static String getName(ItemStack i) {
        if (i.getType() == Material.POTION) {
            ItemMeta meta = i.getItemMeta();
            String name = null;
            if (meta instanceof PotionMeta) {
                name = getPotionName(i);
            }
            if (name != null) {
                return name;
            }
        } else if (i.getType().isRecord()) {
            return getRecordName(i.getType());
        } else if (i.getType() == Material.MAP) {
            ItemMeta meta = i.getItemMeta();
            if (meta instanceof MapMeta) {
                if (((MapMeta) meta).getMapView() == null) {
                    return "Empty Map";
                }
                if (((MapMeta) meta).getMapView().isVirtual()) {
                    return "Virtual Map";
                }
                int x = ((MapMeta) meta).getMapView().getCenterX();
                int z = ((MapMeta) meta).getMapView().getCenterZ();
                String world = ((MapMeta) meta).getMapView().getWorld().getName();
                return String.format("Map of %1s centered on %2d, %3d", world, x, z);
            }

        }
        return prettifyText(i.getType().getKey().getKey());
    }

    private static String prettifyText(String ugly) {
        if (!ugly.contains("_") && (!ugly.equals(ugly.toUpperCase()))) {
            return ugly;
        }
        StringBuilder fin = new StringBuilder();
        ugly = ugly.toLowerCase();
        if (ugly.contains("_")) {
            final String[] splt = ugly.split("_");
            int i = 0;
            for (final String s : splt) {
                i += 1;
                if (s.isEmpty()) {
                    continue;
                }

                if (s.length() == 1) {
                    fin.append(Character.toUpperCase(s.charAt(0)));
                } else {
                    fin.append(Character.toUpperCase(s.charAt(0))).append(s.substring(1));
                }

                if (i < splt.length) {
                    fin.append(" ");
                }
            }
        } else {
            fin.append(Character.toUpperCase(ugly.charAt(0))).append(ugly.substring(1));
        }
        return fin.toString();
    }

    private static String getRecordName(Material record) {
        switch (record) {
            case MUSIC_DISC_13:
                return "Record - 13";
            case MUSIC_DISC_CAT:
                return "Record - cat";
            case MUSIC_DISC_BLOCKS:
                return "Record - blocks";
            case MUSIC_DISC_CHIRP:
                return "Record - chirp";
            case MUSIC_DISC_FAR:
                return "Record - far";
            case MUSIC_DISC_MALL:
                return "Record - mall";
            case MUSIC_DISC_MELLOHI:
                return "Record - mellohi";
            case MUSIC_DISC_STAL:
                return "Record - stal";
            case MUSIC_DISC_STRAD:
                return "Record - strad";
            case MUSIC_DISC_WARD:
                return "Record - ward";
            case MUSIC_DISC_11:
                return "Record - 11";
            case MUSIC_DISC_WAIT:
                return "Record - wait";
            default:
                throw new AssertionError("Unknown record " + record);
        }
    }


    private static String getPotionName(ItemStack item) {
        PotionMeta meta = null;
        String prefix = "MAIN-";
        if (item.getType() == Material.POTION) {
            if (item.getItemMeta() instanceof PotionMeta) {
                meta = (PotionMeta) item.getItemMeta();
                prefix += "Potion";
            } else {
                return "Water Bottle";
            }
        }
        if (item.getType() == Material.SPLASH_POTION) {
            if (item.getItemMeta() instanceof PotionMeta) {
                meta = (PotionMeta) item.getItemMeta();
                prefix += "Splash Potion";
            } else {
                return "Splasn Water Bottle";
            }
        }
        if (item.getType() == Material.LINGERING_POTION) {
            if (item.getItemMeta() instanceof PotionMeta) {
                meta = (PotionMeta) item.getItemMeta();
                prefix += "Lingering Potion";
            } else {
                return "Lingering Water Bottle";
            }
        }
        if (meta == null) {
            return prefix + "NO Potion Data ";
        }
        if (meta.getBasePotionData().isExtended()) {
            prefix += "Extended Duration ";
        }
        if (meta.getBasePotionData().isUpgraded()) {
            prefix += "Amplified Effect ";
        }
        prefix += meta.getDisplayName();
        boolean noEffects;
        List<PotionEffect> potionEffects = meta.getCustomEffects();
        noEffects = potionEffects.isEmpty();
        if (!noEffects) {
            PotionEffect maineffect = potionEffects.get(0);
            prefix += maineffect.getType().getName() + ",Duration=" + maineffect.getDuration() + "Amplifier=" + maineffect.getAmplifier();
            StringBuilder effects = new StringBuilder("Full Effect List -- ");
            for (final PotionEffect effect : potionEffects) {
                effects.append(" Effect:").append(effect.getType().getName()).append(" Amp:").append(effect.getAmplifier()).append(" Dur:").append(effect.getDuration());
            }
            return prefix + effects;
        }
        return null;
    }
}

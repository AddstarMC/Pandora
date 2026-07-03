package au.com.addstar.pandora.modules;

import au.com.addstar.monolith.lookup.Lookup;
import au.com.addstar.pandora.MasterPlugin;
import au.com.addstar.pandora.Module;
import au.com.addstar.pandora.Utilities;
import io.papermc.paper.datacomponent.DataComponentType;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemAttributeModifiers;
import io.papermc.paper.persistence.PersistentDataContainerView;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created for use for the Add5tar MC Minecraft server
 * Created by benjamincharlton on 13/09/2015.
 * <p>
 * Reports information about the item in the player's main hand.
 * {@code /itemmeta} shows a readable summary (name, durability, potion, lore,
 * enchants, flags and attribute modifiers). {@code /itemmeta raw} additionally
 * dumps every data component present on the item and all plugin persistent-data
 * (PDC) keys/values.
 */
public class ItemMetaReporter implements Module, CommandExecutor, TabCompleter {

    // Components already rendered in readable form by the summary (and, for
    // attribute_modifiers, by printAttributes). We skip these in the raw
    // component dump to avoid repeating them as large toString() blobs -
    // minecraft:lore in particular is enormous.
    private static final Set<DataComponentType> SUMMARISED_COMPONENTS = Set.of(
            DataComponentTypes.ATTRIBUTE_MODIFIERS,
            DataComponentTypes.ENCHANTMENTS,
            DataComponentTypes.CUSTOM_NAME,
            DataComponentTypes.ITEM_NAME,
            DataComponentTypes.LORE,
            DataComponentTypes.DAMAGE
    );

    // The same set as namespaced-key strings (e.g. "minecraft:lore"), for
    // matching against the serialized-component map keys in the nbt dump.
    private static final Set<String> SUMMARISED_COMPONENT_KEYS = SUMMARISED_COMPONENTS.stream()
            .map(type -> type.getKey().toString())
            .collect(java.util.stream.Collectors.toUnmodifiableSet());

    // The common PDC types we can decode a value for. Foreign plugins may use
    // other/complex types; those are reported as present but not decoded.
    private static final PersistentDataType<?, ?>[] SIMPLE_PDC_TYPES = {
            PersistentDataType.STRING,
            PersistentDataType.BYTE,
            PersistentDataType.SHORT,
            PersistentDataType.INTEGER,
            PersistentDataType.LONG,
            PersistentDataType.FLOAT,
            PersistentDataType.DOUBLE,
            PersistentDataType.BOOLEAN,
            PersistentDataType.BYTE_ARRAY,
            PersistentDataType.INTEGER_ARRAY,
            PersistentDataType.LONG_ARRAY,
    };

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] args) {
        // Args are order-independent: "raw" and "nbt" flags plus an optional
        // target player name. "nbt" implies "raw" (it is the fullest output).
        boolean raw = false;
        boolean nbt = false;
        String targetName = null;
        for (String arg : args) {
            if (arg.equalsIgnoreCase("raw")) {
                raw = true;
            } else if (arg.equalsIgnoreCase("nbt")) {
                nbt = true;
                raw = true;
            } else {
                targetName = arg;
            }
        }

        // Resolve whose held item to inspect. A named target lets this be run
        // from the console (where the full output is readable).
        Player target;
        if (targetName != null) {
            target = Bukkit.getPlayerExact(targetName);
            if (target == null) {
                commandSender.sendMessage(ChatColor.RED + "Player not found or not online: " + targetName);
                return true;
            }
        } else if (commandSender instanceof Player) {
            target = (Player) commandSender;
        } else {
            commandSender.sendMessage(ChatColor.RED + "From console you must specify a player: /" + s + " [raw] <player>");
            return true;
        }

        ItemStack item = target.getInventory().getItemInMainHand();
        if (item.getType() == Material.AIR) {
            commandSender.sendMessage(ChatColor.RED + target.getName() + " is not holding an item.");
            return true;
        }

        printSummary(commandSender, item);

        if (raw) {
            printAttributes(commandSender, item);
            printComponents(commandSender, item);
            printPersistentData(commandSender, item);
        }

        if (nbt) {
            printNbt(commandSender, item);
        }

        return true;
    }

    private void printSummary(CommandSender sender, ItemStack item) {
        sender.sendMessage(ChatColor.GOLD + "Item Name: " + ChatColor.RED + Utilities.getName(item));
        sender.sendMessage(ChatColor.GOLD + "Item Type: " + ChatColor.RED + item.getType().toString());
        String mcName = Lookup.findMinecraftNameByItem(item.getType());
        if (mcName != null)
            sender.sendMessage(ChatColor.GOLD + "Minecraft Name: " + ChatColor.RED + mcName);

        ItemMeta imeta = item.getItemMeta();
        if (imeta instanceof Damageable) {
            int maxDura = item.getType().getMaxDurability();
            int uses = maxDura + 1 - ((Damageable) imeta).getDamage();
            sender.sendMessage(ChatColor.GOLD + "Durability: " + ChatColor.RED + ((Damageable) imeta).getDamage() + " / " + (maxDura + 1) + " (" + uses + " uses)");
        }
        if (item.getType() == Material.POTION || item.getType() == Material.SPLASH_POTION || item.getType() == Material.LINGERING_POTION) {
            StringBuilder msg = new StringBuilder(" **Potion** \n");
            PotionMeta meta = (PotionMeta) imeta;
            if (meta != null) {
                msg.append(" Main type: ");
                if (!meta.getCustomEffects().isEmpty() && meta.getCustomEffects().get(0) != null) {
                    msg.append(meta.getCustomEffects().get(0).getType().getKey());
                    msg.append("  Strength: ")
                            .append(meta.getCustomEffects().get(0).getAmplifier()).append(" Dur Mod: ").append(meta.getCustomEffects().
                            get(0).getDuration());
                    if (item.getType() == Material.SPLASH_POTION) msg.append(" Splash Potion: YES");
                    if (item.getType() == Material.LINGERING_POTION) msg.append(" Lingering Potion: YES");
                } else {
                    msg.append("  Custom Potion - could not be cast to a real potion.");
                }
                for (PotionEffect e : meta.getCustomEffects()) {
                    msg.append("  Subtype: ").append(e.getType().getKey()).append(" Strength: ").append(e.getAmplifier()).append(" Duration: ").append(e.getDuration());
                }
            }

            sender.sendMessage(ChatColor.GOLD + msg.toString());
        }

        if (item.hasItemMeta()) {
            if (imeta.hasLore()) {
                for (String slore : imeta.getLore()) {
                    sender.sendMessage(ChatColor.GOLD + "Lore: " + slore);
                }
            }
            if (imeta.hasDisplayName()) sender.sendMessage(ChatColor.GOLD + "DisplayName: " + imeta.getDisplayName());
            if (imeta.hasEnchants()) {
                for (Map.Entry<Enchantment, Integer> entry : imeta.getEnchants().entrySet()) {
                    sender.sendMessage(ChatColor.GOLD + "Enchantment: " + entry.getKey().getKey() + " Level: " + entry.getValue());
                }
            }
            Set<ItemFlag> flags = imeta.getItemFlags();
            if (flags != null && !flags.isEmpty()) {
                StringBuilder msg = new StringBuilder("Flags: ");
                for (ItemFlag flag : flags) {
                    msg.append(flag.toString());
                    msg.append(" ");
                }
                sender.sendMessage(ChatColor.GOLD + msg.toString());
            }
        }
    }

    /**
     * Prints the item's attribute modifiers when they have been overridden from
     * the item type's vanilla defaults (e.g. applied by another plugin). Items
     * carry default modifiers (a diamond pickaxe's attack damage/speed) that we
     * skip, so this only fires for genuinely custom attribute data.
     */
    private void printAttributes(CommandSender sender, ItemStack item) {
        if (!item.isDataOverridden(DataComponentTypes.ATTRIBUTE_MODIFIERS)) {
            return;
        }
        ItemAttributeModifiers modifiers = item.getData(DataComponentTypes.ATTRIBUTE_MODIFIERS);
        if (modifiers == null || modifiers.modifiers().isEmpty()) {
            return;
        }

        sender.sendMessage(ChatColor.AQUA + "=== Attribute Modifiers ===");
        for (ItemAttributeModifiers.Entry entry : modifiers.modifiers()) {
            AttributeModifier mod = entry.modifier();
            String amount = (mod.getAmount() >= 0 ? "+" : "") + mod.getAmount();
            sender.sendMessage(ChatColor.GOLD + "  " + entry.attribute().getKey()
                    + ChatColor.RED + "  " + amount + " " + mod.getOperation()
                    + ChatColor.GRAY + "  [" + mod.getSlotGroup() + "]  key=" + mod.getKey());
        }
    }

    /**
     * Dumps the data components that have been <em>overridden</em> on this item
     * (i.e. that differ from the item type's vanilla defaults). This filters out
     * the large block of default component data every item carries, leaving only
     * what something actually changed. Components with a readable value are shown;
     * the rest are listed by key so their presence is visible (e.g.
     * {@code minecraft:custom_data}, whose raw contents are not exposed as a typed
     * value through the API).
     */
    private void printComponents(CommandSender sender, ItemStack item) {
        List<DataComponentType> overridden = new ArrayList<>();
        for (DataComponentType type : item.getDataTypes()) {
            // Skip components already shown readably in the summary / attributes.
            if (SUMMARISED_COMPONENTS.contains(type)) {
                continue;
            }
            if (item.isDataOverridden(type)) {
                overridden.add(type);
            }
        }
        if (overridden.isEmpty()) {
            return;
        }

        sender.sendMessage(ChatColor.AQUA + "=== Components (non-default) ===");
        for (DataComponentType type : overridden) {
            String value = describeComponent(item, type);
            if (value != null) {
                sender.sendMessage(ChatColor.GOLD + "  " + type.getKey() + ChatColor.RED + " = " + value);
            } else {
                sender.sendMessage(ChatColor.GOLD + "  " + type.getKey() + ChatColor.DARK_GRAY + " (present)");
            }
        }
    }

    /**
     * Best-effort readable value for a component. Only the {@code Valued}
     * components can be fetched; returns null for non-valued components or any
     * type we cannot safely stringify.
     */
    private String describeComponent(ItemStack item, DataComponentType type) {
        if (!(type instanceof DataComponentType.Valued)) {
            return null;
        }
        try {
            @SuppressWarnings("unchecked")
            Object value = item.getData((DataComponentType.Valued<Object>) type);
            return value == null ? null : String.valueOf(value);
        } catch (RuntimeException e) {
            return null;
        }
    }

    /**
     * Lists all plugin persistent-data (PDC) keys on the item and, where the
     * value uses a common data type, its value. Keys are always shown so a
     * plugin's custom tags are visible even when the value type is complex.
     */
    private void printPersistentData(CommandSender sender, ItemStack item) {
        PersistentDataContainerView pdc = item.getPersistentDataContainer();
        Set<NamespacedKey> keys = pdc.getKeys();
        if (keys.isEmpty()) {
            return;
        }

        sender.sendMessage(ChatColor.AQUA + "=== Persistent Data (plugin) ===");
        for (NamespacedKey key : keys) {
            String value = describePersistentValue(pdc, key);
            if (value != null) {
                sender.sendMessage(ChatColor.GOLD + "  " + key + ChatColor.RED + " = " + value);
            } else {
                sender.sendMessage(ChatColor.GOLD + "  " + key + ChatColor.DARK_GRAY + " (complex/unknown type)");
            }
        }
    }

    private String describePersistentValue(PersistentDataContainerView pdc, NamespacedKey key) {
        for (PersistentDataType<?, ?> type : SIMPLE_PDC_TYPES) {
            try {
                if (pdc.has(key, type)) {
                    Object value = pdc.get(key, type);
                    if (value instanceof byte[]) {
                        return "byte[" + ((byte[]) value).length + "]";
                    }
                    if (value instanceof int[]) {
                        return "int[" + ((int[]) value).length + "]";
                    }
                    if (value instanceof long[]) {
                        return "long[" + ((long[]) value).length + "]";
                    }
                    return String.valueOf(value);
                }
            } catch (IllegalArgumentException e) {
                // Stored under a different type; keep probing.
            }
        }
        return null;
    }

    /**
     * Dumps the item's full serialized form, including the raw contents of
     * components the typed API hides (notably {@code minecraft:custom_data},
     * where plugins such as X-Prison store their state via NBT libraries).
     * <p>
     * This uses Bukkit's {@link ItemStack#serialize()} config-serialization,
     * so it stays on the public API with no NMS/NBT-library dependency. The
     * output is the serialized structure (nested maps/lists), not vanilla SNBT
     * syntax, but it exposes the values. This is intentionally gated behind the
     * {@code nbt} flag because the output is large and verbose.
     */
    private void printNbt(CommandSender sender, ItemStack item) {
        sender.sendMessage(ChatColor.AQUA + "=== Serialized NBT / components ===");
        Map<String, Object> data;
        try {
            data = item.serialize();
        } catch (RuntimeException e) {
            sender.sendMessage(ChatColor.RED + "  Unable to serialize item: " + e.getMessage());
            return;
        }
        printTree(sender, data, 1);
    }

    /** Recursively prints a serialized value, one node per line, indented by depth. */
    private void printTree(CommandSender sender, Object value, int depth) {
        String indent = "  ".repeat(depth);
        if (value instanceof Map<?, ?> map) {
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                // Skip components already shown readably in the summary (e.g.
                // minecraft:lore, whose SNBT is enormous and adds no new info).
                if (SUMMARISED_COMPONENT_KEYS.contains(String.valueOf(entry.getKey()))) {
                    continue;
                }
                Object v = entry.getValue();
                if (v instanceof Map || v instanceof Iterable) {
                    sender.sendMessage(ChatColor.GOLD + indent + entry.getKey() + ":");
                    printTree(sender, v, depth + 1);
                } else {
                    sender.sendMessage(ChatColor.GOLD + indent + entry.getKey() + ChatColor.RED + " = " + v);
                }
            }
        } else if (value instanceof Iterable<?> iterable) {
            for (Object element : iterable) {
                if (element instanceof Map || element instanceof Iterable) {
                    sender.sendMessage(ChatColor.GOLD + indent + "-");
                    printTree(sender, element, depth + 1);
                } else {
                    sender.sendMessage(ChatColor.GOLD + indent + "- " + ChatColor.RED + element);
                }
            }
        } else {
            sender.sendMessage(ChatColor.GOLD + indent + ChatColor.RED + value);
        }
    }

    @Override
    public void onEnable() {

    }

    @Override
    public void onDisable() {

    }

    @Override
    public void setPandoraInstance(MasterPlugin plugin) {
        plugin.getCommand("itemmeta").setExecutor(this);
    }

    @Override
    public List<String> onTabComplete(CommandSender commandSender, Command command, String s, String[] args) {
        if (args.length == 0) {
            return null;
        }
        // Which flags were already supplied in earlier args.
        boolean rawGiven = false;
        boolean nbtGiven = false;
        for (int i = 0; i < args.length - 1; i++) {
            if (args[i].equalsIgnoreCase("raw")) {
                rawGiven = true;
            } else if (args[i].equalsIgnoreCase("nbt")) {
                nbtGiven = true;
            }
        }
        String partial = args[args.length - 1].toLowerCase();
        List<String> out = new ArrayList<>();
        if (!rawGiven && "raw".startsWith(partial)) {
            out.add("raw");
        }
        if (!nbtGiven && "nbt".startsWith(partial)) {
            out.add("nbt");
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getName().toLowerCase().startsWith(partial)) {
                out.add(player.getName());
            }
        }
        return out;
    }
}

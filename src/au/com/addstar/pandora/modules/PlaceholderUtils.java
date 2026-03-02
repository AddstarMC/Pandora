package au.com.addstar.pandora.modules;

import au.com.addstar.pandora.MasterPlugin;
import au.com.addstar.pandora.Module;
import me.clip.placeholderapi.PlaceholderAPI;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PlaceholderUtils implements Module, Listener {
    private MasterPlugin mPlugin;
    private PandoraExpansion expansion;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final LegacyComponentSerializer legacySerializer = LegacyComponentSerializer.legacySection();

    @Override
    public void onEnable() {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            expansion = new PandoraExpansion();
            expansion.register();
            log("PlaceholderAPI expansion registered with prefix: pandora_");
        } else {
            log("PlaceholderAPI not found, expansion not registered");
        }
    }

    @Override
    public void onDisable() {
        if (expansion != null) {
            expansion.unregister();
            expansion = null;
        }
    }

    @Override
    public void setPandoraInstance(MasterPlugin plugin) {
        mPlugin = plugin;
    }

    /**
     * Converts MiniMessage format to legacy color codes
     * @param miniMessageText The MiniMessage formatted text
     * @return Legacy formatted text with § color codes
     */
    public String miniMessageToLegacy(String miniMessageText) {
        Component component = miniMessage.deserialize(miniMessageText);
        return legacySerializer.serialize(component);
    }

    /**
     * Converts legacy color codes to MiniMessage format
     * @param legacyText The legacy formatted text with § or & color codes
     * @return MiniMessage formatted text
     */
    public String legacyToMiniMessage(String legacyText) {
        // Replace & with § for consistency
        legacyText = legacyText.replace('&', '§');
        Component component = legacySerializer.deserialize(legacyText);
        return miniMessage.serialize(component);
    }

    /**
     * Strips all formatting from MiniMessage text
     * @param miniMessageText The MiniMessage formatted text
     * @return Plain text without formatting
     */
    public String stripMiniMessage(String miniMessageText) {
        Component component = miniMessage.deserialize(miniMessageText);
        return legacySerializer.serialize(component).replaceAll("§[0-9a-fk-or]", "");
    }

    /**
     * PlaceholderAPI expansion for Pandora placeholders
     */
    private class PandoraExpansion extends PlaceholderExpansion {

        @Override
        public @NotNull String getIdentifier() {
            return "pandora";
        }

        @Override
        public @NotNull String getAuthor() {
            return mPlugin.getDescription().getAuthors().toString();
        }

        @Override
        public @NotNull String getVersion() {
            return mPlugin.getDescription().getVersion();
        }

        @Override
        public boolean persist() {
            return true; // Keep expansion registered across PlaceholderAPI reloads
        }

        @Override
        public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
            // Handle offline player placeholders
            return handlePlaceholder(player, params);
        }

        @Override
        public @Nullable String onPlaceholderRequest(Player player, @NotNull String params) {
            // Handle online player placeholders
            return handlePlaceholder(player, params);
        }

        /**
         * Central placeholder handler
         * @param player The player context (can be null)
         * @param params The placeholder parameters after "pandora_"
         * @return The placeholder value or null if not found
         */
        private String handlePlaceholder(OfflinePlayer player, String params) {
            // MiniMessage conversion placeholders
            if (params.startsWith("from_mm_")) {
                String placeholderName = params.substring("from_mm_".length());
                String resolvedText = resolvePlaceholder(player, placeholderName);
                if (resolvedText == null) {
                    return null;
                }
                return miniMessageToLegacy(resolvedText);
            }

            if (params.startsWith("to_mm_")) {
                String placeholderName = params.substring("to_mm_".length());
                String resolvedText = resolvePlaceholder(player, placeholderName);
                if (resolvedText == null) {
                    return null;
                }
                return legacyToMiniMessage(resolvedText);
            }

            if (params.startsWith("strip_mm_")) {
                String placeholderName = params.substring("strip_mm_".length());
                String resolvedText = resolvePlaceholder(player, placeholderName);
                if (resolvedText == null) {
                    return null;
                }
                return stripMiniMessage(resolvedText);
            }

            // Placeholder not found
            return null;
        }

        /**
         * Resolves a placeholder string by wrapping it with % and using PlaceholderAPI
         * @param player The player context (can be null)
         * @param placeholderName The placeholder name without % symbols
         * @return The resolved placeholder value or null if not found
         */
        private String resolvePlaceholder(OfflinePlayer player, String placeholderName) {
            String placeholder = "%" + placeholderName + "%";
            String resolved = PlaceholderAPI.setPlaceholders(player, placeholder);

            // If the placeholder wasn't resolved, PlaceholderAPI returns the original string
            if (resolved.equals(placeholder)) {
                return null;
            }

            return resolved;
        }
    }
}

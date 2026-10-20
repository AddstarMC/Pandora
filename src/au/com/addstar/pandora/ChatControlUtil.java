package au.com.addstar.pandora;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.mineacademy.chatcontrol.lib.exception.EventHandledException;
import org.mineacademy.chatcontrol.model.Channel;

/**
 * All references to ChatControl classes live here rather than in MasterPlugin, so Pandora
 * can still load when ChatControl is missing or loads after it. Only call this when the
 * ChatControl plugin is present.
 */
public class ChatControlUtil {
    public static void sendMessage(MasterPlugin plugin, CommandSender sender, String channel, String msg) {
        if (Channel.isChannelLoaded(channel)) {
            Channel realchannel = Channel.findChannel(channel);
            String colourmsg = ChatColor.translateAlternateColorCodes('&', msg);
            try {
                realchannel.sendMessage(sender, msg);
            } catch (EventHandledException e) {
                if (e.isCancelled()) {
                    plugin.getLogger().warning("Error: Chat message of " + sender.getName()
                            + " was cancelled by CCR: " + e.getMessage());
                    sender.sendMessage(ChatColor.RED + "Your chat message was cancelled");
                } else {
                    plugin.getLogger().warning("Error: Failed to send chat message of " + sender.getName() + ": "
                            + e.getMessage());
                    sender.sendMessage(ChatColor.RED + "Error sending channel message");
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Error sending channel message: " + e.getClass());
                plugin.getLogger().warning("Reason: " + e.getMessage());
                e.printStackTrace();
                sender.sendMessage(ChatColor.RED + "Error sending channel message");
            }
        } else {
            plugin.getLogger().warning("[Pandora] Invalid channel \"" + channel + "\"");
            Thread.dumpStack();
        }
    }
}

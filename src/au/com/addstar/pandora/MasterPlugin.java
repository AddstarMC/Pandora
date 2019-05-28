package au.com.addstar.pandora;

import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;

/**
 * The type Master plugin.
 */
public class MasterPlugin extends JavaPlugin {
    /**
     * The loaded modules
     */
    private HashMap<String, Module> mLoadedModules;
    /**
     * Available modules
     */
    private HashMap<String, ModuleDefinition> mAvailableModules;
    /**
     * Modules by name
     */
    private HashMap<String, ModuleDefinition> mAvailableModulesByName;
    /**
     * The config
     */
    private Config mConfig;
    /**
     * Static instance
     */
    private static MasterPlugin mInstance;
    
    public boolean isBungeeChatAvailable() {
        return bungeeChatAvailable;
    }
    
    private boolean bungeeChatAvailable = false;
    
    /**
     * Gets instance.
     *
     * @return the instance
     */
    public static MasterPlugin getInstance() {
        return mInstance;
    }
    
    /**
     * Instantiates a new Master plugin.
     */
    public MasterPlugin() {
        mAvailableModules = new HashMap<>();
        mAvailableModulesByName = new HashMap<>();
        
        mLoadedModules = new HashMap<>();
    }
    
    private void registerModules() {
        // TrustedHomes also depends on geSuitHomes; explicit dependency removed due to the Redis refactor of geSuit
        registerModule("TrustedHomes", "au.com.addstar.pandora.modules.TrustedHomes",
                "GriefPrevention");
        registerModule("Quickshop-Griefprevention-Interop", "au.com.addstar.pandora.modules.QuickshopGPInterop",
                "GriefPrevention", "QuickShop");
        registerModule("Vanish-Citizens-Interop", "au.com.addstar.pandora.modules.VanishCitizensIO",
                "VanishNoPacket", "Citizens");
        registerModule("AntiChatRepeater", "au.com.addstar.pandora.modules.AntiChatRepeater");
        registerModule("SignLogger", "au.com.addstar.pandora.modules.SignLogger",
                "BungeeChatBukkit");
        registerModule("KickBanner", "au.com.addstar.pandora.modules.KickBanner");
        registerModule("AntiPortalTrap", "au.com.addstar.pandora.modules.AntiPortalTrap");
        registerModule("LWC-GP-Interop", "au.com.addstar.pandora.modules.LWCGPInterop",
                "LWC", "GriefPrevention");
        registerModule("TPClaim", "au.com.addstar.pandora.modules.TpClaim",
                "GriefPrevention");
        registerModule("FlyCanceller", "au.com.addstar.pandora.modules.FlyCanceller");
        registerModule("PVPHandler", "au.com.addstar.pandora.modules.PVPHandler",
                "WorldGuard");
        registerModule("EventManipulator", "au.com.addstar.pandora.modules.EventManipulator");
        registerModule("BeaconFix", "au.com.addstar.pandora.modules.BeaconFix",
                "ProtocolLib");
        registerModule("AntiPortalEntityTravel", "au.com.addstar.pandora.modules.AntiPortalEntityTravel");
        registerModule("SignColour", "au.com.addstar.pandora.modules.SignColour");
        registerModule("MinigameBCast", "au.com.addstar.pandora.modules.MinigameBroadcaster",
                "Minigames", "BungeeChatBukkit");
        registerModule("AntiBuild", "au.com.addstar.pandora.modules.AntiBuild");
        registerModule("ItemDB", "au.com.addstar.pandora.modules.ItemDB", "Monolith");
        registerModule("ItemMetaReporter", "au.com.addstar.pandora.modules.ItemMetaReporter");
        registerModule("ItemGiving", "au.com.addstar.pandora.modules.ItemGiving",
                "Monolith");
        registerModule("LobbyProtection", "au.com.addstar.pandora.modules.LobbyProtection");
        registerModule("SurvivalGamesBCast", "au.com.addstar.pandora.modules.SurvivalGamesBroadcaster",
                "SurvivalGames", "BungeeChatBukkit");
        registerModule("PlayerList", "au.com.addstar.pandora.modules.PlayerListing",
                "BungeeChatBukkit");
        registerModule("MemoryCleaner", "au.com.addstar.pandora.modules.MemoryCleaner");
        registerModule("AngryPigmen", "au.com.addstar.pandora.modules.AngryPigmen");
        registerModule("ClaimSelect", "au.com.addstar.pandora.modules.ClaimSelect", "GriefPrevention", "WorldEdit");
        registerModule("AntiSpawnerChange", "au.com.addstar.pandora.modules.AntiSpawnerChange");
        registerModule("Sparklers", "au.com.addstar.pandora.modules.Sparklers");
        registerModule("GPExtended", "au.com.addstar.pandora.modules.GPExtended", "GriefPrevention");
        registerModule("MinigameLocks", "au.com.addstar.pandora.modules.MinigameLocks", "Minigames");
        registerModule("PlayerLocationLimit", "au.com.addstar.pandora.modules.PlayerLocationLimit");
        registerModule("BlockhuntBroadcaster", "au.com.addstar.pandora.modules.BlockhuntBroadcaster", "BlockHunt", "BungeeChatBukkit");
        registerModule("BookMonitor", "au.com.addstar.pandora.modules.BookMonitor", "Monolith");
        registerModule("TreasureHelper", "au.com.addstar.pandora.modules.TreasuresHelper", "Treasures");
        registerModule("AntiSwim", "au.com.addstar.pandora.modules.AntiSwim");
        registerModule("PhatLootsHelper", "au.com.addstar.pandora.modules.PhatLootsHelper", "PhatLoots");
        registerModule("SlimeChunk", "au.com.addstar.pandora.modules.SlimeChunk");
        registerModule("DeathInterceptor", "au.com.addstar.pandora.modules.DeathInterceptor");
        registerModule("ActionBlocker", "au.com.addstar.pandora.modules.ActionBlocker");
        //TODO: Register additional modules here
        registerNMSModule("Autosaver", "au.com.addstar.pandora.modules.autosave.Autosaver",
                "1_14_R1");
        registerNMSModule("UndesiredMovementBlocker", "au.com.addstar.pandora.modules.UndesiredMovementBlocker", "1_14_R1", "ProtocolLib");
    }
    
    @Override
    public void onEnable() {
        mInstance = this;
        mConfig = new Config(new File(getDataFolder(), "config.yml"));
        
        getDataFolder().mkdir();
        
        if (mConfig.load())
            mConfig.save();
        
        PandoraCommand cmd = new PandoraCommand(this);
        getCommand("pandora").setExecutor(cmd);
        getCommand("pandora").setTabCompleter(cmd);
        
        registerModules();
        loadModules();
    }
    
    @Override
    public void onDisable() {
        for (Entry<String, Module> module : mLoadedModules.entrySet()) {
            try {
                module.getValue().onDisable();
            } catch (Throwable e) {
                getLogger().severe("Error disabling module: " + module.getKey());
                e.printStackTrace();
            }
        }
        deregisterBungeeChat();
        mLoadedModules.clear();
        mAvailableModules.clear();
        mAvailableModulesByName.clear();
        mInstance = null;
    }
    
    /**
     * Is module loaded boolean.
     *
     * @param module the module
     *
     * @return the boolean
     */
    public final boolean isModuleLoaded(String module) {
        return mLoadedModules.containsKey(module);
    }
    
    /**
     * Gets all modules.
     *
     * @return the all modules
     */
    public final Set<String> getAllModules() {
        return Collections.unmodifiableSet(mAvailableModulesByName.keySet());
    }
    
    /**
     * Reload module boolean.
     *
     * @param module the module
     *
     * @return the boolean
     */
    public final boolean reloadModule(String module) {
        if (!isModuleLoaded(module))
            return loadModule(module);
        
        Module instance = mLoadedModules.get(module);
        
        mLoadedModules.remove(module);
        
        try {
            instance.onDisable();
        } catch (Throwable e) {
            getLogger().severe("Error disabling module: " + module);
            e.printStackTrace();
            return false;
        }
        
        try {
            instance.onEnable();
        } catch (Throwable e) {
            getLogger().severe("Error enabling module: " + module);
            e.printStackTrace();
            return false;
        }
        
        mLoadedModules.put(module, instance);
        return true;
    }
    
     /**
     *  Enable module boolean.
     *
     * @param module the module
     *
     * @return the boolean
     */
    public final boolean enableModule(String module) {
        if (isModuleLoaded(module))
            return false;
        
        return loadModule(module);
    }
    
    /**
     * Disable module boolean.
     *
     * @param module the module
     *
     * @return the boolean
     */
    public final boolean disableModule(String module) {
        if (!isModuleLoaded(module))
            return false;
        
        Module instance = mLoadedModules.get(module);
        
        mLoadedModules.remove(module);
        
        try {
            instance.onDisable();
            if (instance instanceof Listener)
                HandlerList.unregisterAll((Listener) instance);
        } catch (Throwable e) {
            getLogger().severe("Error disabling module: " + module);
            e.printStackTrace();
            return false;
        }
        
        return true;
    }
    
    /**
     * Registers a module for loading
     *
     * @param name         Name of module
     * @param moduleClass  Class for the module
     * @param dependencies Names of plugins needed for this module to load
     */
    public void registerModule(String name, String moduleClass, String... dependencies) {
        ModuleDefinition def = new ModuleDefinition(name, moduleClass, dependencies);
        mAvailableModules.put(moduleClass, def);
        mAvailableModulesByName.put(name, def);
    }
    
    /**
     * Registers a module with NMS code for loading
     *
     * @param name         Name of module
     * @param moduleClass  Class for the module
     * @param version      The CB version that must be loaded. This should be in the form of "1_7_R4"
     * @param dependencies Names of plugins needed for this module to load
     */
    public void registerNMSModule(String name, String moduleClass, String version, String... dependencies) {
        if (!version.equals(getCBVersion())) {
            getLogger().severe("[NMS Module] Cannot load " + name + ". Required: " + version + " has: " + getCBVersion());
            return;
        }
        
        registerModule(name, moduleClass, dependencies);
    }
    
    private String mCBVersion = null;
    
    private String getCBVersion() {
        if (mCBVersion == null) {
            String name = Bukkit.getServer().getClass().getName();
            name = name.substring("org.bukkit.craftbukkit.v".length());
            name = name.substring(0, name.indexOf("."));
            mCBVersion = name;
        }
        
        return mCBVersion;
    }
    
    private void loadModules() {
        mLoadedModules.clear();
        
        for (String name : mAvailableModulesByName.keySet()) {
            if (!mConfig.disabledModules.contains(name.toLowerCase()))
                loadModule(name);
            else
                getLogger().info(String.format("[%s] Not enabling, disabled from config", name));
        }
    }
    
    private boolean loadModule(String name) {
        ModuleDefinition module = mAvailableModulesByName.get(name);
        
        StringBuilder missingDeps = new StringBuilder();
        
        for (String plugin : module.dependencies) {
            if (!Bukkit.getPluginManager().isPluginEnabled(plugin)) {
                if (missingDeps.length() > 0)
                    missingDeps.append(", ");
                missingDeps.append(plugin);
            }
        }
        
        if (missingDeps.length() > 0) {
            getLogger().info(String.format("[%s] Not enabling, missing dependencies: %s", name, missingDeps.toString()));
            return false;
        }
        
        Module instance = createModule(module.name, module.moduleClass);
        
        if (instance == null)
            return false;
        
        mLoadedModules.put(module.name, instance);
        
        return true;
    }
    
    /**
     * Register bungee chat boolean.
     *
     * @return the boolean
     */
    public boolean registerBungeeChat() {
        bungeeChatAvailable = registerMessageChannel("bungeechat:chat");
        
        //  try {
        // Class clazz = this.getClassLoader().loadClass("au.com.addstar.bc.BungeeChat"); //force a class loader error if no BungeeChat
        return bungeeChatAvailable;
        // }catch (ClassNotFoundException exception){
        //     this.getLogger().warning("Attempt to Register a Bungee Chat Channel with no BungeeChat installed.");
        //  }
        //  return false;
    }
    
    /**
     * Register message channel boolean.
     *
     * @param channel the channel
     *
     * @return the boolean
     */
    public boolean registerMessageChannel(String channel) {
        Set<String> channels = Bukkit.getMessenger().getOutgoingChannels(this);
        if (channels.size() > 0 && channels.contains(channel)) {
            return true;
        } else {
            Bukkit.getMessenger().registerOutgoingPluginChannel(this, channel);
            return true;
        }
    }
    
    /**
     * Deregister bungee chat.
     */
    public void deregisterBungeeChat() {
        deregisterMessagechannel("bungeechat:chat");
        bungeeChatAvailable = false;
    }
    
    
    /**
     * Deregister messagechannel.
     *
     * @param channel the channel
     */
    public void deregisterMessagechannel(String channel) {
        Set<String> channels = Bukkit.getMessenger().getOutgoingChannels(this);
        if (channels.size() > 0 && channels.contains(channel)) {
            Bukkit.getMessenger().unregisterOutgoingPluginChannel(this, channel);
        }
    }
    
    
    private Module createModule(String name, String moduleClass) {
        try {
            Class<?> rawClazz = Class.forName(moduleClass);
            if (!Module.class.isAssignableFrom(rawClazz)) {
                getLogger().severe("Module class '" + moduleClass + "' is not an instance of Module!");
                return null;
            }
            
            Module module = rawClazz.asSubclass(Module.class).newInstance();
            module.setPandoraInstance(this);
            
            try {
                module.onEnable();
                if (module instanceof Listener)
                    if(!module.disableListener()) {
                        Bukkit.getPluginManager().registerEvents((Listener) module, this);
                    }else{
                        module.log("Listener was disabled");
                    }
                
                return module;
            } catch (Throwable e) {
                getLogger().severe("Failed to enable module: " + name);
                e.printStackTrace();
            }
        } catch (InstantiationException | ExceptionInInitializerError e) {
            getLogger().severe("Failed to instanciate " + name);
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            getLogger().severe("Failed to instanciate " + name + ". No public default constructor available.");
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            getLogger().severe("Failed to instanciate " + name + ". Class not found");
            e.printStackTrace();
        }
        
        return null;
    }
    
    private static class Config extends AutoConfig {
        /**
         * Instantiates a new Config.
         *
         * @param file the file
         */
        public Config(File file) {
            super(file);
        }
    
        /**
         * The Disabled modules.
         */
        @ConfigField()
        public HashSet<String> disabledModules = new HashSet<>();
        
        @Override
        protected void onPostLoad() {
            HashSet<String> lowerCaseSet = new HashSet<>(disabledModules.size());
            
            for (String name : disabledModules) {
                lowerCaseSet.add(name.toLowerCase());
            }
            
            disabledModules = lowerCaseSet;
        }
    }
    
    private static class ModuleDefinition {
        /**
         * The Name.
         */
        public final String name;
        /**
         * The Module class.
         */
        public final String moduleClass;
        /**
         * The Dependencies.
         */
        public final String[] dependencies;
    
        /**
         * Instantiates a new Module definition.
         *
         * @param name         the name
         * @param moduleClass  the module class
         * @param dependencies the dependencies
         */
        public ModuleDefinition(String name, String moduleClass, String... dependencies) {
            this.name = name;
            this.moduleClass = moduleClass;
            if (dependencies == null)
                this.dependencies = new String[0];
            else
                this.dependencies = dependencies;
        }
    }
    
}

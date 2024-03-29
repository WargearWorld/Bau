package net.wargearworld.bau;

import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagConflictException;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;
import net.wargearworld.bau.advancement.AdvManager;
import net.wargearworld.bau.commands.*;
import net.wargearworld.bau.communication.DatabaseCommunication;
import net.wargearworld.bau.config.BauConfig;
import net.wargearworld.bau.listener.*;
import net.wargearworld.bau.player.BauPlayer;
import net.wargearworld.bau.player.DefaultPlayer;
import net.wargearworld.bau.tabCompleter.TntReloaderTC;
import net.wargearworld.bau.tabCompleter.tbsTC;
import net.wargearworld.bau.tools.*;
import net.wargearworld.bau.tools.cannon_reloader.AutoCannonReloaderListener;
import net.wargearworld.bau.tools.cannon_timer.CannonTimerListener;
import net.wargearworld.bau.tools.explosion_cache.ExplosionCacheListener;
import net.wargearworld.bau.tools.particles.Particles;
import net.wargearworld.bau.tools.plotrights.PlotRights;
import net.wargearworld.bau.tools.testBlock.TestBlockCore;
import net.wargearworld.bau.tools.testBlock.testBlock.DefaultTestBlock;
import net.wargearworld.bau.tools.testBlock.testBlockEditor.TestBlockEditorCore;
import net.wargearworld.bau.tools.waterremover.WaterRemoverListener;
import net.wargearworld.bau.tools.worldfuscator.WorldFuscatorCommand;
import net.wargearworld.bau.tools.worldfuscator.WorldFuscatorIntegration;
import net.wargearworld.bau.world.WorldCommand;
import net.wargearworld.bau.world.gui.WorldGUI;
import net.wargearworld.bau.world.WorldManager;
import net.wargearworld.bau.worldedit.SaWE;
import net.wargearworld.bau.listener.WorldEditPreCommand;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

public class Main extends JavaPlugin {
    private static Main plugin;
    public static StateFlag TntExplosion;
    public static StateFlag stoplag;



    public static String prefix = "§8[§6Bau§8] §r";
    public static File tempAddConfigFile;
    public static YamlConfiguration tempAddConfig;

    private WorldFuscatorIntegration integration;

    @Override
    public void onEnable() {
        super.onEnable();
        plugin = this;
        createConfigs();

        registerCommands();
        registerListener();
        WorldManager.startCheckForTempAddRemoves();
        WorldManager.checkForWorldsToUnload();
        new CompassBar();
//        new GUI_API(this, MessageHandler.getInstance());
        new WorldGUI(this);
        DefaultTestBlock.generateDefaultTestBlocks();
        DatabaseCommunication.startRecieve();
        integration =  new WorldFuscatorIntegration(this);
        integration.start();
        AdvManager.start(this);
    }

    private void registerListener() {
        new TNT(this);
        new PlayerListener(this);
        new SignListener(this);
        new PlayerMovement(this);
        new SpawnEvent(this);
        new WaterRemoverListener(this);
        new ExplosionCacheListener();
        new GUI(this);
        new CannonTimerListener(this);
        PluginManager pm = Bukkit.getPluginManager();
        pm.registerEvents(TestBlockCore.getInstance(), this);
        pm.registerEvents(new EventsToCancel(), this);
        pm.registerEvents(new Stoplag(), this);
        pm.registerEvents(new WorldEditPreCommand(), this);
        pm.registerEvents(new DesignTool(), this);
        pm.registerEvents(new TntChest(), this);
        pm.registerEvents(new TestBlockEditorCore(), this);
        pm.registerEvents(new SaWE(), this);
        pm.registerEvents(AutoCannonReloaderListener.getInstance(), this);
        pm.registerEvents(new PlotRights(), this);
        pm.registerEvents(new PlayerPreCommand(),this);
    }

    private void registerCommands() {
        new GS();
        new Particles();
        new WorldFuscatorIntegration(this);
        new WorldFuscatorCommand(this);
        new WorldCommand(this);
        new PlotCommand(this);
        new Buy(this);
        getCommand("tbs").setExecutor(TestBlockCore.getInstance());
        getCommand("tbs").setTabCompleter(new tbsTC());
        getCommand("sl").setExecutor(new Stoplag());
        getCommand("dt").setExecutor(new DesignTool());
        getCommand("ds").setExecutor(new DebugStick());
        getCommand("debugstick").setExecutor(new DebugStick());
        getCommand("chest").setExecutor(new TntChest());
        getCommand("stats").setExecutor(new Stats());
        getCommand("plotreset").setExecutor(new PlotResetter());
        getCommand("baureload").setExecutor(new Bau());
        getCommand("tr").setTabCompleter(new TntReloaderTC());
        getCommand("clear").setExecutor(new Clear());
        getCommand("tr").setExecutor(AutoCannonReloaderListener.getInstance());
        getCommand("tr").setTabCompleter(AutoCannonReloaderListener.getInstance());
    }

    public void createConfigs() {

        File customConfigFile = createConfigFile("config.yml");
        FileConfiguration customConfig = createConfig(customConfigFile);
            new BauConfig(customConfig,customConfigFile);

        tempAddConfigFile = createConfigFile("tempAddConfig.yml");
        tempAddConfig = createConfig(tempAddConfigFile);

        Particles.particlesConfigFile = createConfigFile("particles.yml");
        Particles.particleConfig = createConfig(Particles.particlesConfigFile);

        DefaultPlayer.configFile = createConfigFile("playerDefaults.yml");
        DefaultPlayer.config = createConfig(DefaultPlayer.configFile);
    }

    public static File createConfigFile(String string) {
        File configFile = new File(plugin.getDataFolder(), string);
        if (!configFile.exists()) {
            configFile.getParentFile().mkdirs();
            plugin.saveResource(string, false);
        }
        return configFile;
    }

    public static YamlConfiguration createConfig(File configFile) {
        YamlConfiguration config = new YamlConfiguration();

        try {
            FileInputStream in = new FileInputStream(configFile);
            InputStreamReader reader = new InputStreamReader(in, "UTF-8");
            config.load(reader);
        } catch (IOException | InvalidConfigurationException e) {
            e.printStackTrace();
        }

        return config;
    }

    public static Main getPlugin() {
        return plugin;
    }

    @Override
    public void onLoad() {

        FlagRegistry registry = WorldGuard.getInstance().getFlagRegistry();
        try {
            // create a flag with the name "my-custom-flag", defaulting to true
            StateFlag flag = new StateFlag("TntExplosion", false);
            StateFlag stoplag = new StateFlag("stoplag", false);
            StateFlag waterRemoverFlag = new StateFlag("waterremover", false);
            StateFlag worldfuscatorFlag = new StateFlag("worldfuscator", false);
            registry.register(flag);
            registry.register(stoplag);
            registry.register(waterRemoverFlag);
            registry.register(worldfuscatorFlag);
            TntExplosion = flag; // only set our field if there was no error
            Main.stoplag = stoplag;
            WaterRemoverListener.waterRemoverFlag = waterRemoverFlag;
            WorldFuscatorIntegration.worldfuscatorFlag = worldfuscatorFlag;
        } catch (FlagConflictException e) {
            Flag<?> existing = registry.get("TntExplosion");
            Flag<?> existingSL = registry.get("stoplag");
            Flag<?> existingWR = registry.get("waterremover");
            Flag<?> existingWF = registry.get("worldfuscator");
            if ((existing instanceof StateFlag) && existingSL instanceof StateFlag && existingWR instanceof StateFlag && existingWF instanceof StateFlag) {
                TntExplosion = (StateFlag) existing;
                stoplag = (StateFlag) existingSL;
                WaterRemoverListener.waterRemoverFlag = (StateFlag) existingWR;
                WorldFuscatorIntegration.worldfuscatorFlag = (StateFlag) existingWF;
            } else {
                System.out.println("Fehler");
                // types don't match - this is bad news! some other plugin conflicts with you
                // hopefully this never actually happens
            }
        }
        super.onLoad();
    }


    @Override
    public void onDisable() {
        for (World w : Bukkit.getServer().getWorlds()) {
            if (!w.getName().equals("world")) {
                for (Player a : w.getPlayers()) {
                    a.kickPlayer("Close");
                }
                WorldManager.unloadWorld(w.getName());
            }
        }
        super.onDisable();
    }

    public boolean deleteWorld(File path) {
        if (path.exists()) {
            File files[] = path.listFiles();
            for (int i = 0; i < files.length; i++) {
                if (files[i].isDirectory()) {
                    deleteWorld(files[i]);
                } else {
                    files[i].delete();
                }
            }
        }
        return (path.delete());
    }

    public File getTempAddConfigFile() {
        return tempAddConfigFile;
    }

    public YamlConfiguration getTempAddConfig() {
        return tempAddConfig;
    }

    public static void send(Player p, String messageKey, String... args) {
        String message = Main.prefix + MessageHandler.getInstance().getString(p, messageKey);
        for (String rep : args) {
            message = message.replaceFirst("%r", rep);
        }
        p.sendMessage(message);
    }

    public static void send(Player p, boolean otherPrefix, String prefix, String messageKey, String... args) {
        String message = prefix + MessageHandler.getInstance().getString(p, messageKey);
        for (String rep : args) {
            message = message.replaceFirst("%r", rep);
        }
        p.sendMessage(message);
    }

    public static void send(BauPlayer p, String messageKey, String... args) {
        String message = prefix + MessageHandler.getInstance().getString(p, messageKey);
        for (String rep : args) {
            message = message.replaceFirst("%r", rep);
        }
        p.sendMessage(message);
    }

}

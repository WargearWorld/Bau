package de.AS.Bau.Listener;

import java.io.File;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import de.AS.Bau.Main;
import de.AS.Bau.StringGetterBau;
import de.AS.Bau.HikariCP.DBConnection;
import de.AS.Bau.Plots.Plots;
import de.AS.Bau.Scoreboard.ScoreBoardBau;
import de.AS.Bau.Tools.DesignTool;
import de.AS.Bau.utils.CoordGetter;
import de.AS.Bau.utils.ItemStackCreator;
import de.AS.Bau.utils.Language;
import de.AS.Bau.utils.WorldHandler;

public class onPlayerJoin implements Listener {

	public onPlayerJoin(JavaPlugin plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerJoinEvent(PlayerJoinEvent e) {
		// sprache
		Player p = e.getPlayer();
		p.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
		// p.teleport(new Location(Bukkit.getWorld("world"), 0, 30, 0));
		String lang = DBConnection.getLanguage(p);
		StringGetterBau.playersLanguage.put(p.getUniqueId(), Language.getLanguageByString(lang));
		Main main = Main.getPlugin();
		// has own gs?
		String path = main.getCustomConfig().getString("Config.path");
		File gs = new File(path + "/" + p.getUniqueId().toString());
		if (!DBConnection.hasOwnPlots(p.getName()) && !gs.exists()) {
			// Bukkit.createWorld((WorldCreator) WorldCreator.name("test").createWorld());
			// wenn nicht-> erstellen und hinteleportieren
			WorldHandler.createWorldDir(p);
			p.sendMessage(Main.prefix + StringGetterBau.getString(p, "plotGenerating"));
		}
		World world = WorldHandler.loadWorld(p.getUniqueId().toString());

		DesignTool.playerHasDtOn.put(p.getUniqueId(), false);
		String spawnPlot =  Plots.getJoinPlot(p.getUniqueId());
		Location loc = CoordGetter.getTeleportLocation(world,spawnPlot);
		onPlayerMove.playersLastPlot.put(p, spawnPlot);
		p.teleport(loc);
		// wenn ja-> teleportieren
		// item
		p.getInventory().setItem(0, ItemStackCreator.createNewItemStack(Material.NETHER_STAR, "§6GUI"));
		new ScoreBoardBau(p);

	}

}

package net.wargearworld.Bau.World;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.domains.DefaultDomain;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.managers.storage.StorageException;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

import net.wargearworld.Bau.Main;
import net.wargearworld.Bau.MessageHandler;
import net.wargearworld.Bau.HikariCP.DBConnection;
import net.wargearworld.Bau.Listener.onPlayerMove;
import net.wargearworld.Bau.Player.BauPlayer;
import net.wargearworld.Bau.utils.ClickAction;
import net.wargearworld.Bau.utils.HelperMethods;
import net.wargearworld.Bau.utils.JsonCreater;
import net.wargearworld.Bau.utils.MethodResult;

public class BauWorld {
	private UUID worldUUID;

	private int id;
	private String name;
	private HashMap<String, Plot> plots;
	private RegionManager regionManager;
	private WorldTemplate template;

//	private File configFile;
//	private FileConfiguration config;
	private File logFile;
	private String owner; // cpuld be an team!

	private Map<UUID, Date> members;

	public BauWorld(int id, String owner, World world) {
		this.name = world.getName();
		this.id = id;

		regionManager = WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt(world));
		String templateName = DBConnection.getTemplate(id);
		template = WorldTemplate.getTemplate(templateName);

		plots = new HashMap<>();
		for (PlotPattern plotPattern : template.getPlots()) {
			plots.put(plotPattern.getID(), plotPattern.toPlot(this));
		}

//		configFile = new File(Main.getPlugin().getDataFolder(), "worlds/" + world.getName() + "/settings.yml");
		// TODO inf this File not Exists!
//		config = new YamlConfiguration();
		
		logFile = new File(Main.getPlugin().getDataFolder(),"worlds" + id + "/logs.txt");
		try {
			if (!logFile.exists())
				if(!logFile.getParentFile().exists())
					logFile.getParentFile().mkdirs();
				logFile.createNewFile();
//			config.load(configFile);
		} catch (IOException e) {
			e.printStackTrace();
		}

		members = DBConnection.getMembers(id);
		checkForTimeoutMembership();
	}

	public String getName() {
		return name;
	}

	public int getId() {
		return id;
	}

	public Plot getPlot(String plotID) {
		return plots.get(plotID);
	}

	public Plot getPlot(Location loc) {
		BlockVector3 pos = BlockVector3.at(loc.getX(), loc.getY(), loc.getZ());
		return getPlot(regionManager.getApplicableRegionsIDs(pos).get(0));
	}

	public World getWorld() {
		return Bukkit.getWorld(name);
	}

	public RegionManager getRegionManager() {
		return regionManager;
	}

	public void spawn(Player p) {
		Plot plot = plots.get(template.getSpawnPlotID());
		System.out.println(p==null);
		System.out.println(plot == null);
		p.teleport(plot.getTeleportPoint());
		onPlayerMove.playersLastPlot.put(p.getUniqueId(), plot.getId());
	}

	public boolean isAuthorized(UUID uuid) {
		return owner.equalsIgnoreCase(uuid.toString()) || members.keySet().contains(uuid);
	}

	public void showInfo(Player p) {
		boolean isOwner = owner.equals(p.getUniqueId().toString());
		Set<String> memberlist = DBConnection.getMember(p.getUniqueId().toString());
		Main.send(p, "memberListHeader", getName(p.getWorld()));
		for (String memberUUID : memberlist) {
			String memberName = DBConnection.getName(memberUUID);
			String hover = MessageHandler.getInstance().getString(p, "memberHoverRemove").replace("%r", memberName);
			JsonCreater remove = new JsonCreater("§7[§6" + memberName + "§7]");
			if (isOwner) {
				remove.addClickEvent("/gs remove " + memberName, ClickAction.SUGGEST_COMMAND).addHoverEvent(hover);
			}
			remove.send(p);
		}
		if (isOwner) {
			new JsonCreater("§a[+]§r  ").addClickEvent("/gs add ", ClickAction.SUGGEST_COMMAND)
					.addHoverEvent(MessageHandler.getInstance().getString(p, "addMemberHover")).send(p);
		}
		Main.send(p, "timeShow", p.getWorld().getTime() + "");
	}

	public String getName(World w) {
		if (w.getName().contains("test")) {
			return w.getName();
		} else {
			return DBConnection.getName(w.getName());
		}
	}

	public boolean isOwner(Player player) {
		return owner.equals(player.getUniqueId().toString());
	}

	public void addTemp(String playerName, int time) {
		BauPlayer p = BauPlayer.getBauPlayer(UUID.fromString(owner));
		Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("Europe/Berlin"));
		calendar.add(Calendar.HOUR_OF_DAY, time);
		Date to = calendar.getTime();
		if (add(playerName, to) == MethodResult.SUCCESS) {
			Main.send(p, "memberTempAdded", playerName, "" + time);
			log(WorldAction.ADD, playerName, playerName, time + "");
		}
	}

	public MethodResult add(String playerName, Date to) {
		BauPlayer p = BauPlayer.getBauPlayer(UUID.fromString(owner));
		UUID uuidMember = UUID.fromString(DBConnection.getUUID(playerName));
		if (!isAuthorized(uuidMember)) {
			members.put(uuidMember, to);
			if (DBConnection.addMember(id, uuidMember, to)) {
				addPlayerToAllRegions(uuidMember);
				if (to == null) {
					log(WorldAction.ADD, uuidMember.toString(), playerName);
				}
				p.sendMessage(Main.prefix
						+ MessageHandler.getInstance().getString(p, "plotMemberAdded").replace("%r", playerName));

				return MethodResult.SUCCESS;
			} else {
				p.sendMessage(Main.prefix + MessageHandler.getInstance().getString(p, "error"));
				return MethodResult.ERROR;
			}
		} else {
			p.sendMessage(
					Main.prefix + MessageHandler.getInstance().getString(p, "alreadyMember").replace("%r", playerName));
			return MethodResult.FAILURE;
		}
	}

	private void addPlayerToAllRegions(UUID uuidMember) {
		for (ProtectedRegion region : regionManager.getRegions().values()) {
			DefaultDomain members = region.getMembers();
			members.addPlayer(uuidMember);
			region.setMembers(members);
		}
		try {
			regionManager.saveChanges();
		} catch (StorageException e) {
			e.printStackTrace();
		}
	}

	private void removeMemberFromAllRegions(UUID uuidMember) {
		for (ProtectedRegion region : regionManager.getRegions().values()) {
			DefaultDomain members = region.getMembers();
			members.removePlayer(uuidMember);
			region.setMembers(members);
		}
		try {
			regionManager.saveChanges();
		} catch (StorageException e) {
			e.printStackTrace();
		}
	}

	private void log(WorldAction action, String... args) {
		String message = HelperMethods.getTime() + action.getMessage();
		for (String a : args) {
			message = message.replace("%r", a);
		}

		try (FileWriter writer = new FileWriter(logFile, true)) {
			writer.write(message);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void checkForTimeoutMembership() {
		Date date = new Date();
		for (Entry<UUID, Date> entry : members.entrySet()) {
			if (entry.getValue() != null && entry.getValue().after(date)) {
				removeMember(entry.getKey());
			}
		}
	}

	public void setTime(Integer time) {
		getWorld().setTime(time);
	}

	public void removeMember(UUID member) {
		UUID ownerUUID = UUID.fromString(owner);
		Player ownerPlayer = Bukkit.getPlayer(ownerUUID);
		if (!member.toString().equals(this.owner)) {
			if (DBConnection.removeMember(UUID.fromString(this.owner), member)) {
				removeMemberFromAllRegions(member);
				String name = DBConnection.getName(member.toString());
				members.remove(member);
				log(WorldAction.REMOVE, member.toString(), name);
				Player memberPlayer = Bukkit.getPlayer(member);
				if (memberPlayer != null) {
					Main.send(memberPlayer, "plotMemberRemove_memberMsg", DBConnection.getName(owner));
					if (WorldManager.get(memberPlayer.getWorld()) == this) {
						memberPlayer.performCommand("gs");
					}
				}
				if (ownerPlayer != null) {
					Main.send(ownerPlayer, "plotMemberRemoved", name);
				} else {
					DBConnection.addMail("plugin: BAU", owner,
							MessageHandler.getInstance().getString(ownerUUID, "plotMemberRemoved").replace("%r", name));
				}
			} else {
				if (ownerPlayer != null) {
					Main.send(ownerPlayer, "error");
				}
			}
		} else {
			if (ownerPlayer != null)
				Main.send(ownerPlayer, "YouCantRemoveYourself");
		}
	}

	public boolean newWorld() {
		removeAllMembers();
		regionManager = null;
		World world = WorldManager.createNewWorld(this);
		this.worldUUID = world.getUID();
		this.name = world.getName();
		
		regionManager = WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt(world));
		return true;
	}
	public void setTemplate(String templateName) {
		this.template = WorldTemplate.getTemplate(templateName);
		DBConnection.setTemplate(id,template);
	}

	public void removeAllMembers() {
		for (UUID member : members.keySet()) {
			for (ProtectedRegion region : regionManager.getRegions().values()) {
				DefaultDomain members = region.getMembers();
				members.removePlayer(member);
				region.setMembers(members);
			}
		}
		try {
			regionManager.saveChanges();
		} catch (StorageException e) {
			e.printStackTrace();
		}
	}

	public void addAllMembers() {
		for (UUID member : members.keySet()) {
			for (ProtectedRegion region : regionManager.getRegions().values()) {
				DefaultDomain members = region.getMembers();
				members.addPlayer(member);
				region.setMembers(members);
			}
		}
		try {
			regionManager.saveChanges();
		} catch (StorageException e) {
			e.printStackTrace();
		}
	}
	
	private enum WorldAction {
		ADD, ADDTEMP, REMOVE, NEW, DELETE;

		protected String getMessage() {
			switch (this) {
			case ADD:
				return "add %r(Name:%r)";
			case ADDTEMP:
				return ADD.getMessage() + " for %r hours";
			case DELETE:
				return "deleted";
			case NEW:
				return "NEW GS (Removed everyone)";
			case REMOVE:
				return "removed %r(Name: %r)";
			default:
				return "";
			}

		}
	}

	public UUID getWorldUUID() {
		return worldUUID;
	}

	public String getOwner() {
		return owner;
	}


}

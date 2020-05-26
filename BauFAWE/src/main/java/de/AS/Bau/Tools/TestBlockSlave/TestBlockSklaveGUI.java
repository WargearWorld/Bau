package de.AS.Bau.Tools.TestBlockSlave;

import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import de.AS.Bau.StringGetterBau;
import de.AS.Bau.utils.Banner;
import de.AS.Bau.utils.ItemStackCreator;

public class TestBlockSklaveGUI implements Listener{

	/* Main gui: 
	 *  save Block as own TestBlock
	 *  personal tbs (nether star?)
	 *	TBS Editor
	 *
	 *  Paste default TB(old TBS)
	 *
	 *  favorites
	 *  manage favorites
	 *  
	 *  favorties : click and choose direction : If I have to rotate them: z--
	 *  banner: tier 1 with yellow background
	 *  add: per personal tb-> add To favorites
	 *  max favs: 9
	 *  
	 *  save own Block: wodenAxe as symbol:
	 *   -> stand on the right plot!
	 *   -> choose facing of the given on
	 *   -> Name into anvil Inventory(Name banner?)
	 *   -> then saving
	 */
	

	public static Inventory tbsStartInv(Player p, Set<TestBlock> favorites) {
		String inventoryName = StringGetterBau.getString(p, "testBlockSklaveTierInv");
		Inventory inv = Bukkit.createInventory(null, 45, inventoryName);
		
		inv.setItem(2, ItemStackCreator.createNewItemStack(Material.WHITE_WOOL, "§6Editor"));
		inv.setItem(4, ItemStackCreator.createNewItemStack(Material.WOODEN_AXE, StringGetterBau.getString(p, "tbs_gui_newTB")));
		inv.setItem(6, ItemStackCreator.createNewItemStack(Material.NETHER_STAR, StringGetterBau.getString(p, "tbs_gui_favorites")));
		
		ItemStack head = ItemStackCreator.createNewItemStack(Material.PLAYER_HEAD, StringGetterBau.getString(p, "tbs_gui_lastPaste"));
		SkullMeta headmeta = (SkullMeta) head.getItemMeta();
		headmeta.setOwningPlayer(p);
		head.setItemMeta(headmeta);
		inv.setItem(18, head );
		
		inv.setItem(21, Banner.ONE.create(DyeColor.WHITE, DyeColor.BLACK, "§rTier I"));
		inv.setItem(22, Banner.TWO.create(DyeColor.WHITE, DyeColor.BLACK, "§rTier II"));
		inv.setItem(23, Banner.THREE.create(DyeColor.WHITE, DyeColor.BLACK, "§rTier III/IV"));
		
		inv.setItem(26, ItemStackCreator.createNewItemStack(Material.BARRIER,StringGetterBau.getString(p, "tbs_gui_close")));
		
		int favIndex = 36;
		for(TestBlock tb:favorites) {
			if(favIndex>=45) {
				System.err.println("§c[TBS] §6TOO MANY FAVORITES: " + p.getName());
				break;
			}
			inv.setItem(favIndex, tb.getBanner());
			favIndex++;
		}
		for(int i = favIndex; i<45;i++) {	
			inv.setItem(i, Banner.PLUS.create(DyeColor.WHITE, DyeColor.BLACK, StringGetterBau.getString(p,"tbs_gui_addFavorites")));
		}
		return inv;
	}


}

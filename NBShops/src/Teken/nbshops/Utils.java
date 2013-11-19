package Teken.nbshops;

import org.bukkit.Location;
import org.bukkit.entity.Player;

public class Utils {

	public static boolean isOwner(Shop s,Player p){
		if(s.owner.equalsIgnoreCase(p.getName()) || p.isOp())return true;
		return false;
	}
	
	public static boolean isOwnerOrSubOwner(Shop s,Player p){
		if(s.owner.equalsIgnoreCase(p.getName()) || s.subOwners.contains(p.getName()) || p.isOp())return true;
		return false;
	}
	
	public static boolean isSubOwner(Shop s,Player p){
		if(s.subOwners.contains(p.getName()) || p.isOp())return true;
		return false;
	}

	public static boolean isInShop(Main m,Player p){
		for (Shop s : m.shops) {
			int[] max = new int[] { s.region.getMaximumPoint().getBlockX(),
					s.region.getMaximumPoint().getBlockY(),
					s.region.getMaximumPoint().getBlockZ() };
			int[] min = new int[] { s.region.getMinimumPoint().getBlockX(),
					s.region.getMinimumPoint().getBlockY(),
					s.region.getMinimumPoint().getBlockZ() };
			Location loc = p.getLocation();
			loc.add(0, 1, 0);
			for (int x = min[0]; x < max[0] + 1; x++) {
				for (int y = min[1]; y < max[1] + 1; y++) {
					for (int z = min[2]; z < max[2] + 1; z++) {
						
							if (loc.getBlockX() == x
								&& loc.getBlockY()== y
								&&loc.getBlockZ() == z) {
							return true;
						}
					}
				}
			}
		}
		return false;
	}
	
	public static boolean isCoordShop(Main m, Location loc){
		for (Shop s : m.shops) {
			int[] max = new int[] { s.region.getMaximumPoint().getBlockX(),
					s.region.getMaximumPoint().getBlockY(),
					s.region.getMaximumPoint().getBlockZ() };
			int[] min = new int[] { s.region.getMinimumPoint().getBlockX(),
					s.region.getMinimumPoint().getBlockY(),
					s.region.getMinimumPoint().getBlockZ() };
			for (int x = min[0]; x < max[0] + 1; x++) {
				for (int y = min[1]; y < max[1] + 1; y++) {
					for (int z = min[2]; z < max[2] + 1; z++) {
						
							if (loc.getBlockX() == x
								&& loc.getBlockY()== y
								&&loc.getBlockZ() == z) {
							return true;
						}
					}
				}
			}
		}
		return false;
	}
	
	public static Shop getShop(Location loc,Main m){
		return getShop(loc.getBlockX(),loc.getBlockY(),loc.getBlockZ(),m);
	}
	
	public static Shop getShop(int px,int py,int pz,Main m){
		for (Shop s : m.shops) {
			int[] max = new int[] { s.region.getMaximumPoint().getBlockX(),
					s.region.getMaximumPoint().getBlockY(),
					s.region.getMaximumPoint().getBlockZ() };
			int[] min = new int[] { s.region.getMinimumPoint().getBlockX(),
					s.region.getMinimumPoint().getBlockY(),
					s.region.getMinimumPoint().getBlockZ() };
			
			for (int x = min[0]; x < max[0] + 1; x++) {
				for (int y = min[1]; y < max[1] + 1; y++) {
					for (int z = min[2]; z < max[2] + 1; z++) {
						if (px == x
								&& py + 1 == y
								&&pz == z) {
							return s;
						}
					}
				}
			}
		}
		return null;
	}
	public static Shop getShopStandingIn(Player p,Main m){
		return getShop((int) p.getLocation().getBlockX(),
						(int) p.getLocation().getBlockY(), (int) p
								.getLocation().getBlockZ(),m);
	}
	
	public static Shop getShopByID(int ID,Main m){
		for (Shop shop : m.shops) {
			if (shop.id == ID) {
				return shop;
			}
		}
		return null;
	}
	
	public static Shop getShopByName(String name,Main m){
		for (Shop shop : m.shops) {
			if (shop.name.equalsIgnoreCase(name)) {
				return shop;
			}
		}
		return null;
	}
	
	public static boolean isCurrentNameInUse(String name, Main m){
		for(Shop s : m.shops){
			if(s.name.equals(name)){
				return true;
			}
		}
		return false;
	}
}

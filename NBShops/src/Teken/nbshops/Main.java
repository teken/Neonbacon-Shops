package Teken.nbshops;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.WorldSaveEvent;
import org.bukkit.plugin.java.JavaPlugin;

import com.earth2me.essentials.Essentials;
import com.earth2me.essentials.api.Economy;
import com.earth2me.essentials.api.NoLoanPermittedException;
import com.earth2me.essentials.api.UserDoesNotExistException;
import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.regions.Region;

public class Main extends JavaPlugin implements Listener {

	Essentials e;
	WorldEditPlugin g;

	String prevOwner;

	HashMap<Player, ShopRegion> waiting = new HashMap<Player, ShopRegion>();
	
	HashMap<Player, Integer> newSign = new HashMap<Player, Integer>();

	ArrayList<Shop> shops = new ArrayList<Shop>();

	ArrayList<Integer> allowedBlockIDs = new ArrayList<Integer>();

	String WORLD_NAME = "world";

	public void onEnable() {
		this.getServer().getPluginManager().registerEvents(this, this);

		this.e = (Essentials) this.getServer().getPluginManager()
				.getPlugin("Essentials");
		if (e != null)
			System.out.println("[NBShops] Found essentials!");

		this.g = (WorldEditPlugin) this.getServer().getPluginManager()
				.getPlugin("WorldEdit");
		if (g != null)
			System.out.println("[NBShops] Found WorldEdit!");

		this.getCommand("shop").setExecutor(this);
		new Thread(new Runnable() {
			public void run() {
				try {
					loadShops();
				} catch (FileNotFoundException e1) {
					e1.printStackTrace();
				}
			}
		}).start();

		File file = new File("plugins/nbshop/block.whitelist");
		if (!file.exists()) {
			try {
				file.createNewFile();
			} catch (IOException e2) {
				e2.printStackTrace();
			}
		}
		try {
			Scanner scanner = new Scanner(file);
			while (scanner.hasNext()) {
				allowedBlockIDs.add(Integer.parseInt(scanner.nextLine()));
			}
			scanner.close();
			for(int i :allowedBlockIDs){
				System.out.println("[NBShops] Shop "+i+" Loaded");
			}
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		}

		Bukkit.getScheduler().scheduleAsyncRepeatingTask(this, new Runnable(){
			@Override
			public void run() {
				for (Shop s : shops) {
					try {
						s.save();
					} catch (IOException er) {
						er.printStackTrace();
					}
				}
			}

		}, 6000, 6000);
	}

	@Override
	public void onDisable() {
		for (Shop s : shops) {
			try {
				s.save();
			} catch (IOException er) {
				er.printStackTrace();
			}
		}
	}

	public void loadShops() throws FileNotFoundException {
		File shops = new File("plugins/nbshop/");
		if(!shops.exists())shops.mkdir();
		String[] files = shops.list();
		for (int i = 0; i != files.length; i++) {
			String f = files[i];
			if (f.equalsIgnoreCase("block.whitelist"))
				continue;
			try{
				File shop = new File(shops, f);
				Scanner scanner = new Scanner(shop);

				String name = scanner.nextLine();
				String owner = scanner.nextLine();
				int id = Integer.parseInt(scanner.nextLine());
				String region = scanner.nextLine();
				String[] signLoc = scanner.nextLine().split(":");
				boolean forSale = Boolean.valueOf(scanner.nextLine());
				String[] minSplit = scanner.nextLine().split(":");
				String[] maxSplit = scanner.nextLine().split(":");
				int price = Integer.parseInt(scanner.nextLine());
				Vector min = new Vector(Double.valueOf(minSplit[0]),
						Double.valueOf(minSplit[1]), Double.valueOf(minSplit[2]));
				Vector max = new Vector(Double.valueOf(maxSplit[0]),
						Double.valueOf(maxSplit[1]), Double.valueOf(maxSplit[2]));
				List<String> subOwnersList = new ArrayList<String>();
				try{
					subOwnersList = Arrays.asList(scanner.nextLine().split(":"));
				}catch(Exception e){
				}
				ShopRegion sr = new ShopRegion(max, min, region);
				Shop sObject = new Shop(sr, owner, name, id);
				sObject.forSale = forSale;
				sObject.price = price;
				sObject.subOwners.addAll(subOwnersList);
				Sign sign = (Sign) this.getServer().getWorld(WORLD_NAME)
						.getBlockAt(Integer.parseInt(signLoc[0]),
								Integer.parseInt(signLoc[1]),
								Integer.parseInt(signLoc[2])).getState();
				sObject.setSign(sign);
				this.shops.add(sObject);
				sObject.signLoc = signLoc;
				sObject.updateName();
				sObject.updateStatus();
				scanner.close();
			}catch(Exception e){
				e.printStackTrace();

				System.out.println("[NBShops] Error loading: " + f);
				continue;
			}
		}
	}

	@Override
	public boolean onCommand(CommandSender arg0, Command arg1, String arg2,	String[] cmdArgs) {
		Player p = (Player) arg0;
		if (cmdArgs[0].equalsIgnoreCase("set")) {
			if (!p.isOp())
				return true;
			int sid = 0;
			if(cmdArgs[1] == null)sid = Integer.parseInt(cmdArgs[1]);
			else p.sendMessage("[NBSHOP] Correct Formate: /shop set <ID>");
			
			for(Shop s: this.shops){
				if(s.id == sid){
					p.sendMessage("[NBSHOP] That ID is already in use");
					return true;
				}
			}

			LocalSession ls = g.getAPI().getSession(p);
			Region selection;
			try {
				selection = ls.getSelection(ls.getSelectionWorld());
			} catch (IncompleteRegionException e) {
				p.sendMessage("[NBSHOP] Incomplete region!");
				return true;
			}
			ShopRegion sr = new ShopRegion(selection.getMaximumPoint(),
					selection.getMinimumPoint(), "shop_" + sid);
			p.sendMessage(ChatColor.BLACK + "[" + ChatColor.YELLOW + "NBSHOP"
					+ ChatColor.BLACK + "]" + ChatColor.WHITE + "Shop:" + sid
					+ "set, please select the shops sign");
			waiting.put(p, sr);
			return true;
		}else if (cmdArgs[0].equalsIgnoreCase("name")) {
			String newName = "";
			Shop s = null;
			if (cmdArgs.length == 2 && Utils.isInShop(this, p)) {
				s = Utils.getShopStandingIn(p, this);
				newName = textToColor(cmdArgs[1]);
			} else {
				s = Utils.getShopByID(Integer.parseInt(cmdArgs[1]), this);
				newName = textToColor(cmdArgs[2]);
			}
			if (!Utils.isOwner(s, p)) {
				doesNotOwn(p);
				return true;
			}
			if (newName.length() > 15) {
				p.sendMessage("[NBSHOP] Name is to long!");
				return true;
			}
			if(Utils.isCurrentNameInUse(newName, this)){
				p.sendMessage("[NBSHOP] That name is already taken!");
				return true;
			}else{
				s.name = newName;
				s.updateName();
				p.sendMessage("[NBSHOP] Shop renamed to " + newName);
				return true;
			} 
		}else if (cmdArgs[0].equalsIgnoreCase("sell")) {
			int ID = 0;
			int price = 0;
			boolean sale = true;
			Shop s = null;
			if (cmdArgs.length == 2 && Utils.isInShop(this, p)) {
				s = Utils.getShopStandingIn(p, this);
				ID = s.id;
				price = Integer.parseInt(cmdArgs[1]);
			} else if (cmdArgs.length == 3) {
				ID = Integer.parseInt(cmdArgs[1]);
				s = Utils.getShopByID(ID, this);
				price = Integer.parseInt(cmdArgs[2]);
			} else if (cmdArgs.length == 1 && Utils.isInShop(this, p)) {
				s = Utils.getShopStandingIn(p, this);
				ID = s.id;
				sale = false;
			} else if (!Utils.isInShop(this, p) && cmdArgs.length == 2) {
				ID = Integer.parseInt(cmdArgs[1]);
				sale = false;
				p.sendMessage("[NBSHOP] You arn't in a shop.");
				return true;
			} else {
				p.sendMessage("[NBSHOP] It's dead Jim, please try again");
			}

			if (Utils.isOwner(s, p)) {
				if (s.id == ID) {
					s.price = price;
					s.forSale = sale;
					s.updateStatus();
				}
				if (sale)p.sendMessage("[NBSHOP] Shop for sale for " + price);
				else p.sendMessage("[NBSHOP] Shop is not for sale");
			}else{
				doesNotOwn(p);
			}
			return true;
		}else if (cmdArgs[0].equalsIgnoreCase("id")) {
			Shop s = null;
			if (cmdArgs.length == 1 && Utils.isInShop(this, p)) {
				s = Utils.getShopStandingIn(p, this);
				p.sendMessage(ChatColor.BLACK + "[" + ChatColor.YELLOW
						+ "NBSHOP" + ChatColor.BLACK + "]"
						+ ChatColor.WHITE + "Shop id = " + s.id);
			} else if(cmdArgs.length == 2 && Utils.isInShop(this, p)){
				s = Utils.getShopByID(Integer.parseInt(cmdArgs[1]), this);
				p.sendMessage(ChatColor.BLACK + "["  + ChatColor.YELLOW
						+ "NBSHOP" + ChatColor.BLACK + "]"
						+ ChatColor.WHITE + "Shop id = " + s.id);
			}else{
				p.sendMessage("[NBSHOP] You arn't in a shop");
			}
			return true;
		}else if (cmdArgs[0].equalsIgnoreCase("info")) {
			Shop s = null;
			if (cmdArgs.length == 1 && Utils.isInShop(this, p)) {
				s = Utils.getShopStandingIn(p, this);
			} else {
				s = Utils.getShopByID(Integer.parseInt(cmdArgs[1]),this);
			}
			shopInfo(p,s);
			return true;
		}else if(cmdArgs[0].equalsIgnoreCase("list")){
			try{
				String name = cmdArgs[1];
				String message = "[NBSHOP] Shops owned by " + name  + ":\n";
				for(Shop s : this.shops){
					if(s.owner.equalsIgnoreCase(name))message += "   +"+s.name +"\n";
				}
				p.sendMessage(message);
			}catch(Exception e){
				p.sendMessage("[NBSHOP] Correct Formate: /shop list <player_name>");
			}
			return true;
		}else if(cmdArgs[0].equalsIgnoreCase("tp")){
			String name = cmdArgs[1];
			Shop shop = Utils.getShopByName(name, this);
			if(shop == null){
				p.sendMessage("[NBSHOP] That shop doesn't exist");
			}
			else{
				int x =Integer.parseInt(shop.signLoc[0]);
				int y =Integer.parseInt(shop.signLoc[1]);
				int z =Integer.parseInt(shop.signLoc[2]);
				p.teleport(new Location(Bukkit.getWorld(WORLD_NAME), x,y,z));
				p.sendMessage("[NBSHOP] Welcome to " + name);
			}
			return true;
		}else if (cmdArgs[0].equalsIgnoreCase("setOwner")) {
			if (!p.isOp()) {
				return true;
			}
			String owner;
			int shop;
			if (cmdArgs.length == 2 && Utils.isInShop(this, p)) {
				Shop s = Utils.getShopStandingIn(p, this);
				shop = s.id;
				owner = cmdArgs[1];
			} else {
				shop = Integer.parseInt(cmdArgs[1]);
				owner = cmdArgs[2];
			}
			Shop shopObject = Utils.getShopByID(shop, this);
			shopObject.owner = owner;
			shopObject.updateStatus();
			p.sendMessage("[NBSHOP] Owner changed to "+owner);
			return true;
		}else if (cmdArgs[0].equalsIgnoreCase("delete")) {
			if (!p.isOp()) {
				return true;
			}
			int shop;
			if (cmdArgs.length == 2 && Utils.isInShop(this, p)) {
				Shop s = Utils.getShopStandingIn(p, this);
				shop = s.id;
			} else {
				shop = Integer.parseInt(cmdArgs[1]);
			}
			Shop shopObject = Utils.getShopByID(shop, this);
			shopObject.delete(this);
			p.sendMessage("[NBSHOP] Shop deleted");
			return true;
		}else if(cmdArgs[0].equalsIgnoreCase("reloadSigns")){
			if(!p.isOp()){
				return true;
			}
			World world = Bukkit.getWorld(WORLD_NAME);
			for(Shop i : this.shops){
				p.sendMessage("[NBSHOP] "+String.valueOf(i.signLoc == null));
				int x =Integer.parseInt(i.signLoc[0]);
				int y =Integer.parseInt(i.signLoc[1]);
				int z =Integer.parseInt(i.signLoc[2]);
				Sign sign = (Sign) world
						.getBlockAt(x,y,z).getState();
				i.sign = sign;
				i.updateName();
				i.updateStatus();
			}	
			p.sendMessage("[NBSHOP] Signs updated");
			return true;
		}else if(cmdArgs[0].equalsIgnoreCase("reloadSign")){
			if(!p.isOp()){
				return true;
			}

			int shop;
			if (cmdArgs.length == 1 && Utils.isInShop(this, p)) {
				Shop s = Utils.getShopStandingIn(p, this);
				shop = s.id;
			} else {
				shop = Integer.parseInt(cmdArgs[1]);
			}
			Shop shopObject = Utils.getShopByID(shop, this);
			shopObject.updateStatus();
			shopObject.updateName();
			p.sendMessage("[NBSHOP] Signs updated");
			return true;
		}else if (cmdArgs[0].equalsIgnoreCase("addSubOwner")) {
			String newSubOwner;
			int shop;
			if (cmdArgs.length == 2 && Utils.isInShop(this, p)) {
				Shop s = Utils.getShopStandingIn(p, this);
				shop = s.id;
				newSubOwner = cmdArgs[1];
			} else {
				shop = Integer.parseInt(cmdArgs[1]);
				newSubOwner = cmdArgs[2];
			}
			Shop shopObject = Utils.getShopByID(shop, this);
			if(!Utils.isOwner(shopObject, p)) {
				doesNotOwn(p);
				return true;
			}
			if(shopObject.subOwners.contains(newSubOwner)){
				p.sendMessage("[NBSHOP] Sub-owner already on list.");
			}else{
				try{
					shopObject.subOwners.add(newSubOwner);
				}catch(Exception e){
					p.sendMessage("[NBSHOP] Failed to add subowner");
				}
			}
			p.sendMessage("[NBSHOP] New sub-owner added");
			shopObject.updateStatus();
			return true;
		}else if (cmdArgs[0].equalsIgnoreCase("removeSubOwner")) {
			String newSubOwner;
			int shop;
			if (cmdArgs.length == 2 && Utils.isInShop(this, p)) {
				Shop s = Utils.getShopStandingIn(p, this);
				shop = s.id;
				newSubOwner = cmdArgs[1];
			} else {
				shop = Integer.parseInt(cmdArgs[1]);
				newSubOwner = cmdArgs[2];
			}
			Shop shopObject = Utils.getShopByID(shop, this);
			if(!Utils.isOwner(shopObject, p)) {
				doesNotOwn(p);
				return true;
			}
			p.sendMessage("[NBSHOP] Sub-owner removed");
			shopObject.subOwners.remove(newSubOwner);
			shopObject.updateStatus();
			return true;
		}else if (cmdArgs[0].equalsIgnoreCase("clearSubOwners")){
			int shop;
			if (cmdArgs.length == 1 && Utils.isInShop(this, p)) {
				Shop s = Utils.getShopStandingIn(p, this);
				shop = s.id;
			} else {
				shop = Integer.parseInt(cmdArgs[1]);
			}
			Shop shopObject = Utils.getShopByID(shop, this);
			if(!Utils.isOwner(shopObject, p)) {
				doesNotOwn(p);
				return true;
			}
			p.sendMessage("[NBSHOP] All sub-owners removed");
			shopObject.subOwners.clear();
			shopObject.updateStatus();
			return true;
		}else if (cmdArgs[0].equalsIgnoreCase("notForSale")){
			int shop;
			if (cmdArgs.length == 1 && Utils.isInShop(this, p)) {
				Shop s = Utils.getShopStandingIn(p, this);
				shop = s.id;
			} else {
				shop = Integer.parseInt(cmdArgs[1]);
			}
			Shop shopObject = Utils.getShopByID(shop, this);
			if(!Utils.isOwnerOrSubOwner(shopObject, p)) {
				doesNotOwn(p);
				return true;
			}
			shopObject.setSaleState(false);
			shopObject.updateStatus();
			p.sendMessage("[NBSHOP] "+shopObject.name+" is know longer for sale");
			return true;
		}else if(cmdArgs[0].equalsIgnoreCase("relocateSign")){
			if(!p.isOp())return true;
			Shop s = null;
			if (cmdArgs.length == 1 && Utils.isInShop(this, p)) {
				s = Utils.getShopStandingIn(p, this);
			} else {
				s = Utils.getShopByID(Integer.parseInt(cmdArgs[1]),this);
			}
			newSign.put(p, s.id);
			p.sendMessage("[NBSHOP] Right click new sign");
			return true;
		}else if (cmdArgs[0].equalsIgnoreCase("help") || cmdArgs[0].equalsIgnoreCase("?")) {
			shopHelp(p);
			return true;
		}else{
			shopHelp(p);
			return true;
		}
	}
	
	public void shopInfo(Player p,Shop s){
		p.sendMessage( new String[] {
				"========SHOP INFO========",
				"Shop name: " + s.name,
				"Shop ID: " + s.id,
				"For sale: " + String.valueOf(s.forSale),
				"Region name: " + s.region.id,
				"Owner: " + s.owner,
				"SubOwners: " + s.subOwners.toString()});
		if (s.forSale)
			p.sendMessage("Price: " + s.price);
		p.sendMessage("=========================");
	}

	public void shopHelp(Player p){
		String[] cmd = new String[] {
				"=============NeonBacon Shop Help=============",
				ChatColor.GREEN + "/shop sell [Name] <Price>",
				ChatColor.GREEN + "/shop name [Old Name] <New Name>]",
				ChatColor.GREEN + "/shop id [Shop ID]",
				ChatColor.GREEN + "/shop info [Shop ID]",
				ChatColor.GREEN + "/shop list <Player Name>",
				ChatColor.GREEN + "/shop tp <Shop Name>",
				ChatColor.GREEN + "/shop addSubOwner [Shop ID] <Player Name>",
				ChatColor.GREEN + "/shop removeSubOwner [Shop ID] <Player Name>",
				ChatColor.GREEN + "/shop clearSubOwners",
				ChatColor.GREEN + "/shop notForSale"};
		String[] admin = new String[] { "ADMIN COMMANDS:",
				ChatColor.DARK_RED + "/shop set <ID>",
				ChatColor.DARK_RED + "/shop setOwner [Shop Name] <Player Name>",
				ChatColor.DARK_RED + "/shop delete [Shop Name]", 
				ChatColor.DARK_RED + "/shop reloadSigns",
				ChatColor.DARK_RED + "/shop reloadSign",
				ChatColor.DARK_RED + "/shop relocateSign"};
		p.sendMessage(cmd);
		if(p.isOp())p.sendMessage(admin);
		p.sendMessage("=============================================");
	}

	@EventHandler
	public void onBlockBreak(BlockDamageEvent e) {
		boolean canceld = false;
		if (!e.getPlayer().getWorld().getName().equalsIgnoreCase(WORLD_NAME))return;
		if (e.getPlayer().isOp())return;
		for (Shop s : shops) {
			int[] max = new int[] { s.region.getMaximumPoint().getBlockX(), s.region.getMaximumPoint().getBlockY(), s.region.getMaximumPoint().getBlockZ() };
			int[] min = new int[] { s.region.getMinimumPoint().getBlockX(), s.region.getMinimumPoint().getBlockY(), s.region.getMinimumPoint().getBlockZ() };
			for (int x = min[0]; x != max[0] + 1; x++) {
				for (int y = min[1]; y != max[1] + 1; y++) {
					for (int z = min[2]; z != max[2] + 1; z++) {
						if (e.getBlock().getX() == x && e.getBlock().getY() == y && e.getBlock().getZ() == z) {
							if (!Utils.isOwnerOrSubOwner(s, e.getPlayer()) || !isBlockAllowed(e.getBlock().getTypeId())) {
								canceld = true;
								e.setCancelled(canceld);
								return;
							}
						}
					}
				}
			}
		}
	}

	@EventHandler
	public void onWorldSave(WorldSaveEvent e) {
		for (Shop s : shops) {
			try {
				s.save();
			} catch (IOException er) {
				er.printStackTrace();
			}
		}
	}

	@EventHandler
	public void onBlockPlace(BlockPlaceEvent e) {
		boolean canceld = false;
		if (!e.getPlayer().getWorld().getName().equalsIgnoreCase(WORLD_NAME))return;
		if (e.getPlayer().isOp())return;
		for (Shop s : shops) {
			int[] max = new int[] { s.region.getMaximumPoint().getBlockX(), s.region.getMaximumPoint().getBlockY(), s.region.getMaximumPoint().getBlockZ() };
			int[] min = new int[] { s.region.getMinimumPoint().getBlockX(), s.region.getMinimumPoint().getBlockY(), s.region.getMinimumPoint().getBlockZ() };
			for (int x = min[0]; x != max[0] + 1; x++) {
				for (int y = min[1]; y != max[1] + 1; y++) {
					for (int z = min[2]; z != max[2] + 1; z++) {
						if (e.getBlock().getX() == x && e.getBlock().getY() == y && e.getBlock().getZ() == z) {
							if (!Utils.isOwnerOrSubOwner(s, e.getPlayer()) || !isBlockAllowed(e.getBlock().getTypeId())) {
								canceld = true;
								e.setCancelled(canceld);
								return;
							}
						}
					}
				}
			}
		}
	}


	@EventHandler
	public void onEntityDamage(EntityDamageEvent e){
		if(e.getEntity() instanceof Player){
			Player p = (Player)e.getEntity();
			if(p.getLocation().getWorld().getName().equalsIgnoreCase(WORLD_NAME)){
				e.setCancelled(true);
			}
		}
	}
	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent e) {
		if (!e.getPlayer().getWorld().getName().equalsIgnoreCase(WORLD_NAME))return;
		if (e.getAction() == Action.RIGHT_CLICK_BLOCK && e.getClickedBlock().getType() == Material.WALL_SIGN) {
			if (waiting.containsKey(e.getPlayer())) {
				ShopRegion r = waiting.get(e.getPlayer());
				String shop_name = r.id;
				Shop shop = new Shop(r, e.getPlayer().getName(), shop_name,	Integer.parseInt(shop_name.split("_")[1]));
				shops.add(shop);
				Block b = e.getClickedBlock();
				Sign s = (Sign) b.getState();
				shop.setSign(s);
				s.setLine(0, "[NBSHOP]");
				s.setLine(1, shop.name);
				s.setLine(2, e.getPlayer().getName());
				s.update();
				e.getPlayer().sendMessage("[NBSHOP] Sign selected");
				waiting.remove(e.getPlayer());
				return;
			}else if (newSign.containsKey(e.getPlayer())) {
				Shop shop = Utils.getShopByID(newSign.get(e.getPlayer()),this);
				Block b = e.getClickedBlock();
				shop.clearSign();
				Sign s = (Sign) b.getState();
				shop.setSign(s);
				String[] newSignLoc = {Integer.toString(s.getX()), Integer.toString(s.getY()), Integer.toString(s.getZ())};
				shop.signLoc = newSignLoc;
				shop.clearSign();
				s.setLine(0, "[NBSHOP]");
				shop.updateStatus();
				e.getPlayer().sendMessage("[NBSHOP] New sign set");
				newSign.remove(e.getPlayer());
				return;
			}
			Block b = e.getClickedBlock();
			Sign s = (Sign) b.getState();
			String[] text = s.getLines();
			if (text[0].equalsIgnoreCase("[NBSHOP]")) {
				for (Shop shop : shops) {
					if (shop.name.equalsIgnoreCase(text[1])) {
						if (shop.saleSate()) {
							double price = shop.price;
							try {
								if (Economy.hasEnough(e.getPlayer().getName(),price)) {
									Economy.subtract(e.getPlayer().getName(),price);
									e.getPlayer().sendMessage("[NBSHOP] Your balance is now: " + Economy.getMoney(e.getPlayer().getName()));
									e.getPlayer().sendMessage("[NBSHOP] You now own this shop!");
									prevOwner = shop.owner;
									shop.name = "shop_" + shop.id;
									shop.owner = e.getPlayer().getName();
									try{
										shop.subOwners.clear();
									}catch(Exception ex){
										e.getPlayer().sendMessage("[NBSHOP] Failed to clear subowner");
									}
									if(Bukkit.getPlayer(prevOwner).isOnline()){
										Bukkit.getPlayer(prevOwner).sendMessage("[NBSHOP] Your shop " + shop.name + " has been sold!");
										Bukkit.getPlayer(prevOwner).sendMessage("[NBSHOP] Your balance is now: " + Economy.getMoney(prevOwner));
									}
									shop.setSaleState(false);
									Economy.add(prevOwner, shop.price);
									shop.price = 0;
									shop.updateName();
									shop.updateStatus();
									e.getPlayer().sendMessage("[NBSHOP] Do \"/shop ?\" for help with your new shop");
								} else {
									e.getPlayer().sendMessage("[NBSHOP] You dont have enough money to do that!");
								}
							} catch (UserDoesNotExistException e1) {
								e1.printStackTrace();
							} catch (NoLoanPermittedException e1) {
								e1.printStackTrace();
							}
						}
					}
				}
			}
		}
	}

	public static String textToColor(String text)
	{
		text = text.replaceAll("&0", ChatColor.BLACK+"");
		text = text.replaceAll("&1", ChatColor.DARK_BLUE+"");
		text = text.replaceAll("&2", ChatColor.DARK_GREEN+"");
		text = text.replaceAll("&3", ChatColor.DARK_AQUA+"");
		text = text.replaceAll("&4", ChatColor.DARK_RED+"");
		text = text.replaceAll("&5", ChatColor.DARK_PURPLE+"");
		text = text.replaceAll("&6", ChatColor.GOLD+"");
		text = text.replaceAll("&7", ChatColor.GRAY+"");
		text = text.replaceAll("&8", ChatColor.DARK_GRAY+"");
		text = text.replaceAll("&9", ChatColor.BLUE+"");
		text = text.replaceAll("&A", ChatColor.GREEN+"");
		text = text.replaceAll("&B", ChatColor.AQUA+"");
		text = text.replaceAll("&C", ChatColor.RED+"");
		text = text.replaceAll("&D", ChatColor.LIGHT_PURPLE+"");
		text = text.replaceAll("&E", ChatColor.YELLOW+"");
		text = text.replaceAll("&F", ChatColor.WHITE+"");
		text = text.replaceAll("&a", ChatColor.GREEN+"");
		text = text.replaceAll("&b", ChatColor.AQUA+"");
		text = text.replaceAll("&c", ChatColor.RED+"");
		text = text.replaceAll("&d", ChatColor.LIGHT_PURPLE+"");
		text = text.replaceAll("&e", ChatColor.YELLOW+"");
		text = text.replaceAll("&u", ChatColor.UNDERLINE+"");
		text = text.replaceAll("&U", ChatColor.UNDERLINE+"");
		text = text.replaceAll("&n", ChatColor.BOLD+"");
		text = text.replaceAll("&N", ChatColor.BOLD+"");
		text = text.replaceAll("&o", ChatColor.ITALIC+"");
		text = text.replaceAll("&O", ChatColor.ITALIC+"");
		text = text.replaceAll("&i", ChatColor.ITALIC+"");
		text = text.replaceAll("&I", ChatColor.ITALIC+"");
		text = text.replaceAll("&k", ChatColor.MAGIC+"");
		text = text.replaceAll("&K", ChatColor.MAGIC+"");
		return text;
	}

	private void doesNotOwn(Player p){
		p.sendMessage("[NBSHOP] You don't own this shop.");
	}
	
	private boolean isBlockAllowed(int blockID){
		return allowedBlockIDs.contains(blockID);
	}
}

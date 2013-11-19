package Teken.nbshops;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.block.Sign;

public class Shop {
	ShopRegion region;
	String owner;
    List<String> subOwners;
	String name;
	int id;
	Sign sign;
	int price;
	boolean forSale;
	String[] signLoc;

	public Shop(ShopRegion region, String owner, String name, int id) {
		this.region = region;
		this.owner = owner;
		this.name = name;
		this.id = id;  
		subOwners = new ArrayList<String>();
	}

	public void setSign(Sign sign) {
		this.sign = sign;
        try {
            save();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
	
	public boolean saleSate() {
		return forSale;
	}
	
	public void setSaleState(boolean q){
		forSale = q;
	}

	public void updateName() {
		sign.setLine(1, name);
		sign.update();
        try {
            save();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

	public void updateStatus() {
		if (forSale) {
			sign.setLine(3, "FOR SALE");
			sign.setLine(2, "Price:" + price);
		} else {
			sign.setLine(2, owner);
			sign.setLine(3, "");
		}
		sign.update();
        try {
            save();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

	public void clearSign() {
		sign.setLine(0, "");
		sign.setLine(1, "");
		sign.setLine(2, "");
		sign.setLine(3, "");
		sign.update();
	}
	
	public Sign getSign(Main m){
		Sign sign = null;
		try{
			sign = (Sign) m.getServer().getWorld(m.WORLD_NAME).getBlockAt(Integer.parseInt(signLoc[0]),Integer.parseInt(signLoc[1]),Integer.parseInt(signLoc[2])).getState();
		}catch(Exception e){}
		return sign;
	}

	public void save() throws IOException{
	File file = new File("plugins/nbshop/" + this.id + ".shop");
	BufferedWriter writer = new BufferedWriter(new FileWriter(file));
	writer.write(name);
	writer.newLine();
	writer.write(owner);
	writer.newLine();
	writer.write(id + "");
	writer.newLine();
	writer.write(region.id);
	writer.newLine();
	writer.write(sign.getX() + ":" + sign.getY() + ":" + sign.getZ());
	writer.newLine();
	writer.write(String.valueOf(forSale));
	writer.newLine();
	writer.write(region.getMinimumPoint().getBlockX() + ":" + 
	region.getMinimumPoint().getBlockY() + ":" + region.getMinimumPoint().getBlockZ());
	writer.newLine();
	writer.write(region.getMaximumPoint().getBlockX() + ":" + 
	region.getMaximumPoint().getBlockY() + ":" + region.getMaximumPoint().getBlockZ());
	writer.newLine();
	writer.write(String.valueOf(price));
	writer.newLine();
	for(String s: subOwners){
		writer.write(s+":");
	}
	writer.close();
}

	public void delete(Main main) {
		main.shops.remove(this);
		clearSign();
		File file = new File("plugins/nbshop/" + this.id + ".shop");
		file.delete();
	}
}

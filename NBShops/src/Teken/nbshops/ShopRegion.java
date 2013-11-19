package Teken.nbshops;

import com.sk89q.worldedit.Vector;

public class ShopRegion {
Vector max = null;
Vector min = null;
String id = "";

public ShopRegion(Vector vector, Vector vector2, String id){
	this.max = (Vector) vector;
	this.min = (Vector) vector2;
	this.id = id;
}

public Vector getMaximumPoint() {
	return max;
}
public Vector getMinimumPoint() {
	return min;
}
}

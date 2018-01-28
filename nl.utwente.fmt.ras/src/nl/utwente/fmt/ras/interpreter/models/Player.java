package nl.utwente.fmt.ras.interpreter.models;

import java.util.ArrayList;
import java.util.List;

public class Player {
	
	private ArrayList<Location> locations;
	private String name;

	public Player() {
		locations = new ArrayList<>();
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void addLocation(Location loc) {
		this.locations.add(loc);
	}

	public List<Location> getLocations() {
		return this.locations;
	}

	@Override
	public String toString() {
		StringBuffer result = new StringBuffer();
		result.append(this.getName());
		result.append(" ( ");
		for(Location l : locations) {
			result.append(l.toString() + " ");
		}
		result.append(") ");
		return result.toString();
	}
}

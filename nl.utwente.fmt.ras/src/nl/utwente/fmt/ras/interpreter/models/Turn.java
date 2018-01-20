package nl.utwente.fmt.ras.interpreter.models;

import nl.utwente.fmt.ras.ras.Card;

public class Turn {
	
	public Player player;
	public Card card;
	public String command;
	public Location from;
	public Location to;

	@Override
	public String toString() {
		return player.getName() + " " + command + " " + card.getName() + " from " + from.getLoc().getName() + " to " + to.getLoc().getName();
	}
}

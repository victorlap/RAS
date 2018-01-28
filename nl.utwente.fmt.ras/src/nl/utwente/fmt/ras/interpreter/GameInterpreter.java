package nl.utwente.fmt.ras.interpreter;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.eclipse.emf.common.util.EList;

import nl.utwente.fmt.ras.interpreter.models.Location;
import nl.utwente.fmt.ras.interpreter.models.Player;
import nl.utwente.fmt.ras.interpreter.models.Turn;
import nl.utwente.fmt.ras.ras.Card;
import nl.utwente.fmt.ras.ras.CardLocation;
import nl.utwente.fmt.ras.ras.Expression;
import nl.utwente.fmt.ras.ras.Game;
import nl.utwente.fmt.ras.ras.InitialLocationState;
import nl.utwente.fmt.ras.ras.LocationType;
import nl.utwente.fmt.ras.ras.PlayerExpression;
import nl.utwente.fmt.ras.ras.PlayerKeyword;
import nl.utwente.fmt.ras.ras.RuleExpression;
import nl.utwente.fmt.ras.ras.RuleKeyword;
import nl.utwente.fmt.ras.ras.SideEffect;
import nl.utwente.fmt.ras.ras.TurnExpression;
import nl.utwente.fmt.ras.ras.TurnKeyword;

public class GameInterpreter {

	private Game game;
	private Player[] players;
	private ArrayList<Location> sharedLocations = new ArrayList<>();
	private RuleEngine engine;

	private ArrayList<Turn> turns = new ArrayList<>();
	private int current = 0;
	private Turn currentTurn = null;
	private int turnDir = 1;
	private int turnAdd = 1;
	private int nextPlayerTakes = 0;

	public void setup(Game game) {
		this.game = game;
		this.engine = new RuleEngine(game, this);

		setupPlayers();
		setupLocations();
		setupCards();
	}

	public void startGame() {
		Scanner in = new Scanner(System.in);
		display();
		printInstructions();
		printHelp();
		while (in.hasNext()) {
			String line = in.nextLine();

			if (line.toLowerCase().equals("exit")) {
				break;
			}

			handleAction(line);

			printInstructions();
		}

		in.close();
		print("Goodbye! Thanks for playing!");
	}

	public void handleAction(String line) {
		String[] split = line.split(" ");
		if (split.length < 1) {
			err("No command found!");
		}

		setCurrentTurn(split[0]);

		switch (split[0].toLowerCase()) {
		case "display":
			display();
			break;
		case "play":
			if (split.length == 4) {
				doMove(split[1], split[2], split[3]);
			} else {
				err("play expects 3 parameters, " + (split.length - 1) + " given");
			}
			break;
		case "draw":
			if (split.length == 3) {
				draw(split[1], split[2]);
			} else {
				err("draw expects 2 parameters, " + (split.length - 1) + " given");
			}
			break;
		case "action":
			if (split.length == 5) {
				action(split[1], split[2], split[3], split[4]);
			} else {
				err("action expects 4 parameters, " + (split.length - 1) + " given");
			}
			break;
		case "list":
			listTurns();
			break;
		case "help":
			printHelp();
			break;
		default:
			err("Command " + split[0] + " not found");
			break;
		}
	}

	public Turn prevTurn() {
		if (turns.size() > 1) {
			// Last item on the stack is the prev turn
			return turns.get(turns.size() - 1);
		}
		return null;
	}

	public Turn curTurn() {
		return currentTurn;
	}

	public Player parsePlayer(String player) {
		try {
			int playerIndex = Integer.valueOf(player) - 1;
			if (playerIndex < 0 || playerIndex >= players.length) {
				return null;
			}
			return players[playerIndex];
		} catch (NumberFormatException e) {
			return null;
		}
	}

	public Location parseLocation(String location, Player p) {
		for (Location l : this.sharedLocations) {
			if (l.is(location)) {
				return l;
			}
		}
		for (Location l : p.getLocations()) {
			if (l.is(location)) {
				return l;
			}
		}
		return null;
	}

	public Location parseLocation(String location) {
		return parseLocation(location, players[current]);
	}

	public Card parseCard(Location l, String card) {
		for (Card c : l.getCards()) {
			if (c.getName().toLowerCase().equals(card.toLowerCase())) {
				return c;
			}
		}
		return null;
	}

	private static void print(String msg) {
		System.out.println(msg);
	}

	private static void err(String msg) {
		System.err.println(msg);
	}

	private void listTurns() {
		if (turns.size() == 0) {
			print("No turns have happened yet");
		}
		for (Turn t : turns) {
			print(t.toString());
		}
	}

	private void setCurrentTurn(String line) {
		currentTurn = new Turn();
		currentTurn.command = line;
		currentTurn.player = players[current];
	}

	private void display() {
		StringBuilder sb = new StringBuilder();
		for (Player p : players) {
			sb.append(p.getName());
			sb.append(" vs ");
		}
		print(sb.substring(0, sb.lastIndexOf(" vs ")));

		for (Player p : players) {
			print(p.toString());
		}
		for (Location l : sharedLocations) {
			print(l.toString());
		}
	}

	private void printInstructions() {
		print(players[current].getName() + "'s turn:");
		if (nextPlayerTakes != 0) {
			print("You need to draw " + nextPlayerTakes + " cards");
		}
	}

	private void printHelp() {
		print("Possible commands: display, list, help, exit, play [FromLocation] [ToLocation] [Card], draw [FromLocation] [ToLocation], action [Player] [FromLocation] [ToLocation] [Card]");
	}

	private void action(String p, String from, String to, String c) {
		Player player = parsePlayer(p);
		if (player == null) {
			err("Player " + p + " is not found!");
			return;
		}

		Location fromLocation = parseLocation(from, player);
		Location toLocation = parseLocation(to, player);
		if (fromLocation == null) {
			err("Location " + from + " is not found!");
			return;
		}
		if (toLocation == null) {
			err("Location " + to + " is not found!");
			return;
		}

		Card card = parseCard(fromLocation, c);
		if (card == null) {
			err("Card " + c + " is not found!");
			return;
		}
		if (fromLocation.equals(toLocation)) {
			err("FromLocation and ToLocation cannot be the same!");
			return;
		}
		err("Not implemented");
	}

	private void draw(String from, String to) {
		Location fromLocation = parseLocation(from);
		Location toLocation = parseLocation(to);
		if (fromLocation == null) {
			err("Location " + from + " is not found!");
			return;
		}
		if (toLocation == null) {
			err("Location " + to + " is not found!");
			return;
		}
		if (!engine.canDraw(fromLocation, toLocation)) {
			err("You cannot draw from " + fromLocation.getLoc().getName() + " to " + toLocation.getLoc().getName()
					+ "!");
			return;
		}
		if (fromLocation.equals(toLocation)) {
			err("FromLocation and ToLocation cannot be the same!");
			return;
		}
		if (!fromLocation.hasCards()) {
			err("" + fromLocation.getLoc().getName() + "  has no cards!");
			return;
		}

		Card c = fromLocation.takeCard();
		fromLocation.removeCard(c);
		toLocation.addCard(c);

		currentTurn.card = c;
		currentTurn.from = fromLocation;
		currentTurn.to = toLocation;

		print("You got a " + c.getName());

		if (nextPlayerTakes != 0) {
			nextPlayerTakes--;
		} else {
			nextPlayer();
		}
	}

	private void doMove(String from, String to, String c) {
		Location fromLocation = parseLocation(from);
		Location toLocation = parseLocation(to);
		if (fromLocation == null) {
			err("Location " + from + " is not found!");
			return;
		}
		if (toLocation == null) {
			err("Location " + to + " is not found!");
			return;
		}
		Card card = parseCard(fromLocation, c);
		if (card == null) {
			err("Card " + c + " is not found!");
			return;
		}
		if (fromLocation.equals(toLocation)) {
			err("FromLocation and ToLocation cannot be the same!");
			return;
		}
		if (!engine.canPlay(fromLocation, toLocation, card)) {
			err("You cannot play " + card.getName() + " from " + fromLocation.getLoc().getName() + " to "
					+ toLocation.getLoc().getName() + "!");
			return;
		}
		if (nextPlayerTakes != 0) {
			err("You should first draw " + nextPlayerTakes + " cards!");
			return;
		}

		fromLocation.removeCard(card);
		toLocation.addCard(card);

		handleSideEffects(fromLocation, toLocation, card);

		currentTurn.card = card;
		currentTurn.from = fromLocation;
		currentTurn.to = toLocation;

		nextPlayer();
	}

	private void handleSideEffects(Location from, Location to, Card card) {
		for (SideEffect effect : card.getSideeffects()) {
			List<Expression> exs = effect.getActions();
			for (Expression ex : exs) {
				if (ex instanceof TurnExpression) {
					TurnExpression turnex = (TurnExpression) ex;
					if (turnex.getKeyword().equals(TurnKeyword.REPEAT)) {
						this.turnAdd = 0;
					} else if (turnex.getKeyword().equals(TurnKeyword.REVERSE)) {
						this.turnDir *= -1;
					} else if (turnex.getKeyword().equals(TurnKeyword.SKIP)) {
						this.turnAdd = 2;
					} else if (turnex.getKeyword().equals(TurnKeyword.NEXT)) {
						if (turnex.getExpression() instanceof PlayerExpression) {
							PlayerExpression playerex = (PlayerExpression) turnex.getExpression();
							if (playerex.getKeyword().equals(PlayerKeyword.TAKE)) {
								this.nextPlayerTakes = playerex.getValue();
							} else {
								System.err.println("Cannot interpret sideeffects" + playerex.toString());
							}
						} else if (turnex.getExpression() instanceof RuleExpression) {
							RuleExpression ruleex = (RuleExpression) turnex.getExpression();
							if (ruleex.getKeyword().equals(RuleKeyword.REMOVE)) {
								engine.temporarilyRemove(ruleex.getRule(), 2);
							} else {
								System.err.println("Cannot interpret sideeffects" + ruleex.toString());
							}
						} else {
							System.err.println("Cannot interpret sideeffects" + turnex.getExpression().toString());
						}
					} else {
						System.err.println("Cannot interpret sideeffects" + turnex.toString());
					}
				} else {
					System.err.println("Cannot interpret sideeffects" + ex.toString());
				}
			}
		}
	}

	private void nextPlayer() {
		if (turnAdd != 1) {
			print("Skipping " + (turnAdd - 1) + " player(s)");
		}

		current = (current + players.length + (turnAdd * turnDir)) % players.length;
		turnAdd = 1;
		turns.add(currentTurn);

		engine.next();
	}

	private void setupPlayers() {
		List<nl.utwente.fmt.ras.ras.Player> gamePlayers = game.getSetup().getPlayers();

		int numPlayers = gamePlayers.size();
		players = new Player[numPlayers];

		for (int i = 0; i < players.length; i++) {
			Player p = new Player();
			p.setName(gamePlayers.get(i).getName());
			
			setupPlayerLocations(p);
			
			addPlayerCardsToLocation(gamePlayers.get(i), p);

			players[i] = p;
		}
	}

	private void setupPlayerLocations(Player player) {
		for (CardLocation loc : game.getLocations()) {
			if (loc.getType().equals(LocationType.INDIVIDUAL)) {
				player.addLocation(new Location(loc));
			}
		}
	}

	private void setupLocations() {
		for (CardLocation loc : game.getLocations()) {
			if (loc.getType().equals(LocationType.SHARED)) {
				this.sharedLocations.add(new Location(loc));
			}
		}
	}
	
	private void addPlayerCardsToLocation(nl.utwente.fmt.ras.ras.Player gamePlayer, Player p) {
		for (InitialLocationState s : gamePlayer.getCards()) {
			for (Location l : p.getLocations()) {
				if (l.getLoc().equals(s.getLoc()) && !l.hasCards()) {
					l.addCards(s.getCards());
					l.shuffle();
					return;
				}
			}
			err("Could not add cards for player " + p.getName() + " : " + s.getLoc().getName());
		}
	}

	private void addCardsToLocation(CardLocation loc, EList<Card> cards) {
		if (loc.getType().equals(LocationType.SHARED)) {
			for (Location l : this.sharedLocations) {
				if (l.getLoc().equals(loc)) {
					l.addCards(cards);
					l.shuffle();
					return;
				}
			}
		}
		err("Could not add cards: " + loc.getName());
	}

	private void setupCards() {
		for (InitialLocationState s : game.getSetup().getCards()) {

			if (s.getLoc().getType().equals(LocationType.INDIVIDUAL)) {
				err("Individual location should be added to a player, not globally! Skipping...");
				continue;
			}

			this.addCardsToLocation(s.getLoc(), s.getCards());
		}
	}
}

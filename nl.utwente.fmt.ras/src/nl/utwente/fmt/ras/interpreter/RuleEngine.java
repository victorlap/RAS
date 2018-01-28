package nl.utwente.fmt.ras.interpreter;

import java.util.ArrayList;
import java.util.List;

import nl.utwente.fmt.ras.interpreter.models.Location;
import nl.utwente.fmt.ras.interpreter.models.RemovedRule;
import nl.utwente.fmt.ras.interpreter.models.Turn;
import nl.utwente.fmt.ras.ras.Card;
import nl.utwente.fmt.ras.ras.CardProperty;
import nl.utwente.fmt.ras.ras.CardPropertyExpression;
import nl.utwente.fmt.ras.ras.CardRule;
import nl.utwente.fmt.ras.ras.DrawExpression;
import nl.utwente.fmt.ras.ras.DrawPlayKeyword;
import nl.utwente.fmt.ras.ras.Expression;
import nl.utwente.fmt.ras.ras.ExpressionBlock;
import nl.utwente.fmt.ras.ras.ExpressionKeyword;
import nl.utwente.fmt.ras.ras.Game;
import nl.utwente.fmt.ras.ras.LocationCardExpression;
import nl.utwente.fmt.ras.ras.LocationExpression;
import nl.utwente.fmt.ras.ras.PlayExpression;
import nl.utwente.fmt.ras.ras.TurnExpression;
import nl.utwente.fmt.ras.ras.TurnKeyword;
import nl.utwente.fmt.ras.ras.ValueExpression;


public class RuleEngine {

	private GameInterpreter interpreter;
	private List<CardRule> rules = new ArrayList<>();
	private List<RemovedRule> removedRules = new ArrayList<>();

	public RuleEngine(Game game, GameInterpreter interpreter) {
		this.interpreter = interpreter;

		rules.addAll(game.getRules());
	}

	public void temporarilyRemove(CardRule rule, int d) {
		RemovedRule r = new RemovedRule();
		r.duration = d;
		r.rule = rule;

		removedRules.add(r);
	}

	public void next() {
		for (int i = removedRules.size()-1; i >= 0; i--) {
			removedRules.get(i).duration--;

			if (removedRules.get(i).duration == 0) {
				removedRules.remove(removedRules.get(i));
			}
		}
	}

	public boolean canDraw(Location from, Location to) {
		for (CardRule r : getRules()) {
			for (ExpressionBlock ex : r.getRequirements()) {
				if (ex.getLeft() instanceof DrawExpression && ex.getRight() instanceof LocationExpression) {
					boolean result = evaluateDrawExpression(ex, from, to);
					if (result == false) {
						return false;
					}
				}
			}
		}
		return true;
	}

	public boolean canPlay(Location from, Location to, Card card) {
		for (CardRule r : getRules()) {
			for (ExpressionBlock ex : r.getRequirements()) {
				boolean result = evaluateExpression(ex, from, to, card);

				if (result == false) {
					return false;
				}
			}
		}
		return true;
	}
	
	private List<CardRule> getRules() {
		List<CardRule> list = new ArrayList<CardRule>();
		list.addAll(rules);

		for (RemovedRule t : removedRules) {
			if (list.contains(t.rule)) {
				list.remove(t.rule);
			}
		}
		
		return list;
	}

	private boolean evaluateExpression(ExpressionBlock ex, Location from, Location to, Card card) {
		Expression left = ex.getLeft();
		Expression right = ex.getRight();
		ExpressionKeyword key = ex.getKeyword();
		ExpressionBlock or = ex.getOr();

		boolean result = true;

		if (left instanceof PlayExpression && right instanceof LocationExpression) {
			result = evaluatePlayExpression(ex, from, to);
		} else if (left instanceof CardPropertyExpression && key.equals(ExpressionKeyword.MATCHES)) {
			result = evaluateCardMatchesExpression(ex, from, to, card);
		} else if (left instanceof DrawExpression && right instanceof LocationExpression) {
			// Silently fail because this rule is only evaluated in canDraw()
		} else {
			System.err.println("Cannot interpret expressionblock" + left.toString() + " " + key.toString() + " "
					+ right.toString());
		}

		if (or == null) {
			return result;
		} else {
			return result || evaluateExpression(or, from, to, card);
		}
	}

	private boolean evaluateCardMatchesExpression(ExpressionBlock ex, Location from, Location to, Card card) {
		CardPropertyExpression left = (CardPropertyExpression) ex.getLeft();
		int leftvalue = -1;
		int rightvalue = -1;

		for (CardProperty cp : card.getProperties()) {
			if (cp.getType().equals(left.getProperty())) {
				leftvalue = cp.getValue();
			}
		}

		if (ex.getRight() instanceof TurnExpression) {
			TurnExpression right = (TurnExpression) ex.getRight();
			Turn turn = null;
			if (right.getKeyword().equals(TurnKeyword.PREV)) {
				turn = interpreter.prevTurn();
			}
			if (turn == null) {
				return true;
			}
			for (CardProperty cp : turn.card.getProperties()) {
				if (cp.getType().equals(left.getProperty())) {
					rightvalue = cp.getValue();
				}
			}
		} else if (ex.getRight() instanceof LocationCardExpression) {
			LocationCardExpression right = (LocationCardExpression) ex.getRight();
			Card locationCard = interpreter.parseLocation(right.getLocation().getName()).takeCard();
			for (CardProperty cp : locationCard.getProperties()) {
				if (cp.getType().equals(left.getProperty())) {
					rightvalue = cp.getValue();
				}
			}
		} else if (ex.getRight() instanceof ValueExpression) {
			ValueExpression right = (ValueExpression) ex.getRight();
			rightvalue = right.getValue();
		} else {
			System.err.println("Cannot parse evaluateCardMatchesExpression");
		}

		return leftvalue != -1 && leftvalue == rightvalue;
	}

	private boolean evaluatePlayExpression(ExpressionBlock ex, Location from, Location to) {
		PlayExpression left = (PlayExpression) ex.getLeft();
		LocationExpression right = (LocationExpression) ex.getRight();
		DrawPlayKeyword keyword = left.getFromOrTo();

		if (keyword.equals(DrawPlayKeyword.FROM)) {
			return from.is(right.getLocation().getName());
		} else if (keyword.equals(DrawPlayKeyword.TO)) {
			return to.is(right.getLocation().getName());
		} else {
			System.err.println("Cannot parse evaluatePlayExpression");
		}

		return false;
	}

	private boolean evaluateDrawExpression(ExpressionBlock ex, Location from, Location to) {
		DrawExpression left = (DrawExpression) ex.getLeft();
		LocationExpression right = (LocationExpression) ex.getRight();
		DrawPlayKeyword keyword = left.getFromOrTo();

		if (keyword.equals(DrawPlayKeyword.FROM)) {
			return from.is(right.getLocation().getName());
		} else if (keyword.equals(DrawPlayKeyword.TO)) {
			return to.is(right.getLocation().getName());
		} else {
			System.err.println("Cannot parse evaluateDrawExpression");
		}

		return false;
	}

}

package nl.utwente.fmt.ras.interpreter.utils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.eclipse.emf.common.util.EList;

public class CollectionUtils {

	private static Random r;

	public static <T> List<T> intersection(List<T> list1, List<T> list2) {
		List<T> list = new ArrayList<T>();

		for (T t : list1) {
			if (list2.contains(t)) {
				list.add(t);
			}
		}

		return list;
	}

	public static <T> T random(EList<T> list) {
		Random rnd = r;

		if (rnd == null)
			r = rnd = new Random();

		return list.get(rnd.nextInt(list.size()));
	}

	public static void shuffle(EList<?> list) {
		Random rnd = r;

		if (rnd == null)
			r = rnd = new Random(); // harmless race.

		for (int i = list.size(); i > 1; i--)
			list.move(i - 1, rnd.nextInt(i));
	}

	public static <T> List<T> union(List<T> list1, List<T> list2) {
		Set<T> set = new HashSet<T>();

		set.addAll(list1);
		set.addAll(list2);

		return new ArrayList<T>(set);
	}
}

package main.java.paladinus;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import main.java.paladinus.explicit.ExplicitEffect;
import main.java.paladinus.explicit.ExplicitOperator;
import main.java.paladinus.state.Operator;

/**
 *
 * @author Robert Mattmueller
 *
 */
public class Preprocessor {

	/**
	 * Remove empty effects to avoid self loops.
	 *
	 * @param ops operators to be processed
	 * @return preprocessed operators
	 */
	public static LinkedHashSet<Operator> preprocessForStrongCyclicPlanning(Set<Operator> ops) {
		// TODO Prefer ops with less uncertainty, if an modified op is equal to another
		// op from ops.
		// TODO Is this preprocessing useful because it leads to biased costs in
		// LAO*-search?
		LinkedHashSet<Operator> remainingOps = new LinkedHashSet<Operator>((int) (ops.size() / 0.75) + 1);
		for (Operator op : ops) {
			ExplicitOperator explicitOp = (ExplicitOperator) op;
			Set<Set<ExplicitEffect>> newChoices = new HashSet<Set<ExplicitEffect>>(
					explicitOp.getNondeterministicEffect());
			for (Set<ExplicitEffect> choice : explicitOp.getNondeterministicEffect()) {
				if (choice.isEmpty()) {
					boolean found = newChoices.remove(choice);
					assert found;
				}
			}
			if (newChoices.size() > 0) {
				if (newChoices.size() < explicitOp.getNondeterministicEffect().size()) {
					explicitOp.setNonDeterministicChoices(newChoices); // At least one empty effect was
					// removed.
					// System.out.println("At least one empty effect was removed");
				}
				remainingOps.add(op); // Keep this operator.
			}
		}
		return remainingOps;
	}

	/**
	 * Remove empty effects to avoid self loops.
	 *
	 * @param ops operators to be processed
	 * @return preprocessed operators
	 */
	public static LinkedHashSet<Operator> preprocessForStrongPlanning(Set<Operator> ops) {
		return preprocessForStrongCyclicPlanning(ops);
	}
	
	/**
	 * Remove operators with empty effects, because such an operator causes a self
	 * loop and can not be part of a strong solution.
	 *
	 * @param ops operators to be processed
	 * @return preprocessed operators
	 */
	public static LinkedHashSet<Operator> preprocessForStrongPlanningOld(Set<Operator> ops) {
		LinkedHashSet<Operator> remainingOps = new LinkedHashSet<Operator>((int) (ops.size() / 0.75) + 1);
		for (Operator op : ops) {
			ExplicitOperator explicitOp = (ExplicitOperator) op;
			boolean removeOperator = false;
			for (Set<ExplicitEffect> choice : explicitOp.getNondeterministicEffect()) {
				if (choice.isEmpty()) {
					removeOperator = true;
					break;
				}
			}
			if (!removeOperator) {
				remainingOps.add(op);
			}
		}
		return remainingOps;
	}
}

package main.java.paladinus.symbolic;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import main.java.javabdd.BDD;
import main.java.paladinus.explicit.ExplicitOperator.OperatorRule;
import main.java.paladinus.problem.Problem;
import main.java.paladinus.util.Pair;

/**
 * Symbolic axiom evaluator to apply axiom rules in case of partial
 * observability.
 *
 * @author Manuela Ortlieb
 */

public class SymbolicAxiomEvaluator {

	/**
	 * BDDManager used with this axiom evaluator
	 */
	public BDDManager BDDManager;

	/**
	 * Symbolic representation of axioms.
	 */
	private BDD axioms;

	/**
	 * Symbolic representation of derived variables.
	 */
	private BDD derivedVars;

	/**
	 * Number of axioms.
	 */
	private int numberOfAxioms;

	/**
	 * Set true for debug information.
	 */
	public final static boolean DEBUG = false;

	/**
	 * Create and initialize a symbolic axiom evaluator.
	 */
	public SymbolicAxiomEvaluator(BDDManager BDDManager, Problem problem) {
		this.BDDManager = BDDManager;
		numberOfAxioms = problem.numAxioms;
		if (numberOfAxioms == 0) {
			return;
		}
		initialize(problem.axioms);
	}

	public SymbolicAxiomEvaluator(BDDManager BDDManager, Set<OperatorRule> axioms) {
		this.BDDManager = BDDManager;
		numberOfAxioms = axioms.size();
		if (numberOfAxioms == 0) {
			return;
		}
		initialize(axioms);
	}

	private void initialize(Set<OperatorRule> setOfAxioms) {
		Map<Pair<Integer, Integer>, Set<Set<Pair<Integer, Integer>>>> derivedVarsToAxioms = new HashMap<Pair<Integer, Integer>, Set<Set<Pair<Integer, Integer>>>>(
				(int) (numberOfAxioms / 0.75) + 1);
		for (OperatorRule axiom : setOfAxioms) {
			if (derivedVarsToAxioms.containsKey(axiom.head)) {
				derivedVarsToAxioms.get(axiom.head).add(axiom.body);
			} else {
				Set<Set<Pair<Integer, Integer>>> newSet = new HashSet<Set<Pair<Integer, Integer>>>();
				newSet.add(axiom.body);
				derivedVarsToAxioms.put(axiom.head, newSet);
			}
		}

		Set<Integer> derivedVariables = new HashSet<Integer>(derivedVarsToAxioms.size());
		// Build symbolic representation of axioms.
		axioms = BDDManager.B.one();
		for (Pair<Integer, Integer> fact : derivedVarsToAxioms.keySet()) {
			derivedVariables.add(fact.first);
			BDD disjunction = BDDManager.B.zero();
			for (Set<Pair<Integer, Integer>> condition : derivedVarsToAxioms.get(fact)) {
				BDD conjunction = BDDManager.B.one();
				for (Pair<Integer, Integer> conditionFact : condition) {
					conjunction = conjunction.and(BDDManager.factBDDs[conditionFact.first][conditionFact.second]);
				}
				disjunction.orWith(conjunction);
			}
			BDD biimp = BDDManager.factBDDs[fact.first][fact.second].biimp(disjunction);
			axioms.andWith(biimp);
			disjunction.free();
		}
		derivedVars = BDDManager.B.one();
		for (int var : derivedVariables) {
			derivedVars = derivedVars.and(BDDManager.stateVarDomains[var].set());
			derivedVars = derivedVars.and(BDDManager.primedStateVarDomains[var].set());
		}
		derivedVarsToAxioms = null;
		derivedVariables = null;
	}

	/**
	 * Evaluate a given belief state BDD.
	 *
	 * @param beliefStateBDD
	 * @return given BDD after applying axiom rules.
	 */
	public BDD evaluate(BDD beliefStateBDD) {
		if (numberOfAxioms == 0) {
			return beliefStateBDD;
		}
		beliefStateBDD = beliefStateBDD.id().exist(derivedVars);
		if (DEBUG) {
			System.out.println("Evaluate...");
			System.out.println(beliefStateBDD);
		}
		beliefStateBDD = beliefStateBDD.and(axioms);
		if (DEBUG) {
			System.out.println("Deduce...");
			System.out.println(beliefStateBDD);
		}
		return beliefStateBDD;
	}
}

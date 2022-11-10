package main.java.paladinus.explicit;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import main.java.paladinus.explicit.ExplicitOperator.OperatorRule;
import main.java.paladinus.problem.Problem;
import main.java.paladinus.util.Pair;

/**
 * Axiom evaluator in the fully observable case. Reimplementation of Fast
 * Downward axiom evaluator.
 *
 * @author Manuela Ortlieb
 */

public class ExplicitAxiomEvaluator {

	protected Problem problem;

	protected AxiomLiteral[][] axiomLiterals;

	protected AxiomRule[] rules;

	protected List<List<NegationAsFailureInfo>> nafInfoByLayer;

	protected int numberOfAxioms;

	public static final boolean DEBUG = false;

	public ExplicitAxiomEvaluator(Problem problem) {
		this(problem, problem.axioms);
	}

	public ExplicitAxiomEvaluator(Problem problem, Set<OperatorRule> axioms) {
		this.problem = problem;
		numberOfAxioms = axioms.size();
		if (numberOfAxioms == 0) {
			return;
		}
		rules = new AxiomRule[numberOfAxioms];
		axiomLiterals = new AxiomLiteral[problem.numStateVars][];
		initialize(axioms);
	}

	// Copy constructor
	public ExplicitAxiomEvaluator(ExplicitAxiomEvaluator oldEvaluator) {
		problem = oldEvaluator.problem;
		axiomLiterals = oldEvaluator.axiomLiterals;
		rules = oldEvaluator.rules;
		nafInfoByLayer = oldEvaluator.nafInfoByLayer;
		numberOfAxioms = oldEvaluator.numberOfAxioms;
	}

	private void initialize(Set<OperatorRule> axioms) {
		// Initialize literals.
		for (int i = 0; i < problem.numStateVars; i++) {
			axiomLiterals[i] = new AxiomLiteral[problem.domainSizes.get(i)];
			for (int j = 0; j < axiomLiterals[i].length; j++) {
				axiomLiterals[i][j] = new AxiomLiteral();
			}
		}

		// Initialize rules.
		int i = 0;
		for (OperatorRule axiom : axioms) {
			Set<Pair<Integer, Integer>> conditions = axiom.body;
			int effVar = axiom.head.first;
			int effVal = axiom.head.second;
			AxiomLiteral effLiteral = axiomLiterals[effVar][effVal];
			rules[i] = new AxiomRule(conditions.size(), effVar, effVal, effLiteral);

			// Cross-reference rules and literals.
			for (Pair<Integer, Integer> condition : conditions) {
				assert axiomLiterals[condition.first][condition.second] != null;
				axiomLiterals[condition.first][condition.second].conditionOf.add(rules[i]);
			}
			i++;
		}

		// Initialize negation as failure information.
		int lastLayer = -1;
		for (int var = 0; var < problem.numStateVars; var++) {
			lastLayer = Math.max(lastLayer, problem.axiomLayer.get(var));
		}
		nafInfoByLayer = new ArrayList<List<NegationAsFailureInfo>>(lastLayer + 1);
		for (int j = 0; j < lastLayer + 1; j++) {
			nafInfoByLayer.add(j, new ArrayList<NegationAsFailureInfo>()); // initialization
		}
		for (int var = 0; var < problem.numStateVars; var++) {
			int layer = problem.axiomLayer.get(var);
			if (layer != -1 && layer != lastLayer) {
				int nafValue = problem.defaultAxiomValues.get(var);
				AxiomLiteral nafLiteral = axiomLiterals[var][nafValue];
				nafInfoByLayer.get(layer).add(new NegationAsFailureInfo(var, nafLiteral));
			}
		}
	}

	public Map<Integer, Integer> evaluate(Map<Integer, Integer> variableValueAssignment) {
		if (numberOfAxioms == 0) {
			return variableValueAssignment;
		}

		if (DEBUG) {
			System.out.println("Evaluate axioms...");
			System.out.println("on state " + variableValueAssignment);
		}
		Queue<AxiomLiteral> queue = new LinkedList<AxiomLiteral>();
		for (int var : variableValueAssignment.keySet()) {
			if (problem.axiomLayer.get(var) != -1) {
				variableValueAssignment.put(var, problem.defaultAxiomValues.get(var));
				if (DEBUG) {
					System.out.println(
							"Set var " + var + " to default axiom value " + problem.defaultAxiomValues.get(var));
				}
			} else {
				if (DEBUG) {
					System.out.println("Enqueueing " + axiomLiterals[var][variableValueAssignment.get(var)]);
				}
				queue.add(axiomLiterals[var][variableValueAssignment.get(var)]);
			}
		}

		for (int i = 0; i < rules.length; i++) {
			rules[i].unsatisfiedCondition = rules[i].numberOfConditions;
			// Note: In a perfect world, trivial axioms would have been
			// compiled away, and we could have the following assertion
			// instead of the following block.
			// assert(rules[i].numberOfConditions != 0);

			if (rules[i].numberOfConditions == 0) {
				deduceAndEnqueue(rules[i], queue, variableValueAssignment);
			}
		}

		for (int layer = 0; layer < nafInfoByLayer.size(); layer++) {
			// Apply Horn rules.
			while (!queue.isEmpty()) {
				AxiomLiteral currentLiteral = queue.poll();
				for (int i = 0; i < currentLiteral.conditionOf.size(); i++) {
					AxiomRule rule = currentLiteral.conditionOf.get(i);
					if (--rule.unsatisfiedCondition == 0) {
						deduceAndEnqueue(rule, queue, variableValueAssignment);
					}
				}
			}

			// Apply negation as failure rules.
			List<NegationAsFailureInfo> nafInfo = nafInfoByLayer.get(layer);
			for (int i = 0; i < nafInfo.size(); i++) {
				int var = nafInfo.get(i).variableNumber;
				if (variableValueAssignment.get(var) == problem.defaultAxiomValues.get(var)) {
					queue.add(nafInfo.get(i).literal);
				}
			}
		}
		if (DEBUG) {
			System.out.println("Resulting state " + variableValueAssignment);
		}
		return variableValueAssignment;
	}

	private void deduceAndEnqueue(AxiomRule rule, Queue<AxiomLiteral> queue,
			Map<Integer, Integer> variableValueAssignment) {
		int var = rule.effectVariable;
		int val = rule.effectValue;
		assert variableValueAssignment != null;
		assert variableValueAssignment.containsKey(var);
		if (variableValueAssignment.get(var) != val) {
			if (DEBUG) {
				System.out.println(" -> deduced " + var + " = " + val);
			}
			variableValueAssignment.put(var, val);
			queue.add(rule.effectLiteral);
		}
	}
}

class NegationAsFailureInfo {

	int variableNumber;
	AxiomLiteral literal;

	public NegationAsFailureInfo(int variableNumber, AxiomLiteral literal) {
		this.variableNumber = variableNumber;
		this.literal = literal;
	}
}

class AxiomRule {

	int effectVariable;
	int effectValue;
	AxiomLiteral effectLiteral;

	/**
	 * Number of conditions of this rule.
	 */
	int numberOfConditions;

	/**
	 * Number of conditions of this rule which are still unsatisfied.
	 */
	int unsatisfiedCondition = -1;

	/**
	 * Creates a new axiom rule.
	 */
	public AxiomRule(int numberOfConditions, int effectVariable, int effectValue, AxiomLiteral effectLiteral) {
		this.numberOfConditions = numberOfConditions;
		this.effectVariable = effectVariable;
		this.effectValue = effectValue;
		this.effectLiteral = effectLiteral;
	}

	@Override
	public String toString() {
		return "(" + effectVariable + "," + effectValue + ")\nunsatisfiedCondition: " + unsatisfiedCondition
				+ "\nConditionOf: " + effectLiteral.conditionOf;
	}
}

class AxiomLiteral {
	List<AxiomRule> conditionOf = new ArrayList<AxiomRule>();

	@Override
	public String toString() {
		return conditionOf.toString();
	}
}

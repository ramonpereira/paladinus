package paladinus.heuristic;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

import paladinus.Global;
import paladinus.explicit.ExplicitCondition;
import paladinus.explicit.ExplicitOperator;
import paladinus.explicit.ExplicitOperator.OperatorRule;
import paladinus.explicit.ExplicitState;
import paladinus.problem.Problem;
import paladinus.state.Operator;
import paladinus.state.State;
import paladinus.util.Pair;

/**
 * Relaxed planning graph heuristic as implemented in the FF planning system by
 * J. Hoffmann.
 *
 * @author Robert Mattmueller
 */
public class FFHeuristic extends Heuristic {

	/**
	 * List of all rules
	 */
	List<FFRule> rules;

	/**
	 * Mapping from variable indices to mappings from value indices to FF
	 * propositions
	 */
	private List<List<FFProposition>> variableValueToProposition;

	/**
	 * Queue containing all reachable FF propositions.
	 */
	private PriorityQueue<FFProposition> reachableQueue;

	/**
	 * FFProposition corresponding to goal. This is either the auxiliary variable
	 * corresponding to the conjunction of the goal atoms or the variable
	 * corresponding to the root node of BDD.
	 */
	final FFProposition goalProp;

	/**
	 * How to accumulate heuristic values using the relaxed planning graph.
	 */
	public enum RPGStrategy {
		MAX, FF, ADD
	};

	/**
	 * The strategy that is used to evaluate the relaxed planning graph. By default
	 * FF is used.
	 */
	RPGStrategy strategy = RPGStrategy.FF;

	/**
	 * Set DEBUG true for more outputs.
	 */
	public static boolean DEBUG = false;
	
	private List<ExplicitOperator> relaxedPlan = new ArrayList<>();
	
	public FFHeuristic(Problem problem, RPGStrategy strategy) {
		this(problem, strategy, problem.explicitGoal);
	}
	
	/**
	 * Create a new FF heuristic evaluator for a given problem.
	 *
	 * @param problem The problem for which to compute heuristic estimates
	 */
	public FFHeuristic(Problem problem, RPGStrategy strategy, ExplicitCondition goal) {
		super(problem, true); // FF heuristic supports axioms.
		this.strategy = strategy;
		reachableQueue = new PriorityQueue<FFProposition>();
		buildPropositions();
		goalProp = buildGoalPropositions();
		buildRules();
		buildGoalRules(goal);
		rules = Collections.unmodifiableList(rules);
		if (DEBUG) {
			System.out.println("Initialized rules...");
			for (FFRule r : rules) {
				r.dump();
			}
		}
	}

	/**
	 * Add a rule with given head, body, and base cost to the rule base.
	 *
	 * @param head   Head of the rule
	 * @param opBody Body of the rule
	 * @param cost   Base cost of the rule
	 */
	private void addRule(FFProposition head, Collection<Pair<Integer, Integer>> opBody, ExplicitOperator op) {
		List<FFProposition> body = new ArrayList<FFProposition>();
		for (Pair<Integer, Integer> opCondition : opBody) {
			body.add(getProposition(opCondition));
		}
		addRule(head, body, op);
	}

	/**
	 * Add a rule with given head, body, and base cost to the rule base.
	 *
	 * @param head Head of the rule
	 * @param body Body of the rule
	 * @param cost Base cost of the rule
	 */
	private void addRule(FFProposition head, List<FFProposition> body, ExplicitOperator op) {
		FFRule rule = new FFRule(body, head, op);
		rules.add(rule);
		for (FFProposition condition : body) {
			condition.preconditionOf.add(rule);
		}
	}

	/**
	 * Add a rule with given head, body, and operator to the rule base.
	 *
	 * @param opHead Head of the rule
	 * @param opBody Body of the rule
	 * @param op     Operator defined the rule
	 */
	private void addRule(Pair<Integer, Integer> opHead, Collection<Pair<Integer, Integer>> opBody,
			ExplicitOperator op) {
		FFProposition head = getProposition(opHead);
		addRule(head, opBody, op);
	}

	/**
	 * Build goal propositions.
	 *
	 * @param problem The problem
	 */
	private FFProposition buildGoalPropositions() {
		List<FFProposition> goalPropositions = new ArrayList<FFProposition>();
		FFProposition goalProp = new FFProposition(problem.numStateVars, 1);
		goalPropositions.add(goalProp);
		variableValueToProposition.add(goalPropositions);
		return goalProp;
	}

	/**
	 * Build goal rules, i.e., rules stating when a goal proposition has been
	 * reached.
	 *
	 * @param goals Goal conditions
	 */
	private void buildGoalRules(ExplicitCondition goal) {
		List<Pair<Integer, Integer>> opBody = goal.getVariableValueAssignmentAsPairs();

		// create a dummy operator with operator cost = 0
		Set<Pair<Integer, Integer>> obs = new HashSet<Pair<Integer, Integer>>();
		Map<Integer, Integer> map = new HashMap<Integer, Integer>();
		ExplicitCondition precondition = new ExplicitCondition(map);
		ExplicitOperator goalOperator = new ExplicitOperator("goalRule", precondition, null, obs, false, 0);

		addRule(goalProp, opBody, goalOperator);
	}

	/**
	 * Build the FF propositions for the given problem.
	 *
	 * @param problem The problem
	 */
	private void buildPropositions() {
		variableValueToProposition = new ArrayList<List<FFProposition>>();
		for (int var = 0; var < problem.numStateVars; var++) {
			List<FFProposition> propositions = new ArrayList<FFProposition>();
			int valueCount = problem.domainSizes.get(var);
			for (int val = 0; val < valueCount; val++) {
				propositions.add(new FFProposition(var, val));
			}
			variableValueToProposition.add(propositions);
		}
	}

	/**
	 * Build the rules for the given problem.
	 *
	 * @param problem The problem
	 */
	private void buildRules() {
		rules = new LinkedList<FFRule>();

		// Add rules for causative operators.
		for (Operator op : problem.getOperators()) {
			if(this.partitioner != null) {
				if(this.partitioner.getUnstableOperatorsForGoalFacts().contains(op)) {
					continue;
				}
			}
			if (op.isCausative) {
				buildRulesForOperator(op.getExplicitOperator());
			}
		}

		// Add rules for axioms.
		for (OperatorRule axiom : problem.axioms) {
			// Only the costs of this "dummy" operator are used. // TODO Fix this?
			ExplicitOperator dummy = new ExplicitOperator("axiom", null, null,
					Collections.<Pair<Integer, Integer>>emptySet(), false, 0);
			addRule(axiom.head, axiom.body, dummy);
		}

		simplify();
		// Sort rules by costs.
		Collections.sort(rules); // TODO Is this necessary?
	}

	/**
	 * Build the rules corresponding to the given operator. E.g., an operator
	 * <tt>(pre,add,del)</tt> with <tt>pre = {1:0, 3:1}</tt>,
	 * <tt>add = {2:1, 3:2}</tt>, and <tt>del = {4:0, 3:1}</tt>, which has the
	 * relaxed operator <tt>({1:0, 3:1}, {2:1, 3:2}, {})</tt>, yields the two rules
	 * <tt>2:1 :- 1:0, 3:1</tt> and <tt>3:2 :- 1:0, 3:1</tt>.
	 *
	 * @param op Operator for which to construct the rules.
	 */
	private void buildRulesForOperator(ExplicitOperator op) {
		Collection<OperatorRule> opRules = op.getRules();
		for (OperatorRule opRule : opRules) {
			addRule(opRule.head, opRule.body, op);
		}
	}

	/**
	 * Collect the rules required to reach a given effect proposition. The collected
	 * rules are accumulated in the set <tt>collectedRules</tt>.
	 *
	 * @param effect         Proposition from which to chain backward
	 * @param collectedRules Accumulator holding all rules collected so far
	 */
	private void collectRequiredRules(FFProposition effect, List<FFRule> collectedRules) {
		FFRule newRule = effect.reachedBy;
		if (newRule != null) {
			relaxedPlan.add(this.index, newRule.operator);
			this.index++;
			collectedRules.add(newRule);
			for (FFProposition precondition : newRule.body) {
				if (!collectedRules.contains(precondition.reachedBy)) {
					collectRequiredRules(precondition, collectedRules);
				}
			}
		}
	}

	/**
	 * Explore the relaxed planning graph starting with the propositions true in the
	 * current state until either the goal is reached or the planning graph has
	 * levelled off.
	 *
	 * @return True iff there is a goal condition for which all propositions have
	 *         been reached.
	 */
	private boolean explore() {
		if (DEBUG) {
			System.out.println("Reachable queue: " + reachableQueue);
		}
		HashSet<FFProposition> seen = new HashSet<FFProposition>();
		while (!reachableQueue.isEmpty()) {
			FFProposition prop = reachableQueue.poll();

			if (!seen.contains(prop)) {
				if (prop.equals(goalProp)) {
					return true;
				}

				for (FFRule rule : prop.preconditionOf) {
					rule.unsatisfiedPreconditions--;
					assert rule.unsatisfiedPreconditions >= 0;
					if (rule.unsatisfiedPreconditions == 0) {
						triggerRule(rule);
					}
				}
				seen.add(prop);
			}
		}

		return false;
	}

	/**
	 * Extract the value of the FF heuristic from the relaxed planning graph by
	 * performing a backward sweep from the reached goal proposition. If there was
	 * more than one goal condition, the extraction starts with the first goal
	 * condition (= conjunction of goal propositions) reached.
	 *
	 * @return The value of the FF heuristic extracted from the relaxed planning
	 *         graph
	 */
	int index = 0;
	private int extractFFValue() {
		List<FFRule> requiredRules = new ArrayList<FFRule>();

		FFProposition goalProposition = goalProp;

		assert goalProposition != null;
		if (goalProposition == null) {
			System.err.println("Error in FFHeuristic.extractFFValue: none of the goal monomials has been reached yet.");
			Global.ExitCode.EXIT_CRITICAL_ERROR.exit();
		}
		index = 0;
		this.relaxedPlan = new ArrayList<>();
		collectRequiredRules(goalProposition, requiredRules);
		if (DEBUG) {
			System.out.println("FF: Required rules collected.");
		}
		assert requiredRules.size() >= 1;
		int cost = 0;
		for (FFRule rule : requiredRules) {
			cost += rule.operator.getCost();
		}
		return cost;
	}

	/**
	 * Compute the heuristic estimate of a given state.
	 *
	 * @param state The state for which to compute the heuristic estimate
	 * @return The evaluation of the given state, i.e., the length of a relaxed
	 *         plan. <tt>FFHeuristic.INFINITE_HEURISTIC</tt> indicates that the
	 *         relaxed planning graph exploration has levelled off without reaching
	 *         all the goals.
	 */
	public double getRPGHeuristic(ExplicitState state) {
		assert reachableQueue.isEmpty();
		initializePropositions(state);
		if (DEBUG) {
			System.out.println("FF: Propositions initialized.");
		}
		initializeRules();
		if (DEBUG) {
			System.out.println("FF: Rules initialized.");
		}

		boolean solvable = explore();
		if (DEBUG) {
			System.out.println("FF: Exploration done. Result solvable is " + solvable);
		}
		reachableQueue.clear();

		double result = INFINITE_HEURISTIC;
		switch (strategy) {
		case FF:
			result = solvable ? extractFFValue() : INFINITE_HEURISTIC;
			if (DEBUG) {
				System.out.println("FF: extractFFValue done.");
			}
			break;
		case MAX:
			result = solvable ? goalProp.reachCost : INFINITE_HEURISTIC;
			break;
			
		case ADD:
			result = solvable ? goalProp.reachCost : INFINITE_HEURISTIC;
			break;			
		}
		return result;
	}

	/**
	 * Get heuristic value for given state.
	 *
	 * @param state state to be evaluated by heuristic
	 * @return heuristic value for given state
	 */
	@Override
	public double getHeuristic(State state) {
		if (DEBUG) {
			System.out.println("FF: getHeuristic called");
		}
		if (state instanceof ExplicitState) {
			double hvalue = getRPGHeuristic((ExplicitState) state);
			/*
			double nonDeterministicHvalue = 0; 
			for(ExplicitOperator o: this.getRelaxedPlan())
				if(!o.getName().contains("goalRule"))
					nonDeterministicHvalue += o.getCost() * o.getNondeterministicEffect().size();
			
			return nonDeterministicHvalue;
			*/
			return hvalue;
		}
		return Double.POSITIVE_INFINITY;
	}
	
	public List<ExplicitOperator> getRelaxedPlan() {
		return relaxedPlan;
	}

	/**
	 * Get the FF proposition corresponding to a given variable-value pair.
	 *
	 * @param variable Variable
	 * @param value    Value
	 * @return The FF proposition corresponding to the given variable-value pair
	 */
	FFProposition getProposition(int variable, int value) {
		return variableValueToProposition.get(variable).get(value);
	}

	/**
	 * Get the FF proposition corresponding to a given variable-value pair.
	 *
	 * @param pair Variable-value pair
	 * @return The FF proposition corresponding to the given variable-value pair
	 */
	private FFProposition getProposition(Pair<Integer, Integer> pair) {
		return getProposition(pair.first, pair.second);
	}

	/**
	 * Initialize the propositions and mark those as reached which are true in the
	 * current state. Put them into the queue of reachable propositions.
	 *
	 * @param state Current state
	 */
	private void initializePropositions(ExplicitState state) {
		for (int var : state.variableValueAssignment.keySet()) {
			int value = state.variableValueAssignment.get(var);
			assert value >= 0;
			for (FFProposition prop : variableValueToProposition.get(var)) {
				prop.reachedBy = null;
				prop.reachCost = FFProposition.INFINITE_REACH_COST;
			}
			FFProposition currentProp = getProposition(var, value);
			currentProp.reachCost = 0;

			assert (!reachableQueue.contains(currentProp));
			reachableQueue.add(currentProp);
			if (problem.axiomLayer.get(var) != -1) {
				// Add default value of this derived variable.
				currentProp = getProposition(var, problem.defaultAxiomValues.get(var));
				currentProp.reachCost = 0;
				reachableQueue.add(currentProp);
			}
		}
		assert goalProp != null;
		goalProp.reachedBy = null; // reset goal proposition
		goalProp.reachCost = FFProposition.INFINITE_REACH_COST;
	}

	/**
	 * Initialize rules by setting their unsatisfied precondition counts to the
	 * respective numbers of preconditions, and trigger rules with empty bodies.
	 */
	private void initializeRules() {
		for (FFRule rule : rules) {
			rule.unsatisfiedPreconditions = rule.body.size();
			if (rule.unsatisfiedPreconditions == 0) {
				triggerRule(rule);
			}
		}
	}

	/**
	 * Simplify the proposition and rule sets.
	 */
	private void simplify() {
		// TODO Implement this
	}

	/**
	 * Trigger a rule and mark its effect/head proposition as reached and add it to
	 * the queue of reached propositions, unless it has already been reached before.
	 * Set the cost of reaching the proposition.
	 *
	 * @param rule Rule to be triggered.
	 */
	private void triggerRule(FFRule rule) {
		FFProposition effect = rule.head;
		switch (strategy) {
		case FF:
			if (effect.reachCost == FFProposition.INFINITE_REACH_COST) {
				effect.reachedBy = rule;
				double cost = rule.operator.getCost();
				for (FFProposition condition : rule.body) {
					if (cost + condition.reachCost > 0) {
						cost += condition.reachCost;
					}
				}
				effect.reachCost = cost;
				reachableQueue.add(effect);
			}
			break;
			
		case ADD:
			if (effect.reachCost == FFProposition.INFINITE_REACH_COST) {
				effect.reachedBy = rule;
				double cost = rule.operator.getCost();
				for (FFProposition condition : rule.body) {
					if (cost + condition.reachCost > 0) {
						cost += condition.reachCost;
					}
				}
				effect.reachCost = cost;
				reachableQueue.add(effect);
			}
			break;
			
		case MAX:
			// effect.reachedBy = rule;
			if (DEBUG) {
				System.out.println("case MAX: trigger rule for effect " + effect + " of op " + rule.operator);
			}
			double cost = rule.operator.getCost();
			double maxCost = 0;
			for (FFProposition condition : rule.body) {
				if (condition.reachCost > maxCost) {
					maxCost = condition.reachCost;
				}
			}
			if (DEBUG) {
				System.out.println("cost is " + cost);
			}
			if ((cost + maxCost) < effect.reachCost || effect.reachCost == FFProposition.INFINITE_REACH_COST) {
				effect.reachedBy = rule;
				cost += maxCost;
				assert cost >= 0 : "cost is " + cost + " and max cost is " + maxCost;
				effect.reachCost = cost;
				reachableQueue.add(effect);
			}
			if (DEBUG) {
				System.out.println("Cost of this effect " + cost);
			}
			break;
		}
	}

}

/**
 * A proposition, i.e., a variable-value pair, together with a list of FF rules
 * in whose precondition it occurs, the rule by which it was reached (initially
 * <tt>null</tt>) and to cost to reach it.
 *
 * @author Robert Mattmueller
 */
class FFProposition implements Comparable<FFProposition> {

	/**
	 * Indicates invalid reachability cost.
	 */
	public static final double INVALID_REACH_COST = -1;

	/**
	 * Indicates infinite reachability cost.
	 */
	public static final double INFINITE_REACH_COST = Double.POSITIVE_INFINITY;

	/**
	 * Used to mark proposition nodes in search graph.
	 */
	boolean markedForwards = false;

	/**
	 * Used to mark proposition nodes in search graph.
	 */
	boolean markedBackwards = false;

	/**
	 * The list of FF rules in whose body this proposition occurs.
	 */
	List<FFRule> preconditionOf;

	/**
	 * The FF rule by which this proposition was reached.
	 */
	FFRule reachedBy;

	/**
	 * The cost to reach this proposition.
	 */
	double reachCost;

	/**
	 * Variable represented by the variable-value pair of this proposition.
	 */
	int var;

	/**
	 * Value represented by the variable-value pair of this proposition.
	 */
	int value;

	/**
	 * Creates an FF proposition for a given variable-value pair.
	 *
	 * @param var   Variable
	 * @param value Value
	 */
	public FFProposition(int var, int value) {
		preconditionOf = new LinkedList<FFRule>();
		reachedBy = null;
		this.var = var;
		this.value = value;
		reachCost = INVALID_REACH_COST;
	}

	@Override
	public String toString() {
		return var + ":" + value;
	}

	@Override
	public int compareTo(FFProposition o) {
		if (reachCost < o.reachCost) {
			return -1;
		} else if (reachCost > o.reachCost) {
			return 1;
		}
		return 0;
	}
}

/**
 * An FF rule is a rule (head :- body) where the body is the precondition of an
 * operator and the head is one of the operator's add effects.
 *
 * @author Robert Mattmueller
 */
class FFRule implements Comparable<FFRule> {

	/**
	 * Indicates that the counter of unsatisfied preconditions of a rule has not yet
	 * been initialized.
	 */
	public static final int INVALID_PRECONDITION_COUNT = -1;

	/**
	 * Preconditions / body of this rule.
	 */
	Collection<FFProposition> body;

	/**
	 * Effect / head of this rule.
	 */
	FFProposition head;

	/**
	 * Number of preconditions of this rule which are still unsatisfied. This number
	 * is decreased during the exploration of the relaxed planning graph until it
	 * drops to zero and this rule triggers.
	 */
	int unsatisfiedPreconditions;

	/**
	 * Explicit operator to which this rule belongs.
	 */
	ExplicitOperator operator;

	/**
	 * Creates a new FF rule with a given body, head, and base cost.
	 *
	 * @param body     Body of this rule
	 * @param head     Head of this rule
	 * @param baseCost Base cost of this rule
	 */
	public FFRule(Collection<FFProposition> body, FFProposition head, ExplicitOperator op) {
		this.body = body;
		this.head = head;
		unsatisfiedPreconditions = INVALID_PRECONDITION_COUNT;
		operator = op;
	}

	@Override
	public String toString() {
		return "[FFRule] " + head + " :- " + body;
	}

	@Override
	public int compareTo(FFRule o) {
		if (operator.getCost() < o.operator.getCost()) {
			return -1;
		}
		if (operator.getCost() > o.operator.getCost()) {
			return 1;
		}
		return 0;
	}

	public void dump() {
		System.out.println(this);
		System.out.println("corresponding operator: " + operator + " with cost " + operator.getCost());
	}
}

package main.java.paladinus.problem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import main.java.paladinus.explicit.ExplicitAxiomEvaluator;
import main.java.paladinus.explicit.ExplicitCondition;
import main.java.paladinus.explicit.ExplicitOperator.OperatorRule;
import main.java.paladinus.explicit.ExplicitState;
import main.java.paladinus.heuristic.pdb.Abstraction;
import main.java.paladinus.state.Condition;
import main.java.paladinus.state.Operator;
import main.java.paladinus.state.State;

/**
 *
 * @author Manuela Ortlieb
 *
 */
public abstract class Problem {

	/**
	 * Explicit goal of this planning problem. Note: Explicit representation is also
	 * used in a partially observable problem for pattern selection search.
	 */
	public ExplicitCondition explicitGoal;

	/**
	 * Mapping from variables to their names.
	 */
	public final List<String> variableNames;

	/**
	 * List of proposition names corresponding to variable-value pairs.
	 * propositionNames.get(i).get(j) is the name of the proposition corresponding
	 * to the equality (var_i = j).
	 */
	public final List<List<String>> propositionNames;

	/**
	 * Mapping from variables to their domain sizes, i.e., the numbers of possible
	 * values the variables can assume.
	 */
	public final List<Integer> domainSizes;

	/**
	 * Number of state variables.
	 */
	public int numStateVars;

	/**
	 * Number of axioms.
	 */
	public final int numAxioms;

	/**
	 * Operators of this planning task. They could be different from the original
	 * given operators of the planning task, f.e. preprocessed with the
	 * preprocessor.
	 */
	protected Set<Operator> operators;

	/**
	 * Original given operators of the planning task. We need to keep them to
	 * compute the correct expected costs in the end.
	 */
	protected Map<String, Operator> originalOperators;
	
	protected Map<String, Operator> modifiedOperators;

	/**
	 * Indicates if problem is full observable or not.
	 */
	public final boolean isFullObservable;

	/**
	 * Explicit initial state(s) of the planning task. Note: Not final since
	 * explicit initial states are only computed if they are really needed. This is
	 * because this computation is expensive for a partially observable problem
	 * where belief state BDDs are used.
	 */
	protected List<ExplicitState> explicitInitialStateList;

	/**
	 * Axioms of this planning problem.
	 */
	public final Set<OperatorRule> axioms;

	/**
	 * Axiom layer of each variable.
	 */
	public final List<Integer> axiomLayer;

	/**
	 * Default values of all variables in current evaluated state.
	 */
	public final List<Integer> defaultAxiomValues;

	public Problem(ExplicitCondition explicitGoal, List<String> variableNames, List<List<String>> propositionNames,
			ArrayList<Integer> domainSizes, ArrayList<Integer> axiomLayer, ArrayList<Integer> defaultAxiomValues,
			LinkedHashSet<Operator> operators, Set<OperatorRule> axioms, boolean isFullObservable) {
		this.explicitGoal = explicitGoal;
		this.variableNames = variableNames;
		this.propositionNames = Collections.unmodifiableList(propositionNames);
		this.domainSizes = domainSizes; // Collections.unmodifiableList(domainSizes); // FIXME
		// Workaround for CEGAR
		numStateVars = domainSizes.size();
		this.axiomLayer = Collections.unmodifiableList(axiomLayer);
		this.axioms = Collections.unmodifiableSet(axioms);
		numAxioms = axioms.size();
		this.defaultAxiomValues = Collections.unmodifiableList(defaultAxiomValues);
		this.isFullObservable = isFullObservable;
		this.operators = operators;
	}

	/**
	 * Get set of explicit initial states of this planning task.
	 *
	 * @return set of explicit initial states
	 */
	public abstract List<ExplicitState> getExplicitInitialStates();

	/**
	 * Get the single initial state of this planning task.
	 *
	 * @return the initial state of this planning task
	 */
	public abstract State getSingleInitialState();

	/**
	 * Create an abstraction of this problem induced by given pattern.
	 *
	 * @param pattern the set of state variables to which this problem is abstracted
	 * @return abstraction induced by given pattern
	 */
	public Abstraction abstractToPattern(Set<Integer> pattern) {
		return abstractToPattern(pattern, getGoal());
	}

	/**
	 * Create an abstraction of this problem induced by given pattern.
	 *
	 * @param pattern the set of state variables to which this problem is abstracted
	 * @return abstraction induced by given pattern
	 */
	public abstract Abstraction abstractToPattern(Set<Integer> pattern, Condition goal);

	/**
	 * Takes abstraction info from abstractToPattern and returns a new Problem
	 * instnace.
	 */
	public abstract Problem newProblemFromAbstraction(Abstraction abstraction);

	/**
	 * Get goal of this planning task.
	 *
	 * @return goal
	 */
	public abstract Condition getGoal();

	/**
	 * Finish initialization of components and preprocessing.
	 */
	public abstract void finishInitializationAndPreprocessing();

	/**
	 * Dumping method for debugging reasons.
	 */
	public void dump() {
		System.out.println("There are " + numStateVars + " state variables.");
		System.out.println("Domain sizes:");
		for (int i = 0; i < variableNames.size(); i++) {
			System.out.println(variableNames.get(i).toString() + ":" + domainSizes.get(i));
		}
		System.out.println("Explicit initial state(s):");
		for (State state : getExplicitInitialStates()) {
			System.out.println(state);
		}
		System.out.println("Actions:");
		for (Operator op : operators) {
			op.dump();
		}
	}

	public void setGoal(ExplicitCondition newGoal) {
		explicitGoal = newGoal;
	}

	/**
	 * Test whether this problem is propositional in the sense that all variables
	 * occurring in it are Boolean.
	 *
	 * @return true iff. all variables in this problem have a domain size of at most
	 *         two
	 */
	public boolean isPropositional() {
		for (int i = 0; i < numStateVars; i++) {
			if (domainSizes.get(i) > 2) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Get the planning problem's operators.
	 *
	 * @return operators of the planning task.
	 */
	public Set<Operator> getOperators() {
		return operators;
	}

	/**
	 * Get the explicit axiom evaluator.
	 *
	 * @return explicit axiom evaluator.
	 */
	public abstract ExplicitAxiomEvaluator getExplicitAxiomEvaluator();

	/**
	 * Set the operators of this planning problem.
	 *
	 * @param ops set of operators
	 */
	public void setOperators(Set<Operator> ops) {
		setOperators(ops, false);
	}

	/**
	 * Set the operators of this planning problem.
	 *
	 * Note to updateOriginalOperators: The original operators are the operators
	 * described in the given planning task (SAS-file). They are used to compute the
	 * expected costs of the plan in the end. Manipulating operators f.e. our
	 * preprocessing leads to wrong expected costs if we do not store the original
	 * operators. That means that in general the operators may not be updated!
	 *
	 * @param ops                     set of operators
	 * @param updateOriginalOperators set true iff the original operators of this
	 *                                planning have to be updated
	 */
	public void setOperators(Set<Operator> ops, boolean updateOriginalOperators) {
		operators = ops;
		if (updateOriginalOperators) {
			setOriginalOperators(operators); // TODO Explanation
		}
	}

	/**
	 * Set initial state of this planning problem.
	 *
	 * @param initState
	 */
	public abstract void setInitialState(State initState);

	/**
	 * Get the original operators of planning task.
	 *
	 * @return original operator map
	 */
	public Map<String, Operator> getOriginalOperatorMap() {
		return originalOperators;
	}
	
	public Map<String, Operator> getModifiedOperatorMap() {
		return modifiedOperators;
	}

	protected abstract void setOriginalOperators(Set<Operator> ops);
	
	protected abstract void setModifiedOperators(Set<Operator> ops);

}

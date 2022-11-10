package main.java.paladinus.heuristic.pdb;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import main.java.paladinus.Global;
import main.java.paladinus.PaladinusPlanner;
import main.java.paladinus.explicit.ExplicitEffect;
import main.java.paladinus.explicit.ExplicitOperator;
import main.java.paladinus.explicit.ExplicitOperator.OperatorRule;
import main.java.paladinus.problem.OperatorAnalyzer;
import main.java.paladinus.problem.OperatorAnalyzer.Connector;
import main.java.paladinus.problem.OperatorAnalyzer.DependencyGraph;
import main.java.paladinus.problem.OperatorAnalyzer.Node;
import main.java.paladinus.problem.Problem;
import main.java.paladinus.state.Operator;
import main.java.paladinus.state.State;
import main.java.paladinus.symbolic.PartiallyObservableProblem;
import main.java.paladinus.util.Pair;

/**
 * This class does a hill climbing search to find a pattern selection. 1.
 * Initial Collection: {{v} | v is a state variable mentioned in the goal } 2.
 * Termination: as soon as the current pattern collection has no successors of
 * better quality or there are no successors at all because further growth of
 * the the current collection would exceed a memory limit 3. Neighborhood: the
 * neighbors of the collection C are all pattern collections C union {P'} where
 * - P' = P union {v} for some P in C - P' not in C - all variables of P' are
 * causally relevant in P' - P' is causally connected and - all pattern
 * databases C union {P'} can be represented within some prespecified space
 * limit
 *
 * @author Manuela Ortlieb
 * @author Robert Mattmueller
 */

public class PatternCollectionSearch {
	/**
	 * The problem this pattern collection search works on.
	 */
	protected final Problem problem;

	public enum PatternSearch {
		NONE, FO
	};

	/**
	 * Start time of the pattern selection procedure to calculate the overall time
	 * of the pattern selection.
	 */
	private long starttime;

	/**
	 * Mapping from derived variables to their set of variables they depend on.
	 */
	public final Map<Integer, Set<Integer>> derivedVarsDependencies;

	/**
	 * State variables which are considered as pattern variables.
	 */
	private Set<Integer> patternCandidateVariables;

	/**
	 * State variables which are forbidden as pattern variables.
	 */
	private Set<Integer> forbiddenVariables = new HashSet<Integer>();

	/**
	 * A graph which represents relations between observed variables and
	 * preconditions of sensing actions.
	 */
	private DependencyGraph graph;

	/**
	 * Set true for debug output information.
	 */
	public static boolean DEBUG = false;

	/**
	 * Initialize hill climbing search
	 *
	 * @param problem The underlying planning problem.
	 */
	public PatternCollectionSearch(Problem problem) {
		this.problem = problem;
		starttime = System.currentTimeMillis();
		// Compute dependencies of derived variables.
		if (problem.numAxioms > 0) {
			derivedVarsDependencies = Collections.unmodifiableMap(computeDependenciesOfDerivedVars());
		} else {
			derivedVarsDependencies = Collections.unmodifiableMap(Collections.<Integer, Set<Integer>>emptyMap());
		}
		if (problem.isFullObservable || Global.options.assumeFullObservabilityForPDBs()) {
			// In case of full observability each variable is a pattern candidate.
			patternCandidateVariables = new HashSet<Integer>();
			for (int var = 0; var < problem.numStateVars; var++) {
				patternCandidateVariables.add(var);
			}
		} else {
			patternCandidateVariables = computeAllowedPatternVariablesForPOND(problem);
			for (int var = 0; var < problem.numStateVars; var++) {
				if (!patternCandidateVariables.contains(var)) {
					forbiddenVariables.add(var);
				}
			}
			Set<Operator> ops = new HashSet<Operator>();
			for (Operator op : problem.getOperators()) {
				if (op.isSensing) {
					boolean takeIt = true;
					for (Pair<Integer, Integer> fact : op.observation) {
						if (!patternCandidateVariables.contains(fact.first)) {
							takeIt = false;
							break;
						}
					}
					if (takeIt) {
						ops.add(op);
					}
				}
			}
			if (Global.options.useDependencyGraph()) {
				graph = OperatorAnalyzer.analyze(ops);
				if (DEBUG) {
					System.out.println("Dependency graph: " + graph);
				}
			}
		}
	}

	/**
	 * Returns all causally relevant variables to the given set of variables without
	 * the variables itself.
	 *
	 * @param problem problem used for the analysis
	 * @param vars
	 * @return set of causally relevant variables for vars (without vars)
	 */
	public static Set<Integer> causallyRelevantVariables(Problem problem, Set<Integer> vars) {
		// More than one step in the causal graph?
		// Actually yes, but maybe this way we get a better pruning and still do not
		// lose too much.
		Set<Integer> result = new LinkedHashSet<Integer>();

		for (Operator op : problem.getOperators()) {
			Set<Integer> affectedVariables = op.getAffectedVariables();
			assert !affectedVariables.isEmpty(); // Useless operator.
			boolean affectsVars = false;
			for (int var : vars) {
				if (affectedVariables.contains(var)) {
					affectsVars = true;
					break;
				}
			}
			if (affectsVars) {
				for (int precondVar : op.getExplicitOperator().precondition.variableValueMap.keySet()) {
					if (!vars.contains(precondVar)) {
						result.add(precondVar);
					}
				}
				for (int var : affectedVariables) {
					if (!vars.contains(var)) {
						result.add(var);
					}
				}
			}
		}
		return result;
	}

	private Map<Integer, Set<Integer>> computeDependenciesOfDerivedVars() {
		Map<Integer, Set<Integer>> dependencies = new HashMap<Integer, Set<Integer>>(problem.numStateVars);
		Map<Integer, Set<Integer>> directDependency = new HashMap<Integer, Set<Integer>>(problem.numStateVars);
		for (OperatorRule axiom : problem.axioms) {
			Set<Integer> vars;
			if (directDependency.containsKey(axiom.head.first)) {
				vars = directDependency.get(axiom.head.first);
			} else {
				vars = new HashSet<Integer>();
			}
			for (Pair<Integer, Integer> fact : axiom.body) {
				vars.add(fact.first);
			}
			if (!vars.isEmpty()) {
				directDependency.put(axiom.head.first, vars);
			}
		}
		for (int var : directDependency.keySet()) {
			Set<Integer> varDependsOn = new HashSet<Integer>();
			Queue<Integer> queue = new LinkedList<Integer>();
			queue.add(var);
			while (!queue.isEmpty()) {
				int v = queue.poll();
				if (v != var) {
					varDependsOn.add(v);
				}
				if (directDependency.containsKey(v)) {
					for (int w : directDependency.get(v)) {
						if (!queue.contains(w) && !varDependsOn.contains(w)) {
							queue.add(w);
						}
					}
				}
			}
			dependencies.put(var, varDependsOn);
		}
		return dependencies;
	}

	private Set<Set<Integer>> computeAdditionalPatterns(CanonicalPDBHeuristic canonical, Set<Integer> pattern) {

		System.out.println("Computing additional patterns:");

		Set<Set<Integer>> result = new HashSet<Set<Integer>>();
		for (int var : causallyRelevantVariables(problem, pattern)) {
			if (forbiddenVariables.contains(var)) {
				continue;
			}
			Set<Integer> newPattern = new LinkedHashSet<Integer>(); // keeps order of variables
			newPattern.addAll(pattern);
			// Add new causally relevant variable.
			newPattern.add(var);
			if (derivedVarsDependencies.containsKey(var)) {
				// Add variables on which the new variable depends.
				newPattern.addAll(derivedVarsDependencies.get(var));
			}
			if (newPattern.equals(pattern) || canonical.temporaryPDBs.containsKey(newPattern)
					|| canonical.patterntoPDB.containsKey(newPattern)) {
				continue;
			}
			if (!problem.isFullObservable && !Global.options.assumeFullObservabilityForPDBs()
					&& Global.options.useDependencyGraph() && (graph.containsNode(var) || graph.containsLabel(var))) {
				// System.err.println("new pattern before: " + newPattern);
				newPattern = extendToValidPattern(newPattern, var);
				// System.err.println("new pattern after extension: " + newPattern);
			}

			System.out.println("  Candidate for additional pattern: " + newPattern);

			if ((projectedOverallMemoryUsageOK(canonical, newPattern)
					&& (!canonical.temporaryPDBs.containsKey(newPattern)) && Global.options.cachePDBs())
					|| (projectedOverallMemoryUsageOK(canonical, newPattern)
							&& !canonical.patterntoPDB.containsKey(newPattern) && !Global.options.cachePDBs())) {
				result.add(newPattern);
				System.out.println("    ... accepted!");
			} else {
				if (!projectedOverallMemoryUsageOK(canonical, newPattern)) {
					System.out.println("    ... rejected! Too large!");
				} else {
					System.out.println("    ... rejected! Already there!");
				}
			}
		}
		System.out.println("Done!");
		return result;
	}

	/**
	 * Create initial pattern collection which consists of singleton patterns each
	 * containing a goal variable.
	 *
	 * @return initial pattern collection
	 */
	private Set<Set<Integer>> createInitialPatternCollection() {
		Set<Set<Integer>> result = new LinkedHashSet<Set<Integer>>((int) Math.ceil(problem.explicitGoal.size / 0.75));
		for (int var : problem.explicitGoal.variableValueMap.keySet()) {
			if (DEBUG) {
				System.out.println("goalvar " + var);
			}
			if (patternCandidateVariables.contains(var)) {
				Set<Integer> pattern = createSingletonPattern(var);
				// Check for dependencies of derived variables.
				if (derivedVarsDependencies.containsKey(var)) {
					pattern.addAll(derivedVarsDependencies.get(var));
				}
				if (projectedOverallMemoryUsageOK(null, pattern)) {
					result.add(pattern);
				} else {
					System.out.println("Pattern " + pattern + " rejected! Too large!");
					System.err.println("WARNING: There is a (maybe derived?) goal variable");
					System.err.println("which can not be taken as pattern for initial pattern");
					System.err.println("collection! Try to increase pdbMaxSize/pdbsMaxSize.");
				}
			}
		}
		if (DEBUG) {
			System.out.println("Following goal variables are allowed as pattern variables:");
			System.out.println(result);
		}
		return result;
	}

	private Set<Integer> createSingletonPattern(int var) {
		Set<Integer> patternSet = new HashSet<Integer>();
		patternSet.add(var);
		return patternSet;
	}

	private int patternImprovesHeuristic(CanonicalPDBHeuristic canonical, Set<Integer> newPattern,
			Collection<State> samples) {
		Set<Set<Set<Integer>>> maxAdditiveSubsets = canonical.compatibilityGraph.getMaxAdditiveSubsets(newPattern);
		int numberOfImprovements = 0;
		for (State sample : samples) {
			if (improvement(canonical, newPattern, sample, maxAdditiveSubsets)) {
				numberOfImprovements++;
			}

		}
		return numberOfImprovements;
	}

	private boolean improvement(CanonicalPDBHeuristic canonical, Set<Integer> newPattern, State sample,
			Set<Set<Set<Integer>>> maxAdditiveSubsets) {
		double oldCanonicalHeuristicValue = canonical.getCanonicalHeuristic(sample);
		double newPatternHeuristicValue;
		newPatternHeuristicValue = canonical.temporaryPDBs.get(newPattern).getHeuristic(sample);
		if (oldCanonicalHeuristicValue == Double.POSITIVE_INFINITY) {
			return false;
		}
		if (newPatternHeuristicValue == Double.POSITIVE_INFINITY) {
			// It has to be ensured that the set of
			// candidates is free of duplicates! Elsewhere this "return"
			// won't be correct, if newPattern is
			// in the collection yet.
			return true;
		}
		for (Set<Set<Integer>> subset : maxAdditiveSubsets) {
			// for each maximal subset...
			double subsetHeuristicValue = 0;
			for (Set<Integer> pattern : subset) {
				double d = canonical.patterntoPDB.get(pattern).getHeuristic(sample);
				if (d == Double.POSITIVE_INFINITY) {
					System.out.println(pattern);
					System.out.println(canonical.patterntoPDB.get(pattern));
					System.out.println(sample);
				}
				assert (!(d == Double.POSITIVE_INFINITY));
				subsetHeuristicValue += d;
			}
			if ((newPatternHeuristicValue + subsetHeuristicValue) > oldCanonicalHeuristicValue
					+ 2 * AbstractCostComputation.EPSILON) {
				return true;
			}
		}
		return false;
	}

	private boolean projectedOverallMemoryUsageOK(CanonicalPDBHeuristic canonical, Set<Integer> newPattern) {
		// a) check if the new pattern alone is too large (this does not
		// immediately
		// have to do with memory usage and rather with trying to steer the
		// search
		// towards roughly equal sized patterns instead of one large dominating
		// pattern
		// plus a few relatively unimportant small patterns.
		int sizeOfNewPattern = PDB.numAbstractStates(problem, newPattern);
		if (sizeOfNewPattern > Global.options.pdbMaxSize()) {
			return false;
		}

		// b) check if the sum of the sizes of the pattern databases represented
		// by the
		// current pattern collection together with the new pattern will not
		// exceed
		// a given threshold.
		int canonicalSize = 0;
		if (canonical != null) {
			canonicalSize = canonical.size;
		}
		if (canonicalSize + sizeOfNewPattern > Global.options.pdbsMaxSize()) {
			return false;
		}

		return true;
	}

	/**
	 * Get variables which are allowed as pattern variables for singleton patterns.
	 *
	 * Note: There could be variables in this set which are not unrestricted allowed
	 * in a non-singleton pattern. For example a variable "a" which is part of a
	 * precondition of a sensing action <a, {}, {x}>. Such a precondition variable
	 * could prohibit, that the sensing action is applicable in the abstraction to
	 * observe a variable "x". If "x" could be observed in the concrete transition
	 * system implicitly via a sensing action <T, {}, {y}>, which observes "y". If
	 * "y" is not in the pattern, then this could lead to a unsolvable abstract
	 * problem, whereas the concrete problem is solvable. Therefore we require to
	 * add "y" to a pattern containing "x" and "a". This is done by analyzing
	 * preconditions of sensing actions. See OperatorAnalyzer class.
	 *
	 * TODO: static?
	 *
	 * @return Set of pattern variables which are allowed in POND planning for
	 *         singleton patterns
	 */
	public static Set<Integer> computeAllowedPatternVariablesForPOND(Problem problem) {
		return computeAllowedPatternVariablesForPOND(problem, problem.explicitGoal.variableValueMap.keySet());
	}

	public static Set<Integer> computeAllowedPatternVariablesForPOND(Problem problem, Set<Integer> goalVariables) {
		assert !problem.isFullObservable;
		Set<Integer> result = new HashSet<Integer>();
		HashMap<Integer, HashSet<Integer>> observableValues = new HashMap<Integer, HashSet<Integer>>();
		HashSet<Integer> conditionalVars = new HashSet<Integer>();
		HashSet<Integer> varsWhichCouldBecomeUncertain = new HashSet<Integer>();

		for (Operator op : problem.getOperators()) {
			// 1. variable is "good" if it is observable.
			// That means that at least k-1 values has to be observable, if domain size is
			// k.
			// Furthermore the sensing action must not have a precondition to guarantee,
			// that it is always
			// observable.
			for (Pair<Integer, Integer> varVal : op.observation) {
				if (observableValues.containsKey(varVal.first)) {
					observableValues.get(varVal.first).add(varVal.second);
				} else {
					HashSet<Integer> values = new HashSet<Integer>();
					values.add(varVal.second);
					observableValues.put(varVal.first, values);
				}
			}
			ExplicitOperator explicitOp = op.getExplicitOperator();
			// 3. variable is "good" if it is does not become uncertain and its value is
			// initially known.
			// Check all nondeterministic effects.
			if (explicitOp.isCausative && explicitOp.getNondeterministicEffect().size() > 1) {
				// Start by creating a map from every affected var to its precondition value or
				// -1 if
				// it is not part of the precondition.
				HashMap<Integer, Integer> affectedVarsToPreconditionValue = new HashMap<Integer, Integer>();
				for (int var : explicitOp.getAffectedVariables()) {
					if (explicitOp.precondition.variableValueMap.containsKey(var)) {
						affectedVarsToPreconditionValue.put(var, explicitOp.precondition.variableValueMap.get(var));
					} else {
						affectedVarsToPreconditionValue.put(var, -1);
					}
				}

				HashMap<Integer, Integer> variableValuePairsOfAllChoices = new HashMap<Integer, Integer>();
				for (Set<ExplicitEffect> choice : explicitOp.getNondeterministicEffect()) {
					HashMap<Integer, Integer> choiceVarVals = new HashMap<Integer, Integer>();
					for (ExplicitEffect eff : choice) {
						choiceVarVals.put(eff.variable, eff.value); // collect all variables and values of this
						// choice.
					}

					// For each var (which is not yet in varsWhichCouldBecomeUncertain), test if
					// value is the same as in other choices (of earlier iterations).
					for (int var : affectedVarsToPreconditionValue.keySet()) {
						if (!varsWhichCouldBecomeUncertain.contains(var)) {
							if (!variableValuePairsOfAllChoices.containsKey(var)) { // initialization
								if (choiceVarVals.containsKey(var)) {
									variableValuePairsOfAllChoices.put(var, choiceVarVals.get(var));
								} else {
									variableValuePairsOfAllChoices.put(var, affectedVarsToPreconditionValue.get(var));
								}
							} else { // Test for equality.
								if (choiceVarVals.containsKey(var)) {
									if (!variableValuePairsOfAllChoices.get(var).equals(choiceVarVals.get(var))) {
										varsWhichCouldBecomeUncertain.add(var);
									}
								} else {
									if (!variableValuePairsOfAllChoices.get(var)
											.equals(affectedVarsToPreconditionValue.get(var))) {
										varsWhichCouldBecomeUncertain.add(var);
									}
								}
							}
						}
					}
				}
			}
			// 2. variable is "good" if it does not occur in a condition or the goal.
			conditionalVars.addAll(explicitOp.precondition.variableValueMap.keySet());
			conditionalVars.addAll(goalVariables);
		}
		for (int var = 0; var < problem.numStateVars; ++var) {
			// 1. variable is "good" if it is observable.
			if (observableValues.containsKey(var)
					&& observableValues.get(var).size() >= problem.domainSizes.get(var) - 1) {
				result.add(var);
			}
			// 2. variable is "good" if it is does not become uncertain and is initially
			// known.
			else if (!varsWhichCouldBecomeUncertain.contains(var)
					&& ((PartiallyObservableProblem) problem).variablesWhichAreInitiallyKnown.contains(var)) {
				result.add(var);
			}
			// 3. variable is "good" if it does not occur in a condition or the goal.
			else if (!conditionalVars.contains(var)) {
				result.add(var);
			}
		}
		return result;
	}

	public Set<Integer> reduceToValidPattern(Set<Integer> pattern) {
		if (DEBUG) {
			System.out.println("pattern is " + pattern);
		}
		Set<Integer> resultPattern = new LinkedHashSet<Integer>(pattern);
		Queue<Integer> varsToCheck = new LinkedList<Integer>(pattern);
		while (!varsToCheck.isEmpty()) {
			int var = varsToCheck.poll();
			if (!resultPattern.contains(var)) {
				// Variable is not relevant anymore because it was removed.
				continue;
			}
			if (DEBUG) {
				System.out.println("Check variable: " + var);
			}
			if (forbiddenVariables.contains(var)) {
				if (DEBUG) {
					System.out.println("Variable " + var
							+ " is removed, since it is contained in the set of forbidden variables.");
				}
				resultPattern.remove(var);
			} else if (graph.containsNode(var)) {
				if (DEBUG) {
					System.out.println("graph contains node " + var);
				}
				for (Connector con : graph.getNode(var).outgoingConnectors) {
					// Each outgoing connector is labeled with a variable which is relevant for var.
					if (resultPattern.contains(con.label)) {
						// Note: This check for result is done to skip earlier removed variables.
						if (DEBUG) {
							System.out.println("pattern contains label " + con.label);
						}
						// Variables var and con.label are only allowed together if all successors of
						// var, which
						// are labeled with con.label are in the pattern, too.
						// while (!queue.isEmpty()) {
						for (Node child : con.getChildren()) {
							if (!resultPattern.contains(child.stateVariable)) {
								// in this case the con.label variable is not allowed as pattern variable
								resultPattern.remove(con.label);
								if (graph.containsNode(con.label)) {
									// In this case we have to remove the node with state variable con.label
									// and the nodes's incoming connectors
									varsToCheck.remove(con.label);
									if (DEBUG) {
										System.out.println("Remove node " + con.label + " from graph.");
										System.out.println("Remove connector " + con + " from graph.");
									}
									// All nodes which are connected to this removed node, via an incoming connector
									// have to be checked again.
									for (Connector connector : graph.getNode(con.label).incomingConnectors) {
										if (!varsToCheck.contains(connector.parent.stateVariable)) {
											varsToCheck.add(connector.parent.stateVariable);
										}
									}
									if (DEBUG) {
										System.out.println("Removed: " + con.label);
									}
								}
								break;
							}
						}
					}
				}
			}
		}
		return resultPattern;
	}

	/**
	 * If the given pattern is not valid, the pattern is extended by using the
	 * dependency graph of sensing/preconditions.
	 *
	 * Note: It is assumed, that the pattern was valid before newVar was added (!).
	 *
	 * @param pattern the new pattern
	 * @param newVar  the new variable which was added to the pattern
	 * @return either pattern if it was valid or an extended valid version of
	 *         pattern
	 */
	private Set<Integer> extendToValidPattern(Set<Integer> pattern, int newVar) {
		assert (pattern.contains(newVar));
		SortedSet<Integer> result = new TreeSet<Integer>();
		result.addAll(pattern);
		Queue<Node> newVarQueue = new LinkedList<Node>();
		if (graph.containsNode(newVar)) {
			newVarQueue.add(graph.getNode(newVar));
		}
		if (graph.containsLabel(newVar)) {
			// Test all pattern variables, which are nodes of the graph about the label
			// newVar
			for (int patternVar : pattern) {
				if (graph.containsNode(patternVar)) {
					// Check if there is an outgoing connector with label newVar
					for (Connector con : graph.getNode(patternVar).outgoingConnectors) {
						if (con.label == newVar) {
							// Then the children have to be in the pattern, too.
							for (Node child : con.getChildren()) {
								if (!result.contains(child.stateVariable)) {
									newVarQueue.add(child);
									result.add(child.stateVariable);
								}
							}
						}
					}
				}
			}
		}
		while (!newVarQueue.isEmpty()) {
			Node n = newVarQueue.poll();
			// Test if the outgoing connector's labels are in the pattern.
			for (Connector con : n.outgoingConnectors) {
				if (result.contains(con.label)) {
					// Label is in the pattern, such that children have to be checked, too.
					for (Node child : con.getChildren()) {
						// If child is already in the pattern, we are fine.
						// Else the child is a new node, which has to be added to the pattern.
						if (!result.contains(child.stateVariable)) {
							result.add(child.stateVariable);
							newVarQueue.add(child);
						}
					}
				}
			}
		}
		return result;
	}

	@SuppressWarnings("unlikely-arg-type")
	public boolean patternAllowed(Set<Integer> pattern) {
		for (int var : pattern) {
			assert (!forbiddenVariables.contains(var));
			if (graph.containsNode(var)) {
				for (Connector con : graph.getNode(var).outgoingConnectors) {
					// Each outgoing connector is labeled with a variable which is causally relevant
					// to var.
					if (pattern.contains(con.label)) {
						// The pattern is only allowed if all successors labeled with con.label are in
						// the
						// pattern, too.
						Queue<Node> queue = new LinkedList<OperatorAnalyzer.Node>();
						Set<Node> seen = new HashSet<OperatorAnalyzer.Node>();
						queue.addAll(con.getChildren());
						seen.addAll(con.getChildren());
						while (!queue.isEmpty()) {
							Node n = queue.poll();
							if (!pattern.contains(n)) {
								return false;
							}
							for (Connector c : n.outgoingConnectors) {
								if (c.label == con.label) {
									for (Node child : c.getChildren()) {
										if (!seen.contains(child)) {
											seen.add(child);
											queue.add(child);
										}
									}
								}
							}
						}
					}
				}
			}
		}
		return true;
	}

	/**
	 * Perform hill climbing search until there is no more improvement or the
	 * possible improvers are pruned away because of memory constraints.
	 *
	 * @return The canonical PDB heuristic corresponding to the pattern collection
	 *         found by this search
	 */
	public CanonicalPDBHeuristic search() {
		System.out.println("Starting search for pattern collection.");

		// Initialize pattern collection (all singleton patterns for goal
		// variables)
		Set<Set<Integer>> currentCollection = createInitialPatternCollection();
		assert !currentCollection.isEmpty();

		// Initialize corresponding canonical PDB heuristic
		CanonicalPDBHeuristic canonical = new CanonicalPDBHeuristic(problem, currentCollection);
		if (DEBUG) {
			System.out.println("initial canonical pdb heuristic.");
			System.out.println(canonical);
		}

		if (Global.options.getNumHillClimbingSteps() == 0) {
			if (DEBUG) {

				System.out.println("Using initial pattern collection for search.");
			}
			return canonical; // Pattern collection with goal variables as patterns.
		}

		// Initial pattern candidate collection
		Set<Set<Integer>> candidatePatterns = new HashSet<Set<Integer>>();

		for (Set<Integer> pattern : currentCollection) {
			candidatePatterns.addAll(computeAdditionalPatterns(canonical, pattern));
		}

		// Is there still progress in hillclimbing search?
		boolean betterSuccessor = true;

		// Best successor pattern
		Set<Integer> bestAdditionalPattern;

		// Number of samples for which best successor pattern improves
		// the heuristic value
		int bestNumberOfImprovements;

		// steepest ascent hillclimbing
		boolean outOfTime = System.currentTimeMillis() - starttime > Global.options.getPDBTimeout();

		int step = 0;
		while (betterSuccessor && !outOfTime && step < Global.options.getNumHillClimbingSteps()) {
			Collection<State> samples = new RandomWalk(problem, canonical).getSamples();
			if (samples == null) {
				// Unsolvable problem detected.
				return null;
			}
			betterSuccessor = false;
			bestAdditionalPattern = null;
			bestNumberOfImprovements = 0;

			// check all patterns of the current collection
			// int c = 0;
			for (Set<Integer> candidate : candidatePatterns) {
				// c++;
				// System.out.println("pattern " + c + " of " + candidatePatterns.size());

				int numberOfImprovements = 0;

				// builds new PDB if there is no one for this candidate
				canonical.addTemporaryPatternDatabase(candidate);

				numberOfImprovements = patternImprovesHeuristic(canonical, candidate, samples);

				// System.out.println("numberOfImprovements " +
				// numberOfImprovements + " for pattern " + candidate);

				if (numberOfImprovements >= (Global.options.getGreedyImprovementFraction() * samples.size())) {
					// take this pattern immediately
					bestAdditionalPattern = candidate;
					betterSuccessor = true;
					System.out.println("Pattern taken immediately.");
					break;
				}
				// TODO Evaluate tie breaking
				// if (numberOfImprovements == bestNumberOfImprovements && ((double)
				// numberOfImprovements >=
				// minImprovement
				// * (double) samples.size())) {
				// // tie breaking rule (prefer smaller pattern?)
				// if (bestNumberOfImprovements > 0 && candidate.size() <
				// bestAdditionalPattern.size()) {
				// bestAdditionalPattern = candidate;
				// betterSucc = true;
				// }
				// }
				else if (numberOfImprovements > bestNumberOfImprovements
						&& (numberOfImprovements >= Global.options.getMinImprovementFraction() * samples.size())) {

					bestNumberOfImprovements = numberOfImprovements;
					// System.out.println("bestNumberofImprovements " + bestNumberOfImprovements);
					bestAdditionalPattern = candidate;
					// System.out.println("bestPattern " + bestAdditionalPattern);
					betterSuccessor = true;
				}
				outOfTime = System.currentTimeMillis() - starttime > Global.options.getPDBTimeout();
				if (outOfTime) {
					break;
				}
				if (!Global.options.cachePDBs()) {
					canonical.temporaryPDBs.clear();
				}
			}
			if (betterSuccessor) {
				currentCollection.add(bestAdditionalPattern);
				canonical.addPatternToPatternCollection(bestAdditionalPattern);
				canonical.dominancePruning();
				if (!outOfTime) {
					candidatePatterns.remove(bestAdditionalPattern);
					candidatePatterns.addAll(computeAdditionalPatterns(canonical, bestAdditionalPattern));
				}
			}
			step++;
			// clean bdds of sample states != initial state
			// if (!assumeFullObservability) {
			// for (State sample : samples) {
			// if (!sample.equals(Global.problem.manager.initialState))
			// sample.deleteBeliefStateBDD();
			// }
			// }
			System.out.println("Number of temporary PDBs: " + canonical.temporaryPDBs.size());
			// System.out.println("Estimated memory usage for temporary PDBs: " +
			// canonical.sizesOfTemporaryPDBs * 16/1000 + " KByte");
		}
		System.out.println("Finished search for pattern collection.");

		if (outOfTime) {
			System.out.println("Reason: out of time!");
		} else if (step >= Global.options.getNumHillClimbingSteps()) {
			System.out.println(
					"Reason: hillclimbing step bound of " + Global.options.getNumHillClimbingSteps() + " reached!");
		} else {
			System.out.println("Reason: local minimum!");
		}

		System.out.println("Canonical heuristic:");
		System.out.println(canonical);
		if (!problem.isFullObservable && Global.options.patternSearch() == PatternSearch.FO
				&& !Global.options.assumeFullObservabilityForPDBs()) {
			// use found pattern collection to build up a BeliefState PDB for the search
			PDB.buildExplicitPDBs = false; // necessary for canonical heuristic
			canonical = new CanonicalPDBHeuristic(problem, canonical.patterntoPDB.keySet());
			canonical.dominancePruning();
		}

		// delete temporary PDBs
		if (!PaladinusPlanner.testMode) {
			canonical.temporaryPDBs.clear();
		}
		RandomWalk.applicableOpsforSamples.clear();
		return canonical;
	}
}

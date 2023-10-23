package paladinus.search.dfs;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

import paladinus.Global;
import paladinus.heuristic.Heuristic;
import paladinus.problem.Problem;
import paladinus.search.HeuristicSearch;
import paladinus.search.SearchConnector;
import paladinus.search.SearchConnector.EvaluationFunctionCriterion;
import paladinus.search.SearchConnectorComparator;
import paladinus.search.SearchFlag;
import paladinus.search.SearchNode;
import paladinus.search.policy.Policy;
import paladinus.state.Operator;
import paladinus.state.State;
import paladinus.util.ActionSelectionRule;
import paladinus.util.Pair;

/**
 * 
 * A depth-first search algorithm for FOND Planning.
 * 
 * @author Ramon Fraga Pereira
 *
 */
public class DepthFirstSearch extends HeuristicSearch {
	
	public static boolean DEBUG = Global.options.debug();
	
	protected int alternatingIndex = 0;
	
	protected Policy policy;
	
	protected SearchFlag searchStatus = SearchFlag.NO_POLICY;
	
	protected Set<SearchNode> closedDeadEndsNodes = new HashSet<>();
	
	protected Set<SearchNode> closedVisitedNodes = new HashSet<>();
	
	protected Set<SearchNode> closedSolvedNodes = new HashSet<>();
	
	protected ActionSelectionRule actionSelectionCriterion = ActionSelectionRule.MIN_MAX_H;
	protected EvaluationFunctionCriterion evaluationFunctionCriterion = EvaluationFunctionCriterion.MAX;
	
	protected double estimatedValueBestConnectorFromInitialState = 0;
	
	protected int NUMBER_ITERATIONS = 0;
	protected int FIXED_POINT_COUNTER = 0;
	
	protected Map<BigInteger, Double> stateNodeMapHValue = new HashMap<BigInteger, Double>();
	
	public DepthFirstSearch(Problem problem, Heuristic heuristic, String actionSelection, String criterion) {
		super(problem, heuristic);
		this.setActionSelectionFunction(actionSelection);
		this.setEvaluationFunctionCriterion(criterion);
		System.out.println("Action Selection Criterion    : " + this.actionSelectionCriterion);
		System.out.println("Evaluation Function Criterion : " + this.evaluationFunctionCriterion);
	}
	
	public DepthFirstSearch(Problem problem, Heuristic heuristic, String actionSelection) {
		super(problem, heuristic);
		this.setActionSelectionFunction(actionSelection);
		System.out.println("Action Selection Criterion    : " + this.actionSelectionCriterion);
	}
	
	public DepthFirstSearch(Problem problem, Heuristic heuristic) {
		super(problem, heuristic);
	}

	@Override
	public void doIteration() {}
	
	protected Pair<SearchFlag, Set<SearchNode>> doSearch(SearchNode node, Set<SearchNode> closedSolved) {
		if (DEBUG)
			dumpStateSpace();
		
		if(timeout())
			return new Pair<SearchFlag, Set<SearchNode>>(SearchFlag.TIMEOUT, null);
		
		RECURSION_COUNTER++;
		
		if(node.isGoalNode() || closedSolved.contains(node)) {
			closedSolved.addAll(this.closedVisitedNodes);
			return new Pair<SearchFlag, Set<SearchNode>>(SearchFlag.GOAL, closedSolved);
		} else if (node.isDeadEndNode() || this.closedDeadEndsNodes.contains(node)) {
			return new Pair<SearchFlag, Set<SearchNode>>(SearchFlag.DEAD_END, closedSolved);
		} else if (this.closedVisitedNodes.contains(node)) {
			return new Pair<SearchFlag, Set<SearchNode>>(SearchFlag.VISITED, closedSolved);
		}
		this.closedVisitedNodes.add(node);
		
		PriorityQueue<SearchConnector> connectors = this.getNodeConnectors(node);
		
		NODE_EXPANSIONS++;

		boolean allConnectorsDeadEnds = true;
		
		while(!connectors.isEmpty()) {
			SearchConnector c = connectors.poll();
			
			if(node.equals(this.initialNode))
				this.estimatedValueBestConnectorFromInitialState = c.getEstimatedCost();

			Set<SearchNode> pathsFound = new HashSet<>();
			
			boolean newGoalPathFound = true;
			
			boolean connectorDeadEnd = false;
			
			Set<SearchNode> copyClosedSolved = new HashSet<>(closedSolved);
			
			while(newGoalPathFound == true) {
				newGoalPathFound = false;
				Set<SearchNode> findingGoalPath = new HashSet<>();
				
				for(SearchNode s: c.getChildren()) {
					if(!pathsFound.contains(s))
						findingGoalPath.add(s);
				}
				for(SearchNode s: findingGoalPath) {
					Pair<SearchFlag, Set<SearchNode>> resultSearch = doSearch(s, copyClosedSolved);
					SearchFlag flag = resultSearch.first;
					copyClosedSolved = new HashSet<SearchNode>(resultSearch.second);
					
					if(flag == SearchFlag.DEAD_END) {
						newGoalPathFound = false;
						connectorDeadEnd = true;
						break;
					}
					if(flag == SearchFlag.GOAL){
						newGoalPathFound = true;
						pathsFound.add(s);
					}
				}
				if(pathsFound.size() == c.getChildren().size()) {
					this.closedVisitedNodes.remove(node);
					node.setMarkedConnector(c);
					return new Pair<SearchFlag, Set<SearchNode>>(SearchFlag.GOAL, copyClosedSolved);
				}
				if(!connectorDeadEnd)
					allConnectorsDeadEnds = false;
			}
		}
		if(allConnectorsDeadEnds) {
			this.closedVisitedNodes.remove(node);
			this.closedDeadEndsNodes.add(node);
			return new Pair<SearchFlag, Set<SearchNode>>(SearchFlag.DEAD_END, closedSolved);
		}
		this.closedVisitedNodes.remove(node);
		return new Pair<SearchFlag, Set<SearchNode>>(SearchFlag.VISITED, closedSolved);
	}
	
	protected PriorityQueue<SearchConnector> getNodeConnectors(SearchNode node) {
		PriorityQueue<SearchConnector> connectors = this.getInstantiatedPriorityQueueOfConnectors();
		List<Operator> applicableOps = node.state.getApplicableOps(this.getProblem().getOperators());
		for(Operator op: applicableOps) {
			Set<State> successorStates = node.state.apply(op);
			assert !successorStates.isEmpty();
			List<SearchNode> children = new ArrayList<SearchNode>();
			for (State successor : successorStates) {
				SearchNode newNode = this.lookupAndInsertNode(successor, (int) (node.getDepth() + op.getCost()));
				newNode.setParent(node);
				children.add(newNode);
			}
			if(Global.options.getSuccessorsExploration().equals("SORT")) {
				Collections.sort(children);
			} else if(Global.options.getSuccessorsExploration().equals("REVERSE")) {
				Collections.sort(children, Collections.reverseOrder());
			} else if(Global.options.getSuccessorsExploration().equals("RANDOM")) {
				Collections.shuffle(children, Global.generator);
			}
			SearchConnector connector = new SearchConnector(node, children, op, this.evaluationFunctionCriterion);
			if(connector.getAverageChildEstimate() == Double.POSITIVE_INFINITY)
				continue;
			
			connectors.add(connector);
		}
		PriorityQueue<SearchConnector> priorityQueueConnectors = this.getInstantiatedPriorityQueueOfConnectors();
		if(connectors.size() == 0)
			return priorityQueueConnectors;
		
		double avgBranchingFactor = 0;
		double sumBranchingFactor = 0;
		for(SearchConnector connector: connectors)
			sumBranchingFactor += connector.getChildren().size();
		
		avgBranchingFactor = (sumBranchingFactor / connectors.size());
		
		for(SearchConnector connector: connectors) {
			connector.setAvgBranchingFactor(avgBranchingFactor);
			priorityQueueConnectors.add(connector);
		}
		return priorityQueueConnectors;
	}
	
	protected PriorityQueue<SearchConnector> getInstantiatedPriorityQueueOfConnectors(){
		if(Global.options.useClosedVistedNodes())
			return new PriorityQueue<>(new SearchConnectorComparator(this.actionSelectionCriterion, this.closedVisitedNodes));
		else return new PriorityQueue<>(new SearchConnectorComparator(this.actionSelectionCriterion));
	}
	
	protected double getMinEstimateFromSetOfNodes(Set<SearchNode> setNodes) {
		double min = Double.POSITIVE_INFINITY;
		for (SearchNode n: setNodes)
			if (n.getHeuristic() < min)
				min = n.getHeuristic();
		return min;
	}

	@Override
	public Policy getPolicy() {
		if (this.searchStatus == SearchFlag.DEAD_END) {
			System.out.println("Planning task is unsolvable. There is no policy.");
			return null;
		} else if (this.searchStatus == SearchFlag.NO_POLICY) {
			System.out.println("Planning task is not solved so far. No policy so far.");
			return null;
		} else if (this.searchStatus == SearchFlag.GOAL) {
			if (this.policy == null) {
				this.policy = new Policy(problem);
				this.fillStateActionTable((SearchNode) initialNode);
			}
			return this.policy;
		}
		return null;
	}

	@Override
	public Result run() {
		/* Start measuring search time. */
		starttime = System.currentTimeMillis();

		/* Get initial state and insert it with depth 0. */
		this.initialNode = this.lookupAndInsertNode(problem.getSingleInitialState(), 0);
		assert ((SearchNode) this.initialNode).getDepth() == 0;

		Set<SearchNode> closedSolved = new HashSet<>();
		
		Pair<SearchFlag, Set<SearchNode>> resultSearch = doSearch((SearchNode) this.initialNode, closedSolved);

		SearchFlag flag = resultSearch.first;
		this.closedSolvedNodes = resultSearch.second;
		
		System.out.println("\n# Closed-Solved Nodes = " + closedSolvedNodes.size());
		System.out.println("# Closed-Dead-End Nodes = " + this.closedDeadEndsNodes.size());
		
		this.searchStatus = flag;

		/* Finish measuring search time. */
		endtime = System.currentTimeMillis();
		
		if (DEBUG)
			dumpStateSpace();

		if(timeout())
			return Result.TIMEOUT;
		
		if (flag == SearchFlag.GOAL) {
			return Result.PROVEN;
		} else if (flag == SearchFlag.DEAD_END || flag == SearchFlag.NO_POLICY) {
			return Result.DISPROVEN;
		} else return Result.TIMEOUT;
	}
	
	public SearchNode lookupAndInsertNode(State state, int depth) {
		assert depth >= 0;
		SearchNode node;
		if (!this.stateNodeMap.containsKey(state.uniqueID)) {
			node = new SearchNode(state, this, depth, alternatingIndex);
			
			/*
			 * TODO: Novelty.
			node.setNodesSeenSoFar(new HashSet<>(this.stateNodeMap.values()));
			node.setNumberStateVariables(this.problem.numStateVars);
			node.computeBinaryNovelty();
			node.computeQuantifiedNovel();
			*/
			
			this.stateNodeMap.put(state.uniqueID, node);
			this.stateNodeMapHValue.put(state.uniqueID, node.getHeuristic());
			if (DEBUG)
				System.out.println("New node (index = " + node.index + "): " + node);
		} else {
			node = stateNodeMap.get(state.uniqueID);
			if (!state.equals(node.state))
				assert false;
			if (DEBUG)
				System.out.println("Known node (index = " + node.index + "): " + node);
		}
		return node;
	}
	
	private void setEvaluationFunctionCriterion(String criterion) {
		if(criterion == null) 
			return;
		
		switch (criterion) {
			case "MAX":
				this.evaluationFunctionCriterion = EvaluationFunctionCriterion.MAX;
				break;
			
			case "MIN":
				this.evaluationFunctionCriterion = EvaluationFunctionCriterion.MIN;
				break;
			
			default:
				throw new IllegalArgumentException("Unexpected value: " + criterion);
		}
		
	}
	
	private void setActionSelectionFunction(String function) {
		if(function == null) 
			return;
		
		switch (function) {
			case "NONE":
				this.actionSelectionCriterion = ActionSelectionRule.NONE;
				break;		
		
			case "MIN_H":
				this.actionSelectionCriterion = ActionSelectionRule.MIN_H;
				break;
				
			case "MIN_H_TIMES_CHILDREN_SIZE":
				this.actionSelectionCriterion = ActionSelectionRule.MIN_H_TIMES_CHILDREN_SIZE;
				break;
				
			case "MIN_H_POWER_CHILDREN_SIZE":
				this.actionSelectionCriterion = ActionSelectionRule.MIN_H_POWER_CHILDREN_SIZE;
				break;
				
				
			case "MIN_SUM_H":
				this.actionSelectionCriterion = ActionSelectionRule.MIN_SUM_H;
				break;
				
			case "MIN_SUM_H_TIMES_CHILDREN_SIZE":
				this.actionSelectionCriterion = ActionSelectionRule.MIN_SUM_H_TIMES_CHILDREN_SIZE;
				break;
				
			case "MIN_SUM_H_POWER_CHILDREN_SIZE":
				this.actionSelectionCriterion = ActionSelectionRule.MIN_SUM_H_POWER_CHILDREN_SIZE;
				break;

				
			case "MIN_MAX_H":
				this.actionSelectionCriterion = ActionSelectionRule.MIN_MAX_H;
				break;
				
			case "MAX_H":
				this.actionSelectionCriterion = ActionSelectionRule.MAX_H;
				break;
				
			case "MIN_MAX_H_TIMES_CHILDREN_SIZE":
				this.actionSelectionCriterion = ActionSelectionRule.MIN_MAX_H_TIMES_CHILDREN_SIZE;
				break;
				
			case "MIN_MAX_H_POWER_CHILDREN_SIZE":
				this.actionSelectionCriterion = ActionSelectionRule.MIN_MAX_H_POWER_CHILDREN_SIZE;
				break;
			
			
			case "MIN_SUM_H_ESTIMATED_BRANCHING_FACTOR":
				this.actionSelectionCriterion = ActionSelectionRule.MIN_SUM_H_ESTIMATED_BRANCHING_FACTOR;
				break;
				
				
			case "MAX_AVG_H_VALUE":
				this.actionSelectionCriterion = ActionSelectionRule.MAX_AVG_H_VALUE;
				break;
				
				
			case "MEAN_H":
				this.actionSelectionCriterion = ActionSelectionRule.MEAN_H;
				break;
			default:
				throw new IllegalArgumentException("Unexpected value: " + function);
		}
	}
	
	public int getNumberIterations() {
		return NUMBER_ITERATIONS;
	}
	
	public int getFixedPointCounter() {
		return FIXED_POINT_COUNTER;
	}
	
	protected void fillStateActionTable(SearchNode node) {
		for(SearchNode n: this.closedSolvedNodes) {
			if(n.getMarkedConnector() != null)
				this.policy.addEntry(n.state, n.getMarkedConnector().getOperator());
		}
	}
	
	@Override
	public void printStats(boolean simulatePlan) {
		NODES = stateNodeMap.size();
		System.out.println("\n# Total Nodes               = " + NODES);
		System.out.println("# Number of Expansions      = "   + RECURSION_COUNTER);
		System.out.println("# Number of Node Expansions = "   + NODE_EXPANSIONS);
		System.out.println("# Policy Size               = "   + getPolicy().size());
		
		if (simulatePlan)
			simulatePlan();
	}
}
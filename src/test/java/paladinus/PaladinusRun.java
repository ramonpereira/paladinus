package paladinus;

import java.io.IOException;

import org.junit.Test;

import paladinus.util.TranslateFONDUtils;

public class PaladinusRun {

	@Test
	public void runTest() {
		String[] args = new String[] {

//				"-search", "DFS",
//				
//				"-search", "ITERATIVE_DFS",

//				"-search", "ITERATIVE_DFS_LEARNING",
//				"-search", "ITERATIVE_DFS_LEARNING_NEW",
				
//				"-search", "ITERATIVE_DFS_PRUNING",
//				"-search", "ITERATIVE_DFS_PRUNING_NEW",
//				"-search", "ITERATIVE_DFS_PRUNING_LEARNING",
				"-search", "ITERATIVE_DFS_PRUNING_LEARNING_NEW",

//				"-heuristic", "PDBS",
//				"-heuristic", "FF",
				"-heuristic", "HMAX",
//				"-heuristic", "HADD",
//				"-heuristic", "LMCUT",
//				"-heuristic", "BLIND_DEADEND",
//				"-heuristic", "BLIND",

//				"-evaluationFunctionCriterion", "MIN",
				"-evaluationFunctionCriterion", "MAX",

				"-actionSelectionCriterion", "MIN_H",
//				"-actionSelectionCriterion", "MIN_MAX_H",
				
//				"-successorsExploration", "SORT",
//				"-successorsExploration", "REVERSE",
//				"-successorsExploration", "RANDOM",
				
//				"-tieBreakConnectors", "MIN_SUM",
//				"-tieBreakConnectors", "MAX_SUM",
//				"-tieBreakConnectors", "MIN_OUTCOMES_SIZE",
//				"-tieBreakConnectors", "MAX_OUTCOMES_SIZE",
//				"-tieBreakConnectors", "MIN_H_TIMES_OUTCOMES_SIZE",
//				"-tieBreakConnectors", "MAX_H_TIMES_OUTCOMES_SIZE",
//				"-tieBreakConnectors", "MIN_MAX_H_TIMES_OUTCOMES_SIZE",

				"-debug", "ON",

				"-timeout", "1200",

//				"-validatePolicyPRP", "ON",

//				"-t",
//				"FOND",
//				"-policytype", "STRONG_CYCLIC",

//				"benchmarks/acrobatics/domain.pddl",
//				"benchmarks/acrobatics/p8.pddl",

//				"benchmarks/beam-walk/domain.pddl",
//				"benchmarks/beam-walk/p5.pddl",

//				"benchmarks/blocksworld-2/domain.pddl",
//				"benchmarks/blocksworld-2/p10.pddl",

//				"benchmarks/blocksworld-new/domain.pddl",
//				"benchmarks/blocksworld-new/p11.pddl",

//				"benchmarks/blocksworld-original/domain.pddl",
//				"benchmarks/blocksworld-original/p27.pddl",

//				"benchmarks/chain-of-rooms/domain.pddl",
//				"benchmarks/chain-of-rooms/p3.pddl",
//
//				"benchmarks/doors/domain.pddl",
//				"benchmarks/doors/p13.pddl",

//				"benchmarks/earth-observation/domain.pddl",
//				"benchmarks/earth-observation/p16.pddl",

//				"benchmarks/elevators/domain.pddl",
//				"benchmarks/elevators/p10.pddl",

//				"benchmarks/faults/d31.pddl",
//				"benchmarks/faults/p31.pddl",

//				"benchmarks/first-responders/domain.pddl",
//				"benchmarks/first-responders/fr-p_9_1.pddl",

//				"benchmarks/islands/domain.pddl",
//				"benchmarks/islands/p35.pddl",

//				"benchmarks/miner/domain.pddl",
//				"benchmarks/miner/p17.pddl",

//				"benchmarks/tireworld-spiky/domain.pddl",
//				"benchmarks/tireworld-spiky/p3.pddl",

//				"benchmarks/tireworld-truck/domain.pddl",
//				"benchmarks/tireworld-truck/p5.pddl",

				"benchmarks/triangle-tireworld/domain.pddl",
				"benchmarks/triangle-tireworld/p3.pddl",

//				"benchmarks/zenotravel/domain.pddl",
//				"benchmarks/zenotravel/p6.pddl",

				"-exportPolicy", "policy.txt", "-exportDot", "policy.dot", 
//				"-printPolicy"
		};
		try {
			TranslateFONDUtils.removeSASFile();

			PaladinusPlanner paladinus = new PaladinusPlanner(args);
			paladinus.runProblem();

		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}
	}
}

package paladinus;

import java.io.IOException;

import org.junit.Test;

import paladinus.PaladinusPlanner;
import paladinus.util.TranslateFONDUtils;

public class PaladinusRun {

	@Test
	public void runTest() {
		String[] args = new String[] {

//				"-search", "DFS",
//				"-search", "ITERATIVE_DFS",
				"-search", "ITERATIVE_DFS_PRUNING",
//				"-search", "ITERATIVE_DFS_LEARNING",

//				"-heuristic", "PDBS",
//				"-heuristic", "FF",
//				"-heuristic", "HMAX",
				"-heuristic", "HADD",
//				"-heuristic", "LMCUT",
//				"-heuristic", "BLIND_DEADEND",
//				"-heuristic", "BLIND",

//				"-evaluationFunctionCriterion", "MIN",
				"-evaluationFunctionCriterion", "MAX",

//				"-actionSelectionCriterion", "NONE",
//				"-actionSelectionCriterion", "MIN_H",
//				"-actionSelectionCriterion", "MAX_H",
//				"-actionSelectionCriterion", "MIN_SUM_H",
				"-actionSelectionCriterion", "MIN_MAX_H",
//				"-actionSelectionCriterion", "MIN_MAX_H_POWER_CHILDREN_SIZE",
//				"-actionSelectionCriterion", "MIN_SUM_H_ESTIMATED_BRANCHING_FACTOR",
//				"-actionSelectionCriterion", "MEAN_H",
//				"-actionSelectionCriterion", "MAX_AVG_H_VALUE",

//				"-debug", "ON",

				"-timeout", "600",

//				"-validatePolicyPRP", "ON",

//				"-t",
//				"FOND",
//				"-policytype", "STRONG_CYCLIC",

//				"benchmarks/acrobatics/domain.pddl",
//				"benchmarks/acrobatics/p8.pddl",

//				"benchmarks/beam-walk/domain.pddl",
//				"benchmarks/beam-walk/p8.pddl",

//				"benchmarks/blocksworld-2/domain.pddl",
//				"benchmarks/blocksworld-2/p15.pddl",

//				"benchmarks/blocksworld-new/domain.pddl",
//				"benchmarks/blocksworld-new/p19.pddl",

//				"benchmarks/blocksworld-original/domain.pddl",
//				"benchmarks/blocksworld-original/p30.pddl",

//				"benchmarks/chain-of-rooms/domain.pddl",
//				"benchmarks/chain-of-rooms/p3.pddl",

//				"benchmarks/doors/domain.pddl",
//				"benchmarks/doors/p13.pddl",

//				"benchmarks/earth-observation/domain.pddl",
//				"benchmarks/earth-observation/p16.pddl",

//				"benchmarks/elevators/domain.pddl",
//				"benchmarks/elevators/p13.pddl",

//				"benchmarks/faults/d37.pddl",
//				"benchmarks/faults/p37.pddl",

//				"benchmarks/first-responders/domain.pddl",
//				"benchmarks/first-responders/fr-p_4_4.pddl",

//				"benchmarks/islands/domain.pddl",
//				"benchmarks/islands/p60.pddl",

//				"benchmarks/miner/domain.pddl",
//				"benchmarks/miner/p17.pddl",

				"benchmarks/tireworld-spiky/domain.pddl",
				"benchmarks/tireworld-spiky/p10.pddl",

//				"benchmarks/tireworld-truck/domain.pddl",
//				"benchmarks/tireworld-truck/p50.pddl",

//				"benchmarks/triangle-tireworld/domain.pddl",
//				"benchmarks/triangle-tireworld/p8.pddl",

//				"benchmarks/zenotravel/domain.pddl",
//				"benchmarks/zenotravel/p3.pddl",

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

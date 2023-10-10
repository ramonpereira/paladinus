package paladinus;

import java.io.IOException;

import org.junit.Test;

import paladinus.util.TranslateFONDUtils;

public class PaladinusRun {

	@Test
	public void runTest() {
		String[] args = new String[] {

				"-search", "DFS",
//				"-search", "ITERATIVE_DFS",
//				"-search", "ITERATIVE_DFS_PRUNING",
//				"-search", "ITERATIVE_DFS_LEARNING",
//				"-search", "ITERATIVE_DFS_PRUNING_LEARNING",

//				"-heuristic", "PDBS",
//				"-heuristic", "FF",
//				"-heuristic", "HMAX",
//				"-heuristic", "HADD",
//				"-heuristic", "LMCUT",
//				"-heuristic", "BLIND_DEADEND",
				"-heuristic", "BLIND",

//				"-evaluationFunctionCriterion", "MIN",
				"-evaluationFunctionCriterion", "MAX",

//				"-actionSelectionCriterion", "MIN_H",
				"-actionSelectionCriterion", "MIN_MAX_H",
				
				"-successorsExploration", "SORT",
				
				"-tieBreakConnectors", "MINSUM",

//				"-debug", "ON",

				"-timeout", "1200",

//				"-validatePolicyPRP", "ON",

//				"-t",
//				"FOND",
//				"-policytype", "STRONG_CYCLIC",

//				"benchmarks/acrobatics/domain.pddl",
//				"benchmarks/acrobatics/p8.pddl",

//				"benchmarks/beam-walk/domain.pddl",
//				"benchmarks/beam-walk/p10.pddl",

//				"benchmarks/blocksworld-2/domain.pddl",
//				"benchmarks/blocksworld-2/p15.pddl",

//				"benchmarks/blocksworld-new/domain.pddl",
//				"benchmarks/blocksworld-new/p13.pddl",

//				"benchmarks/blocksworld-original/domain.pddl",
//				"benchmarks/blocksworld-original/p1.pddl",

//				"benchmarks/chain-of-rooms/domain.pddl",
//				"benchmarks/chain-of-rooms/p3.pddl",

//				"benchmarks/doors/domain.pddl",
//				"benchmarks/doors/p14.pddl",

//				"benchmarks/earth-observation/domain.pddl",
//				"benchmarks/earth-observation/p16.pddl",

				"benchmarks/elevators/domain.pddl",
				"benchmarks/elevators/p15.pddl",

//				"benchmarks/faults/d37.pddl",
//				"benchmarks/faults/p37.pddl",

//				"benchmarks/first-responders/domain.pddl",
//				"benchmarks/first-responders/fr-p_4_4.pddl",

//				"benchmarks/islands/domain.pddl",
//				"benchmarks/islands/p35.pddl",

//				"benchmarks/miner/domain.pddl",
//				"benchmarks/miner/p17.pddl",

//				"benchmarks/tireworld-spiky/domain.pddl",
//				"benchmarks/tireworld-spiky/p5.pddl",

//				"benchmarks/tireworld-truck/domain.pddl",
//				"benchmarks/tireworld-truck/p22.pddl",

//				"benchmarks/triangle-tireworld/domain.pddl",
//				"benchmarks/triangle-tireworld/p1.pddl",

//				"benchmarks/zenotravel/domain.pddl",
//				"benchmarks/zenotravel/p7.pddl",
				
//				"benchmarks/ecai-23/Res/blocksworldMA/k_1/k_1_domain.pddl",
//				"benchmarks/ecai-23/Res/blocksworldMA/k_1/k_1_pfilep1.pddl",				

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

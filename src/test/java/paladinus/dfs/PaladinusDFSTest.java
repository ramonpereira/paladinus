package paladinus.dfs;

import java.io.IOException;

import org.junit.Test;

import paladinus.PaladinusPlanner;
import paladinus.util.TranslateFONDUtils;

public class PaladinusDFSTest {

	@Test
	public void testAcrobatics0() {
		String[] args = new String[] {

				"-search", "DFS",

//				"-heuristic", "PDBS",
//				"-heuristic", "FF",
//				"-heuristic", "HMAX",
				"-heuristic", "HADD",
//				"-heuristic", "LMCUT",
//				"-heuristic", "BLIND_DEADEND",
//				"-heuristic", "BLIND",

//				"-evaluationFunctionCriterion", "MIN",
				"-evaluationFunctionCriterion", "MAX",

//				"-actionSelectionCriterion", "MIN_H",
				"-actionSelectionCriterion", "MIN_MAX_H",
				
				"-successorsExploration", "SORT",
//				"-successorsExploration", "REVERSE",
//				"-successorsExploration", "RANDOM",
				
				"-tieBreakConnectors", "MIN_SUM",
//				"-tieBreakConnectors", "MAX_SUM",
//				"-tieBreakConnectors", "MIN_OUTCOMES_SIZE",
//				"-tieBreakConnectors", "MAX_OUTCOMES_SIZE",
//				"-tieBreakConnectors", "MIN_H_TIMES_OUTCOMES_SIZE",
//				"-tieBreakConnectors", "MAX_H_TIMES_OUTCOMES_SIZE",
//				"-tieBreakConnectors", "MIN_MAX_H_TIMES_OUTCOMES_SIZE",

//				"-debug", "ON",

				"-timeout", "1200",

//				"-validatePolicyPRP", "ON",

//				"-t",
//				"FOND",
//				"-policytype", "STRONG_CYCLIC",

				"benchmarks/acrobatics/domain.pddl",
				"benchmarks/acrobatics/p3.pddl",

//				"benchmarks/beam-walk/domain.pddl",
//				"benchmarks/beam-walk/p8.pddl",

//				"benchmarks/blocksworld-2/domain.pddl",
//				"benchmarks/blocksworld-2/p3.pddl",

//				"benchmarks/blocksworld-new/domain.pddl",
//				"benchmarks/blocksworld-new/p4.pddl",

//				"benchmarks/blocksworld-original/domain.pddl",
//				"benchmarks/blocksworld-original/p11.pddl",

//				"benchmarks/chain-of-rooms/domain.pddl",
//				"benchmarks/chain-of-rooms/p3.pddl",

//				"benchmarks/doors/domain.pddl",
//				"benchmarks/doors/p2.pddl",

//				"benchmarks/earth-observation/domain.pddl",
//				"benchmarks/earth-observation/p16.pddl",

//				"benchmarks/elevators/domain.pddl",
//				"benchmarks/elevators/p10.pddl",

//				"benchmarks/faults/d37.pddl",
//				"benchmarks/faults/p37.pddl",

//				"benchmarks/first-responders/domain.pddl",
//				"benchmarks/first-responders/fr-p_4_4.pddl",

//				"benchmarks/islands/domain.pddl",
//				"benchmarks/islands/p35.pddl",

//				"benchmarks/miner/domain.pddl",
//				"benchmarks/miner/p17.pddl",

//				"benchmarks/tireworld-spiky/domain.pddl",
//				"benchmarks/tireworld-spiky/p8.pddl",

//				"benchmarks/tireworld-truck/domain.pddl",
//				"benchmarks/tireworld-truck/p15.pddl",

//				"benchmarks/triangle-tireworld/domain.pddl",
//				"benchmarks/triangle-tireworld/p5.pddl",

//				"benchmarks/zenotravel/domain.pddl",
//				"benchmarks/zenotravel/p1.pddl",

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
	
	@Test
	public void testAcrobatics1() {
		String[] args = new String[] {

				"-search", "DFS",

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
//				"-successorsExploration", "REVERSE",
//				"-successorsExploration", "RANDOM",
				
				"-tieBreakConnectors", "MIN_SUM",
//				"-tieBreakConnectors", "MAX_SUM",
//				"-tieBreakConnectors", "MIN_OUTCOMES_SIZE",
//				"-tieBreakConnectors", "MAX_OUTCOMES_SIZE",
//				"-tieBreakConnectors", "MIN_H_TIMES_OUTCOMES_SIZE",
//				"-tieBreakConnectors", "MAX_H_TIMES_OUTCOMES_SIZE",
//				"-tieBreakConnectors", "MIN_MAX_H_TIMES_OUTCOMES_SIZE",

//				"-debug", "ON",

				"-timeout", "1200",

//				"-validatePolicyPRP", "ON",

//				"-t",
//				"FOND",
//				"-policytype", "STRONG_CYCLIC",

				"benchmarks/acrobatics/domain.pddl",
				"benchmarks/acrobatics/p2.pddl",

//				"benchmarks/beam-walk/domain.pddl",
//				"benchmarks/beam-walk/p8.pddl",

//				"benchmarks/blocksworld-2/domain.pddl",
//				"benchmarks/blocksworld-2/p3.pddl",

//				"benchmarks/blocksworld-new/domain.pddl",
//				"benchmarks/blocksworld-new/p4.pddl",

//				"benchmarks/blocksworld-original/domain.pddl",
//				"benchmarks/blocksworld-original/p11.pddl",

//				"benchmarks/chain-of-rooms/domain.pddl",
//				"benchmarks/chain-of-rooms/p3.pddl",

//				"benchmarks/doors/domain.pddl",
//				"benchmarks/doors/p2.pddl",

//				"benchmarks/earth-observation/domain.pddl",
//				"benchmarks/earth-observation/p16.pddl",

//				"benchmarks/elevators/domain.pddl",
//				"benchmarks/elevators/p10.pddl",

//				"benchmarks/faults/d37.pddl",
//				"benchmarks/faults/p37.pddl",

//				"benchmarks/first-responders/domain.pddl",
//				"benchmarks/first-responders/fr-p_4_4.pddl",

//				"benchmarks/islands/domain.pddl",
//				"benchmarks/islands/p35.pddl",

//				"benchmarks/miner/domain.pddl",
//				"benchmarks/miner/p17.pddl",

//				"benchmarks/tireworld-spiky/domain.pddl",
//				"benchmarks/tireworld-spiky/p8.pddl",

//				"benchmarks/tireworld-truck/domain.pddl",
//				"benchmarks/tireworld-truck/p15.pddl",

//				"benchmarks/triangle-tireworld/domain.pddl",
//				"benchmarks/triangle-tireworld/p5.pddl",

//				"benchmarks/zenotravel/domain.pddl",
//				"benchmarks/zenotravel/p1.pddl",

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
	
	@Test
	public void testBeamWalk0() {
		String[] args = new String[] {

				"-search", "DFS",

//				"-heuristic", "PDBS",
//				"-heuristic", "FF",
				"-heuristic", "HMAX",
//				"-heuristic", "HADD",
//				"-heuristic", "LMCUT",
//				"-heuristic", "BLIND_DEADEND",
//				"-heuristic", "BLIND",

				"-evaluationFunctionCriterion", "MIN",
//				"-evaluationFunctionCriterion", "MAX",

				"-actionSelectionCriterion", "MIN_H",
//				"-actionSelectionCriterion", "MIN_MAX_H",
				
				"-successorsExploration", "SORT",
//				"-successorsExploration", "REVERSE",
//				"-successorsExploration", "RANDOM",
				
				"-tieBreakConnectors", "MIN_SUM",
//				"-tieBreakConnectors", "MAX_SUM",
//				"-tieBreakConnectors", "MIN_OUTCOMES_SIZE",
//				"-tieBreakConnectors", "MAX_OUTCOMES_SIZE",
//				"-tieBreakConnectors", "MIN_H_TIMES_OUTCOMES_SIZE",
//				"-tieBreakConnectors", "MAX_H_TIMES_OUTCOMES_SIZE",
//				"-tieBreakConnectors", "MIN_MAX_H_TIMES_OUTCOMES_SIZE",

//				"-debug", "ON",

				"-timeout", "1200",

//				"-validatePolicyPRP", "ON",

//				"-t",
//				"FOND",
//				"-policytype", "STRONG_CYCLIC",

				"benchmarks/beam-walk/domain.pddl",
				"benchmarks/beam-walk/p2.pddl",

//				"benchmarks/blocksworld-2/domain.pddl",
//				"benchmarks/blocksworld-2/p3.pddl",

//				"benchmarks/blocksworld-new/domain.pddl",
//				"benchmarks/blocksworld-new/p4.pddl",

//				"benchmarks/blocksworld-original/domain.pddl",
//				"benchmarks/blocksworld-original/p11.pddl",

//				"benchmarks/chain-of-rooms/domain.pddl",
//				"benchmarks/chain-of-rooms/p3.pddl",

//				"benchmarks/doors/domain.pddl",
//				"benchmarks/doors/p2.pddl",

//				"benchmarks/earth-observation/domain.pddl",
//				"benchmarks/earth-observation/p16.pddl",

//				"benchmarks/elevators/domain.pddl",
//				"benchmarks/elevators/p10.pddl",

//				"benchmarks/faults/d37.pddl",
//				"benchmarks/faults/p37.pddl",

//				"benchmarks/first-responders/domain.pddl",
//				"benchmarks/first-responders/fr-p_4_4.pddl",

//				"benchmarks/islands/domain.pddl",
//				"benchmarks/islands/p35.pddl",

//				"benchmarks/miner/domain.pddl",
//				"benchmarks/miner/p17.pddl",

//				"benchmarks/tireworld-spiky/domain.pddl",
//				"benchmarks/tireworld-spiky/p8.pddl",

//				"benchmarks/tireworld-truck/domain.pddl",
//				"benchmarks/tireworld-truck/p15.pddl",

//				"benchmarks/triangle-tireworld/domain.pddl",
//				"benchmarks/triangle-tireworld/p5.pddl",

//				"benchmarks/zenotravel/domain.pddl",
//				"benchmarks/zenotravel/p1.pddl",

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
	
	@Test
	public void testBeamWalk1() {
		String[] args = new String[] {

				"-search", "DFS",

//				"-heuristic", "PDBS",
				"-heuristic", "FF",
//				"-heuristic", "HMAX",
//				"-heuristic", "HADD",
//				"-heuristic", "BLIND_DEADEND",
//				"-heuristic", "BLIND",

				"-evaluationFunctionCriterion", "MIN",
//				"-evaluationFunctionCriterion", "MAX",

				"-actionSelectionCriterion", "MIN_H",
//				"-actionSelectionCriterion", "MIN_MAX_H",
				
//				"-successorsExploration", "SORT",
				"-successorsExploration", "REVERSE",
//				"-successorsExploration", "RANDOM",
				
//				"-tieBreakConnectors", "MIN_SUM",
				"-tieBreakConnectors", "MAX_SUM",
//				"-tieBreakConnectors", "MIN_OUTCOMES_SIZE",
//				"-tieBreakConnectors", "MAX_OUTCOMES_SIZE",
//				"-tieBreakConnectors", "MIN_H_TIMES_OUTCOMES_SIZE",
//				"-tieBreakConnectors", "MAX_H_TIMES_OUTCOMES_SIZE",
//				"-tieBreakConnectors", "MIN_MAX_H_TIMES_OUTCOMES_SIZE",

//				"-debug", "ON",

				"-timeout", "1200",

//				"-validatePolicyPRP", "ON",

//				"-t",
//				"FOND",
//				"-policytype", "STRONG_CYCLIC",

				"benchmarks/beam-walk/domain.pddl",
				"benchmarks/beam-walk/p5.pddl",

//				"benchmarks/blocksworld-2/domain.pddl",
//				"benchmarks/blocksworld-2/p3.pddl",

//				"benchmarks/blocksworld-new/domain.pddl",
//				"benchmarks/blocksworld-new/p4.pddl",

//				"benchmarks/blocksworld-original/domain.pddl",
//				"benchmarks/blocksworld-original/p11.pddl",

//				"benchmarks/chain-of-rooms/domain.pddl",
//				"benchmarks/chain-of-rooms/p3.pddl",

//				"benchmarks/doors/domain.pddl",
//				"benchmarks/doors/p2.pddl",

//				"benchmarks/earth-observation/domain.pddl",
//				"benchmarks/earth-observation/p16.pddl",

//				"benchmarks/elevators/domain.pddl",
//				"benchmarks/elevators/p10.pddl",

//				"benchmarks/faults/d37.pddl",
//				"benchmarks/faults/p37.pddl",

//				"benchmarks/first-responders/domain.pddl",
//				"benchmarks/first-responders/fr-p_4_4.pddl",

//				"benchmarks/islands/domain.pddl",
//				"benchmarks/islands/p35.pddl",

//				"benchmarks/miner/domain.pddl",
//				"benchmarks/miner/p17.pddl",

//				"benchmarks/tireworld-spiky/domain.pddl",
//				"benchmarks/tireworld-spiky/p8.pddl",

//				"benchmarks/tireworld-truck/domain.pddl",
//				"benchmarks/tireworld-truck/p15.pddl",

//				"benchmarks/triangle-tireworld/domain.pddl",
//				"benchmarks/triangle-tireworld/p5.pddl",

//				"benchmarks/zenotravel/domain.pddl",
//				"benchmarks/zenotravel/p1.pddl",

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

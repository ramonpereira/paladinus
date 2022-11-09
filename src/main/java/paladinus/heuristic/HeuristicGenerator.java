package paladinus.heuristic;

import java.util.List;

import paladinus.Global;
import paladinus.heuristic.FFHeuristic.RPGStrategy;
import paladinus.heuristic.pdb.PatternCollectionSearch;
import paladinus.problem.Problem;

public abstract class HeuristicGenerator {

	public static Heuristic getHeuristic(Problem problem, String heuristicName) {
		return getHeuristic(problem, heuristicName, null);
	}

	public static Heuristic getHeuristic(Problem problem, String heuristicName, List<String> heuristicNames) {
		Heuristic heuristic = null;
		switch (heuristicName) {
		
		case "BLIND":
			System.out.println("Heuristic: Blind Heuristic.");
			heuristic = new BlindHeuristic(problem);
			break;
		case "BLIND_DEADEND":
			System.out.println("Heuristic: Blind (Dead-End) Heuristic.");
			heuristic = new BlindDeadEndHeuristic(problem);
			break;
			
		case "PDBS":
			System.out.println("Heuristic: Canonical PDB Heuristic.");
			heuristic = new PatternCollectionSearch(problem).search();
			break;
		
		case "FF":
			System.out.println("Heuristic: FF Heuristic.");
			heuristic = new FFHeuristic(problem, RPGStrategy.FF);
			break;
		case "HADD":
			System.out.println("Heuristic: ADD Heuristic.");
			heuristic = new FFHeuristic(problem, RPGStrategy.ADD);
			break;	
		case "HMAX":
			System.out.println("Heuristic: HMAX Heuristic.");
			heuristic = new HMaxHeuristic(problem);
			break;
		
		case "LMCUT":
			System.out.println("Heuristic: LM-Cut Heuristic.");
			heuristic = new LMCutHeuristic(problem);
			break;
		default:
			new Exception("Unknown Heuristic Estimator.").printStackTrace();
			Global.ExitCode.EXIT_CRITICAL_ERROR.exit();
		}
		return heuristic;
	}
}

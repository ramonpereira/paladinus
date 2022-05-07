package paladinus;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import args4j.CmdLineException;
import args4j.CmdLineParser;
import args4j.ParserProperties;
import paladinus.Global.ExitCode;
import paladinus.heuristic.Heuristic;
import paladinus.heuristic.HeuristicGenerator;
import paladinus.parser.SasParser;
import paladinus.problem.Problem;
import paladinus.search.AbstractSearch;
import paladinus.search.AbstractSearch.Result;
import paladinus.search.dfs.DepthFirstSearch;
import paladinus.search.dfs.iterative.IterativeDepthFirstSearch;
import paladinus.search.dfs.iterative.IterativeDepthFirstSearchPruning;
import paladinus.simulator.PlanSimulator;

/**
 * Paladinus is a FOND planner based myND planner.
 * 
 * Paladinus takes as input a description of a FOND planning task and tries to solve it using an appropriate heuristic.
 *
 * Original authors (myND):
 * @author Robert Mattmueller
 * @author Manuela Ortlieb
 * 
 * @author Ramon Fraga Pereira
 */
public class PaladinusPlanner {

	/**
	 * The problem used with the planner right now (usually parsed from a file).
	 */
	protected Problem problem;

	/**
	 * Search algorithm which is used for solving the problem.
	 */
	private AbstractSearch search = null;

	/**
	 * Start time of the planner. Parsing and initialization of the planning problem
	 * is not measured.
	 */
	private static long startTime;

	/**
	 * Time used for preprocessing of the used heuristic.
	 */
	private static long timeUsedForPreprocessing;

	/**
	 * Time used in overall by runSearchDo()
	 */
	private static long timeUsedOverall;

	/**
	 * For unit tests.
	 */
	public static boolean testMode = false;

	/**
	 * Set DEBUG true for more output.
	 */
	public static final boolean DEBUG = false;

	/**
	 * Create a planner to solve a specific planning task.
	 *
	 * @param args planning options and SAS file to solve
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public PaladinusPlanner(String[] args) throws FileNotFoundException, IOException {
		new Global().initialize();
		initialize(args);
	}
	
	public PaladinusPlanner() {}

	/**
	 * Main method expecting a single command line argument, the name of the input SAS file.
	 *
	 * @param args Command line arguments.
	 * @throws IOException
	 * @throws FileNotFoundException
	 */
	public static void main(String[] args) throws IOException, FileNotFoundException {
		if(args == null || args.length == 0) {
			System.err.println("Error: Invalid or empty arguments!");
			System.err.println(
					  "- Example (0): java [java_options] paladinus.PaladinusPlanner -search ITERATIVE_DFS -heuristic FF benchmarks/blocksworld-sas/blocksworld_p1.sas \n"
					+ "- Example (1): java [java_options] paladinus.PaladinusPlanner -search ITERATIVE_DFS -heuristic FF -t benchmarks/blocksworld-new/domain.pddl benchmarks/blocksworld-new/pb1.pddl \n" 
					+ "- Example (2): java -jar paladinus.jar -search ITERATIVE_DFS -heuristic FF -t benchmarks/blocksworld-sas/blocksworld_p1.sas \n"
					+ "- Example (3): java -jar paladinus.jar -search ITERATIVE_DFS -heuristic FF -t benchmarks/blocksworld-new/domain.pddl benchmarks/blocksworld-new/pb1.pddl");
		} else {
			Locale.setDefault(Locale.US);
			Result result = new PaladinusPlanner(args).runProblem();
			assert (result != null);

			switch (result) {
			case PROVEN:
				ExitCode.EXIT_PROVEN.exit();
			case DISPROVEN:
				ExitCode.EXIT_DISPROVEN.exit();
			default:
				ExitCode.EXIT_UNPROVEN.exit();
			}			
		}
	}

	/**
	 * Print some Garbage Collector stats.
	 */
	public void printGCStats() {
		long totalGarbageCollections = 0;
		long garbageCollectionTime = 0;
		for (GarbageCollectorMXBean gc : ManagementFactory.getGarbageCollectorMXBeans()) {
			long count = gc.getCollectionCount();
			if (count >= 0)
				totalGarbageCollections += count;

			long time = gc.getCollectionTime();

			if (time >= 0)
				garbageCollectionTime += time;
		}
		System.out.println("Total Garbage Collections: " + totalGarbageCollections);
		System.out.println("Total Garbage Collection Time: " + garbageCollectionTime / 1000 + " seconds.");
	}

	private void executeTranslator() {
		String domain = new File(Global.options.getDomainFilename()).getAbsolutePath();
		String instance = new File(Global.options.getInstanceFilename()).getAbsolutePath();
		String translator;
		if (Global.options.type == Options.Type.FOND) {
			translator = new File("translator-fond/translate.py").getAbsolutePath();
		} else {
			System.err.println("Translate type not specified");
			return;
		}
		try {
			Process translate_p = new ProcessBuilder(translator, domain, instance).start();
			InputStream is = translate_p.getInputStream();
			InputStreamReader isr = new InputStreamReader(is);
			BufferedReader br = new BufferedReader(isr);
			String line;
			System.out.printf("Output of running %s is:\n", translator);
			while ((line = br.readLine()) != null) {
				System.out.println(line);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Initialize Paladinus by parsing planning options and the SAS-file.
	 * 
	 * @param args
	 * @throws IOException
	 * @throws FileNotFoundException
	 */
	private void initialize(String[] args) throws FileNotFoundException, IOException {
		/* Create either a partially observable or a fully observable problem. */
		assert problem == null;
		CmdLineParser parser = new CmdLineParser(Global.options, ParserProperties.defaults().withOptionSorter(null));
		Global.options.setParser(parser);
		try {
			parser.parseArgument(args);
			Global.options.setDefaults();
			Global.options.parseArgs();
			if (Global.options.getDomainFilename() != null && Global.options.getInstanceFilename() != null) {
				executeTranslator();
			}
			Global.options.checkOptions();
		} catch (CmdLineException e) {
			/* Handling of wrong arguments. */
			System.err.println(e.getMessage() + "\n");
			Global.options.help = true;
		}
		if (Global.options.help) {
			Global.options.printHelp();
			Global.ExitCode.EXIT_INPUT_ERROR.exit();
		} else {
			/* Create either a partially observable or a fully observable problem. */
			if (DEBUG)
				System.err.println("Start parsing given planning task.");
			problem = new SasParser().parse(new FileInputStream(Global.options.getSASFilename()));
		}
		assert problem != null;

		/* Do operator preprocessing respectively initialization of BDDs. */
		problem.finishInitializationAndPreprocessing();
		if (DEBUG) 
			problem.dump();
	}

	/**
	 * Run an appropriate heuristic search algorithm on the given problem.
	 *
	 * @return true iff a solution is found.
	 */
	public Result runProblem() {
		Result planFound = runProblemWithoutStats();
		assert problem != null;

		if (planFound == Result.PROVEN) {
			System.out.println("INITIAL IS PROVEN!");
			if(Global.options.validatePolicyPRP() || Global.options.validatePolicy()) {
				search.validatePolicy();
				if(search.getPolicy().isValid())
					System.out.println("Result: Strong cyclic policy found.");
				else System.out.println("Result: No strong cyclic policy found.");
			} else {
				System.out.println("\nResult: Strong cyclic policy found.");
			}
		} else if (planFound == Result.DISPROVEN) {
			System.out.println("INITIAL IS DISPROVEN!");
			System.out.println("\nResult: No strong cyclic policy found.");
		} else if (planFound == Result.TIMEOUT) {
			System.out.println("INITIAL IS UNPROVEN!");
			System.out.println("\nResult: No policy found due to time-out.");
		} else if (planFound == Result.OUT_OF_MEMORY) {
			System.out.println("INITIAL IS UNPROVEN!");
			System.out.println("\nResult: No policy could be found due to out-of-memory.");			
		} else if (planFound == Result.UNDECIDED) {
			System.out.println("INITIAL IS UNPROVEN!");
			System.out.println("\nResult: No policy found. Undecided.");
		} else {
			assert planFound == Result.EXPANDED_ALL;
			System.out.println("\nResult: Search-space exapanded.");
		}
		System.out.println();
		System.out.println("Time needed for preprocess (Parsing, PDBs, ...):    " + timeUsedForPreprocessing / 1000.0 + " seconds.");
		System.out.println("Time needed for search:                             " + (timeUsedOverall - timeUsedForPreprocessing) / 1000.0 + " seconds.");
		System.out.println("Time needed:                                        " + timeUsedOverall / 1000.0 + " seconds.");
		printGCStats();
		System.out.println();
		search.printStats(Global.options.computeCosts);
		if (planFound == Result.PROVEN) {
			System.out.println("# Total Time                = " + (timeUsedOverall / 1000.0) + " seconds.");
			System.out.println();
			if (Global.options.exportPolicyFilename != null)
				search.printPolicy(Global.options.exportPolicyFilename);
			
			if (Global.options.exportDotFilename != null) {
				System.out.println("@> Dot file: " + Global.options.exportDotFilename);
				PlanSimulator.savePlanAsDot(problem, search.getPolicy(), Global.options.exportDotFilename);
			}
			/* Extract and dump policy. */
			System.out.println("\n# Strong Cyclic Policy: \n");
			if (Global.options.dumpPolicy) {
				search.dumpPolicy();
			}
		}
		return planFound;
	}

	@SuppressWarnings("unchecked")
	public Result runProblemWithoutStats() {
		assert problem != null;

		/* Start measuring of preprocessing time. */
		System.out.println();
		startTime = System.currentTimeMillis();

		Result planFound = Result.UNDECIDED;
		
		Heuristic heuristic = null;
		boolean heuristicConstructed = false;
		heuristic = HeuristicGenerator.getHeuristic(problem, Global.options.heuristic.toString());
		heuristicConstructed = true;
		
		if (!heuristicConstructed) {
			/* Unsolvable problem detected during heuristic construction. */
			planFound = Result.DISPROVEN;
		} else {
			if (DEBUG)
				System.out.print("Running Garbage Collection ... ");
			long gc_start = System.currentTimeMillis();
			System.gc();
			if (DEBUG)
				System.out.println(String.format("Done, took %.2f s.", (System.currentTimeMillis() - gc_start) / 1000.0) + "\n");
			
			switch (Global.options.searchAlgorithm) {
				case DFS:
					System.out.println("Algorithm: Depth-First Search for FOND Planning");
					if(heuristic != null)
						search = new DepthFirstSearch(problem, heuristic, Global.options.actionSelectionCriterion, Global.options.evaluationFunctionCriterion);
					break;
					
				case ITERATIVE_DFS:
					System.out.println("Algorithm: Iterative Depth-First Search for FOND Planning");
					if(heuristic != null)
						search = new IterativeDepthFirstSearch(problem, heuristic, Global.options.actionSelectionCriterion, Global.options.evaluationFunctionCriterion);
					break;
					
				case ITERATIVE_DFS_PRUNING:
					System.out.println("Algorithm: Iterative Depth-First Search Pruning for FOND Planning");
					if(heuristic != null)
						search = new IterativeDepthFirstSearchPruning(problem, heuristic, Global.options.actionSelectionCriterion, Global.options.evaluationFunctionCriterion, Global.options.checkSolvedStates);
					break;
					
				default:
					new Exception("Unknown Search Algorithm.").printStackTrace();
					Global.ExitCode.EXIT_CRITICAL_ERROR.exit();
			}

			/* Finish measuring of preprocessing time. */
			timeUsedForPreprocessing = System.currentTimeMillis() - startTime;

			/* Set timeout for search. */
			long t = Global.options.timeout - timeUsedForPreprocessing;
			if (t <= 0) {
				new Exception("Results: Timeout occurs during preprocessing.").printStackTrace();
				Global.ExitCode.EXIT_UNPROVEN.exit();
			}
			search.setTimeout(Global.options.timeout - timeUsedForPreprocessing);
			
			ExecutorService service = Executors.newFixedThreadPool(1);
		    Future<Result> futureResult = service.submit(search);
		    try{
		        planFound = futureResult.get(Global.options.timeout, TimeUnit.MILLISECONDS);
		    }catch(TimeoutException e){
		    	planFound = Result.TIMEOUT;
		        futureResult.cancel(true);
		    } catch (InterruptedException e) {
		    	planFound = Result.TIMEOUT;
				e.printStackTrace();
			} catch (ExecutionException e) {
				planFound = Result.OUT_OF_MEMORY;
				if(e.getCause() instanceof NullPointerException)
					planFound = Result.TIMEOUT;
				e.printStackTrace();
			}catch (OutOfMemoryError e) {
				e.printStackTrace();
			}
		    service.shutdown();
		}
		/* Stop measuring search time. */
		timeUsedOverall = System.currentTimeMillis() - startTime;
		System.out.println("\nTotal Memory (GB) = " + ((Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory())/ (1024.0 * 1024.0 * 1024.0)) + "\n");
		assert planFound != null;
		return planFound;
	}

	/**
	 * Get start time of the planner.
	 *
	 * @return start time
	 */
	public static long getStartTime() {
		return startTime;
	}

	/**
	 * Get preprocessing time of the used heuristic.
	 *
	 * @return preprocessing time
	 */
	public static long getTimeUsedForPreprocessing() {
		return timeUsedForPreprocessing;
	}

	/**
	 * Change the problem searched on. Used by CegarSearch.
	 */
	public void setProblem(Problem problem) {
		this.problem = problem;
	}

	public Problem getProblem() {
		return problem;
	}

	/**
	 * Return the used search algorithm. Useful for reuse of the solution graph.
	 *
	 * @return search
	 */
	public AbstractSearch getSearch() {
		return search;
	}

	/**
	 * Get name of problem instance.
	 *
	 * @return name of problem instance
	 */
	public static String getNameOfProblemInstance() {
		String[] str = Global.options.getSASFilename().split("/");
		return str[str.length - 1].replace(".sas", "");
	}
}

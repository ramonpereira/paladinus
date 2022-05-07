package paladinus;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import args4j.Argument;
import args4j.CmdLineException;
import args4j.CmdLineParser;
import args4j.Option;
import args4j.OptionHandlerFilter;
import args4j.spi.OptionHandler;
import paladinus.heuristic.HeuristicEstimator;
import paladinus.heuristic.pdb.PDB;
import paladinus.heuristic.pdb.PatternCollectionSearch.PatternSearch;
import paladinus.parser.SasParser;
import paladinus.search.AbstractSearch;
import paladinus.search.SearchAlgorithm;

/**
 *
 * @author Manuela Ortlieb
 *
 */
public class Options {

	/**
	 * The parser which uses this options.
	 */
	CmdLineParser parser;

	/**
	 * Sets the parser.
	 *
	 * @param parser
	 */
	void setParser(CmdLineParser parser) {
		this.parser = parser;
	}

	enum Type {
		FOND
	};

	enum Bool {
		ON, OFF
	};

	/**
	 * Denotes the planning type: FOND
	 */
	Type planningTask = Type.FOND;

	/**
	 * The defined groups are used for a proper arranged usage print and for
	 * checking dependencies between options. For example, if an option is in the
	 * group AO, the search algorithm has to be AO* search.
	 */
	enum Group {
		HELP {
			@Override
			public String getException() {
				return null;
			}
		},
		MAIN {
			@Override
			public String getException() {
				return null;
			}
		},
		SEARCH {
			@Override
			public String getException() {
				return null;
			}
		},
		USEMAX {
			@Override
			public String getException() {
				return null;
			}
		},		
		CRITERIA {
			@Override
			public String getException() {
				return null;
			}
		},
		HEURISTICS {
			@Override
			public String getException() {
				return null;
			}
		},
		TRANSLATE {
			@Override
			public String getException() {
				return null;
			}
		},
		HEURISTIC {
			@Override
			public String getException() {
				return null;
			}
		},
		GENERAL {
			@Override
			public String getException() {
				return null;
			}
		},
		UTILS {
			@Override
			public String getException() {
				return null;
			}
		},
		FOND {
			@Override
			public boolean check() {
				return Global.options.planningTask == Type.FOND;
			}

			@Override
			public String getException() {
				return "a FOND planning task";
			}
		},
		PDB {
			@Override
			public boolean check() {
				return Global.options.heuristic == HeuristicEstimator.PDBS;
			}

			@Override
			public String getException() {
				return "PDB heuristic";
			}
		};

		public boolean check() {
			return true;
		}

		public abstract String getException();

	};

	/**
	 * Denotes if Paladinus prints the help message and finishes after that.
	 */
	@Option(name = "-h", aliases = { "-help" }, usage = "print this message", groups = { "HELP" }, help = true)
	boolean help = false;

	/**
	 * A hidden option to print all hidden options. :) Indeed only hidden options
	 * with "usage" are printed.
	 */
	@Option(name = "-hidden", usage = "print hidden options", hidden = true, help = true)
	boolean hiddenOptions = false;

	private void checkHiddenOptions() {
		if (hiddenOptions) {
			help = true;
		}
	}
	
	@Option(name = "-debug", hidden = true, usage = "use debug option.", groups = {"GENERAL"})
	Bool debug = Bool.OFF;
	
	public boolean debug() {
		return debug == Bool.ON;
	}

	@Option(name = "-t", aliases = "-type", usage = "use fond translate (Example: -t <domain_file> <problem_file>)", help = true, groups = { "TRANSLATE" })
	Type type;

	public Type getType() {
		return type;
	}

	/**
	 * Arguments given by user. If no help option is used, then the SAS+ file is
	 * required or domain and instance pddl
	 */

	private String domain;
	private String instance;

	private String sas;
	@Argument(required = false)
	String[] args;

	public void parseArgs() {
		assert args.length <= 2;
		assert args.length >= 1;
		if (args.length == 1) {
			sas = args[0];
		} else {
			domain = args[0];
			instance = args[1];
			assert type != null;
			sas = "output.sas"; // result of translator
		}
	}

	public String getDomainFilename() {
		return domain;
	}

	public String getInstanceFilename() {
		return instance;
	}

	public String getSASFilename() {
		return sas;
	}

	// //////////////// Planner options //////////////////

	//@Option(name = "-computeCosts", usage = "compute and print expected costs of the plan", groups = { "MAIN" })
	public boolean computeCosts = false;

	@Option(name = "-printPolicy", usage = "print policy to stdout", groups = { "MAIN" })
	public boolean dumpPolicy = false;

	private void testFilename(String filename, String option) throws CmdLineException {
		if (filename.startsWith("-") || (filename.endsWith(".sas") && args == null)) {
			throw new CmdLineException(parser, new Exception("option " + option + " requires a filename. Use " + option
					+ "= or " + option + " \"\" to get a default filename"));
		}
	}

	@Option(name = "-exportPolicy", usage = "export policy to file", groups = { "MAIN" }, metaVar = "FILENAME")
	String exportPolicyFilename = null;

	private void checkExportPlanFilename() throws CmdLineException {
		if (exportPolicyFilename != null) {
			testFilename(exportPolicyFilename, "-exportPolicy");
		}
	}

	/**
	 * Export the .dot output from the PlanSimulator to this file.
	 */
	@Option(name = "-exportDot", usage = "export polity as DOT graph (GraphViz)", groups = { "MAIN" }, metaVar = "FILENAME")
	String exportDotFilename = null;
	
	public String getExportDotFilename() {
		return exportDotFilename;
	}

	private void checkExportDot() throws CmdLineException {
		if (exportDotFilename != null) {
			testFilename(exportDotFilename, "-exportDot");
		}
	}

	/**
	 * Timeout for the planner. The user gives the timeout in seconds. Internally we
	 * use milliseconds.
	 */
	@Option(name = "-timeout", usage = "set timeout in seconds", groups = { "MAIN" })
	Long timeout = null;

	private void checkPlannerTimeout() throws CmdLineException {
		if (timeout == null) {
			timeout = AbstractSearch.NO_TIMEOUT;
		} else {
			// Timeout has been set by user (in seconds).
			if (timeout < 1) {
				throw new CmdLineException(parser, new Exception("a timeout of " + timeout + " s does not make sense"));
			}
			timeout = timeout * 1000;
		}
	}

	@Option(name = "-as", aliases = "-actionSelectionCriterion", usage = "set actionSelectionCriterion", groups = { "CRITERIA" }, showDefault = true)
	String actionSelectionCriterion = "MIN_MAX_H";

	public String getActionSelectionCriterion() {
		return actionSelectionCriterion;
	}
	
	@Option(name = "-ef", aliases = "-evaluationFunctionCriterion", usage = "set evaluationFunctionCriterion", groups = { "CRITERIA" }, showDefault = true)
	String evaluationFunctionCriterion = "MAX";

	public String getEvaluationFunctionCriterion() {
		return evaluationFunctionCriterion;
	}
	
	@Option(name = "-cs", aliases = "-checkSolvedStates", usage = "set checkSolvedStates", groups = { "UTILS" }, showDefault = true)
	String checkSolvedStates = null;

	public String getCheckSolvedStates() {
		return checkSolvedStates;
	}
	
	@Option(name = "-pr", aliases = "-pruning", usage = "set pruning", groups = { "UTILS" }, showDefault = true)
	String pruning = null;

	public String getPruning() {
		return this.pruning;
	}

	@Option(name = "-hs", aliases = "-heuristics", usage = "set heuristics", groups = {"HEURISTICS" }, showDefault = true)
	String heuristics = null;
	
	@Option(name = "-heuristic", usage = "set heuristic", groups = { "HEURISTIC", "FOND" }, showDefault = true)
	HeuristicEstimator heuristic = HeuristicEstimator.FF;

	public String getHeuristics() {
		return heuristics;
	}

	@Option(name = "-s", aliases = "-search", usage = "set search algorithm", groups = { "SEARCH", "FOND" }, showDefault = true)
	SearchAlgorithm searchAlgorithm = SearchAlgorithm.ITERATIVE_DFS;
	
	/**
	 * Get the search algorithm.
	 *
	 * @return search algorithm
	 */
	public SearchAlgorithm getSearchAlgorithm() {
		return searchAlgorithm;
	}

	@Option(name = "-validatePolicy", hidden = true, usage = "validate policy using our validator.", groups = {"GENERAL"})
	Bool validatePolicy = Bool.OFF;
	
	public boolean validatePolicy() {
		return validatePolicy == Bool.ON;
	}
	
	@Option(name = "-validatePolicyPRP", hidden = true, usage = "validate policy using PRP validator.", groups = {"GENERAL"})
	Bool validatePolicyPRP = Bool.OFF;
	
	public boolean validatePolicyPRP() {
		return validatePolicyPRP == Bool.ON;
	}
	
	@Option(name = "-useClosedVistedNodes", hidden = true, usage = "use the closed-set of visited nodes when estimating the policy size.", groups = {"GENERAL"})
	Bool useClosedVistedNodes = Bool.OFF;
	
	public boolean useClosedVistedNodes() {
		return useClosedVistedNodes == Bool.ON;
	}
	
	public void useClosedVistedNodes(boolean value) {
		this.useClosedVistedNodes = (value ? Bool.ON : Bool.OFF);
	}
	
	@Option(name = "-useMaxChildNodesToMarkBestActions", hidden = true, usage = "use the cost estimate among the child nodes when perfoming Value Iteration. The default setting is using the average cost estimate for the child nodes.", groups = {"GENERAL"})
	Bool useMaxChildNodes = Bool.OFF;
	
	public boolean useMaxChildNodes() {
		return useMaxChildNodes == Bool.ON;
	}
	
	public void setUseMaxChildNodes(boolean value) {
		this.useMaxChildNodes = (value ? Bool.ON : Bool.OFF);
	}
	
	@Option(name = "-useMaxHeuristicAndAvgConnectors2SelectBestActions", hidden = true, usage = "use the max value between H and the average of the connectors to select the best actions.", groups = {"GENERAL"})
	Bool useMaxHeuristicAndAvgConnectors2SelectBestActions = Bool.OFF;
	
	public boolean useMaxHeuristicAndAvgConnectors2SelectBestActions() {
		return useMaxHeuristicAndAvgConnectors2SelectBestActions == Bool.ON;
	}
	
	@Option(name = "-policytype", aliases = "-policytype", usage = "set policytype", groups = {"GENERAL" })
	String policyType = null;
	
	public String getPolicyType() {
		return policyType;
	}
	
	public void setPolicyType(String policyType) {
		this.policyType = policyType;
	}
	
	// //////////////// PDB options //////////////////

	@Option(name = "-patternSearch", usage = "set type of pattern search", showDefault = true, groups = { "FOND", "PDB" })
	PatternSearch patternSearch = PatternSearch.FO;

	private void checkPatternSearch() throws CmdLineException {
		switch (patternSearch) {
		case NONE:
			if (hillClimbingSteps != 0 && hillClimbingSteps != Integer.MAX_VALUE) {
				throw new CmdLineException(parser, new Exception(
						"performing none pattern search implies that the number of hill climbing steps has to be 0"));
			}
			hillClimbingSteps = 0;
			if (planningTask == Type.FOND) {
				PDB.buildExplicitPDBs = true;
			} else {
				PDB.buildExplicitPDBs = false;
			}
			break;
		case FO:
			PDB.buildExplicitPDBs = true;
			break;
		default:
			assert false;
		}
	}

	public PatternSearch patternSearch() {
		return patternSearch;
	}

	@Option(name = "-steps", usage = "set maximal number of hill climbing iterations in pattern search", groups = {
			"PDB", "FOND" }, showDefault = true)
	int hillClimbingSteps = Integer.MAX_VALUE;

	private void checkSteps() throws CmdLineException {
		if (hillClimbingSteps < 0) {
			throw new CmdLineException(parser,
					new Exception("number of hill climbing iterations has to be non-negative"));
		}
		if (hillClimbingSteps == 0) {
			patternSearch = PatternSearch.NONE;
		}
	}

	public int getNumHillClimbingSteps() {
		return hillClimbingSteps;
	}

	/**
	 * Timeout for the pattern collection search. The user gives the timeout in
	 * seconds. Internally we use milliseconds.
	 */
	@Option(name = "-pdbTimeout", usage = "set timeout in seconds for the pattern search", groups = { "PDB",
			"FOND" }, showDefault = true)
	private int pdbTimeout = 600;

	private void checkPDBTimeout() throws CmdLineException {
		if (pdbTimeout < 1) {
			throw new CmdLineException(parser,
					new Exception("a PDB timeout of " + pdbTimeout + " s does not make sense"));
		}
	}

	/**
	 * Get timeout for pattern search.
	 *
	 * @return timeout in ms
	 */
	public long getPDBTimeout() {
		return pdbTimeout * 1000;
	}

	@Option(name = "-pdbMaxSize", usage = "set maximal number of abstract states induced by a single pattern", groups = {
			"PDB", "FOND" }, showDefault = true)
	int pdbMaxSize = -1;

	private void setDefaultPDBMaxSize() {
		if (pdbMaxSize == -1) {
			pdbMaxSize = 50000;
			setDefaultTextToOption("50000", "pdbMaxSize");
		}
	}

	private void checkPDBMaxSize() throws CmdLineException {
		if (pdbMaxSize < 2) {
			throw new CmdLineException(parser, new Exception("pdbMaxSize of " + pdbMaxSize + " does not make sense"));
		}
	}

	public int pdbMaxSize() {
		return pdbMaxSize;
	}

	@Option(name = "-pdbsMaxSize", usage = "set maximal number of abstract states of all patterns", groups = { "FOND", "PDB" }, showDefault = true)
	int pdbsOverallMaxSize = -1;

	private void setDefaultPDBsMaxSize() {
		assert pdbMaxSize > -1 : "default of pdbMaxSize has to be assigned before";
		if (pdbsOverallMaxSize == -1) {
			pdbsOverallMaxSize = 10 * pdbMaxSize;
		}
		setDefaultTextToOption("10 * pdbMaxSize", "pdbsMaxSize");
	}

	private void checkPDBsMaxSize() throws CmdLineException {
		if (pdbsOverallMaxSize < 2) {
			throw new CmdLineException(parser,
					new Exception("pdbsMaxSize of " + pdbsOverallMaxSize + " does not make sense"));
		}
	}

	public int pdbsMaxSize() {
		return pdbsOverallMaxSize;
	}

	@Option(name = "-minImprovement", usage = "set fraction of required improvers to continue pattern search", groups = {
			"PDB", "FOND" }, showDefault = true, metaVar = "X")
	double minImprovement = 0.1;

	public double getMinImprovementFraction() {
		return minImprovement;
	}

	@Option(name = "-greedyImprovement", usage = "set faction for required improvers to take the pattern immediately", hidden = true, groups = { "FOND", "PDB" }, showDefault = true)
	double greedyImprovement = 1.0;

	private void checkMinImprovementAndGreedyImprovement() throws CmdLineException {
		if (minImprovement < 0 || minImprovement > 1) {
			throw new CmdLineException(parser,
					new Exception(minImprovement + " is not a valid ratio for minImprovement"));
		}
		if (greedyImprovement < 0 || greedyImprovement > 1) {
			throw new CmdLineException(parser,
					new Exception(greedyImprovement + " is not a valid ratio for greedyImprovement"));
		}
		if (greedyImprovement < minImprovement) {
			throw new CmdLineException(parser,
					new Exception("greedyImprovement ratio has to be greater or equal to the minImprovement ratio"));
		}
	}

	public double getGreedyImprovementFraction() {
		return greedyImprovement;
	}

	@Option(name = "-cachePDBs", usage = "set caching of PDBs on/off", groups = { "PDB", "FOND" }, showDefault = true)
	Bool cachePDBs = Bool.ON;

	public boolean cachePDBs() {
		return cachePDBs == Bool.ON;
	}

	@Option(name = "-randomWalkSamples", usage = "set number of samples that are collected during random walks", groups = { "PDB", "FOND" }, showDefault = true)
	int randomWalkSamples = 1000;

	private void checkRandomWalkSamples() throws CmdLineException {
		if (randomWalkSamples < 1) {
			throw new CmdLineException(parser, new Exception("at least one sample has to be specified"));
		}
	}

	public int getNumberOfRandomWalkSamples() {
		return randomWalkSamples;
	}

	/**
	 * When assuming full observability, explicit states are used instead of belief
	 * states even for problem solving. Only for PDB heuristic, because in FF we
	 * assume full observability automatically and for ZERO it does not make sense.
	 */
	@Option(name = "-assumeFO", usage = "assume full observability for PDB heuristic and search", groups = {"PDB" }, showDefault = true)
	boolean assumeFOforPDBs = false;

	private void checkAssumeFOForPDBs() throws CmdLineException {
		if (assumeFOforPDBs) {
			PDB.buildExplicitPDBs = true;
		}
	}

	public boolean assumeFullObservabilityForPDBs() {
		return assumeFOforPDBs;
	}

	@Option(name = "-useDependencyGraph", usage = "use dependency graph for preconditions of sensing actions (POND only)", groups = {"PDB" }, showDefault = true)
	Bool useDependencyGraph = Bool.ON;

	public boolean useDependencyGraph() {
		return useDependencyGraph == Bool.ON;
	}

	// //////////////// GRID options (hidden, no usage) //////////////////

	/**
	 * This id is appended to written outputs to avoid overwriting outputs from
	 * parallel running Paladinus planners. Useful on GKI grid.
	 */
	@Option(name = "-runID", aliases = { "-runid", "-id" }, hidden = true)
	int runID = -1; // default

	public int getRunID() {
		return runID;
	}

	// //////////////// Print help //////////////////
	/**
	 * Prints the usage text to stderr. Note: Options without "usage" description
	 * are ignored.
	 */
	void printHelp() {
		System.err.println(
				  "- Example (0): java [java_options] paladinus.PaladinusPlanner -search ITERATIVE_DFS -heuristic FF benchmarks/blocksworld-sas/blocksworld_p1.sas \n"
				+ "- Example (1): java [java_options] paladinus.PaladinusPlanner -search ITERATIVE_DFS -heuristic FF -t benchmarks/blocksworld-new/domain.pddl benchmarks/blocksworld-new/pb1.pddl \n" 
				+ "- Example (2): java -jar paladinus.jar -search ITERATIVE_DFS -heuristic FF -t benchmarks/blocksworld-sas/blocksworld_p1.sas \n"
				+ "- Example (3): java -jar paladinus.jar -search ITERATIVE_DFS -heuristic FF -t benchmarks/blocksworld-new/domain.pddl benchmarks/blocksworld-new/pb1.pddl");
		System.err.println();
		if (hiddenOptions) {
			System.err.println("Hidden options:");
			parser.printUsage(System.err, OptionHandlerFilter.HIDDEN);
			System.err.println();
		} else {
			System.err.println("Help:");
			parser.printUsage(System.err, new GroupFilter(Group.HELP));
			System.err.println();
			System.err.println("Paladinus options:");
			parser.printUsage(System.err, new GroupFilter(Group.MAIN));
			System.err.println();
			System.err.println("Search algorithms:");
			parser.printUsage(System.err, new GroupFilter(Group.SEARCH));
			System.err.println();
			System.err.println("Heuristics:");
			parser.printUsage(System.err, new GroupFilter(Group.HEURISTIC));
			System.err.println();
			System.err.println("Action Selection and Evaluation Function Criteria:");
			parser.printUsage(System.err, new GroupFilter(Group.CRITERIA));
			System.err.println();
			System.err.println("Translate:");
			parser.printUsage(System.err, new GroupFilter(Group.TRANSLATE));
			//System.err.println();
			//System.err.println("PDB Heuristic Options:");
			//parser.printUsage(System.err, new GroupFilter(Group.PDB));
		}
		System.err.println();
		System.err.println(
				  "- Example (0): java [java_options] paladinus.PaladinusPlanner -search ITERATIVE_DFS -heuristic FF benchmarks/blocksworld-sas/blocksworld_p1.sas \n"
				+ "- Example (1): java [java_options] paladinus.PaladinusPlanner -search ITERATIVE_DFS -heuristic FF -t benchmarks/blocksworld-new/domain.pddl benchmarks/blocksworld-new/pb1.pddl \n" 
				+ "- Example (2): java -jar paladinus.jar -search ITERATIVE_DFS -heuristic FF -t benchmarks/blocksworld-sas/blocksworld_p1.sas \n"
				+ "- Example (3): java -jar paladinus.jar -search ITERATIVE_DFS -heuristic FF -t benchmarks/blocksworld-new/domain.pddl benchmarks/blocksworld-new/pb1.pddl");

		if (debug()) {
			System.out.println();
			System.out.println("ALL OPTIONS:");
			parser.printUsage(System.out, OptionHandlerFilter.ALL);
		}
	}

	void setDefaults() {
		setDefaultPDBMaxSize();
		setDefaultPDBsMaxSize();
	}

	void checkOptions() throws CmdLineException, FileNotFoundException, IOException {
		checkGroups();
		checkHiddenOptions();
		if (!help) {
			checkExportPlanFilename();
			checkExportDot();
			checkSASFile();
			checkDependencies();
			checkPlannerTimeout();
			checkMinImprovementAndGreedyImprovement();
			checkPDBTimeout();
			checkPDBMaxSize();
			checkPDBsMaxSize();
			checkRandomWalkSamples();
			checkPatternSearch();
			checkSteps();
			checkAssumeFOForPDBs();
		}
	}

	/**
	 * Check if the assigned groups exists.
	 */
	private void checkGroups() {
		Set<String> groupNames = new HashSet<String>();
		for (Group v : Group.values()) {
			groupNames.add(v.name());
		}
		for (OptionHandler<?> h : parser.getOptions()) {
			for (Set<String> groups : h.option.getGroups()) {
				for (String g : groups) {
					if(g.equals("POND"))
						System.out.println(g);
					assert (groupNames.contains(g));
				}
			}
		}
	}

	private void checkDependencies() throws CmdLineException {
		for (OptionHandler<?> h : parser.getOptionsSetByUser()) {
			for (Set<String> groupSet : h.option.getGroups()) {
				boolean valid = false;
				String message = "Option " + h.option + " requires ";
				for (String name : groupSet) {
					valid |= Group.valueOf(name).check();
					message += Group.valueOf(name).getException() + " or ";
				}
				if (!valid) {
					throw new CmdLineException(parser, new Exception(message.substring(0, message.length() - 3)));
				}
			}
		}
	}

	private void checkSASFile() throws CmdLineException, FileNotFoundException, IOException {
		if (args == null) {
			throw new CmdLineException(parser, new Exception("No SAS+ file or " + "PDDL files given."));
		} else if (Global.options.args.length > 2) {
			throw new CmdLineException(parser, new Exception("Too many arguments: " + Arrays.asList(args)));
		}
		// String sasFilename = args[0];
		// Check if given file is a partial observable or a full observable problem.
		boolean fond = new SasParser().isFond(new FileInputStream(this.sas));
		if (fond)
			planningTask = Type.FOND;
		
	}

	private void setDefaultTextToOption(String defaultText, String option) {
		for (OptionHandler<?> handler : parser.getOptions()) {
			if (handler.option.toString().contains(option)) {
				handler.option.setDefault(defaultText);
				return;
			}
		}
		assert false : "option " + option + " not found";
	}

	public class GroupFilter implements OptionHandlerFilter {

		/**
		 * Filters options which are not in all of this groups.
		 */
		private final Set<Group> groups;

		public GroupFilter(Set<Group> groups) {
			this.groups = groups;
		}

		public GroupFilter(Group singleGroup) {
			this(new HashSet<Options.Group>(Arrays.asList(singleGroup)));
		}

		@Override
		public boolean select(OptionHandler<?> o) {
			if (o.option.hidden()) {
				return false;
			}
			for (Group g : groups) {
				boolean found = false;
				for (Set<String> groupSet : o.option.getGroups()) {
					if (groupSet.contains(g.name())) {
						found = true;
						break;
					}
				}
				if (!found) {
					return false;
				}
			}
			return true;
		}

	}
}

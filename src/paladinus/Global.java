package paladinus;

import java.util.Random;

import paladinus.heuristic.pdb.PDB;
import paladinus.heuristic.pdb.RandomWalk;
import paladinus.search.AbstractNode;

/**
 * Class used to hold static global settings.
 *
 * @author Robert Mattmueller
 * @author Manuela Ortlieb
 */
public class Global {

	public enum ExitCode {
		EXIT_PROVEN { // task solved, plan found
			@Override
			public void exit() {
				System.exit(0);
			}
		},
		EXIT_CRITICAL_ERROR {
			@Override
			public void exit() {
				System.exit(1);
			}
		},
		EXIT_INPUT_ERROR {
			@Override
			public void exit() {
				System.exit(2);
			}
		},
		EXIT_UNSUPPORTED {
			@Override
			public void exit() {
				System.exit(3);
			}
		},
		EXIT_DISPROVEN { // task provably unsolvable
			@Override
			public void exit() {
				System.exit(4);
			}
		},
		EXIT_UNPROVEN { // timeout, task not solved
			@Override
			public void exit() {
				System.exit(5);
			}
		},
		EXIT_OUT_OF_MEMORY {
			@Override
			public void exit() {
				System.exit(6);
			}
		};

		public abstract void exit();
	};

	/**
	 * Options given by the user (or defaults).
	 */
	public static Options options;

	/**
	 * To use everywhere in the code, where a random number is needed.
	 */
	public static Random generator;

	/**
	 * Initialize and reset static members.
	 */
	void initialize() {
		options = new Options();
		generator = new Random();
		PDB.buildExplicitPDBs = false;
		AbstractNode.resetIndex();
		RandomWalk.reset();
	}

}

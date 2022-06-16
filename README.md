# Paladinus

Paladinus: An Iterative Depth-First Search FOND Planner.

Paladinus has two iterative depth-first search (IDFS) algorithms for FOND Planning:
- IDFS (Iterative Depth-First Search);
- IDFS Pruning (Iterative Depth-First Search with Pruning);

## Usage

```bash

Help:
 -h (-help)                             : print this message

Paladinus options:
 -printPolicy                           : print policy to stdout
 -exportPolicy FILENAME                 : export policy to file
 -exportDot FILENAME                    : export polity as DOT graph (GraphViz)
 -timeout N                             : set timeout in seconds

Search algorithms:
 -s (-search) [ITERATIVE_DFS | ITERATIVE_DFS_PRUNING | DFS]  : set search algorithm [default: ITERATIVE_DFS]

Heuristics:
 -heuristic [HMAX | HADD | FF | PDBS | LMCUT | BLIND | BLIND_DEADEND] : set heuristic [default: FF]            

Action Selection and Evaluation Function Criteria:
 -as (-actionSelectionCriterion) [MIN_H | MIN_MAX_H | MEAN_H | MIN_SUM_H]  : set actionSelectionCriterion [default: MIN_MAX_H]
 -ef (-evaluationFunctionCriterion) [MAX | MIN]                            : set evaluationFunctionCriterion [default: MAX]
```

## Usage Examples

```bash
Examples of usage by calling the class:
- Example (0): java [java_options] paladinus.PaladinusPlanner -search ITERATIVE_DFS -heuristic FF benchmarks/blocksworld-sas/blocksworld_p1.sas -printPolicy
- Example (1): java [java_options] paladinus.PaladinusPlanner -search ITERATIVE_DFS -heuristic FF benchmarks/blocksworld-new/domain.pddl benchmarks/blocksworld-new/p1.pddl -printPolicy

Examples of usage by calling the .JAR file:
- Example (0): java -jar [java_options] paladinus1.0.jar -search ITERATIVE_DFS -heuristic FF benchmarks/blocksworld-sas/blocksworld_p1.sas -printPolicy
- Example (1): java -jar [java_options] paladinus1.0.jar -search ITERATIVE_DFS -heuristic FF benchmarks/blocksworld-new/domain.pddl benchmarks/blocksworld-new/p1.pddl -printPolicy
```

## IDFS Usage

```bash
- Example (0): java -jar [java_options] paladinus1.0.jar -search ITERATIVE_DFS -heuristic HMAX benchmarks/blocksworld-sas/blocksworld_p2.sas -printPolicy
- Example (1): java -jar [java_options] paladinus1.0.jar -search ITERATIVE_DFS -heuristic HMAX benchmarks/blocksworld-new/domain.pddl benchmarks/blocksworld-new/p2.pddl -printPolicy
```

## IDFS Pruning Usage
```bash
- Example (0): java -jar [java_options] paladinus1.0.jar -search ITERATIVE_DFS_PRUNING -heuristic HADD benchmarks/blocksworld-sas/blocksworld_p3.sas -printPolicy
- Example (1): java -jar [java_options] paladinus1.0.jar -search ITERATIVE_DFS_PRUNING -heuristic HADD benchmarks/blocksworld-new/domain.pddl benchmarks/blocksworld-new/p3.pddl -printPolicy
```

## Policy Output and Visualization

Paladinus outputs a policy when one exists for FOND planning task.
We provide two types of outputs: 
- A text file (policy.txt), containing the output policy that maps states into actions; and 
- A graph visualization for the output policy (policy.dot);

Our policy visualization outputs a DOT file that corresponds to a graph of the solution policy.

For more details, have a look at [POLICY.md](POLICY.md).

## Debug

Paladinus provides a state-space debugger that shows the expanded states for every iteration of our algorithms.
Our state-space debugger outputs a DOT file for every state expansion during the search.

For more details, have a look at [DEBUG.md](DEBUG.md).

## Requirements

### Java JDK

We use the Java JDK 1.6.

### Python

We use Python 2.7.16 for [translator-fond](translator-fond/).

### Commons-IO

We use [lib/commons-io-2.11.0.jar](lib/commons-io-2.11.0.jar).

### GraphViz

We use [GraphViz](https://graphviz.readthedocs.io/) for visualizing the state-space and the output policy.

## License

This software is released under the GNU Lesser General Public License version 3 or later.

## Contributors

- Ramon Fraga Pereira
- Frederico Messa
- André Grahl Pereira
- Giuseppe De Giacomo

## Reference and Citation

Please, use the following reference when citing Paladinus.

- [_Iterative Depth-First Search for FOND Planning_](https://ojs.aaai.org/index.php/ICAPS/article/view/19789/19548), Ramon Fraga Pereira, André Grahl Pereira, Frederico Messa, and Giuseppe De Giacomo. [International Conference on Automated Planning and Scheduling (ICAPS), 2022](http://icaps22.icaps-conference.org). 
  - You can find the BibTex [here](idfs-paladinus-icaps22.bib)!

## Acknowledgements

This work has been partially funded by the [ERC Advanced Grant "WhiteMech"](whitemech.github.io/)
(No. 834228) and by the [TAILOR research network](https://tailor-network.eu/) (No. 952215).

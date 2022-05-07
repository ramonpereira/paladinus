# PaladinusPlanner

Paladinus: An Iterative Depth-First Search FOND Planner.

## Usage

```bash
Examples of usage:
- Example (0): java [java_options] paladinus.PaladinusPlanner -search ITERATIVE_DFS -heuristic FF benchmarks/blocksworld-sas/blocksworld_p1.sas -printPolicy
- Example (1): java [java_options] paladinus.PaladinusPlanner -search ITERATIVE_DFS -heuristic FF benchmarks/blocksworld-new/domain.pddl benchmarks/blocksworld-new/p1.pddl -printPolicy
- Example (2): java -jar [java_options] paladinus0.1.jar -search ITERATIVE_DFS -heuristic FF benchmarks/blocksworld-sas/blocksworld_p1.sas -printPolicy
- Example (3): java -jar [java_options] paladinus0.1.jar -search ITERATIVE_DFS -heuristic FF benchmarks/blocksworld-new/domain.pddl benchmarks/blocksworld-new/p1.pddl -printPolicy

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

## Usage Iterative DFS

```bash
```

## Usage Iterative DFS with Pruning
```bash
```

## Requirements

### Java JDK

We use the Java JDK 1.6.

### Python

We use Python 2.7.16 for [translator-fond](translator-fond/).

### Commons-IO

We use [lib/commons-io-2.11.0.jar](lib/commons-io-2.11.0.jar).

## License

This software is released under the GNU Lesser General Public License version 3 or later.

## Contributors

- Ramon Fraga Pereira
- Frederico Messa
- André Grahl Pereira

## Acknowledgements

This work has been partially funded by the [ERC Advanced Grant "WhiteMech"](whitemech.github.io/)
(No. 834228) and by the [TAILOR research network](https://tailor-network.eu/) (No. 952215).
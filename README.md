# Paladinus: An Iterative DFS FOND Planner

**Paladinus** is an Iterative Depth-First Search FOND planner written in Java. It has two iterative depth-first search (IDFS) algorithms for FOND Planning:

1. IDFS (Iterative Depth-First Search).
2. IDFS Pruning (Iterative Depth-First Search with Pruning).

## Requirements

* Java JDK 16.0.1+ with the following libraries (obtained via Maven, but available under `lib/`for manual compile):
  * [Commons-IO](https://commons.apache.org/proper/commons-io/).
  * [junit 4](https://junit.org/junit4/).
  * [args4j](https://args4j.kohsuke.org/).
* Python 3.6+ for [translator-fond](/translator-fond/) tool to translate FOND PDDL files to SAS.
* [GraphViz](https://graphviz.readthedocs.io/) for visualizing the state-space and the output policy.
* [Maven](https://maven.apache.org/) for building the system (and further development, if needed).

## Build (and test)

The planner is set-up as a Java Maven project. So, the easiest way to build and test is as follows (to skip the tests add `-Dmaven.test.skip.exec`):

```shell
$ mvn clean package
```

This will generate a JAR files (with and without dependencies) under `target/` and all classes under `target/classes`.

We can test it by getting the help with `-h` (or `-help`) using the provided script or directly via Java (using the generated packaged JAR file or complied classes):

```shell
$ # using the provided bash script
$ ./paladinus -h

Paladinus: An Iterative Depth-First Search FOND Planner

 -h (--help)                            : print this message (default: true)
 -debug [ON | OFF]                      : use debug option (default: OFF)
 -t (-type) [FOND]                      : use fond translate (Example: -t FOND
                                          <domain_file> <problem_file>)
                                          (default: FOND)
 -printPolicy                           : print policy to stdout (default:
                                          false)
 -exportPolicy FILENAME                 : export policy to file
 -exportDot FILENAME                    : export policy as DOT graph (GraphViz)
 -translatorPath DIRNAME                : path to SAS translator script
                                          (default: ./translator-fond/translate.
                                          py)
 -timeout N                             : set timeout in seconds
 -as (-actionSelectionCriterion) VAL    : set actionSelectionCriterion
                                          (default: MIN_MAX_H)
...
...
```

Check below for running the system directly via the JAR file or compiled classes (mostly when developing further the system).

## Usage

Paladinus can be run via script `paladinus` and accepts either SAS files or domain and problem PDDL files as input, with the following patterns:

```shell
$ # SAS file as input
$ /path/to/paladinus/paladinus [planner_options] <problem.sas>

or 

$ # PDDL files as input
$ /path/to/paladinus/paladinus [planner_options] <domain.pddl> <problem.pddl>
```

There are two main planner options:

* `-search`: algorithm to use (default is `ITERATIVE_DFS`).
* `-heuristic`: heuristic to use (default is `FF`).

Other options, including those for reporting the solution policy, can be found via help option `-h`.

For example, to run plain Iterative DFS with HMAX heuristics and print the policy:

```shell
$ # Using SAS inputs
$ ./paladinus -search ITERATIVE_DFS -heuristic HMAX -printPolicy \
    benchmarks/blocksworld-sas/blocksworld_p2.sas

$ # Using PDDL inputs
$ ./paladinus -search ITERATIVE_DFS -heuristic HMAX -printPolicy \
    benchmarks/blocksworld-original/domain.pddl \
    benchmarks/blocksworld-original/p2.pddl
```

To run IDFS Pruning, use `-search ITERATIVE_DFS_PRUNING` and `-heuristic HADD`.

```shell
$ ./paladinus -search ITERATIVE_DFS_PRUNING -heuristic HADD -printPolicy \
    benchmarks/blocksworld-original/domain.pddl \
    benchmarks/blocksworld-original/p10.pddl
```

**Note:** By using the provided script, the planner can be called from any directory, not just the root of the planner. See below under for more details when developing.

## Policy Output and Visualization

Paladinus outputs a policy when one exists for FOND planning task.
We provide two types of outputs:

- A text file (`policy.txt`), containing the output policy that maps states into actions; and
- A graph visualization for the output policy (`policy.dot`);

Our policy visualization outputs a [DOT file](https://en.wikipedia.org/wiki/DOT_(graph_description_language)) that corresponds to a graph of the solution policy.

To report the policy solution, one can use the following options:

* `-printPolicy`: prints the policy to standard output.
* `-exportPolicy FILENAME`: exports the policy to a `.txt` file.
* `-exportDot FILENAME`: exports the graph visualization file (DOT graph using GraphViz) of the policy.

For more details, check [POLICY.md](POLICY.md).

## Developing

When developing in a Maven aware IDE (e.g., VSCODE) it may be convenient to run the planner using the compiled classes in `target/classes` (instead of the JAR file) and the main class `paladinus.PaladinusPlanner` , as these will be automatically re-generated by the IDE when there is a change in source code:

```shell
$ java [java_options] -cp ./target/classes/:lib/commons-io-2.11.0.jar:lib/args4j-2.33.jar  paladinus.PaladinusPlanner \
    [planner_options] \
    (<domain.pddl> <problem.pddl> OR <problem.sas>)
```

We can also run the system directly from the Maven produced JAR file (which includes all dependencies):

```shell
$ # using the packaged JAR (includes all dependencies)
$ java [java_options] -jar target/paladinus-1.1-jar-with-dependencies.jar \
    [planner_options] \
    (<domain.pddl> <problem.pddl> OR <problem.sas>)
```

Useful Java Options are:

* `-Xmx4g`: to set the maximum heap space to 4 GB.
* `-ea`: to enable assertions.

Note that these commands need to be run from the system root folder when the input are PDDL files. This is because the planner uses the Python FOND translator under  [translator-fond/](translator-fond/) and it will assume it is in the "current" dir.

### SAS translator

When given PDDL files as input, the planner first translates them to a SAS representation using Python script [`translate.py`](translator-fond/translate.py). By default, the system will assume the planner is being run from its root directory and hence will look for such script in the current dir.

To run it from a directory different than that of the planner, we can use option `-translatorPath` to specify the location of the Python translator:

```shell
-translatorPath $HOME/planners/paladinus.git/translator-fond/translate.py
```

Note that the shell scripts provided already have that built-in so they can be run from anywhere. Furthermore, a script  [`./paladinus-dev`](./paladinus-dev) is provided for development that uses the generated classes under `target/classes` (often re-generated by an IDE after a change in source code) rather than the JAR file.

### Debugger

Paladinus provides a **state-space debugger** that shows the expanded states for every iteration of our algorithms. The state-space debugger outputs a [DOT file](https://en.wikipedia.org/wiki/DOT_(graph_description_language)) for every state expansion during the search. For more details, have a look at [DEBUG.md](DEBUG.md).

## Contributors

- Ramon Fraga Pereira
- Frederico Messa
- André Grahl Pereira
- Giuseppe De Giacomo
- Sebastian Sardiña

## Citation

Please, use the following reference when citing Paladinus:

- [Iterative Depth-First Search for FOND Planning](https://ojs.aaai.org/index.php/ICAPS/article/view/19789/19548), Ramon Fraga Pereira, André Grahl Pereira, Frederico Messa, and Giuseppe De Giacomo. [The 32nd International Conference on Automated Planning and Scheduling (ICAPS), 2022](http://icaps22.icaps-conference.org). 
  - You can find the BibTex [here](idfs-paladinus-icaps22.bib)!

### Acknowledgements

This work has been partially funded by the [ERC Advanced Grant "WhiteMech"](whitemech.github.io/)
(No. 834228) and by the [TAILOR research network](https://tailor-network.eu/) (No. 952215).

## License

This software is released under the GNU Lesser General Public License version 3 or later.


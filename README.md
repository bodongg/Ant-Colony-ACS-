# Ant Colony ACS Simulator

A Java Swing application for visualizing and comparing Ant Colony System (ACS) with an improved Ant Colony Optimization (IACO) variant on a randomly generated Traveling Salesperson Problem (TSP).

The simulator displays the generated network, the best tour found, how the best tour cost changes over iterations, and the final pheromone values on each edge. It can run the baseline and improved variants one at a time or in sequence for comparison.

## Features

- Generate a complete, symmetric TSP graph with 5 to 40 nodes and random edge distances from 10 to 99.
- Configure the number of ants and maximum iterations.
- Adjust alpha, beta, pheromone evaporation, pseudo-random selection threshold, IACO mutation probability, and top-L ant count.
- Run baseline ACO, IACO, or both sequentially.
- View the best tour on the network graph.
- Compare best-cost convergence curves and inspect the pheromone heatmap.
- Stop a run, reset results, or generate a new random problem.

## How it works

Each ant builds a tour by choosing unvisited nodes. Choices use pheromone strength and inverse edge distance, controlled by alpha and beta. The pseudo-random parameter q0 controls the balance between selecting the strongest next edge and sampling from the available choices.

After each iteration, pheromone evaporates and ants deposit pheromone along selected tours. In the baseline ACO mode, only the best ant from that iteration deposits pheromone. In IACO mode, the top-L ants deposit rank-weighted pheromone, tours receive a local pheromone update while being built, and a swap-based mutation tries to improve the current best tour.

Both variants are stochastic heuristics. They can find short tours quickly, but they do not guarantee the globally shortest tour.

## Requirements

- Java Development Kit (JDK) 9 or newer
- No external libraries

## Important: fix the Java source filename

The repository currently names the source file `Antcolony.java`, but its public class is named `IACO_Final_GUI`. Java requires those names to match. Rename the file to:

```text
IACO_Final_GUI.java
```

Then compile it from the folder containing the renamed file:

```bash
javac IACO_Final_GUI.java
```

Run the simulator:

```bash
java IACO_Final_GUI
```

On Windows PowerShell, the same commands are:

```powershell
javac IACO_Final_GUI.java
java IACO_Final_GUI
```

## Using the simulator

1. Set the node count in the **Problem** section. Choose **New Problem** to generate a new random graph.
2. Adjust the ant count and iteration limit.
3. Set the pheromone and selection parameters. The sliders show the active values.
4. Click **ACO only** or **IACO only** to run one algorithm, or **Run Both** to run the baseline first and IACO after it.
5. Compare the best path cost, average cost over the last 20 iterations, convergence iteration, and runtime in the **Results** panel.
6. Use the tabs to inspect the network graph, convergence curve, and pheromone map.

## Parameters

| Parameter | Meaning |
|---|---|
| Nodes | Number of locations in the generated TSP graph (5–40) |
| Ants | Number of candidate tours built during each iteration (3–30) |
| Max iterations | Maximum number of optimization iterations (50–800) |
| Alpha (α) | Influence of pheromone on the next-node choice |
| Beta (β) | Influence of edge distance on the next-node choice |
| Evaporation (ρ) | Rate at which existing pheromone fades |
| Pseudo-rand (q₀) | Chance of choosing the strongest available next edge instead of sampling |
| Top-L ants | Number of high-ranking ants that deposit pheromone in IACO |
| Mutation probability | Chance that IACO tries a swap-based tour improvement |

## Notes

- The graph is generated inside the application; the current UI does not load a custom graph from a file.
- The **Stop** button stops the active phase. In **Run Both**, stopping the baseline phase may still allow the IACO phase to begin afterward.
- The displayed runtime and tour costs depend on the random graph, random choices, and selected parameters.

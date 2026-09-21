An Improved Ant Colony Optimization (IACO) Algorithm


Overview


This repository features an advanced metaheuristic optimization approach designed to solve complex combinatorial problems such as the Mobile Agent Routing Problem (MARP). While the baseline Ant Colony System (ACS) is effective, it frequently suffers from premature convergence and gets trapped in local minima because its updates rely entirely on a single best ant.


This repository features an advanced metaheuristic optimization approach designed to solve complex combinatorial problems such as the Mobile Agent Routing Problem (MARP). While the baseline Ant Colony System (ACS) is effective, it frequently suffers from premature convergence and gets trapped in local minima because its updates rely entirely on a single best ant.


Key Enhancements We Made


To elevate the performance of the traditional Ant Colony System, we implemented two major improvements:
Genetic Mutation Operator: When the algorithm detects stagnation—signaling that it has fallen into a suboptimal local minimum—it triggers a stochastic mutation step. By evaluating pairwise node swaps on the current best tour, the algorithm can actively jump out of local traps and resume its convergence toward the global optimum.


Revised Global Update Rule (Multi-Ant Update): Instead of restricting pheromone reinforcement strictly to the single best ant, our improved algorithm allows the top-$l$ best ants from each iteration to simultaneously update the pheromone matrix. Each ant's contribution is inversely weighted by its rank, which maintains a more balanced and diverse pheromone landscape and prevents premature convergence.


Performance Highlights


Through rigorous experimental testing on MARP instances ($n = 30$ host computers) and benchmark functions, our enhanced IACO algorithm demonstrates exceptional performance gains over the baseline approach: 
Faster Execution: Achieved a 2.54x speedup in wall-clock execution time (down to 156.03 seconds from 397.13 seconds).
Fewer Iterations: Converged in 6,909 iterations compared to the baseline's 8,147 iterations.   
Superior Solution Quality: Improved the optimal solution cost to 121 (an 8.3% improvement over the baseline's 132) and reduced the normal error rate to 0.04.

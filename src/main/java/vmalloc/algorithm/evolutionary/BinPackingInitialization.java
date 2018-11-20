/*
 * VMAlloc, Copyright (c) 2018, Miguel Terra-Neves, Vasco Manquinho, Ines Lynce
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package vmalloc.algorithm.evolutionary;

import org.moeaframework.core.Solution;

import vmalloc.algorithm.BestFitDecreasingAlloc;
import vmalloc.algorithm.BinPackingAllocAlgorithm;
import vmalloc.algorithm.FirstFitDecreasingAlloc;
import vmalloc.domain.MappingVec;
import vmalloc.domain.VirtualMachineVec;
import vmalloc.utils.IOUtils;
import vmalloc.utils.MiscUtils;

/**
 * This class implements an initialization operator for Virtual Machine Consolidation problems that performs
 * bin-packing in order to produce a set of feasible solutions. Randomization is accomplished by considering
 * virtual machines in random orders. When it is not possible to maintain feasibility, leftover virtual
 * machines are mapped randomly while still satisfying anti-colocation and platform constraints. Only
 * supports Virtual Machine Consolidation problems.
 * @author Miguel Terra-Neves
 */
public class BinPackingInitialization extends FeasibleInitialization {

    /**
     * The VMC problem instance.
     */
    private final VMCwMProblem instance;
    
    /**
     * Boolean indicating if the bin-packing algorithm should perform migration cost minimization.
     */
    private final boolean min_migrations;
    
    /**
     * Boolean indicating if the initialization operator should apply a mix of bin-packing algorithms.
     */
    private final boolean mix;
    
    /**
     * Counts the number of solutions that have been initialized. Used when performing mixed initialization.
     * @see {@link #mix} {@link #attemptFeasibleInitialization(Solution)}
     */
    private int initialized = 0;
    
    /**
     * Creates an instance of the bin-packing initialization operator.
     * @param problem The problem instance.
     * @param populationSize The size of the population.
     * @param min_migrations True if the bin-packing algorithm should minimize migration costs, false
     * otherwise.
     * @param mix True if operator should apply a mix of bin-packing algorithms, false otherwise.
     */
    public BinPackingInitialization(VMCwMProblem problem,
                                    int populationSize,
                                    boolean min_migrations,
                                    boolean mix) {
        super(problem, populationSize);
        this.instance = problem;
        this.min_migrations = min_migrations;
        this.mix = mix;
    }
    
    /**
     * Attempts to initialize a solution as a feasible solution using bin-packing. When it is not possible
     * to maintain feasibility, leftover virtual machines are mapped randomly while still satisfying
     * anti-colocation and platform constraints.
     * @param solution The solution to initialize.
     */
    @Override
    protected void attemptFeasibleInitialization(Solution solution) {
        // Attempt to place using shuffled Bin Packing
        BinPackingAllocAlgorithm alloc = null;
        if (this.mix && this.initialized == 0) {
            alloc = new BestFitDecreasingAlloc(instance);
        }
        else if (this.mix && this.initialized == 1) {
            alloc = new BestFitDecreasingAlloc(instance);
            alloc.setMappings(new MappingVec());
        }
        else {
            alloc = new FirstFitDecreasingAlloc(instance);
            if (!this.min_migrations) {
                alloc.setMappings(new MappingVec());
            }
        }
        alloc.enableVirtualMachineShuffling();
        IOUtils.stdoutDisable();
        alloc.allocate();
        IOUtils.stdoutEnable();
        // Generate solution from Bin Packing result
        MappingVec allocation = null;
        VirtualMachineVec leftover_vms = null;
        if (alloc.foundSolution()) {
            allocation = alloc.getAllocation();
            leftover_vms = new VirtualMachineVec();
        }
        else {
            allocation = alloc.getPartialAllocation();
            leftover_vms = alloc.getLeftoverVirtualMachines();
        }
        MiscUtils.injectPartialAllocationInSolution(this.instance, solution, allocation, leftover_vms);
        ++this.initialized;
    }
    
}

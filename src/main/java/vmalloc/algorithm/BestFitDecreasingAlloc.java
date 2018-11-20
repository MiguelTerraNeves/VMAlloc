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

package vmalloc.algorithm;

import java.util.Comparator;

import vmalloc.algorithm.evolutionary.VMCwMProblem;
import vmalloc.domain.PhysicalMachine;

/**
 * Virtual Machine Consolidation problem solver based on the best-fit decreasing heuristic for bin-packing.
 * @author Miguel Terra-Neves
 */
public class BestFitDecreasingAlloc extends BinPackingAllocAlgorithm {

    /**
     * Creates an instance of a Virtual Machine Consolidation problem solver that relies on the best-fit
     * decreasing heuristic for bin-packing.
     * @param instance The problem instance.
     */
    public BestFitDecreasingAlloc(VMCwMProblem instance) { super(instance); }

    /**
     * Makes the usage information comparator that implements the best-fit decreasing heuristic for
     * bin-packing.
     * @return The usage information comparator.
     */
    @Override
    protected Comparator<UsageInfo> makeUsageInfoComparator() {
        return new Comparator<UsageInfo>() {
            Comparator<PhysicalMachine> cap_comp = makeDecreasingPhysicalMachineComparator();
            public int compare(UsageInfo ui1, UsageInfo ui2) {
                double diff = ui1.getLeftoverCPUPercentile() - ui2.getLeftoverCPUPercentile();
                if (diff == 0.0) {
                    diff = ui1.getLeftoverMemoryPercentile() - ui2.getLeftoverMemoryPercentile();
                    if (diff == 0.0) {
                        return cap_comp.compare(ui1.getPhysicalMachine(), ui2.getPhysicalMachine());
                    }
                }
                assert(diff != 0.0);
                return (diff > 0.0) ? 1 : -1;
            }
        };
    }
    
}

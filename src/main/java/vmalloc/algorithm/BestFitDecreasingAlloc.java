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

package vmalloc.preprocess;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.sat4j.core.Vec;
import org.sat4j.specs.IVec;

import vmalloc.algorithm.AllocAlgorithm;
import vmalloc.algorithm.evolutionary.VMCwMProblem;
import vmalloc.domain.MappingVec;
import vmalloc.domain.PhysicalMachine;
import vmalloc.domain.PhysicalMachineVec;
import vmalloc.domain.VirtualMachine;
import vmalloc.exception.HeuristicReductionFailedException;
import vmalloc.utils.IOUtils;
import vmalloc.utils.MathUtils;
import vmalloc.utils.MiscUtils;

/**
 * Class that implements the heuristic reduction procedure. It uses an allocation algorithm (usually a fast
 * heuristic) to compute an initial allocation. Physical machines not used in that allocation are discarded
 * from the reduced instance.
 * @author Miguel Terra-Neves
 */
public class HeuristicReducer {

    /**
     * The original problem instance.
     */
    private VMCwMProblem instance = null;
    
    /**
     * The allocation algorithm to use for heuristic reduction.
     */
    private AllocAlgorithm reduction_alg = null;
    
    /**
     * Stores the reduced instance after a call to {@link #apply()}.
     */
    private VMCwMProblem reduced_instance = null;

    /**
     * Creates an instance of the heuristic problem reducer.
     * @param instance The original problem instance.
     * @param reduction_alg The allocation algorithm to use for heuristic reduction.
     */
    public HeuristicReducer(VMCwMProblem instance, AllocAlgorithm reduction_alg) {
        this.instance = instance;
        this.reduction_alg = reduction_alg;
    }
    
    /**
     * Retrieves the reduced instance produced on the last call to {@link #apply()}.
     * @return The reduced instance.
     */
    public VMCwMProblem getReducedInstance() {
        assert(this.reduced_instance != null);
        return this.reduced_instance;
    }
    
    /**
     * Applies the heuristic reduction procedure with the allocation algorithm provided in
     * {@link #HeuristicReducer}.
     * @throws HeuristicReductionFailedException If the reduction algorithm fails to find an allocation.
     */
    public void apply() throws HeuristicReductionFailedException {
        System.out.println("c WARNING: timeout ignored when performing heuristic reduction");
        IOUtils.stdoutDisable();
        reduction_alg.allocate();
        IOUtils.stdoutEnable();
        if (reduction_alg.foundSolution()) {
            assert(MiscUtils.allocationIsValid(this.instance, reduction_alg.getAllocations().get(0)));
            BigInteger total_mem_cap = BigInteger.ZERO;
            Map<Integer, Integer> pm_id_to_idx = new HashMap<Integer, Integer>();
            for (int i = 0; i < this.instance.getPhysicalMachines().size(); ++i) {
                pm_id_to_idx.put(
                        new Integer(this.instance.getPhysicalMachines().get(i).getID()), new Integer(i));
                total_mem_cap = total_mem_cap.add(this.instance.getPhysicalMachines().get(i).getMemory());
            }
            MappingVec allocation = reduction_alg.getAllocations().get(0);
            IVec<Set<VirtualMachine>> placement =
                    new Vec<Set<VirtualMachine>>(this.instance.getPhysicalMachines().size());
            for (int i = 0; i < this.instance.getPhysicalMachines().size(); ++i) {
                placement.push(new HashSet<VirtualMachine>());
            }
            for (int i = 0; i < allocation.size(); ++i) {
                PhysicalMachine pm = allocation.get(i).getPhysicalMachine();
                VirtualMachine vm = allocation.get(i).getVirtualMachine();
                int pm_idx = pm_id_to_idx.get(new Integer(pm.getID())).intValue();
                placement.get(pm_idx).add(vm);
            }
            double new_mig_budget = this.instance.getMaxMigrationPercentile();
            MappingVec new_maps = new MappingVec();
            for (int i = 0; i < this.instance.getMappings().size(); ++i) {
                PhysicalMachine pm = this.instance.getMappings().get(i).getPhysicalMachine();
                VirtualMachine vm = this.instance.getMappings().get(i).getVirtualMachine();
                int pm_idx = pm_id_to_idx.get(new Integer(pm.getID())).intValue();
                if (placement.get(pm_idx).contains(vm)) {
                    new_maps.push(this.instance.getMappings().get(i));
                }
                else {
                    new_mig_budget -= MathUtils.toPercentile(vm.getMemory(), total_mem_cap);
                }
            }
            assert(new_mig_budget >= 0.0);
            BigInteger new_total_mem_cap = BigInteger.ZERO;
            PhysicalMachineVec new_pms = new PhysicalMachineVec();
            for (int i = 0; i < this.instance.getPhysicalMachines().size(); ++i) {
                if (placement.get(i).size() > 0) {
                    new_pms.push(this.instance.getPhysicalMachines().get(i));
                    new_total_mem_cap = new_total_mem_cap.add(
                            this.instance.getPhysicalMachines().get(i).getMemory());
                }
            }
            new_mig_budget = MathUtils.scalePercentile(total_mem_cap, new_mig_budget, new_total_mem_cap);
            new_mig_budget = MathUtils.normalizePercentile(new_mig_budget);
            this.reduced_instance =
                    new VMCwMProblem(new_pms, this.instance.getJobs(), new_maps, new_mig_budget);
        }
        else {
            throw new HeuristicReductionFailedException();
        }
    }
    
}

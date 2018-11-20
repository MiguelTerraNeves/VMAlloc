package vmalloc.domain;

import java.math.BigInteger;

/**
 * Visitor object that aggregates the sum of machines' CPU values.
 * @author Miguel Terra-Neves
 */
public class CPUSumAggregator extends MachineProcessor {

    /**
     * Variable that aggregates the CPU total.
     */
    private BigInteger cpu_sum = BigInteger.ZERO;
    
    /**
     * Adds the CPU value of a machine to the current CPU sum.
     * @param m The machine.
     */
    @Override
    public void processMachine(Machine m) { cpu_sum = cpu_sum.add(m.getCPU()); }
    
    /**
     * Retrieves the CPU sum.
     * @return The CPU sum.
     */
    public BigInteger getCPUSum() { return cpu_sum; }
    
}

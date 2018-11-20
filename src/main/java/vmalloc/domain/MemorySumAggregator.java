package vmalloc.domain;

import java.math.BigInteger;

/**
 * Visitor object that aggregates the sum of machines' memory values.
 * @author Miguel Terra-Neves
 */
public class MemorySumAggregator extends MachineProcessor {

    /**
     * Variable that aggregates the memory total.
     */
    private BigInteger mem_sum = BigInteger.ZERO;
    
    /**
     * Adds the memory value of a machine to the current memory sum.
     * @param m The machine.
     */
    @Override
    public void processMachine(Machine m) { mem_sum = mem_sum.add(m.getMemory()); }
    
    /**
     * Retrieves the memory sum.
     * @return The memory sum.
     */
    public BigInteger getMemorySum() { return mem_sum; }
    
}

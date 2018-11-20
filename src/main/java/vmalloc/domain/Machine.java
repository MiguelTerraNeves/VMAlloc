package vmalloc.domain;

import java.math.BigInteger;

/**
 * Abstract machine superclass.
 * @author Miguel Terra-Neves
 */
public abstract class Machine {
    
    /**
     * Machine's CPU property.
     */
    private BigInteger cpu;
    
    /**
     * Machine's RAM memory property.
     */
    private BigInteger mem;
    
    /**
     * Creates an instance of a machine.
     * @param cpu The machine's CPU.
     * @param mem The machine's RAM memory.
     */
    public Machine(BigInteger cpu, BigInteger mem) {
        this.cpu = cpu;
        this.mem = mem;
    }
    
    /**
     * Retrieves the machine's CPU property.
     * @return The machine's CPU.
     */
    public BigInteger getCPU() { return this.cpu; }
    
    /**
     * Retrieves the machine's RAM memory property.
     * @return The machine's RAM memory.
     */
    public BigInteger getMemory() { return this.mem; }
    
    /**
     * Accepts a {@link MachineProcessor} visitor object.
     * @param mp A visitor object.
     */
    public abstract void accept(MachineProcessor mp);
    
}

package vmalloc.domain;

import java.math.BigInteger;

import org.sat4j.specs.IVec;

import vmalloc.utils.CollectionUtils;

/**
 * Vector object for containing physical machines.
 * @author Miguel Terra-Neves
 */
public class PhysicalMachineVec extends MachineVec<PhysicalMachine> {

    private static final long serialVersionUID = 6250248900373913306L;
    
    /**
     * A maximum energy consumption getter visitor object.
     * @author Miguel Terra-Neves
     */
    private class MaxConsumptionGetter extends ValueGetter {
    
        /**
         * Stores the maximum energy consumption of a given physical machine.
         * @param pm The physical machine.
         */
        @Override
        public void processPhysicalMachine(PhysicalMachine pm) {
            setValue(BigInteger.valueOf(pm.getMaxConsumption()));
        }
        
    }
    
    /**
     * Creates an instance of a physical machine vector.
     */
    public PhysicalMachineVec() { super(); }
    
    /**
     * Creates an instance of a physical machine vector with the contents of another vector.
     * @param vec A physical machine vector implementing the {@link IVec} interface.
     */
    public PhysicalMachineVec(IVec<PhysicalMachine> vec) { super(vec); }
    
    /**
     * Creates an instance of a physical machine vector with the contents of an array.
     * @param array The array.
     */
    public PhysicalMachineVec(PhysicalMachine[] array) { super(array); }
    
    /**
     * Retrieves the maximum energy consumption values of the physical machines in the vector.
     * @return Array with the physical machines' maximum energy consumptions in order.
     */
    public long[] getMaxConsumptions() {
        BigInteger[] consumptions = getValues(new MaxConsumptionGetter());
        return CollectionUtils.bigIntegerArrayToLongArray(consumptions);
    }

    @Override
    public void accept(MachineProcessor mp) { mp.processPhysicalMachines(this); }
    
}

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

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

/**
 * Abstract superclass for vectors containing machines.
 * @author Miguel Terra-Neves
 * @param <MachineType> The type of machine.
 */
public abstract class MachineVec<MachineType extends Machine> extends DomainVec<MachineType> {
    
    private static final long serialVersionUID = -12981842624598244L;
    
    /**
     * Abstract superclass for a resource getter visitor object.
     * @author Miguel Terra-Neves
     */
    protected abstract class ValueGetter extends MachineProcessor {
        
        /**
         * The resource value.
         */
        private BigInteger value;
        
        /**
         * Sets the resource value.
         * @param val The new resource value.
         */
        protected void setValue(BigInteger val) { this.value = val; }
        
        /**
         * Retrieves the resource value.
         * @return The resource value.
         */
        public BigInteger getValue() { return this.value; }
        
    }
    
    /**
     * A CPU getter visitor object.
     * @author Miguel Terra-Neves
     */
    private class CPUGetter extends ValueGetter {
        
        /**
         * Stores the CPU of a given machine
         * @param m The machine.
         */
        @Override
        public void processMachine(Machine m) { setValue(m.getCPU()); }
        
    }
    
    /**
     * A memory getter visitor object.
     * @author Miguel Terra-Neves
     */
    private class MemoryGetter extends ValueGetter {
        
        /**
         * Stores the memory of a given machine.
         * @param m The machine.
         */
        @Override
        public void processMachine(Machine m) { setValue(m.getMemory()); }
        
    }
    
    /**
     * Creates an instance of a machine vector.
     */
    public MachineVec() { super(); }
    
    /**
     * Creates an instance of a machine vector with the contents of another vector.
     * @param vec A machine vector implementing the {@link IVec} interface.
     */
    public MachineVec(IVec<MachineType> vec) { super(vec); }
    
    /**
     * Creates an instance of a machine vector with the contents of an array.
     * @param array The array.
     */
    public MachineVec(MachineType[] array) { super(array); }
    
    /**
     * Retrieves resource values of the machines in the vector using a given getter object.
     * @param getter The resource getter.
     * @return Array with the resource values retrieved by the getter in order.
     */
    protected BigInteger[] getValues(ValueGetter getter) {
        BigInteger[] values = new BigInteger[size()];
        for (int i = 0; i < size(); ++i) {
            get(i).accept(getter);
            values[i] = getter.getValue();
        }
        return values;
    }
    
    /**
     * Retrieves the CPU values of the machines in the vector.
     * @return Array with the machines' CPU values in order.
     */
    public BigInteger[] getCPUs() { return getValues(new CPUGetter()); }
    
    /**
     * Retrieves the memory values of the machines in the vector.
     * @return Array with the machines' memory values in order.
     */
    public BigInteger[] getMemories() { return getValues(new MemoryGetter()); }

    /**
     * Accepts a {@link MachineProcessor} visitor object.
     * @param mp A visitor object.
     */
    public abstract void accept(MachineProcessor mp);
    
}

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

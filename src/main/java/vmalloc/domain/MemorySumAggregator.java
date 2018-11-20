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

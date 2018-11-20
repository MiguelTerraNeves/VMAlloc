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
 * Representation of a data center server.
 * @author Miguel Terra-Neves
 */
public class PhysicalMachine extends Machine {

    /**
     * Physical machine's unique id.
     */
    private int id;
    
    /**
     * Energy consumed by the physical machine when its CPU load is at 0% of its capacity.
     */
    private int idle_consume;
    
    /**
     * Energy consumed by the physical machine when its CPU load is at 100% of its capacity.
     */
    private int max_consume;
    
    /**
     * Creates an instance of a physical machine.
     * @param id The physical machine's unique id.
     * @param cpu The physical machine's CPU capacity.
     * @param mem The physical machine's RAM memory capacity.
     * @param idle_consume The physical machine's energy consumption at 0% CPU load.
     * @param max_consume The physical machine's energy consumption at 100% CPU load.
     */
    public PhysicalMachine(int id, BigInteger cpu, BigInteger mem, int idle_consume, int max_consume) {
        super(cpu, mem);
        this.id = id;
        this.idle_consume = idle_consume;
        this.max_consume = max_consume;
    }
    
    /**
     * Retrieves the physical machine's id.
     * @return The physical machine's id.
     */
    public int getID() { return this.id; }
    
    /**
     * Retrieves the energy consumed by the physical machine when its CPU load is at 0% of its
     * capacity.
     * @return The physical machine's energy consumption at 0% CPU load.
     */
    public int getIdleConsumption() { return this.idle_consume; }
    
    /**
     * Retrieves the energy consumed by the physical machine when its CPU load is at 100% of its
     * capacity.
     * @return The physical machine's energy consumption at 100% CPU load.
     */
    public int getMaxConsumption() { return this.max_consume; }
    
    /**
     * Produces and returns a string representation of the physical machine.
     * @return The string representation of the physical machine.
     */
    public String toString() {
        return String.format("Server: id=%d, CPU=%d, RAM=%d, IdleConsumption=%d, MaxConsumption=%d",
                             getID(), getCPU(), getMemory(), getIdleConsumption(), getMaxConsumption());
    }

    @Override
    public void accept(MachineProcessor mp) { mp.processPhysicalMachine(this); }

}

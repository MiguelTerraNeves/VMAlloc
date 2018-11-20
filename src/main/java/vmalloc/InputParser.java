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

package vmalloc;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigInteger;

import vmalloc.domain.Job;
import vmalloc.domain.JobVec;
import vmalloc.domain.Mapping;
import vmalloc.domain.MappingVec;
import vmalloc.domain.PhysicalMachine;
import vmalloc.domain.PhysicalMachineVec;
import vmalloc.domain.VirtualMachine;

/**
 * Class responsible for parsing input files with Virtual Machine Consolidation problem instances.
 * @author Miguel Terra-Neves
 */
public class InputParser {

    /**
     * String representation of {@code true} in the input files.
     */
    private static final String TRUE_STRING = "True";
    
    /**
     * String representation of {@code false} in the input files.
     */
    private static final String FALSE_STRING = "False";
    
    /**
     * The path to the input file.
     */
    private String fname;
    
    /**
     * Stores the physical machines parsed from the input file.
     */
    private PhysicalMachineVec pms;
    
    /**
     * Stores the jobs parsed from the input file.
     */
    private JobVec jobs;
    
    /**
     * Stores the pre-existing allocation parsed from the input file.
     */
    private MappingVec mappings;
    
    /**
     * Creates an instance of an input parser that reads from a given file.
     * @param fname The path to the input file.
     */
    public InputParser(String fname) {
        this.fname = fname;
        this.pms = new PhysicalMachineVec();
        this.jobs = new JobVec();
        this.mappings = new MappingVec();
    }
    
    /**
     * Reads the physical machines from a reader object. It is assumed that the next read operation returns
     * the line with the number of physical machines in the instance.
     * @param reader The reader.
     * @throws IOException If an error occurs reading from the file.
     */
    private void parsePhysicalMachines(BufferedReader reader) throws IOException {
        int npms = Integer.parseInt(reader.readLine().trim());
        for (int i = 0; i < npms; ++i) {
            String line = reader.readLine();
            String[] tokens = line.split(" ");
            assert(tokens.length == 5);
            int id = Integer.parseInt(tokens[0]);
            BigInteger cpu = new BigInteger(tokens[1]);
            BigInteger mem = new BigInteger(tokens[2]);
            int idle_consume = Integer.parseInt(tokens[3]);
            int max_consume = Integer.parseInt(tokens[4]);
            this.pms.push(new PhysicalMachine(id, cpu, mem, idle_consume, max_consume));
        }
    }
    
    /**
     * Parses a set of physical machines from a string of comma-separated physical machine indexes.
     * @param str The string.
     * @return The physical machines in {@code str}.
     */
    private PhysicalMachineVec parseCommaSeparatedPhysicalMachines(String str) {
        String[] tokens = str.split(",");
        PhysicalMachineVec pms = new PhysicalMachineVec();
        assert(tokens.length < this.pms.size());
        for (String token_id : tokens) {
            int pm_id = Integer.parseInt(token_id);
            pms.push(this.pms.get(pm_id));
        }
        return pms;
    }
    
    /**
     * Parses the virtual machines from a reader object. It is assumed that the next read operation returns
     * the line with the number of virtual machines in the instance.
     * @param reader The reader.
     * @throws IOException If an error occurs reading from the file.
     */
    private void parseVirtualMachines(BufferedReader reader) throws IOException {
        int nvms = Integer.parseInt(reader.readLine().trim());
        for (int i = 0; i < nvms; ++i) {
            String line = reader.readLine();
            String[] tokens = line.split(" ");
            assert(tokens.length == 5 || tokens.length == 6);
            int job_id = Integer.parseInt(tokens[0]);
            while (job_id >= this.jobs.size()) {
                this.jobs.push(new Job(job_id));
            }
            int vm_idx = Integer.parseInt(tokens[1]);
            BigInteger cpu = new BigInteger(tokens[2]);
            BigInteger mem = new BigInteger(tokens[3]);
            boolean anti_coloc = TRUE_STRING.equals(tokens[4]);
            assert(anti_coloc || FALSE_STRING.equals(tokens[4]));
            if (tokens.length > 5) {
                PhysicalMachineVec unallowed_pms = parseCommaSeparatedPhysicalMachines(tokens[5]);
                this.jobs.get(job_id).addVirtualMachine(
                        new VirtualMachine(job_id, vm_idx, cpu, mem, anti_coloc, unallowed_pms));
            }
            else {
                this.jobs.get(job_id).addVirtualMachine(
                        new VirtualMachine(job_id, vm_idx, cpu, mem, anti_coloc));
            }
        }
    }
    
    /**
     * Parses the pre-existing allocation from a reader object. It is assumed that the next read operation
     * returns the line with the number of virtual machines mapped to some physical machine in the
     * pre-existing allocation.
     * @param reader The reader.
     * @throws IOException If an error occurs reading from the file.
     */
    private void parseMappings(BufferedReader reader) throws IOException {
        int nmaps = Integer.parseInt(reader.readLine().trim());
        for (int i = 0; i < nmaps; ++i) {
            String line = reader.readLine();
            String[] tokens = line.split(" ");
            assert(tokens.length == 3);
            int job_id = Integer.parseInt(tokens[0]);
            int vm_idx = Integer.parseInt(tokens[1]);
            int pm_id = Integer.parseInt(tokens[2]);
            this.mappings.push(new Mapping(this.jobs.get(job_id).getVirtualMachine(vm_idx),
                                           this.pms.get(pm_id)));
        }
    }
    
    /**
     * Parses a Virtual Machine Consolidation problem instance from the input file provided in
     * {@link #InputParser}.
     * @throws IOException If an error occurs reading from the input file.
     */
    // FIXME: assumes complying input file
    // FIXME: assumes IDs and indexes conforming with the object order in the file
    public void parse() throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(this.fname));
        parsePhysicalMachines(reader);
        parseVirtualMachines(reader);
        parseMappings(reader);
        reader.close();
    }
    
    /**
     * Retrieves the set of physical machines parsed from the input file. It is assumed that {@link #parse()}
     * was invoked already.
     * @return The physical machines.
     */
    public PhysicalMachineVec getPhysicalMachines() { return this.pms; }
    
    /**
     * Retrieves the set of jobs parsed from the input file. It is assumed that {@link #parse()} was invoked
     * already.
     * @return The jobs.
     */
    public JobVec getJobs() { return this.jobs; }
    
    /**
     * Retrieves the pre-existing allocation parsed from the input file. It is assumed that {@link #parse()}
     * was invoked already.
     * @return The pre-existing allocation.
     */
    public MappingVec getMappings() { return this.mappings; }
    
}

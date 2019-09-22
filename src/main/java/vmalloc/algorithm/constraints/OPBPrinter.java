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

package vmalloc.algorithm.constraints;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;

import org.sat4j.core.Vec;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.IVec;
import org.sat4j.specs.IVecInt;

import vmalloc.exception.NotSupportedException;
import vmalloc.utils.CollectionUtils;

/**
 * Printer that prints a Pseudo-Boolean instance in OPB format. Supports multiple objectives.
 * @author Miguel Terra-Neves
 */
public class OPBPrinter extends ConstraintAggregator {
    
    /**
     * Big decimal scale (number of digits to the right of the decimal point) to use when printing
     * coefficients.
     */
    // TODO: make it configurable
    private static final int DECIMAL_SCALE = 5;
    
    /**
     * OPB format representation of greater or equal.
     */
    private static final String GREATER_OR_EQUAL = ">=";
    
    /**
     * OPB format representation of equal.
     */
    private static final String EQUAL = "=";
    
    /**
     * Counts the number of variables in the problem instance.
     */
    private int nvars = 0;
    
    /**
     * Counts the number of constraints in the problem instance.
     */
    private int nconstraints = 0;

    /**
     * {@link StringWriter} object that accumulates the constraints, in the OPB file format, to
     * be included in the OPB file.
     */
    private StringWriter cons_writer = new StringWriter();
    
    /**
     * {@link StringWriter} object that accumulates the objective functions, in the OPB file format, to
     * be included in the OPB file.
     */
    private StringWriter obj_writer = new StringWriter();
    
    /**
     * Indicates if the printed instance is allowed to have decimal coefficients.
     */
    private boolean allow_decimals = false;
    
    /**
     * Creates an instance of an OPB format printer that forces all coefficients to be integer.
     */
    public OPBPrinter() {}
    
    /**
     * Creates an instance of an OPB format printer.
     * @param allow_decimals True if the instance is allowed to have decimal coefficients, false otherwise.
     */
    public OPBPrinter(boolean allow_decimals) { this.allow_decimals = allow_decimals; }
    
    /**
     * Converts a literal to its string representation.
     * @param lit The literal.
     * @return The string representation of literal {@code lit}.
     */
    private String litToString(int lit) { return lit > 0 ? ("x" + lit) : ("~x" + (-lit)); }
    
    /**
     * Writes a Pseudo-Boolean constraint or expression to a given writer object.
     * @param writer The writer object where the constraint is to be written.
     * @param lits The literals.
     * @param coeffs The coefficients.
     * @param rhs The right-hand side if a constraint, null otherwise.
     * @param op The relation operator if a constraint (like "=" or ">="), null otherwise.
     * @param term True if the line should be terminated with ';\n', false otherwise.
     */
    private void writePseudoBoolean(Writer writer,
                                    IVecInt lits,
                                    IVec<BigDecimal> coeffs,
                                    BigDecimal rhs,
                                    String op,
                                    boolean term) {
        assert(lits.size() == coeffs.size());
        assert(op == null || op.equals(GREATER_OR_EQUAL) || op.equals(EQUAL));
        try {
            BigDecimal lim_scale_rhs = null;
            if (op != null) {
                lim_scale_rhs = rhs.setScale(DECIMAL_SCALE, RoundingMode.HALF_EVEN).stripTrailingZeros();
            }
            IVec<BigDecimal> lim_scale_coeffs = new Vec<BigDecimal>(coeffs.size());
            for (int i = 0; i < coeffs.size(); ++i) {
                lim_scale_coeffs.unsafePush(
                        coeffs.get(i).setScale(DECIMAL_SCALE, RoundingMode.HALF_EVEN).stripTrailingZeros());
            }
            if (this.allow_decimals) {
                for (int i = 0; i < lits.size(); ++i) {
                    BigDecimal coeff = lim_scale_coeffs.get(i);
                    writer.write(coeff.toPlainString() + " " + litToString(lits.get(i)) + " ");
                }
                if (op != null) {
                    writer.write(op + " " + lim_scale_rhs.toPlainString());
                }
            }
            else {
                ScaledResult scaled = scaleToInteger(lim_scale_coeffs, lim_scale_rhs);
                IVec<BigInteger> int_coeffs = scaled.getCoefficients();
                for (int i = 0; i < lits.size(); ++i) {
                    BigInteger coeff = int_coeffs.get(i);
                    writer.write(coeff + " " + litToString(lits.get(i)) + " ");
                }
                if (op != null) {
                    writer.write(op + " " + scaled.getRightHandSide());
                }
            }
            writer.write(term ? ";\n" : "");
        }
        catch (IOException e) {
            throw new RuntimeException("Failed to write PseudoBoolean constraint/expression", e);
        }
    }
    
    /**
     * Writes a Pseudo-Boolean constraint in the constraint writer object {@link #cons_writer}.
     * @param lits The constraint's literals.
     * @param coeffs The constraint's coefficients.
     * @param rhs The constraint's right-hand side.
     * @param op The relational operator (like "=" or ">=").
     */
    private void writeConstraint(IVecInt lits, IVec<BigInteger> coeffs, BigInteger rhs, String op) {
        assert(rhs != null && op != null);
        writePseudoBoolean(this.cons_writer,
                           lits,
                           CollectionUtils.bigIntegerVecToBigDecimalVec(coeffs),
                           new BigDecimal(rhs),
                           op,
                           true);
        ++this.nconstraints;
    }
    
    /**
     * Writes a Pseudo-Boolean constraint in the constraint writer object {@link #cons_writer}.
     * @param lits The constraint's literals.
     * @param coeffs The constraint's coefficients.
     * @param rhs The constraint's right-hand side.
     * @param op The relational operator (like "=" or ">=").
     */
    private void writeConstraint(IVecInt lits, IVec<BigDecimal> coeffs, BigDecimal rhs, String op) {
        assert(rhs != null && op != null);
        writePseudoBoolean(this.cons_writer, lits, coeffs, rhs, op, true);
        ++this.nconstraints;
    }
    
    /**
     * Writes a Pseudo-Boolean objective function in the objective writer object {@link #obj_writer}.
     * @param lits The objective's literals.
     * @param coeffs The objective's coefficients.
     */
    private void writeObjective(IVecInt lits, IVec<BigDecimal> coeffs) {
        this.obj_writer.write("min: ");
        writePseudoBoolean(this.obj_writer, lits, coeffs, null, null, true);
    }
    
    /**
     * Writes a Pseudo-Boolean division objective function in the objective writer object {@link #obj_writer}.
     * @param num_lits The literal vectors of the numerators.
     * @param num_coeffs The coefficient vectors of the numerators.
     * @param den_lits The literal vectors of the denominators.
     * @param den_coeffs The coefficient vectors of the denominators.
     */
    private void writeObjective(IVec<IVecInt> num_lits, IVec<IVec<BigDecimal>> num_coeffs,
                                IVec<IVecInt> den_lits, IVec<IVec<BigDecimal>> den_coeffs) {
        assert(num_lits.size() == num_coeffs.size() && den_lits.size() == den_coeffs.size());
        assert(num_lits.size() == den_lits.size() && num_coeffs.size() == den_coeffs.size());
        this.obj_writer.write("min: ");
        for (int i = 0; i < num_lits.size(); ++i) {
            assert(num_lits.get(i).size() == num_coeffs.get(i).size());
            assert(den_lits.get(i).size() == den_coeffs.get(i).size());
            this.obj_writer.write("(");
            writePseudoBoolean(this.obj_writer, num_lits.get(i), num_coeffs.get(i), null, null, false);
            this.obj_writer.write(") / (");
            writePseudoBoolean(this.obj_writer, den_lits.get(i), den_coeffs.get(i), null, null, false);
            this.obj_writer.write(i == num_lits.size()-1 ? ") ;\n" : ") + ");
        }
    }
    
    /**
     * Prints the instance in OPB format in a given file.
     * @param path The file's path.
     */
    public void print(String path) {
        System.out.println("c Writing MOCO instance to " + path);
        try {
            BufferedWriter opb_writer = new BufferedWriter(new FileWriter(path));
            opb_writer.write("* #variable= " + nVars() + " #constraint= " + this.nconstraints + "\n");
            opb_writer.write(this.obj_writer.toString());
            opb_writer.write(this.cons_writer.toString());
            opb_writer.close();
        }
        catch (IOException e) {
            throw new RuntimeException("Failed to write .opb file", e);
        }
        System.out.println("c Done");
    }
    
    /**
     * Adds an objective function to be minimized.
     * @param lits The objective's literals.
     * @param coeffs The objective's coefficients.
     */
    public void addObjective(IVecInt lits, IVec<BigDecimal> coeffs) {
        writeObjective(lits, coeffs);
    }
    
    /**
     * Adds a sum of divisions objective function to be minimized.
     * @param num_lits The literal vectors of the numerators.
     * @param num_coeffs The coefficient vectors of the numerators.
     * @param den_lits The literal vectors of the denominators.
     * @param den_coeffs The coefficient vectors of the denominators.
     */
    public void addObjective(IVec<IVecInt> num_lits, IVec<IVec<BigDecimal>> num_coeffs,
                             IVec<IVecInt> den_lits, IVec<IVec<BigDecimal>> den_coeffs) {
        writeObjective(num_lits, num_coeffs, den_lits, den_coeffs);
    }
    
    @Override
    public void newVar() { ++nvars; }

    @Override
    public void newVars(int nvars) { this.nvars += nvars; }

    @Override
    public int nVars() { return nvars; }
    
    @Override
    public void addExactly(IVecInt lits, int rhs) throws ContradictionException {
        addEqual(lits, new Vec<BigInteger>(lits.size(), BigInteger.ONE), BigInteger.valueOf(rhs));
    }

    @Override
    public void addAtMost(IVecInt lits, int rhs) throws ContradictionException {
        addLessOrEqual(lits, new Vec<BigInteger>(lits.size(), BigInteger.ONE), BigInteger.valueOf(rhs));
    }

    @Override
    public void addAtLeast(IVecInt lits, int rhs) throws ContradictionException {
        addGreaterOrEqual(lits, new Vec<BigInteger>(lits.size(), BigInteger.ONE), BigInteger.valueOf(rhs));
    }

    @Override
    public void addEqual(IVecInt lits, IVec<BigInteger> coeffs, BigInteger rhs)
            throws ContradictionException {
        writeConstraint(lits, coeffs, rhs, EQUAL);
    }

    @Override
    public void addEqual(IVecInt lits, IVec<BigDecimal> coeffs, BigDecimal rhs)
            throws ContradictionException {
        writeConstraint(lits, coeffs, rhs, EQUAL);
    }

    @Override
    public void addGreaterOrEqual(IVecInt lits, IVec<BigInteger> coeffs, BigInteger rhs)
            throws ContradictionException {
        writeConstraint(lits, coeffs, rhs, GREATER_OR_EQUAL);
    }

    @Override
    public void addGreaterOrEqual(IVecInt lits, IVec<BigDecimal> coeffs, BigDecimal rhs)
            throws ContradictionException {
        writeConstraint(lits, coeffs, rhs, GREATER_OR_EQUAL);
    }

    @Override
    public void addLessOrEqual(IVecInt lits, IVec<BigInteger> coeffs, BigInteger rhs)
            throws ContradictionException {
        IVec<BigInteger> neg_coeffs = new Vec<BigInteger>(coeffs.size());
        for (int i = 0; i < coeffs.size(); ++i) {
            neg_coeffs.push(coeffs.get(i).negate());
        }
        addGreaterOrEqual(lits, neg_coeffs, rhs.negate());
    }

    // FIXME: very similar to BigInteger version of addLessOrEqual
    @Override
    public void addLessOrEqual(IVecInt lits, IVec<BigDecimal> coeffs, BigDecimal rhs)
            throws ContradictionException {
        IVec<BigDecimal> neg_coeffs = new Vec<BigDecimal>(coeffs.size());
        for (int i = 0; i < coeffs.size(); ++i) {
            neg_coeffs.push(coeffs.get(i).negate());
        }
        addGreaterOrEqual(lits, neg_coeffs, rhs.negate());
    }

    @Override
    public void addGreater(IVecInt lits, IVec<BigDecimal> coeffs, BigDecimal rhs)
            throws ContradictionException {
        throw new NotSupportedException("OPBWriter does not support > constraints");
    }

    @Override
    public void addLess(IVecInt lits, IVec<BigDecimal> coeffs, BigDecimal rhs) throws ContradictionException {
        throw new NotSupportedException("OPBWriter does not support < constraints");
    }

    @Override
    public void addClause(IVecInt lits) throws ContradictionException {
        addAtLeast(lits, 1);
    }

}

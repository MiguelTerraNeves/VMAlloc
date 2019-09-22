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

import java.math.BigInteger;

/**
 * An identifier for constraints added to constraint solvers. Some constraint solvers support
 * constraint removal. These solvers return a {@code ConstraintID} object when adding a removable
 * constraint, that can then be used to remove it. Constraint ids can also be used as timestamps, since
 * these can be compared and are created in increasing order by {@link #makeFresh()}.
 * @author Miguel Terra-Neves
 */
public class ConstraintID implements Comparable<ConstraintID> {
    
    /**
     * Counter used to generate unique constraint ids.
     */
    private static BigInteger id_gen = BigInteger.ZERO;

    /**
     * The {@code BigInteger} representation of the constraint id.
     */
    private BigInteger id = null;
    
    /**
     * Creates an instance of a constraint id.
     * @param id {@code BigInteger} representation of the id.
     */
    private ConstraintID(BigInteger id) { this.id = id; }
    
    /**
     * Equals comparator method for constraint ids. Checks if the constraint id and a given object
     * are equal.
     * @param other The object to be compared with the constraint id.
     * @return True if {@code other} is a constraint id and references the same constraint as this
     * constraint id, false otherwise.
     */
    @Override
    public boolean equals(Object other) {
        if (other instanceof ConstraintID) {
            ConstraintID other_id = (ConstraintID)other;
            return other_id.id.equals(id);
        }
        return false;
    }

    /**
     * Implementation of the {@code compareTo} method of the {@code Comparable} interface for
     * constraint ids. Checks if the constraint id is smaller or larger than another given
     * constraint id.
     * @param other The constraint id object to be compared with this constraint id.
     * @return An integer smaller, larger or equal to 0 if this constraint id is smaller, larger or
     * equal to {@code other} respectively.
     */
    public int compareTo(ConstraintID other) { return id.compareTo(other.id); }
    
    /**
     * Produces and returns a string representation of the constraint id.
     * @return The string representation of the constraint id.
     */
    @Override
    public String toString() { return id.toString(); }
    
    /**
     * Generates a fresh unique constraint id, not equal to any constraint id generated previously.
     * @return A fresh unique constraint id.
     */
    public static ConstraintID makeFresh() {
        ConstraintID new_id = new ConstraintID(id_gen);
        id_gen = id_gen.add(BigInteger.ONE);
        return new_id;
    }
    
}

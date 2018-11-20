package vmalloc.algorithm.constraints;

import java.math.BigInteger;

/**
 * An identifier for constraints added to constraint solvers. Some constraint solvers support
 * constraint removal. These solvers return a {@code ConstraintID} object when adding a removable
 * constraint, that can then be used to remove it.
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

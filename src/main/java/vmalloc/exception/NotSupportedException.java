package vmalloc.exception;

/**
 * Exception thrown when a user tries to perform an unsupported operation.
 * @author Miguel Terra-Neves
 */
public class NotSupportedException extends RuntimeException {
    
    private static final long serialVersionUID = 1L;
    
    public NotSupportedException() { super(); }
    
    public NotSupportedException(String string) { super(string); }
    
}

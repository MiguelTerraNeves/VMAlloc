package vmalloc.exception;

import vmalloc.preprocess.HeuristicReducer;

/**
 * Exception thrown when the heuristic reduction procedure fails to produce a reduced instance.
 * @author Miguel Terra-Neves
 * @see HeuristicReducer
 */
public class HeuristicReductionFailedException extends Exception {
    private static final long serialVersionUID = 1L;
}

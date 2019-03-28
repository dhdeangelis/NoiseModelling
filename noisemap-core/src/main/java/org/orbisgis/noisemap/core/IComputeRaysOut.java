package org.orbisgis.noisemap.core;

import java.util.List;

public interface IComputeRaysOut {

    /**
     * Add propagation path
     * @param propagationPath Propagation path result
     * @return Optional global energetic contribution
     */
    double addPropagationPaths(int sourceId, int receiverId, List<PropagationPath> propagationPath);

    /**
     * If the implementation does not support thread concurrency, this method is called to return an instance
     * @param receiverStart
     * @param receiverEnd
     * @return
     */
    IComputeRaysOut subProcess(int receiverStart, int receiverEnd);
}

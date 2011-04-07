package org.opentox.jaqpot3.qsar;

import org.opentox.jaqpot3.qsar.exceptions.BadParameterException;
import org.opentox.jaqpot3.qsar.exceptions.QSARException;
import org.opentox.toxotis.core.component.Algorithm;
import org.opentox.toxotis.core.component.Task;
import org.opentox.toxotis.util.aa.AuthenticationToken;

/**
 *
 * @author Pantelis Sopasakis
 * @author Charalampos Chomenides
 */
public interface IParametrizableAlgorithm {

    /**
     * Parametrize the algorithm providing the client's raw input.
     *
     * @param clientParameters
     *      Parameters provided by the client or end user for the parametrization
     *      of the algorithm.
     *
     * @return
     *      The updated instance of the mutable parametrizable algorithm.
     * @throws BadParameterException
     *      If an illegal parameter is provided to the algorithm.
     */
    IParametrizableAlgorithm parametrize(IClientInput clientParameters) throws BadParameterException;

    IParametrizableAlgorithm setTask(Task task);

    Task getTask();

    IParametrizableAlgorithm setToken(AuthenticationToken token);

    Algorithm getAlgorithm();
}

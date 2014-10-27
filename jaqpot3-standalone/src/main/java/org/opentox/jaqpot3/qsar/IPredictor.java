/*
 *
 * Jaqpot - version 3
 *
 * The JAQPOT-3 web services are OpenTox API-1.2 compliant web services. Jaqpot
 * is a web application that supports model training and data preprocessing algorithms
 * such as multiple linear regression, support vector machines, neural networks
 * (an in-house implementation based on an efficient algorithm), an implementation
 * of the leverage algorithm for domain of applicability estimation and various
 * data preprocessing algorithms like PLS and data cleanup.
 *
 * Copyright (C) 2009-2012 Pantelis Sopasakis & Charalampos Chomenides
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Contact:
 * Pantelis Sopasakis
 * chvng@mail.ntua.gr
 * Address: Iroon Politechniou St. 9, Zografou, Athens Greece
 * tel. +30 210 7723236
 *
 */

package org.opentox.jaqpot3.qsar;

import org.opentox.jaqpot3.exception.JaqpotException;
import org.opentox.jaqpot3.qsar.exceptions.BadParameterException;
import org.opentox.toxotis.client.VRI;
import org.opentox.toxotis.core.component.Dataset;
import org.opentox.toxotis.core.component.Model;
import org.opentox.toxotis.core.component.SubstanceDataset;
import org.opentox.toxotis.core.component.Task;
import org.opentox.toxotis.util.aa.AuthenticationToken;
import weka.core.Instances;

/**
 * An IPredictor is a processor that accepts a test dataset and produces a dataset
 * that contains the predictions of a given model. We note here that the provided
 * test dataset in the method {@link #process(java.lang.Object) #process(Object)} should have at
 * least the independent features of the model as provided in the method {@link
 * #setModel(org.opentox.toxotis.core.component.Model) #setModel(Model)}.
 *
 * @author Pantelis Sopasakis
 * @author Charalampos Chomenides
 */
public interface IPredictor {

    /**
     * Parametrize the prediction procedure. Using this interface method, user can provide
     * some tuning parameters such as the dataset service to which the result (dataset)
     * should be posted, whether the content of the dataset (feature values) should be
     * replicated in the output etc.
     *
     * @param clientParameters
     *      Parameters provided by the client
     *
     * @return
     *      Parameterized predictor object
     * 
     * @throws BadParameterException
     *      In case the client has provided illegal parameters or has ommited to provide
     *      some necessary parameter.
     */
    IPredictor parametrize(IClientInput clientParameters) throws BadParameterException;

    /**
     * Provide the model which will be used to generate the predictions.
     *
     * @param model
     *      A model component. Make sure that the object you provide has a not
     *      <code>null</code> actual model parameter as returned by the method
     *      {@link Model#getActualModel() } and preferable have not <code>null</code>
     *      URI.
     *
     * @return
     *      Updated predictor with the model
     */
    IPredictor setModel(Model model);

    Model getModel();

    /**
     * The predictor has to have control of the task under which the prediction job
     * runs. Once the client initiates a prediction session, a task is created and
     * its URI is returned immediately to the client to monitor the progress of the job.
     * 
     * @param task
     *      Asynchronous task for the prediction.
     * @return
     *      The current predictor object.
     */
    IPredictor setTask(Task task);

    /**
     * Retrieve the task that is created to control the asynchronous prediction
     * job.
     *
     * @return
     *      The underlying task.
     */
    Task getTask();

    /**
     * The client's token might be needed to access third party web services on
     * behalf of it.
     * @param token
     *      Client's token.
     * @return
     *      The current predictor object.
     */
    IPredictor setToken(AuthenticationToken token);

    Instances predict(Instances input) throws JaqpotException;
    
    Dataset predict(VRI input) throws JaqpotException;
    
    SubstanceDataset predictEnm(VRI input) throws JaqpotException;
    
    Instances getPredictedInstances() throws JaqpotException;
}

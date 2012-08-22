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

package org.opentox.jaqpot3.www.services;

import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.concurrent.Future;
import org.opentox.jaqpot3.exception.JaqpotException;
import org.opentox.jaqpot3.qsar.IClientInput;
import org.opentox.jaqpot3.qsar.IPredictor;
import org.opentox.jaqpot3.qsar.exceptions.BadParameterException;
import org.opentox.jaqpot3.util.Configuration;
import org.opentox.toxotis.client.VRI;
import org.opentox.toxotis.client.collection.Services;
import org.opentox.toxotis.core.component.Dataset;
import org.opentox.toxotis.core.component.Task.Status;
import org.opentox.toxotis.database.engine.task.UpdateTask;
import org.opentox.toxotis.database.exception.DbException;
import org.opentox.toxotis.ontology.LiteralValue;
import org.opentox.toxotis.ontology.ResourceValue;
import org.opentox.toxotis.ontology.collection.OTClasses;
import org.opentox.toxotis.util.aa.AuthenticationToken;

/**
 *
 * @author Pantelis Sopasakis
 * @author Charalampos Chomenides
 */
public class PredictionService extends RunnableTaskService {

    private IPredictor predictor;
    private IClientInput clientInput;
    private AuthenticationToken token;
    private org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(PredictionService.class);
    private VRI datasetServiceUri = Services.ideaconsult().augment("dataset");

    public PredictionService(IPredictor predictor, IClientInput clientInput, AuthenticationToken token) {
        this.predictor = predictor;
        this.clientInput = clientInput;
        this.token = token;
    }

    private void parametrize(IClientInput clientParameters) throws BadParameterException {
        String datasetServiceString = clientParameters.getFirstValue("dataset_service");
        if (datasetServiceString != null) {
            try {
                datasetServiceUri = new VRI(datasetServiceString);
            } catch (URISyntaxException ex) {
                throw new BadParameterException("The parameter 'dataset_uri' you provided is not a valid URI.", ex);
            }
        }
    }

    @Override
    public void run() {

        /*
         * Change the status of the task from QUEUED to RUNNING
         * The task has ALREADY been registered (see ModelResource)
         */
        predictor.getTask().setStatus(Status.RUNNING);
        predictor.getTask().getMeta().setDate(
                new LiteralValue(new Date(System.currentTimeMillis()), XSDDatatype.XSDdate));
        if (predictor.getModel() != null && predictor.getModel().getUri() != null) {
            predictor.getTask().getMeta().addHasSource(new ResourceValue(predictor.getModel().getUri(), 
                    OTClasses.model()));
        }

        UpdateTask updater = new UpdateTask(predictor.getTask());
        updater.setUpdateTaskStatus(true);
        updater.setUpdateMeta(true);
        try {
            updater.update();// update the task (QUEUED --> RUNNING)
        } catch (DbException ex) {
            logger.error("Cannot update task to RUNNING", ex);
        } finally {
            if (updater != null) {
                try {
                    updater.close();
                } catch (DbException ex) {
                    logger.error("TaskUpdater is uncloseable", ex);
                }
            }
        }

        String datasetUri = clientInput.getFirstValue("dataset_uri");
        try {
            this.parametrize(clientInput);
            predictor.parametrize(clientInput);
            VRI datasetURI = new VRI(datasetUri);
            

            /* GET THE PREDICTIONS FROM THE PREDICTOR */
            Dataset output = predictor.predict(datasetURI);
            /* */
            System.out.println(datasetServiceUri);
            System.out.println(token);
            Future<VRI> future = output.publish(datasetServiceUri, token);
            float counter = 1;
            while (!future.isDone()) {
                try {
                    Thread.sleep(1000);
                    float prc = 100f - (50.0f / (float) Math.sqrt(counter));
                    predictor.getTask().setPercentageCompleted(prc);
                    UpdateTask updateTask = new UpdateTask(predictor.getTask());
                    updateTask.setUpdateMeta(true);
                    updateTask.setUpdatePercentageCompleted(true);
                    updateTask.update();
                    updateTask.close();
                    counter++;
                } catch (InterruptedException ex) {
                    logger.error("Interrupted", ex);
                    throw new JaqpotException("UnknownCauseOfException", ex);
                }
            }
            try {
                VRI resultUri = future.get();
                predictor.getTask().setHttpStatus(200).setPercentageCompleted(100.0f).
                        setResultUri(resultUri).setStatus(Status.COMPLETED);
                UpdateTask updateTask = new UpdateTask(predictor.getTask());
                updateTask.setUpdateHttpStatus(true);
                updateTask.setUpdateTaskStatus(true);
                updateTask.setUpdateResultUri(true);
                updateTask.update();
                updateTask.close();

            } catch (InterruptedException ex) {
                logger.error("Task update was abnormally interrupted", ex);
                throw new JaqpotException("UnknownCauseOfException", ex);
            }

        } catch (URISyntaxException ex) {
            logger.trace(null, ex);
            updateFailedTask(predictor.getTask(), ex, "The parameter 'dataset_uri' provided by the user cannot be "
                    + "cast as a valid URI", 400, predictor.getTask().getCreatedBy().getUid());
        } catch (BadParameterException ex) {
            logger.trace(null, ex);
            updateFailedTask(predictor.getTask(), ex, "Task failed due to illegal parametrization. ", 400,
                    predictor.getTask().getCreatedBy().getUid());
        } catch (Throwable ex) {
            logger.error(null, ex);
            updateFailedTask(predictor.getTask(), ex, "", 500, Configuration.BASE_URI);
        }

    }
}

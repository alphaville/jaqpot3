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
import org.opentox.jaqpot3.exception.JaqpotException;
import org.opentox.jaqpot3.qsar.IClientInput;
import org.opentox.jaqpot3.qsar.ITrainer;
import org.opentox.jaqpot3.qsar.exceptions.BadParameterException;
import org.opentox.jaqpot3.util.Configuration;
import org.opentox.toxotis.client.VRI;
import org.opentox.toxotis.core.component.Dataset;
import org.opentox.toxotis.core.component.ErrorReport;
import org.opentox.toxotis.core.component.Model;
import org.opentox.toxotis.core.component.Task.Status;
import org.opentox.toxotis.database.engine.model.AddModel;
import org.opentox.toxotis.database.engine.task.UpdateTask;
import org.opentox.toxotis.database.exception.DbException;
import org.opentox.toxotis.exceptions.impl.ServiceInvocationException;
import org.opentox.toxotis.ontology.LiteralValue;
import org.opentox.toxotis.ontology.ResourceValue;
import org.opentox.toxotis.util.aa.AuthenticationToken;
import org.opentox.toxotis.util.aa.policy.IPolicyWrapper;
import org.opentox.toxotis.util.aa.policy.PolicyManager;
import org.opentox.toxotis.util.arff.RemoteArffRertiever;

/**
 *
 * @author Pantelis Sopasakis
 * @author Charalampos Chomenides
 */
public class TrainingService extends RunnableTaskService {

    private ITrainer trainer;
    private IClientInput clientInput;
    private AuthenticationToken token;
    private org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(TrainingService.class);

    public TrainingService(ITrainer trainer, IClientInput clientInput, AuthenticationToken token) {
        this.trainer = trainer;
        this.clientInput = clientInput;
        this.token = token;
    }

    @Override
    public void run() {
        long startingTime = System.currentTimeMillis();

        /*
         * Change the status of the task from QUEUED to RUNNING
         * The task has ALREADY been registered (see ModelResource)
         */
        trainer.getTask().setStatus(Status.RUNNING);
        trainer.getTask().getMeta().addHasSource(new ResourceValue(trainer.getAlgorithm().getUri(), null)).setDate(
                new LiteralValue(new Date(System.currentTimeMillis()), XSDDatatype.XSDdate));

        UpdateTask updater = new UpdateTask(trainer.getTask());
        updater.setUpdateTaskStatus(true);
        updater.setUpdateMeta(true);
        try {
            updater.update();// update the task
        } catch (DbException ex) {
            logger.error("Cannot update task to RUNNING", ex);
        } finally {
            if (updater != null) {
                try {
                    updater.close();
                } catch (DbException ex) {
                }
            }
        }

        String datasetUri = clientInput.getFirstValue("dataset_uri");
        try {
            trainer.parametrize(clientInput); // #NODE_01
            VRI datasetURI = datasetUri != null ? new VRI(datasetUri) : null;// #NODE_02

            Model resultModel = trainer.train(datasetURI);// #NODE_03_b

            /* Create a policy for the model (on behalf of the user) */
            IPolicyWrapper pw = PolicyManager.defaultSignleUserPolicy("model_" + resultModel.getUri().getId(), resultModel.getUri(), token);
            pw.publish(null, token);

            /* STORE THE MODEL IN THE DATABASE :-)*/
            resultModel.getMeta().addCreator(token.getUser().getUid());

            AddModel modelAdder = new AddModel(resultModel);

            //TODO: Handle exceptions properly
            modelAdder.write();
            modelAdder.close();

            /* UPDATE THE TASK - COMPLETED :)*/
            trainer.getTask().setDuration(System.currentTimeMillis() - startingTime);
            trainer.getTask().getMeta().
                    addComment("Training completed successfully! The model is now stored in the database.");
            trainer.getTask().setStatus(Status.COMPLETED).setHttpStatus(200).setResultUri(resultModel.getUri()).setPercentageCompleted(100);

            UpdateTask taskFinalUpdater = new UpdateTask(trainer.getTask());
            taskFinalUpdater.setUpdateTaskStatus(true);
            taskFinalUpdater.setUpdateDuration(true);
            taskFinalUpdater.setUpdateResultUri(true);
            taskFinalUpdater.setUpdateMeta(true);
            taskFinalUpdater.update();
            taskFinalUpdater.close();

        } catch (BadParameterException ex) {// FROM #NODE_01
            updateFailedTask(trainer.getTask(), ex, "Task failed due to illegal parametrization. ", 400,
                    trainer.getTask().getCreatedBy().getUid());
            logger.trace(null, ex);
        } catch (URISyntaxException ex) {// FROM #NODE_02
            ex.printStackTrace();
            updateFailedTask(trainer.getTask(), ex, "The dataset URI you provided cannot be cast as a valid URI object.", 400,
                    trainer.getTask().getCreatedBy().getUid());
            logger.trace(null, ex);
        } catch (JaqpotException ex) {//FROM NODE_03
            logger.info(null, ex);
            ex.printStackTrace();
            updateFailedTask(trainer.getTask(), ex, "", 500, Configuration.BASE_URI);
        } catch (ServiceInvocationException ex) {
            ErrorReport er = ex.asErrorReport();
            er.setErrorCode(ex.getClass().getSimpleName());
            updateFailedTask(trainer.getTask(), er);
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            logger.error(null, throwable);
            updateFailedTask(trainer.getTask(), throwable, "", 500, Configuration.BASE_URI);
        }

    }
}

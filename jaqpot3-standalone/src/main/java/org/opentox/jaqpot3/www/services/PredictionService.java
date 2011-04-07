package org.opentox.jaqpot3.www.services;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URISyntaxException;
import org.opentox.jaqpot3.qsar.IClientInput;
import org.opentox.jaqpot3.qsar.IPredictor;
import org.opentox.jaqpot3.qsar.exceptions.BadParameterException;
import org.opentox.jaqpot3.util.Configuration;
import org.opentox.toxotis.client.VRI;
import org.opentox.toxotis.core.component.Dataset;
import org.opentox.toxotis.core.component.Model;
import org.opentox.toxotis.core.component.Task;
import org.opentox.toxotis.core.component.Task.Status;
import org.opentox.toxotis.database.engine.task.UpdateTask;
import org.opentox.toxotis.database.exception.DbException;
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

    public PredictionService(IPredictor predictor, IClientInput clientInput, AuthenticationToken token) {
        this.predictor = predictor;
        this.clientInput = clientInput;
        this.token = token;
    }

    @Override
    public void run() {

        /*
         * Change the status of the task from QUEUED to RUNNING
         * The task has ALREADY been registered (see ModelResource)
         */
        predictor.getTask().setStatus(Status.RUNNING);

        UpdateTask updater = new UpdateTask(predictor.getTask());
        updater.setUpdateTaskStatus(true);
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
            predictor.parametrize(clientInput);
            VRI datasetURI = new VRI(datasetUri);
            Dataset ds = new Dataset(datasetURI).loadFromRemote(token);
            predictor.predict(ds);
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

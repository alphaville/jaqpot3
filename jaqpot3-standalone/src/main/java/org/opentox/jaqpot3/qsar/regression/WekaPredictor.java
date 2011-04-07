package org.opentox.jaqpot3.qsar.regression;

import java.net.URISyntaxException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.opentox.jaqpot3.exception.JaqpotException;
import org.opentox.jaqpot3.qsar.AbstractPredictor;
import org.opentox.jaqpot3.qsar.IClientInput;
import org.opentox.jaqpot3.qsar.IPredictor;
import org.opentox.jaqpot3.qsar.InstancesUtil;
import org.opentox.jaqpot3.qsar.exceptions.BadParameterException;
import org.opentox.jaqpot3.qsar.exceptions.QSARException;
import org.opentox.jaqpot3.qsar.filter.AttributeCleanup;
import org.opentox.toxotis.client.VRI;
import org.opentox.toxotis.client.collection.Services;
import org.opentox.toxotis.core.component.Dataset;
import org.opentox.toxotis.core.component.Task;
import org.opentox.toxotis.core.component.Task.Status;
import org.opentox.toxotis.database.engine.task.UpdateTask;
import org.opentox.toxotis.exceptions.impl.ServiceInvocationException;
import org.opentox.toxotis.exceptions.impl.ToxOtisException;
import org.opentox.toxotis.factory.DatasetFactory;
import weka.classifiers.Classifier;
import weka.core.Instances;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Add;

import static org.opentox.jaqpot3.qsar.filter.AttributeCleanup.ATTRIBUTE_TYPE.*;

/**
 *
 * @author Pantelis Sopasakis
 * @author Charalampos Chomenides
 */
public class WekaPredictor extends AbstractPredictor {

    private VRI datasetServiceUri = Services.ideaconsult().augment("dataset");
    private org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(WekaPredictor.class);

    public WekaPredictor() {
        super();
    }

    @Override
    public IPredictor parametrize(IClientInput clientParameters) throws BadParameterException {
        String datasetServiceString = clientParameters.getFirstValue("dataset_uri");
        if (datasetServiceString != null) {
            try {
                datasetServiceUri = new VRI(datasetServiceString);
            } catch (URISyntaxException ex) {
                throw new BadParameterException("The parameter 'dataset_uri' you provided is not a valid URI.", ex);
            }
        }
        return this;
    }

    @Override
    public Dataset predict(Dataset data) throws JaqpotException {
        Instances inputSet = data.getInstances();
        try {
            /* THE OBJECT newData WILL HOST THE PREDICTIONS... */
            Instances newData = InstancesUtil.sortForModel(model, inputSet, -1);
            /* ADD TO THE NEW DATA THE PREDICTION FEATURE*/
            Add attributeAdder = new Add();
            attributeAdder.setAttributeIndex("last");
            attributeAdder.setAttributeName(model.getPredictedFeatures().iterator().next().getUri().toString());
            Instances predictions = null;
            try {
                attributeAdder.setInputFormat(newData);
                predictions = Filter.useFilter(newData, attributeAdder);
                predictions.setClass(predictions.attribute(model.getPredictedFeatures().iterator().next().getUri().toString()));
            } catch (Exception ex) {
                String message = "Exception while trying to add prediction feature to Instances";
                logger.debug(message, ex);
                throw new JaqpotException(message, ex);
            }

            if (predictions != null) {
                Classifier classifier = (Classifier) model.getActualModel();

                int numInstances = predictions.numInstances();
                for (int i = 0; i < numInstances; i++) {
                    try {
                        double predictionValue = classifier.distributionForInstance(predictions.instance(i))[0];
                        predictions.instance(i).setClassValue(predictionValue);
                    } catch (Exception ex) {
                        logger.warn("Prediction failed :-(", ex);
                    }
                }
            }

            AttributeCleanup justCompounds = new AttributeCleanup(true, nominal, numeric, string);
            Instances compounds = null;
            try {
                compounds = justCompounds.filter(inputSet);
            } catch (QSARException ex) {
                logger.debug(null, ex);
            }


            Instances result = Instances.mergeInstances(compounds, predictions);
            Dataset ds = DatasetFactory.createFromArff(result);


            Future<VRI> future = ds.publish(datasetServiceUri, token);
            float counter = 1;
            while (!future.isDone()) {
                try {
                    Thread.sleep(1000);
                    float prc = 100f - (50.0f / (float) Math.sqrt(counter));
                    getTask().setPercentageCompleted(prc);

                    UpdateTask updateTask = new UpdateTask(getTask());
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
                getTask().setHttpStatus(200).setPercentageCompleted(100.0f).
                        setResultUri(resultUri).setStatus(Status.COMPLETED);

                UpdateTask updateTask = new UpdateTask(getTask());
                updateTask.setUpdateTaskStatus(true);
                updateTask.setUpdateResultUri(true);
                updateTask.update();
                updateTask.close();

            } catch (InterruptedException ex) {
                logger.error("Task update was abnormally interrupted", ex);
                throw new JaqpotException("UnknownCauseOfException", ex);
            } catch (ExecutionException ex) {
                logger.warn(null, ex);
                throw new JaqpotException("UnknownCauseOfException", ex);
            }

            return ds;
        } catch (ServiceInvocationException ex) {
            throw new JaqpotException("Exception while performing prediction", ex);
        } catch (ToxOtisException ex) {
            logger.debug(null, ex);
            throw new JaqpotException("Exception while performing prediction", ex);
        }

    }
}

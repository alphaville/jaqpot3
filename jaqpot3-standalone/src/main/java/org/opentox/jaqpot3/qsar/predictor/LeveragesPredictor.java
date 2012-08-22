package org.opentox.jaqpot3.qsar.predictor;

import org.opentox.jaqpot3.qsar.serializable.LeveragesModel;
import Jama.Matrix;
import java.net.URISyntaxException;
import org.opentox.jaqpot3.exception.JaqpotException;
import org.opentox.jaqpot3.qsar.AbstractPredictor;
import org.opentox.jaqpot3.qsar.IClientInput;
import org.opentox.jaqpot3.qsar.IPredictor;
import org.opentox.jaqpot3.qsar.InstancesUtil;
import org.opentox.jaqpot3.qsar.exceptions.BadParameterException;
import org.opentox.jaqpot3.qsar.exceptions.QSARException;
import org.opentox.jaqpot3.qsar.util.AttributeCleanup;
import org.opentox.toxotis.client.VRI;
import org.opentox.toxotis.client.collection.Services;
import org.opentox.toxotis.core.component.Dataset;
import org.opentox.toxotis.exceptions.impl.ToxOtisException;
import org.opentox.toxotis.factory.DatasetFactory;
import weka.core.Instances;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Add;

import static org.opentox.jaqpot3.qsar.util.AttributeCleanup.ATTRIBUTE_TYPE.*;

/**
 *
 * @author Pantelis Sopasakis
 * @author Charalampos Chomenides
 */
public class LeveragesPredictor extends AbstractPredictor {

    private VRI datasetServiceUri = Services.ideaconsult().augment("dataset");
    private transient org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(LeveragesPredictor.class);

    public LeveragesPredictor() {
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
    public Dataset predict(Instances inputSet) throws JaqpotException {
        LeveragesModel actualModel = (LeveragesModel) model.getActualModel();
        Matrix matrix = actualModel.getDataMatrix();
        double gamma = actualModel.getGamma();
        Instances orderedDataset = null;
        try {
            orderedDataset = InstancesUtil.sortForModel(model, inputSet, -1);
        } catch (JaqpotException ex) {
            String message = "It is not possible to apply the dataset "
                    + " to the model : " + (model != null ? model.getUri().toString() : "(no URI)") + ". Most probably the dataset does not contain "
                    + "the independent features for this model.";
            logger.debug(message, ex);
            throw new JaqpotException(message, ex);
        }

        AttributeCleanup justCompounds = new AttributeCleanup(true, nominal, numeric, string);
        justCompounds.setKeepCompoundURI(true);
        Instances compounds = null;
        try {
            compounds = justCompounds.filter(inputSet);
        } catch (QSARException ex) {
            logger.debug(null, ex);
        }

        int numInstances = orderedDataset.numInstances();
        int numAttributes = orderedDataset.numAttributes();
        /*Dataset containing the predictions (DoA estimations) [predictions]*/
        Instances predictions = new Instances(orderedDataset);
        /* ADD TO THE NEW DATA THE PREDICTION FEATURE*/
        Add attributeAdder = new Add();
        attributeAdder.setAttributeIndex("last");
        attributeAdder.setAttributeName(model.getPredictedFeatures().iterator().next().getUri().toString());
        try {
            attributeAdder.setInputFormat(predictions);
            predictions = Filter.useFilter(predictions, attributeAdder);
            predictions.setClass(predictions.attribute(model.getPredictedFeatures().iterator().next().getUri().toString()));
        } catch (Exception ex) {
            String message = "Exception while trying to add prediction feature to Instances";
            logger.debug(message, ex);
            throw new JaqpotException(message, ex);
        }


        Matrix x = null;
        for (int i = 0; i < numInstances; i++) {
            x = new Matrix(orderedDataset.instance(i).toDoubleArray(), numAttributes);
            double indicator = Math.max(0, (gamma - x.transpose().times(matrix).times(x).get(0, 0)) / gamma);
            predictions.instance(i).setClassValue(indicator);
        }

        try {

            Dataset output = DatasetFactory.getInstance().
                    createFromArff(Instances.mergeInstances(compounds, predictions));

            return output;
        } catch (ToxOtisException ex) {
            logger.error(null, ex);
            throw new JaqpotException(ex);
        }


    }
}

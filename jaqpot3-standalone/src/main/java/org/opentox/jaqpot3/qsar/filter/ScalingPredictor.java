package org.opentox.jaqpot3.qsar.filter;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.opentox.jaqpot3.exception.JaqpotException;
import org.opentox.jaqpot3.qsar.AbstractPredictor;
import org.opentox.jaqpot3.qsar.IClientInput;
import org.opentox.jaqpot3.qsar.IPredictor;
import org.opentox.jaqpot3.qsar.exceptions.BadParameterException;
import org.opentox.toxotis.client.VRI;
import org.opentox.toxotis.core.component.Dataset;
import org.opentox.toxotis.core.component.Feature;
import org.opentox.toxotis.core.component.Model;
import org.opentox.toxotis.exceptions.impl.ToxOtisException;
import org.opentox.toxotis.factory.DatasetFactory;
import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;

/**
 *
 * @author Pantelis Sopasakis
 */
public class ScalingPredictor extends AbstractPredictor {

    /**
     * Feature URI ---> Scaled Feature URI
     */
    private Map<String, String> featureToScaled = new HashMap<String, String>();

    @Override
    public IPredictor parametrize(IClientInput clientParameters) throws BadParameterException {
        return this;
    }

    private void updateFeatureMap(Model model) {
        assert (model.getIndependentFeatures().size() == model.getDependentFeatures().size());
        List<Feature> predictedFeatures = model.getPredictedFeatures();
        Iterator<Feature> predictedIterator = predictedFeatures.iterator();
        for (Feature f : model.getIndependentFeatures()) {
            featureToScaled.put(f.getUri().toString(), predictedIterator.next().getUri().toString());
        }
    }

    @Override
    public Dataset predict(Dataset input) throws JaqpotException {
        try{
        ScalingModel actualModel = (ScalingModel) model.getActualModel();
        System.out.println(actualModel);
        System.out.println(actualModel.getMax());
        System.out.println(actualModel.getMin());
        
        Map<String, Double> mins = actualModel.getMinVals2();
        Map<String, Double> maxs = actualModel.getMaxVals2();

        System.out.println("mins ...");
        System.out.println(actualModel.getMinVals());
        System.out.println("maxs ...");
        System.out.println(actualModel.getMaxVals());

        updateFeatureMap(model);

        /** GET INSTANCES**/
        Instances inputData = input.getInstances();


        //int Nattr = inputData.numAttributes();
        int Ninst = inputData.numInstances();

        Iterator<String> features = featureToScaled.keySet().iterator();

        String nextFeature = null;
        Attribute currentAttribute = null;
        double currentMin = 0;
        double currentMax = 1;
        double currentValue = 0;

        while (features.hasNext()) {
            nextFeature = features.next();
            currentMin = mins.get(nextFeature);
            currentMax = maxs.get(nextFeature);
            currentAttribute = inputData.attribute(nextFeature);
            for (int iInst = 0; iInst < Ninst; iInst++) {
                currentValue = inputData.instance(iInst).value(currentAttribute);
                currentValue = (currentValue - currentMin) / (currentMax - currentMin);
                inputData.instance(iInst).setValue(currentAttribute, currentValue);
            }
        }


        /** Rename Attributes in `inputData` **/
        features = featureToScaled.keySet().iterator();
        while (features.hasNext()) {
            nextFeature = features.next();
            currentAttribute = inputData.attribute(nextFeature);
            if (currentAttribute == null) {
                throw new JaqpotException("The dataset you provided does not contain the necessary "
                        + "feature : " + nextFeature);
            }
            inputData.renameAttribute(currentAttribute, featureToScaled.get(nextFeature));
        }
        try {
            return DatasetFactory.createFromArff(inputData);
        } catch (ToxOtisException ex) {
            throw new JaqpotException(ex);
        }


        } catch (Throwable thr){
            thr.printStackTrace();

        }
        return null;
    }
}

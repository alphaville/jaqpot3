package org.opentox.jaqpot3.qsar.filter;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.opentox.jaqpot3.exception.JaqpotException;
import org.opentox.jaqpot3.qsar.AbstractPredictor;
import org.opentox.jaqpot3.qsar.IClientInput;
import org.opentox.jaqpot3.qsar.IPredictor;
import org.opentox.jaqpot3.qsar.exceptions.BadParameterException;
import org.opentox.toxotis.core.component.Dataset;
import org.opentox.toxotis.core.component.Feature;
import org.opentox.toxotis.core.component.Model;
import org.opentox.toxotis.exceptions.impl.ToxOtisException;
import org.opentox.toxotis.factory.DatasetFactory;
import weka.core.Attribute;
import weka.core.Instances;
import weka.filters.unsupervised.attribute.ReplaceMissingValues;

/**
 *
 * @author Pantelis Sopasakis
 * @author Charalampos Chomenides
 */
public class MissingValueFilterPredictor extends AbstractPredictor {

    private Map<String, String> featureToMVH = new HashMap<String, String>();

    @Override
    public IPredictor parametrize(IClientInput clientParameters) throws BadParameterException {
        return this;
    }

    private void updateFeatureMap(Model model) {
        assert (model.getIndependentFeatures().size() == model.getDependentFeatures().size());
        List<Feature> predictedFeatures = model.getPredictedFeatures();
        Iterator<Feature> predictedIterator = predictedFeatures.iterator();
        for (Feature f : model.getIndependentFeatures()) {
            featureToMVH.put(f.getUri().toString(), predictedIterator.next().getUri().toString());
        }
    }

    @Override
    public Dataset predict(Dataset input) throws JaqpotException {
        HashSet<String> ignoredUris = (HashSet<String>) model.getActualModel();
        Instances data = input.getInstances();
        for (String attribute2Bignored : ignoredUris) {
            Attribute attr = data.attribute(attribute2Bignored);
            if (attr != null) {
                data.deleteAttributeAt(attr.index());
            }
        }
        updateFeatureMap(model);
        weka.filters.unsupervised.attribute.ReplaceMissingValues replacer = new ReplaceMissingValues();

        try {
            replacer.setInputFormat(data);
        } catch (Exception ex) {
            Logger.getLogger(MissingValueFilterPredictor.class.getName()).log(Level.SEVERE, null, ex);
            throw new JaqpotException(ex);
        }


        Iterator<String> features = featureToMVH.keySet().iterator();
        String nextFeature = null;
        Attribute currentAttribute = null;
        while (features.hasNext()) {
            nextFeature = features.next();
            currentAttribute = data.attribute(nextFeature);
            if (currentAttribute == null) {
                throw new JaqpotException("The dataset you provided does not contain the necessary "
                        + "feature : " + nextFeature);
            }
            data.renameAttribute(currentAttribute, featureToMVH.get(nextFeature));
        }
        

        try {
            return DatasetFactory.createFromArff(data);
        } catch (ToxOtisException ex) {
            Logger.getLogger(MissingValueFilterPredictor.class.getName()).log(Level.SEVERE, null, ex);
            throw new JaqpotException(ex);
        }



    }
}

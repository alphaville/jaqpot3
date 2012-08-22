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

package org.opentox.jaqpot3.qsar.predictor;

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
    public Dataset predict(Instances data) throws JaqpotException {
        HashSet<String> ignoredUris = (HashSet<String>) model.getActualModel();        
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
            return DatasetFactory.getInstance().createFromArff(data);
        } catch (ToxOtisException ex) {
            Logger.getLogger(MissingValueFilterPredictor.class.getName()).log(Level.SEVERE, null, ex);
            throw new JaqpotException(ex);
        }



    }
}

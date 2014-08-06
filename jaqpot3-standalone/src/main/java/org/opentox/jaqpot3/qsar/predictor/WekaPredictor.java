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

import java.util.List;
import org.opentox.jaqpot3.exception.JaqpotException;
import org.opentox.jaqpot3.qsar.AbstractPredictor;
import org.opentox.jaqpot3.qsar.IClientInput;
import org.opentox.jaqpot3.qsar.IPredictor;
import org.opentox.jaqpot3.qsar.InstancesUtil;
import org.opentox.jaqpot3.qsar.exceptions.BadParameterException;
import org.opentox.toxotis.core.component.Dataset;
import org.opentox.toxotis.exceptions.impl.ToxOtisException;
import org.opentox.toxotis.factory.DatasetFactory;
import weka.classifiers.Classifier;
import weka.core.Instances;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Add;

import org.opentox.jaqpot3.qsar.util.WekaInstancesProcess;

/**
 *
 * @author Pantelis Sopasakis
 * @author Charalampos Chomenides
 */
public class WekaPredictor extends AbstractPredictor {

    private org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(WekaPredictor.class);

    public WekaPredictor() {
        super();
    }

    @Override
    public IPredictor parametrize(IClientInput clientParameters) throws BadParameterException {

        return this;
    }

    @Override
    public Instances predict(Instances inputSet) throws JaqpotException {
        
        /* THE OBJECT newData WILL HOST THE PREDICTIONS... */
        Instances newData = InstancesUtil.sortForPMMLModel(model.getIndependentFeatures(),trFieldsAttrIndex, inputSet, -1);
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
            Classifier classifier = (Classifier) model.getActualModel().getSerializableActualModel();

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

        List<Integer> trFieldsIndex = WekaInstancesProcess.getTransformationFieldsAttrIndex(predictions, pmmlObject);
        predictions = WekaInstancesProcess.removeInstancesAttributes(predictions, trFieldsIndex);
        Instances result = Instances.mergeInstances(justCompounds, predictions);

        return result;
    }
}

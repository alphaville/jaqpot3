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

import org.opentox.jaqpot3.qsar.serializable.FastRbfNnModel;
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
import weka.core.Instance;
import weka.core.Instances;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Add;
import static org.opentox.jaqpot3.qsar.util.AttributeCleanup.AttributeType.*;

/**
 *
 * @author Pantelis Sopasakis
 * @author Charalampos Chomenides
 */
public class FastRbfNnPredictor extends AbstractPredictor {

    private VRI datasetServiceUri = Services.ideaconsult().augment("dataset");
    private org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(FastRbfNnPredictor.class);

    public FastRbfNnPredictor() {
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

    private static double squaredNormDifference(Instance a, Instance b) {
        int numAttributes = a.numAttributes();
        if (numAttributes != b.numAttributes()) {
            throw new IllegalArgumentException("Provided instances of different length! "
                    + "Squared Norm of the difference cannot be calculated!");
        }
        double sum = 0;
        for (int i = 0; i < numAttributes; i++) {
            sum += Math.pow(a.value(i) - b.value(i), 2);
        }
        return sum;
    }

    private static double rbf(double sigma, Instance x, Instance node) {
        double result = Math.exp(-squaredNormDifference(x, node) / Math.pow(sigma, 2));
        return result;
    }

    @Override
    public Instances predict(Instances inputSet) throws JaqpotException {
        FastRbfNnModel actualModel = (FastRbfNnModel) model.getActualModel();
        Instances orderedDataset = null;
        try {
            orderedDataset = InstancesUtil.sortForModel(model, inputSet, -1);
        } catch (JaqpotException ex) {
            logger.error(null, ex);
        }

        Instances predictions = new Instances(orderedDataset);
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


        Instances nodes = actualModel.getNodes();
        double[] sigma = actualModel.getSigma();
        double[] coeffs = actualModel.getLrCoefficients();
        double sum;
        for (int i = 0; i < orderedDataset.numInstances(); i++) {
            sum = 0;
            for (int j = 0; j < nodes.numInstances(); j++) {
                sum += rbf(sigma[j], orderedDataset.instance(i), nodes.instance(j)) * coeffs[j];
            }
            predictions.instance(i).setClassValue(sum);
        }

        Instances resultSet = Instances.mergeInstances(justCompounds, predictions);
        return resultSet;

    }
    
}

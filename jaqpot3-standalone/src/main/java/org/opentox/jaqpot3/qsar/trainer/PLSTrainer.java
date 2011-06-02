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
package org.opentox.jaqpot3.qsar.trainer;

import java.io.NotSerializableException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.opentox.jaqpot3.exception.JaqpotException;
import org.opentox.jaqpot3.qsar.AbstractTrainer;
import org.opentox.jaqpot3.qsar.IClientInput;
import org.opentox.jaqpot3.qsar.IParametrizableAlgorithm;
import org.opentox.jaqpot3.qsar.exceptions.BadParameterException;
import org.opentox.jaqpot3.qsar.exceptions.QSARException;
import org.opentox.jaqpot3.qsar.serializable.PLSModel;
import org.opentox.jaqpot3.qsar.util.AttributeCleanup;
import org.opentox.jaqpot3.qsar.util.SimpleMVHFilter;
import org.opentox.jaqpot3.resources.collections.Algorithms;
import org.opentox.jaqpot3.util.Configuration;
import org.opentox.toxotis.client.VRI;
import org.opentox.toxotis.client.collection.Services;
import org.opentox.toxotis.core.component.Algorithm;
import org.opentox.toxotis.core.component.Dataset;
import org.opentox.toxotis.core.component.Feature;
import org.opentox.toxotis.core.component.Model;
import weka.classifiers.functions.PLSClassifier;
import weka.core.Instances;
import weka.filters.Filter;
import weka.filters.supervised.attribute.PLSFilter;

/**
 *
 * @author Pantelis Sopasakis
 * @author Charalampos Chomenides
 */
public class PLSTrainer extends AbstractTrainer {

    private VRI featureService;
    private VRI datasetUri;
    private int numComponents;
    private String preprocessing;
    private String pls_algorithm;
    private String target;

    @Override
    public IParametrizableAlgorithm parametrize(IClientInput clientParameters) throws BadParameterException {
        String datasetUriString = clientParameters.getFirstValue("dataset_uri");
        if (datasetUriString == null) {
            throw new BadParameterException("The parameter 'dataset_uri' is mandatory for this algorithm.");
        }
        try {
            datasetUri = new VRI(datasetUriString);
        } catch (URISyntaxException ex) {
            throw new BadParameterException("The parameter 'dataset_uri' you provided is not a valid URI.", ex);
        }
        String featureServiceString = clientParameters.getFirstValue("feature_service");
        if (featureServiceString != null) {
            try {
                featureService = new VRI(featureServiceString);
            } catch (URISyntaxException ex) {
                throw new BadParameterException("The parameter 'feature_service' you provided is not a valid URI.", ex);
            }
        } else {
            featureService = Services.ideaconsult().augment("feature");
        }
        String numComponentsString = clientParameters.getFirstValue("numComponents");
        if (numComponentsString != null) {
            numComponents = Integer.parseInt(numComponentsString);
        }
        pls_algorithm = clientParameters.getFirstValue("algorithm");
        if (pls_algorithm == null) {
            pls_algorithm = "PLS1";
        }
        /*
         * This is a mandatory parameter:
         */
        target = clientParameters.getFirstValue("target");
        return this;
    }

    @Override
    public Algorithm getAlgorithm() {
        return Algorithms.plsFilter();
    }

    @Override
    public Model train(Instances data) throws JaqpotException {
        Model model = new Model(Configuration.getBaseUri().augment("model", getUuid().toString()));

        /*
         * Remove unnecessary (string) features
         */
        AttributeCleanup cleanup = new AttributeCleanup(false, AttributeCleanup.ATTRIBUTE_TYPE.string);
        try {
            data = cleanup.filter(data);
        } catch (QSARException ex) {
            throw new JaqpotException(ex);
        }
        data.setClass(data.attribute(target));

        model.setIndependentFeatures(new ArrayList<Feature>(data.numAttributes()));
        for (int i = 0; i < data.numAttributes(); i++) {
            try {
                model.getIndependentFeatures().add(new Feature(new VRI(data.attribute(i).name())));
            } catch (URISyntaxException ex) {
                throw new RuntimeException(ex);
            }
        }


        /*
         * Train the PLS filter
         */
        PLSFilter pls = new PLSFilter();
        try {
            pls.setInputFormat(data);
            pls.setOptions(new String[]{"-C", numComponents + "", "-A", pls_algorithm, "-P", "none", "-U", "off"});
            System.out.println(
                    PLSFilter.useFilter(data, pls));
        } catch (Exception ex) {
            Logger.getLogger(PLSTrainer.class.getName()).log(Level.SEVERE, null, ex);
        }

        PLSModel actualModel = new PLSModel(pls);
        try {
            model.setActualModel(actualModel);
        } catch (NotSerializableException ex) {
            Logger.getLogger(PLSTrainer.class.getName()).log(Level.SEVERE, null, ex);
            throw new JaqpotException(ex);
        }
        model.setDataset(datasetUri);
        model.setAlgorithm(Algorithms.plsFilter());


        return model;
    }
}

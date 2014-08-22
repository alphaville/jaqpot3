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

import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import java.io.NotSerializableException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import static java.util.Arrays.asList;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.opentox.jaqpot3.exception.JaqpotException;
import org.opentox.jaqpot3.qsar.AbstractTrainer;
import org.opentox.jaqpot3.qsar.IClientInput;
import org.opentox.jaqpot3.qsar.IParametrizableAlgorithm;
import org.opentox.jaqpot3.qsar.exceptions.BadParameterException;
import org.opentox.jaqpot3.qsar.serializable.PLSModel;
import org.opentox.jaqpot3.resources.collections.Algorithms;
import org.opentox.jaqpot3.util.Configuration;
import org.opentox.toxotis.client.VRI;
import org.opentox.toxotis.client.collection.Services;
import org.opentox.toxotis.core.component.ActualModel;
import org.opentox.toxotis.core.component.Algorithm;
import org.opentox.toxotis.core.component.Feature;
import org.opentox.toxotis.core.component.Model;
import org.opentox.toxotis.core.component.Parameter;
import org.opentox.toxotis.ontology.LiteralValue;
import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.functions.LinearRegression;
import weka.classifiers.functions.PLSClassifier;
import weka.core.Instances;
import weka.filters.supervised.attribute.PLSFilter;

/**
 *
 * @author Pantelis Sopasakis
 * @author Charalampos Chomenides
 */
public class PLSTrainer extends AbstractTrainer {

    private static final Random RANDOM = new Random(11 * System.currentTimeMillis() + 21);
    private VRI featureService;
    private VRI datasetUri;
    private VRI targetUri;
    private int numComponents;
    private String preprocessing;
    private String pls_algorithm;
    private String doUpdateClass;

    @Override protected boolean keepNumeric() { return true; }
    @Override protected boolean keepNominal() { return true; }
    @Override protected boolean keepString()  { return false; }
    @Override protected boolean pmmlSupported()  { return true; }
    @Override protected boolean scalingSupported()  { return true; }
    @Override protected boolean normalizationSupported()  { return true; }
    @Override protected boolean DoASupported()  { return true; }
    @Override protected boolean performMVH()  { return false; }
    
    @Override
    public IParametrizableAlgorithm doParametrize(IClientInput clientParameters) throws BadParameterException {
        //PLS is a filtering algorithm and doesnt uses prediction feature
        //instead a target feature must be specified in the bottom and it may be any of the other features
        //clientParameters.getFirstValue("prediction_feature")
        
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
        if (!pls_algorithm.equals("PLS1") && !pls_algorithm.equals("SIMPLS")) {
            throw new BadParameterException("Algorithm not acceptable : " + pls_algorithm + ". Admissible "
                    + "values are PLS1 and SIMPLS");
        }

        preprocessing = clientParameters.getFirstValue("preprocessing");
        if (preprocessing == null) {
            preprocessing = "none";
        }
        if (!preprocessing.equals("none") && !preprocessing.equals("standardize") && !preprocessing.equals("center")) {
            throw new BadParameterException("Bad Parameter : '" + preprocessing + "'. Admissible values for the parameter 'preprocessing' "
                    + "are 'none', 'center' and 'standardize'.");
        }

        doUpdateClass = clientParameters.getFirstValue("doUpdateClass");
        if (doUpdateClass == null) {
            doUpdateClass = "off";
        }
        if (!doUpdateClass.equals("off") && !doUpdateClass.equals("on")) {
            throw new BadParameterException("Bad Parameter : '" + doUpdateClass + "'. Admissible values for the "
                    + "parameter doUpdateClass are only 'on' and 'off'.");
        }
        
        String targetString = clientParameters.getFirstValue("target");
        if (targetString == null) {
            throw new BadParameterException("The parameter 'target' is mandatory for this algorithm.");
        }
        try {
            targetUri = new VRI(targetString);
        } catch (URISyntaxException ex) {
            throw new BadParameterException("The parameter 'target' you provided is not a valid URI.", ex);
        }
        return this;
    }

    @Override
    public Algorithm getAlgorithm() {
        return Algorithms.plsFilter();
    }

    @Override
    public Model train(Instances data) throws JaqpotException {
        Model model = new Model(Configuration.getBaseUri().augment("model", getUuid().toString()));

        data.setClass(data.attribute(targetUri.toString()));

        model.setIndependentFeatures(independentFeatures);
       
        /*
         * Train the PLS filter
         */
        PLSFilter pls = new PLSFilter();
        try {
            pls.setInputFormat(data);
            pls.setOptions(new String[]{"-C", Integer.toString(numComponents),
                        "-A", pls_algorithm,
                        "-P", preprocessing,
                        "-U", doUpdateClass});
            PLSFilter.useFilter(data, pls);
        } catch (Exception ex) {
            Logger.getLogger(PLSTrainer.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        PLSModel actualModel = new PLSModel(pls);
        try {
            
            PLSClassifier cls = new PLSClassifier();
            cls.setFilter(pls);
            cls.buildClassifier(data);
            
            // evaluate classifier and print some statistics
            Evaluation eval = new Evaluation(data);
            eval.evaluateModel(cls, data);
            String stats = eval.toSummaryString("", false);

            ActualModel am = new ActualModel(actualModel);
            am.setStatistics(stats);
            
            model.setActualModel(am);
        } catch (NotSerializableException ex) {
            Logger.getLogger(PLSTrainer.class.getName()).log(Level.SEVERE, null, ex);
            throw new JaqpotException(ex);
        } catch (Exception ex) {
            Logger.getLogger(PLSTrainer.class.getName()).log(Level.SEVERE, null, ex);
            throw new JaqpotException(ex);
        }
       
        model.setDataset(datasetUri);
        model.setAlgorithm(Algorithms.plsFilter());
        model.getMeta().addTitle("PLS Model for " + datasetUri);

        Set<Parameter> parameters = new HashSet<Parameter>();
        Parameter targetPrm = new Parameter(Configuration.getBaseUri().augment("parameter", RANDOM.nextLong()),
                "target", new LiteralValue(targetUri.toString(), XSDDatatype.XSDstring)).setScope(Parameter.ParameterScope.MANDATORY);
        Parameter nComponentsPrm = new Parameter(Configuration.getBaseUri().
                augment("parameter", RANDOM.nextLong()), "numComponents",
                new LiteralValue(numComponents, XSDDatatype.XSDpositiveInteger)).setScope(Parameter.ParameterScope.MANDATORY);
        Parameter preprocessingPrm = new Parameter(Configuration.getBaseUri().
                augment("parameter", RANDOM.nextLong()), "preprocessing",
                new LiteralValue(preprocessing, XSDDatatype.XSDstring)).setScope(Parameter.ParameterScope.OPTIONAL);
        Parameter algorithmPrm = new Parameter(Configuration.getBaseUri().
                augment("parameter", RANDOM.nextLong()), "algorithm",
                new LiteralValue(pls_algorithm, XSDDatatype.XSDstring)).setScope(Parameter.ParameterScope.OPTIONAL);
        Parameter doUpdatePrm = new Parameter(Configuration.getBaseUri().
                augment("parameter", RANDOM.nextLong()), "doUpdateClass",
                new LiteralValue(doUpdateClass, XSDDatatype.XSDboolean)).setScope(Parameter.ParameterScope.OPTIONAL);

        parameters.add(targetPrm);
        parameters.add(nComponentsPrm);
        parameters.add(preprocessingPrm);
        parameters.add(doUpdatePrm);
        parameters.add(algorithmPrm);
        model.setParameters(parameters);


        for (int i = 0; i < numComponents; i++) {
            Feature f = publishFeature(model,"","PLS-" + i,datasetUri,featureService);
            model.addPredictedFeatures(f);
        }
            
        //save the instances being predicted to abstract trainer for calculating DoA
        predictedInstances = data;
        model.getActualModel().setExcludeFeatures(asList(targetUri));
        
        return model;
    }
}

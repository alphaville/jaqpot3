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
import static java.util.Arrays.asList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import org.opentox.jaqpot3.exception.JaqpotException;
import org.opentox.jaqpot3.qsar.AbstractTrainer;
import org.opentox.jaqpot3.qsar.IClientInput;
import org.opentox.jaqpot3.qsar.ITrainer;
import org.opentox.jaqpot3.qsar.InstancesUtil;
import org.opentox.jaqpot3.qsar.exceptions.BadParameterException;
import org.opentox.jaqpot3.qsar.exceptions.QSARException;
import org.opentox.jaqpot3.resources.collections.Algorithms;
import org.opentox.jaqpot3.util.Configuration;
import org.opentox.toxotis.client.VRI;
import org.opentox.toxotis.client.collection.Services;
import org.opentox.toxotis.core.component.ActualModel;
import org.opentox.toxotis.core.component.Algorithm;
import org.opentox.toxotis.core.component.Feature;
import org.opentox.toxotis.core.component.Model;
import org.opentox.toxotis.core.component.Parameter;
import org.opentox.toxotis.database.engine.task.UpdateTask;
import org.opentox.toxotis.database.exception.DbException;
import org.opentox.toxotis.exceptions.impl.ServiceInvocationException;
import org.opentox.toxotis.ontology.LiteralValue;
import weka.classifiers.functions.SVMreg;
import weka.classifiers.functions.supportVector.Kernel;
import weka.classifiers.functions.supportVector.PolyKernel;
import weka.classifiers.functions.supportVector.RBFKernel;
import weka.core.Attribute;
import weka.core.Instances;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import weka.classifiers.Evaluation;
import weka.classifiers.functions.LibSVM;
import weka.classifiers.functions.supportVector.KernelEvaluation;

/**
 *
 * This SVM Trainer accepts the training data as instances and produces a model file
 * which is saved in the corresponding folder on the server for weka serialized models.
 * What is more, a PMML file is generated and stored as well.
 * @author Pantelis Sopasakis
 * @author Charalampos Chomenides
 */
public class SvmRegression extends AbstractTrainer {

    private static final Random RANDOM = new Random(19 * System.currentTimeMillis() + 53);

    @Override protected boolean keepNumeric() { return true; }
    @Override protected boolean keepNominal() { return true; }
    @Override protected boolean keepString()  { return false; }
    @Override protected boolean pmmlSupported()  { return true; }
    @Override protected boolean scalingSupported()  { return true; }
    @Override protected boolean normalizationSupported()  { return true; }
    @Override protected boolean DoASupported()  { return true; }
    @Override protected boolean performMVH()  { return true; }
    
    @Override
    public Algorithm getAlgorithm() {
        return Algorithms.svm();
    }

    private enum SvmParameter {

        gamma,
        cost,
        epsilon,
        tolerance,
        cacheSize,
        degree,
        kernel;
    }
    private double gamma = 1.50,
            cost = 100.0,
            epsilon = 0.100,
            tolerance = 0.0001;
    private int cacheSize = 250007,
            degree = 3;
    private String kernel = "RBF";
    private VRI targetUri;
    private VRI datasetUri;
    private VRI featureService;
    private Logger logger = LoggerFactory.getLogger(SvmRegression.class);

    public SvmRegression() {
    }

    @Override
    public Model train(Instances data) throws JaqpotException {
        try {
            Attribute target = data.attribute(targetUri.toString());
            if (target == null) {
                throw new QSARException("The prediction feature you provided was not found in the dataset");
            } else {
                if (!target.isNumeric()) {
                    throw new QSARException("The prediction feature you provided is not numeric.");
                }
            }
            data.setClass(target);
            //data.deleteAttributeAt(0);//remove the first attribute, i.e. 'compound_uri' or 'URI'
            /* Very important: place the target feature at the end! (target = last)*/
            int numAttributes = data.numAttributes();
            int classIndex = data.classIndex();
            Instances orderedTrainingSet = null;
            List<String> properOrder = new ArrayList<String>(numAttributes);
            for (int j = 0; j < numAttributes; j++) {
                if (j != classIndex) {
                    properOrder.add(data.attribute(j).name());
                }
            }
            properOrder.add(data.attribute(classIndex).name());
            try {
                orderedTrainingSet = InstancesUtil.sortByFeatureAttrList(properOrder, data, -1);
            } catch (JaqpotException ex) {
                logger.error(null, ex);
            }
            orderedTrainingSet.setClass(orderedTrainingSet.attribute(targetUri.toString()));

            getTask().getMeta().addComment("Dataset successfully retrieved and converted into a weka.core.Instances object");
            UpdateTask firstTaskUpdater = new UpdateTask(getTask());
            firstTaskUpdater.setUpdateMeta(true);
            firstTaskUpdater.setUpdateTaskStatus(true);//TODO: Is this necessary?
            try {
                firstTaskUpdater.update();
            } catch (DbException ex) {
                throw new JaqpotException(ex);
            } finally {
                try {
                    firstTaskUpdater.close();
                } catch (DbException ex) {
                    throw new JaqpotException(ex);
                }
            }

            Model m = new Model(Configuration.getBaseUri().augment("model", getUuid().toString()));
            
            // INITIALIZE THE REGRESSOR regressor
            SVMreg regressor = new SVMreg();
            final String[] regressorOptions = {
                "-P", Double.toString(epsilon),
                "-T", Double.toString(tolerance)
            };
            Kernel svm_kernel = null;
            if (kernel.equalsIgnoreCase("rbf")) {
                RBFKernel rbf_kernel = new RBFKernel();
                rbf_kernel.setGamma(Double.parseDouble(Double.toString(gamma)));
                rbf_kernel.setCacheSize(Integer.parseInt(Integer.toString(cacheSize)));
                svm_kernel = rbf_kernel;
            } else if (kernel.equalsIgnoreCase("polynomial")) {
                PolyKernel poly_kernel = new PolyKernel();
                poly_kernel.setExponent(Double.parseDouble(Integer.toString(degree)));
                poly_kernel.setCacheSize(Integer.parseInt(Integer.toString(cacheSize)));
                poly_kernel.setUseLowerOrder(true);
                svm_kernel = poly_kernel;
            } else if (kernel.equalsIgnoreCase("linear")) {
                PolyKernel poly_kernel = new PolyKernel();
                poly_kernel.setExponent((double) 1.0);
                poly_kernel.setCacheSize(Integer.parseInt(Integer.toString(cacheSize)));
                poly_kernel.setUseLowerOrder(true);
                svm_kernel = poly_kernel;
            }
            
            try {
                regressor.setOptions(regressorOptions);
            } catch (final Exception ex) {
                throw new QSARException("Bad options in SVM trainer for epsilon = {" + epsilon + "} or "
                        + "tolerance = {" + tolerance + "}.", ex);
            }
            regressor.setKernel(svm_kernel);
            // START TRAINING & CREATE MODEL
            try {
                regressor.buildClassifier(orderedTrainingSet);
                
                // evaluate classifier and print some statistics
                Evaluation eval = new Evaluation(orderedTrainingSet);
                eval.evaluateModel(regressor, orderedTrainingSet);
                String stats = eval.toSummaryString("", false);
                
                ActualModel am = new ActualModel(regressor);
                am.setStatistics(stats);
                m.setActualModel(am);
               // m.setStatistics(stats);
            } catch (NotSerializableException ex) {
                String message = "Model is not serializable";
                logger.error(message, ex);
                throw new JaqpotException(message, ex);
            }  catch (final Exception ex) {
                throw new QSARException("Unexpected condition while trying to train "
                        + "the model. Possible explanation : {" + ex.getMessage() + "}", ex);
            } 
            
            m.setAlgorithm(getAlgorithm());
            m.setCreatedBy(getTask().getCreatedBy());
            m.setDataset(datasetUri);
            m.addDependentFeatures(dependentFeature);
            try {
                dependentFeature.loadFromRemote();
            } catch (ServiceInvocationException ex) {
                java.util.logging.Logger.getLogger(SvmRegression.class.getName()).log(Level.SEVERE, null, ex);
            }
            m.addDependentFeatures(dependentFeature);

            m.setIndependentFeatures(independentFeatures);
            
            String predictionFeatureUri = null;
            Feature predictedFeature = publishFeature(m,dependentFeature.getUnits(),"Feature created as prediction feature for SVM model " + m.getUri(),datasetUri,featureService);
            m.addPredictedFeatures(predictedFeature);
            predictionFeatureUri = predictedFeature.getUri().toString();
            
            getTask().getMeta().addComment("Prediction feature " + predictionFeatureUri + " was created.");
            
            /* SET PARAMETERS FOR THE TRAINED MODEL */
            m.setParameters(new HashSet<Parameter>());
            Parameter<String> kernelParam = new Parameter("kernel", new LiteralValue<String>(kernel)).setScope(Parameter.ParameterScope.OPTIONAL);
            kernelParam.setUri(Services.anonymous().augment("parameter", RANDOM.nextLong()));
            Parameter<Double> costParam = new Parameter("cost", new LiteralValue<Double>(cost)).setScope(Parameter.ParameterScope.OPTIONAL);
            costParam.setUri(Services.anonymous().augment("parameter", RANDOM.nextLong()));
            Parameter<Double> gammaParam = new Parameter("gamma", new LiteralValue<Double>(gamma)).setScope(Parameter.ParameterScope.OPTIONAL);
            gammaParam.setUri(Services.anonymous().augment("parameter", RANDOM.nextLong()));
            Parameter<Double> epsilonParam = new Parameter("espilon", new LiteralValue<Double>(epsilon)).setScope(Parameter.ParameterScope.OPTIONAL);
            epsilonParam.setUri(Services.anonymous().augment("parameter", RANDOM.nextLong()));
            Parameter<Integer> degreeParam = new Parameter("degree", new LiteralValue<Integer>(degree)).setScope(Parameter.ParameterScope.OPTIONAL);
            degreeParam.setUri(Services.anonymous().augment("parameter", RANDOM.nextLong()));
            Parameter<Double> toleranceParam = new Parameter("tolerance", new LiteralValue<Double>(tolerance)).setScope(Parameter.ParameterScope.OPTIONAL);
            toleranceParam.setUri(Services.anonymous().augment("parameter", RANDOM.nextLong()));

            m.getParameters().add(kernelParam);
            m.getParameters().add(costParam);
            m.getParameters().add(gammaParam);
            m.getParameters().add(epsilonParam);
            m.getParameters().add(degreeParam);
            m.getParameters().add(toleranceParam);

            //save the instances being predicted to abstract trainer for calculating DoA
            predictedInstances = orderedTrainingSet;
            excludeAttributesDoA.add(dependentFeature.getUri().toString());
            
            return m;
        } catch (QSARException ex) {
            logger.debug(null, ex);
            throw new JaqpotException(ex);
        }
    }

    @Override
    public ITrainer doParametrize(IClientInput clientParameters) throws BadParameterException {
        String targetString = clientParameters.getFirstValue("prediction_feature");
        if (targetString == null) {
            throw new BadParameterException("The parameter 'prediction_feature' is mandatory for this algorithm.");
        }
        try {
            targetUri = new VRI(targetString);
        } catch (URISyntaxException ex) {
            throw new BadParameterException("The parameter 'prediction_feature' you provided is not a valid URI.", ex);
        }
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

        for (Parameter p : getAlgorithm().getParameters()) {
            Boolean exists = false;
            for(SvmParameter temp : SvmParameter.values()) {
                if(temp.name() == p.getName().getValueAsString()) {
                    exists = true;
                    break;
                }
            }
            if(exists) {
                try {
                    switch (SvmParameter.valueOf(p.getName().getValueAsString())) {
                        case gamma:
                            String gammaString = clientParameters.getFirstValue(SvmParameter.gamma.name());
                            if (gammaString == null) {
                                gamma = 1.5;
                                break;
                            }
                            gamma = Double.parseDouble(gammaString);
                            if (gamma <= 0) {
                                throw new BadParameterException(
                                        "The parameter " + p.getName() + " must be strictly positive. "
                                        + "You provided the illegal value: {" + p.getValue() + "}");
                            }
                            p.getTypedValue().setValue(gamma);

                            break;

                        case cost:
                            String costString = clientParameters.getFirstValue(SvmParameter.cost.name());
                            if (costString == null) {
                                cost = 100;
                                break;
                            }
                            cost = Double.parseDouble(costString);
                            if (cost <= 0) {
                                throw new BadParameterException(
                                        "The parameter " + p.getName() + " must be strictly positive. "
                                        + "You provided the illegal value: {" + p.getValue() + "}");
                            }
                            p.getTypedValue().setValue(cost);
                            break;

                        case epsilon:
                            String epsilonString = clientParameters.getFirstValue(SvmParameter.epsilon.name());
                            if (epsilonString == null) {
                                epsilon = 0.10;
                                break;
                            }
                            epsilon = Double.parseDouble(epsilonString);
                            if (epsilon <= 0) {
                                throw new BadParameterException(
                                        "The parameter " + p.getName() + " must be strictly positive. "
                                        + "You provided the illegal value: {" + p.getValue() + "}");
                            }
                            p.getTypedValue().setValue(epsilon);
                            break;

                        case tolerance:
                            String toleranceString = clientParameters.getFirstValue(SvmParameter.tolerance.name());
                            if (toleranceString == null) {
                                tolerance = 0.0001;
                                break;
                            }
                            tolerance = Double.parseDouble(toleranceString);
                            if (tolerance < 1E-6) {
                                throw new BadParameterException(
                                        "The parameter " + p.getName() + " must be greater that 1E-6. "
                                        + "You provided the illegal value: {" + p.getValue() + "}");
                            }
                            p.getTypedValue().setValue(tolerance);
                            break;

                        case cacheSize:
                            String cacheSizeString = clientParameters.getFirstValue(SvmParameter.cacheSize.name());
                            if (cacheSizeString == null) {
                                cacheSize = 25007;
                                break;
                            }
                            cacheSize = Integer.parseInt(cacheSizeString);
                            p.getTypedValue().setValue(cacheSize);
                            break;

                        case degree:
                            String degreeString = clientParameters.getFirstValue(SvmParameter.degree.name());
                            if (degreeString == null) {
                                degree = 3;
                                break;
                            }
                            p.getTypedValue().setValue(degreeString);
                            degree = Integer.parseInt(degreeString);
                            if (degree <= 0) {
                                throw new BadParameterException("The parameter degree should be a strictly "
                                        + "POSITIVE integer (greater of equal to 1)");
                            }
                            p.getTypedValue().setValue(degree);
                            break;

                        case kernel:
                            kernel = clientParameters.getFirstValue(SvmParameter.kernel.name());
                            if (kernel == null) {
                                kernel = "RBF";
                            }
                            kernel = kernel.toUpperCase();
                            if (!kernel.equals("RBF") && !kernel.equals("LINEAR") && !kernel.equals("POLYNOMIAL")) {
                                throw new BadParameterException("The available kernels are [RBF; LINEAR; POLYNOMIAL]. Note that "
                                        + "this parameter is not case-sensitive, i.e. rbf is the same as RbF. However you provided "
                                        + "the illegal value : {" + kernel + "}");
                            }
                            p.getTypedValue().setValue(kernel);
                            break;

                        default:
                            break;

                    }
                } catch (NumberFormatException ex) {
                    String message = "Parameter " + p.getName() + " should be numeric. "
                            + "You provided the illegal "
                            + "value : {" + p.getTypedValue() + "} ";
                    throw new BadParameterException(message, ex);
                }
            }
        }
        return this;
    }

    /*private String attributeHint(Instances data) {
        final String NEWLINE = "\n";
        StringBuilder hint = new StringBuilder();
        hint.append("Available features :");
        hint.append(NEWLINE);
        final int MAX_ROWS = 5;
        int jndex = 0;
        Attribute temp;
        for (int i = 0; i < data.numAttributes() && jndex < MAX_ROWS; i++) {
            temp = data.attribute(i);
            if (temp.type() == Attribute.NUMERIC) {
                hint.append(temp.name());
                hint.append(NEWLINE);
                jndex++;
            }
        }
        return new String(hint);
    }*/
}

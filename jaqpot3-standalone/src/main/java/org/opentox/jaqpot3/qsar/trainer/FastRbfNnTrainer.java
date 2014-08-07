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

import org.opentox.jaqpot3.qsar.util.SimpleMVHFilter;
import org.opentox.jaqpot3.qsar.serializable.FastRbfNnModel;
import Jama.Matrix;
import java.io.NotSerializableException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import org.opentox.jaqpot3.exception.JaqpotException;
import org.opentox.jaqpot3.qsar.AbstractTrainer;
import org.opentox.jaqpot3.qsar.IClientInput;
import org.opentox.jaqpot3.qsar.ITrainer;
import org.opentox.jaqpot3.qsar.exceptions.BadParameterException;
import org.opentox.jaqpot3.qsar.exceptions.QSARException;
import org.opentox.jaqpot3.qsar.util.AttributeCleanup;
import org.opentox.jaqpot3.resources.collections.Algorithms;
import org.opentox.jaqpot3.util.Configuration;
import org.opentox.toxotis.client.VRI;
import org.opentox.toxotis.client.collection.Services;
import org.opentox.toxotis.core.component.Algorithm;
import org.opentox.toxotis.core.component.Dataset;
import org.opentox.toxotis.core.component.Feature;
import org.opentox.toxotis.core.component.Model;
import org.opentox.toxotis.core.component.Parameter;
import org.opentox.toxotis.exceptions.impl.ServiceInvocationException;
import org.opentox.toxotis.factory.FeatureFactory;
import org.opentox.toxotis.ontology.LiteralValue;
import org.opentox.toxotis.ontology.ResourceValue;
import org.opentox.toxotis.ontology.collection.OTClasses;
import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;

import static org.opentox.jaqpot3.qsar.util.AttributeCleanup.AttributeType.*;
import org.opentox.toxotis.core.component.ActualModel;

/**
 *
 * @author Pantelis Sopasakis
 * @author Charalampos Chomenides
 */
public class FastRbfNnTrainer extends AbstractTrainer {

    private VRI targetUri;
    private VRI datasetUri;
    private VRI featureService;
    private double a = 1;
    private double b = 0.9;
    private double e = 0.6;
    private int p = 5;
    private org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(FastRbfNnModel.class);
    private static final Random RANDOM = new Random(3 * System.currentTimeMillis() + 71);

    @Override protected boolean keepNumeric() { return true; }
    @Override protected boolean keepNominal() { return false; }
    @Override protected boolean keepString()  { return false; }
    @Override protected boolean pmmlSupported()  { return true; }
    @Override protected boolean scalingSupported()  { return true; }
    @Override protected boolean normalizationSupported()  { return true; }
    @Override protected boolean DoASupported()  { return true; }
    @Override protected boolean performMVH()  { return true; }


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

    private double[] calculatePotential(Instances in) {
        int K = in.numInstances();
        double[] potential = new double[K];
        double sum = 0;
        Instance currectInstance = null;
        for (int i = 0; i < K; i++) {
            sum = 0;
            currectInstance = in.instance(i);
            for (int j = 0; j < K; j++) {
                sum += Math.exp(-a * squaredNormDifference(currectInstance, in.instance(j)));
            }
            potential[i] = sum;
        }
        return potential;
    }

    private static int locationOfMax(double[] values) {
        int maxLoc = 0;
        int i = 0;
        double maxValue = Double.MIN_VALUE;
        for (double value : values) {
            if (value > maxValue) {
                maxValue = value;
                maxLoc = i;
            }
            i++;
        }
        return maxLoc;
    }

    private static int[] locationOfpMinimum(int p, double[] data) {
        double[] datacopy = Arrays.copyOf(data, data.length);
        Arrays.sort(datacopy);
        List listdata = new ArrayList();
        for (double d : data) {
            listdata.add(d);
        }
        int[] result = new int[p];
        for (int i = 0; i < p; i++) {
            result[i] = listdata.indexOf(datacopy[i]);
        }
        return result;
    }

    private double[] updatePotential(double[] oldPotential, int i_star, Instances data) {
        double potential_star = oldPotential[i_star];
        Instance instance_star = data.instance(i_star);
        for (int i = 0; i < oldPotential.length; i++) {
            oldPotential[i] = oldPotential[i] - potential_star * Math.exp(-b * squaredNormDifference(data.instance(i), instance_star));
        }
        return oldPotential;
    }

    private static double rbf(double sigma, Instance x, Instance node) {
        double result = Math.exp(-squaredNormDifference(x, node) / Math.pow(sigma, 2));
        return result;
    }

    @Override
    public Model train(Instances training) throws JaqpotException {
        /*
         * For this algorithm we need to remove all string and nominal attributes
         * and additionally we will remove the target attribute too.
         */
        
        Instances cleanedTraining = training;
        
        Attribute targetAttribute = cleanedTraining.attribute(targetUri.toString());
        if (targetAttribute == null) {
            throw new JaqpotException("The prediction feature you provided was not found in the dataset. "
                    + "Prediction Feature provided by the client: " + targetUri.toString());
        } else {
            if (!targetAttribute.isNumeric()) {
                throw new JaqpotException("The prediction feature you provided is not numeric.");
            }
        }
        double[] targetValues = new double[cleanedTraining.numInstances()];
        for (int i = 0; i < cleanedTraining.numInstances(); i++) {
            targetValues[i] = cleanedTraining.instance(i).value(targetAttribute);
        }
        cleanedTraining.deleteAttributeAt(targetAttribute.index());


        Instances rbfNnNodes = new Instances(cleanedTraining);
        rbfNnNodes.delete();
        double[] potential = calculatePotential(cleanedTraining);

        int L = 1;
        int i_star = locationOfMax(potential);
        double potential_star = potential[i_star];
        double potential_star_1 = potential_star;
        do {
            rbfNnNodes.add(cleanedTraining.instance(i_star));
            potential = updatePotential(potential, i_star, cleanedTraining);
            i_star = locationOfMax(potential);
            double diff = potential[i_star] - e * potential_star_1;
            if (Double.isNaN(diff)) {
                throw new JaqpotException("Not converging");
            }
            if (potential[i_star] <= e * potential_star_1) {
                break;
            } else {
                L = L + 1;
                potential_star = potential[i_star];
            }
        } while (true);

        /* P-nearest neighbors */
        double[] pNn = null;
        double[] sigma = new double[rbfNnNodes.numInstances()];
        double s = 0;
        for (int i = 0; i < rbfNnNodes.numInstances(); i++) {
            pNn = new double[cleanedTraining.numInstances()];
            s = 0;
            for (int j = 0; j < cleanedTraining.numInstances(); j++) {
                if (j != i) {
                    pNn[j] = squaredNormDifference(rbfNnNodes.instance(i), cleanedTraining.instance(j));
                } else {
                    pNn[j] = 0;
                }
            }
            int[] minPoints = locationOfpMinimum(p, pNn); // indices refer to 'cleanedTraining'
            for (int q : minPoints) {
                s += squaredNormDifference(rbfNnNodes.instance(i), cleanedTraining.instance(q));
            }
            sigma[i] = Math.sqrt(s / p);
        }

        /* Caclulate the matrix X = (l_{i,j})_{i,j} */
        double[][] X = new double[cleanedTraining.numInstances()][rbfNnNodes.numInstances()];
        for (int i = 0; i < cleanedTraining.numInstances(); i++) {
            for (int j = 0; j < rbfNnNodes.numInstances(); j++) {
                X[i][j] = rbf(sigma[j], cleanedTraining.instance(i), rbfNnNodes.instance(j));
            }
        }

        Jama.Matrix X_matr = new Matrix(X);
        Jama.Matrix Y_matr = new Matrix(targetValues, targetValues.length);
        Jama.Matrix coeffs = (X_matr.transpose().times(X_matr)).inverse().times(X_matr.transpose()).times(Y_matr);        


        FastRbfNnModel actualModel = new FastRbfNnModel();
        actualModel.setAlpha(a);
        actualModel.setBeta(b);
        actualModel.setEpsilon(e);
        actualModel.setNodes(rbfNnNodes);
        actualModel.setSigma(sigma);
        actualModel.setLrCoefficients(coeffs.getColumnPackedCopy());

        Model m = new Model(Configuration.getBaseUri().augment("model", getUuid().toString()));
        m.setAlgorithm(getAlgorithm());
        m.setCreatedBy(getTask().getCreatedBy());
        m.setDataset(datasetUri);
        m.addDependentFeatures(dependentFeature);
        
        Feature predictedFeature = publishFeature(m,dependentFeature.getUnits(),"Created as prediction feature for the RBF NN model " + m.getUri(),datasetUri,featureService);
        m.addPredictedFeatures(predictedFeature);
        
        m.setIndependentFeatures(independentFeatures);
        try {
            m.setActualModel(new ActualModel(actualModel));
        } catch (NotSerializableException ex) {
            logger.error("The provided instance of model cannot be serialized! Critical Error!", ex);
        }
        m.setParameters(new HashSet<Parameter>());
        Parameter<Double> aParam = new Parameter("a", new LiteralValue<Double>(a)).setScope(Parameter.ParameterScope.OPTIONAL);
        aParam.setUri(Services.anonymous().augment("parameter", RANDOM.nextLong()));
        Parameter<Double> bParam = new Parameter("b", new LiteralValue<Double>(b)).setScope(Parameter.ParameterScope.OPTIONAL);
        bParam.setUri(Services.anonymous().augment("parameter", RANDOM.nextLong()));
        Parameter<Double> eParam = new Parameter("e", new LiteralValue<Double>(e)).setScope(Parameter.ParameterScope.OPTIONAL);
        eParam.setUri(Services.anonymous().augment("parameter", RANDOM.nextLong()));

        m.getParameters().add(aParam);
        m.getParameters().add(bParam);
        m.getParameters().add(eParam);
        
        //todo set instances for DoA 
        return m;
    }

    @Override
    public ITrainer doParametrize(IClientInput clientParameters) throws BadParameterException {
        String targetString = clientParameters.getFirstValue("prediction_feature");
        if (targetString == null) {
            throw new BadParameterException("The parameter 'prediction_feaure' is mandatory for this algorithm.");
        }
        try {
            targetUri = new VRI(targetString);
        } catch (URISyntaxException ex) {
            throw new BadParameterException("The parameter 'prediction_feaure' you provided is not a valid URI.", ex);
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
        String alphaString = clientParameters.getFirstValue("a");
        if (alphaString != null) {
            try {
                a = Double.parseDouble(alphaString);
            } catch (NumberFormatException ex) {
                throw new BadParameterException("The parameter 'a' accepts only double values", ex);
            }
        }
        String betaString = clientParameters.getFirstValue("b");
        if (betaString != null) {
            try {
                b = Double.parseDouble(betaString);
            } catch (NumberFormatException ex) {
                throw new BadParameterException("The parameter 'b' accepts only double values", ex);
            }
        }
        String epsilonString = clientParameters.getFirstValue("e");
        if (epsilonString != null) {
            try {
                e = Double.parseDouble(epsilonString);
            } catch (NumberFormatException ex) {
                throw new BadParameterException("The parameter 'e' accepts only double values", ex);
            }
        }
        String pString = clientParameters.getFirstValue("p");
        if (pString != null) {
            try {
                p = Integer.parseInt(pString);
            } catch (NumberFormatException ex) {
                throw new BadParameterException("The parameter 'p' accepts only integer values", ex);
            }
        }
        return this;
    }

    @Override
    public Algorithm getAlgorithm() {
        return Algorithms.fastRbfNn();
    }

}

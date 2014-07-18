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

import com.google.common.collect.Sets.SetView;
import java.io.NotSerializableException;
import java.net.URISyntaxException;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.opentox.jaqpot3.exception.JaqpotException;
import org.opentox.jaqpot3.qsar.AbstractTrainer;
import org.opentox.jaqpot3.qsar.IClientInput;
import org.opentox.jaqpot3.qsar.ITrainer;
import org.opentox.jaqpot3.qsar.InstancesUtil;
import org.opentox.jaqpot3.qsar.exceptions.BadParameterException;
import org.opentox.jaqpot3.qsar.exceptions.QSARException;
import org.opentox.jaqpot3.qsar.util.WekaInstancesProcess;
import org.opentox.jaqpot3.resources.collections.Algorithms;
import org.opentox.jaqpot3.util.Configuration;
import org.opentox.toxotis.client.VRI;
import org.opentox.toxotis.client.collection.Services;
import org.opentox.toxotis.core.component.ActualModel;
import org.opentox.toxotis.core.component.Algorithm;
import org.opentox.toxotis.core.component.Feature;
import org.opentox.toxotis.core.component.Model;
import org.opentox.toxotis.database.engine.task.UpdateTask;
import org.opentox.toxotis.database.exception.DbException;
import org.opentox.toxotis.exceptions.impl.ServiceInvocationException;
import org.opentox.toxotis.factory.FeatureFactory;
import org.opentox.toxotis.ontology.LiteralValue;
import org.opentox.toxotis.ontology.ResourceValue;
import org.opentox.toxotis.ontology.collection.OTClasses;
import weka.core.Attribute;
import weka.core.Instances;
import weka.classifiers.functions.LinearRegression;

/**
 *
 * @author Pantelis Sopasakis
 * @author Charalampos Chomenides
 */
public class MlrRegression extends AbstractTrainer {

    private VRI targetUri;
    private VRI datasetUri;
    private VRI featureService;
    private org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(MlrRegression.class);

    @Override
    protected boolean keepNumeric() { return true; }
    @Override
    protected boolean keepNominal() { return true; }
    @Override
    protected boolean keepString()  { return false; }
    @Override
    protected boolean performMVH()  { return true; }
    
    public MlrRegression() {
    }


    /*private Instances preprocessInstances(Instances in) throws QSARException {
        AttributeCleanup cleanup = new AttributeCleanup(false, AttributeCleanup.AttributeType.string);
        try {
            Instances filt1 = cleanup.filter(in);
            SimpleMVHFilter mvh = new SimpleMVHFilter();
            Instances fin = mvh.filter(filt1);
            return fin;
        } catch (JaqpotException ex) {
            throw new QSARException(ex);
        } catch (QSARException ex) {
            throw new QSARException(ex);
        }
    }*/

    

    @Override
    public ITrainer doParametrize(IClientInput clientParameters) throws BadParameterException {
        String targetString = clientParameters.getFirstValue("prediction_feature");
        if (targetString == null) {
            throw new BadParameterException("The parameter 'prediction_feature' is mandatory for this algorithm.");
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
        return this;
    }

    @Override
    public Algorithm getAlgorithm() {
        return Algorithms.mlr();
    }

    @Override
    public Model train(Instances data) throws JaqpotException {
        data.renameAttribute(0, "compound_uri");
        try {

            getTask().getMeta().addComment("Dataset successfully retrieved and converted "
                    + "into a weka.core.Instances object");
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

            Instances trainingSet = data;
            getTask().getMeta().addComment("The downloaded dataset is now preprocessed");
            firstTaskUpdater = new UpdateTask(getTask());
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

            /* SET CLASS ATTRIBUTE */
            Attribute target = trainingSet.attribute(targetUri.toString());
            if (target == null) {
                throw new BadParameterException("The prediction feature you provided was not found in the dataset");
            } else {
                if (!target.isNumeric()) {
                    throw new QSARException("The prediction feature you provided is not numeric.");
                }
            }
            trainingSet.setClass(target);
            /* Very important: place the target feature at the end! (target = last)*/
            int numAttributes = trainingSet.numAttributes();
            int classIndex = trainingSet.classIndex();
            Instances orderedTrainingSet = null;
            List<String> properOrder = new ArrayList<String>(numAttributes);
            for (int j = 0; j < numAttributes; j++) {
                if (j != classIndex) {
                    properOrder.add(trainingSet.attribute(j).name());
                }
            }
            properOrder.add(trainingSet.attribute(classIndex).name());
            try {
                orderedTrainingSet = InstancesUtil.sortByFeatureAttrList(properOrder, trainingSet, -1);
            } catch (JaqpotException ex) {
                logger.error("Improper dataset - training will stop", ex);
                throw ex;
            }
            orderedTrainingSet.setClass(orderedTrainingSet.attribute(targetUri.toString()));

            /* START CONSTRUCTION OF MODEL */
            Model m = new Model(Configuration.getBaseUri().augment("model", getUuid().toString()));
            m.setAlgorithm(getAlgorithm());
            m.setCreatedBy(getTask().getCreatedBy());
            m.setDataset(datasetUri);
            m.addDependentFeatures(dependentFeature);
            try {
                dependentFeature.loadFromRemote();
            } catch (ServiceInvocationException ex) {
                Logger.getLogger(MlrRegression.class.getName()).log(Level.SEVERE, null, ex);
            }

            Set<LiteralValue> depFeatTitles = null;
            if(dependentFeature.getMeta()!=null){
                depFeatTitles = dependentFeature.getMeta().getTitles();
            }
                

            String depFeatTitle = dependentFeature.getUri().toString();
            if (depFeatTitles!=null) {
                depFeatTitle = depFeatTitles.iterator().next().getValueAsString();
                m.getMeta().addTitle("MLR model for " + depFeatTitle).
                        addDescription("MLR model for the prediction of " + depFeatTitle + " (uri: " + dependentFeature.getUri() + " ).");
            } else {
                m.getMeta().addTitle("MLR model for the prediction of the feature with URI " + depFeatTitle).
                        addComment("No name was found for the feature " + depFeatTitle);
            }

            /*
             * COMPILE THE LIST OF INDEPENDENT FEATURES with the exact order in which
             * these appear in the Instances object (training set).
             */
            m.setIndependentFeatures(independentFeatures);


            /* CREATE PREDICTED FEATURE AND POST IT TO REMOTE SERVER */
            String predictionFeatureUri = null;
            try {
                Feature predictedFeature = FeatureFactory.createAndPublishFeature(
                        "Predicted " + depFeatTitle + " by MLR model", dependentFeature.getUnits(),
                        new ResourceValue(m.getUri(), OTClasses.model()), featureService, token);
                m.addPredictedFeatures(predictedFeature);
                predictionFeatureUri = predictedFeature.getUri().toString();
            } catch (ServiceInvocationException ex) {
                logger.warn(null, ex);
                throw new JaqpotException(ex);
            }

            getTask().getMeta().addComment("Prediction feature " + predictionFeatureUri + " was created.");
            firstTaskUpdater = new UpdateTask(getTask());
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


            /* ACTUAL TRAINING OF THE MODEL USING WEKA */
            LinearRegression linreg = new LinearRegression();
            String[] linRegOptions = {"-S", "1", "-C"};

            try {
                linreg.setOptions(linRegOptions);
                linreg.buildClassifier(orderedTrainingSet);
            } catch (final Exception ex) {// illegal options or could not build the classifier!
                String message = "MLR Model could not be trained";
                logger.error(message, ex);
                throw new JaqpotException(message, ex);
            }
            try {
                m.setActualModel(new ActualModel(linreg));
            } catch (NotSerializableException ex) {
                String message = "Model is not serializable";
                logger.error(message, ex);
                throw new JaqpotException(message, ex);
            }
            m.getMeta().addPublisher("OpenTox").addComment("This is a Multiple Linear Regression Model");
            return m;
        } catch (QSARException ex) {
            String message = "QSAR Exception: cannot train MLR model";
            logger.error(message, ex);
            throw new JaqpotException(message, ex);
        }
    }

    
}

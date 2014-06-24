/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.opentox.jaqpot3.qsar.trainer;

import java.io.NotSerializableException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang.StringUtils;
import org.opentox.jaqpot3.exception.JaqpotException;
import org.opentox.jaqpot3.qsar.AbstractTrainer;
import org.opentox.jaqpot3.qsar.IClientInput;
import org.opentox.jaqpot3.qsar.IParametrizableAlgorithm;
import org.opentox.jaqpot3.qsar.exceptions.BadParameterException;
import org.opentox.jaqpot3.qsar.serializable.CustomAlgorithmModel;
import org.opentox.jaqpot3.resources.collections.Algorithms;
import org.opentox.jaqpot3.util.Configuration;
import org.opentox.toxotis.client.VRI;
import org.opentox.toxotis.client.collection.Services;
import org.opentox.toxotis.core.component.Algorithm;
import org.opentox.toxotis.core.component.Feature;
import org.opentox.toxotis.core.component.Model;
import org.opentox.toxotis.database.engine.task.UpdateTask;
import org.opentox.toxotis.database.exception.DbException;
import org.opentox.toxotis.exceptions.impl.ServiceInvocationException;
import org.opentox.toxotis.factory.FeatureFactory;
import org.opentox.toxotis.ontology.ResourceValue;
import org.opentox.toxotis.ontology.collection.OTClasses;
import weka.core.Attribute;
import weka.core.Instances;

/**
 *
 * @author lampovas
 */

//this is the trainer for the custom algorithm. Through this class its model is created.
//The model has the 3 descriptors of the form as independent features and 6 predicted features
//that are calculated based on them.

public class CustomAlgorithmFilter extends AbstractTrainer {
    private org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ScalingFilter.class);
    private VRI featureService;
    private VRI datasetUri;
    private VRI predictionfeatureUri;
    VRI descriptor1Uri = null;
    VRI descriptor2Uri = null;
    
    
    private Model processAbsoluteValuesFeatures(Instances dataInst) throws JaqpotException {
        VRI newModelUri = Configuration.getBaseUri().augment("model", getUuid());
        Model absoluteValueModel = new Model(newModelUri);
        CustomAlgorithmModel actualModel = new CustomAlgorithmModel();
        int nAttr = dataInst.numAttributes();
        
        String descriptor1UriStr =  descriptor1Uri.toString();
        String descriptor2UriStr =  descriptor2Uri.toString();
        
        //search all the descriptors and find those selected in the form.
        //upon selection add them as independent features in the model
        for (int i = 0; i < nAttr; i++) {
            Attribute attribute = dataInst.attribute(i);
            if (attribute.isNumeric()) {
                try {
                    VRI featureVri = new VRI(attribute.name());
                    String featureVriStr = featureVri.toString();
                    
                    
                    if (StringUtils.equals(featureVriStr, descriptor1UriStr) ||
                        StringUtils.equals(featureVriStr, descriptor2UriStr) ||
                        StringUtils.equals(featureVriStr, predictionfeatureUri.toString())    ) {
                        
                        Feature f = new Feature(featureVri);
                        absoluteValueModel.addIndependentFeatures(f);
                                                
                        getTask().getMeta().addComment("Independent feature " + f.getUri().toString());
                        //getTask().getMeta().setComments(new HashSet<LiteralValue>());
                        taskUpdater();
                        
                    }
                } catch (final URISyntaxException ex) {
                    String message = "URI syntax exception for numeric feature : '" + attribute.name() + "'. Invalid URI provided.";
                    logger.error(message, ex);
                    throw new JaqpotException(message, ex);
                }
            }
        }

        if (absoluteValueModel.getIndependentFeatures().size() ==3) {
            try {
                //Here the predicted features are created. 
                //The URIs of them are stored in the model in order for them to be found by the predictor,
                //when the predictor traverses the dataset
                
                String title = "Difference ("+descriptor2UriStr+" - "+descriptor1UriStr+")";
                Feature feature_diff = FeatureFactory.createAndPublishFeature(title,"",
                                new ResourceValue(newModelUri, OTClasses.model()), featureService, token);                    
                absoluteValueModel.addPredictedFeatures(feature_diff);   
                Map tempDiffVRI = new HashMap();
                tempDiffVRI.put("VRI", feature_diff.getUri());
                tempDiffVRI.put("minuend", descriptor2UriStr);
                tempDiffVRI.put("subtrahend", descriptor1UriStr);
                actualModel.setDiffVRI(tempDiffVRI);
                
                title = "Division ("+descriptor2UriStr+" / "+descriptor1UriStr+")";
                Feature feature_division = FeatureFactory.createAndPublishFeature(title,"",
                                new ResourceValue(newModelUri, OTClasses.model()), featureService, token);                    
                absoluteValueModel.addPredictedFeatures(feature_division); 
                Map tempDivisionVRI = new HashMap();
                tempDivisionVRI.put("VRI", feature_division.getUri());
                tempDivisionVRI.put("dividend", descriptor2UriStr);
                tempDivisionVRI.put("divisor", descriptor1UriStr);
                actualModel.setDivisionVRI(tempDivisionVRI);
                
                
                title = "Sign ("+descriptor1UriStr+")";
                Feature feature1_sign = FeatureFactory.createAndPublishFeature(title,"",
                                new ResourceValue(newModelUri, OTClasses.model()), featureService, token);                    
                absoluteValueModel.addPredictedFeatures(feature1_sign);   
                actualModel.setSign1VRI(feature1_sign.getUri());
                
                title = "Sign ("+descriptor2UriStr+")";
                Feature feature2_sign = FeatureFactory.createAndPublishFeature(title,"",
                                new ResourceValue(newModelUri, OTClasses.model()), featureService, token);                    
                absoluteValueModel.addPredictedFeatures(feature2_sign);   
                actualModel.setSign2VRI(feature2_sign.getUri());            
                
                title = "Magnitude ("+descriptor1UriStr+")";
                Feature feature1_magnitude = FeatureFactory.createAndPublishFeature(title,"",
                                new ResourceValue(newModelUri, OTClasses.model()), featureService, token);                    
                absoluteValueModel.addPredictedFeatures(feature1_magnitude);   
                actualModel.setMagn1VRI(feature1_magnitude.getUri());
                
                title = "Magnitude ("+descriptor2UriStr+")";
                Feature feature2_magnitude = FeatureFactory.createAndPublishFeature(title,"",
                                new ResourceValue(newModelUri, OTClasses.model()), featureService, token);                    
                absoluteValueModel.addPredictedFeatures(feature2_magnitude); 
                actualModel.setMagn2VRI(feature2_magnitude.getUri());
                
                getTask().getMeta().addComment("Dependent features ");

                taskUpdater();
            } catch (ServiceInvocationException ex) {
                   logger.debug("", ex);
            }
        }
        
        try {
            try {
                actualModel.setModelDescr1VRI(descriptor1Uri);
                actualModel.setModelDescr2VRI(descriptor2Uri);
                absoluteValueModel.setActualModel(actualModel);
            } catch (NotSerializableException ex) {
                Logger.getLogger(ScalingFilter.class.getName()).log(Level.SEVERE, null, ex);
            }
        } catch (RuntimeException ex) {
            logger.error("", ex);
        }

        absoluteValueModel.setAlgorithm(getAlgorithm());
        absoluteValueModel.setDataset(datasetUri);
        return absoluteValueModel;
    }
    
    @Override
    public Model train(Instances data) throws JaqpotException {
       return processAbsoluteValuesFeatures(data);
    }

    @Override
    public IParametrizableAlgorithm parametrize(IClientInput clientParameters) throws BadParameterException {
        String descriptor1String = clientParameters.getFirstValue("descriptor1");
        
        if (descriptor1String == null) {
            throw new BadParameterException("The parameter 'descriptor1' is mandatory for this algorithm.");
        }
        try {
            descriptor1Uri = new VRI(descriptor1String);
        } catch (URISyntaxException ex) {
            throw new BadParameterException("The parameter 'descriptor1' you provided is not a valid URI.", ex);
        }
        
        String descriptor2String = clientParameters.getFirstValue("descriptor2");
        
        if (descriptor2String == null) {
            throw new BadParameterException("The parameter 'descriptor2' is mandatory for this algorithm.");
        }
        try {
            descriptor2Uri = new VRI(descriptor2String);
        } catch (URISyntaxException ex) {
            throw new BadParameterException("The parameter 'descriptor2' you provided is not a valid URI.", ex);
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

        String predictionFeatureUriString = clientParameters.getFirstValue("prediction_feature");
        if (predictionFeatureUriString == null) {
            throw new BadParameterException("The parameter 'prediction_feature' is mandatory for this algorithm.");
        }
        try {
            predictionfeatureUri = new VRI(predictionFeatureUriString);
        } catch (URISyntaxException ex) {
            throw new BadParameterException("The parameter 'prediction_feature' you provided is not a valid URI.", ex);
        }
        
        return this;
    }

    @Override
    public Algorithm getAlgorithm() {
        return Algorithms.customAlgorithm();
    }
    
    private void taskUpdater() throws JaqpotException {
        UpdateTask taskUpdater = new UpdateTask(getTask());
        //taskUpdater.setUpdatePercentageCompleted(true);
        taskUpdater.setUpdateMeta(true);

        try {
            taskUpdater.update();
        } catch (DbException ex) {
            throw new JaqpotException(ex);
        } finally {
            try {
                taskUpdater.close();
            } catch (DbException ex) {
                throw new JaqpotException(ex);
            }
        }
    }
}

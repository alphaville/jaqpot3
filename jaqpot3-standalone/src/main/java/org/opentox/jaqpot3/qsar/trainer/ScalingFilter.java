package org.opentox.jaqpot3.qsar.trainer;

import java.io.NotSerializableException;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang.StringUtils;
import org.opentox.jaqpot3.exception.JaqpotException;
import org.opentox.jaqpot3.qsar.AbstractTrainer;
import org.opentox.jaqpot3.qsar.IClientInput;
import org.opentox.jaqpot3.qsar.IParametrizableAlgorithm;
import org.opentox.jaqpot3.qsar.exceptions.BadParameterException;
import org.opentox.jaqpot3.qsar.serializable.ScalingModel;
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
import org.opentox.toxotis.ontology.ResourceValue;
import org.opentox.toxotis.ontology.collection.OTClasses;
import weka.core.Attribute;
import weka.core.Instances;

/**
 * IMPORTANT NOTE: Scaling Filter outputs a Model (NOT a Dataset)
 * @author Pantelis Sopasakis
 * @author Charalampos Chomenides
 */
public class ScalingFilter extends AbstractTrainer {

    double min = 0;
    double max = 1;
    private org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ScalingFilter.class);
    private VRI featureService;
    private VRI datasetUri;
    Set<String> ignored = new HashSet<String>();

    
    @Override protected boolean keepNumeric() { return true; }
    @Override protected boolean keepNominal() { return false; }
    @Override protected boolean keepString()  { return false; }
    @Override protected boolean pmmlSupported()  { return true; }
    @Override protected boolean scalingSupported()  { return false; }
    @Override protected boolean normalizationSupported()  { return false; }
    @Override protected boolean DoASupported()  { return false; }
    @Override protected boolean performMVH()  { return false; }
    
    private double minValue(Instances dataInst, int attributeIndex) {
        return dataInst.kthSmallestValue(attributeIndex, 1);
    }

    private double maxValue(Instances dataInst, int attributeIndex) {
        double maxVal = Double.MIN_VALUE;
        double currentValue = maxVal;
        int nInst = dataInst.numInstances();
        for (int i = 0; i < nInst; i++) {
            currentValue = dataInst.instance(i).value(attributeIndex);
            if (currentValue > maxVal) {
                maxVal = currentValue;
            }
        }
        return maxVal;
    }

    @Override
    public Model train(Instances dataInst) throws JaqpotException {
        VRI newModelUri = Configuration.getBaseUri().augment("model", getUuid());
        Model scalingModel = new Model(newModelUri);
        scalingModel.setIndependentFeatures(independentFeatures);
        ScalingModel actualModel = new ScalingModel();
        
        Attribute attr;
        for(String attrName : excludeAttributesDoA) {
            attr = dataInst.attribute(attrName);
            if(attr!=null) {
                dataInst.deleteAttributeAt(attr.index());
            }
        }
        int nAttr = dataInst.numAttributes();
        for (int i = 0; i < nAttr; i++) {
            Attribute attribute = dataInst.attribute(i);
            
            Feature selected = null;
            for(Feature temp : independentFeatures) {
                if(StringUtils.equals(temp.getUri().getUri(),attribute.name())) {
                    selected = temp;
                    break;
                }
            }
            
            if (selected!=null && !ignored.contains(attribute.name())) {
                //TODO: Create in-house private methods to find min and max values
                actualModel.getMinVals().put(selected.getUri(), minValue(dataInst, i));
                actualModel.getMaxVals().put(selected.getUri(), maxValue(dataInst, i));                 
                Feature f = publishFeature(scalingModel,dependentFeature.getUnits(),"Scaled " + i,datasetUri,featureService);
                scalingModel.addPredictedFeatures(f);
                //getTask().getMeta().setComments(new HashSet<LiteralValue>());
                getTask().getMeta().addComment("Scaled feature for " + selected.getUri().toString()
                        + " has been created at " + f.getUri().toString());

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

        try {
            try {
                scalingModel.setActualModel(actualModel);
            } catch (NotSerializableException ex) {
                Logger.getLogger(ScalingFilter.class.getName()).log(Level.SEVERE, null, ex);
            }
        } catch (RuntimeException ex) {
            logger.error("", ex);
        }

        scalingModel.setAlgorithm(getAlgorithm());
        scalingModel.setDataset(datasetUri);
        return scalingModel;
    }

    @Override
    public IParametrizableAlgorithm doParametrize(IClientInput clientParameters) throws BadParameterException {

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

        String[] ignoredUris = clientParameters.getValuesArray("ignore_uri");

        for (String s : ignoredUris) {
            ignored.add(s);
        }
        return this;
    }

    @Override
    public Algorithm getAlgorithm() {
        return Algorithms.scaling();
    }


    
}

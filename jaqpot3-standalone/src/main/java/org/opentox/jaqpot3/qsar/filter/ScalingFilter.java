package org.opentox.jaqpot3.qsar.filter;

import java.io.NotSerializableException;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.opentox.jaqpot3.exception.JaqpotException;
import org.opentox.jaqpot3.qsar.AbstractTrainer;
import org.opentox.jaqpot3.qsar.IClientInput;
import org.opentox.jaqpot3.qsar.IParametrizableAlgorithm;
import org.opentox.jaqpot3.qsar.exceptions.BadParameterException;
import org.opentox.jaqpot3.resources.collections.Algorithms;
import org.opentox.jaqpot3.util.Configuration;
import org.opentox.toxotis.client.VRI;
import org.opentox.toxotis.client.collection.Services;
import org.opentox.toxotis.core.component.Algorithm;
import org.opentox.toxotis.core.component.Dataset;
import org.opentox.toxotis.core.component.Feature;
import org.opentox.toxotis.core.component.Model;
import org.opentox.toxotis.database.engine.task.UpdateTask;
import org.opentox.toxotis.database.exception.DbException;
import org.opentox.toxotis.exceptions.impl.ServiceInvocationException;
import org.opentox.toxotis.exceptions.impl.ToxOtisException;
import org.opentox.toxotis.factory.DatasetFactory;
import org.opentox.toxotis.factory.FeatureFactory;
import org.opentox.toxotis.ontology.LiteralValue;
import org.opentox.toxotis.ontology.ResourceValue;
import org.opentox.toxotis.ontology.collection.OTClasses;
import org.opentox.toxotis.util.arff.ArffDownloader;
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

    private Model processAbsoluteScaling(Instances dataInst) throws JaqpotException {
        VRI newModelUri = Configuration.getBaseUri().augment("model", getUuid());
        Model scalingModel = new Model(newModelUri);
        ScalingModel actualModel = new ScalingModel();
        int nAttr = dataInst.numAttributes();
        for (int i = 0; i < nAttr; i++) {
            Attribute attribute = dataInst.attribute(i);
            if (attribute.isNumeric() && !ignored.contains(attribute.name())) {
                try {
                    VRI featureVri = new VRI(attribute.name());
                    scalingModel.addIndependentFeatures(new Feature(featureVri));
                    //TODO: Create in-house private methods to find min and max values
                    actualModel.getMinVals().put(featureVri, minValue(dataInst, i));
                    actualModel.getMaxVals().put(featureVri, maxValue(dataInst, i));
                    Feature f = FeatureFactory.createAndPublishFeature("Scaled Feature " + featureVri.toString() + " within [" + min + ", " + max + "]", "",
                            new ResourceValue(newModelUri, OTClasses.Model()), featureService, token);
                    f.getMeta().setHasSources(new HashSet<ResourceValue>());// << workaround!
                    scalingModel.addPredictedFeatures(f);
                    //getTask().getMeta().setComments(new HashSet<LiteralValue>());
                    getTask().getMeta().addComment("Scaled feature for " + featureVri.toString()
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
                } catch (final URISyntaxException ex) {
                    String message = "URI syntax exception for numeric feature : '" + attribute.name() + "'. Invalid URI provided.";
                    logger.error(message, ex);
                    throw new JaqpotException(message, ex);
                } catch (ServiceInvocationException ex) {
                    logger.debug("", ex);
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
    public Model train(Instances data) throws JaqpotException {
        return processAbsoluteScaling(data);
    }

    @Override
    public IParametrizableAlgorithm parametrize(IClientInput clientParameters) throws BadParameterException {
        String minString = clientParameters.getFirstValue("min");
        if (minString != null) {
            try {
                min = Double.parseDouble(minString);
            } catch (NumberFormatException nfe) {
                throw new BadParameterException("Invalid value for the parameter 'scaling_min' (" + minString + ")", nfe);
            }
        }
        String maxString = clientParameters.getFirstValue("max");
        if (maxString != null) {
            try {
                max = Double.parseDouble(maxString);
            } catch (NumberFormatException nfe) {
                throw new BadParameterException("Invalid value for the parameter 'scaling_max' (" + maxString + ")", nfe);
            }
        }
        if (max <= min) {
            throw new BadParameterException("Assertion Exception: max >= min. The values for the parameters min and max that "
                    + "you spcified are inconsistent. min=" + min + " while max=" + max + ". It should be min &lt; max.");
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

    @Override
    public Dataset preprocessDataset(Dataset dataset) {
        return dataset;
    }

    
}

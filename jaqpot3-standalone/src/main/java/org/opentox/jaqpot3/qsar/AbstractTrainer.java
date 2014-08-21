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
package org.opentox.jaqpot3.qsar;

import Jama.Matrix;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.commons.lang.StringUtils;
import org.dmg.pmml.DataDictionary;
import org.dmg.pmml.DataField;
import org.dmg.pmml.PMML;
import org.opentox.jaqpot3.exception.JaqpotException;
import org.opentox.jaqpot3.qsar.exceptions.BadParameterException;
import org.opentox.jaqpot3.qsar.exceptions.QSARException;
import org.opentox.jaqpot3.qsar.util.AttributeCleanup;
import org.opentox.jaqpot3.qsar.util.PMMLProcess;
import org.opentox.toxotis.client.VRI;
import org.opentox.toxotis.core.component.Dataset;
import org.opentox.toxotis.core.component.Feature;
import org.opentox.toxotis.core.component.Model;
import org.opentox.toxotis.core.component.Task;
import org.opentox.toxotis.exceptions.impl.ServiceInvocationException;
import org.opentox.toxotis.exceptions.impl.ToxOtisException;
import org.opentox.toxotis.util.aa.AuthenticationToken;
import org.opentox.toxotis.util.arff.ArffDownloader;
import weka.core.Instances;
import org.opentox.jaqpot3.qsar.util.WekaInstancesProcess;
import org.opentox.toxotis.core.component.SubstanceDataset;
import org.opentox.toxotis.factory.FeatureFactory;
import org.opentox.toxotis.factory.PropertyFactory;
import org.opentox.toxotis.ontology.ResourceValue;
import org.opentox.toxotis.ontology.collection.OTClasses;
/**
 *
 * @author Pantelis Sopasakis
 * @author Charalampos Chomenides
 */
public abstract class AbstractTrainer implements ITrainer {

    private IClientInput ClientParams;
    private Task task;
    protected AuthenticationToken token;
    private UUID uuid = UUID.randomUUID();
    protected List<Feature> independentFeatures = new ArrayList<Feature>();
    protected Feature dependentFeature;
    
    private Boolean hasScaling = false;
    private Boolean hasNormalization = false;
    private Boolean hasMVH = false;
    
    protected byte[] pmml =null;
    protected PMML pmmlObject;
    private double scalingMin = 0;
    private double scalingMax = 1;
    HashMap<VRI, Double> scalingMinVals = null;
    HashMap<VRI, Double> scalingMaxVals = null;
    HashMap<VRI, Double> normalizationMinVals = null;
    HashMap<VRI, Double> normedVals = null;
    
    protected Instances nonProcessedInstances; //used in publishing property 
    //predictedInstances is used in DoA must be set in the end of training of each algorithm
    protected Instances predictedInstances = null; 

    protected abstract boolean keepNumeric();
    protected abstract boolean keepNominal();
    protected abstract boolean keepString();
    protected abstract boolean pmmlSupported();
    protected abstract boolean scalingSupported();
    protected abstract boolean normalizationSupported();
    protected abstract boolean DoASupported();
    protected abstract boolean performMVH();
    
    
    private Instances doPreprocessing(Instances inst) throws JaqpotException {
        
        nonProcessedInstances = inst;
        //TODO: PREPROC set all above flags for each algorithm properly
        if (!keepNominal()) {
            AttributeCleanup cleanup = new AttributeCleanup(false, AttributeCleanup.AttributeType.nominal);
            try {
                inst = cleanup.filter(inst);
            }catch(QSARException ex) {
                String message = "Exception while trying to cleanup nominals in instances";
                throw new JaqpotException(message, ex);
            }
            
        }
        
        if (!keepString()) {
            AttributeCleanup cleanup = new AttributeCleanup(false, AttributeCleanup.AttributeType.string);
            try {
                inst = cleanup.filter(inst);
            }catch(QSARException ex) {
                String message = "Exception while trying to cleanup strings in instances";
                throw new JaqpotException(message, ex);
            }
        }
        
        if (!keepNumeric()) {
            AttributeCleanup cleanup = new AttributeCleanup(false, AttributeCleanup.AttributeType.numeric);
            try {
                inst = cleanup.filter(inst);
            }catch(QSARException ex) {
                String message = "Exception while trying to cleanup strings in instances";
                throw new JaqpotException(message, ex);
            }
        }
        
        if(pmml!=null && pmmlSupported()) {
            pmmlObject = PMMLProcess.loadPMMLObject(pmml);
        }
            
        setIndepNDependentFeatures(inst);
        
        if(pmml!=null && pmmlSupported()) {
            inst = WekaInstancesProcess.getFilteredInstances(inst, independentFeatures,dependentFeature);
        }
        
        if( hasMVH) {
            inst = WekaInstancesProcess.handleMissingValues(inst, ClientParams);
        }
           
        //TODO: PMML specs must have datadictionary
        if(pmml!=null && pmmlSupported()) {
            inst = WekaInstancesProcess.transformDataset(inst,pmmlObject);
        }
        
        if(hasScaling) {
            scalingMinVals = WekaInstancesProcess.setMinValuesToModel(inst, independentFeatures);
            scalingMaxVals = WekaInstancesProcess.setScalingMaxValuesToModel(inst, independentFeatures);
            Map<String, Double> tempScalingMinVals = WekaInstancesProcess.getVRIkeyMapToDoublekeyMap(scalingMinVals);
            Map<String, Double> tempScalingMaxVals = WekaInstancesProcess.getVRIkeyMapToDoublekeyMap(scalingMaxVals);
            inst = WekaInstancesProcess.scaleInstances(inst,independentFeatures,tempScalingMinVals,tempScalingMaxVals);
        }
        
        if(hasNormalization) {
            normalizationMinVals = WekaInstancesProcess.setMinValuesToModel(inst, independentFeatures);
            normedVals = WekaInstancesProcess.setNormalizedValuesToModel(inst, independentFeatures);
            Map<String, Double> tempNormalizationMinVals = WekaInstancesProcess.getVRIkeyMapToDoublekeyMap(normalizationMinVals);
            Map<String, Double> tempNormedVals = WekaInstancesProcess.getVRIkeyMapToDoublekeyMap(normedVals);
            inst = WekaInstancesProcess.normalizeInstances(inst,independentFeatures,tempNormalizationMinVals,tempNormedVals);
        }
        
        return preprocessDataset(inst);
    }
    
    private Model postProcessModel(Model model) throws JaqpotException {
        
        //TODO: PMML xml for scaling
        if(hasScaling) {
            model.getActualModel().setHasScaling(hasScaling);
            model.getActualModel().setScalingMax(scalingMax);
            model.getActualModel().setScalingMin(scalingMin);
            model.getActualModel().setScalingMinVals(scalingMinVals);
            model.getActualModel().setScalingMaxVals(scalingMaxVals);
        }
        
        //TODO: PMML xml for normalization
        if(hasNormalization) {
            model.getActualModel().setHasNormalization(hasNormalization);
            model.getActualModel().setNormalizationMinVals(normalizationMinVals);
            model.getActualModel().setNormedVals(normedVals);
        }
        
        //TODO: PMML xml for DoA
        if( predictedInstances!=null && DoASupported()) {
            model.getActualModel().setHasDoA(true);
            
            Matrix omega = WekaInstancesProcess.getLeverageDoAMatrix(predictedInstances);            
            model.getActualModel().setDataMatrix(omega);
            int k = omega.getRowDimension();
            int n = omega.getColumnDimension();
            model.getActualModel().setGamma(k, n);
        }
        
        if(pmml!=null && pmmlSupported()) {
            model.getActualModel().setPmml(pmml);
        }
        
        try {
            model.setActualModel(model.getActualModel());
        } catch (Exception ex) {
            String message = "Exception while trying to add pmml to ActualModel";
            throw new JaqpotException(message, ex);
        }
        return model;
    }
    
    @Override
    public Instances preprocessDataset(Instances inst) {
        return inst;
    }    
    
    private void setIndepNDependentFeatures(Instances inst) throws JaqpotException{
        String targetString = ClientParams.getFirstValue("prediction_feature");
        
        if (pmmlObject!=null) {
                List<String> featuresList = new ArrayList<String>();
                DataDictionary dtDir = pmmlObject.getDataDictionary();
                if (dtDir!=null) {
                    List<DataField> dtfVar = dtDir.getDataFields();
                    if (!dtfVar.isEmpty()) {
                        for(int j=0;j<dtfVar.size();++j) {
                           featuresList.add(dtfVar.get(j).getName().getValue());
                        }
                        
                        for (int i = 0; i < inst.numAttributes(); i++) {
                            if(featuresList.contains(inst.attribute(i).name())){
                                Feature f;
                                try {
                                    f = new Feature(new VRI(inst.attribute(i).name()));
                                    independentFeatures.add(f);
                                } catch (URISyntaxException ex) {
                                    throw new JaqpotException("The URI: " + inst.attribute(i).name() + " is not valid", ex);
                                }
                            }
                        }
                    }
                }
        } else {
            for (int i = 0; i < inst.numAttributes(); i++) {
                //if not the endpoint
                if(!StringUtils.equals(inst.attribute(i).name().trim(),targetString.trim())){
                    Feature f;
                    try {
                        f = new Feature(new VRI(inst.attribute(i).name()));
                        independentFeatures.add(f);
                    } catch (URISyntaxException ex) {
                        throw new JaqpotException("The URI: " + inst.attribute(i).name() + " is not valid", ex);
                    }
                }
            }
        }
        
        try {
            VRI  targetUri = new VRI(targetString);
            dependentFeature = new Feature(targetUri);
        } catch (URISyntaxException ex) {
            //already validated
        }
    }

    @Override
    public ITrainer setTask(Task task) {
        this.task = task;
        return this;
    }

    @Override
    public Task getTask() {
        return this.task;
    }

    @Override
    public ITrainer setToken(AuthenticationToken token) {
        this.token = token;
        return this;
    }

    protected UUID getUuid() {
        return this.uuid;
    }
    
    @Override
    public boolean needsDataset() {
        return true;
    }

    public abstract IParametrizableAlgorithm doParametrize (IClientInput clientParameters) throws BadParameterException;
    
    @Override
    public IParametrizableAlgorithm parametrize(IClientInput clientParameters) throws BadParameterException {
        ClientParams = clientParameters;
        pmml = clientParameters.getUploadBytes();
        preprocParametrize(clientParameters);
        return doParametrize(clientParameters);
    }

    @Override
    public Model train(Dataset data) throws JaqpotException {
        return train(data.getInstances());
    }

    @Override
    public Model train(VRI data) throws JaqpotException {
        if (!needsDataset()) {
            return train((Instances) null);
        }
        ArffDownloader downloader = new ArffDownloader(data);
        Instances inst = downloader.getInstances();
        Model resultModel;
        
        
        if (inst != null) {
            inst = doPreprocessing(inst);
            resultModel =  train(inst);
        } else {
            try {
                resultModel = train(new Dataset(data).loadFromRemote());
            } catch (ToxOtisException ex) {
                throw new JaqpotException(ex);
            } catch (ServiceInvocationException ex) {
                throw new JaqpotException(ex);
            }
        }
        resultModel = postProcessModel(resultModel);
        return resultModel;
    }
    
    /*
        Publishes features or properties
    */
    protected Feature publishFeature(Model m,String units,String title,VRI datasetUri,VRI featureService) throws JaqpotException {
        Feature predictedFeature = new Feature();
        try {
            if(datasetUri.getOpenToxType() == SubstanceDataset.class) {
                
                SubstanceDataset ds = new SubstanceDataset();
                
                String host = SubstanceDataset.getHostFromVRI(datasetUri.toString());
                units = (units==null) ? "" : units;
                //TODO: API EXT custom enanomapper
                //gets the csv data for publishing a property to enanomapper
                String csvData = WekaInstancesProcess.getCSVOutputForProperty(token,nonProcessedInstances,units,title,datasetUri,host);
                ds.setCsv(csvData);
                ds.setClearData(false);
                //this csv name must be final in order to have only one dataset for the published properties
                ds.setOwnerName("publishProperty.csv");
                ds.setUri(datasetUri);
                
                predictedFeature = PropertyFactory.createAndPublishProperty(title, units,ds,featureService, token);
                
            } else {
                predictedFeature = FeatureFactory.createAndPublishFeature(title, units,
                            new ResourceValue(m.getUri(), OTClasses.model()), featureService, token);
            }
        } catch (ServiceInvocationException ex) {
            String message = "QSAR Exception: cannot train MLR model";
            throw new JaqpotException(message, ex);
        }
        
            
        return predictedFeature;   
    }
    
    private void preprocParametrize(IClientInput clientParameters) throws BadParameterException {
        
        if(scalingSupported()) {
            String minString = clientParameters.getFirstValue("scalingMin");
            if (minString != null) {
                hasScaling = true;
                try {
                    scalingMin = Double.parseDouble(minString);
                } catch (NumberFormatException nfe) {
                    throw new BadParameterException("Invalid value for the parameter 'scaling_min' (" + minString + ")", nfe);
                }
            }
            String maxString = clientParameters.getFirstValue("scalingMax");
            if (maxString != null) {
                try {
                    scalingMax = Double.parseDouble(maxString);
                } catch (NumberFormatException nfe) {
                    throw new BadParameterException("Invalid value for the parameter 'scaling_max' (" + maxString + ")", nfe);
                }
            }
            if (scalingMax <= scalingMin) {
                throw new BadParameterException("Assertion Exception: max >= min. The values for the parameters min and max that "
                        + "you spcified are inconsistent. min=" + scalingMin + " while max=" + scalingMax + ". It should be min &lt; max.");
            }
        }
          
        if(normalizationSupported()) {
            String normalizeString = clientParameters.getFirstValue("normalize");
            if (normalizeString != null) {
                try {
                    int tempNorm = Integer.parseInt(normalizeString);
                    hasNormalization = (Integer.compare(tempNorm, 1)==0)? true : false;
                } catch (NumberFormatException nfe) {
                    throw new BadParameterException("Invalid value for the parameter 'normalize' (" + normalizeString + ")", nfe);
                }
            }
        }
        
        if(hasScaling && hasNormalization) {
            throw new BadParameterException("cannot both scale and normalize a dataset");
        }
        
        if(performMVH()) {
            String mvhString = clientParameters.getFirstValue("mvh");
            if (mvhString != null) {
                try {
                    int mvh = Integer.parseInt(mvhString);
                    hasMVH = (Integer.compare(mvh, 1)==0)? true : false;
                } catch (NumberFormatException nfe) {
                    throw new BadParameterException("Invalid value for the parameter 'mvh' (" + mvhString + ")", nfe);
                }
            }
        }
    }
}

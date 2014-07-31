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

import com.sun.org.apache.bcel.internal.util.ByteSequence;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import javax.xml.transform.sax.SAXSource;
import org.apache.commons.lang.StringUtils;
import org.dmg.pmml.DataDictionary;
import org.dmg.pmml.DataField;
import org.dmg.pmml.DerivedField;
import org.dmg.pmml.PMML;
import org.dmg.pmml.TransformationDictionary;
import org.jpmml.evaluator.FieldValue;
import org.jpmml.model.ImportFilter;
import org.jpmml.model.JAXBUtil;
import org.opentox.jaqpot3.exception.JaqpotException;
import org.opentox.jaqpot3.qsar.exceptions.BadParameterException;
import org.opentox.jaqpot3.qsar.exceptions.QSARException;
import org.opentox.jaqpot3.qsar.util.AttributeCleanup;
import org.opentox.jaqpot3.qsar.util.ExpressionUtilExtended;
import org.opentox.jaqpot3.qsar.util.LocalEvaluationContext;
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
import org.xml.sax.InputSource;
import weka.core.Instances;
import org.opentox.jaqpot3.qsar.util.WekaInstancesProcess;
import org.opentox.jaqpot3.util.Configuration;
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

    protected byte[] pmml =null;
    protected PMML pmmlObject;
    private IClientInput ClientParams;
    private Task task;
    protected Instances nonProcessedInstances;
    protected AuthenticationToken token;
    private UUID uuid = UUID.randomUUID();
    protected List<Feature> independentFeatures = new ArrayList<Feature>();
    protected Feature dependentFeature;

    protected abstract boolean keepNumeric();
    protected abstract boolean keepNominal();
    protected abstract boolean keepString();
    protected abstract boolean performMVH();
    
    
    private Instances doPreprocessing(Instances inst) throws JaqpotException {
        
        nonProcessedInstances=inst;
        String isMvh = ClientParams.getFirstValue("mvh");
        Boolean isMvhEnabled = (StringUtils.equals(isMvh,"1")) ? true : false;

        //todo cleanup
        //missing value
        if (!keepNominal()){
            
        }
        if (!keepString()){
            AttributeCleanup cleanup = new AttributeCleanup(false, AttributeCleanup.AttributeType.string);
            try {
                inst = cleanup.filter(inst);
            }catch(QSARException ex) {
                String message = "Exception while trying to cleanup strings in instances";
                throw new JaqpotException(message, ex);
            }
        }
        if (!keepNumeric()){
            
        }
        if(pmml!=null) {
            pmmlObject = PMMLProcess.loadPMMLObject(pmml);
        }
            
        setIndepNDependentFeatures(inst);
        
        if(pmml!=null) {
            inst = WekaInstancesProcess.getFilteredInstances(inst, independentFeatures,dependentFeature);
        }
        
        if( isMvhEnabled || performMVH() ) {
            inst = WekaInstancesProcess.handleMissingValues(inst, ClientParams);
        }
           
         
        if(pmml!=null) {
            inst = WekaInstancesProcess.transformDataset(inst,pmmlObject);
        }
        
        return preprocessDataset(inst);
    }
    
    private Model postProcessModel(Model model) throws JaqpotException {
        if(pmml!=null) {
            model.getActualModel().setPmml(pmml);
            try {
                model.setActualModel(model.getActualModel());
            } catch (Exception ex) {
                String message = "Exception while trying to add pmml to ActualModel";
                throw new JaqpotException(message, ex);
            }
            
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
                //TODO custom enanomapper
                //gets the csv data for publishing a property to enanomapper
                String csvData = WekaInstancesProcess.getCSVOutputForProperty(token,nonProcessedInstances,units,title,datasetUri,host);
                ds.setCsv(csvData);
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
}

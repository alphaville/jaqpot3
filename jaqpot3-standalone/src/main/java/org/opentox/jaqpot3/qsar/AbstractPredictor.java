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
import java.util.List;
import java.util.Map;
import org.dmg.pmml.PMML;
import org.opentox.jaqpot3.exception.JaqpotException;
import org.opentox.jaqpot3.qsar.exceptions.BadParameterException;
import org.opentox.jaqpot3.qsar.util.PMMLProcess;
import org.opentox.jaqpot3.qsar.util.WekaInstancesProcess;
import org.opentox.toxotis.client.VRI;
import org.opentox.toxotis.core.component.Dataset;
import org.opentox.toxotis.core.component.Feature;
import org.opentox.toxotis.core.component.Model;
import org.opentox.toxotis.core.component.SubstanceDataset;
import org.opentox.toxotis.core.component.Task;
import org.opentox.toxotis.exceptions.impl.ServiceInvocationException;
import org.opentox.toxotis.exceptions.impl.ToxOtisException;
import org.opentox.toxotis.factory.DatasetFactory;
import org.opentox.toxotis.util.aa.AuthenticationToken;
import org.opentox.toxotis.util.arff.ArffDownloader;
import weka.core.Attribute;
import weka.core.Instances;

/**
 *
 * @author Pantelis Sopasakis
 * @author Charalampos Chomenides
 */
public abstract class AbstractPredictor implements IPredictor {

    private IClientInput ClientParams;
    protected byte[] pmml;
    protected List<Integer> trFieldsAttrIndex;
    protected PMML pmmlObject;
    protected Instances justCompounds;
    private Task task;
    protected AuthenticationToken token;
    protected Model model;
    protected List<Feature> independentFeatures = new ArrayList<Feature>();
    protected Feature dependentFeature;
    private Instances predictedInstances;
    protected Instances processedInstances; //used in DoA 
    private Boolean hasMVH = false;

    public AbstractPredictor() {
        trFieldsAttrIndex = new ArrayList<Integer>();
    }
    
    @Override
    public IPredictor parametrize(IClientInput clientParameters) throws BadParameterException {
        ClientParams = clientParameters;
        String mvhString = clientParameters.getFirstValue("mvh");
        if (mvhString != null) {
            try {
                int mvh = Integer.parseInt(mvhString);
                hasMVH = (Integer.compare(mvh, 1)==0)? true : false;
            } catch (NumberFormatException nfe) {
                throw new BadParameterException("Invalid value for the parameter 'mvh' (" + mvhString + ")", nfe);
            }
        }
        return this;
    }
    
    public Instances preprocessDataset(Instances inst) throws JaqpotException {
        independentFeatures = model.getIndependentFeatures();
        dependentFeature = (model.getDependentFeatures().isEmpty()) ? null : model.getDependentFeatures().get(0);
        justCompounds = WekaInstancesProcess.loadJustCompounds(inst);
        
                
        if(model.getActualModel()!=null) {
            pmml = model.getActualModel().getPmml();
            if(pmml!=null) {
                
                pmmlObject = PMMLProcess.loadPMMLObject(pmml);
                //IMPORTANT!!!! WekaInstancesProcess.getFilteredInstances removes compound URI that is needed
                
                Map<String,Object> resMap = WekaInstancesProcess.transformDataset(inst,pmmlObject);
                inst = (Instances) resMap.get("instances");
                
                trFieldsAttrIndex = WekaInstancesProcess.getTransformationFieldsAttrIndex(inst, pmmlObject);
            }
            if(hasMVH) {
                inst = WekaInstancesProcess.handleMissingValues(inst, ClientParams);
            }
            if(model.getActualModel().hasScaling()) {
                inst = WekaInstancesProcess.scaleInstances(inst,independentFeatures,model.getActualModel().getScalingMinVals2(),model.getActualModel().getScalingMaxVals2());
            }
            if(model.getActualModel().hasNormalization()) {
                inst = WekaInstancesProcess.normalizeInstances(inst,independentFeatures,model.getActualModel().getNormalizationMinVals2(),model.getActualModel().getNormedVals2());
            }
            processedInstances = inst;
        } else {
            if(hasMVH) {
                inst = WekaInstancesProcess.handleMissingValues(inst, ClientParams);
            }
        }
        return inst;
    }    
    
    public Instances postprocessDataset(Instances inst,String datasetUri) throws JaqpotException {
        if(model.getActualModel().hasDoA()) {
            inst = WekaInstancesProcess.getLeverageDoAPredictedInstances(processedInstances,inst, datasetUri, model);
        }
        return inst;
    }
    
    @Override
    public IPredictor setModel(Model model) {
        this.model = model;
        return this;
    }

    @Override
    public IPredictor setTask(Task task) {
        this.task = task;
        return this;
    }

    @Override
    public Task getTask() {
        return this.task;
    }

    @Override
    public IPredictor setToken(AuthenticationToken token) {
        this.token = token;
        return this;
    }

    @Override
    public Model getModel() {
        return model;
    }

    
    private Instances predictInstances(VRI input) throws JaqpotException {
        Instances inst;
        
        ArffDownloader downloader = new ArffDownloader(input);
        inst = downloader.getInstances();// the dataset is available in text/x-arff directly
        
        if (inst == null) { 
             // The instances object has to be retrieved from the RDF format
            try {
                Dataset data = new Dataset(input).loadFromRemote();
                inst = data.getInstances();
            } catch (ToxOtisException ex) {
                throw new JaqpotException(ex);
            } catch (ServiceInvocationException ex) {
                throw new JaqpotException(ex);
            }
        }
        inst = preprocessDataset(inst);
        inst = predict(inst);
        inst = postprocessDataset(inst,input.getUri());
        return inst;
    }
    
    @Override
    public Dataset predict(VRI input) throws JaqpotException {
        Instances inst = predictedInstances = predictInstances(input);
        try {
            return DatasetFactory.getInstance().createFromArff(inst);
        } catch (ToxOtisException ex) {
            throw new JaqpotException(ex);
        }
    }
    
    /*
        Perform prediction for enanomapper datasets
    */
    @Override
    public SubstanceDataset predictEnm(VRI input) throws JaqpotException {
        
        Instances inst = predictedInstances = predictInstances(input);
        SubstanceDataset ds = new SubstanceDataset();
        
        //TODO: API EXT custom enanomapper
        String host = SubstanceDataset.getHostFromVRI(input.toString());
        //get the csv data and the owner for the dataset to be published
        String csvData = WekaInstancesProcess.getCSVOutput(model,token,inst,input,host);
        String ownerName = WekaInstancesProcess.getSubstanceKeyFromInstances(token,inst,"ownerName");
        
        ds.setCsv(csvData);
        ds.setOwnerName(ownerName);
        return ds;
    }

    public Instances getPredictedInstances() {
        return predictedInstances;
    }
    
    
}

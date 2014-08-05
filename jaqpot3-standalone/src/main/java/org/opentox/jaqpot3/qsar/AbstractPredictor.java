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

import java.util.ArrayList;
import java.util.List;
import org.dmg.pmml.PMML;
import org.opentox.jaqpot3.exception.JaqpotException;
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
import weka.core.Instances;

/**
 *
 * @author Pantelis Sopasakis
 * @author Charalampos Chomenides
 */
public abstract class AbstractPredictor implements IPredictor {

    protected byte[] pmml;
    protected List<Integer> trFieldsAttrIndex;
    protected PMML pmmlObject;
    protected Instances justCompounds;
    private Task task;
    protected AuthenticationToken token;
    protected Model model;
    protected List<Feature> independentFeatures = new ArrayList<Feature>();
    protected Feature dependentFeature;

    public AbstractPredictor() {
        trFieldsAttrIndex = new ArrayList<Integer>();
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
                
                //TODO check Spot for MVH
                //inst = WekaInstancesProcess.handleMissingValues(inst, ClientParams);
                inst = WekaInstancesProcess.transformDataset(inst,pmmlObject);
                trFieldsAttrIndex = WekaInstancesProcess.getTransformationFieldsAttrIndex(inst, pmmlObject);
            }
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
        return inst;
    }
    
    @Override
    public Dataset predict(VRI input) throws JaqpotException {
        Instances inst = predictInstances(input);
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
        
        Instances inst = predictInstances(input);
        SubstanceDataset ds = new SubstanceDataset();
        
        //TODO custom enanomapper
        String host = SubstanceDataset.getHostFromVRI(input.toString());
        //get the csv data and the owner for the dataset to be published
        String csvData = WekaInstancesProcess.getCSVOutput(model,token,inst,input,host);
        String ownerName = WekaInstancesProcess.getSubstanceKeyFromInstances(token,inst,"ownerName");
        
        ds.setCsv(csvData);
        ds.setOwnerName(ownerName);
        return ds;
    }
    
    
}

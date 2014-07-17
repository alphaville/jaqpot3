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
import java.io.File;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.xml.transform.sax.SAXSource;
import org.apache.commons.lang.ArrayUtils;
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
import org.opentox.jaqpot3.qsar.util.SimpleMVHFilter;
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
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.CSVSaver;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Add;
import weka.filters.unsupervised.attribute.Remove;
import org.opentox.jaqpot3.qsar.util.WekaInstancesProcess;
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
    protected AuthenticationToken token;
    private UUID uuid = UUID.randomUUID();
    protected List<Feature> independentFeatures = new ArrayList<Feature>();
    protected Feature dependentFeature;

    protected abstract boolean keepNumeric();
    protected abstract boolean keepNominal();
    protected abstract boolean keepString();
    
    
    private Instances doPreprocessing(Instances inst) throws JaqpotException {
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
            loadPMMObject();
        }
            
        setIndepNDependentFeatures(inst);
        
        if(pmml!=null) {
            inst = WekaInstancesProcess.getFilteredInstances(inst, independentFeatures, dependentFeature);
        }
        
        //TODO check Spot for MVH
            inst = WekaInstancesProcess.handleMissingValues(inst, ClientParams);
            
        if(pmml!=null) {
            inst = transformDataset(inst);
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
    
    private void loadPMMObject() throws JaqpotException{
        try {    
            InputStream is = new ByteSequence(pmml);
            InputSource source = new InputSource(is);

            SAXSource transformedSource = ImportFilter.apply(source);
            pmmlObject = JAXBUtil.unmarshalPMML(transformedSource);
        } catch (Exception ex) {
            String message = "Exception while loading PMML to object";
            throw new JaqpotException(message, ex);
        }
    }
    
    private void setIndepNDependentFeatures(Instances inst) throws JaqpotException{
        if (pmmlObject!=null) {
                List<String> indepFeaturesPMML = new ArrayList<String>();
                DataDictionary dtDir = pmmlObject.getDataDictionary();
                if (dtDir!=null) {
                    List<DataField> dtfVar = dtDir.getDataFields();
                    if (!dtfVar.isEmpty()) {
                        for(int j=0;j<dtfVar.size();++j) {
                           indepFeaturesPMML.add(dtfVar.get(j).getName().getValue());
                        }
                        
                        for (int i = 0; i < inst.numAttributes(); i++) {
                            if(indepFeaturesPMML.contains(inst.attribute(i).name())){
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
                Feature f;
                try {
                    f = new Feature(new VRI(inst.attribute(i).name()));
                    independentFeatures.add(f);
                } catch (URISyntaxException ex) {
                    throw new JaqpotException("The URI: " + inst.attribute(i).name() + " is not valid", ex);
                }
            }
        }
        
        String targetString = ClientParams.getFirstValue("prediction_feature");

        try {
            VRI  targetUri = new VRI(targetString);
            dependentFeature = new Feature(targetUri);
        } catch (URISyntaxException ex) {
            //already validated
        }
    }
    
    private Instances transformDataset(Instances inst) throws JaqpotException{
        //TODO add new features when uris missing
        try {     

            if (pmmlObject!=null) {
                //Get the Derived fields (math formulas) of the PMML file
                TransformationDictionary trDir = pmmlObject.getTransformationDictionary();
                if (trDir!=null) {
                    List<DerivedField> dfVar = trDir.getDerivedFields();

                    if (!dfVar.isEmpty()) {
                        int numAttributes = inst.numAttributes();
                        int numInstances = inst.numInstances();

                        int targetAttributeIndex = numAttributes;
                        Map<String,Double> featureMap; 
                        for(int i=0;i<dfVar.size();++i) {

                            String attrName = (StringUtils.isNotEmpty(dfVar.get(i).getName().getValue())) ? dfVar.get(i).getName().getValue() : "New Attribute"+i;
                            inst = WekaInstancesProcess.addNewAttribute(inst, attrName);

                            Double res;
                            for (int j = 0; j < numInstances; j++) {

                                featureMap = WekaInstancesProcess.getInstanceAttributeValues(inst.instance(j),numAttributes);

                                FieldValue val = ExpressionUtilExtended.evaluate(dfVar.get(i), new LocalEvaluationContext(),featureMap);

                                res = (Double) val.getValue();
                                res = (!Double.isNaN(res)) ? res : Double.MIN_VALUE;
                                inst.instance(j).setValue(targetAttributeIndex, res);
                            }
                            ++targetAttributeIndex;
                        }
                    }
                }
            }
        } catch (Exception ex) {
            String message = "Exception while trying to transform Instances";
            throw new JaqpotException(message, ex);
        }
                
        return inst;
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
    
}

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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
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
import org.jpmml.evaluator.LocalEvaluationContext;
import org.jpmml.evaluator.ExpressionUtilExtended;
import org.jpmml.model.ImportFilter;
import org.jpmml.model.JAXBUtil;
import org.opentox.jaqpot3.exception.JaqpotException;
import org.opentox.jaqpot3.qsar.exceptions.BadParameterException;
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

/**
 *
 * @author Pantelis Sopasakis
 * @author Charalampos Chomenides
 */
public abstract class AbstractTrainer implements ITrainer {

    protected byte[] pmml;
    private IClientInput ClientParams;
    private Task task;
    protected AuthenticationToken token;
    private UUID uuid = UUID.randomUUID();

    private Instances doPreprocessing(Instances inst) throws JaqpotException {
        if(pmml!=null) {
            // TODO : validate pmml
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
    
    private Map<String,Double> getInstanceAttributeValues(Instance inst,int numAttributes) {
        //numAttributes need to be set before adding the new attributes
        Map<String,Double> featureMap = new HashMap(); 
        if (numAttributes > 0) {
            double res;
            for (int i=0;i < numAttributes;++i) {
                res = (!Double.isNaN(inst.value(i))) ? inst.value(i) : 0;
                res = (!Double.isInfinite(res)) ? res : 0;
                featureMap.put(inst.attribute(i).name(), res);
            }
        }
        return featureMap;
    }
    
    private List<Integer> getDescriptorsIndexArray(Instances inputData,List<String> attrList) {
        List<Integer> tempArray = new ArrayList();
        int NAttr = inputData.numAttributes();
        
        if (!attrList.isEmpty()) {
            //get the index of the uri to be kept, this URI must be first and preceedes the independent features 
            for(int i=0;i<NAttr;++i) {
                if(StringUtils.equals( inputData.attribute(i).name().toString() , "URI" )) {
                    tempArray.add(i);
                    break;
                }
            }

            for(int j=0;j<attrList.size();++j) {
                for(int i=0;i<NAttr;++i) {
                    if(StringUtils.equals( inputData.attribute(i).name().toString() , attrList.get(j) )) {
                        tempArray.add(i);
                    }
                }
            }
        }
        
        return tempArray;
    }
    
    private Instances getFilteredInstances(List<Integer> indexArray, Instances inputData) throws JaqpotException {
        try {
            //apply filter for deleting the attributes other than these descriptors 
            int[] intArray = ArrayUtils.toPrimitive(indexArray.toArray(new Integer[indexArray.size()]));
            int m=1;
            Remove rm = new Remove();
            rm.setOptions(new String[]{"-V"});
            rm.setAttributeIndicesArray(intArray);
            rm.setInputFormat(inputData);
            Instances filteredData = Filter.useFilter(inputData, rm);
            
            return filteredData;

        } catch (Exception ex) {
            throw new JaqpotException(ex);
        }
    }
    
    private Instances addNewAttribute(Instances filteredData,String nextFeatureUri) throws JaqpotException{
        Add attributeAdder = new Add();
        attributeAdder.setAttributeIndex("last");
        attributeAdder.setAttributeName(nextFeatureUri);
        try {
            attributeAdder.setInputFormat(filteredData);
            filteredData = Filter.useFilter(filteredData, attributeAdder);
            
            if (filteredData == null) throw new JaqpotException("no instances data");
        } catch (Exception ex) {
            String message = "Exception while trying to add prediction feature to Instances";
                throw new JaqpotException(message, ex);
        }
        return filteredData;
    }
    
    private Instances transformDataset(Instances inst) throws JaqpotException{
        //Todo evaluate pmml
        //Convert the String to PMML object
        try {     

            InputStream is = new ByteSequence(pmml);
            InputSource source = new InputSource(is);

            SAXSource transformedSource = ImportFilter.apply(source);
            PMML pmml = JAXBUtil.unmarshalPMML(transformedSource);

            DataDictionary dtDir = pmml.getDataDictionary();
            if (dtDir!=null) {
                List<DataField> dtfVar = dtDir.getDataFields();
                if (!dtfVar.isEmpty()) {

                    //List<Integer> indexArray = getDescriptorsIndexArray(inst);
                    //inst = getFilteredInstances(indexArray,inst);
                }
            }
            
            //Get the Derived fields (math formulas) of the PMML file
            TransformationDictionary trDir = pmml.getTransformationDictionary();
            if (trDir!=null) {
                List<DerivedField> dfVar = trDir.getDerivedFields();

                if (!dfVar.isEmpty()) {
                    int numAttributes = inst.numAttributes();
                    int targetAttributeIndex = numAttributes;
                    Map<String,Double> featureMap = new HashMap(); 
                    for(int i=0;i<dfVar.size();++i) {

                        String attrName = (StringUtils.isNotEmpty(dfVar.get(i).getName().getValue())) ? dfVar.get(i).getName().getValue() : "New Attribute"+i;
                        inst = addNewAttribute(inst,attrName);

                        int numInstances = inst.numInstances();
                        Double res;
                        for (int j = 0; j < numInstances; j++) {

                            featureMap = getInstanceAttributeValues(inst.instance(j),numAttributes);

                            FieldValue val = ExpressionUtilExtended.evaluate(dfVar.get(i), new LocalEvaluationContext(),featureMap);

                            res = (Double) val.getValue();
                            res = (!Double.isNaN(res)) ? res : Double.MIN_VALUE;
                            inst.instance(j).setValue(targetAttributeIndex, res);
                        }
                        ++targetAttributeIndex;
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
            toCSV(inst,"C:\\Users\\philip\\Downloads\\TrainNoProcess.csv");
            inst = doPreprocessing(inst);
            toCSV(inst,"C:\\Users\\philip\\Downloads\\TrainPreProcess.csv");
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
    
    private void toCSV(Instances inst,String Filename){
        
        File file = new File(Filename);
        CSVSaver cs = new CSVSaver();
        
        try {
            cs.setRetrieval(1);
            cs.setFile(file);
            cs.setInstances(inst);
            cs.writeBatch();
        } catch (Exception ex) {}
    }
            
}

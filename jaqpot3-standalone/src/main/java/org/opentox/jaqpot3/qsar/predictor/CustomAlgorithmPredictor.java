/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.opentox.jaqpot3.qsar.predictor;

import static java.lang.Math.abs;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.opentox.jaqpot3.exception.JaqpotException;
import org.opentox.jaqpot3.qsar.AbstractPredictor;
import org.opentox.jaqpot3.qsar.IClientInput;
import org.opentox.jaqpot3.qsar.IPredictor;
import org.opentox.jaqpot3.qsar.exceptions.BadParameterException;
import org.opentox.jaqpot3.qsar.serializable.CustomAlgorithmModel;
import org.opentox.toxotis.core.component.Dataset;
import org.opentox.toxotis.core.component.Feature;
import org.opentox.toxotis.exceptions.impl.ToxOtisException;
import org.opentox.toxotis.factory.DatasetFactory;
import weka.core.Instances;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Add;
import weka.filters.unsupervised.attribute.Remove;

/**
 *
 * @author lampovas
 */
public class CustomAlgorithmPredictor extends AbstractPredictor {
    private int descr1Index = -1;
    private int descr2Index = -1;    
    private int minuendIndex = -1;
    private int subtrahendIndex = -1;
    private int dividendIndex = -1;
    private int divisorIndex = -1;
    
    @Override
    public IPredictor parametrize(IClientInput clientParameters) throws BadParameterException {
        return this;
    }
        
    @Override
    public Instances predict(Instances inputData) throws JaqpotException {
        try {
        CustomAlgorithmModel actualModel = (CustomAlgorithmModel) model.getActualModel();

        //delete data
        Feature nextFeature;
        Iterator<Feature> features;
        
        //from the given dataset we need only the 3 independent features of the model
        //thus we get their indexes from 'getDescriptorsIndexArray'
        //we filter the dataset so that only the 3 of them remain 'getFilteredInstances'
        //and we get again the new indexes of the 3 independent features 'getDescriptorsNewIndexes'
        
        List<Integer> indexArray = getDescriptorsIndexArray(inputData);

        Instances filteredData = getFilteredInstances(indexArray,inputData);

        getDescriptorsNewIndexes(filteredData, actualModel);
        
        if (descr1Index>0 && descr2Index>0) {
            
            //based on the model we get the indexes of the features of the divident,minuendIndex ...
            assignCalculationIndexes(actualModel);

            features = model.getPredictedFeatures().iterator();
            while(features.hasNext()) {
                nextFeature = features.next();
                String nextFeatureUri = nextFeature.getUri().toString();
            
                //we add the new predicted feature
                filteredData = addNewAttribute(filteredData,nextFeatureUri);
                        
                int num = filteredData.instance(0).numValues()-1;

                double val1,val2,res;
                
                int numInstances = filteredData.numInstances();
                
                //the type of calculation is selected based on the feature and upon selection each calculation
                //result is stored in its instance 
                for (int i = 0; i < numInstances; i++) {
                    val1 = filteredData.instance(i).value(descr1Index);
                    val2 = filteredData.instance(i).value(descr2Index);
                    res = Double.MIN_VALUE;
                    Boolean isCalculated = true;

                    if (StringUtils.equals(nextFeatureUri,actualModel.getDiffVRI().get("VRI").toString()) &&
                        minuendIndex>0 && subtrahendIndex>0) {
                            res = filteredData.instance(i).value(minuendIndex) - filteredData.instance(i).value(subtrahendIndex);

                    } else if (StringUtils.equals(nextFeatureUri,actualModel.getDivisionVRI().get("VRI").toString()) &&
                               dividendIndex>0 && divisorIndex>0) {
                            res = ((double) filteredData.instance(i).value(dividendIndex)) / ((double)  filteredData.instance(i).value(divisorIndex));

                    } else if (StringUtils.equals(nextFeatureUri,actualModel.getSign1VRI().toString())) {
                            res = (val1 >0) ? 1 : (val1 <0)? -1 :0;

                    } else if (StringUtils.equals(nextFeatureUri,actualModel.getSign2VRI().toString())) {
                            res = (val2 >0) ? 1 : (val2 <0)? -1 :0;

                    } else if (StringUtils.equals(nextFeatureUri,actualModel.getMagn1VRI().toString())) {
                            res = abs(val1);

                    } else if (StringUtils.equals(nextFeatureUri,actualModel.getMagn2VRI().toString())) {
                            res = abs(val2);

                    } else {
                            isCalculated = false;
                    }

                    if (isCalculated)  filteredData.instance(i).setValue(num, res);
                }
            }
            
            return filteredData;
        } else {
            throw new JaqpotException("Independent features not found in Instances");
        }

        } catch (Throwable thr){
            thr.printStackTrace();

        }
        return null;
    }
    
    private List<Integer> getDescriptorsIndexArray(Instances inputData) {
        Feature nextFeature;
        List<Integer> tempArray = new ArrayList();
        int NAttr = inputData.numAttributes();
        
        //get the index of the uri to be kept, this URI must be first and preceedes the independent features 
        for(int i=0;i<NAttr;++i) {
            if(StringUtils.equals( inputData.attribute(i).name().toString() , "URI" )) {
                tempArray.add(i);
                break;
            }
        }
        Iterator<Feature> features = model.getIndependentFeatures().iterator();
        //get the index of the descriptors to be kept
        while (features.hasNext()) {
            nextFeature = features.next();
            for(int i=0;i<NAttr;++i) {
                if(StringUtils.equals( inputData.attribute(i).name().toString() , nextFeature.getUri().toString() )) {
                    tempArray.add(i);
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
    
    private void getDescriptorsNewIndexes(Instances filteredData,CustomAlgorithmModel actualModel) {
        int NAttr = filteredData.numAttributes();
         //get the index of the descriptors again because of indexing changes
        Iterator<Feature> features = model.getIndependentFeatures().iterator();
        Feature nextFeature;
        while (features.hasNext()) {
            nextFeature = features.next();
            for(int i=0;i<NAttr;++i) {
                if(StringUtils.equals( filteredData.attribute(i).name().toString() , nextFeature.getUri().toString() )) {
                    if(StringUtils.equals( actualModel.getModelDescr1VRI().toString() , nextFeature.getUri().toString() )) {
                        descr1Index =i;
                    } else if (StringUtils.equals( actualModel.getModelDescr2VRI().toString() , nextFeature.getUri().toString() )) {
                        descr2Index =i;
                    }
                }
            }
        }
    }
   
    private void assignCalculationIndexes(CustomAlgorithmModel actualModel) {
                   
        // find the indexes for the minuend and subtrahend for the 2 descriptors
        if (StringUtils.equals(actualModel.getModelDescr1VRI().toString(),
                               actualModel.getDiffVRI().get("minuend").toString()) && 
            StringUtils.equals(actualModel.getModelDescr2VRI().toString(),
                               actualModel.getDiffVRI().get("subtrahend").toString())) {
            minuendIndex = descr1Index;
            subtrahendIndex = descr2Index;
        } else if (StringUtils.equals(actualModel.getModelDescr1VRI().toString(),
                               actualModel.getDiffVRI().get("subtrahend").toString()) && 
            StringUtils.equals(actualModel.getModelDescr2VRI().toString(),
                               actualModel.getDiffVRI().get("minuend").toString())) {
            minuendIndex = descr2Index;
            subtrahendIndex = descr1Index;
        }

        // find the indexes for the dividend and divisor for the 2 descriptors
        if (StringUtils.equals(actualModel.getModelDescr1VRI().toString(),
                               actualModel.getDivisionVRI().get("dividend").toString()) && 
            StringUtils.equals(actualModel.getModelDescr2VRI().toString(),
                               actualModel.getDivisionVRI().get("divisor").toString())) {
            dividendIndex = descr1Index;
            divisorIndex = descr2Index;
        } else if (StringUtils.equals(actualModel.getModelDescr1VRI().toString(),
                               actualModel.getDivisionVRI().get("divisor").toString()) && 
            StringUtils.equals(actualModel.getModelDescr2VRI().toString(),
                               actualModel.getDivisionVRI().get("dividend").toString())) {
            dividendIndex = descr2Index;
            divisorIndex = descr1Index;
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
}

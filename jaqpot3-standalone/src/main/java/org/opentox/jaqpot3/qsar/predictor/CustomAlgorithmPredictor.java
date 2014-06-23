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
 * @author philip
 */
public class CustomAlgorithmPredictor extends AbstractPredictor {
    
    private Map<String,String> featureToAbsValue = new HashMap();
    
    @Override
    public IPredictor parametrize(IClientInput clientParameters) throws BadParameterException {
        return this;
    }
        
    @Override
    public Dataset predict(Instances inputData) throws JaqpotException {
        try {
        CustomAlgorithmModel actualModel = (CustomAlgorithmModel) model.getActualModel();

        //delete data
        Feature nextFeature = null;

        List<Integer> indexArray = new ArrayList();
        int NAttr = inputData.numAttributes();



        Iterator<Feature> features = model.getIndependentFeatures().iterator();
        //get the index of the uri to be kept
        for(int i=0;i<NAttr;++i) {
            if(StringUtils.equals( inputData.attribute(i).name().toString() , "URI" )) {
                indexArray.add(i);
                break;
            }
        }
        //get the index of the descriptors to be kept
        while (features.hasNext()) {
            nextFeature = features.next();
            for(int i=0;i<NAttr;++i) {
                if(StringUtils.equals( inputData.attribute(i).name().toString() , nextFeature.getUri().toString() )) {
                    indexArray.add(i);
                }
            }
        }

        //apply filter for deleting the attributes other than these descriptors 
        int[] intArray = ArrayUtils.toPrimitive(indexArray.toArray(new Integer[indexArray.size()]));
        int m=1;
        Remove rm = new Remove();
        rm.setOptions(new String[]{"-V"});
        rm.setAttributeIndicesArray(intArray);
        rm.setInputFormat(inputData);
        Instances deletedData = Filter.useFilter(inputData, rm);

        int descr1Index = -1;
        int descr2Index = -1;

        NAttr = deletedData.numAttributes();
         //get the index of the descriptors again because of indexing changes
        features = model.getIndependentFeatures().iterator();
        while (features.hasNext()) {
            nextFeature = features.next();
            for(int i=0;i<NAttr;++i) {
                if(StringUtils.equals( deletedData.attribute(i).name().toString() , nextFeature.getUri().toString() )) {
                    if(StringUtils.equals( actualModel.getModelDescr1VRI().toString() , nextFeature.getUri().toString() )) {
                        descr1Index =i;
                    } else if (StringUtils.equals( actualModel.getModelDescr2VRI().toString() , nextFeature.getUri().toString() )) {
                        descr2Index =i;
                    }
                }
            }
        }

        if (descr1Index>0 && descr2Index>0) {

            int minuendIndex = -1;
            int subtrahendIndex = -1;
            int dividendIndex = -1;
            int divisorIndex = -1;
            
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

            Add attributeAdder;
            features = model.getPredictedFeatures().iterator();
            while(features.hasNext()) {
                nextFeature = features.next();
                String nextFeatureUri = nextFeature.getUri().toString();
                attributeAdder = new Add();
                attributeAdder.setAttributeIndex("last");
                attributeAdder.setAttributeName(nextFeatureUri);
                try {
                    attributeAdder.setInputFormat(deletedData);
                    deletedData = Filter.useFilter(deletedData, attributeAdder);
                } catch (Exception ex) {
                    String message = "Exception while trying to add prediction feature to Instances";
                        throw new JaqpotException(message, ex);
                }
                int num = deletedData.instance(0).numValues()-1;

                if (deletedData != null) {            

                    double val1,val2,res;
                    String resSign;
                    int numInstances = deletedData.numInstances();
                    for (int i = 0; i < numInstances; i++) {
                            val1 = deletedData.instance(i).value(descr1Index);
                            val2 = deletedData.instance(i).value(descr2Index);
                            res = Double.MIN_VALUE;
                            if (StringUtils.equals(nextFeatureUri,actualModel.getDiffVRI().get("VRI").toString())) {
                                if(minuendIndex>0 && subtrahendIndex>0) {
                                    res = deletedData.instance(i).value(minuendIndex) - deletedData.instance(i).value(subtrahendIndex);
                                    deletedData.instance(i).setValue(num, res);
                                }
                            } else if (StringUtils.equals(nextFeatureUri,actualModel.getDivisionVRI().get("VRI").toString())) {
                                if(dividendIndex>0 && divisorIndex>0) {
                                    res = ((double) deletedData.instance(i).value(dividendIndex)) / ((double)  deletedData.instance(i).value(divisorIndex));
                                    deletedData.instance(i).setValue(num, res);
                                }
                            } else if (StringUtils.equals(nextFeatureUri,actualModel.getSign1VRI().toString())) {
                                if (val1 >0) {
                                    deletedData.instance(i).setValue(num, 1);
                                } else if (val1 <0) {
                                    deletedData.instance(i).setValue(num, -1);
                                }
                            } else if (StringUtils.equals(nextFeatureUri,actualModel.getSign2VRI().toString())) {
                                if (val2 >0) {
                                    deletedData.instance(i).setValue(num, 1);
                                } else if (val2 <0) {
                                    deletedData.instance(i).setValue(num, -1);
                                }
                            } else if (StringUtils.equals(nextFeatureUri,actualModel.getMagn1VRI().toString())) {
                                res = abs(val1);
                                deletedData.instance(i).setValue(num, res);
                            } else if (StringUtils.equals(nextFeatureUri,actualModel.getMagn2VRI().toString())) {
                                res = abs(val2);
                                deletedData.instance(i).setValue(num, res);
                            }
                        
                    }
                }
            }

            try {
                return DatasetFactory.getInstance().createFromArff(deletedData);
            } catch (ToxOtisException ex) {
                throw new JaqpotException(ex);
            }
        } else {
            throw new JaqpotException("Independent features not found in Instances");
        }

        } catch (Throwable thr){
            thr.printStackTrace();

        }
        return null;
    }
}
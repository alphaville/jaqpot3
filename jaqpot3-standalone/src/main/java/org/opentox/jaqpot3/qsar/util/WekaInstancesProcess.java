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
 * Copyright (C) 2009-2014 Pantelis Sopasakis & Charalampos Chomenides & Lampovas Nikolaos
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

package org.opentox.jaqpot3.qsar.util;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.dmg.pmml.DerivedField;
import org.dmg.pmml.PMML;
import org.dmg.pmml.TransformationDictionary;
import org.jpmml.evaluator.FieldValue;
import org.opentox.jaqpot3.exception.JaqpotException;
import org.opentox.jaqpot3.qsar.IClientInput;
import org.opentox.jaqpot3.qsar.exceptions.QSARException;
import static org.opentox.jaqpot3.qsar.util.AttributeCleanup.AttributeType.nominal;
import static org.opentox.jaqpot3.qsar.util.AttributeCleanup.AttributeType.numeric;
import static org.opentox.jaqpot3.qsar.util.AttributeCleanup.AttributeType.string;
import org.opentox.toxotis.core.component.Feature;
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
 * @author Nikolaos Lampovas
 */
public class WekaInstancesProcess {
    
    private static WekaInstancesProcess instanceOfThis = null;
    public static final WekaInstancesProcess proc = getInstance();

    private static WekaInstancesProcess getInstance() {
        if (instanceOfThis == null) {
            instanceOfThis = new WekaInstancesProcess();
        }
        return instanceOfThis;
    }

    public static Map<String,Double> getInstanceAttributeValues(Instance inst,int numAttributes) {
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
    
    public static List<Integer> getDescriptorsIndexArray(Instances inputData,List<Feature> independentFeatures,Feature dependentFeature) {
        List<Integer> tempArray = new ArrayList();
        int NAttr = inputData.numAttributes();
        
        if (!independentFeatures.isEmpty()) {
            //get the index of the uri to be kept, this URI must be first and preceedes the independent features 
            for(int i=0;i<NAttr;++i) {
                for(int j=0;j<independentFeatures.size();++j) {
                    String uri = independentFeatures.get(j).getUri().toString();
                    if(StringUtils.equals( inputData.attribute(i).name().toString() , uri)) {
                        tempArray.add(i);
                        break;
                    }
                }
            }
        }
        if (StringUtils.isNotEmpty(dependentFeature.getUri().toString())) {
            for(int i=0;i<NAttr;++i) {
                if(StringUtils.equals( inputData.attribute(i).name().toString() , dependentFeature.getUri().toString() )) {
                    tempArray.add(i);
                    break;
                }
            }
        }
        
        return tempArray;
    }
    
    public static Instances getFilteredInstances(Instances inputData,List<Feature> independentFeatures,Feature dependentFeature) throws JaqpotException {
        try {
            List<Integer> indexArray = getDescriptorsIndexArray(inputData,independentFeatures,dependentFeature);
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
    
    public static Instances addNewAttribute(Instances filteredData,String nextFeatureUri) throws JaqpotException{
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
    
    
    public static Instances loadJustCompounds(Instances inputSet) throws JaqpotException{
        AttributeCleanup justCompounds = new AttributeCleanup(true, nominal, numeric, string);
        Instances compounds = null;
        try {
            compounds = justCompounds.filter(inputSet);
        } catch (QSARException ex) {
            String message = "Exception while filtering compounds";
            throw new JaqpotException(message, ex);
        }
        return compounds;
    }
    
    
    public static Instances handleMissingValues(Instances inst,IClientInput ClientParams) throws JaqpotException{
        Instances rv;
        try {
            SimpleMVHFilter mvh = new SimpleMVHFilter();
            mvh.parametrize(ClientParams);
            rv = mvh.filter(inst);
        } catch ( JaqpotException ex) {
            String message = "Exception while trying to handle missing values";
            throw new JaqpotException(message, ex);
        }
        return rv;
    }
    
    public static void toCSV(Instances inst,String Filename){
        
        File file = new File(Filename);
        CSVSaver cs = new CSVSaver();
        
        try {
            cs.setRetrieval(1);
            cs.setFile(file);
            cs.setInstances(inst);
            cs.writeBatch();
        } catch (Exception ex) {}
    }
    
    public static Instances transformDataset(Instances inst,PMML pmmlObject) throws JaqpotException{
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
    
    public static List<Integer> getTransformationFieldsAttrIndex(Instances inst,PMML pmmlObject) throws JaqpotException{
        //TODO add new features when uris missing
        List<Integer> res = new ArrayList<Integer>();
        try {     

            if (pmmlObject!=null) {
                //Get the Derived fields (math formulas) of the PMML file
                TransformationDictionary trDir = pmmlObject.getTransformationDictionary();
                if (trDir!=null) {
                    List<DerivedField> dfVar = trDir.getDerivedFields();

                    if (!dfVar.isEmpty()) {
                        int numAttributes = inst.numAttributes();
                        for(int i=0;i<dfVar.size();++i) {
                            
                            for (int j = 0; j < numAttributes; j++) {
                                if(StringUtils.equals(dfVar.get(i).getName().getValue(),inst.attribute(j).name())){
                                    res.add(j);
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception ex) {
            String message = "Exception while trying to transform Instances";
            throw new JaqpotException(message, ex);
        }
                
        return res;
    }
}

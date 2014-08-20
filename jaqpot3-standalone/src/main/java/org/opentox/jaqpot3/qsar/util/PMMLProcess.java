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


package org.opentox.jaqpot3.qsar.util;

import com.sun.org.apache.bcel.internal.util.ByteSequence;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import org.apache.commons.lang.StringUtils;
import org.dmg.pmml.PMML;
import org.dmg.pmml.TransformationDictionary;
import org.jpmml.model.ImportFilter;
import org.jpmml.model.JAXBUtil;
import org.opentox.jaqpot3.exception.JaqpotException;
import org.opentox.jaqpot3.qsar.serializable.FastRbfNnModel;
import org.opentox.jaqpot3.qsar.serializable.PLSModel;
import org.opentox.jaqpot3.resources.collections.Algorithms;
import org.opentox.jaqpot3.util.Configuration;
import org.opentox.toxotis.core.component.Feature;
import org.opentox.toxotis.core.component.Model;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import weka.classifiers.functions.LinearRegression;
import weka.classifiers.functions.SVMreg;
import weka.classifiers.functions.supportVector.Kernel;
import weka.core.Instances;
import weka.filters.supervised.attribute.PLSFilter;

/**
 *
 * @author Pantelis Sopasakis
 * @author Charalampos Chomenides
 */

//TODO: PMML insert to pmml xml the statistics 
public class PMMLProcess {

    private static org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(PMMLProcess.class);

    public static String generatePMML(Model model) throws JaqpotException{
        if(model.getAlgorithm().equals(Algorithms.mlr())){
            return generateMLR(model);
        }else if(model.getAlgorithm().equals(Algorithms.plsFilter())){
            return generatePLS(model);
        }else if(model.getAlgorithm().equals(Algorithms.svm())){
            return generateSVM(model);
        }else if(model.getAlgorithm().equals(Algorithms.fastRbfNn())){
            return generateFastRbf(model);
        }else{
            throw new UnsupportedOperationException("PMML Representation for this model is not implemented yet.");
        }
    }

    private static String generateMLR(Model model) throws JaqpotException{
        LinearRegression wekaModel = (LinearRegression) model.getActualModel().getSerializableActualModel();
        byte[] pmmlFile = model.getActualModel().getPmml();
        
        String uuid = model.getUri().getId();
        String PMMLIntro = Configuration.getStringProperty("pmml.intro");
        StringBuilder pmml = new StringBuilder();
        try {
            final double[] coefficients = wekaModel.coefficients();
            pmml.append("<?xml version=\"1.0\" ?>");
            pmml.append(PMMLIntro);
            pmml.append("<Model ID=\"" + uuid + "\" Name=\"MLR Model\">\n");
            pmml.append("<AlgorithmID href=\"" + Configuration.BASE_URI + "/algorithm/mlr\"/>\n");

            pmml.append("<DatasetID href=\"" + model.getDataset().toString() + "\"/>\n");
            pmml.append("<AlgorithmParameters />\n");
            pmml.append("<Timestamp>" + java.util.GregorianCalendar.getInstance().getTime() + "</Timestamp>\n");
            pmml.append("</Model>\n");
            pmml.append("<DataDictionary numberOfFields=\"" + model.getIndependentFeatures().size() + "\" >\n");
            for (Feature feature : model.getIndependentFeatures()) {
                pmml.append("<DataField name=\"" + feature.getUri().toString() + "\" optype=\"continuous\" dataType=\"double\" />\n");
            }
            pmml.append("</DataDictionary>\n");
            
            if(pmmlFile!=null) {
                if(pmmlFile.length>0) {
                    String TrDictionaryString = getTransformationDictionaryXML(pmmlFile);
                    pmml.append(TrDictionaryString+"\n");
                }
            }
           
           String dependentFeatures = (model.getDependentFeatures().isEmpty()) ? 
                                "" : model.getDependentFeatures().iterator().next().getUri().toString() ;
            // RegressionModel
            pmml.append("<RegressionModel modelName=\"" + uuid.toString() + "\"" + 
                    " functionName=\"regression\" modelType=\"linearRegression\"" +
                    " algorithmName=\"linearRegression\">\n");
            // RegressionModel::MiningSchema
            pmml.append("<MiningSchema>\n");
            for (Feature feature : model.getIndependentFeatures()) {
                    pmml.append("<MiningField name=\"" + feature.getUri().toString() + "\" />\n");

            }
            
            pmml.append("<MiningField name=\"" + dependentFeatures + "\" " + "usageType=\"predicted\"/>\n");
            pmml.append("</MiningSchema>\n");
            // RegressionModel::RegressionTable
            pmml.append("<RegressionTable intercept=\"" + coefficients[coefficients.length - 1] + "\">\n");
            
            int k = 0;
            
            for (k = 0; k < model.getIndependentFeatures().size() ; k++) {
                    pmml.append("<NumericPredictor name=\"" + model.getIndependentFeatures().get(k).getUri().toString() + "\" " + " exponent=\"1\" " + "coefficient=\"" + coefficients[k] + "\"/>\n");
            }
            
            if(pmmlFile!=null) {
                if(pmmlFile.length>0) {
                    PMML pmmlObject = loadPMMLObject(pmmlFile);
                    TransformationDictionary trDir = pmmlObject.getTransformationDictionary();
                    if(trDir!=null) {
                        int trDirSize = trDir.getDerivedFields().size();
                        int j=0;
                        while(j<trDirSize) {
                            pmml.append("<NumericPredictor name=\"" + trDir.getDerivedFields().get(j).getName().toString()+ "\" " + " exponent=\"1\" " + "coefficient=\"" + coefficients[k] + "\"/>\n");
                            ++k;
                            ++j;
                        }
                    }
                }
            }
            pmml.append("</RegressionTable>\n");
            pmml.append("</RegressionModel>\n");
            pmml.append("</PMML>\n\n");
        } catch (UnsupportedEncodingException ex) {
            String message = "Character Encoding :'"
                    + Configuration.getStringProperty("jaqpot.urlEncoding") + "' is not supported.";
            logger.debug(message, ex);
            throw new JaqpotException(message, ex);
        } catch (Exception ex) {
            String message = "Unexpected exception was caught while generating"
                    + " the PMML representaition of a trained model.";
            logger.error(message, ex);
            throw new JaqpotException(message, ex);
        }
        return pmml.toString();
    }
    
    
    private static String generateSVM(Model model) throws JaqpotException{
        SVMreg regressor = (SVMreg) model.getActualModel().getSerializableActualModel();
        Kernel kr = regressor.getKernel();
        byte[] pmmlFile = model.getActualModel().getPmml();
        
        String uuid = model.getUri().getId();
        String PMMLIntro = Configuration.getStringProperty("pmml.intro");
        StringBuilder pmml = new StringBuilder();
        String kernel = GenericUtils.getValueFromSet(model.getParameters(), "kernel").toLowerCase();
        
        try {
            pmml.append("<?xml version=\"1.0\" ?>");
            pmml.append(PMMLIntro);
            pmml.append("<Model ID=\"" + uuid + "\" Name=\"SVM Model\">\n");
            pmml.append("<AlgorithmID href=\"" + Configuration.BASE_URI + "/algorithm/svm\"/>\n");

            pmml.append("<DatasetID href=\"" + model.getDataset().toString() + "\"/>\n");
            pmml.append("<AlgorithmParameters />\n");
            pmml.append("<Timestamp>" + java.util.GregorianCalendar.getInstance().getTime() + "</Timestamp>\n");
            pmml.append("</Model>\n");
            pmml.append("<DataDictionary numberOfFields=\"" + model.getIndependentFeatures().size() + "\" >\n");
            for (Feature feature : model.getIndependentFeatures()) {
                pmml.append("<DataField name=\"" + feature.getUri().toString() + "\" optype=\"continuous\" dataType=\"double\" />\n");
            }
            pmml.append("</DataDictionary>\n");
            
            if(pmmlFile!=null) {
                if(pmmlFile.length>0) {
                    String TrDictionaryString = getTransformationDictionaryXML(pmmlFile);
                    pmml.append(TrDictionaryString+"\n");
                }
            }
           
            String dependentFeatures = (model.getDependentFeatures().isEmpty()) ? 
                                "" : model.getDependentFeatures().iterator().next().getUri().toString() ;
           
            String svmRepresentation =  (StringUtils.equals(kernel, "linear")) ? "Coefficients" : "SupportVectors";           
            // SupportVectorMachineModel
            pmml.append("<SupportVectorMachineModel modelName=\"" + uuid.toString() + "\"" + 
                    " algorithmName=\"supportVectorMachine\" functionName=\"regression\" svmRepresentation=\""+svmRepresentation+"\">\n");
            
            pmml.append("<MiningSchema>\n");
            for (Feature feature : model.getIndependentFeatures()) {
                    pmml.append("<MiningField name=\"" + feature.getUri().toString() + "\" />\n");

            }
            
            pmml.append("<MiningField name=\"" + dependentFeatures + "\" " + "usageType=\"predicted\"/>\n");
            pmml.append("</MiningSchema>\n");
            
            if(StringUtils.equals(kernel, "rbf")) {
                String gamma = GenericUtils.getValueFromSet(model.getParameters(), "gamma");
                pmml.append("<RadialBasisKernelType gamma=\"" + gamma + "\" description=\"Radial basis kernel type\"/>\n");
                
            } else if(StringUtils.equals(kernel, "polynomial")) {
                String gamma = GenericUtils.getValueFromSet(model.getParameters(), "gamma");
                String degree = GenericUtils.getValueFromSet(model.getParameters(), "degree");
                pmml.append("<PolynomialKernelType coef0=\"0\" gamma=\"" + gamma + "\" degree=\"" + degree + "\" description=\"Polynomial kernel type\"/>\n");
            } else if(StringUtils.equals(kernel, "linear")) {
                
                pmml.append("<LinearKernelType description=\"Linear kernel type\"/>\n");
            }
            
            if(StringUtils.equals(kernel, "rbf") || StringUtils.equals(kernel, "polynomial") ) {
                Map<Integer,String> supportVectors = getSupportVectors(regressor.toString());
                String supportCoeffs = getSupportVectorsCoefficientB(regressor.toString());

                pmml.append("<VectorDictionary numberOfVectors=\""+supportVectors.size()+"\">\n");
                pmml.append("<VectorFields  numberOfFields=\""+model.getIndependentFeatures().size()+"\">\n");
                for (Feature feature : model.getIndependentFeatures()) {
                   pmml.append("<FieldRef field=\"" + feature.getUri().toString() + "\" />\n");
                }
                pmml.append("</VectorFields>\n");
                for (int i=0;i<supportVectors.size();++i) {
                   pmml.append("<VectorInstance id=\"k" + i + "\">\n");
                   pmml.append("</VectorInstance>\n");
                }    

                pmml.append("</VectorDictionary>\n");
            
                pmml.append("<SupportVectorMachine targetCategory=\"no\" alternateTargetCategory=\"yes\">\n");
                pmml.append("<SupportVectors numberOfAttributes=\""+model.getIndependentFeatures().size()+"\" numberOfSupportVectors=\""+supportVectors.size()+"\">\n");
                 for (int i=0;i<supportVectors.size();++i) {
                   pmml.append("<SupportVector vectorId=\"k" + i + "\">\n");
                   pmml.append("</SupportVector>\n");
                }    
                pmml.append("</SupportVectors>\n");

                pmml.append("<Coefficients absoluteValue=\""+supportCoeffs+"\" numberOfCoefficients=\""+supportVectors.size()+"\">\n");
                 for (int i=0;i<supportVectors.size();++i) {
                   pmml.append("<Coefficient value=\""+supportVectors.get(i)+"\"/>\n");
                }    
                pmml.append("</Coefficients>\n");


                pmml.append("</SupportVectorMachine>\n");
            } else if( StringUtils.equals(kernel, "linear")) {
                List<String> linearCoeffs = getSVMLinearCoefficients(regressor.toString());
                String supportCoeffs = getSVMLinearCoefficientB(regressor.toString());

                pmml.append("<SupportVectorMachine targetCategory=\"no\" alternateTargetCategory=\"yes\">\n");
           
                pmml.append("<Coefficients absoluteValue=\""+supportCoeffs+"\" numberOfCoefficients=\""+linearCoeffs.size()+"\">\n");
                 for (int i=0;i<linearCoeffs.size();++i) {
                   pmml.append("<Coefficient value=\""+linearCoeffs.get(i)+"\"/>\n");
                }    
                pmml.append("</Coefficients>\n");

                pmml.append("</SupportVectorMachine>\n");
            }
            
            pmml.append("</SupportVectorMachineModel>\n");
            pmml.append("</PMML>\n\n");
        } catch (UnsupportedEncodingException ex) {
            String message = "Character Encoding :'"
                    + Configuration.getStringProperty("jaqpot.urlEncoding") + "' is not supported.";
            logger.debug(message, ex);
            throw new JaqpotException(message, ex);
        } catch (Exception ex) {
            String message = "Unexpected exception was caught while generating"
                    + " the PMML representation of a trained model.";
            logger.error(message, ex);
            throw new JaqpotException(message, ex);
        }
        return pmml.toString();
    }
    
    private static String generatePLS(Model model) throws JaqpotException{
        PLSModel actual = (PLSModel) model.getActualModel().getSerializableActualModel();
        PLSFilter plsFilter = actual.getPls();
        byte[] pmmlFile = model.getActualModel().getPmml();
        
        String uuid = model.getUri().getId();
        String PMMLIntro = Configuration.getStringProperty("pmml.intro");
        StringBuilder pmml = new StringBuilder();
        try {
            pmml.append("<?xml version=\"1.0\" ?>");
            pmml.append(PMMLIntro);
            pmml.append("<Model ID=\"" + uuid + "\" Name=\"PLS Model\">\n");
            pmml.append("<AlgorithmID href=\"" + Configuration.BASE_URI + "/algorithm/pls\"/>\n");
          //  URI trainingDatasetURI = URI.create(model.getDataset().getUri());

            pmml.append("<DatasetID href=\"" + URLEncoder.encode(model.getDataset().toString(),
                    Configuration.getStringProperty("jaqpot.urlEncoding")) + "\"/>\n");
            pmml.append("<AlgorithmParameters />\n");
//            pmml.append("<FeatureDefinitions>\n");
//            for (Feature feature : model.getIndependentFeatures()) {
//                pmml.append("<link href=\"" + feature.getUri().toString() + "\"/>\n");
//            }
//            pmml.append("<target index=\"" + data.attribute(model.getPredictedFeature().getUri().toString()).index() + "\" name=\"" + model.getPredictedFeature().getUri().toString() + "\"/>\n");
//            pmml.append("</FeatureDefinitions>\n");
            pmml.append("<Timestamp>" + java.util.GregorianCalendar.getInstance().getTime() + "</Timestamp>\n");
            pmml.append("</Model>\n");
            pmml.append("<DataDictionary numberOfFields=\"" + model.getIndependentFeatures().size() + "\" >\n");
            for (Feature feature : model.getIndependentFeatures()) {
                pmml.append("<DataField name=\"" + feature.getUri().toString() + "\" optype=\"continuous\" dataType=\"double\" />\n");
            }
            pmml.append("</DataDictionary>\n");
            
            if(pmmlFile!=null) {
                if(pmmlFile.length>0) {
                    String TrDictionaryString = getTransformationDictionaryXML(pmmlFile);
                    pmml.append(TrDictionaryString+"\n");
                }
            }
            
            // RegressionModel
            pmml.append("<RegressionModel modelName=\"" + uuid.toString() + "\"" + 
                    " functionName=\"regression\" modelType=\"linearRegression\"" +
                    " algorithmName=\"linearRegression\">\n");
            // RegressionModel::MiningSchema
            pmml.append("<MiningSchema>\n");
            for (Feature feature : model.getIndependentFeatures()) {
                    pmml.append("<MiningField name=\"" + feature.getUri().toString() + "\" />\n");

            }
            for (Feature feature : model.getPredictedFeatures()) {
                    pmml.append("<MiningField name=\"" + feature.getUri().toString() + "\" " + "usageType=\"predicted\"/>\n");

            }
            pmml.append("</MiningSchema>\n");
            // RegressionModel::RegressionTable
           
            pmml.append("<ParameterList>");
            pmml.append("<Parameter name=\"algorithm\" label=\"Algorithm\"/>");
            pmml.append("<Parameter name=\"preprocessing\" label=\"Preprocessing\"/>");
            pmml.append("<Parameter name=\"doUpdateClass\" label=\"doUpdateClass\"/>");
            pmml.append("<Parameter name=\"numComponents\" label=\"Number of Components\"/>");
            pmml.append("</ParameterList>");
            pmml.append("</RegressionTable>\n");
            pmml.append("</RegressionModel>\n");
            pmml.append("</PMML>\n\n");
        } catch (UnsupportedEncodingException ex) {
            String message = "Character Encoding :'"
                    + Configuration.getStringProperty("jaqpot.urlEncoding") + "' is not supported.";
            logger.debug(message, ex);
            throw new JaqpotException(message, ex);
        } catch (Exception ex) {
            String message = "Unexpected exception was caught while generating"
                    + " the PMML representaition of a trained model.";
            logger.error(message, ex);
            throw new JaqpotException(message, ex);
        }
        return pmml.toString();
    }
    
    
    private static String generateFastRbf(Model model) throws JaqpotException{
        FastRbfNnModel rbfModel = (FastRbfNnModel) model.getActualModel().getSerializableActualModel();
        byte[] pmmlFile = model.getActualModel().getPmml();
        
        Instances rbfNnNodes = rbfModel.getNodes();
        double[] sigma = rbfModel.getSigma();
        
        String uuid = model.getUri().getId();
        String PMMLIntro = Configuration.getStringProperty("pmml.intro");
        StringBuilder pmml = new StringBuilder();
        try {
            //final double[] coefficients = wekaModel.coefficients();
            pmml.append("<?xml version=\"1.0\" ?>");
            pmml.append(PMMLIntro);
            pmml.append("<Model ID=\"" + uuid + "\" Name=\"fastRbfNn Model\">\n");
            pmml.append("<AlgorithmID href=\"" + Configuration.BASE_URI + "/algorithm/fastRbfNn\"/>\n");

            pmml.append("<DatasetID href=\"" + model.getDataset().toString() + "\"/>\n");
            pmml.append("<AlgorithmParameters />\n");
            pmml.append("<Timestamp>" + java.util.GregorianCalendar.getInstance().getTime() + "</Timestamp>\n");
            pmml.append("</Model>\n");
            pmml.append("<DataDictionary numberOfFields=\"" + model.getIndependentFeatures().size() + "\" >\n");
            for (Feature feature : model.getIndependentFeatures()) {
                pmml.append("<DataField name=\"" + feature.getUri().toString() + "\" optype=\"continuous\" dataType=\"double\" />\n");
            }
            pmml.append("</DataDictionary>\n");
            
            if(pmmlFile!=null) {
                if(pmmlFile.length>0) {
                    String TrDictionaryString = getTransformationDictionaryXML(pmmlFile);
                    pmml.append(TrDictionaryString+"\n");
                }
            }
           
           String dependentFeatures = (model.getDependentFeatures().isEmpty()) ? 
                                "" : model.getDependentFeatures().iterator().next().getUri().toString() ;
            // fastRbfNnModel
            pmml.append("<NeuralNetwork modelName=\"" + uuid.toString() + "\"" + 
                    " functionName=\"regression\" activationFunction=\"radialBasis\">\n");
            
            pmml.append("<MiningSchema>\n");
            for (Feature feature : model.getIndependentFeatures()) {
                    pmml.append("<MiningField name=\"" + feature.getUri().toString() + "\" />\n");

            }
            
            pmml.append("<MiningField name=\"" + dependentFeatures + "\" " + "usageType=\"predicted\"/>\n");
            pmml.append("</MiningSchema>\n");
            pmml.append("<NeuralLayer numberOfNeurons=\""+rbfNnNodes.numInstances()+"\">\n");
            int numAttributes;
            for (int i = 0; i < rbfNnNodes.numInstances(); i++) {
                numAttributes = rbfNnNodes.instance(i).numAttributes();
                
                pmml.append("<Neuron width=\""+sigma[i]+"\" id=\""+i+"\">\n");
                for (int j = 0; j < numAttributes; j++) {
                    pmml.append("<Con from=\""+j+"\" weight=\""+rbfNnNodes.instance(i).value(j)+"\"/>\n");
                }
                pmml.append("</Neuron>\n");
            }
            pmml.append("</NeuralLayer>\n");
            
            pmml.append("</NeuralNetwork>\n");
            pmml.append("</PMML>\n\n");
        } catch (UnsupportedEncodingException ex) {
            String message = "Character Encoding :'"
                    + Configuration.getStringProperty("jaqpot.urlEncoding") + "' is not supported.";
            logger.debug(message, ex);
            throw new JaqpotException(message, ex);
        } catch (Exception ex) {
            String message = "Unexpected exception was caught while generating"
                    + " the PMML representaition of a trained model.";
            logger.error(message, ex);
            throw new JaqpotException(message, ex);
        }
        return pmml.toString();
    }
    
    
    public static PMML loadPMMLObject(byte[] pmml) throws JaqpotException{
        PMML pmmlObject;
        try {    
            InputStream is = new ByteSequence(pmml);
            InputSource source = new InputSource(is);

            SAXSource transformedSource = ImportFilter.apply(source);
            pmmlObject = JAXBUtil.unmarshalPMML(transformedSource);
        } catch (Exception ex) {
            String message = "Exception while loading PMML to object";
            throw new JaqpotException(message, ex);
        }
        return pmmlObject;
    }
    
    private static String getNodeFromXML(String xml) throws JaqpotException{
        String res="";
        try {
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            ByteArrayInputStream bis = new ByteArrayInputStream(xml.getBytes());
            Document doc = docBuilder.parse(bis);

            // XPath to retrieve the content of the <FamilyAnnualDeductibleAmount> tag
            XPath xpath = XPathFactory.newInstance().newXPath();
            String expression = "/PMML/TransformationDictionary";
            Node node = (Node) xpath.compile(expression).evaluate(doc, XPathConstants.NODE);
            
            StreamResult xmlOutput = new StreamResult(new StringWriter());
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            transformer.transform(new DOMSource(node), xmlOutput);
            res =  xmlOutput.getWriter().toString();

        } catch (Exception ex) {
            String message = "Unexpected exception was caught while generating"
                    + " the PMML representaition of a trained model.";
            logger.error(message, ex);
            throw new JaqpotException(message, ex);
        }
        
        return res;
    }
    
    private static String getTransformationDictionaryXML(byte[] pmmlFile) throws JaqpotException,IOException {
        String res = "";
        PMML pmmlObject = loadPMMLObject(pmmlFile);
        PMML newPmmlObject = new PMML();
        newPmmlObject.setTransformationDictionary(pmmlObject.getTransformationDictionary());
        StringWriter outWriter = new StringWriter();
        try { 
            JAXBUtil.marshalPMML(newPmmlObject, new StreamResult(outWriter));
            StringBuffer sb = outWriter.getBuffer(); 
            res = getNodeFromXML(sb.toString());
        } catch (Exception ex) {
            String message = "Unexpected exception was caught while generating"
            + " the PMML representation of a trained model.";
            logger.error(message, ex);
            throw new JaqpotException(message, ex);
        } finally {
            outWriter.close();
        }
        return res;
    }
    
    private static String getCorrelation(String stats) {
        return getStatisticsAttr("Correlation\\s*coefficient",stats);
    }
    
    private static String getMeanAbsError(String stats) {
        return getStatisticsAttr("Mean\\s*absolute\\s*error",stats);
    }
    
    private static String getRootMeanSquaredError(String stats) {
        return getStatisticsAttr("Root\\s*mean\\s*squared\\s*error",stats);
    }
    
    private static String getRelativeAbsoluteError(String stats) {
        return getStatisticsAttr("Relative\\s*absolute\\s*error",stats);
    }
    
    private static String getRootRelativeSquaredError (String stats) {
        return getStatisticsAttr("Root\\s*relative\\s*squared\\s*error",stats);
    }
    
    private static String getStatisticsAttr(String attr,String stats) {
        return GenericUtils.getRegex("\\s*"+attr+"\\s*([0-9.]*)", stats);
    }
    
    private static Map<Integer,String> getSupportVectors(String info) {
        Map<Integer,String>  res = new HashMap();
        Pattern p = Pattern.compile("\\s*([+\\-]?(?:0|[1-9]\\d*)(?:\\.\\d*)?(?:[eE][+\\-]?\\d+)?)\\s*\\*\\s*k\\[([0-9]*)\\]");
        Matcher m = p.matcher(info);
        
        while (m.find()) {
            res.put(Integer.parseInt(m.group(2)), m.group(1));
        }
        return res;
    }
    
    
    private static String getSupportVectorsCoefficientB(String info) {
        String res = "";
        Pattern p = Pattern.compile("\\s*([+\\-]?(?:0|[1-9]\\d*)(?:\\.\\d*)?(?:[eE][+\\-]?\\d+)?)\\s*\\*\\s*k\\[([0-9]*)\\]");
        Matcher m = p.matcher(info);
        
        info = m.replaceAll("");
        p = Pattern.compile("\\s*([+\\-]\\s*?(?:0|[1-9]\\d*)(?:\\.\\d*)?(?:[eE][+\\-]?\\d+)?)");
        m = p.matcher(info);
        if (m.find()) {
            res = m.group(0);
        }
        res = res.replace("\n","");
        res = res.replaceAll(" ","");
        return res;
    }
    
    private static List<String> getSVMLinearCoefficients(String info) {
        List<String>  res = new ArrayList();
        Pattern p = Pattern.compile("\\s*([+\\-]?\\s*(?:0|[1-9]\\d*)(?:\\.\\d*)?(?:[eE][+\\-]?\\d+)?)\\s*\\*\\s*\\(normalized\\)");
        Matcher m = p.matcher(info);
        
        while (m.find()) {
            res.add(m.group(1));
        }
        return res;
    }
    
    private static String getSVMLinearCoefficientB(String info) {
        String res = "";
        Pattern p = Pattern.compile("\\s*([+\\-]?\\s*(?:0|[1-9]\\d*)(?:\\.\\d*)?(?:[eE][+\\-]?\\d+)?)\\s*\\*\\s*\\(normalized\\).*");
        Matcher m = p.matcher(info);
        
        info = m.replaceAll("");
        p = Pattern.compile("\\s*([+\\-]?\\s*(?:0|[1-9]\\d*)(?:\\.\\d*)?(?:[eE][+\\-]?\\d+)?)");
        m = p.matcher(info);
        if (m.find()) {
            res = m.group(0);
        }
        res = res.replace("\n","");
        res = res.replaceAll(" ","");
        return res;
    }
    //[-+]([0-9.]*|[0-9]*|[0-9.]*[eE]\-[0-9]*)\s*\*\s*\k\[[0-9]*\]  -?\d+(,\d+)*(\.\d+(E\-\d+)?)?
}


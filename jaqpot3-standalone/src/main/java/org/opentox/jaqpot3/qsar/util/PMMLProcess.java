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
        byte[] pmmlFile = model.getActualModel().getPmml();
        
        String uuid = model.getUri().getId();
        String PMMLIntro = Configuration.getStringProperty("pmml.intro");
        StringBuilder pmml = new StringBuilder();
        try {
            pmml.append("<?xml version=\"1.0\" ?>");
            pmml.append(PMMLIntro);
            pmml.append("<Model ID=\"" + uuid + "\" Name=\"MLR Model\">\n");
            pmml.append("<AlgorithmID href=\"" + Configuration.BASE_URI + "/algorithm/mlr\"/>\n");
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
}


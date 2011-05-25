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

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import org.opentox.jaqpot3.exception.JaqpotException;
import org.opentox.jaqpot3.resources.collections.Algorithms;
import org.opentox.jaqpot3.util.Configuration;
import org.opentox.toxotis.core.component.Feature;
import org.opentox.toxotis.core.component.Model;
import weka.classifiers.functions.LinearRegression;

/**
 *
 * @author Pantelis Sopasakis
 * @author Charalampos Chomenides
 */
public class PMMLGenerator {

    private static org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(PMMLGenerator.class);

    public static String generatePMML(Model model) throws JaqpotException{
        if(model.getAlgorithm().equals(Algorithms.mlr())){
            return generateMLR(model);
        }else{
            throw new UnsupportedOperationException("PMML Representation for this model is not implemented yet.");
        }
    }

    private static String generateMLR(Model model) throws JaqpotException{
        LinearRegression wekaModel = (LinearRegression) model.getActualModel();
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
            // RegressionModel
            pmml.append("<RegressionModel modelName=\"" + uuid.toString() + "\"" + 
                    " functionName=\"regression\" modelType=\"linearRegression\"" +
                    " algorithmName=\"linearRegression\"" + " targetFieldName=\"" +
                    model.getDependentFeatures().iterator().next().getUri().toString() + "\"" + ">\n");
            // RegressionModel::MiningSchema
            pmml.append("<MiningSchema>\n");
            for (Feature feature : model.getIndependentFeatures()) {
                    pmml.append("<MiningField name=\"" + feature.getUri().toString() + "\" />\n");

            }
            pmml.append("<MiningField name=\"" + model.getDependentFeatures().iterator().next().getUri().toString() + "\" " + "usageType=\"predicted\"/>\n");
            pmml.append("</MiningSchema>\n");
            // RegressionModel::RegressionTable
            pmml.append("<RegressionTable intercept=\"" + coefficients[coefficients.length - 1] + "\">\n");
            for (int k = 0; k < model.getIndependentFeatures().size() ; k++) {
                    pmml.append("<NumericPredictor name=\"" + model.getIndependentFeatures().get(k).getUri().toString() + "\" " + " exponent=\"1\" " + "coefficient=\"" + coefficients[k] + "\"/>\n");

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
    
}


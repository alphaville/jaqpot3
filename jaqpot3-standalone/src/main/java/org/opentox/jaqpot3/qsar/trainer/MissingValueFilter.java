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

package org.opentox.jaqpot3.qsar.trainer;

import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import java.io.NotSerializableException;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.opentox.jaqpot3.exception.JaqpotException;
import org.opentox.jaqpot3.qsar.AbstractTrainer;
import org.opentox.jaqpot3.qsar.IClientInput;
import org.opentox.jaqpot3.qsar.IParametrizableAlgorithm;
import org.opentox.jaqpot3.qsar.exceptions.BadParameterException;
import org.opentox.jaqpot3.resources.collections.Algorithms;
import org.opentox.jaqpot3.util.Configuration;
import org.opentox.toxotis.client.VRI;
import org.opentox.toxotis.client.collection.Services;
import org.opentox.toxotis.core.component.Algorithm;
import org.opentox.toxotis.core.component.Dataset;
import org.opentox.toxotis.core.component.Feature;
import org.opentox.toxotis.core.component.Model;
import org.opentox.toxotis.core.component.Parameter;
import org.opentox.toxotis.database.engine.task.UpdateTask;
import org.opentox.toxotis.database.exception.DbException;
import org.opentox.toxotis.exceptions.impl.ServiceInvocationException;
import org.opentox.toxotis.factory.FeatureFactory;
import org.opentox.toxotis.ontology.LiteralValue;
import org.opentox.toxotis.ontology.ResourceValue;
import org.opentox.toxotis.ontology.collection.OTClasses;
import weka.core.Attribute;
import weka.core.Instances;

/**
 *
 * @author Pantelis Sopasakis
 * @author Charalampos Chomenides
 */
public class MissingValueFilter extends AbstractTrainer {

    HashSet<String> ignored = new HashSet<String>();
    private VRI datasetUri;
    private VRI featureService;


    @Override
    public Model train(Instances data) throws JaqpotException {
        VRI newModelUri = Configuration.getBaseUri().augment("model", getUuid());
        Model mvh = new Model(newModelUri);
        try {
            mvh.setActualModel(ignored);
        } catch (NotSerializableException ex) {
            throw new RuntimeException(ex);
        }
        /*
         * TODO: Create NEW Features!!!
         * For every feature of the old dataset, create a new one
         */
        int nAttr = data.numAttributes();

        for (int i = 0; i < nAttr; i++) {
            Attribute attribute = data.attribute(i);
            if ((attribute.isNumeric() || attribute.isNominal()) && !ignored.contains(attribute.name())) {

                try {
                    VRI featureVri = new VRI(attribute.name());
                    mvh.addIndependentFeatures(new Feature(featureVri));
                    Feature f = FeatureFactory.createAndPublishFeature("MVH " + featureVri.toString(), "",
                            new ResourceValue(newModelUri, OTClasses.model()), featureService, token);
                    mvh.addPredictedFeatures(f);
                    getTask().getMeta().addComment("MVH feature for " + featureVri.toString()
                            + " has been created at " + f.getUri().toString());
                    getTask().setPercentageCompleted((float) ((float) i / (float) nAttr) * 99.5f);
                    UpdateTask taskUpdater = new UpdateTask(getTask());                    
                    taskUpdater.setUpdateMeta(true);
                    try {
                        taskUpdater.update();
                    } catch (DbException ex) {
                        throw new JaqpotException(ex);
                    } finally {
                        try {
                            taskUpdater.close();
                        } catch (DbException ex) {
                            throw new JaqpotException(ex);
                        }
                    }
                } catch (ServiceInvocationException ex) {
                    Logger.getLogger(MissingValueFilter.class.getName()).log(Level.SEVERE, null, ex);
                } catch (URISyntaxException ex) {
                    Logger.getLogger(MissingValueFilter.class.getName()).log(Level.SEVERE, null, ex);
                }

            }
        }

        if (ignored != null && !ignored.isEmpty()) {
            mvh.setParameters(new HashSet<Parameter>());
            for (String ign : ignored) {
                mvh.getParameters().add(new Parameter<String>("ignore_uri",
                        new LiteralValue(ign, XSDDatatype.XSDstring)).setUri(
                        Configuration.getBaseUri().augment("parameter", UUID.randomUUID().toString())).
                        setScope(Parameter.ParameterScope.OPTIONAL));
            }
        }
        mvh.setCreatedBy(getTask().getCreatedBy());
        mvh.setDataset(datasetUri);
        mvh.setAlgorithm(getAlgorithm());
        mvh.getMeta().addTitle("Missing Value Handling Model").addDescription("This model is used to replace missing values in a "
                + "dataset using the Means & Modes algorithm").addComment("MVH models, unlike predictive QSAR models, do not have predicted features. "
                + "Additionally they were not trained using some dataset for they are not models in the QSAR sense, but better to say \"Model-like web services\"");
        return mvh;
    }

    @Override
    public IParametrizableAlgorithm parametrize(IClientInput clientParameters) throws BadParameterException {
        String[] ignoredUris = clientParameters.getValuesArray("ignore_uri");
        for (String s : ignoredUris) {
            ignored.add(s);
        }
        String datasetUriString = clientParameters.getFirstValue("dataset_uri");
        if (datasetUriString == null) {
            throw new BadParameterException("The parameter 'dataset_uri' is mandatory for this algorithm.");
        }
        try {
            datasetUri = new VRI(datasetUriString);
        } catch (URISyntaxException ex) {
            throw new BadParameterException("The parameter 'dataset_uri' you provided is not a valid URI.", ex);
        }
        String featureServiceString = clientParameters.getFirstValue("feature_service");
        if (featureServiceString != null) {
            try {
                featureService = new VRI(featureServiceString);
            } catch (URISyntaxException ex) {
                throw new BadParameterException("The parameter 'feature_service' you provided is not a valid URI.", ex);
            }
        } else {
            featureService = Services.ideaconsult().augment("feature");
        }
        return this;
    }

    @Override
    public Algorithm getAlgorithm() {
        return Algorithms.mvh();
    }

    @Override
    public boolean needsDataset() {
        return false;
    }
}

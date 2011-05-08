package org.opentox.jaqpot3.qsar.filter;

import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import java.io.NotSerializableException;
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
import org.opentox.toxotis.core.component.Algorithm;
import org.opentox.toxotis.core.component.Dataset;
import org.opentox.toxotis.core.component.Feature;
import org.opentox.toxotis.core.component.Model;
import org.opentox.toxotis.core.component.Parameter;
import org.opentox.toxotis.ontology.LiteralValue;

/**
 *
 * @author Pantelis Sopasakis
 * @author Charalampos Chomenides
 */
public class MissingValueFilter extends AbstractTrainer {

    HashSet<String> ignored = new HashSet<String>();

    @Override
    public Dataset preprocessDataset(Dataset dataset) {
        return dataset;
    }

    @Override
    public Model train(Dataset data) throws JaqpotException {
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
        mvh.setDataset(data != null ? data.getUri() : null);
        mvh.setAlgorithm(getAlgorithm());
        mvh.getMeta().addTitle("Missing Value Handling Model").addDescription("This model is used to replace missing values in a " +
                "dataset using the Means & Modes algorithm").addComment("MVH models, unlike predictive QSAR models, do not have predicted features. " +
                "Additionally they were not trained using some dataset for they are not models in the QSAR sense, but better to say \"Model-like web services\"");
        return mvh;
    }

    @Override
    public IParametrizableAlgorithm parametrize(IClientInput clientParameters) throws BadParameterException {
        String[] ignoredUris = clientParameters.getValuesArray("ignore_uri");
        for (String s : ignoredUris) {
            ignored.add(s);
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

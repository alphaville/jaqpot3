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
import org.opentox.toxotis.core.component.Model;
import org.opentox.toxotis.core.component.Parameter;
import org.opentox.toxotis.core.component.User;
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
            Logger.getLogger(MissingValueFilter.class.getName()).log(Level.SEVERE, null, ex);
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
        mvh.setDataset(data.getUri());
        mvh.setAlgorithm(getAlgorithm());
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

package org.opentox.jaqpot3.qsar.trainer;

import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import java.io.NotSerializableException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.opentox.jaqpot3.exception.JaqpotException;
import org.opentox.jaqpot3.qsar.AbstractTrainer;
import org.opentox.jaqpot3.qsar.IClientInput;
import org.opentox.jaqpot3.qsar.IParametrizableAlgorithm;
import org.opentox.jaqpot3.qsar.exceptions.BadParameterException;
import org.opentox.jaqpot3.qsar.serializable.ModelBundle;
import org.opentox.jaqpot3.resources.collections.Algorithms;
import org.opentox.jaqpot3.util.Configuration;
import org.opentox.toxotis.core.component.Algorithm;
import org.opentox.toxotis.core.component.Model;
import org.opentox.toxotis.core.component.Parameter;
import org.opentox.toxotis.ontology.LiteralValue;
import weka.core.Instances;

/**
 *
 * @author Pantelis Sopasakis
 * @author Charalampos Chomenides
 */
public class ModelBundlerTrainer extends AbstractTrainer {

    private ArrayList<String> modelUris = new ArrayList<String>();
    private static final Random RANDOM = new Random(78 * System.currentTimeMillis() + 131);

    @Override
    public boolean needsDataset() {
        return false;
    }

    @Override
    public Model train(Instances data) throws JaqpotException {

        Model model = new Model(Configuration.getBaseUri().augment("model", getUuid()));
        model.setAlgorithm(getAlgorithm());
        ModelBundle actual = new ModelBundle();
        actual.setModelUris(modelUris);
        try {
            model.setActualModel(actual);
        } catch (NotSerializableException ex) {
            Logger.getLogger(ModelBundlerTrainer.class.getName()).log(Level.SEVERE, null, ex);
        }

        model.setParameters(new HashSet<Parameter>());
        for (String modelUri : modelUris) {
            Parameter p = new Parameter(Configuration.getBaseUri().augment("parameter", RANDOM.nextLong()),
                    "model", new LiteralValue(modelUri, XSDDatatype.XSDanyURI)).setScope(Parameter.ParameterScope.MANDATORY);
            model.getParameters().add(p);
        }
        return model;
    }

    @Override
    public IParametrizableAlgorithm doParametrize(IClientInput clientParameters) throws BadParameterException {
        String[] models = clientParameters.getValuesArray("model");
        for (String model : models) {
            modelUris.add(model);
            System.out.println(model + " added");
        }
        return this;
    }

    @Override
    public Algorithm getAlgorithm() {
        return Algorithms.modelBundler();
    }
}

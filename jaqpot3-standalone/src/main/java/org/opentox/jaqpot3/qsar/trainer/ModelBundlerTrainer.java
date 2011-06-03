package org.opentox.jaqpot3.qsar.trainer;

import org.opentox.jaqpot3.exception.JaqpotException;
import org.opentox.jaqpot3.qsar.AbstractTrainer;
import org.opentox.jaqpot3.qsar.IClientInput;
import org.opentox.jaqpot3.qsar.IParametrizableAlgorithm;
import org.opentox.jaqpot3.qsar.exceptions.BadParameterException;
import org.opentox.toxotis.core.component.Algorithm;
import org.opentox.toxotis.core.component.Model;
import weka.core.Instances;

/**
 *
 * @author Pantelis Sopasakis
 * @author Charalampos Chomenides
 */
public class ModelBundlerTrainer extends AbstractTrainer {

    @Override
    public boolean needsDataset() {
        return false;
    }

    @Override
    public Model train(Instances data) throws JaqpotException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public IParametrizableAlgorithm parametrize(IClientInput clientParameters) throws BadParameterException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Algorithm getAlgorithm() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}

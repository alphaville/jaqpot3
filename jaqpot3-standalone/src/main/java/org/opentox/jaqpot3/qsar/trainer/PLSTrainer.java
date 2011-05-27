package org.opentox.jaqpot3.qsar.trainer;

import org.opentox.jaqpot3.exception.JaqpotException;
import org.opentox.jaqpot3.qsar.AbstractTrainer;
import org.opentox.jaqpot3.qsar.IClientInput;
import org.opentox.jaqpot3.qsar.IParametrizableAlgorithm;
import org.opentox.jaqpot3.qsar.exceptions.BadParameterException;
import org.opentox.jaqpot3.resources.collections.Algorithms;
import org.opentox.toxotis.client.VRI;
import org.opentox.toxotis.core.component.Algorithm;
import org.opentox.toxotis.core.component.Dataset;
import org.opentox.toxotis.core.component.Model;
import weka.classifiers.functions.PLSClassifier;
import weka.core.Instances;
import weka.filters.supervised.attribute.PLSFilter;

/**
 *
 * @author Pantelis Sopasakis
 * @author Charalampos Chomenides
 */
public class PLSTrainer extends AbstractTrainer {

    @Override
    public Dataset preprocessDataset(Dataset dataset) {
        return dataset;
    }

    @Override
    public IParametrizableAlgorithm parametrize(IClientInput clientParameters) throws BadParameterException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Algorithm getAlgorithm() {
        return Algorithms.plsFilter();
    }

    @Override
    public Model train(Instances data) throws JaqpotException {
        PLSFilter pls = new PLSFilter();
        throw new UnsupportedOperationException("Not supported yet.");
    }
}

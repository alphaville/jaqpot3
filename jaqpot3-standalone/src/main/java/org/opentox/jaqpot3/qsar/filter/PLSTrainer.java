package org.opentox.jaqpot3.qsar.filter;

import org.opentox.jaqpot3.exception.JaqpotException;
import org.opentox.jaqpot3.qsar.AbstractTrainer;
import org.opentox.jaqpot3.qsar.IClientInput;
import org.opentox.jaqpot3.qsar.IParametrizableAlgorithm;
import org.opentox.jaqpot3.qsar.exceptions.BadParameterException;
import org.opentox.jaqpot3.resources.collections.Algorithms;
import org.opentox.toxotis.core.component.Algorithm;
import org.opentox.toxotis.core.component.Dataset;
import org.opentox.toxotis.core.component.Model;
import weka.classifiers.functions.PLSClassifier;
import weka.filters.supervised.attribute.PLSFilter;

/**
 *
 * @author Pantelis Sopasakis
 * @author Charalampos Chomenides
 */
public class PLSTrainer extends AbstractTrainer{

    @Override
    public Dataset preprocessDataset(Dataset dataset) {
        return dataset;
    }

    @Override
    public Model train(Dataset data) throws JaqpotException {
        
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public IParametrizableAlgorithm parametrize(IClientInput clientParameters) throws BadParameterException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Algorithm getAlgorithm() {
        return Algorithms.plsFilter();
    }

}
package org.opentox.jaqpot3.qsar;

import org.opentox.jaqpot3.exception.JaqpotException;
import org.opentox.toxotis.client.VRI;
import org.opentox.toxotis.core.component.Dataset;
import org.opentox.toxotis.core.component.Model;
import weka.core.Instances;

/**
 *
 * @author Pantelis Sopasakis
 * @author Charalampos Chomenides
 */
public interface ITrainer extends IParametrizableAlgorithm {

    Dataset preprocessDataset(Dataset dataset);

    Model train(Dataset data) throws JaqpotException;

    Model train(Instances data) throws JaqpotException;
    
    Model train(VRI data) throws JaqpotException;

    boolean needsDataset();
}

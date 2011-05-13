package org.opentox.jaqpot3.qsar.filter;

import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.opentox.jaqpot3.exception.JaqpotException;
import org.opentox.jaqpot3.qsar.AbstractPredictor;
import org.opentox.jaqpot3.qsar.IClientInput;
import org.opentox.jaqpot3.qsar.IPredictor;
import org.opentox.jaqpot3.qsar.exceptions.BadParameterException;
import org.opentox.toxotis.core.component.Dataset;
import weka.core.Instances;
import weka.filters.unsupervised.attribute.ReplaceMissingValues;

/**
 *
 * @author Pantelis Sopasakis
 * @author Charalampos Chomenides
 */
public class MissingValueFilterPredictor extends AbstractPredictor {

    @Override
    public IPredictor parametrize(IClientInput clientParameters) throws BadParameterException {
        return this;
    }

    @Override
    public Dataset predict(Dataset input) throws JaqpotException {
        HashSet<String> ignoredUris = (HashSet<String>) model.getActualModel();       

        weka.filters.unsupervised.attribute.ReplaceMissingValues replacer = new ReplaceMissingValues();
        
        Instances i = input.getInstances();
        try {
            replacer.setInputFormat(i);
        } catch (Exception ex) {
            Logger.getLogger(MissingValueFilterPredictor.class.getName()).log(Level.SEVERE, null, ex);
        }
        



        throw new UnsupportedOperationException("Not supported yet.");
    }

}
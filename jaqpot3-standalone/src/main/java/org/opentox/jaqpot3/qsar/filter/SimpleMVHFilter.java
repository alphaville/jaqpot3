package org.opentox.jaqpot3.qsar.filter;


import org.opentox.jaqpot3.exception.JaqpotException;
import org.opentox.jaqpot3.qsar.IClientInput;
import org.opentox.jaqpot3.resources.collections.Algorithms;
import org.opentox.toxotis.core.component.Algorithm;
import org.opentox.toxotis.core.component.Dataset;
import org.opentox.toxotis.exceptions.impl.ToxOtisException;
import org.opentox.toxotis.factory.DatasetFactory;
import weka.core.Instances;
import weka.filters.unsupervised.attribute.ReplaceMissingValues;

/**
 *
 * @author Pantelis Sopasakis
 * @author Charalampos Chomenides
 * @deprecated
 */

@Deprecated
public class SimpleMVHFilter  {

    private boolean ignoreClass = false;

    
    public Algorithm getAlgorithm() {
        return Algorithms.plsFilter();
    }

    public Instances filter(Instances data) throws JaqpotException {

        ReplaceMissingValues replacer = new ReplaceMissingValues();
        try {
            replacer.setInputFormat(data);
            replacer.setIgnoreClass(ignoreClass);
            Instances filtered_data = ReplaceMissingValues.useFilter(data, replacer);
            return filtered_data;
        } catch (Exception ex) {
            throw new JaqpotException("Cannot apply missing values filtering", ex);
        }
    }

    public Dataset process(Dataset data) throws JaqpotException {
        Instances in = data.getInstances();
        Instances out = filter(in);
        Dataset o = null;
        try {
            o = DatasetFactory.createFromArff(out);
        } catch (ToxOtisException ex) {
            throw new JaqpotException(ex);
        }
        return o;
    }

    public SimpleMVHFilter parametrize(IClientInput clientParameters) {
        if (clientParameters != null) {
            String ignoreClassIN = clientParameters.getFirstValue("ignoreClass");
            if (ignoreClassIN != null) {
                ignoreClass = Boolean.parseBoolean(ignoreClassIN.trim());
            }
        }
        /* No parameters needed here */
        return this;
    }
//    public static void main(String... args) throws Exception{
//        Dataset d = new Dataset(Services.ambitUniPlovdiv().augment("dataset","6").addUrlParameter("max", "5")).loadFromRemote();
//        SimpleMVHFilter filter = new SimpleMVHFilter();
//        Dataset o = filter.process(d);
//        System.out.println(o.getInstances());
//        System.out.println();
//        Future<VRI> t = o.publish(Services.ambitUniPlovdiv().augment("dataset"), (AuthenticationToken) null);
//         while (!t.isDone()) {
//        }
//        System.out.println(t.get());
//    }
}

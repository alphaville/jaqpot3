package org.opentox.jaqpot3.qsar.filter;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.opentox.jaqpot3.exception.JaqpotException;
import org.opentox.jaqpot3.qsar.AbstractPredictor;
import org.opentox.jaqpot3.qsar.IClientInput;
import org.opentox.jaqpot3.qsar.IPredictor;
import org.opentox.jaqpot3.qsar.exceptions.BadParameterException;
import org.opentox.toxotis.client.VRI;
import org.opentox.toxotis.core.component.Dataset;
import org.opentox.toxotis.core.component.Feature;
import org.opentox.toxotis.core.component.Model;
import org.opentox.toxotis.exceptions.impl.ToxOtisException;
import org.opentox.toxotis.factory.DatasetFactory;
import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;

/**
 *
 * @author Pantelis Sopasakis
 */
public class ScalingPredictor extends AbstractPredictor {

    @Override
    public IPredictor parametrize(IClientInput clientParameters) throws BadParameterException {
        return this;
    }

    @Override
    public Dataset predict(Dataset input) throws JaqpotException {
        
        getTask().setPercentageCompleted(3);
        

        ScalingModel actualModel = (ScalingModel) model.getActualModel();


        for (Feature f : model.getPredictedFeatures()) {
            System.out.println(">>> " + f.getUri());
        }

        Instances inputData = input.getInstances();
        int Nattr = inputData.numAttributes();
        int Ninst = inputData.numInstances();

        System.out.println(3);
        double[] mins = new double[Nattr - 1];
        double[] maxs = new double[Nattr - 1];

        System.out.println(4);
        Attribute currentAttr = null;
        VRI attributeAsVRI = null;

        for (int jAttr = 1; jAttr < Nattr; jAttr++) {
            System.out.println(5);
            currentAttr = inputData.attribute(jAttr);
            System.out.println(6);
            try {
                attributeAsVRI = new VRI(currentAttr.name());
            } catch (URISyntaxException ex) {
                ex.printStackTrace(System.out);
                continue;
            }

            System.out.println(7);
            mins[jAttr - 1] = 0;
            maxs[jAttr - 1] = 0;
            if (currentAttr.isNumeric()) {
                System.out.println(8);
                mins[jAttr - 1] = actualModel.getMinVals().get(attributeAsVRI);
                System.out.println(9);
                maxs[jAttr - 1] = actualModel.getMaxVals().get(attributeAsVRI);
                System.out.println(10);
            }
        }

        /* Rename attributes */
        for (int j = 1; j < Nattr; j++) {
            VRI scaledFeatureVri = correspondingScaledVri(inputData.attribute(j).name(), model);
            System.out.println("renaming " + inputData.attribute(j).name() + " into " + scaledFeatureVri);
            if (scaledFeatureVri != null) {
                inputData.renameAttribute(j, scaledFeatureVri.toString());
            }
        }

        /* Scale the data */
        Attribute currentAttribute = null;
        Instance currentInstance = null;
        for (int i = 0; i < Ninst; i++) {
            currentInstance = inputData.instance(i);
            for (int j = 1; j < Nattr; j++) {
                currentAttribute = inputData.attribute(j);
                if (currentAttribute.isNumeric() && !currentInstance.isMissing(j)) {
                    double scaledValue = (currentInstance.value(j) - mins[j - 1]) / (maxs[j - 1] - mins[j - 1]);
                    currentInstance.setValue(j, scaledValue);
                    System.out.println(scaledValue);
                }
            }
        }


        try {
            return DatasetFactory.createFromArff(inputData);
        } catch (ToxOtisException ex) {
            System.out.println("FAILURE");
            return null;
        }

    }

    private VRI correspondingScaledVri(String independentVri, Model scalingModel) {
        int index = -1;
        for (int i = 0; i < scalingModel.getIndependentFeatures().size(); i++) {
            System.out.println(scalingModel.getIndependentFeatures().get(i).getUri().toString() + " $$$$");
            System.out.println(independentVri.toString() + " @@@@");
            if (scalingModel.getIndependentFeatures().get(i).getUri().toString().equals(independentVri)) {
                System.out.println("******");
                index = i;
                break;
            }
        }
        if (index != -1) {
            return scalingModel.getPredictedFeatures().get(index).getUri();
        }
        return null;

    }
}

/*
 *
 * Jaqpot - version 3
 *
 * The JAQPOT-3 web services are OpenTox API-1.2 compliant web services. Jaqpot
 * is a web application that supports model training and data preprocessing algorithms
 * such as multiple linear regression, support vector machines, neural networks
 * (an in-house implementation based on an efficient algorithm), an implementation
 * of the leverage algorithm for domain of applicability estimation and various
 * data preprocessing algorithms like PLS and data cleanup.
 *
 * Copyright (C) 2009-2012 Pantelis Sopasakis & Charalampos Chomenides
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Contact:
 * Pantelis Sopasakis
 * chvng@mail.ntua.gr
 * Address: Iroon Politechniou St. 9, Zografou, Athens Greece
 * tel. +30 210 7723236
 *
 */
package org.opentox.jaqpot3.qsar.predictor;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.opentox.jaqpot3.exception.JaqpotException;
import org.opentox.jaqpot3.qsar.AbstractPredictor;
import org.opentox.jaqpot3.qsar.InstancesUtil;
import org.opentox.jaqpot3.qsar.exceptions.QSARException;
import org.opentox.jaqpot3.qsar.serializable.PLSModel;
import org.opentox.jaqpot3.qsar.util.AttributeCleanup;
import org.opentox.toxotis.core.component.Feature;
import org.opentox.toxotis.core.component.Parameter;
import weka.core.Instances;
import weka.filters.Filter;
import weka.filters.supervised.attribute.PLSFilter;

import static org.opentox.jaqpot3.qsar.util.AttributeCleanup.AttributeType.*;
import org.opentox.jaqpot3.qsar.util.WekaInstancesProcess;

/**
 *
 * @author Pantelis Sopasakis
 */
public class PLSPredictor extends AbstractPredictor {
    
    @Override
    public Instances predict(Instances input) throws JaqpotException {
        PLSModel actual = (PLSModel) model.getActualModel().getSerializableActualModel();
        PLSFilter plsFilter = actual.getPls();
        Instances newData = InstancesUtil.sortForPMMLModel(model.getIndependentFeatures(),trFieldsAttrIndex, input, -1);
        try {
            newData = Filter.useFilter(newData, plsFilter);
        } catch (Exception ex) {
            Logger.getLogger(PLSPredictor.class.getName()).log(Level.SEVERE, null, ex);
        }

        AttributeCleanup justCompounds = new AttributeCleanup(true, nominal, numeric, string);
        Instances compounds = null;
        try {
            compounds = justCompounds.filter(input);
        } catch (QSARException ex) {
//                logger.debug(null, ex);
        }

        int i = 0;
        for (Feature f : model.getPredictedFeatures()) {
            newData.renameAttribute(i++, f.getUri().toString());
        }
        String target = null;
        for (Parameter p : model.getParameters()){
            if ("target".equals(p.getName().getValueAsString())){
                target = p.getValue().toString();
            }
        }        
        newData.renameAttribute(newData.attribute("Class"),target);     
         
        List<Integer> trFieldsIndex = WekaInstancesProcess.getTransformationFieldsAttrIndex(newData, pmmlObject);
        newData = WekaInstancesProcess.removeInstancesAttributes(newData, trFieldsIndex);
        newData = Instances.mergeInstances(compounds, newData);
        return newData;
    }
}

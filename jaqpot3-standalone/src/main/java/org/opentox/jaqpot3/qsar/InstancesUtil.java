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

package org.opentox.jaqpot3.qsar;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import org.opentox.jaqpot3.exception.JaqpotException;
import org.opentox.toxotis.client.VRI;
import org.opentox.toxotis.client.collection.Services;
import org.opentox.toxotis.core.component.Dataset;
import org.opentox.toxotis.core.component.Feature;
import org.opentox.toxotis.core.component.Model;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;

/**
 * Utilities that are absolutely necessary for the manipulation of Instances and
 * preprocessing of them prior to their application on a model for prediction.
 *
 * @author Pantelis Sopasakis
 * @author Charalampos Chomenides
 */
public class InstancesUtil {

    public static Instances sortForModel(Model model, final Instances data, int CompoundURIposition) throws JaqpotException {
        return sortByFeatureList(model.getIndependentFeatures(), data, CompoundURIposition);
    }

    public static Instances sortByFeatureList(List<Feature> features, final Instances data, int CompoundURIposition) throws JaqpotException {
        List<String> list = new ArrayList<String>();
        for (Feature feature : features) {
            list.add(feature.getUri().toString());
        }
        return sortByFeatureAttrList(list, data, CompoundURIposition);
    }

    public static Instances sortByFeatureVriList(List<VRI> features, final Instances data, int CompoundURIposition) throws JaqpotException {
        List<String> list = new ArrayList<String>();
        for (VRI v : features) {
            list.add(v.toString());
        }
        return sortByFeatureAttrList(list, data, CompoundURIposition);
    }

    /**
     * Accepts 
     * @param features
     * @param data
     * @param compoundURIposition
     *      Position where the compound URI should be placed. If set to <code>-1</code>
     *      the compound URI will not be included in the created dataset.
     * @return
     *      A subset of the provided dataset (parameter data in this method) with the
     *      features specified in the provided list with that exact order. The compound
     *      URI feature (string) is placed in the position specified by the parameter
     *      compoundURIposition.
     * @throws JaqpotException
     *      A JaqpotException is thrown with error code {@link ErrorCause#FeatureNotInDataset FeatureNotInDataset}
     *      in case you provide a feature that is not found in the sumbitted Instances.
     */
    public static Instances sortByFeatureAttrList(List<String> features, final Instances data, int compoundURIposition) throws JaqpotException {
        int position = compoundURIposition > features.size() ? features.size() : compoundURIposition;
        if (compoundURIposition != -1) {
            features.add(position, "compound_uri");
        }
        FastVector vector = new FastVector(features.size());
        for (int i = 0; i < features.size(); i++) {
            String feature = features.get(i);
            Attribute attribute = data.attribute(feature);
            if (attribute == null) {
                throw new JaqpotException("The Dataset you provided does not contain feature:" + feature);
            }
            vector.addElement(attribute.copy());
        }
        Instances result = new Instances(data.relationName(), vector, 0);
        Enumeration instances = data.enumerateInstances();
        while (instances.hasMoreElements()) {
            Instance instance = (Instance) instances.nextElement();
            double[] vals = new double[features.size()];
            for (int i = 0; i < features.size(); i++) {
                vals[i] = instance.value(data.attribute(result.attribute(i).name()));
            }
            Instance in = new Instance(1.0, vals);
            result.add(in);
        }
        return result;
    }

//    /**
//     * Main method just for testing purposes.
//     * @param art
//     *      Not used arguments
//     * @throws Exception
//     */
//    public static void main(String... art) throws Exception {
//        Dataset ds = new Dataset(Services.ideaconsult().augment("dataset", "54").addUrlParameter("max", "10")).loadFromRemote();
//        Instances initialDataset = ds.getInstances();
//        System.out.println(initialDataset);
//        List<String> list = new ArrayList<String>();
//        list.add("http://apps.ideaconsult.net:8080/ambit2/feature/22201");
//        list.add("http://apps.ideaconsult.net:8080/ambit2/feature/22199");
//        list.add("http://apps.ideaconsult.net:8080/ambit2/feature/22202");
//        Instances newInst = InstancesUtil.sortByFeatureAttrList(list, initialDataset, -1);
//        System.out.println(newInst);
//    }
}

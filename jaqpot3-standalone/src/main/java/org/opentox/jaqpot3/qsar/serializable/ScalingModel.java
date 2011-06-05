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

package org.opentox.jaqpot3.qsar.serializable;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import org.opentox.toxotis.client.VRI;

/**
 *
 * @author Pantelis Sopasakis
 * @author Charalampos Chomenides
 */
public class ScalingModel implements Serializable {

    // NOTE: HashMap, VRI and Double are all Serializable!    
    private HashMap<VRI, Double> minVals = new HashMap<VRI, Double>();
    private HashMap<VRI, Double> maxVals = new HashMap<VRI, Double>();
    private double min = 0;
    private double max = 1;
    private VRI datasetReference;

    public ScalingModel() {
    }

    public ScalingModel(final double min, final double max) {
        this.min = min;
        this.max = max;
    }

    public Map<VRI, Double> getMaxVals() {
        return maxVals;
    }

    public Map<String, Double> getMaxVals2() {
        if (maxVals == null) {
            return null;
        }
        Map<String, Double> simpleMap = new HashMap<String, Double>();
        if (!maxVals.isEmpty()) {
            Iterator<Entry<VRI, Double>> iterator = maxVals.entrySet().iterator();
            while (iterator.hasNext()) {
                Entry<VRI, Double> entry = iterator.next();
                simpleMap.put(entry.getKey().toString(), entry.getValue());
            }
        }
        return simpleMap;
    }

    public Map<VRI, Double> getMinVals() {
        return minVals;
    }

    public Map<String, Double> getMinVals2() {
        if (minVals == null) {
            return null;
        }
        Map<String, Double> simpleMap = new HashMap<String, Double>();
        if (!minVals.isEmpty()) {
            Iterator<Entry<VRI, Double>> iterator = minVals.entrySet().iterator();
            while (iterator.hasNext()) {
                Entry<VRI, Double> entry = iterator.next();
                simpleMap.put(entry.getKey().toString(), entry.getValue());
            }
        }
        return simpleMap;
    }

    public double getMax() {
        return max;
    }

    public void setMax(double max) {
        this.max = max;
    }

    public double getMin() {
        return min;
    }

    public void setMin(double min) {
        this.min = min;
    }

    public VRI getDatasetReference() {
        return datasetReference;
    }

    public void setDatasetReference(VRI datasetReference) {
        this.datasetReference = datasetReference;
    }
}

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



package org.opentox.jaqpot3.qsar.util;

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
public class SimpleMVHFilter {

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
            o = DatasetFactory.getInstance().createFromArff(out);
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
}

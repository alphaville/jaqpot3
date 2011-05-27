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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import org.opentox.jaqpot3.qsar.predictor.*;

/**
 *
 * @author Pantelis Sopasakis
 * @author Charalampos Chomenides
 */
public class PredictorFinder {

    private static Map<String, Class<? extends IPredictor>> map = new HashMap<String, Class<? extends IPredictor>>();

    static {
        map.put("mlr", WekaPredictor.class);
        map.put("svm", WekaPredictor.class);
        map.put("leverages", LeveragesPredictor.class);
        map.put("fastRbfNn", FastRbfNnPredictor.class);
        map.put("scaling", ScalingPredictor.class);
        map.put("mvh", MissingValueFilterPredictor.class);
    }

    public static Map<String, Class<? extends IPredictor>> getMapping() {
        return map;
    }

    public static IPredictor getPredictor(String algorithmName) {
        Class<? extends IPredictor> clazz = map.get(algorithmName);
        if (clazz == null) {
            return null;
        }
        try {
            Constructor<? extends IPredictor> c = clazz.getConstructor();
            IPredictor predictor = c.newInstance();
            return predictor;
        } catch (InstantiationException ex) {
            throw new RuntimeException(ex);
        } catch (IllegalAccessException ex) {
            throw new RuntimeException(ex);
        } catch (IllegalArgumentException ex) {
            throw new RuntimeException(ex);
        } catch (InvocationTargetException ex) {
            throw new RuntimeException(ex);
        } catch (NoSuchMethodException ex) {
            throw new RuntimeException(ex);
        } catch (SecurityException ex) {
            throw new RuntimeException(ex);
        }
    }
}

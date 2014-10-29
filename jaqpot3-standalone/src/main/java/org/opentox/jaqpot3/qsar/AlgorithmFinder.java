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
import org.opentox.jaqpot3.qsar.trainer.*;

/**
 *
 * @author Pantelis Sopasakis
 * @author Charalampos Chomenides
 */
public class AlgorithmFinder {

    private static final Map<String, Class<? extends IParametrizableAlgorithm>> map = new HashMap<String, Class<? extends IParametrizableAlgorithm>>();

    static {
        map.put("mvh", MissingValueFilter.class);
        map.put("mlr", MlrRegression.class);
        map.put("svm", SvmRegression.class);
        map.put("leverages", LeveragesTrainer.class);
        map.put("pls", PLSTrainer.class);
        map.put("fastRbfNn", FastRbfNnTrainer.class);
        map.put("scaling", ScalingFilter.class);
        map.put("modelBundler", ModelBundlerTrainer.class);
    }

    public static Map<String, Class<? extends IParametrizableAlgorithm>> getMapping() {
        return map;
    }

    public static IParametrizableAlgorithm getAlgorithm(String algorithmName) {
        Class<? extends IParametrizableAlgorithm> clazz = map.get(algorithmName);
        if (clazz == null) {
            return null;
        }
        try {
            Constructor<? extends IParametrizableAlgorithm> c = clazz.getConstructor();
            IParametrizableAlgorithm algorithm = c.newInstance();
            return algorithm;
        } catch (final InstantiationException ex) {
            throw new IllegalArgumentException("Instantiation Failed", ex);
        } catch (final IllegalAccessException ex) {
            throw new IllegalArgumentException("Access to object denied", ex);
        } catch (final InvocationTargetException ex) {
            throw new IllegalArgumentException("Invocation exception", ex);
        } catch (final NoSuchMethodException ex) {
            throw new IllegalArgumentException("Method not found", ex);
        } catch (final SecurityException ex) {
            throw new IllegalArgumentException("Security Issue", ex);
        }
    }
}

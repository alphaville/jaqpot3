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

    private static Map<String, Class<? extends IParametrizableAlgorithm>> map = new HashMap<String, Class<? extends IParametrizableAlgorithm>>();

    static {
//        map.put("cleanup", CleanupFilter.class);
        map.put("mvh", MissingValueFilter.class);
        map.put("mlr", MlrRegression.class);
        map.put("svm", SvmRegression.class);
        map.put("leverages", LeveragesTrainer.class);
        map.put("pls", PLSTrainer.class);
//        map.put("svmFilter", SvmAttrSelFilter.class);
        map.put("fastRbfNn", FastRbfNnTrainer.class);
        map.put("scaling", ScalingFilter.class);
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

package org.opentox.jaqpot3.qsar;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import org.opentox.jaqpot3.qsar.doa.LeveragesPredictor;
import org.opentox.jaqpot3.qsar.filter.ScalingPredictor;
import org.opentox.jaqpot3.qsar.regression.FastRbfNnPredictor;
import org.opentox.jaqpot3.qsar.regression.WekaPredictor;

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
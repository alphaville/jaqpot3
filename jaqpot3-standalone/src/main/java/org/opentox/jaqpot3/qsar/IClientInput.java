package org.opentox.jaqpot3.qsar;

import java.util.Map;

/**
 * Input provided by a client or end-user.
 *
 * @author Pantelis Sopasakis
 * @author Charalampos Chomenides
 */
public interface IClientInput {

    String getFirstValue(String name);

    String getFirstValue(String name, String defaultValue);

    String getFirstValue(String name, boolean ignoreCase);

    String getFirstValue(String name, boolean ignoreCase, String defaultValue);

    String getMatrixString();

    String getQueryString();

    String getValues(String name);

    Map<String, String> getValuesMap();

    String[] getValuesArray(String name);

    String[] getValuesArray(String name, boolean ignoreCase);
}

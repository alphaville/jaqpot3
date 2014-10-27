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


package org.opentox.jaqpot3.www;

import org.opentox.jaqpot3.util.Configuration;

/**
 *
 * @author Pantelis Sopasakis
 * @author Charalampos Chomenides
 */
public final class URITemplate {

    private String base;
    private String primaryKey;
    private String metaKey;
    private static final String separator = "/";
    private static final String urlsuffix = Configuration.getStringProperty("url.suffix", "");

    public URITemplate(String base, String primaryKey, String metaKey) {
        this.base = base;
        this.primaryKey = primaryKey;
        this.metaKey = metaKey;
    }

    public String getBase() {
        return base;
    }

    public void setBase(String base) {
        this.base = base;
    }

    public String getMetaKey() {
        return metaKey;
    }

    public void setMetaKey(String metaKey) {
        this.metaKey = metaKey;
    }

    public String getPrimaryKey() {
        return primaryKey;
    }

    public void setPrimaryKey(String primaryKey) {
        this.primaryKey = primaryKey;
    }

    @Override
    public String toString() {
        String string = separator;
        if (urlsuffix != null && !urlsuffix.isEmpty()) {
            string += urlsuffix + separator;
        }
        if (base != null) {
            string += base;
            if (primaryKey != null) {
                string += separator + inCurledBrackets(primaryKey);
                if (metaKey != null) {
                    string += separator + inCurledBrackets(metaKey);
                }
            }
        }
        return string;
    }

    private String inCurledBrackets(String in) {
        return "{" + in + "}";
    }

    public static URITemplate primarySubtemplateOf(URITemplate other, String primaryKey) {
        return new URITemplate(other.getBase(), primaryKey, null);
    }

    public static URITemplate secondarySubTemplateOf(URITemplate other, String metaKey) {
        return new URITemplate(other.getBase(), other.getPrimaryKey(), metaKey);
    }
}

/*
 *
 * YAQP - Yet Another QSAR Project:
 * Machine Learning algorithms designed for the prediction of toxicological
 * features of chemical compounds become available on the Web. Yaqp is developed
 * under OpenTox (http://opentox.org) which is an FP7-funded EU research project.
 * This project was developed at the Automatic Control Lab in the Chemical Engineering
 * School of the National Technical University of Athens. Please read README for more
 * information.
 *
 * Copyright (C) 2009-2010 Pantelis Sopasakis & Charalampos Chomenides
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
        String suffix = Configuration.getStringProperty("url.suffix");
        String string = separator;
        if (suffix == null || (suffix != null && suffix.isEmpty())) {
        } else {
//            string += suffix + "/";
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

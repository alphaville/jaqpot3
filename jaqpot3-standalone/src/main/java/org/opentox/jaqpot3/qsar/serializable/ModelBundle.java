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
import java.util.ArrayList;
import java.util.Collection;

/**
 *
 * @author Pantelis Sopasakis
 */
public class ModelBundle implements Serializable {

    private static final long serialVersionUID = 77274120800111457L;
    private ArrayList<String> modelUris = new ArrayList<String>();

    public ModelBundle() {
    }

    public boolean addModel(String e) {
        return modelUris.add(e);
    }

    public boolean addModels(Collection<? extends String> c) {
        return modelUris.addAll(c);
    }

    public void addModel(int index, String element) {
        modelUris.add(index, element);
    }

    public void setModelUris(ArrayList<String> modelUris) {
        this.modelUris = modelUris;
    }

    public ArrayList<String> getModelUris() {
        return modelUris;
    }
}

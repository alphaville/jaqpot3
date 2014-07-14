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

import java.util.List;
import org.opentox.jaqpot3.qsar.IClientInput;
import org.restlet.data.CharacterSet;
import org.restlet.data.Form;
import org.restlet.data.Parameter;
import org.restlet.representation.Representation;

/**
 *
 * @author Pantelis Sopasakis
 * @author Charalampos Chomenides
 */
public class ClientInput extends ClientUploadInput {

    public ClientInput(String parametersString, CharacterSet characterSet, char separator) {
        super(parametersString, characterSet, separator);
    }

    public ClientInput(String queryString, CharacterSet characterSet) {
        super(queryString, characterSet);
    }

    public ClientInput(String parametersString, char separator) {
        super(parametersString, separator);
    }

    public ClientInput(String queryString) {
        super(queryString);
    }

    public ClientInput(Representation webForm) {
        super(webForm);
    }

    public ClientInput(List<Parameter> delegate) {
        super(delegate);
    }

    public ClientInput(int initialCapacity) {
        super(initialCapacity);
    }

    public ClientInput() {
    }

}

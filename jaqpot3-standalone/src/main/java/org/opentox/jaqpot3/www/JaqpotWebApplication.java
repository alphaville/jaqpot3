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

import org.restlet.Application;
import org.restlet.Context;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.Language;
import org.restlet.ext.wadl.ApplicationInfo;
import org.restlet.ext.wadl.DocumentationInfo;
import org.restlet.ext.wadl.WadlApplication;
import org.restlet.routing.Router;
import org.restlet.routing.Template;

/**
 *
 * @author Pantelis Sopasakis
 * @author Charalampos Chomenides
 */
public class JaqpotWebApplication extends WadlApplication {

    public JaqpotWebApplication() {
        super();
        setName("JaQPOT");
        setDescription("JaQPOT Training and Prediction Services");
        setOwner("part of the OpenTox project - http://opentox.org");
        setAuthor("kinkyDesign");
    }


    @Override
    public ApplicationInfo getApplicationInfo(Request request, Response response) {
        ApplicationInfo result = super.getApplicationInfo(request, response);

        DocumentationInfo docInfo = new DocumentationInfo(getName());
        docInfo.setTitle(getName());
        docInfo.setLanguage(Language.ENGLISH);
        docInfo.setTextContent(getDescription());
        result.setDocumentation(docInfo);

        return result;
    }

    public ApplicationInfo getAppInfo(Request request, Response response) {
        return getApplicationInfo(request, response);
    }

    final static public class YaqpRouter extends Router {

        public YaqpRouter(Context context) {
            super(context);
            setDefaultMatchingMode(Template.MODE_STARTS_WITH);
            setRoutingMode(Router.MODE_BEST_MATCH);

        }
    }
}

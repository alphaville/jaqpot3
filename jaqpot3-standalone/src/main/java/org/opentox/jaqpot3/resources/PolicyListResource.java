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


package org.opentox.jaqpot3.resources;

import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.opentox.jaqpot3.exception.JaqpotException;
import org.opentox.jaqpot3.resources.publish.Representer;
import org.opentox.jaqpot3.resources.publish.UriListPublishable;
import org.opentox.jaqpot3.util.Configuration;
import org.opentox.jaqpot3.www.URITemplate;
import org.opentox.toxotis.exceptions.impl.ServiceInvocationException;
import org.opentox.toxotis.util.aa.policy.PolicyManager;
import org.restlet.data.MediaType;
import org.restlet.data.Method;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.representation.Variant;
import org.restlet.resource.ResourceException;

/**
 *
 * @author Pantelis Sopasakis
 */
public class PolicyListResource extends JaqpotResource {

    public static final URITemplate template = new URITemplate("policy", null, null);

    @Override
    protected void doInit() throws ResourceException {
        super.doInit();
        initialize(
                MediaType.TEXT_HTML,
                MediaType.APPLICATION_WADL,
                MediaType.TEXT_URI_LIST);

        Set<Method> allowedMethods = getAllowedMethods();
        allowedMethods.add(Method.GET);
        allowedMethods.add(Method.OPTIONS);
        setAllowedMethods(allowedMethods);
        parseStandardParameters();
        updatePrimaryId(template);
        super.doInit();
    }

    @Override
    public Representation get(Variant variant) {
        if (getUserToken() == null) {
            toggleSeeOther(Configuration.getBaseUri().augment("login").
                    addUrlParameter("redirect", getCurrentVRI().toString()).toString());
            return new StringRepresentation("");
        }
        try {
            UriListPublishable publishable = new UriListPublishable(PolicyManager.listPolicyUris(null, getUserToken()));
            publishable.setBaseUri(getCurrentVRI());
            publishable.setHeading("Policies for user " + getUser().getUid());
            publishable.setMediaType(variant.getMediaType());
            Representer representer = new Representer();
            return representer.process(publishable);
        } catch (JaqpotException ex) {
            Logger.getLogger(UserQuotaResource.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ServiceInvocationException ex) {
            Logger.getLogger(UserQuotaResource.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }
}

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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.opentox.jaqpot3.exception.JaqpotException;
import org.opentox.jaqpot3.resources.collections.Algorithms;
import org.opentox.jaqpot3.resources.publish.Publisher;
import org.opentox.jaqpot3.resources.publish.Representer;
import org.opentox.jaqpot3.resources.publish.UriListPublishable;
import org.opentox.jaqpot3.www.URITemplate;
import org.opentox.toxotis.core.IRestOperation;
import org.opentox.toxotis.core.OTComponent;
import org.opentox.toxotis.core.component.Algorithm;
import org.opentox.toxotis.core.component.DummyComponent;
import org.opentox.toxotis.core.component.HttpMediatype;
import org.opentox.toxotis.core.component.HttpStatus;
import org.opentox.toxotis.core.component.RestOperation;
import org.opentox.toxotis.core.component.ServiceRestDocumentation;
import org.opentox.toxotis.ontology.collection.HttpMethods.MethodsEnum;
import org.opentox.toxotis.ontology.collection.OTRestClasses;
import org.opentox.toxotis.ontology.impl.MetaInfoImpl;
import org.restlet.data.MediaType;
import org.restlet.data.Method;
import org.restlet.data.ReferenceList;
import org.restlet.data.Status;
import org.restlet.ext.wadl.MethodInfo;
import org.restlet.ext.wadl.RepresentationInfo;
import org.restlet.representation.Representation;
import org.restlet.representation.Variant;
import org.restlet.resource.ResourceException;

/**
 *
 * @author Pantelis Sopasakis
 * @author Charalampos Chomenides
 */
public class AlgorithmsResource extends JaqpotResource {

    public static final URITemplate template = new URITemplate("algorithm", null, null);
    private static List<String> algorithmUris = null;

    private List<String> getAlgorithmUris() {
        if (algorithmUris == null) {
            algorithmUris = new ArrayList<String>();
            Set<Algorithm> all = Algorithms.getAll();
            for (Object algorithm : all) {
                algorithmUris.add(((OTComponent) algorithm).getUri().toString());
            }
        }
        return algorithmUris;
    }

    @Override
    protected void doInit() throws ResourceException {
        super.doInit();
        initialize(
                MediaType.TEXT_HTML,
                MediaType.APPLICATION_RDF_XML,
                MediaType.APPLICATION_WADL,
                MediaType.APPLICATION_RDF_TURTLE,
                MediaType.TEXT_URI_LIST);

        Set<Method> allowedMethods = getAllowedMethods();
        allowedMethods.add(Method.GET);
        allowedMethods.add(Method.OPTIONS);
        setAllowedMethods(allowedMethods);

    }

    @Override
    protected Representation get(Variant variant) throws ResourceException {
        MediaType media = variant.getMediaType();
        UriListPublishable publishable = new UriListPublishable(getAlgorithmUris(), media);
        Representer representer = new Representer(true);
        try {
            return representer.process(publishable);
        } catch (JaqpotException ex) {
            return errorReport(ex, "PublicationException", "Cannot create representation - unexpected condition", media, true);
        }

    }

    @Override
    protected Representation describe() {
        setTitle("Algorithm Service");
        return super.describe();
    }

    @Override
    protected ServiceRestDocumentation getServiceDocumentation(Variant variant) {
        IRestOperation restOperation = new RestOperation();
        restOperation.addRestClasses(OTRestClasses.GET_Algorithm(), OTRestClasses.OperationResultAlgorithm());
        restOperation.getMeta().addTitle("Rest interface documentation for the resource /algorithm");
        restOperation.setMethod(MethodsEnum.GET);
        restOperation.addHttpStatusCodes(
                new HttpStatus(OTRestClasses.STATUS_200()).setMeta(new MetaInfoImpl().addDescription("The GET method succeeds and "
                + "a representation of the algorithm is returned to the client in the prefered MIME type")),
                new HttpStatus(OTRestClasses.STATUS_401()).setMeta(new MetaInfoImpl().addDescription("The user is authenticated but not authorized to access the "
                + "underlying resource")),
                new HttpStatus(OTRestClasses.STATUS_403()).setMeta(new MetaInfoImpl().addDescription("Forbidden action and access")),
                new HttpStatus(OTRestClasses.STATUS_500()).setMeta(new MetaInfoImpl().addDescription("The GET method fails and returns an error status "
                + "code 500 in case some unexplicable error inhibits the algorithm to be accessed.")));
        restOperation.addMediaTypes(
                new HttpMediatype().addOntologicalClasses(OTRestClasses.mime_rdf_xml()),
                new HttpMediatype().addOntologicalClasses(OTRestClasses.mime_rdf_turtle()),
                new HttpMediatype().addOntologicalClasses(OTRestClasses.mime_rdf_n3()),
                new HttpMediatype().addOntologicalClasses(OTRestClasses.mime_text_html()),
                new HttpMediatype().addOntologicalClasses(OTRestClasses.mime_text_uri_list()));

        ServiceRestDocumentation doc = new ServiceRestDocumentation(new DummyComponent(getCurrentVRINoQuery()));
        doc.addRestOperations(restOperation);
        return doc;
    }

    // <editor-fold defaultstate="collapsed" desc="For WADL use (describe GET)">
    @Override
    protected void describeGet(MethodInfo info) {
        super.describeGet(info);
        info.setDocumentation("Retrieve a list of Training and Prediction Algorithms supported by YAQP.");
        ArrayList<RepresentationInfo> repList = new ArrayList<RepresentationInfo>();
        RepresentationInfo rdfRep = new RepresentationInfo(MediaType.APPLICATION_RDF_XML);
        rdfRep.setDocumentation("Supported Algorithms in RDF format");
        RepresentationInfo urilistRep = new RepresentationInfo(MediaType.TEXT_URI_LIST);
        urilistRep.setDocumentation("Supported Algorithms in URI-List format");
        repList.add(rdfRep);
        repList.add(urilistRep);

        info.getResponse().setRepresentations(repList);
    }// </editor-fold>
}

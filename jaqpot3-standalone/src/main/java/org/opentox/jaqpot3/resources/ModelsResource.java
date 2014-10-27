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
import org.opentox.jaqpot3.exception.JaqpotException;
import org.opentox.jaqpot3.resources.publish.DbListStreamPublisher;
import org.opentox.jaqpot3.util.Configuration;
import org.opentox.jaqpot3.www.URITemplate;
import org.opentox.toxotis.core.IRestOperation;
import org.opentox.toxotis.core.component.DummyComponent;
import org.opentox.toxotis.core.component.HttpMediatype;
import org.opentox.toxotis.core.component.HttpParameter;
import org.opentox.toxotis.core.component.HttpStatus;
import org.opentox.toxotis.core.component.RestOperation;
import org.opentox.toxotis.core.component.ServiceRestDocumentation;
import org.opentox.toxotis.database.engine.model.ListModel;
import org.opentox.toxotis.ontology.collection.HttpMethods.MethodsEnum;
import org.opentox.toxotis.ontology.collection.OTRestClasses;
import org.opentox.toxotis.ontology.impl.MetaInfoImpl;
import org.restlet.data.MediaType;
import org.restlet.data.Method;
import org.restlet.representation.Representation;
import org.restlet.representation.Variant;
import org.restlet.resource.ResourceException;

/**
 *
 * @author Pantelis Sopasakis
 * @author Charalampos Chomenides
 */
public class ModelsResource extends JaqpotResource {

    public static final URITemplate template = new URITemplate("model", null, null);
    private org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ModelsResource.class);
    private String creator;
    private String dataset_uri;
    private String dependent_feature;
    private String algorithm_uri;
    private String algorithm;

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
        parseStandardParameters();
        creator = parseParameter("creator");
        dataset_uri = parseParameter("dataset_uri");
        dependent_feature = parseParameter("dependent_feature");
        algorithm_uri = parseParameter("algorithm_uri");
        algorithm = parseParameter("algorithm");
    }

    @Override
    protected Representation get(Variant variant) throws ResourceException {

        if (acceptString != null) {
            variant.setMediaType(MediaType.valueOf(acceptString));
        }

        ListModel lister = new ListModel();
        if (getSqlQuery() != null) {
            lister.setWhere(getSqlQuery());
        }
        DbListStreamPublisher publisher = new DbListStreamPublisher();
        publisher.setMedia(variant.getMediaType());
        publisher.setBaseUri(Configuration.getBaseUri().augment("model"));
        try {
            return publisher.process(lister);
        } catch (JaqpotException ex) {
            return errorReport(ex, "DbError", "Error while getting data from the DB",
                    variant.getMediaType(), false);
        }

    }

    private String getSqlQuery() {
        StringBuilder query = new StringBuilder();
        if (this.creator != null) {
            query.append("createdBy LIKE '");
            query.append(creator);
            query.append("@opensso.in-silico.ch'");
        }
        if (this.algorithm != null) {
            if (query.length() > 0) {
                query.append(" AND ");
            }
            query.append("algorithm LIKE '%");
            query.append(this.algorithm);
            query.append("'");
        }
        String q = query.toString();
        return q.isEmpty() ? null : q;
    }

    @Override
    protected ServiceRestDocumentation getServiceDocumentation(Variant variant) {
        ServiceRestDocumentation doc = new ServiceRestDocumentation(new DummyComponent(getCurrentVRINoQuery()));
        IRestOperation get = new RestOperation();
        get.setMethod(MethodsEnum.GET);
        get.addHttpStatusCodes(
                new HttpStatus(OTRestClasses.status200()).setMeta(new MetaInfoImpl().addTitle("Success").
                addDescription("Successful retrieval of the list of models.")),
                new HttpStatus(OTRestClasses.status403()),
                new HttpStatus(OTRestClasses.status401()));
        get.addOntologicalClasses(OTRestClasses.getModel());
        get.addMediaTypes(new HttpMediatype().addOntologicalClasses(OTRestClasses.mimeTextUriList()));

        get.addHttpParameters(
                new HttpParameter().addInputParamClass(OTRestClasses.urlParameter()).
                setOpentoxParameter(false).setParamName("max").setMeta(new MetaInfoImpl().addDescription("The maximum number of results to be displayed").
                addDescription("The page size when paging is applied on the number of results")).setParamOptional(true),
                new HttpParameter().addInputParamClass(OTRestClasses.urlParameter()).
                setOpentoxParameter(false).setParamName("creator").setMeta(new MetaInfoImpl().addDescription("Display only the models created by a certain creator "
                + "identified by its UID (like user%40opensso.in-silico.ch)")).setParamOptional(true),
                new HttpParameter().addInputParamClass(OTRestClasses.urlParameter()).
                setOpentoxParameter(false).setParamName("dataset_uri").setMeta(new MetaInfoImpl().addDescription("Display only those models that were "
                + "trained using the speicified dataset (provide its URI)")).setParamOptional(true),
                new HttpParameter().addInputParamClass(OTRestClasses.urlParameter()).
                setOpentoxParameter(false).setParamName("dependent_feature").setMeta(new MetaInfoImpl().addDescription("Display a list only containing the "
                + "models that have the provided dependent feature")).setParamOptional(true),
                new HttpParameter().addInputParamClass(OTRestClasses.urlParameter()).
                setOpentoxParameter(false).setParamName("algorithm").setMeta(new MetaInfoImpl().addDescription("List of models trained with the "
                + "given algorithm (provide the algorithm id)")).setParamOptional(true),
                new HttpParameter().addInputParamClass(OTRestClasses.urlParameter()).
                setOpentoxParameter(false).setParamName("algorithm_uri").setMeta(new MetaInfoImpl().addDescription("List of models that were trained "
                + "using the specified algorithm (provide the algorithm's URI)")).setParamOptional(true),
                new HttpParameter().addInputParamClass(OTRestClasses.urlParameter()).
                setOpentoxParameter(false).setParamName("page").setMeta(new MetaInfoImpl().addDescription("The index number of the page. Used for paging of the"
                + "listed models.")).setParamOptional(true));
        get.addHttpParameters(
                new HttpParameter().addInputParamClass(OTRestClasses.header()).setParamName("Authorization").setParamOptional(true),
                new HttpParameter().addInputParamClass(OTRestClasses.header()).setParamName("Policy").setParamOptional(true));
        get.setProtectedResource(true);

        IRestOperation delete = new RestOperation();
        delete.addHttpStatusCodes(
                new HttpStatus(OTRestClasses.status200()).setMeta(new MetaInfoImpl().addTitle("Success").
                addDescription("All model resources (that were created by the user that applies the DELETE request) were successfully deleted")),
                new HttpStatus(OTRestClasses.status403()),
                new HttpStatus(OTRestClasses.status401()));
        delete.setMethod(MethodsEnum.DELETE);
        delete.addOntologicalClasses(OTRestClasses.deleteModel(), OTRestClasses.operationNoResult());
        delete.getMeta().addDescription("Deletes all model resources that were created by the user that applies the DELETE operation");
        delete.addHttpParameters(
                new HttpParameter().addInputParamClass(OTRestClasses.header()).setParamName("Authorization").setParamOptional(true),
                new HttpParameter().addInputParamClass(OTRestClasses.header()).setParamName("Policy").setParamOptional(true));
        delete.setProtectedResource(true);

        doc.addRestOperations(get, delete);
        return doc;
    }
}

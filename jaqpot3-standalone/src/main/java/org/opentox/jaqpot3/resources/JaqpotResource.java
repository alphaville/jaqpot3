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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import org.opentox.jaqpot3.exception.JaqpotException;
import org.opentox.jaqpot3.resources.publish.Publisher;
import org.opentox.jaqpot3.util.Configuration;
import org.opentox.jaqpot3.www.URITemplate;
import org.opentox.toxotis.client.VRI;
import org.opentox.toxotis.core.component.DummyComponent;
import org.opentox.toxotis.core.component.ErrorReport;
import org.opentox.toxotis.core.component.ServiceRestDocumentation;
import org.opentox.toxotis.exceptions.impl.ServiceInvocationException;
import org.opentox.toxotis.exceptions.impl.ToxOtisException;
import org.opentox.toxotis.util.aa.AuthenticationToken;
import org.opentox.toxotis.core.component.User;
import org.opentox.toxotis.ontology.collection.OTClasses;
import org.restlet.data.CharacterSet;
import org.restlet.data.Digest;
import org.restlet.data.Form;
import org.restlet.data.Language;
import org.restlet.data.MediaType;
import org.restlet.data.Method;
import org.restlet.data.Reference;
import org.restlet.data.Status;
import org.restlet.ext.wadl.WadlServerResource;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.representation.Variant;
import org.restlet.resource.ResourceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Pantelis Sopasakis
 * @author Charalampos Chomenides
 */
public abstract class JaqpotResource extends WadlServerResource {

    private Logger logger = LoggerFactory.getLogger(JaqpotResource.class);
    protected static final String NEWLINE = "\n";
    protected static final String _500_ =
            "We apologize for the inconvenience - Unknown Exception Caught. "
            + "Information about this exception are logged and "
            + "the administrators will be notified to fix this as soon as possible. If the problem remains, "
            + "please contant the service administrators at chvng(a)mail(d0t]ntua{D0t]gr ." + NEWLINE;
    protected static final String _DEAD_DB_ =
            "Database server seems to be dead for the moment. "
            + "A monitoring service checks for database connection flaws every 15 minutes and takes actions to restore it. "
            + "Please try again later or contact the server administrators at chvng=atT=mail=d0T=ntua=d0t=gr if the problem is not solved automatically "
            + "in a while" + NEWLINE;
    protected String acceptString = null;
    protected String max = null;
    protected String page = null;
    protected String primaryId;
    protected String secondaryId;
    private static Set<MediaType> RDF_RELATED_MEDIATYPES;

    static {
        RDF_RELATED_MEDIATYPES = new HashSet<MediaType>();
        RDF_RELATED_MEDIATYPES.add(MediaType.APPLICATION_RDF_XML);
        RDF_RELATED_MEDIATYPES.add(MediaType.APPLICATION_RDF_TURTLE);
        RDF_RELATED_MEDIATYPES.add(MediaType.TEXT_RDF_N3);
        RDF_RELATED_MEDIATYPES.add(MediaType.TEXT_RDF_NTRIPLES);
        RDF_RELATED_MEDIATYPES.add(MediaType.register("application/rdf+xml-abbrev", NEWLINE));
    }

    protected static boolean isRdfRelated(MediaType mediaType) {
        return RDF_RELATED_MEDIATYPES.contains(mediaType);
    }

    protected User getUser() {
        AuthenticationToken token = this.getUserToken();
        if (token == null) {
            return null;
        }
        try {
            return token.getUser();
        } catch (ServiceInvocationException ex) {
            java.util.logging.Logger.getLogger(JaqpotResource.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ToxOtisException ex) {
            java.util.logging.Logger.getLogger(JaqpotResource.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }
    
    protected VRI getCurrentVRI() {
        try {
            return new VRI(getReference().toString());
        } catch (URISyntaxException ex) {
            logger.error("Illegal URI", ex);
            throw new RuntimeException("", ex);
        }
    }

    protected VRI getCurrentVRINoQuery() {
        try {
            return new VRI(new VRI(getReference().toString()).getStringNoQuery());
        } catch (URISyntaxException ex) {
            logger.error("Illegal URI");
            throw new RuntimeException("", ex);
        }
    }

    protected String parseParameter(String paramName) {
        return getRequest().getResourceRef().getQueryAsForm().getFirstValue(paramName);
    }

    protected void parseStandardParameters() {
        acceptString = parseParameter("accept");
        max = parseParameter("max");
        page = parseParameter("page");

    }

    protected void updatePrimaryId(URITemplate template) {
        String primaryKeyTemplate = template.getPrimaryKey();
        if (primaryKeyTemplate != null) {
            Object atts = getRequest().getAttributes().get(primaryKeyTemplate);
            if (atts != null) {
                primaryId = Reference.decode(atts.toString()).trim();
            }
        }
    }

    protected void updateSecondaryId(URITemplate template) {
        String metaKeyTemplate = template.getMetaKey();
        if (metaKeyTemplate != null) {
            Object atts = getRequest().getAttributes().get(metaKeyTemplate);
            if (atts != null) {
                secondaryId = Reference.decode(atts.toString()).trim();
            }
        }
    }

    public void initialize(Collection<MediaType> supportedMedia) {
        super.doInit();
        for (MediaType m : supportedMedia) {
            getVariants().add(new Variant(m));
        }
    }

    public void initialize(MediaType... supportedMedia) {
        super.doInit();
        for (MediaType m : supportedMedia) {
            getVariants().add(new Variant(m));
        }

    }

    protected AuthenticationToken getUserToken() {
        Form headerParams = (Form) getRequest().getAttributes().get("org.restlet.http.headers");
        String token = headerParams.getFirstValue("subjectid");
        if (token == null) {
            token = getCookies().getValues("subjectid");
            if (token == null) {

                String userTokenInUrl = getReference().getQueryAsForm().getFirstValue("subjectid");
                if (userTokenInUrl != null) {
                    try {
                        token = URLDecoder.decode(userTokenInUrl, "UTF-8");
                    } catch (final UnsupportedEncodingException ex) {
                        throw new RuntimeException(ex);
                    }
                }
                if (token == null) {
                    return null;
                }
            }
        }
        AuthenticationToken userToken = new AuthenticationToken(token);
        return userToken;
    }

    /**
     * By default post is not allowed.
     * @param entity
     * @param variant
     * @return
     * @throws ResourceException
     */
    @Override
    protected Representation post(Representation entity, Variant variant) throws ResourceException {
        toggleMethodNotAllowed();
        String message = "POST method is not allowed on this URI. Please check the API documentation and do not repeat this request.\n";
        return sendMessage(message);
    }

    @Override
    protected Representation delete(Variant variant) throws ResourceException {
        toggleMethodNotAllowed();
        String message = "DELETE method is not allowed on this URI. Please check the API documentation and do not repeat this request.\n";
        return sendMessage(message);
    }

    @Override
    protected Representation put(Representation entity, Variant variant) throws ResourceException {
        toggleMethodNotAllowed();
        String message = "PUT method is not allowed on this URI. Please check the API documentation and do not repeat this request.\n";
        return sendMessage(message);
    }

    /**
     * Return a simple message to the client in text format. This is intended to
     * be used for simple error reports.
     * @param message
     *      The message to be returned to the client in plain text format. The character
     *      encoding supported is UTF-8 while the response language is set to {@link Language#ENGLISH }
     * @return
     *      The string representation corresponding to the provided message. The character set
     *      and the language are set as mentioned above.
     */
    protected Representation sendMessage(String message) {
        Representation rep = new StringRepresentation(message, MediaType.TEXT_PLAIN, Language.ENGLISH, CharacterSet.UTF_8);
        rep.setDigest(new Digest(Digest.ALGORITHM_MD5, message.getBytes()));
        return rep;
    }

    /**
     * The method the client applied on the server is not allowed.
     */
    private void toggleMethodNotAllowed() {
        getResponse().setStatus(Status.CLIENT_ERROR_METHOD_NOT_ALLOWED);
    }

    /**
     * Bad request by the client.
     */
    protected void toggleBadRequest() {
        getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
    }

    protected void toggleServerOverloaded() {
        getResponse().setStatus(Status.SERVER_ERROR_SERVICE_UNAVAILABLE);
    }

    /**
     * Server is overloaded
     * @param minutes_after
     *      After how many minutes the client should retry the request
     */
    protected void toggleServerOverloaded(int minutes_after) {
        getResponse().setStatus(Status.SERVER_ERROR_SERVICE_UNAVAILABLE);
        getResponse().setRetryAfter(new java.util.Date(System.currentTimeMillis() + minutes_after * 60L * 1000L));
    }

    /**
     * Error 507
     */
    protected void toggleInsufficientStorage() {
        getResponse().setStatus(Status.SERVER_ERROR_INSUFFICIENT_STORAGE);
    }

    /**
     * Forbidden Operation
     */
    protected void toggleForbidden() {
        getResponse().setStatus(Status.CLIENT_ERROR_FORBIDDEN);
    }

    protected void toggleAccepted() {
        getResponse().setStatus(Status.SUCCESS_ACCEPTED);
    }

    protected void toggleUnauthorized() {
        getResponse().setStatus(Status.CLIENT_ERROR_UNAUTHORIZED);
    }

    /**
     * The requested resource was not found on the server.
     */
    protected void toggleNotFound() {
        getResponse().setStatus(Status.CLIENT_ERROR_NOT_FOUND);
    }

    /**
     * The server encountered an internal error.
     */
    protected void toggleServerError() {
        getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
    }

    /**
     * The request performed by the client succeded.
     */
    protected void toggleSuccess() {
        getResponse().setStatus(Status.SUCCESS_OK);
    }

    protected void toggleCreated() {
        getResponse().setStatus(Status.SUCCESS_CREATED);
    }

    /**
     * Just set the status code to 303.
     */
    protected void toggleSeeOther() {
        getResponse().setStatus(Status.REDIRECTION_SEE_OTHER);
    }

    /**
     * Sets the status code to 303 and modifies the HTTP Headers properly. Adds the following
     * fields in the Header:
     * <ul>
     * <li>Location: {URI of the new location}</li>
     * <li>Pragma: no-cache</li>
     * <li>Refresh: 60; url={New location}</li>
     * </ul>
     * @param newLocation
     *          URI of the new location.
     */
    protected void toggleSeeOther(String newLocation) {
        Form responseHeaders = (Form) getResponse().getAttributes().get("org.restlet.http.headers");
        if (responseHeaders == null) {
            responseHeaders = new Form();
            getResponse().getAttributes().put("org.restlet.http.headers", responseHeaders);
        }
        responseHeaders.add("Cache-Control", "no-store, no-cache, must-revalidate"); //HTTP 1.1
        responseHeaders.add("Cache-Control", "post-check=0, pre-check=0");
        responseHeaders.add("Pragma", "no-cache"); //HTTP 1.0
        responseHeaders.add("Expires", "0"); //prevents caching at the proxy server
        responseHeaders.add("Refresh", String.format("60; url=%s", newLocation));
        setLocationRef(newLocation);
        getResponse().setStatus(Status.REDIRECTION_SEE_OTHER);
    }

    /**
     * The server received an error from a remote service.
     */
    protected void toggleRemoteError() {
        getResponse().setStatus(Status.SERVER_ERROR_BAD_GATEWAY);
    }

    protected Representation errorReport(Throwable ex, String errorCode, String message, MediaType media, boolean doAutoLogging) {
        if (!MediaType.APPLICATION_RDF_XML.equals(media) && !MediaType.APPLICATION_RDF_TURTLE.equals(media) && !MediaType.TEXT_RDF_N3.equals(media) && !MediaType.TEXT_RDF_N3.equals(media) && !MediaType.TEXT_HTML.equals(media)) {
            media = MediaType.APPLICATION_RDF_XML;// Default!
        }
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        ex.printStackTrace(pw);

        Throwable currentThrowable = ex.getCause();
        while (currentThrowable!=null){
            pw.append("\nPrevious Stack Trace...\n");
            currentThrowable.printStackTrace(pw);
            currentThrowable = currentThrowable.getCause();
        }

        String details = sw.toString();

        if (doAutoLogging) {
            logger.warn(message != null ? (message + details != null ? NEWLINE : "") : "" + details != null ? details : "");
        }
        String actor = Configuration.BASE_URI;
        if (getStatus().getCode() >= 400 && getStatus().getCode() < 500) {
            actor = "(Client Error) " + getClientInfo().getAddress() + " using " + getClientInfo().getAgent();
        }
        ErrorReport er = new ErrorReport(getStatus().getCode(), actor, message, details, errorCode);
        Publisher pub = new Publisher(media);
        Representation rep;
        try {
            rep = pub.createRepresentation(er, true);
        } catch (JaqpotException unknownException) {
            throw new RuntimeException(unknownException);
        }
        return rep;
    }

    protected Representation errorReport(String errorCode, String message, String details, MediaType media, boolean doAutoLogging) {
        if (!MediaType.APPLICATION_RDF_XML.equals(media) && !MediaType.APPLICATION_RDF_TURTLE.equals(media) && !MediaType.TEXT_RDF_N3.equals(media) && !MediaType.TEXT_RDF_N3.equals(media) && !MediaType.TEXT_HTML.equals(media)) {
            media = MediaType.APPLICATION_RDF_XML;// Default!
        }
        if (doAutoLogging) {
            logger.warn(message != null ? (message + details != null ? NEWLINE : "") : "" + details != null ? details : "");
        }
        ErrorReport er = new ErrorReport();
        er.setErrorCode(errorCode);
        String actor = Configuration.BASE_URI;
        if (getStatus().getCode() >= 400 && getStatus().getCode() < 500) {
            actor = "(Client Error) " + getClientInfo().getAddress() + " using " + getClientInfo().getAgent();
        }
        er.setActor(actor);
        er.setMessage(message);
        er.setDetails(details);
        er.setHttpStatus(getStatus().getCode());
        Publisher pub = new Publisher(media);
        Representation rep;
        try {
            rep = pub.createRepresentation(er, true);
        } catch (JaqpotException unknownException) {
            throw new RuntimeException(unknownException);
        }
        return rep;
    }

    protected Representation exception(JaqpotException exception, String details, MediaType media) {
        if (!MediaType.APPLICATION_RDF_XML.equals(media) && !MediaType.APPLICATION_RDF_TURTLE.equals(media) && !MediaType.TEXT_RDF_N3.equals(media) && !MediaType.TEXT_RDF_N3.equals(media) && !MediaType.TEXT_HTML.equals(media)) {
            media = MediaType.APPLICATION_RDF_XML;// Default!
        }
        String actor = Configuration.BASE_URI;
        if (getStatus().getCode() >= 400 && getStatus().getCode() < 500) {
            actor = "(Client Error) " + getClientInfo().getAddress() + " using " + getClientInfo().getAgent();
        }
        ErrorReport er = new ErrorReport(getStatus().getCode(), actor, exception.getMessage(), null, exception.getClass().getName().toString());
        if (details == null) {
            StackTraceElement[] stackTrace = exception.getStackTrace();
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < stackTrace.length; i++) {
                sb.append(stackTrace[i].toString());
                sb.append(NEWLINE);
            }
            details = sb.toString();
        }
        er.setDetails(details);
        Publisher pub = new Publisher(media);
        Representation rep;
        try {
            rep = pub.createRepresentation(er, true);
        } catch (JaqpotException unknownException) {
            throw new RuntimeException(unknownException);
        }
        return rep;
    }

    protected Representation fatalException(String errorCode, Exception exception, String details) {
        return fatalException(errorCode, exception, exception.getMessage(), details);
    }

    protected Representation fatalException(String errorCode, Exception exception, String message, String details) {
        String actor = Configuration.BASE_URI;
        if (getStatus().getCode() >= 400 && getStatus().getCode() < 500) {
            actor = "(Client Error) " + getClientInfo().getAddress() + " using " + getClientInfo().getAgent();
        }
        ErrorReport er = new ErrorReport(getStatus().getCode(), actor, message, details, errorCode.toString());
        if (details == null) {
            StackTraceElement[] stackTrace = exception.getStackTrace();
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < stackTrace.length; i++) {
                sb.append(stackTrace[i].toString());
                sb.append(NEWLINE);
            }
            details = sb.toString();
        }
        er.setDetails(details);
        Publisher pub = new Publisher(MediaType.APPLICATION_RDF_XML);
        Representation rep;
        try {
            rep = pub.createRepresentation(er, true);
        } catch (JaqpotException ex) {
            throw new RuntimeException(ex);
        }
        return rep;
    }

    @Override
    public Representation options(Variant variant) {
        Publisher pub = new Publisher(variant.getMediaType());
        try {
            return pub.createRepresentation(getServiceDocumentation(variant), true);
        } catch (JaqpotException ex) {
            toggleServerError();
            return errorReport("PublicationError", ex.getMessage(), null, variant.getMediaType(), false);
        } catch (Exception ex) {
            ex.printStackTrace();
            return errorReport("PublicationError", ex.getMessage(), null, variant.getMediaType(), false);
        }
    }

    /**
     * Subclasses should override this method in order to provide meaningful
     * REST documentation.
     * @param variant
     * @return
     */
    protected ServiceRestDocumentation getServiceDocumentation(Variant variant) {
        DummyComponent dc = new DummyComponent(getCurrentVRINoQuery());
        dc.addOntologicalClasses(OTClasses.OpenToxResource());
        ServiceRestDocumentation doc = new ServiceRestDocumentation(dc);
        return doc;
    }

    protected void allowMethods(Method... methods) {
        Set<Method> allowedMethods = new HashSet<Method>();
        for (Method m : methods) {
            allowedMethods.add(m);
        }
        setAllowedMethods(allowedMethods);
    }
}

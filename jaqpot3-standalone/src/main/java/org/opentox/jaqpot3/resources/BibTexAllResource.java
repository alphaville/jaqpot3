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

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.shared.JenaException;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;
import org.opentox.jaqpot3.exception.JaqpotException;
import org.opentox.jaqpot3.pool.PolicyCreationPool;
import org.opentox.jaqpot3.resources.publish.DbListStreamPublisher;
import org.opentox.jaqpot3.resources.publish.Publisher;
import org.opentox.jaqpot3.util.Configuration;
import org.opentox.jaqpot3.www.URITemplate;
import org.opentox.toxotis.client.collection.Services;
import org.opentox.toxotis.core.IRestOperation;
import org.opentox.toxotis.core.component.BibTeX;
import org.opentox.toxotis.core.component.DummyComponent;
import org.opentox.toxotis.core.component.RestOperation;
import org.opentox.toxotis.core.component.ServiceRestDocumentation;
import org.opentox.toxotis.database.exception.DbException;
import org.opentox.toxotis.ontology.collection.KnoufBibTex;
import org.opentox.toxotis.ontology.impl.SimpleOntModelImpl;
import org.opentox.toxotis.core.component.User;
import org.opentox.toxotis.database.engine.bibtex.AddBibTeX;
import org.opentox.toxotis.database.engine.bibtex.ListBibTeX;
import org.opentox.toxotis.exceptions.impl.ServiceInvocationException;
import org.opentox.toxotis.exceptions.impl.ToxOtisException;
import org.opentox.toxotis.ontology.collection.HttpMethods.MethodsEnum;
import org.opentox.toxotis.util.aa.policy.GroupSubject;
import org.opentox.toxotis.util.aa.policy.Policy;
import org.opentox.toxotis.util.aa.policy.PolicyRule;
import org.opentox.toxotis.util.aa.policy.SingleSubject;
import org.opentox.toxotis.util.spiders.BibTeXSprider;
import org.restlet.data.Form;
import org.restlet.data.MediaType;
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
public class BibTexAllResource extends JaqpotResource {

    private Logger logger = LoggerFactory.getLogger(BibTexAllResource.class);
    public static final URITemplate template = new URITemplate("bibtex", null, null);
    private UUID uuid = UUID.randomUUID();

    public BibTexAllResource() {
    }

    @Override
    protected void doInit() throws ResourceException {
        super.doInit();
        setAutoCommitting(false);
        initialize(
                MediaType.TEXT_HTML,
                MediaType.APPLICATION_RDF_XML,
                MediaType.register("application/rdf+xml-abbrev", NEWLINE),
                MediaType.APPLICATION_RDF_TURTLE,
                MediaType.APPLICATION_PDF,
                MediaType.TEXT_URI_LIST,
                MediaType.TEXT_RDF_N3,
                MediaType.TEXT_RDF_NTRIPLES);
        parseStandardParameters();
    }
    


    @Override
    protected Representation get(Variant variant) throws ResourceException {
        if (acceptString != null) {
            variant.setMediaType(MediaType.valueOf(acceptString));
        }

        ListBibTeX lister = new ListBibTeX();

        if (max != null) {
            try {
                int maxInt = Integer.parseInt(max);
                lister.setPageSize(maxInt);
            } catch (NumberFormatException nfe) {
                toggleBadRequest();
                return errorReport(nfe, "NumberExpected", "Number was expected for the URL parameter 'max'",
                        variant.getMediaType(), false);
            }
        } else {
            lister.setPageSize(500);
        }

        if (page != null) {
            try {
                int pageInt = Integer.parseInt(page);
                lister.setPage(pageInt);
            } catch (NumberFormatException nfe) {
                toggleBadRequest();
                return errorReport(nfe, "NumberExpected", "Number was expected for the URL parameter 'page'",
                        variant.getMediaType(), false);
            }
        }

        DbListStreamPublisher publisher = new DbListStreamPublisher();
        publisher.setMedia(variant.getMediaType());
        publisher.setTitle("BibTex");
        publisher.setBaseUri(Configuration.getBaseUri().augment("bibtex"));
        try {
            return publisher.process(lister);
        } catch (JaqpotException ex) {
            return errorReport(ex, "DbError", "Error while getting data from the DB",
                    variant.getMediaType(), false);
        }




    }

    @Override
    protected Representation post(Representation entity, Variant variant) throws ResourceException {

        if (entity == null) {
            return errorReport("NothingPosted", "Nothing was POSTed while the specified HTTP method is POST",
                    "You can either POST a BibTeX entity in some RDF related MIME (like application/rdf+xml) or "
                    + "create the BibTeX entity you need using HTTP parameters (author, title, etc)", variant.getMediaType(), false);
        }

        final MediaType contentType = entity.getMediaType();
        final MediaType acceptHeader = variant.getMediaType();

        User creator = null;
        try {
            creator = getUser();
            if (creator != null) {
                long entriesForUser = 0;
//                try {
                    entriesForUser = 0;//;new AccountManager(creator).countBibTeX();
//                } catch (DbException ex) {
//                    toggleServerError();
//                    return errorReport(ex, "DbError", "Cannot get the number of running tasks from "
//                            + "the database - Read Error", variant.getMediaType(), false);
//                }
                int maxEntries = Configuration.getIntegerProperty("jaqpot.max_bibtex_per_user");
                if (entriesForUser >= maxEntries) { // max obtained or exceeded!
                    toggleForbidden();
                    return errorReport("UserQuotaExceeded", "User Quota Exceeded! Cannot create another BibTeX entry", "Dear " + creator.getName() + ", "
                            + "you have exceeded your quota on this server. Due to technical limitations you are not allowed to create more "
                            + "entries.", acceptHeader, false);
                }
            } else {
                toggleForbidden();
                return errorReport("Anonymous", "Anonymous use of this service is not allowed", "Please authenticate yourself using the "
                        + "Authorization HTTP Header. You can read more information about this at "
                        + "the RDF-2616 documentation (http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html#sec14.8) "
                        + "about the Authorization Header.", acceptHeader, false);
            }
        } catch (Exception e) {
            logger.error("", e);
        }

        if (MediaType.APPLICATION_WWW_FORM.equals(contentType)) {
            final Form form = new Form(entity);
            return createBibTexFromParameters(form, acceptHeader, creator);
        } else if (isRdfRelated(contentType)) {
            return createBibTexFromRDF(entity, acceptHeader, creator);
        } else if (MediaType.TEXT_PLAIN.equals(contentType)) {
            return createBibTexFromPlainText(entity, acceptHeader, creator);
        } else {
            logger.debug("Content-type " + contentType + " not supported in /bibtex");
            toggleBadRequest();
            return errorReport("ContentTypeNotSupported", "Content-type" + contentType + " not supported", "You can either generate a "
                    + "BibTex by posting an RDF representation of it (either rdf/xml, rdf/turtle or other rdf-related MIME) or "
                    + "by posting your data using a www form. Read the documentation for further information", acceptHeader, true);
        }
    }

    private Representation createBibTexFromPlainText(final Representation postedEntity, final MediaType acceptHeader, User creator) {
        BibTeX bib = null;
        try {
            bib = new BibTeX().readString(postedEntity.getStream());
            bib.setCreatedBy(creator);
            bib.setUri(Configuration.getBaseUri().augment("bibtex", uuid.toString()));
            new AddBibTeX(bib).write();
        } catch (ToxOtisException ex) {
            logger.error("Cannot Parse BibTeX entity POSTed by the client", ex);
        } catch (IOException ex) {
            logger.error("Cannot Read Input Stream from Client Data", ex);
        }

        Publisher pub = new Publisher(acceptHeader);
        try {
            return pub.createRepresentation(bib, true);
        } catch (JaqpotException ex) {
            toggleServerError();
            String message = "Cannot create representation for BibTeX resource with URI " + bib.getUri();
            logger.warn(message, ex);
            return fatalException("PublicationError", ex, message);
        }
    }

    private Representation createBibTexFromRDF(final Representation postedEntity, final MediaType acceptHeader, User creator) {
        OntModel model = new SimpleOntModelImpl();
        InputStream postedEntityStream;

        try {
            postedEntityStream = postedEntity.getStream();
        } catch (IOException ex) {
            logger.error("Stream error while trying to get "
                    + "the stream from the posted entity.", ex);
            toggleServerError();
            return fatalException("InputStreamUnreadable", ex, null);
        } catch (Exception ex) {
            logger.error("Unknown error related to the connection to the "
                    + "input stream from the posted data.", ex);
            toggleServerError();
            return fatalException("UnknownCauseOfException", ex, null);
        }


        try {
            model.read(postedEntityStream, null);
        } catch (JenaException ex) {
            String message = "The entity you posted could not be parsed due to syntactical errors. Check that it is "
                    + "syntactically correct and its format matches the specified Content-type header";
            logger.trace("Parsing Exception", ex);
            toggleBadRequest();
            return fatalException("ImproperBibTexResource", ex, message, null);
        }


        BibTeX bib = null;
        try {
            BibTeXSprider spider = new BibTeXSprider(null, model);
            bib = spider.parse();
            bib.setCreatedBy(creator);
            bib.setUri(Configuration.getBaseUri().augment("bibtex", uuid.toString()));
        } catch (ServiceInvocationException ex) {
            toggleServerError();
            logger.debug("BibTex parsing failed", ex);
            return errorReport(ex, primaryId, page, acceptHeader, true);
        } catch (Throwable ex) {
            toggleServerError();
            logger.debug("BibTex parsing failed", ex);
            return errorReport(ex, primaryId, page, acceptHeader, true);
        }

        AddBibTeX adder = new AddBibTeX(bib);
        try {
            adder.write();
        } catch (DbException ex) {
            String msg = "Cannot add bibtex entry to the database";
            logger.error(msg, ex);
            return errorReport(ex, "Uncloseable", msg, acceptHeader, false);
        } finally {
            try {
                adder.close();
            } catch (DbException ex) {
                String msg = "BibTeX Registerer is uncloseable!";
                logger.error(msg, ex);
                return errorReport(ex, "Uncloseable", msg, acceptHeader, false);
            }
        }

        Publisher pub = new Publisher(acceptHeader);
        try {
            return pub.createRepresentation(bib, true);
        } catch (JaqpotException ex) {
            toggleServerError();
            String message = "Cannot create representation for BibTeX resource with URI " + bib.getUri();
            logger.warn(message, ex);
            return fatalException("PublicationError", ex, message);
        }
    }

    private void createPolicy() {
        Policy p = new Policy();
        p.setPolicyName("bibtex_" + uuid);
        PolicyRule rule = new PolicyRule("rule1");
        rule.setTargetUri(Services.ntua().augment("bibtex", uuid).toString());
        rule.setAllowGet(true);
        rule.setAllowPost(true);
        p.addRule(rule);
        p.addSubject(SingleSubject.YAQPservice);
        p.addSubject(SingleSubject.Admin1);
        p.addSubject(SingleSubject.Admin2);
        p.addSubject(GroupSubject.DEVELOPMENT);
        p.addSubject(GroupSubject.PARTNER);
        PolicyCreationPool.POOL.run(p, null);
    }

    private Representation createBibTexFromParameters(final Form form, final MediaType acceptHeader, User creator) {

        BibTeX bibTex = new BibTeX();
        bibTex.setAuthor(form.getFirstValue("author"));
        if (bibTex.getAuthor() == null) {
            toggleBadRequest();

            return errorReport("AuthorNotProvided", "You have to provide an author",
                    "The parameter 'author' is mandatory and has to specified.",
                    MediaType.APPLICATION_RDF_XML, false);
        }

        String bibTypeParameter = form.getFirstValue("bibtype");
        if (bibTypeParameter == null) {
            logger.trace("Parameter bibtype not provided");
            bibTypeParameter = "Entry";// use the default bibtype (Entry).
        }

        BibTeX.BibTYPE type = null;
        try {
            type = BibTeX.BibTYPE.valueOf(bibTypeParameter);
        } catch (IllegalArgumentException ex) {
            logger.debug("Bibtype requested is not supported (" + bibTypeParameter + ")", ex);
            toggleBadRequest();
            return errorReport("BibTypeNotSupported", "Bibtype  '" + bibTypeParameter
                    + "' is not supported.", "For a list of all available "
                    + "bibliographic types consult the Knouf ontology <" + KnoufBibTex.NS + ">", acceptHeader, true);
        }

        bibTex.setBibType(type);
        bibTex.setAbstract(form.getFirstValue("abstract"));
        bibTex.setAddress(form.getFirstValue("address"));
        bibTex.setAnnotation(form.getFirstValue("annotation"));
        bibTex.setBookTitle(form.getFirstValue("booktitle"));
        bibTex.setChapter(form.getFirstValue("chapter"));
        bibTex.setCopyright(form.getFirstValue("copyright"));
        bibTex.setCrossref(form.getFirstValue("crossref"));
        bibTex.setUri(Configuration.getBaseUri().augment("bibtex", uuid.toString()));
        bibTex.setEditor(form.getFirstValue("editor"));
        bibTex.setEdition(form.getFirstValue("edition"));
        bibTex.setJournal(form.getFirstValue("journal"));
        bibTex.setIsbn(form.getFirstValue("isbn"));
        bibTex.setIssn(form.getFirstValue("issn"));
        bibTex.setKey(form.getFirstValue("key"));
        bibTex.setSeries(form.getFirstValue("series"));
        bibTex.setTitle(form.getFirstValue("title"));
        bibTex.setUrl(form.getFirstValue("url"));
        bibTex.setKeywords(form.getFirstValue("keywords"));
        bibTex.setCreatedBy(creator);

        String year = form.getFirstValue("year");
        String volume = form.getFirstValue("volume");
        String number = form.getFirstValue("number");

        if (year != null) {
            try {
                bibTex.setYear(Integer.parseInt(year.replaceAll(" ", "")));
            } catch (NumberFormatException ex) {
                logger.trace("Parameter 'year' is not numeric : " + year, ex);
                toggleBadRequest();
                return errorReport("NumberExpected", "The parameter 'year' should be "
                        + "numeric. Unacceptable value : " + year, year, acceptHeader, false);
            }
        }

        if (volume != null) {
            try {
                bibTex.setVolume(Integer.parseInt(volume.replaceAll(" ", "")));
            } catch (NumberFormatException ex) {
                logger.trace("Parameter 'volumn' is not numeric : " + volume, ex);
                toggleBadRequest();
                return errorReport("NumberExpected", "The parameter 'volumn' should be "
                        + "numeric. Unacceptable value : " + volume, null, acceptHeader, false);
            }
        }

        if (number != null) {
            try {
                bibTex.setNumber(Integer.parseInt(number.replaceAll(" ", "")));
            } catch (NumberFormatException ex) {
                logger.trace("Parameter 'number' is not numeric : " + number, ex);
                toggleBadRequest();
                return errorReport("NumberExpected", "The parameter 'number' should be "
                        + "numeric. Unacceptable value : " + number, null, acceptHeader, false);
            }
        }
        bibTex.setPages(form.getFirstValue("pages"));

        try {
            AddBibTeX adder = new AddBibTeX(bibTex);
            adder.write();
            adder.close();


            Publisher pub = new Publisher(acceptHeader);
            createPolicy();
            return pub.createRepresentation(bibTex, true);
        } catch (final JaqpotException ex) {
            logger.error("Exception while trying to create a representation "
                    + "for a BibTeX object!", ex);
            return fatalException("UnknownCauseOfException", ex, "Fatal Error while trying to register "
                    + "a new BibTex entry in the database");
        } catch (final Exception ex) {
            logger.error("Unknown source of error.", ex);
            toggleServerError();
            return fatalException("UnknownCauseOfException", ex, "Fatal Error while trying to register "
                    + "a new BibTex entry in the database");
        }
    }

    @Override
    protected Representation delete(Variant variant) throws ResourceException {

//        int linesAffected = 0;
//        try {
//            User user = getUser();
//            if (user == null) {
//                toggleForbidden();
//                return errorReport("Forbidden", "No token is provided!",
//                        "Cannot apply a DELETE method on /bibtex without providing your authentication token",
//                        variant.getMediaType(), false);
//            }
//            String hql = "DELETE FROM BibTeX WHERE createdBy = :creatorUid";
//            Query query = getNewSession().createQuery(hql).setString("creatorUid", user.getUid());
//            linesAffected = query.executeUpdate();
//
//        } catch (ToxOtisException ex) {
//            logger.trace("Error trying to retrieve user data...", ex);
//        } catch (HibernateException ex) {
//            toggleServerError();
//            logger.warn("Hibernate exception caught while trying to DELETE all bibtex resources for a certain user", ex);
//            return errorReport("DatabaseError", "Exceptional event related to bibtex deletion from the database. "
//                    + "The issue is automatically logged and a team of jackals will tackle it asap.", null, variant.getMediaType(), true);
//        } finally {
//            try {
//                getNewSession().close();
//            } catch (HibernateException ex) {
//                toggleServerError();
//                logger.warn("Hibernate exception caught while trying to close a session", ex);
//                return errorReport("DatabaseError", "Exceptional event related to session management. "
//                        + "The issue is automatically logged and a team of jackals will tackle it asap.", null, variant.getMediaType(), true);
//            }
//        }
        return new StringRepresentation(0 + " bibtex resources were deleted!" + NEWLINE, MediaType.TEXT_PLAIN);
    }

    @Override
    protected ServiceRestDocumentation getServiceDocumentation(Variant variant) {
        ServiceRestDocumentation doc = new ServiceRestDocumentation(new DummyComponent(getCurrentVRINoQuery()));

        IRestOperation get = new RestOperation();
        get.setMethod(MethodsEnum.GET);
        //get.addRestClasses(OTRestClasses.GET);
        return super.getServiceDocumentation(variant);
    }
}

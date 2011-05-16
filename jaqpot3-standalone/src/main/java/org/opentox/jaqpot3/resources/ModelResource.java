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

import java.net.URISyntaxException;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.opentox.jaqpot3.exception.JaqpotException;
import org.opentox.jaqpot3.pool.ExecutionPool;
import org.opentox.jaqpot3.qsar.IClientInput;
import org.opentox.jaqpot3.qsar.IPredictor;
import org.opentox.jaqpot3.qsar.PredictorFinder;
import org.opentox.jaqpot3.qsar.util.PMMLGenerator;
import org.opentox.jaqpot3.resources.publish.Publisher;
import org.opentox.jaqpot3.util.Configuration;
import org.opentox.jaqpot3.util.TaskFactory;
import org.opentox.jaqpot3.www.ClientInput;
import org.opentox.jaqpot3.www.URITemplate;
import org.opentox.jaqpot3.www.services.PredictionService;
import org.opentox.toxotis.client.VRI;
import org.opentox.toxotis.core.IRestOperation;
import org.opentox.toxotis.core.component.BibTeX;
import org.opentox.toxotis.core.component.DummyComponent;
import org.opentox.toxotis.core.component.HttpMediatype;
import org.opentox.toxotis.core.component.HttpStatus;
import org.opentox.toxotis.core.component.Model;
import org.opentox.toxotis.core.component.RestOperation;
import org.opentox.toxotis.core.component.ServiceRestDocumentation;
import org.opentox.toxotis.core.component.Task;
import org.opentox.toxotis.core.component.User;
import org.opentox.toxotis.database.IDbIterator;
import org.opentox.toxotis.database.account.AccountManager;
import org.opentox.toxotis.database.engine.DisableComponent;
import org.opentox.toxotis.database.engine.bibtex.AssociateBibTeX;
import org.opentox.toxotis.database.engine.model.FindModel;
import org.opentox.toxotis.database.engine.task.AddTask;
import org.opentox.toxotis.database.engine.user.AddUser;
import org.opentox.toxotis.database.engine.user.FindUser;
import org.opentox.toxotis.database.exception.DbException;
import org.opentox.toxotis.exceptions.impl.ServiceInvocationException;
import org.opentox.toxotis.exceptions.impl.ToxOtisException;
import org.opentox.toxotis.ontology.ResourceValue;
import org.opentox.toxotis.ontology.collection.OTClasses;
import org.opentox.toxotis.ontology.collection.OTRestClasses;
import org.opentox.toxotis.ontology.impl.MetaInfoImpl;
import org.opentox.toxotis.util.aa.policy.PolicyManager;
import org.restlet.data.MediaType;
import org.restlet.data.Method;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.representation.Variant;
import org.restlet.resource.ResourceException;

/**
 *
 * @author Pantelis Sopasakis
 * @author Charalampos Chomenides
 */
public class ModelResource extends JaqpotResource {

    public static final URITemplate template = new URITemplate("model", "model_id", null);
    private org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ModelResource.class);
    private UUID uuid = UUID.randomUUID();

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
                MediaType.TEXT_RDF_NTRIPLES,
                MediaType.APPLICATION_XML);
        parseStandardParameters();
        updatePrimaryId(template);
    }
    

    @Override
    public Representation doConditionalHandle() {
        Method requestMethod = getMethod();
        if ("PATCH".equals(requestMethod.getName())) {
            IClientInput clientInput = new ClientInput(getRequestEntity());
            String[] bibtexUris = clientInput.getValuesArray("bibtex");

            VRI[] bibteXs = new VRI[bibtexUris.length];
            VRI temp = null;
            int index = 0;
            for (String bib : bibtexUris) {
                try {
                    temp = new VRI(bib);
                    if (!BibTeX.class.equals(temp.getOpenToxType())) {
                        toggleBadRequest();
                        return errorReport("BadParametrization", "The parameter '" + bib + "' is not a valid BibTeX URI", "",
                                MediaType.APPLICATION_RDF_XML, false);
                    }
                    bibteXs[index] = temp;
                } catch (URISyntaxException ex) {
                    toggleBadRequest();
                    return errorReport(ex, "BadParametrization", "The parameter '" + bib + "' is not a valid BibTeX URI",
                            MediaType.APPLICATION_RDF_XML, false);
                }
                index++;
            }
            AssociateBibTeX associator = new AssociateBibTeX(primaryId, bibtexUris);
            try {
                associator.write();
            } catch (DbException ex) {
                toggleBadRequest();
                return errorReport(ex, "DatabaseException", "Cannot associate this model with the submitted list of BibTeX URIs ",
                        MediaType.APPLICATION_RDF_XML, false);
            } finally {
                try {
                    associator.close();
                } catch (DbException ex) {
                    toggleServerError();
                    return errorReport(ex, "DatabaseException", "Cannot close a database connection", MediaType.APPLICATION_RDF_XML, false);
                }
            }
            toggleSuccess();
            return new StringRepresentation(getCurrentVRINoQuery().toString(), MediaType.TEXT_URI_LIST);
        }
        return super.doConditionalHandle();
    }

    @Override
    protected Representation get(Variant variant) throws ResourceException {
        FindModel finder = null;
        IDbIterator<Model> modelsFound = null;
        try {
            if (acceptString != null) {
                variant.setMediaType(MediaType.valueOf(acceptString));
            }
            finder = new FindModel(Configuration.getBaseUri());
            finder.setSearchById(primaryId);
            finder.setResolveUsers(true);
            modelsFound = finder.list();
            Model mdl = null;
            if (modelsFound.hasNext()) {
                mdl = modelsFound.next();
            }
            if (mdl == null || (mdl != null && !mdl.isEnabled())) {
                toggleNotFound();
                return errorReport("ModelNotFound", "The model you requested was not found in our database",
                        "The model with id " + primaryId + " was not found in the database",
                        variant.getMediaType(), false);
            }
            if (variant.getMediaType().equals(MediaType.APPLICATION_XML)) {
                try {
                    return sendMessage(PMMLGenerator.generatePMML(mdl));
                } catch (UnsupportedOperationException ex) {
                    getResponse().setStatus(Status.SERVER_ERROR_NOT_IMPLEMENTED);
                    return errorReport("NotSupportedYet", ex.getMessage(),
                            "PMML Representation for " + mdl.getAlgorithm() + " models is not implemented yet",
                            variant.getMediaType(), false);
                }
            }
            Publisher p = new Publisher(variant.getMediaType());
            return p.createRepresentation(mdl, true);
        } catch (Exception ex) {
            ex.printStackTrace();
            logger.error(null, ex);
            throw new ResourceException(Status.SERVER_ERROR_INTERNAL);
        } finally {
            if (modelsFound != null) {
                try {
                    modelsFound.close();
                } catch (DbException ex) {
                    String msg = "DB iterator is uncloseable";
                    logger.error(msg, ex);
                    return errorReport(ex, "DBIteratorUncloseable", msg, variant.getMediaType(), false);
                }
            }
            if (finder != null) {
                try {
                    finder.close();
                } catch (DbException ex) {
                    String msg = "Model Finder (DB reader) is uncloseable";
                    logger.error(msg, ex);
                    return errorReport(ex, "DBReaderUncloseable", msg, variant.getMediaType(), false);
                }
            }
        }
    }

    @Override
    protected Representation delete(Variant variant) throws ResourceException {
        DisableComponent disabler = new DisableComponent(primaryId);
        try {
            int count = disabler.disable();
            return new StringRepresentation(count + " components where disabled.\n", MediaType.TEXT_PLAIN);
        } catch (DbException ex) {
            Logger.getLogger(BibTexResource.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                disabler.close();
            } catch (DbException ex) {
                String msg = "DB updater is uncloseable";
                logger.error(msg, ex);
                return errorReport(ex, "DBUpdaterUncloseable", msg, variant.getMediaType(), false);
            }
        }
        return null;

    }

    @Override
    protected Representation post(Representation entity, Variant variant) throws ResourceException {
        /*
         * The user that triggered the prediction procedure
         */
        User creator = null;

        /*
         * Whether the user uses the service for the first time
         */
        boolean newUser = false;

        /* CHECK USER QUOTA */
        creator = getUser();  // The user as retrieved by its token (Null if no token is provided)
        if (creator == null) {
            toggleUnauthorized();
            return errorReport("AuthenticationFailed", "Anonymous predictions are now allowed",
                    "You have to authenticate yourself using the 'subjectid' Header according to the OpenTox API "
                    + "specifications", variant.getMediaType(), false);
        }

        /**
         * Is that user in the database already or not???
         */
        FindUser finder = new FindUser();
        finder.setWhere("uid='" + creator.getUid() + "'");
        try {
            IDbIterator<User> iterator = finder.list();
            if (iterator.hasNext()) {
                creator = iterator.next();
            } else {
                newUser = true;
            }
        } catch (DbException ex) {
            Logger.getLogger(AlgorithmResource.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                finder.close();
            } catch (DbException ex) {
                Logger.getLogger(AlgorithmResource.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        /*
         * Register the user in the database (in case, not alredy there)
         */
        if (newUser) {
            try {
                PolicyManager.defaultSignleUserPolicy(Configuration.getBaseUri().getHost()+"user_" + creator.getUid(), Configuration.getBaseUri().augment("user",creator.getUid()), getUserToken()).
                        publish(null, getUserToken());
            } catch (ToxOtisException ex) {
                toggleServerError();
                return errorReport(ex, "Policy Creation Failed for user "+creator.getName(), "", variant.getMediaType(), false);
            } catch (ServiceInvocationException ex) {
                toggleRemoteError();
                return errorReport(ex, "Policy Creation Failed for user "+creator.getName(), "", variant.getMediaType(), false);
            }
            creator.setMaxModels(2000);
            creator.setMaxBibTeX(2000);
            creator.setMaxParallelTasks(5);
            AddUser addUser = new AddUser(creator);
            try {
                addUser.write();
            } catch (DbException ex) {
                // User is already registered! :-)
                // Proceed...
            } finally {
                try {
                    addUser.close();
                } catch (DbException ex) {
                    String msg = "User DB writer is uncloseable";
                    logger.error(msg, ex);
                    toggleServerError();
                    return errorReport(ex, "DBWriterUncloseable", msg, variant.getMediaType(), false);
                }
            }
        }


        long numTasksActive = 0;
        try {
            numTasksActive = new AccountManager(creator).countActiveTasks();
        } catch (DbException ex) {
            toggleServerError();
            return errorReport(ex, "DbError", "Cannot get the number of running tasks from "
                    + "the database - Read Error", variant.getMediaType(), false);
        }
        int maxTasks = Configuration.getIntegerProperty("jaqpot.max_tasks_per_user", 5);
        if (numTasksActive >= maxTasks) {
            toggleServerOverloaded(2);
            return errorReport("UserQuotaExceeded",
                    "User Quota Exceeded! Cannot create another Task",
                    "Dear " + creator.getName() + ", " + "you have exceeded your quota on this server (" + maxTasks + "). "
                    + "Due to technical limitations you are not allowed to run more "
                    + "tasks in parallel. Wait for any tasks you created to finish and then submit this task.",
                    variant.getMediaType(), false);
        }


        Task task = TaskFactory.newQueuedTask(creator, uuid);
        task.getMeta().
                addDescription("Asynchronous Task for Prediction").
                addComment("Asynchronous task created for a background job initiated by the model: " + primaryId).
                addHasSource(new ResourceValue(getCurrentVRI().removeUrlParameter("subjectid"), OTClasses.Model()));



        AddTask taskAdder = new AddTask(task);
        try {
            taskAdder.write();
        } catch (DbException ex) {
            Logger.getLogger(AlgorithmResource.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                taskAdder.close();
            } catch (DbException ex) {
                String msg = "Task registerer (DB writer) is uncloseable";
                logger.error(msg, ex);
                return errorReport(ex, "DBWriterUncloseable", msg, variant.getMediaType(), false);
            }
        }

        Model model = null;
        FindModel modelFinder = new FindModel(Configuration.getBaseUri());
        modelFinder.setSearchById(primaryId);
        IDbIterator<Model> modelsFound = null;
        try {
            modelsFound = modelFinder.list();
            if (modelsFound.hasNext()) {
                model = modelsFound.next();
            }
        } catch (DbException ex) {
            String msg = "Model cannot be found due to DB connection problems";
            logger.error(msg, ex);
            return errorReport(ex, "DBConnectionException", msg, variant.getMediaType(), false);
        } finally {
            if (modelsFound != null) {
                try {
                    modelsFound.close();
                } catch (DbException ex) {
                    String msg = "Model Iterator (DB iterator) is uncloseable";
                    logger.error(msg, ex);
                    return errorReport(ex, "DBIteratorUncloseable", msg, variant.getMediaType(), false);
                }
            }
            try {
                modelFinder.close();
            } catch (DbException ex) {
                String msg = "Model Finder (DB reader) is uncloseable";
                logger.error(msg, ex);
                return errorReport(ex, "DBReaderUncloseable", msg, variant.getMediaType(), false);
            }
        }

        if (model != null) {
            IClientInput clientInput = new ClientInput(entity);
            IPredictor predictor = PredictorFinder.getPredictor(model.getAlgorithm().getUri().getId());
            predictor.setModel(model);
            predictor.setTask(task);
            PredictionService ps = new PredictionService(predictor, clientInput, getUserToken());
            ExecutionPool.POOL.run(uuid.toString(), ps);
        } else {
            toggleNotFound();
            return errorReport("ModelNotFound", "The model with id '" + primaryId + "' was not found on the server", "details", variant.getMediaType(), false);
        }

        getResponse().setStatus(Status.valueOf((int) task.getHttpStatus()));
        /** The user takes a task and waits for completion**/
        Publisher publisher = new Publisher(variant.getMediaType());
        release();
        try {
            return publisher.createRepresentation(task, true);
        } catch (JaqpotException ex) {
            return fatalException("PublicationError", ex, null);
        }
    }

    @Override
    protected ServiceRestDocumentation getServiceDocumentation(Variant variant) {
        ServiceRestDocumentation doc = new ServiceRestDocumentation(new DummyComponent(getCurrentVRINoQuery()));
        IRestOperation get = new RestOperation();
        get.addHttpStatusCodes(
                new HttpStatus(OTRestClasses.STATUS_200()).setMeta(new MetaInfoImpl().addTitle("Success").
                addDescription("The model was found in the database and its representation in the prefered MIME type is returned to the client.")),
                new HttpStatus(OTRestClasses.STATUS_404()).setMeta(new MetaInfoImpl().addTitle("Not Found").
                addDescription("The model was not found in the database").addComment("You can have a complete list of the available models at "
                + Configuration.getBaseUri().augment("model"))),
                new HttpStatus(OTRestClasses.STATUS_403()),
                new HttpStatus(OTRestClasses.STATUS_401()));
        get.addOntologicalClasses(OTRestClasses.GET_Model());
        get.addMediaTypes(new HttpMediatype().addOntologicalClasses(OTRestClasses.mime_text_uri_list()),
                new HttpMediatype().addOntologicalClasses(OTRestClasses.mime_text_html()),
                new HttpMediatype().addOntologicalClasses(OTRestClasses.mime_rdf_xml()),
                new HttpMediatype().addOntologicalClasses(OTRestClasses.mime_rdf_n3()),
                new HttpMediatype().addOntologicalClasses(OTRestClasses.mime_rdf_turtle()));


        doc.addRestOperations(get);
        return doc;
    }
}




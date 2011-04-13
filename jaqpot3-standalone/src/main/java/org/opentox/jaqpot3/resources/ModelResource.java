package org.opentox.jaqpot3.resources;

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
import org.opentox.toxotis.core.IRestOperation;
import org.opentox.toxotis.core.component.DummyComponent;
import org.opentox.toxotis.core.component.HttpMediatype;
import org.opentox.toxotis.core.component.HttpStatus;
import org.opentox.toxotis.core.component.Model;
import org.opentox.toxotis.core.component.RestOperation;
import org.opentox.toxotis.core.component.ServiceRestDocumentation;
import org.opentox.toxotis.core.component.Task;
import org.opentox.toxotis.core.component.User;
import org.opentox.toxotis.database.IDbIterator;
import org.opentox.toxotis.database.engine.DisableComponent;
import org.opentox.toxotis.database.engine.model.FindModel;
import org.opentox.toxotis.database.engine.task.AddTask;
import org.opentox.toxotis.database.exception.DbException;
import org.opentox.toxotis.ontology.ResourceValue;
import org.opentox.toxotis.ontology.collection.OTClasses;
import org.opentox.toxotis.ontology.collection.OTRestClasses;
import org.opentox.toxotis.ontology.impl.MetaInfoImpl;
import org.restlet.data.MediaType;
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
        User creator = null;
        /* CHECK USER QUOTA */

        System.out.println(":)))");
        creator = getUser();
        if (creator == null) {
            toggleUnauthorized();
            return errorReport("AuthenticationFailed", "Anonymous predictions are now allowed",
                    "You have to authenticate yourself using the 'subjectid' Header according to the OpenTox API "
                    + "specifications", variant.getMediaType(), false);
        }
        long numTasksActive = getActiveTasks();
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
                addHasSource(new ResourceValue(getCurrentVRI(), OTClasses.Model()));



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




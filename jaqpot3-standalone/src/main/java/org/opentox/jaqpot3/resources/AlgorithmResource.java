package org.opentox.jaqpot3.resources;

import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.opentox.jaqpot3.exception.JaqpotException;
import org.opentox.jaqpot3.pool.ExecutionPool;
import org.opentox.jaqpot3.qsar.AlgorithmFinder;
import org.opentox.jaqpot3.qsar.IClientInput;
import org.opentox.jaqpot3.qsar.IParametrizableAlgorithm;
import org.opentox.jaqpot3.qsar.ITrainer;
import org.opentox.jaqpot3.resources.collections.Algorithms;
import org.opentox.jaqpot3.resources.publish.Publisher;
import org.opentox.jaqpot3.util.Configuration;
import org.opentox.jaqpot3.util.TaskFactory;
import org.opentox.jaqpot3.www.ClientInput;
import org.opentox.jaqpot3.www.URITemplate;
import org.opentox.jaqpot3.www.services.TrainingService;
import org.opentox.toxotis.database.exception.DbException;
import org.opentox.toxotis.core.component.Algorithm;
import org.opentox.toxotis.core.component.ServiceRestDocumentation;
import org.opentox.toxotis.core.component.Task;
import org.opentox.toxotis.core.component.User;
import org.opentox.toxotis.database.account.AccountManager;
import org.opentox.toxotis.database.engine.task.AddTask;
import org.opentox.toxotis.database.engine.user.AddUser;
import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.representation.Variant;
import org.restlet.resource.ResourceException;

/**
 *
 * @author Pantelis Sopasakis
 * @author Charalampos Chomenides
 */
public class AlgorithmResource extends JaqpotResource {

    public static final URITemplate template = new URITemplate("algorithm", "algorithm_id", null);
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
                MediaType.TEXT_RDF_NTRIPLES);
        parseStandardParameters();
        updatePrimaryId(template);
    }

    @Override
    protected Representation get(Variant variant) throws ResourceException {
        if (acceptString != null) {
            variant.setMediaType(MediaType.valueOf(acceptString));
        }
        Algorithm a = Algorithms.forName(primaryId);
        if (a == null) {
            toggleNotFound();
            return errorReport("AlgorithmNotFound", "Algorithm not found on the server", null, variant.getMediaType(), false);
        }
        Publisher pub = new Publisher(variant.getMediaType());
        try {
            Representation rep = pub.createRepresentation(a, true);
            return rep;
        } catch (JaqpotException ex) {
            if ("MediaTypeNotSupported".equals(ex.getMessage())) {
                getResponse().setStatus(Status.CLIENT_ERROR_UNSUPPORTED_MEDIA_TYPE);
                return exception(ex, null, variant.getMediaType());
            } else {
                toggleServerError();
            }
            return errorReport("PublicationError", ex.getMessage(), null, variant.getMediaType(), false);
        } catch (Exception ex) {
            ex.printStackTrace();
            return errorReport("PublicationError", ex.getMessage(), null, variant.getMediaType(), false);
        }
    }

    @Override
    protected Representation post(Representation entity, Variant variant) throws ResourceException {

        User creator = null;
        /* CHECK USER QUOTA */


        creator = getUser();
        if (creator == null) {
            toggleUnauthorized();
            return errorReport("Anonymous", "Anonymous predictions are now allowed",
                    "You have to authenticate yourself using the 'Authorization' Header according to the OpenTox API "
                    + "specifications", variant.getMediaType(), false);
        }

        long numModels = 0;
        try {
            numModels = new AccountManager(creator).countModels();
        } catch (DbException ex) {
            Logger.getLogger(AlgorithmResource.class.getName()).log(Level.SEVERE, null, ex);
        }


        long numTasksActive = 0;
        try {
            numTasksActive = new AccountManager(creator).countActiveTasks();
        } catch (DbException ex) {
            Logger.getLogger(AlgorithmResource.class.getName()).log(Level.SEVERE, null, ex);
        }

        //TODO: This should become user-specific
        int maxModels = Configuration.getIntegerProperty("jaqpot.max_models_per_user");
        int maxTasks = Configuration.getIntegerProperty("jaqpot.max_tasks_per_user", 5);
        if (numModels >= maxModels) {
            toggleInsufficientStorage();
            return errorReport("UserQuotaExceeded",
                    "User Quota Exceeded! Cannot create another Model",
                    "Dear " + creator.getName() + ", " + "you have exceeded your quota on this server (" + maxModels + "). "
                    + "Due to technical limitations you are not allowed to create more "
                    + "models [Insufficient storage]. You are advised to delete some of your models and try again. Check your quota at "
                    + Configuration.BASE_URI + "/user/" + creator.getUid() + "/quota . Maximum number of models for you is : "
                    + maxModels + ".", variant.getMediaType(), false);
        } else if (numTasksActive >= maxTasks) {
            final int retryAfterMinutes = 2;
            toggleServerOverloaded(retryAfterMinutes);
            return errorReport("UserQuotaExceeded",
                    "User Quota Exceeded! Cannot create another Task",
                    "Dear " + creator.getName() + ", " + "you have exceeded your quota on this server (" + maxTasks + "). "
                    + "Due to technical limitations you are not allowed to run more "
                    + "tasks in parallel. Wait for any tasks you created to finish and then submit this task. Follow the Retry-After "
                    + "header of the response (retry after " + retryAfterMinutes + "mins). Check your quota at "
                    + Configuration.BASE_URI + "/user/" + creator.getUid() + "/quota . Maximum number of models for you is : "
                    + maxTasks + ".",
                    variant.getMediaType(), false);
        }


        /*
         * New task is created for the first time (as a task object in-memory)
         */
        Task task = TaskFactory.newQueuedTask(creator, uuid);
        task.getMeta().addDescription("Asynchronous Task").
                addComment("Asynchronous task created for a background job initiated by the algorithm: " + primaryId);
        task.setStatus(Task.Status.QUEUED);



        AddUser addUser = new AddUser(creator);
        try {
            addUser.write();
        } catch (DbException ex) {
            // User is already registered! :-)
            // Proceed...
        }finally{
            try {
                addUser.close();
            } catch (DbException ex) {
                Logger.getLogger(AlgorithmResource.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        /*
         * Task should be added in the database as QUEUED
         */
        //TODO: Handle Exceptions
        AddTask taskAdder = new AddTask(task);
        try {
            taskAdder.write();
        } catch (DbException ex) {
            Logger.getLogger(AlgorithmResource.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                taskAdder.close();
            } catch (DbException ex) {
                Logger.getLogger(AlgorithmResource.class.getName()).log(Level.SEVERE, null, ex);
            }
        }


        IParametrizableAlgorithm algorithm = AlgorithmFinder.getAlgorithm(primaryId);
        if (algorithm != null) {// algorithm found!
            IClientInput clientInput = new ClientInput(entity);
            algorithm.setTask(task);
            ITrainer trainer = (ITrainer) algorithm;
            TrainingService ts = new TrainingService(trainer, clientInput, getUserToken());
            ExecutionPool.POOL.run(uuid.toString(), ts);
        } else {
            toggleNotFound();
            return errorReport("AlgorithmNotFound", "The algorithm with id '" + primaryId + "' was not found on the server", "details", variant.getMediaType(), false);
        }

        /** The user takes a task and waits for completion**/
        Publisher publisher = new Publisher(variant.getMediaType());
        //TODO: Should release be here???
        release();
        try {
            getResponse().setStatus(Status.valueOf((int) task.getHttpStatus()));
            return publisher.createRepresentation(task, true);
        } catch (JaqpotException ex) {
            return fatalException("PublicationError", ex, null);
        }
    }

    @Override
    public ServiceRestDocumentation getServiceDocumentation(Variant variant) {
        throw new UnsupportedOperationException();
    }
}

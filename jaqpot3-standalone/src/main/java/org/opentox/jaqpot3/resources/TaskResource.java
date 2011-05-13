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
 * Copyright (C) 2009-2011 Pantelis Sopasakis & Charalampos Chomenides
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

import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.opentox.jaqpot3.pool.ExecutionPool;
import org.opentox.jaqpot3.resources.publish.Publisher;
import org.opentox.jaqpot3.util.Configuration;
import org.opentox.jaqpot3.www.URITemplate;
import org.opentox.toxotis.core.component.HttpStatus;
import org.opentox.toxotis.core.component.RestOperation;
import org.opentox.toxotis.core.component.ServiceRestDocumentation;
import org.opentox.toxotis.core.component.Task;
import org.opentox.toxotis.database.IDbIterator;
import org.opentox.toxotis.database.engine.DisableComponent;
import org.opentox.toxotis.database.engine.task.FindTask;
import org.opentox.toxotis.database.engine.task.UpdateTask;
import org.opentox.toxotis.database.exception.DbException;
import org.opentox.toxotis.ontology.collection.HttpMethods.MethodsEnum;
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
public class TaskResource extends JaqpotResource {

    public static final URITemplate template = new URITemplate("task", "task_id", null);
    private org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(TaskResource.class);

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
        try {
            if (acceptString != null) {
                variant.setMediaType(MediaType.valueOf(acceptString));
            }
            Task task = null;
            FindTask taskFinder = new FindTask(Configuration.getBaseUri(), true, true);
            IDbIterator<Task> tasksFound = null;

            try {
                taskFinder.setSearchById(primaryId);
                tasksFound = taskFinder.list();
                if (tasksFound.hasNext()) {
                    task = tasksFound.next();
                }
            } catch (DbException ex) {
                logger.error("DB exception while searchin in the DB for the Task with primary ID '" + primaryId + "'");
                throw ex;
            } finally {
                Exception e = null;
                try {
                    if (tasksFound != null) {
                        tasksFound.close();
                    }
                } catch (DbException ex) {
                    logger.error("DB iterator is uncloseable");
                    e = ex;
                }
                try {
                    if (taskFinder != null) {
                        taskFinder.close();
                    }
                } catch (DbException ex) {
                    logger.error("DB reader is uncloseable");
                    e = ex;
                }
                if (e != null) {
                    throw e;
                }
            }


            if (task == null) {
                toggleNotFound();
                return errorReport("TaskNotFound", "The task you requested was not found in our database",
                        "The task with id " + primaryId + " was not found in the database",
                        variant.getMediaType(), false);
            }
            float httpStatus = task.getHttpStatus();
            getResponse().setStatus(Status.valueOf((int) httpStatus));

            if (MediaType.TEXT_URI_LIST.equals(variant.getMediaType())) {
                if (Task.Status.COMPLETED.equals(task.getStatus())) {
                    return new StringRepresentation(task.getResultUri().toString(), MediaType.TEXT_URI_LIST);
                } else {
                    return new StringRepresentation(task.getUri().toString(), MediaType.TEXT_URI_LIST);
                }

            }
            Publisher p = new Publisher(variant.getMediaType());
            return p.createRepresentation(task, true);
        } catch (final Exception ex) {
            logger.error("Exception thrown from task : " + getReference(), ex);
            throw new ResourceException(Status.SERVER_ERROR_INTERNAL);
        }

    }

    @Override
    protected ServiceRestDocumentation getServiceDocumentation(Variant variant) {
        ServiceRestDocumentation doc = new ServiceRestDocumentation(new Task(getCurrentVRINoQuery()));

        RestOperation delete = new RestOperation();
        delete.addHttpStatusCodes(
                new HttpStatus(OTRestClasses.STATUS_200()).setMeta(new MetaInfoImpl().addTitle("Success").
                addDescription("The task was successfully deleted from the database").
                addComment("The status code 200 is returned in any case of successful deletion but excluding the "
                + "case where the underlying task was not found in the database")),
                new HttpStatus(OTRestClasses.STATUS_404()).setMeta(new MetaInfoImpl().addTitle("Not Found").
                addDescription("The task was not found in the database")),
                new HttpStatus(OTRestClasses.STATUS_403()),
                new HttpStatus(OTRestClasses.STATUS_401()));
        delete.setMethod(MethodsEnum.DELETE);
        delete.addOntologicalClasses(OTRestClasses.DELETE_Task(), OTRestClasses.OperationTask(), OTRestClasses.OperationNoResult());
        delete.getMeta().addDescription("Cancels a running task.");
        delete.setProtectedResource(false);


        RestOperation get = new RestOperation();
        get.addHttpStatusCodes(
                new HttpStatus(OTRestClasses.STATUS_200()).setMeta(new MetaInfoImpl().addTitle("Success").
                addDescription("The task is successfully retrieved from the database and has completed redirecting to the result "
                + "of the calculation")),
                new HttpStatus(OTRestClasses.STATUS_202()).setMeta(new MetaInfoImpl().addTitle("Success").
                addDescription("The task is successfully retrieved from the database and is still running/processing")),
                new HttpStatus(OTRestClasses.STATUS_201()).setMeta(new MetaInfoImpl().addTitle("Success").
                addDescription("The task is successfully retrieved from the database has completed its own part of the overall work but redirect to "
                + "some other task on some other server")),
                new HttpStatus(OTRestClasses.STATUS_404()).setMeta(new MetaInfoImpl().addTitle("Not Found").
                addDescription("The task was successfully deleted from the database")));
        get.addUrlParameter("media", true, XSDDatatype.XSDstring, new MetaInfoImpl().addTitle("media").
                addDescription("Specify your prefered MIME type in the URL of the request. In particular useful if "
                + "the service is invoked via a browser."));
        get.addUrlParameter("method", true, XSDDatatype.XSDstring, new MetaInfoImpl().addTitle("method").
                addDescription("Override the GET method. Can be used to view the content of "
                + "an OPTIONS response from your browser."));
        doc.addRestOperations(delete, get);

        return doc;
    }

    @Override
    protected Representation delete(Variant variant) throws ResourceException {

        Task task = null;
        FindTask taskFinder = new FindTask(Configuration.getBaseUri(), true, true);
        IDbIterator<Task> tasksFound = null;

        try {
            taskFinder.setSearchById(primaryId);
            tasksFound = taskFinder.list();
            if (tasksFound.hasNext()) {
                task = tasksFound.next();
            }
        } catch (DbException ex) {
            logger.error("DB exception while searchin in the DB for the Task with primary ID '" + primaryId + "'");
            //TODO: Handle ex

        } finally {
            Exception e = null;
            try {
                if (tasksFound != null) {
                    tasksFound.close();
                }
            } catch (DbException ex) {
                logger.error("DB iterator is uncloseable");
                e = ex;
            }
            try {
                if (taskFinder != null) {
                    taskFinder.close();
                }
            } catch (DbException ex) {
                logger.error("DB reader is uncloseable");
                e = ex;
            }
            if (e != null) {
                //TODO: Handle ex
            }
        }


        if (task == null) {
            toggleNotFound();
            return errorReport("TaskNotFound", "The task you requested was not found in our database",
                    "The task with id " + primaryId + " was not found in the database",
                    variant.getMediaType(), false);
        }


        if (Task.Status.COMPLETED.equals(task.getStatus())
                || Task.Status.REJECTED.equals(task.getStatus())
                || Task.Status.CANCELLED.equals(task.getStatus())) {
            DisableComponent disabler = new DisableComponent(primaryId);
            try {
                disabler.disable();
            } catch (DbException ex) {
                Logger.getLogger(TaskResource.class.getName()).log(Level.SEVERE, null, ex);
            } finally {
                try {
                    disabler.close();
                } catch (DbException ex) {
                    Logger.getLogger(TaskResource.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        } else {
            task.setStatus(Task.Status.CANCELLED);
            ExecutionPool.POOL.cancel(primaryId);
            UpdateTask updater = new UpdateTask(task);
            updater.setUpdateTaskStatus(true);
            try {
                updater.update();
            } catch (DbException ex) {
                Logger.getLogger(TaskResource.class.getName()).log(Level.SEVERE, null, ex);
            } finally {
                try {
                    updater.close();
                } catch (DbException ex) {
                    Logger.getLogger(TaskResource.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }


        return new StringRepresentation("Task cancelled" + NEWLINE);

    }
}

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

import java.io.IOException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.opentox.jaqpot3.www.URITemplate;
import org.opentox.toxotis.database.engine.model.CountModel;
import org.opentox.toxotis.database.engine.task.CountTasks;
import org.opentox.toxotis.database.exception.DbException;
import org.restlet.data.MediaType;
import org.restlet.ext.xml.DomRepresentation;
import org.restlet.ext.xml.XmlRepresentation;
import org.restlet.representation.Representation;
import org.restlet.representation.Variant;
import org.restlet.resource.ResourceException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 *
 * @author Pantelis Sopasakis
 */
public class DbStatisticsResource extends JaqpotResource {

    public static final URITemplate template = new URITemplate("dbstats", null, null);
    private org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ErrorResource.class);

    @Override
    protected void doInit() throws ResourceException {
        super.doInit();
        setAutoCommitting(false);
        initialize(MediaType.APPLICATION_XML);
    }

    private Document doc() throws DbException {
        Document document = null;
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            document = builder.newDocument();

            Element root = (Element) document.createElement("Statistics");
            document.appendChild(root);

            root.setAttribute("timestamp", Long.toString(System.currentTimeMillis()));

            /* Number of models*/
            Element models = (Element) document.createElement("Models");
            CountModel modelCounter = new CountModel();
            int countModels = 0;
            try {
                countModels = modelCounter.count();
            } finally {
                modelCounter.close();
            }
            models.setAttribute("count", countModels + "");
            /* Models Per Algorithm*/
            Element modelsPerAlgorithm = (Element) document.createElement("ModelsPerAlgorithm");

            updateForAlgorithm(document, modelsPerAlgorithm, models, "mlr");
            updateForAlgorithm(document, modelsPerAlgorithm, models, "svm");
            updateForAlgorithm(document, modelsPerAlgorithm, models, "fastRbfNn");
            updateForAlgorithm(document, modelsPerAlgorithm, models, "scaling");
            updateForAlgorithm(document, modelsPerAlgorithm, models, "leverages");

            root.appendChild(models);

            /* Number of tasks*/
            Element tasks = (Element) document.createElement("Tasks");
            CountTasks taskCounter = new CountTasks();
            int countTasks = 0;
            try {
                countTasks = taskCounter.count();
            } finally {
                taskCounter.close();
            }
            tasks.setAttribute("count", countTasks + "");
            root.appendChild(tasks);
           
            updateForTask(document, tasks, "queued");
            updateForTask(document, tasks, "running");
            updateForTask(document, tasks, "completed");
            updateForTask(document, tasks, "error");
            updateForTask(document, tasks, "rejected");

        } catch (ParserConfigurationException ex) {
            Logger.getLogger(DbStatisticsResource.class.getName()).log(Level.SEVERE, null, ex);
        }
        return document;
    }

    private void updateForTask(Document document, Element rootElement, String status) throws DbException {
        CountTasks taskCounter = new CountTasks();
        taskCounter.setWhere(String.format("status='%s'", status.toUpperCase()));
        int countTasks = -1;
        try {
            countTasks = taskCounter.count();
        } finally {
            taskCounter.close();
        }
        Element taskSubElement = (Element) document.createElement("Task");
        taskSubElement.setAttribute("status", status);
        taskSubElement.setTextContent(countTasks + "");
        rootElement.appendChild(taskSubElement);
    }

    private void updateForAlgorithm(Document document, Element modelsPerAlgorithm, Element models, String algorithmId) throws DbException {
        // MLR Models
        int countXModels = 0;
        CountModel modelXCounter = new CountModel();
        modelXCounter.setWhere("algorithm like '%" + algorithmId + "'");
        try {
            countXModels = modelXCounter.count();
        } finally {
            modelXCounter.close();
        }
        Element modelsX = (Element) document.createElement("ForAlgorithm");
        modelsX.setAttribute("algorithm", algorithmId);
        modelsX.setTextContent(countXModels + "");
        modelsPerAlgorithm.appendChild(modelsX);
        models.appendChild(modelsPerAlgorithm);
    }

    @Override
    protected Representation get(Variant variant) throws ResourceException {
        try {
            return new DomRepresentation(MediaType.APPLICATION_XML, doc());
        } catch (DbException ex) {
            throw new ResourceException(500);
        }

    }
}

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
package org.opentox.jaqpot3.www.services;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.opentox.toxotis.core.component.ErrorReport;
import org.opentox.toxotis.core.component.Task;
import org.opentox.toxotis.core.component.Task.Status;
import org.opentox.toxotis.database.engine.error.AddErrorReport;
import org.opentox.toxotis.database.engine.task.AddTask;
import org.opentox.toxotis.database.engine.task.UpdateTask;
import org.opentox.toxotis.database.exception.DbException;

/**
 *
 * @author Pantelis Sopasakis
 * @author Charalampos Chomenides
 */
public abstract class RunnableTaskService implements Runnable {

    protected static void updateFailedTask(Task task, Throwable throwable, String explanation, int httpStatus, String actor) {
        task.getMeta().addDescription("Failed task. " + explanation);
        task.setHttpStatus(httpStatus);
        task.setStatus(Status.ERROR);
        String details = exceptionDetails(throwable);
        String code = "";
        StringBuilder additionalMessage = new StringBuilder();// Detailed message provided to the client (human readable)
        Throwable tempException = throwable;
        /*
         * The detailed message returned to the user is compiled from a the list
         * of (nested) throwables using the method getCause():Trhowable in Throwable
         * and the method getMessage() on each throwable on the thread.
         */
        while (tempException != null) {
            additionalMessage.append(tempException.getMessage() != null ? tempException.getMessage() : "");
            tempException = tempException.getCause();
        }
        if (explanation == null) {
            explanation = "No explanation provided (Please report the issue to the service administrators)";
        }
        ErrorReport er = new ErrorReport(httpStatus, actor, explanation + additionalMessage, details, code);
        task.setErrorReport(er);

        AddErrorReport addError = new AddErrorReport(task.getErrorReport());
        try {
            addError.write();
        } catch (DbException ex) {
            Logger.getLogger(RunnableTaskService.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                addError.close();
            } catch (DbException ex) {
                Logger.getLogger(RunnableTaskService.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        UpdateTask updater = new UpdateTask(task);
        updater.setUpdateErrorReport(true);
        updater.setUpdateTaskStatus(true);
        updater.setUpdateMeta(true);
        try {
            updater.update();
            updater.close();
        } catch (DbException ex) {
            Logger.getLogger(RunnableTaskService.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    protected static void updateFailedTask(Task task, ErrorReport er) {
        task.getMeta().addDescription("Failed task. " + er.getMessage());
        task.setHttpStatus(er.getHttpStatus());
        task.setStatus(Status.ERROR);
        task.setErrorReport(er);
        AddErrorReport addError = new AddErrorReport(task.getErrorReport());
        try {
            addError.write();
        } catch (DbException ex) {
            Logger.getLogger(RunnableTaskService.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                addError.close();
            } catch (DbException ex) {
                Logger.getLogger(RunnableTaskService.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        UpdateTask updater = new UpdateTask(task);
        updater.setUpdateErrorReport(true);
        updater.setUpdateTaskStatus(true);
        updater.setUpdateMeta(true);
        try {
            updater.update();
            updater.close();
        } catch (DbException ex) {
            Logger.getLogger(RunnableTaskService.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    protected static String exceptionDetails(Throwable ex) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        ex.printStackTrace(pw);

        Throwable currentThrowable = ex.getCause();
        while (currentThrowable != null) {
            pw.append("\nPrevious Stack Trace...\n");
            currentThrowable.printStackTrace(pw);
            currentThrowable = currentThrowable.getCause();
        }

        return sw.toString();
    }
}

package org.opentox.jaqpot3.www.services;

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

    protected static String exceptionDetails(Throwable ex) {
        StringBuilder sb = new StringBuilder();
        StackTraceElement[] elements = ex.getStackTrace();
        final String newLine = "\n";
        for (StackTraceElement element : elements) {
            sb.append(element);
            sb.append(newLine);
        }
        return sb.toString();
    }
}

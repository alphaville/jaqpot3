package org.opentox.jaqpot3.util;

import java.util.Date;
import java.util.UUID;
import org.opentox.toxotis.client.VRI;
import org.opentox.toxotis.core.component.Task;
import org.opentox.toxotis.ontology.LiteralValue;
import org.opentox.toxotis.util.aa.TokenPool;
import org.opentox.toxotis.core.component.User;

/**
 *
 * @author Pantelis Sopasakis
 * @author Charalampos Chomenides
 */
public class TaskFactory {

    public static Task newQueuedTask(){
        User loggedUser = TokenPool.getInstance().getLoggedIn().iterator().next();
        return newQueuedTask(loggedUser);
    }

    public static Task newQueuedTask(User createdBy){
        UUID uuid = UUID.randomUUID();
        return newQueuedTask(createdBy, uuid);
    }

    public static Task newQueuedTask(User createdBy, UUID uuid){
        String taskId = uuid.toString();
        return newQueuedTask(createdBy, Configuration.getBaseUri().augment("task", taskId));
    }

    public static Task newQueuedTask(User createdBy, VRI vri){
        Task task = new Task(vri);
        task.setCreatedBy(createdBy);
        task.setStatus(Task.Status.QUEUED);
        task.setPercentageCompleted(0.0f);
        task.setHttpStatus(202);
        if (createdBy != null) {
            task.getMeta().addCreator(createdBy.getUid());
        }
//        task.getMeta().setDate(new LiteralValue(new Date(System.currentTimeMillis())));
        return task;
    }

}

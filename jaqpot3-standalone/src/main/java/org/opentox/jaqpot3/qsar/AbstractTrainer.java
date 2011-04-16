package org.opentox.jaqpot3.qsar;

import java.util.UUID;
import org.opentox.toxotis.core.component.Task;
import org.opentox.toxotis.util.aa.AuthenticationToken;

/**
 *
 * @author Pantelis Sopasakis
 * @author Charalampos Chomenides
 */
public abstract class AbstractTrainer implements ITrainer {

    private boolean enabled = true;
    private boolean synch = true;
    private Task task;
    protected AuthenticationToken token;
    private UUID uuid = UUID.randomUUID();
    

    @Override
    public ITrainer setTask(Task task) {
        this.task = task;
        return this;
    }

    @Override
    public Task getTask() {
        return this.task;
    }

    @Override
    public ITrainer setToken(AuthenticationToken token) {
        this.token = token;
        return this;
    }

    protected UUID getUuid() {
        return this.uuid;
    }

    @Override
    public boolean needsDataset(){
        return true;
    }
}

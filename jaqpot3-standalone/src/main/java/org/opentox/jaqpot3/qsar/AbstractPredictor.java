package org.opentox.jaqpot3.qsar;

import org.opentox.toxotis.core.component.Model;
import org.opentox.toxotis.core.component.Task;
import org.opentox.toxotis.util.aa.AuthenticationToken;

/**
 *
 * @author Pantelis Sopasakis
 * @author Charalampos Chomenides
 */
public abstract class AbstractPredictor implements IPredictor {

    private boolean enabled = true;
    private boolean synch = true;
    private Task task;
    protected AuthenticationToken token;
    protected Model model;

    public AbstractPredictor() {
    }

    @Override
    public IPredictor setModel(Model model) {
        this.model = model;
        return this;
    }

    @Override
    public IPredictor setTask(Task task) {
        this.task = task;
        return this;
    }

    @Override
    public Task getTask() {
        return this.task;
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isSynchronized() {
        return this.synch;
    }

    public void setSynchronized(boolean synch) {
        this.synch = synch;
    }

    @Override
    public IPredictor setToken(AuthenticationToken token) {
        this.token = token;
        return this;
    }

    @Override
    public Model getModel() {
        return model;
    }
}

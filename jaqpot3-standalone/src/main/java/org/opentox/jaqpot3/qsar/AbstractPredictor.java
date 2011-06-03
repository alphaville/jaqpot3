package org.opentox.jaqpot3.qsar;

import org.opentox.jaqpot3.exception.JaqpotException;
import org.opentox.toxotis.client.VRI;
import org.opentox.toxotis.core.component.Dataset;
import org.opentox.toxotis.core.component.Model;
import org.opentox.toxotis.core.component.Task;
import org.opentox.toxotis.exceptions.impl.ServiceInvocationException;
import org.opentox.toxotis.exceptions.impl.ToxOtisException;
import org.opentox.toxotis.util.aa.AuthenticationToken;
import org.opentox.toxotis.util.arff.ArffDownloader;
import weka.core.Instances;

/**
 *
 * @author Pantelis Sopasakis
 * @author Charalampos Chomenides
 */
public abstract class AbstractPredictor implements IPredictor {

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

    @Override
    public IPredictor setToken(AuthenticationToken token) {
        this.token = token;
        return this;
    }

    @Override
    public Model getModel() {
        return model;
    }

    @Override
    public Dataset predict(VRI input) throws JaqpotException {
        ArffDownloader downloader = new ArffDownloader(input);
        Instances inst = downloader.getInstances();
        if (inst != null) { // the dataset is available in text/x-arff directly
            return predict(inst);
        } else { // The instances object has to be retrieved from the RDF format
            try {
                return predict(new Dataset(input).loadFromRemote());
            } catch (ToxOtisException ex) {
                throw new JaqpotException(ex);
            } catch (ServiceInvocationException ex) {
                throw new JaqpotException(ex);
            }
        }
    }

    @Override
    public Dataset predict(Dataset data) throws JaqpotException {
        return predict(data.getInstances());
    }
}

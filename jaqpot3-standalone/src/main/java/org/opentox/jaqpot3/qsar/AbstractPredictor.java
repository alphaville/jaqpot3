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
package org.opentox.jaqpot3.qsar;

import com.sun.org.apache.bcel.internal.util.ByteSequence;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.xml.transform.sax.SAXSource;
import org.apache.commons.lang.StringUtils;
import org.dmg.pmml.DataDictionary;
import org.dmg.pmml.DataField;
import org.dmg.pmml.DerivedField;
import org.dmg.pmml.PMML;
import org.dmg.pmml.TransformationDictionary;
import org.jpmml.evaluator.FieldValue;
import org.jpmml.model.ImportFilter;
import org.jpmml.model.JAXBUtil;
import org.opentox.jaqpot3.exception.JaqpotException;
import org.opentox.jaqpot3.qsar.exceptions.BadParameterException;
import org.opentox.jaqpot3.qsar.exceptions.QSARException;
import org.opentox.jaqpot3.qsar.util.AttributeCleanup;
import static org.opentox.jaqpot3.qsar.util.AttributeCleanup.AttributeType.nominal;
import static org.opentox.jaqpot3.qsar.util.AttributeCleanup.AttributeType.numeric;
import static org.opentox.jaqpot3.qsar.util.AttributeCleanup.AttributeType.string;
import org.opentox.jaqpot3.qsar.util.ExpressionUtilExtended;
import org.opentox.jaqpot3.qsar.util.LocalEvaluationContext;
import org.opentox.jaqpot3.qsar.util.WekaInstancesProcess;
import static org.opentox.jaqpot3.qsar.util.WekaInstancesProcess.addNewAttribute;
import static org.opentox.jaqpot3.qsar.util.WekaInstancesProcess.getInstanceAttributeValues;
import org.opentox.toxotis.client.VRI;
import org.opentox.toxotis.core.component.Dataset;
import org.opentox.toxotis.core.component.Feature;
import org.opentox.toxotis.core.component.Model;
import org.opentox.toxotis.core.component.Task;
import org.opentox.toxotis.exceptions.impl.ServiceInvocationException;
import org.opentox.toxotis.exceptions.impl.ToxOtisException;
import org.opentox.toxotis.util.aa.AuthenticationToken;
import org.opentox.toxotis.util.arff.ArffDownloader;
import org.xml.sax.InputSource;
import weka.core.Instances;

/**
 *
 * @author Pantelis Sopasakis
 * @author Charalampos Chomenides
 */
public abstract class AbstractPredictor implements IPredictor {

    protected byte[] pmml;
    protected PMML pmmlObject;
    protected Instances justCompounds;
    private Task task;
    protected AuthenticationToken token;
    protected Model model;
    protected List<Feature> independentFeatures = new ArrayList<Feature>();
    protected Feature dependentFeature;

    public AbstractPredictor() {
    }

    
    public Instances preprocessDataset(Instances inst) throws JaqpotException {
        
        independentFeatures = model.getIndependentFeatures();
        dependentFeature = model.getDependentFeatures().get(0);
        justCompounds = WekaInstancesProcess.loadJustCompounds(inst);
        
        WekaInstancesProcess.toCSV(inst, "C:\\Users\\philip\\Downloads\\New MLR\\predict\\beforePredictNewOriginal.csv");
                
        if(model.getActualModel()!=null) {
            pmml = model.getActualModel().getPmml();
            if(pmml!=null) {
                
                loadPMMObject();
                //IMPORTANT!!!! WekaInstancesProcess.getFilteredInstances removes compound URI that is needed
                
                //TODO check Spot for MVH
                //inst = WekaInstancesProcess.handleMissingValues(inst, ClientParams);
                inst = WekaInstancesProcess.transformDataset(inst,pmmlObject);
            }
        }
        return inst;
    }    
    
    private void loadPMMObject() throws JaqpotException{
        try {    
            InputStream is = new ByteSequence(pmml);
            InputSource source = new InputSource(is);

            SAXSource transformedSource = ImportFilter.apply(source);
            pmmlObject = JAXBUtil.unmarshalPMML(transformedSource);
        } catch (Exception ex) {
            String message = "Exception while loading PMML to object";
            throw new JaqpotException(message, ex);
        }
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
            inst = preprocessDataset(inst);
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

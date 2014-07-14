/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.opentox.jaqpot3.qsar.serializable;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import org.opentox.toxotis.core.component.ActualModel;
import org.opentox.toxotis.client.VRI;

/**
 *
 * @author philip
 */
public class CustomAlgorithmModel extends ActualModel{
    
    private VRI datasetReference;
    VRI modelDescr1VRI = null;
    VRI modelDescr2VRI = null;
    VRI predictionfeatureUri = null;
    Map diffVRI = null;
    Map divisionVRI = null;
    VRI sign1VRI = null;
    VRI sign2VRI = null;
    VRI magn1VRI = null;
    VRI magn2VRI = null;

    
    public CustomAlgorithmModel() {
        diffVRI = new HashMap();
    }

    
    public VRI getModelDescr1VRI() {
        return modelDescr1VRI;
    }

    public void setModelDescr1VRI(VRI modelDescr1VRI) {
        this.modelDescr1VRI = modelDescr1VRI;
    }

    public VRI getModelDescr2VRI() {
        return modelDescr2VRI;
    }

    public void setModelDescr2VRI(VRI modelDescr2VRI) {
        this.modelDescr2VRI = modelDescr2VRI;
    }
    
    public VRI getDatasetReference() {
        return datasetReference;
    }

    public void setDatasetReference(VRI datasetReference) {
        this.datasetReference = datasetReference;
    }

    public VRI getPredictionfeatureUri() {
        return predictionfeatureUri;
    }

    public void setPredictionfeatureUri(VRI predictionfeatureUri) {
        this.predictionfeatureUri = predictionfeatureUri;
    }

    public VRI getSign1VRI() {
        return sign1VRI;
    }

    public void setSign1VRI(VRI sign1VRI) {
        this.sign1VRI = sign1VRI;
    }

    public VRI getSign2VRI() {
        return sign2VRI;
    }

    public void setSign2VRI(VRI sign2VRI) {
        this.sign2VRI = sign2VRI;
    }

    public VRI getMagn1VRI() {
        return magn1VRI;
    }

    public void setMagn1VRI(VRI magn1VRI) {
        this.magn1VRI = magn1VRI;
    }

    public VRI getMagn2VRI() {
        return magn2VRI;
    }

    public void setMagn2VRI(VRI magn2VRI) {
        this.magn2VRI = magn2VRI;
    }

    public Map getDiffVRI() {
        return diffVRI;
    }

    public void setDiffVRI(Map diffVRI) {
        this.diffVRI = diffVRI;
    }

    public Map getDivisionVRI() {
        return divisionVRI;
    }

    public void setDivisionVRI(Map divisionVRI) {
        this.divisionVRI = divisionVRI;
    }
    
    
}

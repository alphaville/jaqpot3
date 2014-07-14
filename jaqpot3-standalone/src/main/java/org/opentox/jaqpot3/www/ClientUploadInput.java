/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.opentox.jaqpot3.www;

import java.util.List;
import org.opentox.jaqpot3.qsar.IClientInput;
import org.restlet.data.CharacterSet;
import org.restlet.data.Form;
import org.restlet.data.Parameter;
import org.restlet.representation.Representation;

/**
 *
 * @author philip
 */
public class ClientUploadInput extends Form implements IClientInput {
    
    private String uploadFilename;
    private byte[] uploadBytes;
    
    public ClientUploadInput(String parametersString, CharacterSet characterSet, char separator) {
        super(parametersString, characterSet, separator);
    }

    public ClientUploadInput(String queryString, CharacterSet characterSet) {
        super(queryString, characterSet);
    }

    public ClientUploadInput(String parametersString, char separator) {
        super(parametersString, separator);
    }

    public ClientUploadInput(String queryString) {
        super(queryString);
    }

    public ClientUploadInput(Representation webForm) {
        super(webForm);
    }

    public ClientUploadInput(List<Parameter> delegate) {
        super(delegate);
    }

    public ClientUploadInput(int initialCapacity) {
        super(initialCapacity);
    }

    public ClientUploadInput() {
    }
    
    @Override
    public byte[] getUploadBytes(){
        return this.uploadBytes;
    }
    
    @Override
    public String getUploadFilename(){
        return this.uploadFilename;
    }
     
    public void setUploadContent(String filename,byte[] content){
        this.uploadFilename = filename;
        this.uploadBytes = content;
    }
    
    @Override
    public String getFirstValue(String name) {
        return super.getFirstValue(name);
    }       
}

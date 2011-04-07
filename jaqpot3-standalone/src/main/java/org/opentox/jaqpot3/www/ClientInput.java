package org.opentox.jaqpot3.www;

import java.util.List;
import org.opentox.jaqpot3.qsar.IClientInput;
import org.restlet.data.CharacterSet;
import org.restlet.data.Form;
import org.restlet.data.Parameter;
import org.restlet.representation.Representation;

/**
 *
 * @author Pantelis Sopasakis
 * @author Charalampos Chomenides
 */
public class ClientInput extends Form implements IClientInput {

    public ClientInput(String parametersString, CharacterSet characterSet, char separator) {
        super(parametersString, characterSet, separator);
    }

    public ClientInput(String queryString, CharacterSet characterSet) {
        super(queryString, characterSet);
    }

    public ClientInput(String parametersString, char separator) {
        super(parametersString, separator);
    }

    public ClientInput(String queryString) {
        super(queryString);
    }

    public ClientInput(Representation webForm) {
        super(webForm);
    }

    public ClientInput(List<Parameter> delegate) {
        super(delegate);
    }

    public ClientInput(int initialCapacity) {
        super(initialCapacity);
    }

    public ClientInput() {
    }

    @Override
    public String getFirstValue(String name) {
        return super.getFirstValue(name);
    }


}

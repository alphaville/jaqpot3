package org.opentox.jaqpot3.resources.options;

import org.opentox.toxotis.core.IRestOperation;
import org.opentox.toxotis.core.component.Algorithm;
import org.opentox.toxotis.core.component.HttpStatus;
import org.opentox.toxotis.core.component.RestOperation;
import org.opentox.toxotis.core.component.ServiceRestDocumentation;
import org.opentox.toxotis.ontology.collection.HttpMethods.MethodsEnum;
import org.opentox.toxotis.ontology.collection.OTRestClasses;
import org.opentox.toxotis.ontology.impl.MetaInfoImpl;

/**
 *
 * @author Pantelis Sopasakis
 * @author Charalampos Chomenides
 */
public class AlgorithmOptionsHelper {

    public static IRestOperation getAlgorithm() {
        IRestOperation rest = new RestOperation();
        rest.setMethod(MethodsEnum.GET);
        rest.addHttpStatusCodes(
                new HttpStatus(OTRestClasses.STATUS_200()).setMeta(new MetaInfoImpl().addDescription("The GET method succeeds and "
                + "a representation of the algorithm is returned to the client in the prefered MIME type")),
                new HttpStatus(OTRestClasses.STATUS_401()).setMeta(new MetaInfoImpl().addDescription("The user is authenticated but not authorized to access the "
                + "underlying resource")),
                new HttpStatus(OTRestClasses.STATUS_403()).setMeta(new MetaInfoImpl().addDescription("Forbidden action and access")),
                new HttpStatus(OTRestClasses.STATUS_500()).setMeta(new MetaInfoImpl().addDescription("The GET method fails and returns an error status "
                + "code 500 in case some unexplicable error inhibits the algorithm to be accessed.")));
        rest.addRestClasses(OTRestClasses.GET_Algorithm(), OTRestClasses.OperationAlgorithm(), OTRestClasses.OperationResultAlgorithm());
        return rest;
    }

    public static IRestOperation postAlgorithm(Algorithm algorithm) {
        return null;
    }

    public static ServiceRestDocumentation algorithmOptions(Algorithm algorithm) {
        return null;
    }
}

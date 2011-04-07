package org.opentox.jaqpot3.www;

import org.restlet.Application;
import org.restlet.Context;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.Language;
import org.restlet.ext.wadl.ApplicationInfo;
import org.restlet.ext.wadl.DocumentationInfo;
import org.restlet.ext.wadl.WadlApplication;
import org.restlet.routing.Router;
import org.restlet.routing.Template;

/**
 *
 * @author Pantelis Sopasakis
 * @author Charalampos Chomenides
 */
public class JaqpotWebApplication extends WadlApplication {

    public JaqpotWebApplication() {
        super();
        setName("JaQPOT");
        setDescription("JaQPOT Training and Prediction Services");
        setOwner("part of the OpenTox project - http://opentox.org");
        setAuthor("kinkyDesign");
    }


    @Override
    public ApplicationInfo getApplicationInfo(Request request, Response response) {
        ApplicationInfo result = super.getApplicationInfo(request, response);
        DocumentationInfo docInfo = new DocumentationInfo(getName());
        docInfo.setTitle(getName());
        docInfo.setLanguage(Language.ENGLISH);
        docInfo.setTextContent(getDescription());
        result.setDocumentation(docInfo);

        return result;
    }

    public ApplicationInfo getAppInfo(Request request, Response response) {
        return getApplicationInfo(request, response);
    }

    final static public class YaqpRouter extends Router {

        public YaqpRouter(Context context) {
            super(context);
            setDefaultMatchingMode(Template.MODE_STARTS_WITH);
            setRoutingMode(Router.MODE_BEST_MATCH);

        }
    }
}

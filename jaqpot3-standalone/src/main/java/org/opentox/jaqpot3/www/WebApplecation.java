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
package org.opentox.jaqpot3.www;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.SimpleFormatter;
import org.opentox.jaqpot3.resources.*;
import org.opentox.jaqpot3.util.Configuration;
import org.opentox.jaqpot3.util.DatabaseJanitor;
import org.opentox.jaqpot3.www.guard.OpenSSOAuthorizer;
import org.opentox.toxotis.database.exception.DbException;
import org.opentox.toxotis.database.pool.DataSourceFactory;
import org.opentox.toxotis.util.aa.SSLConfiguration;
import org.restlet.Component;
import org.restlet.Context;
import org.restlet.Restlet;
import org.restlet.Server;
import org.restlet.data.Protocol;
import org.restlet.routing.Filter;
import org.restlet.routing.Router;
import org.restlet.service.TunnelService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Pantelis Sopasakis
 * @author Charalampos Chomenides
 */
final public class WebApplecation extends JaqpotWebApplication {

    private static Logger logger = LoggerFactory.getLogger(WebApplecation.class);
    public static final String keyOut_sad = System.console() != null ? "\033[2;35m  [jaqpot] :-( \033[0m" : "  [jaqpot] :-( ";
    public static final String keyOut_normal = System.console() != null ? "\033[3;33m  [jaqpot] :-| \033[0m" : "  [jaqpot] :-| ";
    public static final String keyOut_happy = System.console() != null ? "\033[3;34m  [jaqpot] :-) \033[0m" : "  [jaqpot] :-) ";
    public static final String keyOut_strange = System.console() != null ? "\033[2;36m  [jaqpot] :-/ \033[0m" : "  [jaqpot] :-/ ";
    public static final String done = "[DONE]";
    public static final String fail = System.console() != null ? "\033[1;31m[FAIL]\033[0m" : "[FAIL]";
    private static Component component;

    static {

        ClassLoader cl = WebApplecation.class.getClassLoader();
        org.apache.log4j.LogManager.resetConfiguration();
        org.apache.log4j.PropertyConfigurator.configure(cl.getResource("log4j.properties"));

        /*
         * Configure Database
         */
        Properties props = new Properties();
        InputStream propertiesIn = cl.getResourceAsStream("c3p0.properties");
        try {
            props.load(propertiesIn);
        } catch (IOException ex) {
            java.util.logging.Logger.getLogger(WebApplecation.class.getName()).log(Level.SEVERE, null, ex);
        }
        try {
            propertiesIn.close();
        } catch (IOException ex) {
            java.util.logging.Logger.getLogger(WebApplecation.class.getName()).log(Level.SEVERE, null, ex);
        }

        System.out.println("*** KEY : " + props.getProperty("key"));
        org.opentox.toxotis.database.global.DbConfiguration.getInstance().setProperties(props);

        DataSourceFactory f = DataSourceFactory.getInstance();
        try {
            try {
                f.getConnection().close();
            } catch (SQLException ex) {
                java.util.logging.Logger.getLogger(WebApplecation.class.getName()).log(Level.SEVERE, null, ex);
            }
        } catch (DbException ex) {
            java.util.logging.Logger.getLogger(WebApplecation.class.getName()).log(Level.SEVERE, null, ex);
        }

        //TODO: Cleanup database


        Connection connection = null;
        try {
            connection = f.getConnection();
            Statement statement = connection.createStatement();
            statement.executeUpdate("DELETE FROM Task WHERE status='QUEUED' OR status='RUNNING'");

        } catch (SQLException ex) {
            java.util.logging.Logger.getLogger(WebApplecation.class.getName()).log(Level.SEVERE, null, ex);
        } catch (DbException ex) {
            java.util.logging.Logger.getLogger(WebApplecation.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException ex) {
                    java.util.logging.Logger.getLogger(WebApplecation.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
        System.out.print(keyOut_normal + "Database Janitor is in place and started working ");
        DatabaseJanitor.work();
        System.out.print(done + "\n");

    }

    public WebApplecation() throws IOException {
        super();
        FileHandler fh = new FileHandler("logging/application.log", true);
        fh.setFormatter(new SimpleFormatter());
        Context.getCurrentLogger().addHandler(fh);
        Context.getCurrentLogger().setLevel(Level.INFO);
        setTunnelService(new TunnelService(true, true, true, true, true, true, true));
    }

    @Override
    public Restlet createInboundRoot() {
        Router router = new YaqpRouter(this.getContext().createChildContext());
        router.attachDefault(IndexResource.class);
        router.attach(BibTexResource.template.toString(), BibTexResource.class);
        router.attach(AlgorithmsResource.template.toString(), AlgorithmsResource.class);
        router.attach(AlgorithmResource.template.toString(), AlgorithmResource.class);
        router.attach(TasksResource.template.toString(), TasksResource.class);
        router.attach(TaskResource.template.toString(), TaskResource.class);
        router.attach(ModelsResource.template.toString(), ModelsResource.class);
        router.attach(ErrorResource.template.toString(), ErrorResource.class);
        router.attach(ErrorsResource.template.toString(), ErrorsResource.class);
        router.attach(DbStatisticsResource.template.toString(), DbStatisticsResource.class);
        router.attach(RescueResource.template.toString(), RescueResource.class);
        router.attach(LoginResource.template.toString(), LoginResource.class);
        router.attach(UserQuotaResource.template.toString(), UserQuotaResource.class);
        router.attach(ParametersResource.template.toString(), ParametersResource.class);
        router.attach(ParameterResource.template.toString(), ParameterResource.class);
        router.attach(ModelMetaResource.template.toString(), ModelMetaResource.class);
        router.attach("/test", TestResource.class);
        router.attach("/iface/policy", PolicyCreatorResource.class);
        router.attach("/iface/generic", TrainGeneric.class);
        router.attach("/iface/bibtex", BibTeXInterface.class);
        router.attach("/iface/validation", ValidationInterfaceResource.class);
        router.attach(ListInterfaces.template.toString(), ListInterfaces.class);
        router.attach(PolicyListResource.template.toString(), PolicyListResource.class);
        router.attach(PolicyResource.template.toString(), PolicyResource.class);

        protectResource(router, BibTexAllResource.class, false, true);
        protectResource(router, ModelResource.class, true, true);
        protectResource(router, ShutDownResource.class, true, true);
        protectResource(router, UserResource.class, true, true);
        protectResource(router, UsersResource.class, true, true);


        return router;
    }

    private void protectResource(Router baseRouter, Class<? extends JaqpotResource> toBeAttached, boolean protectGet, boolean protectPost) {
        try {
            String templatedUri = toBeAttached.getField("template").get(null).toString();
            Router protectedRouter = new Router(getContext());
            protectedRouter.attachDefault(toBeAttached);
            protectedRouter.attach(templatedUri, toBeAttached);
            Filter openssoAuthz = new OpenSSOAuthorizer(protectGet, protectPost);
            openssoAuthz.setNext(protectedRouter);
            baseRouter.attach(templatedUri, openssoAuthz);
        } catch (Exception ex) {
            logger.error("", ex);
            throw new RuntimeException(ex);
        }
    }

    public static void main(String... args) throws Exception {

        new WebApplecation();
        SSLConfiguration.initializeSSLConnection();
        Thread shutDownHook = new Thread("shut-down-hook-jaqpot") {
            @Override
            public void run() {
                System.out.print(keyOut_strange + "Stopping web server ");
                if (component != null) {
                    try {
                        component.stop();
                        System.out.print(done + "\n");
                        System.out.print(keyOut_strange + "Logging out ");
                        //TokenPool.getInstance().logoutAll();
                        System.out.print(done + "\n");
                    } catch (Exception ex) {
                        logger.error("Exception caught on shutdown", ex);
                        System.out.print(fail + "\n");
                    }
                }
                System.out.print(keyOut_strange + "Disconnecting from the database ");

                System.out.print(done + "\n");
                System.out.println(keyOut_happy + "bye!");
            }
        };
        Runtime.getRuntime().addShutdownHook(shutDownHook);
        component = new JaqpotComponent();
        final Server server = component.getServers().add(Protocol.HTTP, Configuration.SERVER_PORT);
        component.start();
        System.out.println(keyOut_happy + "Server started on port " + server.getPort());

    }
}

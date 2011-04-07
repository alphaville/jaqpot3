package org.opentox.jaqpot3.util;

import java.net.URISyntaxException;
import org.opentox.jaqpot3.exception.JaqpotException;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.jar.Manifest;
import org.opentox.toxotis.client.VRI;
import org.opentox.toxotis.util.LoggingConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Pantelis Sopasakis
 * @author Charalampos Chomenides
 */
public class Configuration {

    private static Logger logger = LoggerFactory.getLogger(Configuration.class);
    private static Properties properties = null;
    public static final String BASE_URI = "http://" + getProperties().getProperty("server.domainName")
            + ":" + getProperties().getProperty("server.port") + "/" + getProperties().getProperty("url.suffix", "jaqpot");
    public static final int SERVER_PORT = Integer.parseInt(getProperties().getProperty("server.port"));
    
    private static String version = null;

    public static String getVersion() {
        if (version == null) {
            InputStream is = null;
            try {
                is = Configuration.class.getClassLoader().getResourceAsStream("META-INF/MANIFEST.MF");
                Manifest manifest = new Manifest(is);
                version = manifest.getMainAttributes().getValue("Package-Version");
            } catch (IOException ex) {
                logger.error(null, ex);
            } finally {
                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException ex) {
                        logger.error("Cannot close input stream from META-INF/MANIFEST.MF", ex);
                    }
                }
            }
        }
        return version;
    }

    public static VRI getBaseUri() {
        try {
            return new VRI(BASE_URI);
        } catch (URISyntaxException ex) {
            logger.error("Provided base URI '" + BASE_URI + "' is not valid", ex);
            throw new RuntimeException(ex);
        }
    }

    public static String getStringProperty(String key) {
        try {
            return properties.getProperty(key);
        } catch (RuntimeException ex) {
            logger.warn(null, ex);
            return null;
        }
    }

    public static int getIntegerProperty(String key) {
        return Integer.parseInt(properties.getProperty(key));
    }

    public static int getIntegerProperty(String key, int defaultVal) {
        try {
            return Integer.parseInt(properties.getProperty(key));
        } catch (NumberFormatException ex) {
            logger.trace(null, ex);
            return defaultVal;
        }
    }

    public static double getDoubleProperty(String key) throws IllegalArgumentException {
        try {
            return Double.parseDouble(properties.getProperty(key));
        } catch (final NullPointerException npe) {
            logger.warn(null, npe);
            throw new IllegalArgumentException("No such property");
        }
    }

    public static double getDoubleProperty(String key, double defaultVal) {
        try {
            return getDoubleProperty(getProperties().getProperty(key));
        } catch (final NumberFormatException ex) {
            logger.trace(null, ex);
            return defaultVal;
        } catch (final IllegalArgumentException iae) {
            logger.trace(null, iae);
            return defaultVal;
        }
    }

    public static boolean getBooleanProperty(String key) throws IllegalArgumentException {
        String val;
        if ((val = getProperties().getProperty(key)) != null) {
            if (val.equalsIgnoreCase("false")) {
                return false;
            } else if (val.equalsIgnoreCase("true")) {
                return true;
            }
        }
        throw new IllegalArgumentException("Boolean value not found");
    }

    public static boolean getBooleanProperty(String key, boolean defaultVal) {
        try {
            return getBooleanProperty(key);
        } catch (final IllegalArgumentException iae) {
            logger.trace(null, iae);
            return defaultVal;
        }
    }

    public static Properties getProperties() {
        if (properties == null) {
            try {
                properties = loadDefaultProperties();
            } catch (JaqpotException ex) {
                logger.warn("Unable to load properties from file server.properties. Using backup properties", ex);
                backupProperties();
            }
        }
        return properties;
    }

    public static void reloadProperties() {
        try {
            properties = loadDefaultProperties();
        } catch (JaqpotException ex) {
            backupProperties();
        }
    }

    // <editor-fold defaultstate="collapsed" desc="load the Default Properies">
    public static Properties loadDefaultProperties() throws JaqpotException {
        java.io.InputStream inStr = null;
        try {
            properties = new Properties();
            if (System.getProperty("os.name").contains("Linux")) {
                inStr = Configuration.class.getClassLoader().getResourceAsStream("server.properties");
                properties.load(inStr);
            } else if (System.getProperty("os.name").contains("Mac OS")) {
                inStr = Configuration.class.getClassLoader().getResourceAsStream("macos.server.properties");
                properties.load(inStr);
            }
            properties.setProperty("log4j.useDefaultFile", "true");
        } catch (final IOException ex) {
            ex.printStackTrace();
            logger.warn("IOException while trying to access configuration file.", ex);
            backupProperties();
        } finally {
            if (inStr != null) {
                try {
                    inStr.close();
                } catch (IOException ex) {
                    logger.error(null, ex);
                }
            }
            if (properties != null) {
                return properties;
            } else {
                String message = "Could not load the standard properties hence could not use the standard "
                        + "logger - Using the console instead!";
                throw new JaqpotException(message);
            }
        }
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="load Properies from a file">
    public static Properties loadProperties(String propertiesFile) throws JaqpotException {
        java.io.InputStream inStr = null;
        try {
            inStr = new java.io.FileInputStream(propertiesFile);
            properties.load(inStr);
            properties.setProperty("log4j.useCustomFile", "true");
        } catch (final Exception ex) {
            properties = loadDefaultProperties();
            properties.setProperty("log4j.useCustomFile", "false");
        } finally {
            if (inStr != null) {
                try {
                    inStr.close();
                } catch (IOException ex) {
                    logger.error(null, ex);
                }
            }
        }

        if (properties != null) {
            return properties;
        } else {
            String message = "Could not load the standard properties hence could not use the standard "
                    + "logger - Using the console instead";
            throw new JaqpotException(message);
        }

    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Configure">
    public static void configure(Properties properties) throws JaqpotException {
        if (properties != null) {
            Configuration.properties = properties;
        } else {
            String message = "Could not load the standard properties hence could not use the standard "
                    + "logger - Using the console instead";
            throw new JaqpotException(message);
        }
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Backup Properties in case of emergency">
    private static void backupProperties() {
        properties = new Properties();
        properties.setProperty("server.port", "3000");
        properties.setProperty("server.domainName", "opentox.ntua.gr");
        properties.setProperty("log4j.rootCategory", "WARN, file");
        properties.setProperty("log4j.appender.file", "org.apache.log4j.FileAppender");
        properties.setProperty("log4j.appender.file.File", "yaqp.log");
        properties.setProperty("log4j.appender.file.layout", "org.apache.log4j.PatternLayout");
        properties.setProperty("log4j.appender.file.layout.ConversionPattern", "%d [%t] %-5p %c - %m%n");
        properties.setProperty("log4j.useDefaultFile", "false");
    }// </editor-fold>
}

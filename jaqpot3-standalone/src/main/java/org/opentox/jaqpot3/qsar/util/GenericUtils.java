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
 * Copyright (C) 2009-2014 Pantelis Sopasakis & Charalampos Chomenides & Lampovas Nikolaos
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

package org.opentox.jaqpot3.qsar.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Properties;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.opentox.jaqpot3.www.WebApplecation;
import org.opentox.toxotis.core.component.Model;
import org.opentox.toxotis.ontology.LiteralValue;

/**
 *
 * @author Pantelis Sopasakis
 * @author Charalampos Chomenides
 * @author Nikolaos Lampovas
 */
public class GenericUtils {

    public static String getRegex(String pattern,String matcher) {
        String res = "";
        Pattern p = Pattern.compile(pattern);
        Matcher m = p.matcher(matcher);

        if (m.find()) {
            res = m.group(1);
        }
        return res;
    }
    
    public static String getPropertyValue(String key) {
        ClassLoader cl = WebApplecation.class.getClassLoader();
        
        Properties props = new Properties();
        InputStream propertiesIn = cl.getResourceAsStream("server.properties");
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
        return props.getProperty("debug");
    }
    
    public static String getDebugFilename(Model model,Boolean Enm) {
        String preproc ="",datasetUri="";
        
        String algUri = model.getAlgorithm().getUri().toString();
        
        String algTitle = algUri.split("algorithm/")[1];
        
        if(model.getActualModel().hasScaling()) {
            preproc+="_Scaling";
        } else if(model.getActualModel().hasNormalization()) {
            preproc+="_Norm";
        }
        
        if(model.getActualModel().getPmml()!=null) {
            preproc+="_PMML";
        }
        datasetUri = model.getDataset().getUri();
        if(Enm) {
            String dsUriEnm = datasetUri.split("substanceowner/")[1];
            datasetUri = dsUriEnm.split("/dataset")[0];
        } else {
            datasetUri = datasetUri.split("dataset/")[1];
        }
        return getRootCustomPath("dbg")+"\\"+datasetUri+"_"+algTitle+preproc+".csv";
    }
    public static String getRootCustomPath(String child) {
        String res="";
        try {
            File f = new File(".");
            String path = f.getCanonicalPath()+"\\"+child;
            File check = new File(path);
            if(!check.isDirectory()) {
                new File(path).mkdir();
                res = path;
            } else {
                res = check.getCanonicalPath();
            }
        } catch (Exception ex) {
            
        }
        return res;
    }
}

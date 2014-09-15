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


package org.opentox.jaqpot3.resources.publish;

import java.util.Arrays;
import java.util.List;
import org.opentox.toxotis.core.html.GoogleAnalytics;
import org.restlet.data.MediaType;
/**
 *
 * @author Pantelis Sopasakis
 * @author Charalampos Chomenides
 */
public abstract class AbstractPublishable implements Publishable{

    private final List<String> headComponents = Arrays.asList(
        "<link rel=\"stylesheet\" type=\"text/css\" href=\"/static/styles.css\">",
            GoogleAnalytics.getGAjs()
    );
    private final String header = 
        "<div class='headerdiv'><img class='headerimg' src=\"/static/logo.png\">"+
        "<p align=\"center\">\n" +
        "  <a class=\"style7\" href=\"#\">National Technical University of Athens</a><br>\n" +
        "  <a class=\"style7\" href=\"#\">Unit of Process Control &amp; Informatics</a></p></div>";
    
    private MediaType media;

    public AbstractPublishable() {
    }
    

    @Override
    public MediaType getMediaType() {
        return media;
    }

    @Override
    public void setMediaType(MediaType media) {
        this.media = media;
    }

    @Override
    public List<String> getHeadComponents() {
        return headComponents;
    }

    @Override
    public String getHeader() {
        return header;
    }

}
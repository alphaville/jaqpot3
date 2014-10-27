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

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import org.opentox.jaqpot3.exception.JaqpotException;
import org.opentox.jaqpot3.resources.publish.Publishable;
import org.restlet.data.Language;
import org.restlet.representation.OutputRepresentation;
import org.restlet.representation.Representation;

/**
 *
 * @author Pantelis Sopasakis
 * @author Charalampos Chomenides
 */
public class Representer {

    private boolean doClosePublishable = true;
    private Exception m_exception;

    public Representer() {
    }

    public Representer(boolean doClosePublishable) {
        this.doClosePublishable = doClosePublishable;
    }
    
    public Representation process(final Publishable publishable) throws JaqpotException {

        Representation representation = new OutputRepresentation(publishable.getMediaType()) {

            @Override
            public void write(OutputStream outputStream) throws IOException {
                try {
                    publishable.publish(outputStream);
                } catch (Exception ex) {
                    m_exception = ex;
                } finally {
                    if (doClosePublishable && publishable != null) {
                        publishable.close();
                    }
                    if (outputStream != null) {// Always close the output stream!
                        outputStream.flush();
                        outputStream.close();
                    }
                    this.release();
                }
            }
        };
        if (m_exception != null) {
            throw new JaqpotException("Data could not be publihed to output stream.", m_exception);
        }
        List<Language> langs = new ArrayList<Language>();
        langs.add(Language.ENGLISH);
        langs.add(Language.ENGLISH_US);
        representation.setLanguages(langs);
        representation.setMediaType(publishable.getMediaType());        
        return representation;
    }

    
}

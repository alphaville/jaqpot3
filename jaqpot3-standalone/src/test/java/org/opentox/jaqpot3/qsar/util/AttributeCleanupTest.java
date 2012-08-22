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
package org.opentox.jaqpot3.qsar.util;

import java.io.File;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opentox.toxotis.client.collection.Services;
import org.opentox.toxotis.core.component.Dataset;
import static org.junit.Assert.*;
import org.opentox.toxotis.exceptions.impl.ServiceInvocationException;
import org.opentox.toxotis.exceptions.impl.ToxOtisException;
import org.opentox.toxotis.util.aa.AuthenticationToken;
import org.opentox.toxotis.util.aa.TokenPool;
import weka.core.Instances;

/**
 *
 * @author chung
 */
public class AttributeCleanupTest {
    
    public AttributeCleanupTest() {
    }
    
    @BeforeClass
    public static void setUpClass() throws Exception {
    }
    
    @AfterClass
    public static void tearDownClass() throws Exception {
    }
    
    @Test
    public void testCleanupKeepCompoundUri() throws Exception {
        Dataset ds = new Dataset(Services.ideaconsult().augment("dataset", "R545"));
        File passwordFile = new File(System.getProperty("user.home") + "/toxotisKeys/.my.key");        
        AuthenticationToken at = TokenPool.getInstance().login(passwordFile);
        ds = ds.loadFromRemote(at);
        Instances data = ds.getInstances();
        assertTrue(data.numInstances() > 0);
        assertTrue(data.numAttributes() > 0);
        AttributeCleanup cleanup = new AttributeCleanup(true, AttributeCleanup.AttributeType.string);
        data = cleanup.filter(data);
        assertNotNull(data.attribute("compound_uri"));
        assertTrue(data.numInstances() > 0);
        assertTrue(data.numAttributes() > 0);
        cleanup = new AttributeCleanup(false, AttributeCleanup.AttributeType.string);
        data = cleanup.filter(data);
        assertNull(data.attribute("compound_uri"));
        assertTrue(data.numInstances() > 0);
        assertTrue(data.numAttributes() > 0);
    }
}

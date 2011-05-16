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


package org.opentox.jaqpot3.resources;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opentox.jaqpot3.util.Configuration;
import org.opentox.toxotis.client.ClientFactory;
import org.opentox.toxotis.client.IPostClient;
import org.opentox.toxotis.client.VRI;
import org.opentox.toxotis.client.collection.Media;
import org.opentox.toxotis.core.component.Task;
import org.opentox.toxotis.exceptions.impl.ServiceInvocationException;
import org.opentox.toxotis.training.Trainer;
import org.opentox.toxotis.util.TaskRunner;
import org.opentox.toxotis.util.aa.AuthenticationToken;
import static org.junit.Assert.*;

/**
 *
 * @author chung
 */
public class TaskResourceTest {

    public TaskResourceTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    @Test
    public void testSomeMethod() throws ServiceInvocationException, URISyntaxException, InterruptedException, ExecutionException, IOException {
        /*
         * Train a model
         */
        AuthenticationToken at = new AuthenticationToken("guest", "guest");
        AuthenticationToken athampos = new AuthenticationToken("hampos", "arabela");
        
        IPostClient post = ClientFactory.createPostClient(Configuration.getBaseUri().augment("algorithm", "mlr"));
        post.authorize(at);
        post.addPostParameter("dataset_uri", "http://apps.ideaconsult.net:8080/ambit2/dataset/R545");
        post.addPostParameter("prediction_feature", "http://apps.ideaconsult.net:8080/ambit2/feature/22200");
        post.setMediaType(Media.TEXT_URI_LIST);
        post.post();
        int status = post.getResponseCode();
        if (status != 202) {
            fail();
        }
        String task = post.getResponseText();
        post.close();

        TaskRunner runner = new TaskRunner(new Task(new VRI(task)));
        runner.setDelay(0);
        ExecutorService executor = Executors.newFixedThreadPool(1);
        Future<Task> future = executor.submit(runner);
        while (!future.isDone()) {
            Thread.sleep(10);
        }

        Task result = future.get();
        VRI modelUri = result.getResultUri();

        assertNotNull(result);
        assertNotNull(modelUri);

        /*
         * Do prediction
         */
        post = ClientFactory.createPostClient(modelUri);
        post.authorize(athampos);
        post.setMediaType(Media.TEXT_URI_LIST);
        post.addPostParameter("dataset_uri", "http://apps.ideaconsult.net:8080/ambit2/dataset/R545");
        post.post();
        status = post.getResponseCode();
        if (status != 202) {
            fail("Status is "+status);
        }
        task = post.getResponseText();
        
        post.close();

        runner = new TaskRunner(new Task(new VRI(task)));
        runner.setDelay(0);

        future = executor.submit(runner);
        while (!future.isDone()) {
            Thread.sleep(10);
        }
        result = future.get();
        VRI datasetUri = result.getResultUri();
        assertNotNull(datasetUri);

    }
}

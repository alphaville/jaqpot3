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

package org.opentox.jaqpot3.util;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.opentox.toxotis.database.engine.DeleteOldComponents;
import org.opentox.toxotis.database.exception.DbException;

/**
 *
 * @author Pantelis Sopasakis
 * @author Charalampos Chomenides
 */
public class DatabaseJanitor {

    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static boolean isWorking = false;
    private static org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(DatabaseJanitor.class);

    public static void work() {
        if (!isWorking) {
            Thread worker = new Thread("Database_Janitor") {

                @Override
                public void run() {
                    try {
                        DeleteOldComponents deleter = new DeleteOldComponents();
                        deleter.setDays(7);
                        deleter.delete();
                    } catch (DbException ex) {
                        Logger.getLogger(DatabaseJanitor.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    try {
                        Thread.sleep(1000 * 60 * 60 * 2);//2 hours
                    } catch (InterruptedException ex) {
                        Logger.getLogger(DatabaseJanitor.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            };
            executor.submit(worker);
            isWorking = true;
        }
    }
}

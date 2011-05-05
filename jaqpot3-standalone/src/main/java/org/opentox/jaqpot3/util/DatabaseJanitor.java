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

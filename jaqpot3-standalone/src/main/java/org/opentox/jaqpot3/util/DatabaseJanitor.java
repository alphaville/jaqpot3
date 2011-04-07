package org.opentox.jaqpot3.util;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
            Runnable worker = new Runnable() {

                @Override
                public void run() {
//                    while (true) {
//                        try {
//                            Session s = HibernateUtil.getSessionFactory().openSession();
//
//                            /* Delete very old tasks... */
//                            Query q = s.createSQLQuery(" DELETE FROM OTComponent WHERE (DAY(current_date())-DAY( OTComponent.created )) > 15 " +
//                                    "AND OTComponent.uri like '%/task/%'");
//                            int linesAffected = -1;
//                            try {
//                                linesAffected = q.executeUpdate();
//                                logger.info("Database janitor affected " + linesAffected + " lines erasing very old tasks");
//                            } catch (HibernateException ex) {
//                                logger.warn("Task deletion by janitor FAILED", ex);
//                                if (s.getTransaction().isActive()) {
//                                    try {
//                                        s.getTransaction().rollback();
//                                    } catch (HibernateException hbe) {
//                                        logger.error("Rollback failed after unsuccessful batch delete of old tasks by the janitor of the database.", hbe);
//                                    }
//                                }
//                            } finally {
//                                try {
//                                    s.close();
//                                } catch (HibernateException ex) {
//                                    logger.error("Database janitor cannot close the session", ex);
//                                }
//                            }
//                            Thread.sleep(1000 * 60 * 60 * 2);// once every 2 hours
//                        } catch (InterruptedException ex) {
//                            logger.error("Interruption", ex);
//                        }
//                    }
                }
            };
            executor.submit(worker);
            isWorking = true;
        }
    }
}

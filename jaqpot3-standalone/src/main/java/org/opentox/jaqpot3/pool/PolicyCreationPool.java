package org.opentox.jaqpot3.pool;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.opentox.toxotis.client.VRI;
import org.opentox.toxotis.util.aa.AuthenticationToken;
import org.opentox.toxotis.util.aa.TokenPool;
import org.opentox.toxotis.util.aa.policy.Policy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Pantelis Sopasakis
 * @author Charalampos Chomenides
 */
public class PolicyCreationPool {

    private Logger logger = LoggerFactory.getLogger(PolicyCreationPool.class);

    private ExecutorService executor;
    private ConcurrentMap<String, Future> map;
    private static final int _CORE_POOL_SIZE = 2;
    private static final int _MAX_POOL_SIZE = 2;
    private static final int _BLOCKING_QUEUE_SIZE = 100;
    private static final long _KEEP_ALIVE_TIME = 0;
    private static final TimeUnit _TIME_UNIT = TimeUnit.MILLISECONDS;
    private static PolicyCreationPool instanceOfThis = null;
    public static final PolicyCreationPool POOL = getInstance();

    private static PolicyCreationPool getInstance() {
        if (instanceOfThis == null) {
            instanceOfThis = new PolicyCreationPool();
        }
        return instanceOfThis;
    }

    private PolicyCreationPool() {
        ThreadFactory threadFactory = new ThreadFactory() {

            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r);
                thread.setPriority(Thread.MIN_PRIORITY);
                thread.setDaemon(true);
                thread.setUncaughtExceptionHandler(new UncaughtExceptionHandler() {

                    @Override
                    public void uncaughtException(Thread t, Throwable e) {
                        StringWriter stackTraceWriter = new StringWriter();
                        e.printStackTrace(new PrintWriter(stackTraceWriter));
                    }
                });
                return thread;
            }
        };
        executor = new ThreadPoolExecutor(
                _CORE_POOL_SIZE, _MAX_POOL_SIZE, _KEEP_ALIVE_TIME, _TIME_UNIT,
                new LinkedBlockingDeque<Runnable>(_BLOCKING_QUEUE_SIZE), threadFactory);
        try {
            map = new ConcurrentHashMap<String, Future>();
        } catch (final Exception ex) {
            logger.debug("Error in Pool", ex);
        }
    }

    public void cancel(Policy policy) {

        String id = policy.getPolicyName();
        try {
            map.get(id).cancel(true);
        } catch (final Exception ex) {
            logger.debug("Error while a policy creation task was being cancelled", ex);
        }
    }

    public class PolicyCreationWrapper implements Callable<Integer> {

        private Policy pol;
        private VRI policyService;

        public PolicyCreationWrapper(Policy policy, VRI policyService) {
            super();            
            this.pol = policy;
            this.policyService = policyService;
        }

        public Integer call() throws Exception {
            AuthenticationToken at = TokenPool.getInstance().login(new File(org.opentox.jaqpot3.util.Configuration.getStringProperty("jaqpot.key")));
            int policyServerResponse = pol.publish(policyService, at);
            at.invalidate(); // won't be used any further!!!
            return policyServerResponse;
        }
    }

    public void run(Policy policy, VRI polService) {
        map.put(policy.getPolicyName(), executor.submit(new PolicyCreationWrapper(policy, polService)));
    }
}

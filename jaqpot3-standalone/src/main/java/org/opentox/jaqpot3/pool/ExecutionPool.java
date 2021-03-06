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

package org.opentox.jaqpot3.pool;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.opentox.jaqpot3.util.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Pantelis Sopasakis
 * @author Charalampos Chomenides
 */
public class ExecutionPool {

    private Logger logger = LoggerFactory.getLogger(ExecutionPool.class);
    private ExecutorService executor;
    /**
     * Task UUID to Future
     */
    private ConcurrentMap<String, Future> map;
    private static final int _CORE_POOL_SIZE = Configuration.getIntegerProperty("taskpool.coreSize",4);
    private static final int _MAX_POOL_SIZE = Configuration.getIntegerProperty("taskpool.maxSize",4);
    private static final int _BLOCKING_QUEUE_SIZE = Configuration.getIntegerProperty("taskpool.blockingQueueSize",100);
    private static final long _KEEP_ALIVE_TIME = 0;
    private static final TimeUnit _TIME_UNIT = TimeUnit.MILLISECONDS;
    private static ExecutionPool instanceOfThis = null;
    public static final ExecutionPool POOL = getInstance();

    private static ExecutionPool getInstance() {
        if (instanceOfThis == null) {
            instanceOfThis = new ExecutionPool();
        }
        return instanceOfThis;
    }

    private ExecutionPool() {
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
                new LinkedBlockingQueue<Runnable>(_BLOCKING_QUEUE_SIZE), threadFactory);
        try {
            map = new ConcurrentHashMap<String, Future>();
        } catch (final Exception ex) {
            logger.debug("Error in Pool", ex);
        }
    }

    public int cancel(String id) {
        try {
            Future future = map.get(id);
            if (future == null) {
                return 0;
            } else {
                future.cancel(true);
                return 1;
            }
        } catch (final Exception ex) {
            logger.debug("Error while a task was being cancelled", ex);
            return 0;
        }
    }

    public void run(String id, Runnable runnable) {
        map.put(id, executor.submit(runnable));
    }
}

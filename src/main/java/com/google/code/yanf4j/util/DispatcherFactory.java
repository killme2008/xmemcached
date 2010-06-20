package com.google.code.yanf4j.util;

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.TimeUnit;

import com.google.code.yanf4j.core.impl.PoolDispatcher;

/**
 * �ɷ�������
 * @author dennis
 *
 */
public class DispatcherFactory {
    public static com.google.code.yanf4j.core.Dispatcher newDispatcher(int size, RejectedExecutionHandler rejectedExecutionHandler) {
        if (size > 0) {
            return new PoolDispatcher(size, 60, TimeUnit.SECONDS, rejectedExecutionHandler);
        }
        else {
            return null;
        }
    }
}

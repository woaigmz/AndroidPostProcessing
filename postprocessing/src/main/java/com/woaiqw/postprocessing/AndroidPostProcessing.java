package com.woaiqw.postprocessing;

import android.app.Application;
import android.os.Process;

import com.woaiqw.postprocessing.model.AppDelegate;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by haoran on 2018/10/10.
 */
public class AndroidPostProcessing {

    private volatile static AndroidPostProcessing instance = null;

    private volatile static LinkedHashMap<String, AppDelegate> map;

    private volatile static ScheduledExecutorService taskPool;

    private static AtomicBoolean initCompleted = new AtomicBoolean(false);


    static {
        int CPU_COUNT = Runtime.getRuntime().availableProcessors();
        int CORE_POOL_SIZE = Math.max(2, Math.min(CPU_COUNT - 1, 4));
        map = new LinkedHashMap<>(32, 0.75f, true);
        taskPool = Executors.newScheduledThreadPool(CORE_POOL_SIZE);
    }


    private AndroidPostProcessing() {
        initAppDelegateMap();
        initCompleted.set(true);
    }

    public static AndroidPostProcessing initialization() {
        if (null == instance) {
            synchronized (AndroidPostProcessing.class) {
                if (null == instance)
                    instance = new AndroidPostProcessing();
            }
        }
        return instance;
    }


    private void initAppDelegateMap() {
        //TODO: processor annotation
        AppDelegate agent = new AppDelegate();
        map.put("key", agent);
    }

    public void dispatcher(final Application app) {
        for (Map.Entry<String, AppDelegate> entry : map.entrySet()) {
            final AppDelegate value = entry.getValue();
            if (value.isAsync()) {
                taskPool.schedule(new Runnable() {
                    @Override
                    public void run() {
                        Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
                        value.getAgent().dispatcher(app);
                    }
                }, value.getDelayTime(), TimeUnit.MILLISECONDS);
            } else {
                value.getAgent().dispatcher(app);
            }
        }
    }

    public static void release() {
        if (!initCompleted.get())
            throw new RuntimeException(" must init completed before the fun to release ");
        map.clear();
        taskPool.shutdown();
    }


}
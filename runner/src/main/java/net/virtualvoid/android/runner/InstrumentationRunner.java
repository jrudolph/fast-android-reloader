package net.virtualvoid.android.runner;

import android.app.Activity;
import android.app.Application;
import android.app.Helper;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import dalvik.system.DexClassLoader;

import java.io.File;
import java.lang.reflect.Method;

public class InstrumentationRunner extends Instrumentation {
    private static final String TAG = "Runner";

    @Override
    public Application newApplication(ClassLoader cl, String className, Context context) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        Log.d(TAG, "App");
        Application app = (Application) customClassLoader.loadClass(className).newInstance();
        try {
            Method m = app.getClass().getDeclaredMethod("attach", Context.class);
            m.setAccessible(true);
            m.invoke(app, Helper.createContext(context, context.getApplicationInfo(), this, customClassLoader));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return app;
    }

    @Override
    public void callActivityOnCreate(Activity activity, Bundle icicle) {
        Helper.rewireResources(activity, res);
        Log.d(TAG, activity.getString(0x7f030000));

        super.callActivityOnCreate(activity, icicle);
    }
    @Override
    public Activity newActivity(ClassLoader cl, String className, Intent intent) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        Log.d(TAG, "Act1 new");
        return (Activity) customClassLoader.loadClass(className).newInstance();
    }
    @Override
    public Activity newActivity(Class<?> clazz, Context context, IBinder token, Application application, Intent intent, ActivityInfo info, CharSequence title, Activity parent, String id, Object lastNonConfigurationInstance) throws InstantiationException, IllegalAccessException {
        Log.d(TAG, "Unexpected newActivity(clazz)");
        return super.newActivity(clazz, context, token, application, intent, info, title, parent, id, lastNonConfigurationInstance);    //To change body of overridden methods use File | Settings | File Templates.
    }

    ClassLoader customClassLoader;
    Resources res;
    String targetClass;

    @Override
    public void onCreate(Bundle arguments) {
        res = Helper.createRes(this);

        Log.d(TAG, "arguments: " + arguments.keySet().toString());
        targetClass = arguments.getString("class");

        File dexOutputDir = new File("/data/tmp");
        boolean existed = dexOutputDir.exists();
        boolean created = dexOutputDir.mkdir();

        Log.d(TAG, String.format("Existed: %s, created: %s, exists: %s", existed, created, dexOutputDir.exists()));

        customClassLoader = new DexClassLoader("/sdcard/test.apk:/sdcard/scala_library_filtered.jar:/sdcard/scala_collection.jar:/sdcard/scala_collection_mutable.jar:/sdcard/scala_collection_immutable.jar:/sdcard/scala_xml.jar:/sdcard/blubber.jar", dexOutputDir.getAbsolutePath(), null, getClass().getClassLoader()) {
            @Override
            protected Class<?> loadClass(String className, boolean resolve) throws ClassNotFoundException {
                Class<?> cl = findLoadedClass(className);
                if (cl == null) {
                    try {
                        cl = findClass(className);
                        //Log.d(TAG, "Found in self: "+className);
                        return cl;
                    } catch(ClassNotFoundException e) {
                        //Log.d(TAG, "Looking in parent: "+className);
                        return getParent().loadClass(className);
                    }
                }
                else {
                    //Log.d(TAG, "Already loaded: "+className);
                    return cl;
                }
            }
        };

        try {
            Class<?> cl = customClassLoader.loadClass(targetClass);
            Log.d(TAG, "CL: "+cl.getClassLoader());
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }

        start();
    }

    @Override
    public void onStart() {
        Log.d(TAG, "OnStart");
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.setClassName(getTargetContext().getPackageName(), targetClass);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivitySync(intent);
    }
}

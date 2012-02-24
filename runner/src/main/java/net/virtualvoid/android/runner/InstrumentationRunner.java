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
import dalvik.system.PathClassLoader;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;

public class InstrumentationRunner extends Instrumentation {
    @Override
    public Application newApplication(ClassLoader cl, String className, Context context) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        Log.i("INSTJ", "App");
        Application app = (Application) myCL.loadClass(className).newInstance();
        try {
            Method m = app.getClass().getDeclaredMethod("attach", Context.class);
            m.setAccessible(true);
            m.invoke(app, Helper.createContext(context, context.getApplicationInfo(), this, myCL));
        } catch (NoSuchMethodException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (InvocationTargetException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

        return app;
        //return super.newApplication(myCL, className, context);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public void callActivityOnCreate(Activity activity, Bundle icicle) {
        Helper.rewireResources(activity, res);
        Log.i("INSTJ", activity.getString(0x7f030000));

        super.callActivityOnCreate(activity, icicle);
    }

    @Override
    public Activity newActivity(ClassLoader cl, String className, Intent intent) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        Log.i("INSTJ", "Act1 new");
        return (Activity) myCL.loadClass(className).newInstance();
        //return super.newActivity(myCL, className, intent);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public Activity newActivity(Class<?> clazz, Context context, IBinder token, Application application, Intent intent, ActivityInfo info, CharSequence title, Activity parent, String id, Object lastNonConfigurationInstance) throws InstantiationException, IllegalAccessException {
        Log.i("INSTJ", "Act2");
        return super.newActivity(clazz, context, token, application, intent, info, title, parent, id, lastNonConfigurationInstance);    //To change body of overridden methods use File | Settings | File Templates.
    }


    ClassLoader myCL;
    Resources res;
    String targetClass;

    @Override
    public void onCreate(Bundle arguments) {
        res = Helper.createRes(this);

        Log.i("INSTJ", "argser: " + arguments.keySet().toString());
        targetClass = arguments.getString("class");

        File dexOutputDir = new File("/data/tmp");
        boolean existed = dexOutputDir.exists();
        boolean created = dexOutputDir.mkdir();

        Log.i("INSTJ", String.format("Existed: %s, created: %s, exists: %s", existed, created, dexOutputDir.exists()));

        myCL = new DexClassLoader("/sdcard/test.apk:/sdcard/scala_library_filtered.jar:/sdcard/scala_collection.jar:/sdcard/scala_collection_mutable.jar:/sdcard/scala_collection_immutable.jar:/sdcard/scala_xml.jar:/sdcard/blubber.jar", dexOutputDir.getAbsolutePath(), null, getClass().getClassLoader()) {
            /*@Override
            protected Class<?> loadClass(String className, boolean resolve) throws ClassNotFoundException {
                Log.i("INSTJ", "Trying to load "+className);
                return super.loadClass(className, resolve);    //To change body of overridden methods use File | Settings | File Templates.
            }*/
            @Override
            protected Class<?> loadClass(String className, boolean resolve) throws ClassNotFoundException {
                Class<?> cl = findLoadedClass(className);
                if (cl == null) {
                    try {
                        cl = findClass(className);
                        //Log.i("INSTJ", "Found in self: "+className);
                        return cl;
                    } catch(ClassNotFoundException e) {
                        //Log.i("INSTJ", "Looking in parent: "+className);
                        return getParent().loadClass(className);
                    }
                }
                else {
                    //Log.i("INSTJ", "Already loaded: "+className);
                    return cl;
                }
            }
        };

        try {
            Log.i("INSTJ", "CL: "+cl.getClassLoader());
        } catch (Exception e) {
            Log.e("INSTJ", e.getMessage(), e);
            //e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

        start();
    }

    @Override
    public void onStart() {
        Log.i("INSTJ", "OnStart");
        Intent intent = new Intent(Intent.ACTION_MAIN);
        //intent.addCategory(Intent.CATEGORY_LAUNCHER);
        //intent.setPackage(getTargetContext().getPackageName());
        intent.setClassName(getTargetContext().getPackageName(), targetClass);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivitySync(intent);
    }
}

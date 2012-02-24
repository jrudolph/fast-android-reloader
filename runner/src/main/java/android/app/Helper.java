package android.app;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.res.CompatibilityInfo;
import android.content.res.Resources;
import android.util.Log;
import android.view.ContextThemeWrapper;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public abstract class Helper {
    private static Field findField(Class<?> cl, String name) {
        try {
            return cl.getDeclaredField(name);
        } catch (NoSuchFieldException e) {
            return findField(cl.getSuperclass(), name);
        }
    }
    private static Object field(Object recv, String field) {
        try {
            Field f = findField(recv.getClass(), field);
            f.setAccessible(true);
            return f.get(recv);
        } catch(Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
    private static Object staticField(Class<?> cl, String field) {
            try {
                Field f = cl.getDeclaredField(field);
                f.setAccessible(true);
                return f.get(null);
            } catch(Exception e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }
    public static Resources createRes(Instrumentation i) {
        ActivityThread thread = (ActivityThread) field(i, "mThread");
        return thread.getTopLevelResources("/sdcard/test.apk", CompatibilityInfo.DEFAULT_COMPATIBILITY_INFO);
    }
    public static Context createContext(Context outer, ApplicationInfo info, Instrumentation i, ClassLoader newCL) {
        try {
            //Class<?> compat = Class.forName("android.content.res.CompatibilityInfo");
            //Class<?> ContextImpl = Class.forName("android.app.ContextImpl");

            ActivityThread thread = (ActivityThread) field(i, "mThread");

            Resources res = thread.getTopLevelResources("/sdcard/test.apk", CompatibilityInfo.DEFAULT_COMPATIBILITY_INFO);
            Log.i("INSTV", res.getString(0x7f030000));

            //Constructor cons = ContextImpl.getDeclaredConstructor();
            //cons.setAccessible(true);

            /*LoadedApk apk = new LoadedApk(thread, "android", outer, info,
                                            CompatibilityInfo.DEFAULT_COMPATIBILITY_INFO);*/
            Field resF = ContextImpl.class.getDeclaredField("mResources");
            resF.setAccessible(true);
            resF.set(outer, res);

            Field clF = LoadedApk.class.getDeclaredField("mClassLoader");
            clF.setAccessible(true);
            clF.set(((ContextImpl)outer).mPackageInfo, newCL);

            /*ContextImpl context = (ContextImpl) cons.newInstance();
            context.init(apk, null, thread);*/
            return (Context) outer;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
    public static void rewireResources(Activity a, Resources res) {
        try {
            Context context = a.getBaseContext();

            Field resF = ContextImpl.class.getDeclaredField("mResources");
            resF.setAccessible(true);
            resF.set(context, res);

            /*Method m = ContextThemeWrapper.class.getDeclaredMethod("attachBaseContext", Context.class);
            m.setAccessible(true);
            m.invoke(a, context);*/
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
}

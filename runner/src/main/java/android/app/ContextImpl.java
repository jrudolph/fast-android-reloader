package android.app;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.res.CompatibilityInfo;
import android.content.res.Resources;
import android.os.IBinder;

class ActivityThread {
    Resources getTopLevelResources(String resDir, CompatibilityInfo compInfo){ return null;}
}

class LoadedApk {
    public LoadedApk(ActivityThread activityThread, String name,
            Context systemContext, ApplicationInfo info, CompatibilityInfo compatInfo) {}
}

abstract class ContextImpl extends Context {
    LoadedApk mPackageInfo;
    
    ContextImpl(){}
    void init(LoadedApk packageInfo,
                IBinder activityToken, ActivityThread mainThread) {}
}
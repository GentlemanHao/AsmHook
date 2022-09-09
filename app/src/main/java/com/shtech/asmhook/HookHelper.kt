package com.shtech.asmhook

import android.net.wifi.WifiInfo
import android.util.Log
import androidx.annotation.Keep

@Keep
object HookHelper {

    /*@JvmStatic
    @HookMethod(Settings.Secure::class, "getString")
    fun getString(resolver: ContentResolver, name: String): String {
        Log.d("--wh--", "################## getString $name")
        Log.d("--wh--", "${getMethodStack()}")
        return Settings.Secure.getString(resolver, name)
    }

    @JvmStatic
    @HookMethod(ActivityManager::class, "getRunningAppProcesses")
    fun getRunningAppProcesses(activityManager: ActivityManager): List<ActivityManager.RunningAppProcessInfo>? {
        Log.d("--wh--", "################## getRunningAppProcesses")
        Log.d("--wh--", "${getMethodStack()}")
        return activityManager.runningAppProcesses
    }

    @JvmStatic
    @HookMethod(PackageManager::class, "queryIntentActivities")
    fun queryIntentActivities(packageManager: PackageManager, intent: Intent, flags: Int): List<ResolveInfo> {
        Log.d("--wh--", "################## queryIntentActivities")
        Log.d("--wh--", "${getMethodStack()}")
        return packageManager.queryIntentActivities(intent, flags)
    }

    @JvmStatic
    @HookMethod(Application::class, "registerActivityLifecycleCallbacks")
    fun registerActivityLifecycleCallbacks(application: Application, callback: Application.ActivityLifecycleCallbacks) {
        Log.d("--wh--", "################## registerActivityLifecycleCallbacks")
        Log.d("--wh--", "${getMethodStack()}")
        return application.registerActivityLifecycleCallbacks(callback)
    }*/

    @JvmStatic
    @HookMethod(WifiInfo::class, "getSSID")
    fun getSSID(wifiInfo: WifiInfo): String? {
        Log.d("--wh--", "################## getSSID")
        Log.d("--wh--", "${getMethodStack()}")
        return wifiInfo.ssid
    }

    private fun getMethodStack(): String {
        val sb = StringBuilder()
        Thread.currentThread().stackTrace.forEach {
            sb.append("$it\n")
        }
        return sb.toString()
    }
}
package com.jinchim.jbind_sdk;


import android.app.Activity;
import android.util.Log;

import java.lang.reflect.Constructor;
import java.util.LinkedHashMap;
import java.util.Map;

public class JBindSDK {

    private final static String TAG = JBindSDK.class.getSimpleName();
    private static Map<Class<?>, Constructor<? extends Unbinder>> map = new LinkedHashMap<>();

    public static Unbinder bind(Activity activity) {
        String className = activity.getPackageName() + "." + activity.getLocalClassName() + "_JBind";
        try {
            Class clazz = Class.forName(className);
            Constructor<? extends Unbinder> constructor = map.get(clazz);
            if (constructor == null) {
                constructor = clazz.getConstructor(activity.getClass());
                map.put(clazz, constructor);
            }
            return constructor.newInstance(activity);
        } catch (Exception e) {
            Log.i(TAG, "eroor => " + e.getMessage());
            return Unbinder.Empty;
        }
    }


}

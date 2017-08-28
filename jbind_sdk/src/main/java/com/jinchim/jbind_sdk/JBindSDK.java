package com.jinchim.jbind_sdk;


import android.app.Activity;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.LinkedHashMap;
import java.util.Map;

public class JBindSDK {

    private final static String TAG = JBindSDK.class.getSimpleName();
    private static Map<Class<?>, Constructor<? extends Unbinder>> map = new LinkedHashMap<>();

    public static Unbinder bind(Activity activity) {
        Constructor<? extends Unbinder> constructor = findConstructor(activity);
        if (constructor == null) {
            return Unbinder.Empty;
        }
        try {
            return constructor.newInstance(activity);
        } catch (InstantiationException e) {
            throw new RuntimeException("Unable to invoke " + constructor + ".", e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Unable to invoke " + constructor + ".", e);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            if (cause instanceof Error) {
                throw (Error) cause;
            }
            throw new RuntimeException("Unable to create instance.", cause);
        }
    }

    private static Constructor<? extends Unbinder> findConstructor(Activity activity) {
        Class clazzActivity = activity.getClass();
        Constructor<? extends Unbinder> constructor = map.get(clazzActivity);
        if (constructor != null) {
            return constructor;
        }
        String className = activity.getPackageName() + "." + activity.getLocalClassName() + "_JBind";
        try {
            Class clazz = Class.forName(className);
            constructor = clazz.getConstructor(clazzActivity);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Not found class " + className + ".", e);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Unable find the constructor for " + className + ".", e);
        }
        map.put(clazzActivity, constructor);
        return constructor;
    }

}

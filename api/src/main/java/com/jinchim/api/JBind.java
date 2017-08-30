package com.jinchim.api;

import android.app.Activity;
import android.view.View;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.LinkedHashMap;
import java.util.Map;

public class JBind {

    private final static String TAG = JBind.class.getSimpleName();
    private static Map<Class<?>, Constructor<? extends Unbinder>> map = new LinkedHashMap<>();

    public static Unbinder bind(Activity activity) {
        if (activity == null) {
            return Unbinder.Empty;
        }
        return create(activity, activity.getWindow().getDecorView());
    }

    public static Unbinder bind(android.support.v4.app.Fragment fragment, View view) {
        if (fragment == null || view == null) {
            return Unbinder.Empty;
        }
        return create(fragment, view);
    }

    public static Unbinder bind(android.app.Fragment fragment, View view) {
        if (fragment == null || view == null) {
            return Unbinder.Empty;
        }
        return create(fragment, view);
    }


    /**
     * 调用生成类的构造方法并返回一个 Unbinder 对象
     */
    private static Unbinder create(Object object, View view) {
        Class<?> clazz = object.getClass();
        Constructor<? extends Unbinder> constructor = findConstructor(clazz);
        if (constructor == null) {
            return Unbinder.Empty;
        }
        try {
            return constructor.newInstance(object, view);
        } catch (InstantiationException e) {
            throw new RuntimeException("Unable to invoke " + constructor + ".", e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Unable to invoke " + constructor + ".", e);
        } catch (InvocationTargetException e) {
            // 这个地方是反射内部方法抛出异常的处理，必须在这里处理才会有效果
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

    /**
     * 找到某个类的构造方法
     */
    private static Constructor<? extends Unbinder> findConstructor(Class<?> clazz) {
        Constructor<? extends Unbinder> constructor = map.get(clazz);
        if (constructor != null) {
            return constructor;
        }
        String className = clazz.getName() + "_JBind";
        try {
            Class bindClass = Class.forName(className);
            constructor = bindClass.getConstructor(clazz, View.class);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Not found class " + className + ".", e);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Unable find the constructor for " + className + ".", e);
        }
        map.put(clazz, constructor);
        return constructor;
    }

}

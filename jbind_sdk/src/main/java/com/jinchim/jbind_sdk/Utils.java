package com.jinchim.jbind_sdk;


import android.view.View;

public class Utils {

    public static <T> T findViewAsType(View view, int resId, String who, Class<T> clazz) {
        // 获取在 XML 文件中定义的 ID
        String resourceName = view.getContext().getResources().getResourceEntryName(resId);
        View v = view.findViewById(resId);
        // 为 null 表示未找到这个资源
        if (v == null) {
            throw new IllegalStateException("View named '" + resourceName + "' with Id " + resId + " for '" + who + "' was not found.");
        }
        try {
            // 返回找到的控件
            return clazz.cast(v);
        } catch (ClassCastException e) {
            // 控件类型错误
            throw new IllegalStateException("View named '" + resourceName + "' with Id " + resId + " for '" + who + "' was of the wrong type.", e);
        }
    }

}

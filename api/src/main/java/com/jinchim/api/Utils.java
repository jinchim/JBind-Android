package com.jinchim.api;


import android.view.View;

public class Utils {

    /**
     * 这个方法用于控件绑定时使用，需要进行类型判断
     */
    public static <T> T findView(View view, int resId, String who, String where, Class<T> clazz) {
        // 获取在 XML 文件中定义的 ID
        String resourceName = view.getContext().getResources().getResourceEntryName(resId);
        View v = view.findViewById(resId);
        // 为 null 表示未找到这个资源
        if (v == null) {
            throw new IllegalStateException("View named '" + resourceName + "' with Id " + resId + " for '" + who + "' at '" + where + "' was not found.");
        }
        try {
            // 返回找到的控件
            return clazz.cast(v);
        } catch (ClassCastException e) {
            // 控件类型错误
            throw new IllegalStateException("View named '" + resourceName + "' with Id " + resId + " for '" + who + "' at '" + where + "' was of the wrong type.", e);
        }
    }

    /**+
     * 这个方法用于控件事件监听时使用，不需要进行类型判断
     */
    public static View findView(View view, int resId, String who, String where) {
        // 获取在 XML 文件中定义的 ID
        String resourceName = view.getContext().getResources().getResourceEntryName(resId);
        View v = view.findViewById(resId);
        // 为 null 表示未找到这个资源
        if (v == null) {
            throw new IllegalStateException("View named '" + resourceName + "' with Id " + resId + " for '" + who + "' at '" + where + "' was not found.");
        }
        return v;
    }

}

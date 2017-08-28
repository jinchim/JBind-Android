package com.jinchim.jbind.compiler;

import com.jinchim.jbind.annotations.JClick;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.TypeMirror;


public class JClickMethod {

    private ExecutableElement executableElement;
    private int[] resId;

    JClickMethod(ExecutableElement executableElement) {
        this.executableElement = executableElement;
        JClick click = executableElement.getAnnotation(JClick.class);
        resId = click.value();
    }

    int[] getResId() {
        return resId;
    }

    String getMehodName() {
        return executableElement.getSimpleName().toString();
    }

    TypeMirror getMehodType() {
        return executableElement.asType();
    }


}

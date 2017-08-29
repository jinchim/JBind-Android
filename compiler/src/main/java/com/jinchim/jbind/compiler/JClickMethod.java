package com.jinchim.jbind.compiler;


import com.jinchim.jbind.annotations.Click;

import java.util.List;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;


public class JClickMethod {

    private ExecutableElement executableElement;
    private int[] resId;

    JClickMethod(ExecutableElement executableElement) {
        this.executableElement = executableElement;
        Click click = executableElement.getAnnotation(Click.class);
        resId = click.value();
    }

    int[] getResId() {
        return resId;
    }

    String getMehodName() {
        return executableElement.getSimpleName().toString();
    }

    List<? extends VariableElement> getParameters() {
        return executableElement.getParameters();
    }


}

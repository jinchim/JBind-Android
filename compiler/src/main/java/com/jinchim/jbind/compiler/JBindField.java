package com.jinchim.jbind.compiler;


import com.jinchim.jbind.annotations.Bind;

import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;



public class JBindField {

    private VariableElement variableElement;
    private int resId;

    JBindField(VariableElement variableElement) {
        this.variableElement = variableElement;
        Bind bind = variableElement.getAnnotation(Bind.class);
        resId = bind.value();
    }

    int getResId() {
        return resId;
    }

    String getFiledName() {
        return variableElement.getSimpleName().toString();
    }

    TypeMirror getFiledType() {
        return variableElement.asType();
    }


}

package com.jinchim.jbind.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by Administrator on 2017/8/18 0018.
 */

@Retention(RetentionPolicy.CLASS)
@Target(ElementType.FIELD)
public @interface JBind {

     int value();

}

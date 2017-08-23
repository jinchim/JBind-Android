package com.jinchim.jbind.compiler;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;

public class JBindClass {

    // 这里使用 ClassName 的 get() 方法拿到指定具体路径的类名称
    private static final ClassName Unbinder = ClassName.get("com.jinchim.jbind_sdk", "Unbinder");
    private static final ClassName View = ClassName.get("android.view", "View");

    private Elements elements;
    private TypeElement typeElement;
    private List<JBindField> jBindFields;

    JBindClass(Elements elements, TypeElement typeElement) {
        this.elements = elements;
        this.typeElement = typeElement;
        jBindFields = new ArrayList<>();
    }

    void addField(JBindField jBindField) {
        jBindFields.add(jBindField);
    }

    JavaFile prepareFile() {
        // 添加构造器
        MethodSpec.Builder constructor1 = MethodSpec
                .constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addParameter(TypeName.get(typeElement.asType()), "activity")
                .addStatement("this(activity, activity.getWindow().getDecorView())");

        MethodSpec.Builder constructor2 = MethodSpec
                .constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                // 这里是添加方法参数，需要指定参数类型和参数变量
                // 先使用 TypeElement 的 asType() 方法拿到 TypeMirror，再使用 TypeName 的 get() 方法拿到当前注解信息所属的类名称（当前类名当然是注解所在的类）
                .addParameter(TypeName.get(typeElement.asType()), "activity")
                .addParameter(View, "view")
                // 添加代码，进行为空的判断
                .beginControlFlow("if (activity == null)")
                .addStatement("return")
                .endControlFlow()
                // 添加代码，进行成员变量赋值
                .addStatement("this.activity = activity");
        for (JBindField jBindField : jBindFields) {
            // 添加代码，$N 用于指定对象成员变量，&T 用于指定类型，$L 用于方法参数
            constructor2.addStatement("activity.$N = ($T) view.findViewById($L)", jBindField.getFiledName(), jBindField.getFiledType(), jBindField.getResId());
        }

        // 添加 unbind() 方法
        MethodSpec.Builder unbind = MethodSpec
                .methodBuilder("unbind")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .beginControlFlow("if (activity == null)")
                .addStatement("return")
                .endControlFlow();
        for (JBindField jBindField : jBindFields) {
            unbind.addStatement("activity.$N = null", jBindField.getFiledName());
        }

        // 添加成员变量
        FieldSpec.Builder field = FieldSpec.builder(TypeName.get(typeElement.asType()), "activity");

        // 添加类信息
        TypeSpec typeSpec = TypeSpec
                .classBuilder(typeElement.getSimpleName() + "_JBind")
                .addModifiers(Modifier.PUBLIC)
                .addSuperinterface(Unbinder)
                .addField(field.build())
                .addMethod(constructor1.build())
                .addMethod(constructor2.build())
                .addMethod(unbind.build())
                .build();

        return JavaFile.builder(elements.getPackageOf(typeElement).getQualifiedName().toString(), typeSpec).build();
    }

}

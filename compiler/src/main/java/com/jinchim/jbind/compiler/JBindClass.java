package com.jinchim.jbind.compiler;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import java.util.ArrayList;
import java.util.List;

import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.Elements;

public class JBindClass {

    // 这里使用 ClassName 的 get() 方法拿到指定具体路径的类名称
    private static final ClassName Unbinder = ClassName.get("com.jinchim.api", "Unbinder");
    private static final ClassName View = ClassName.get("android.view", "View");
    private static final ClassName Utils = ClassName.get("com.jinchim.api", "Utils");
    private static final ClassName View_Click = ClassName.get("android.view.View", "OnClickListener");

    private Elements elements;
    private TypeElement typeElement;
    private List<JBindField> jBindFields;
    private List<JClickMethod> jClickMethods;

    JBindClass(Elements elements, TypeElement typeElement) {
        this.elements = elements;
        this.typeElement = typeElement;
        jBindFields = new ArrayList<>();
        jClickMethods = new ArrayList<>();
    }

    void addField(JBindField jBindField) {
        jBindFields.add(jBindField);
    }

    void addMethod(JClickMethod jClickMethod) {
        jClickMethods.add(jClickMethod);
    }


    JavaFile prepareFile() {
        // 添加构造器
        MethodSpec.Builder constructor = MethodSpec
                .constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                // 这里是添加方法参数，需要指定参数类型和参数变量
                // 先使用 TypeElement 的 asType() 方法拿到 TypeMirror，再使用 TypeName 的 get() 方法拿到当前注解信息所属的类名称（当前类名当然是注解所在的类），这种方法可以拿到当前注解所在的类信息
                .addParameter(TypeName.get(typeElement.asType()), "target", Modifier.FINAL)
                .addParameter(View, "view")
                // 添加代码，进行为空的判断
                .beginControlFlow("if (target == null)")
                .addStatement("return")
                .endControlFlow()
                // 添加代码，进行成员变量赋值
                .addStatement("this.target = target");
        // 增加控件绑定
        for (JBindField jBindField : jBindFields) {
            // $N 用于指定另外一个通过名字生成的声明，&T 用于指定类型，$L 用于指定字符串的连接（没有双引号），$S 用于指定字符串（有双引号）
            constructor.addStatement("target.$N = $T.findView(view, $L, \"field $L\", \"$L\", $L.class)", jBindField.getFiledName(), Utils, jBindField.getResId(), jBindField.getFiledName(), typeElement.getQualifiedName(), jBindField.getFiledType());
        }
        // 增加控件点击事件的代码
        for (JClickMethod jClickMethod : jClickMethods) {
            // 匿名内部类
            MethodSpec.Builder methodClick = MethodSpec
                    .methodBuilder("onClick")
                    .addAnnotation(Override.class)
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(View, "view");
            if (jClickMethod.getParameters().isEmpty()) {
                methodClick.addStatement("target.$N()", jClickMethod.getMehodName());
            } else {
                methodClick.addStatement("target.$N(view)", jClickMethod.getMehodName());
            }
            TypeSpec.Builder typeClick = TypeSpec
                    .anonymousClassBuilder("")
                    .addSuperinterface(View_Click)
                    .addMethod(methodClick.build());
            // 点击事件回调
            for (int resId : jClickMethod.getResId()) {
                constructor.addStatement("view$L = $T.findView(view, $L, \"method $L\", \"$L\")", resId, Utils, resId, jClickMethod.getMehodName(), typeElement.getQualifiedName());
                constructor.addStatement("view$L.setOnClickListener($L)", resId, typeClick.build());
            }
        }

        // 添加 unbind() 方法，主要是释放各种引用
        MethodSpec.Builder methodUnbind = MethodSpec
                .methodBuilder("unbind")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .beginControlFlow("if (target == null)")
                .addStatement("return")
                .endControlFlow();
        for (JBindField jBindField : jBindFields) {
            methodUnbind.addStatement("target.$N = null", jBindField.getFiledName());
        }
        for (JClickMethod jClickMethod : jClickMethods) {
            for (int resId : jClickMethod.getResId()) {
                methodUnbind.addStatement("view$L.setOnClickListener(null)", resId);
                methodUnbind.addStatement("view$L = null", resId);
            }
        }
        methodUnbind.addStatement("target = null");

        // 添加成员变量 target
        FieldSpec.Builder fieldTarget = FieldSpec.builder(TypeName.get(typeElement.asType()), "target");
        // 添加成员变量 view
        List<FieldSpec.Builder> fieldViews = new ArrayList<>();
        for (JClickMethod jClickMethod : jClickMethods) {
            for (int resId : jClickMethod.getResId()) {
                fieldViews.add(FieldSpec.builder(View, "view" + resId));
            }
        }

        // 添加类注解
        AnnotationSpec.Builder suppressWarnings = AnnotationSpec
                .builder(SuppressWarnings.class)
                .addMember("value", "\"ResourceType\"");

        // 构建类对象
        TypeSpec.Builder type = TypeSpec
                .classBuilder(typeElement.getSimpleName() + "_JBind")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(suppressWarnings.build())
                .addSuperinterface(Unbinder)
                .addField(fieldTarget.build())
                .addMethod(constructor.build())
                .addMethod(methodUnbind.build());
        for (FieldSpec.Builder filedView : fieldViews) {
            type.addField(filedView.build());
        }

        return JavaFile.builder(elements.getPackageOf(typeElement).getQualifiedName().toString(), type.build()).build();
    }

}

package com.jinchim.jbind.compiler;

import com.google.auto.service.AutoService;
import com.jinchim.jbind.annotations.JBind;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;

// 一定要注意是 Processor 而不是 Process
@AutoService(Processor.class)
public class JBindProcess extends AbstractProcessor {

    private static final String Type_View = "android.view.View";
    private static final String Type_Activity = "android.app.Activity";
    private static final String Type_Fragment = "android.app.Fragment";
    private static final String Type_FragmentForV4 = "android.support.v4.app.Fragment";

    // 里面包含一些方法获取有用的信息
    private Elements elements;
    // 用来生成文件的工具
    private Filer filer;
    // 日志辅助工具，在这个处理器内部出错都要使用这个
    private Messager messager;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnvironment) {
        super.init(processingEnvironment);

        elements = processingEnvironment.getElementUtils();
        filer = processingEnvironment.getFiler();
        messager = processingEnvironment.getMessager();
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> annotataionTypes = new LinkedHashSet<>();
        annotataionTypes.add(JBind.class.getCanonicalName());
        return annotataionTypes;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.RELEASE_7;
    }

    // 返回 true 表示不会有其他处理器处理
    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        // 记录扫描到的指定的注解信息
        Map<TypeElement, JBindClass> jBindClassMap = new LinkedHashMap<>();
        // 获取所有使用 JBind 注解的元素
        Set<? extends Element> elements = roundEnvironment.getElementsAnnotatedWith(JBind.class);
        for (Element element : elements) {
            // 检查是否为 VariableElement
            if (!(element instanceof VariableElement)) {
                error(element, "%s is not a variable element." + element.getSimpleName());
                return true;
            }

            // 获取 TypeElement
            TypeElement typeElement = (TypeElement) element.getEnclosingElement();
            // 获取 VariableElement
            VariableElement variableElement = (VariableElement) element;

            // 判断注解的变量修饰符有没有 private、final 以及 static
            for (Modifier modifier:variableElement.getModifiers()) {
                if (modifier == Modifier.FINAL || modifier == Modifier.PRIVATE || modifier == Modifier.STATIC) {
                    error(element, "@Bind fields (%s) must not be private, final or static.", typeElement.asType().toString() + "." + variableElement.getSimpleName());
                    return true;
                }
            }

            // 判断注解的变量类型是不是 View 及其子孙类
            if (!isSubtypeOfType(element.asType(), Type_View)) {
                error(element, "@Bind fields must extend from View, is not extends from %s.", element.asType().toString());
                return true;
            }

            // 判断注解所属的类是不是 Activity 及其子孙类
            if (!isSubtypeOfType(typeElement.asType(), Type_Activity)) {
                error(element, "@Bind fields must in class of extends from Activity, not in class of extends from %s.", typeElement.asType().toString());
                return true;
            }

            // 注解信息初始化
            JBindClass jBindClass = jBindClassMap.get(typeElement);
            if (jBindClass == null) {
                jBindClass = new JBindClass(this.elements, typeElement);
                jBindClassMap.put(typeElement, jBindClass);
            }
            jBindClass.addField(new JBindField(variableElement));
        }

        // 生成代码文件
        for (TypeElement typeElement : jBindClassMap.keySet()) {
            JBindClass jBindClass = jBindClassMap.get(typeElement);
            try {
                jBindClass.prepareFile().writeTo(filer);
            } catch (IOException e) {
                error(typeElement, "Generate file failed, reason: %s.", e.getMessage());
                return true;
            }
        }

        return false;
    }

    /**
     * 判断给定元素的 TypeMirror 类型是不是指定 type 的子孙类
     *
     * @param typeMirror 元素的 TypeMirror
     * @param type       指定 type，该类的全路径
     * @return 是否是其子孙类
     */
    private static boolean isSubtypeOfType(TypeMirror typeMirror, String type) {
        if (typeMirror == null || type == null || type.isEmpty()) {
            return false;
        }
        if (type.equals(typeMirror.toString())) {
            return true;
        }
        if (typeMirror.getKind() != TypeKind.DECLARED) {
            return false;
        }
        DeclaredType declaredType = (DeclaredType) typeMirror;
        List<? extends TypeMirror> typeArguments = declaredType.getTypeArguments();
        if (typeArguments.size() > 0) {
            StringBuilder typeString = new StringBuilder(declaredType.asElement().toString());
            typeString.append('<');
            for (int i = 0; i < typeArguments.size(); i++) {
                if (i > 0) {
                    typeString.append(',');
                }
                typeString.append('?');
            }
            typeString.append('>');
            if (typeString.toString().equals(type)) {
                return true;
            }
        }
        Element element = declaredType.asElement();
        if (!(element instanceof TypeElement)) {
            return false;
        }
        TypeElement typeElement = (TypeElement) element;
        TypeMirror superType = typeElement.getSuperclass();
        if (isSubtypeOfType(superType, type)) {
            return true;
        }
        for (TypeMirror interfaceType : typeElement.getInterfaces()) {
            if (isSubtypeOfType(interfaceType, type)) {
                return true;
            }
        }
        return false;
    }

    private void error(Element element, String message, Object... args) {
        printMessage(Diagnostic.Kind.ERROR, element, message, args);
    }

    private void note(Element element, String message, Object... args) {
        printMessage(Diagnostic.Kind.NOTE, element, message, args);
    }

    private void printMessage(Diagnostic.Kind kind, Element element, String message, Object[] args) {
        if (args.length > 0) {
            message = String.format(message, args);
        }
        messager.printMessage(kind, message, element);
    }

}

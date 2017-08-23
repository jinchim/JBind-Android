# 编写一个 Android 编译时注解框架的一般套路——以 ButterKnife 为例

对于熟悉 Android 应用开发的同学来说，肯定对 [ButterKnife](http://jakewharton.github.io/butterknife/) 这个框架不陌生，这是一个专注于 Android 开发的 View 注入框架，可以减少大量的 findViewById() 以及 setOnClickListener() 代码，可视化一键生成，可以大大提高开发效率并且使得代码变得简洁。

本人通过学习其源码（源码地址：[https://github.com/JakeWharton/butterknife/](https://github.com/JakeWharton/butterknife/)）发现其核心思想是使用注解处理器技术来实现的，并不影响程序执行的效率，于是自己也撸了一个编译时注解框架，这种框架是在程序编译时生成一些开发过程中的重复代码来提高开发效率，所以开发这类框架的大体流程是相似的，下面来一一介绍。

## 注解处理器

注解（一种用来描述 Java 元素的元数据，这里不作介绍）的生命周期分为 SOURCE（源文件保留）、CLASS（字节码文件保留）、RUNTIME（运行时保留），这里不讨论那些在运行时通过反射机制运行处理的注解，而是讨论在编译阶段处理的注解，Javac（编译 Java 源代码的程序）给我们提供了一个注解处理器，用来在编译时扫描和处理注解。

### AbstractProcessor

每个注解处理器都是继承于 AbstractProcessor，如下所示：

``` java
package com.example;
public class MyProcess extends AbstractProcessor {
    @Override
	public synchronized void init(ProcessingEnvironment env){ }
    
   	@Override
   	public boolean process(Set<? extends TypeElement> annoations, RoundEnvironment env) { }

	@Override
	public Set<String> getSupportedAnnotationTypes() { }

	@Override
	public SourceVersion getSupportedSourceVersion() { }
}
```

* init(ProcessingEnvironment env)：每一个注解处理器类都必须有一个空的构造函数。然而，这里有一个特殊的 init() 方法，它会被注解处理工具调用，并输入 ProcessingEnviroment 参数。ProcessingEnviroment 提供很多有用的工具类 Elements、Types 和 Filer。
* process(Set<? extends TypeElement> annotations, RoundEnvironment env)：这相当于每个处理器的主函数 main()。你在这里写你的扫描、评估和处理注解的代码，以及生成 Java 文件。输入参数 RoundEnviroment，可以让你查询出包含特定注解的被注解元素。
* getSupportedAnnotationTypes()：这里你必须指定，这个注解处理器是注册给哪个注解的。注意，它的返回值是一个字符串的集合，包含本处理器想要处理的注解类型的合法全称。换句话说，你在这里定义你的注解处理器注册到哪些注解上。
* getSupportedSourceVersion()：用来指定你使用的 Java 版本。通常这里返回 SourceVersion.latestSupported()，也可以直接指定具体的版本，比如 SourceVersion.RELEASE_7。

### 注册注解处理器

如何让 Javac 在编译时运行我们自定义的处理器呢？需要在最后打包的 .jar 文件中的 /META-INF/services/ 下放入一个文件，文件名为 javax.annotation.processing.Processor，内容就是刚刚定义的处理器的全名列表，如下：

``` java
com.example.MyProcess
```

这样在编译 .jar 文件时便会执行指定的处理器的代码。

## 需求分析

介绍了注解处理器的基本用法后，我们的框架之路可以开始了！先看看 ButterKnife 的基本用法，如下：

``` java
public class ExampleActivity extends Activity {
   	@BindView(R.id.title) TextView title;
   	@BindView(R.id.subtitle) TextView subtitle;
   	@BindView(R.id.footer) TextView footer;
   	Unbinder unbider;

   	@Override 
   	public void onCreate(Bundle savedInstanceState) {
      	super.onCreate(savedInstanceState);
      	setContentView(R.layout.simple_activity);
      	unbider = ButterKnife.bind(this);
   	}
    
   	@Override
   	public void onDestroy() {
      	unbider.unbid();
   	}
}
```

这里我们可以看到仅仅用了一个注解就可以实现对控件的绑定，再也不需要大量重复的控件查找方法了，这里我们实现的框架只针对一个控件绑定，并且只针对 Activity 下的控件绑定，其他比如资源绑定、点击事件等不去实现，因为其原理是一样的。根据 ButterKnife 的用法，我们的框架需要制定一些规则如下：

* 注解只能作用于成员变量上，并且成员变量的类型必须是 View 或者其子孙类。
* 注解所在的类必须是 Activity 或者其子孙类
* 注解的元素不能有 final、static、private 修饰符

对于前两点很好理解，至于第三点，之后在生成代码后便会明白。

## 模块定义与实现

针对上面的需求分析，我们需要建立多个模块，定义如下：

* annotations：用于存放所有的注解，Java 模块
* compiler：用于编写注解处理器，Java 模块
* jbind_sdk：给开发者提供使用的 api，Android 模块
* app：测试模块， Android 模块

其依赖关系是：complier 依赖 annotations，jbind_sdk 依赖 annotations，app 依赖 jbind_sdk。

### annotations 模块

对于 annotations 模块非常简单，这里我们只需要定义一个注解，之前制定的规则是注解只能作用于成员变量上，在这里体现了，只需要将注解的修饰对象范围指定为 ElementType.FIELD 即可，所以我们这样定义：

``` java
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.FIELD)
public @interface JBind {
    int value();
}
```

### compiler 模块

这个模块的功能主要是对代码中的注解进行扫描、处理并生成对应的代码，在进行编码前，我们需要对 Java 文件的结构有所了解。Java 文件是由 Element（元素）构成的，它分为以下几种：

* VariableElement 一般代表成员变量
* ExecutableElement 一般代表类中的方法
* TypeElement 一般代表代表类
* PackageElement 一般代表包

所以在扫描注解时，返回的其实就是一个 Element，里面包含我们需要的所有信息（直接或间接）。

#### 扫描

我们编写一个类，继承于 AbstractProcessor，如下：

``` java
public class JBindProcess extends AbstractProcessor {
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

   	@Override
   	public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
      	// 获取所有使用 JBind 注解的元素
      	Set<? extends Element> elements = roundEnvironment.getElementsAnnotatedWith(JBind.class);
		return false;
	}
}
```

解释都在注释里了，这段代码基本可以看作是模板，编写此类框架都要这样写，后续的处理基本上都是在 process() 方法中了。

需要注意一个地方，之前说过注册注解器需要定义一个 javax.annotation.processing.Processor 文件，Google 官方给了一个插件可以自动生成这个文件，使用方式是在类名前加上注解：

``` java
@AutoService(Processor.class)
public class JBindProcess extends AbstractProcessor
```

当然使用前需要引入一个包，在 build.gradle 中的 dependencies 标签中加入：

``` gradle
compile 'com.google.auto.service:auto-service:1.0-rc3'
```

#### 处理

对于扫描到的注解，我们应该要把它结构化，在一个类里面含有的所有注解信息应该是一个对象，每一个注解信息也应该是一个对象。首先定义一个代表单个注解信息的类：

``` java
// 每一个注解信息需要提供三个方法：获取注解元素类型，获取注解元素名称、获取注解值
public class JBindField {
   	private VariableElement variableElement;
   	private int resId;

   	JBindField(VariableElement variableElement) {
      	this.variableElement = variableElement;
      	JBind bind = variableElement.getAnnotation(JBind.class);
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
```

代码很简单，就不多解释了，下面再定义一个代表一个类里面所有注解信息的类：

``` java
public class JBindClass {
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
}
```

定义好注解类之后我们的任务就很简单了，就是把扫描到的所有注解转换成 JBindClass 对象，然后再根据 JBindClass 对象生成代码。当然在转换过程中不要忘了我们之前制定的规则，需要对注解信息进行判断，这里简单介绍一下之前提到过的 Messager 类。

Messager 是 Javac 在编译过程中用来打印日志的辅助工具，但它作用不止于此，就本人现在知道的，它可以中断编译。什么意思呢，Messager 信息分为几种：ERROR、WARNING、MANDATORY_WARNING、NOTE、OTHER，从字面意思就可以看出各自的含义，如果指定信息为 ERROR，那么在运行到这句代码时，不仅会在编译控制台打印消息，还会直接中断编译。仔细一想就明白了，我们平时编译程序如果有语法错误，Javac 便是通过这个工具来打印错误消息并且中断编译的。

在我们的 JBindProcess 中，只需要用到 ERROR 和 NOTE 这两个类型，我们便可以写出几个辅助方法，如下：

``` java
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
```

了解了日志辅助工具后，我们可以开始进行注解信息到 JBindClass 的转换过程了，根据之前的规则，可以写一个公共的方法，用来判断给定元素的类型是不是某个类型及其子孙类，代码如下：

``` java
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
```

这里要稍微解释下 TypeMirror。Element 代表的是源代码，TypeElement 代表的是源代码中的类型元素，例如类。然而，TypeElement 并不包含类本身的信息。我们可以从 TypeElement 中获取类的名字，但是获取不到类的信息，例如它的父类。这种信息需要通过 TypeMirror 获取。我们可以通过调用 Element 对象的 asType() 获取元素的 TypeMirror。有了这个方法，我们便开始再 process() 方法里面写代码：

``` java
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
	return false;
}
```

这里的代码就蛮简单了，仅仅是作了些简单的判断，并且把最后的注解信息放到了一个 Map 中存储，方便后面进行代码生成。值得一提的是这个方法的返回值，若返回 false，表示本轮注解未声明并且可能要求后续其它的 Processor 处理它们；若返回 true，则代表这些注解已经声明并且不要求后续 Processor 来处理它们。所以当发生错误后，调用 error() 方法打印消息并中断编译，返回 true 不再进行其他处理器的处理。

#### 代码生成

我们回过头去想一想我们需要生成什么样的代码。肯定是要一个单独的类用来盛放像 findViewById() 这样的方法，最简单的做法就是针对每一个 Activity 都有一个对应的类来管理这些代码，而这些代码方法构造器中，使用时进行反射处理便可以进行调用，有了这个想法，我们应该想生成这样的代码（假设在 MainActivity 中定义了一个 textView 的 TextView）：

``` java
// 有这个注解是因为 Android 在编译时会检查 findViewById() 的参数是否为 @ResId 类型，这里忽略了检查，否则编译不通过
@SuppressWarnings("ResourceType")
public class MainActivity_JBind {
   	MainActivity activity;

   	public MainActivity_JBind(MainActivity activity) {
      	this(activity, activity.getWindow().getDecorView());
  	}

  	// 有两个构造方法是为了扩展考虑，这里一个构造器也行
  	public MainActivity_JBind(MainActivity activity, View view) {
     	if (activity == null) {
        	return;
     	}
     	this.activity = activity;
      	activity.textView = (TextView) view.findViewById(xxx);
   	}
}
```

这里的代码可以解释之前我们制定的规则的第三点：注解的元素不能有 final、static、private 修饰符，如果有，便不满足 Java 语言的规则。

这里其实还有一个问题，就是对象释放的问题，毕竟不是在 Activity 中操作的，如果不释放 activity 的引用有可能造成内存泄露，所以还需要一个 unbind() 方法，那是不是直接加一个 unbind() 方法呢？这个当然可以，本人一开始也是这样做的，但是在读了大神的源码后豁然开朗，使用动态代理，先定义一个接口，里面有一个 unbind() 方法，生成的类实现这个接口，再去实现具体的方法。这样做的好处在于，在外部调用生成类构造器绑定控件时可以返回这个接口，需要释放引用时调用接口即可，不用再次反射生成类来调用它本身的 unbind() 方法，这样提高了出现运行效率。不得不说大神的思维就是 666 啊！接口可以这样定义：

``` java
public interface Unbinder {
   	void unbind();

   	Unbinder Empty = new Unbinder() {
     	@Override
     	public void unbind() {
      	}
   	};
}
```

这个接口可以放到 jbind_sdk 模块中去，更加符合模块化的思想。这里定义了一个 Empty 实现了空方法，是为了在反射出错时不至于给一个空的 Unbinder 对象发生空指针，这一招也是学大神的。

接口定义好了，我们修改下生成类的代码，如下：

``` java
@SuppressWarnings("ResourceType")
public class MainActivity_JBind implements Unbinder {
   	MainActivity activity;

   	public MainActivity_JBind(MainActivity activity) {
   	   this(activity, activity.getWindow().getDecorView());
   	}

   	public MainActivity_JBind(MainActivity activity, View view) {
   		if (activity == null) {
        	return;
      	}
      	this.activity = activity;
      	activity.textView = (TextView) view.findViewById(xxx);
   	}

   	@Override
   	public void unbind() {
     	if (activity == null) {
       	return;
      	}
      	activity.textView = null;
      	activity.textView2 = null;
      	activity = null;
    }
}
```

在生成代码前我们需要了解下 javapoet 这个东西，这是一个用于代码生成的工具，比起自己用字符串去拼接，这个简直不能再好了！推荐一篇帖子了解 javapoet 的使用——[javapoet，让你从重复无聊的代码中解放出来](http://www.jianshu.com/p/95f12f72f69a)。

熟悉了 javapoet 后我们在 JBindClass 写一个方法，代码如下：

``` java
// 这里使用 ClassName 的 get() 方法拿到指定具体路径的类名称
private static final ClassName Unbinder = ClassName.get("com.jinchim.jbind_sdk", "Unbinder");
private static final ClassName View = ClassName.get("android.view", "View");

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
   	unbind.addStatement("activity = null");
   	// 添加成员变量
   	FieldSpec.Builder field = FieldSpec.builder(TypeName.get(typeElement.asType()), "activity");
   	// 添加类注解
   	AnnotationSpec.Builder suppressWarnings = AnnotationSpec
   		.builder(SuppressWarnings.class)
      	.addMember("value", "\"ResourceType\"");
   	// 构建类对象
   	TypeSpec typeSpec = TypeSpec
   		.classBuilder(typeElement.getSimpleName() + "_JBind")
     	.addModifiers(Modifier.PUBLIC)
      	.addAnnotation(suppressWarnings.build())
      	.addSuperinterface(Unbinder)
      	.addField(field.build())
      	.addMethod(constructor1.build())
      	.addMethod(constructor2.build())
      	.addMethod(unbind.build())
      	.build();
   	return JavaFile.builder(elements.getPackageOf(typeElement).getQualifiedName().toString(), typeSpec).build();
}
```

然后再在 process() 方法中加一些代码：

``` java
 @Override
public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
   	// 记录扫描到的指定的注解信息
   	Map<TypeElement, JBindClass> jBindClassMap = new LinkedHashMap<>();
   	// 获取所有使用 JBind 注解的元素
   	Set<? extends Element> elements = roundEnvironment.getElementsAnnotatedWith(JBind.class);
   	for (Element element : elements) {
		... // 表示之前的代码
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
```

至此，compiler 模块就完成了，此时如果在 app 模块写有 @JBind 注解，编译后便会生成我们想要的代码，下面来实现给开发者调用的 jbind_sdk 模块应该怎样设计。

### jbind_sdk 模块

对于生成好的代码，它有一个特点，里面只有两个构造方法和一个 unbind() 方法，而且他的路径就在注解所在 Activity 的那个包下面，名称就是 (Activity)_JBind。有了这个特点，我们写一个这样的类即可：

``` java
public class JBindSDK {
   	private final static String TAG = JBindSDK.class.getSimpleName();
   	private static Map<Class<?>, Constructor<? extends Unbinder>> map = new LinkedHashMap<>();

   	public static Unbinder bind(Activity activity) {
      	String className = activity.getPackageName() + "." + activity.getLocalClassName() + "_JBind";
      	try {
         	Class clazz = Class.forName(className);
         	Constructor<? extends Unbinder> constructor = map.get(clazz);
         	if (constructor == null) {
            	constructor = clazz.getConstructor(activity.getClass());
            	map.put(clazz, constructor);
         	}
         	return constructor.newInstance(activity);
      	} catch (Exception e) {
        	return Unbinder.Empty;
      	}
   	}
}
```

首先利用反射获取生成类的类对象，然后获取其构造器对象调用之，返回一个 Unbinder 对象，如果发生异常便返回一个空的 Unbinder 对象，不至于外部调用时发生空指针异常。 

这里还有一个小技巧，就是把生成类的构造器对象缓存了起来，提高了程序的执行效率。

至此，框架开发基本完成，使用方式和 ButterKnife 基本相同（文章开头提到过）。

项目的 github 地址：[https://github.com/jinchim/JBind-Android/](https://github.com/jinchim/JBind-Android/)

## 如何引用框架

对于 ButterKnife，有一个非常优雅的引入方式：

``` gradle
compile 'com.jakewharton:butterknife:8.8.1'
annotationProcessor 'com.jakewharton:butterknife-compiler:8.8.1'
```

这是 gradle 提供的优雅地引入外部开发包的方式，需要把项目编译打包并且上传至 jcenter，至于如何把项目上传至 jcenter，推荐一篇大神的博客：[Android 快速发布开源项目到 jcenter](http://blog.csdn.net/lmj623565791/article/details/51148825/)。

## 结束语

写博客并不只是记录自己的学习过程，因为在写博客的过程中，也会更加理解所写的内容，虽然比较花时间，但这是一个很好的总结过程，也希望自己能一直坚持下去。坚信一句话，量变引起质变！

参考文章：

* [Java注解处理器](https://race604.com/annotation-processing/)
* [Android 如何编写基于编译时注解的项目](http://blog.csdn.net/lmj623565791/article/details/51931859/)
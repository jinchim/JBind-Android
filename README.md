# 控件绑定框架

## 使用步骤

### 引入工程

在对应模块的 build.gradle 中的 dependencies 标签中加入：

``` gradle
compile 'com.jinchim:jbind:1.0.2'
annotationProcessor "com.jinchim:jbind-compiler:1.0.2"
```

### 初始化

``` java
Unbinder unbinder;

@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    unbinder = JBindSDK.bind(this);
}
```

### 绑定控件

``` java
@JBind(R.id.tv) TextView textView;
```

### 释放引用

``` java
@Override
protected void onDestroy() {
    super.onDestroy();
    unbinder.unbind();
}
```

## 注意事项

* 暂时只支持在 Activity 下进行控件绑定，如果不满足条件，编译不通过
* 绑定的控件必须是 View 或者其子孙类，如果不满足条件，编译不通过
* 声明的变量不能有 static、final、private 修饰符，如果不满足条件，编译不通过
* 初始化必须在 setContentView() 后调用才起作用
* 尽量在 onDestroy() 中释放引用，否则可能引起内存泄露
* 本项目用于学习使用，并且项目的开发过程都记录在博客中：[友情链接](http://jinchim.com/2017/08/23/JBind/)
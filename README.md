# 控件绑定框架

## 使用步骤

### 引入工程

在对应模块的 build.gradle 中的 dependencies 标签中加入：

``` gradle
compile 'com.jinchim:jbind:1.0.5'
annotationProcessor 'com.jinchim:jbind-compiler:1.0.5'
```

### 初始化

在 Activity 下：

``` java
Unbinder unbinder;

@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    unbinder = JBindSDK.bind(this);
}
```

在 Fragment 下：

``` java
Unbinder unbinder;

@Override
public void onViewCreated(View view, Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    unbinder = JBindSDK.bind(this, view);
}
```

### 绑定控件

``` java
@Bind(R.id.tv) TextView textView;
```

### 控件点击事件监听

``` java
@Click(R.id.btn)
void onClickBtn(View v) {
    ...
}
```

或者：

``` java
@Click({R.id.btn, R.io.btn2})
void onClickBtn(View v) {
    ...
}
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

* 暂时只支持在 Activity 和 Fragment 下进行控件绑定和控件点击事件监听，否则编译不通过
* 绑定的控件必须是 View 或者其子孙类，否则编译不通过
* 绑定的控件或者方法不能有 static、final、private 修饰符，否则编译不通过
* 对于同一个 Activity 或 Fragment 不能有相同的 ID 进行事件监听，否则编译不通过，而控件绑定可以有相同的 ID
* 控件点击事件监听的方法参数必须是无参或者只带有一个 View 类型的参数，否则编译不通过
* 尽量在 onDestroy() 中释放引用，否则可能引起内存泄露
* 本项目用于学习使用，并且项目的开发过程都记录在博客中：[友情链接](http://jinchim.com/2017/08/23/JBind/)
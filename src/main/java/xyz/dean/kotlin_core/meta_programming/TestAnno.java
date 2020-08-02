package xyz.dean.kotlin_core.meta_programming;

import xyz.dean.myprocessor.annotation.MyAnnotation;

/*
 使用注解处理器实现了一个弱化版本的AutoService(https://github.com/google/auto/tree/master/service)，以实现自定义注解处理器的自动注册过程。
 子项目：autoprocessor -- 自定义注解处理器的自动注册
 子项目：myprocessor -- 用于测试的注解处理器
 因为kapt不支持对kotlin代码进行多轮处理，因此需要将测试用的注解处理器放到单独的项目中。
 */
@MyAnnotation
public class TestAnno {
}

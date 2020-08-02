package xyz.dean.autoprocessor.meta_programming

import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.RoundEnvironment
import javax.annotation.processing.SupportedAnnotationTypes
import javax.annotation.processing.SupportedSourceVersion
import javax.lang.model.SourceVersion
import javax.lang.model.element.Element
import javax.lang.model.element.TypeElement
import javax.tools.Diagnostic
import javax.tools.StandardLocation

@ExperimentalStdlibApi
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedAnnotationTypes("xyz.dean.autoprocessor.meta_programming.annotation.AutoProcessor")
class ProcessAutoProcessor : AbstractProcessor() {
    // annotations--这个Processor所有可处理的注解类型
    // roundEnv--当前处理轮次的上下文
    override fun process(annotations: MutableSet<out TypeElement>?, roundEnv: RoundEnvironment?): Boolean {
        log("====> 开始处理")

        if (annotations.isNullOrEmpty() || roundEnv == null) {
            return true
        }
        val resourceFile = "META-INF/services/javax.annotation.processing.Processor"
        val existingFile = processingEnv.filer.createResource(StandardLocation.CLASS_OUTPUT, "", resourceFile)
        val w = existingFile.openWriter()
        annotations.forEach {annotationType ->
            log("Annotation: $annotationType =========>")
            roundEnv.getElementsAnnotatedWith(annotationType)
                .forEach {annotateElement: Element ->
                    log(annotateElement.toString())
                    w.write(annotateElement.toString())
                }
        }
        w.close()
        log("=====> 完了")
        return false
    }

    private fun log(msg: String) {
        processingEnv.messager.printMessage(Diagnostic.Kind.WARNING, msg)
    }
}
package xyz.dean.kotlin_core.design_pattren

import kotlin.reflect.full.createInstance
import kotlin.reflect.full.superclasses

interface Computer {
    val cpu: String

    companion object {
        operator fun invoke(type: ComputerType): Computer {
            return when (type) {
                ComputerType.PC -> PersonalComputer()
                ComputerType.SERVER -> Server()
            }
        }
    }
}

class PersonalComputer(override val cpu: String = "Core") : Computer
class Server(override val cpu: String = "Xeon") : Computer

enum class ComputerType {
    PC, SERVER
}

class ComputerFactory {
    fun produce(type: ComputerType): Computer {
        return when (type) {
            ComputerType.PC -> PersonalComputer()
            ComputerType.SERVER -> Server()
        }
    }
}

interface CPU { val model: String }
interface GPU { val model: String }

class CoreCPU(override val model: String = "Core") : CPU
class RyzenCPU(override val model: String = "Ryzen") : CPU
class HDGPU(override val model: String = "HD") : GPU
class RXGPU(override val model: String = "RX") : GPU

//interface Factory {
//    fun produceCPU(): CPU
//    fun produceGPU(): GPU
//}
//
//class IntelFactory : Factory {
//    override fun produceCPU(): CPU = CoreCPU()
//    override fun produceGPU(): GPU = HDGPU()
//}
//
//class AMDFactory : Factory {
//    override fun produceCPU(): CPU = RyzenCPU()
//    override fun produceGPU(): GPU = RXGPU()
//}

interface Factory {
    fun produce(): Computer

    companion object {
        operator fun invoke(className: String): Factory {
            val clazz = Class.forName(className).kotlin
            val isSubClass = clazz.superclasses
                .map { it.simpleName }
                .contains(Factory::class.simpleName)
            if (!isSubClass) throw IllegalArgumentException("$className is not A subclass of ${Factory::class.simpleName}")
            return clazz.createInstance() as Factory
        }
    }
}

fun main() {
    val pc = Factory("xyz.dean.kotlin_core.design_pattren.PCFactory").produce()
}

class PCFactory : Factory {
    override fun produce(): Computer {
        return PersonalComputer()
    }
}
//class ServerFactory : Factory {
//    override fun produce(): Computer {
//        ...
//    }
//}
//class MicroComputerFactory : Factory {
//    override fun produce(): Computer {
//        ...
//    }
//}
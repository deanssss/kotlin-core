package xyz.dean.kotlin_core.extensions

open class Base
class Extended : Base()

open class X {
    open fun Base.foo() {
        println("I'm Base.foo in X.")
    }

    open fun Extended.foo() {
        println("I'm Extended.foo in X.")
    }
}

class Y : X() {
    override fun Base.foo() {
        println("I'm Base.foo in Y.")
    }

    override fun Extended.foo() {
        println("I'm Extended in Y.")
    }
}

fun main() {
    val x: X = X()
    val y: X = Y()

    x.apply {
        Base().foo()
        Extended().foo()
    }

    y.apply {
        Base().foo()
        Extended().foo()
    }
}
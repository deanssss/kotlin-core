package xyz.dean.kotlin_core.extensions

class Son {
    fun foo() {
        println("Foo in class Son.")
    }
}

class Parent {
    fun foo() {
        println("Foo in class Parent.")
    }

    fun Son.foo2() {
        this.foo()
        this@Parent.foo()
    }
}

object Test {
    @JvmStatic
    fun main(args: Array<String>) {
        Parent().apply {
            Son().foo2()
        }
    }
}
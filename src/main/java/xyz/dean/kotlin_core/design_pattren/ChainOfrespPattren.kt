package xyz.dean.kotlin_core.design_pattren

import java.lang.IllegalArgumentException

data class ApplyEvent(val money: Int, val title: String)
interface ApplyHandler {
    val successor: ApplyHandler?
    fun handleEvent(event: ApplyEvent)
}

class GroupLeader(override val successor: ApplyHandler?) : ApplyHandler {
    override fun handleEvent(event: ApplyEvent) {
        if (event.money <= 100) {
            println("Group Leader handled this event: ${event.title}.")
        } else {
            successor?.handleEvent(event)
                ?: println("Group Leader: This event can not be handle.")
        }
    }
}

class DepartmentLeader(override val successor: ApplyHandler?) : ApplyHandler {
    override fun handleEvent(event: ApplyEvent) {
        if (event.money <= 500) {
            println("Department Leader handled this event: ${event.title}.")
        } else {
            successor?.handleEvent(event)
                ?: println("Department Leader: This event can not be handle.")
        }
    }
}

class Boss : ApplyHandler {
    override val successor: ApplyHandler? = null
    override fun handleEvent(event: ApplyEvent) {
        if (event.money <= 1000) {
            println("Boss handled this event: ${event.title}.")
        } else {
            println("Boss: This event is refused.")
        }
    }
}

fun t(a: Int, b: Int, c: Int): Int = 0
fun pt(a: Int, b: Int) = t(a, b, 10)

fun main() {
    GroupLeader(DepartmentLeader(Boss())).handleEvent(ApplyEvent(200, "Buy books"))
}

class PartialFunction<P, R>(
    private val defineAt: (P) -> Boolean,
    private val f: (P) -> R
) {
    operator fun invoke(p: P): R {
        if (defineAt(p)) {
            return f(p)
        } else {
            throw IllegalArgumentException("Value $p is not support by this function.")
        }
    }

    fun orElse(next: PartialFunction<P, R>): PartialFunction<P, R> {
        return PartialFunction(
            defineAt = { this.defineAt(it) || next.defineAt(it) },
            f = {
                when {
                    this.defineAt(it) -> this(it)
                    else -> next(it)
                }
            }
        )
    }
}
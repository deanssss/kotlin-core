package xyz.dean.kotlin_core.functional

////////////////////
// 传统的继承方法
////////////////////
//interface Pet {
//    val name: String
//    fun rename(newName: String): Pet
//}
//
//data class Cat(override val name: String) : Pet {
//    override fun rename(newName: String): Pet = this.copy(name = newName)
//}
//
// val cat1 = Cat("BuDing")
// val cat2 = cat1.rename("DouHua") // 没问题，还是只猫
//
//data class Dog(override val name: String) : Pet {
//    override fun rename(newName: String) = Cat(newName) // 手抖，写错了
//}
//
// val dog1 = Dog("WangCai")
// val dog2 = dog1.rename("FuGui") // Oops！狗不是狗，改个名字就成猫了
//
//// 尤其是，继承这种写法，我们没法给它弄个通用方法，还能保住类型
// fun <A : Pet> esquire(pet: A): A = pet.rename("${pet.name}, Esq.") // 编译失败，无法向下转型

////////////////////////////////////////////////////////////////
// F-Bounded Types，我们使用这种技术将子类型参数化，然后传递给父类
////////////////////////////////////////////////////////////////
//interface Pet<A : Pet<A>> {
//    val name: String
//    fun renamed(newName: String): A
//}
//
//open class Cat(override val name: String) : Pet<Cat> {
//    override fun renamed(newName: String) = Cat(newName)
//}
//
//// 不错！通用方法可以工作了
//fun <A : Pet<A>> esquire(a: A) = a.renamed("${a.name}, Esq.")
//
//// 但是搞错当前类型的情况还是可能存在
//data class Dog(override val name: String, val color: Int) : Pet<Cat> {
//    override fun renamed(newName: String) = copy(name = newName) // 不小心又写错了，狗子又变成了猫
//}
//
//// 还有另一种可能
//
//class Kitty(name: String) : Cat(name)
//val kitty1 = Kitty("MiaoMiao")
//val kitty2 = kitty1.renamed("MiMi") // 小猫长大了！！

//////////////////////
// Typeclass如何解决？
/////////////////////
interface Pet {
    val name: String
}

interface Rename<T : Pet> {
    fun T.rename(newName: String): T
}

data class Cat(override val name: String) : Pet

object RenameCat : Rename<Cat> {
    override fun Cat.rename(newName: String) = copy(name = newName)
}

fun <T : Pet> esquire(pet: T, rename: Rename<T>): T {
    return rename.run { pet.rename("${pet.name}, Esq.") }
}

val cat1 = Cat("BuDing")
val cat2 = RenameCat.run { cat1.rename("DouHua") }
val cat3 = esquire(cat1, RenameCat)

data class Dog(override val name: String) : Pet

object RenameDog : Rename<Cat> {
    override fun Cat.rename(newName: String) = copy(name = newName) // 又又手滑了
}

val dog1 = Dog("WangCai")
//val dog2 = RenameDog.run { dog1.rename() } // 这下编译器不干了
//val dog3 = esquire(dog1, RenameDog) // 叫你不认真写代码！

////////////////////
// 简单的Typeclass
////////////////////
interface Eq<T> {
    fun T.equal(t: T): Boolean
}

object IntEq : Eq<Int> {
    override fun Int.equal(t: Int) = this == t
}
object StringEq : Eq<String> {
    override fun String.equal(t: String): Boolean = this == t
}

//////////////////////////////////////
// Typeclass与高阶类型结合处理容器类型
/////////////////////////////////////
interface Kind<out F, out A>

interface Functor<F> {
    fun <A, B> Kind<F, A>.map(f: (A) -> B): Kind<F, B>
}

object ListHK
class ListW<T>(list: List<T>) : Kind<ListHK, T>, List<T> by list
@Suppress("UNCHECKED_CAST")
fun <T> Kind<ListHK, T>.fix(): List<T> = this as List<T>

object ListFunctor : Functor<ListHK> {
    override fun <A, B> Kind<ListHK, A>.map(f: (A) -> B): Kind<ListHK, B> {
        val newList = this.fix().map { f(it) }
        return ListW(newList)
    }
}

object StoreHK
class Store<T>(private val value: T) : Kind<StoreHK, T> {
    fun read(): T = value
}
fun <T> Kind<StoreHK, T>.fix(): Store<T> = this as Store<T>
object StoreFunctor : Functor<StoreHK> {
    override fun <A, B> Kind<StoreHK, A>.map(f: (A) -> B): Kind<StoreHK, B> {
        val oldValue = this.fix().read()
        return Store(f(oldValue))
    }
}

fun <F, A> mapToString(container: Kind<F, A>, functor: Functor<F>): Kind<F, String> {
    return functor.run {
        container.map { "String-$it" }
    }
}

fun main() {
    val intStore = Store(100)
    val list = ListW(listOf(1, 2, 3))

    val newStore = mapToString(intStore, StoreFunctor)
    val newList = mapToString(list, ListFunctor)

    println(newStore.fix().read())
    newList.fix().forEach { println(it) }
}
package xyz.dean.kotlin_core.functional

interface IntFilterList {
    fun filter(p: (Int) -> Boolean): IntFilterList
    fun remove(p: (Int) -> Boolean): IntFilterList = filter { !p(it) }
}

interface FloatFilterList {
    fun filter(p: (Float) -> Boolean): FloatFilterList
    fun remove(p: (Float) -> Boolean): FloatFilterList = filter { !p(it) }
}

// IntFilterList, FloatFilterList... 它们都是具体的类型
// 具体的类型带来的一个麻烦是：那些看起来相似的操作，我们不得不为每个类型都实现一遍，如上面的remove
// 此时，泛型是一个不错的解决办法

interface FilterList<T> {
    fun filter(p: (T) -> Boolean): FilterList<T>
    fun remove(p: (T) -> Boolean): FilterList<T> = filter { !p(it) }
}

// 然而随着我们抽象程度的提高，现有的泛型似乎也不能很好的满足需求了
interface FilterSet<T> {
    fun filter(p: (T) -> Boolean): FilterSet<T>
    fun remove(p: (T) -> Boolean): FilterSet<T> = filter { !p(it) }
}

interface FilterQueue<T> {
    fun filter(p: (T) -> Boolean): FilterQueue<T>
    fun remove(p: (T) -> Boolean): FilterQueue<T> = filter { !p(it) }
}

// 讨厌的冗余代码又出现了，你可能会想到继承

interface FilterContainer<T> {
    fun filter(p: (T) -> Boolean): FilterContainer<T>
    fun remove(p: (T) -> Boolean): FilterContainer<T> = filter { !p(it) }
}

interface MFilterList<T> : FilterContainer<T>
interface MFilterSet<T> : FilterContainer<T>

// 但是，当我们调用从父类继承来的方法时，子类型的信息丢失了
val list1: MFilterList<Int>? = null
val list2 = list1?.remove { it > 10 } // FilterContainer<Int>?类型

// 啧，这不是一个好办法，让我们回到泛型，再次尝试用泛型处理容器类型：将List<T>、Set<T>用一个指代泛型的变量C<_>来代替
/*
interface Filterable<C<_>> {
    fun filter(p: (_) -> Boolean): C<_>
    fun remove(p: (_) -> Boolean): C<_> = filter { !p(it) }
}
*/
// 需要注意的是Kotlin并不支持上面这种用法，这只是个伪代码。不过，至此你已经见过高阶类型了，上面这个Filterable就是一个高阶类型
// 它接受一个泛型类型，返回一个泛型类型。比如我们给C<_>赋值为List<T>，将会得到一个新的泛型类型Filterable<List<T>>。
// 参考： https://zhuanlan.zhihu.com/p/29021140
//       https://hongjiang.info/scala-higher-kinded-type/

// ***** Typeclass *****
// 在Java等OOP语言中，若我们想为某些类型实现一些共同的行为，通常的做法是为这些类型提供一个抽象基类或者interface，
// 然后通过继承覆写的方式来为这些类型实现各自的行为。

// Typeclass来自于Haskell，它有些像Java中的interface：
// A typeclass is a sort of interface that defines some behavior.
// If a type is a part of a typeclass, that means that it supports and implements the behavior the typeclass describes.
// 参考： https://scala.cool/tags/Subtyping-vs-Typeclasses/
//        https://blog.cc1234.cc/articles/typeclasses-1/typeclasses-1.html
// 实际上Typeclass是对类型的再一次抽象，它在类型的层面上找出共同的行为，比如对于值或者对象，它们可以在同类之间比较是否相同(equal)
// 那么，比较是否相同这个行为就是这些类型之间的共同行为，我们可以用一个Typeclass来表示它。

interface Eq<T> {
    fun T.equal(t: T): Boolean
}

object IntEq : Eq<Int> {
    override fun Int.equal(t: Int) = this == t
}
object StringEq : Eq<String> {
    override fun String.equal(t: String): Boolean = this == t
}

//fun main() {
//    IntEq.apply {
//        12.equal(12)
//        13.equal(12)
//    }
//    StringEq.apply {
//        "aaa".equal("aaa")
//        "ccc".equal("aaa")
//    }
//}

// 遗憾的是Kotlin中并没有Scala隐式查找的特性，因此在实现Typeclass显得有些丑陋。
// 暂时不管它，来看下Typleclass与告诫类型的结合
// 现在我们要为list类型实现equal

interface Kind<F, A>

object ListHK

class ListW<T>(list: List<T>) : Kind<ListHK, T>, List<T> by list

@Suppress("UNCHECKED_CAST")
fun <T> Kind<ListHK, T>.fix(): List<T> = this as List<T>

abstract class ListEq<T>(private val ieq: Eq<T>) : Eq<Kind<ListHK, T>> {
    override fun Kind<ListHK, T>.equal(t: Kind<ListHK, T>): Boolean {
        val current = this.fix()
        val target = t.fix()

        if (current == target) return true
        if (current.size != target.size) return false

        for(i in 0 until current.size) {
            ieq.apply {
                if (!current[i].equal(target[i])) {
                    return false
                }
            }
        }
        return true
    }
}

object IntListEq : ListEq<Int>(IntEq)

fun main() {
    IntListEq.apply {
        val list1 = ListW(listOf(1, 2, 3))
        val list2 = ListW(listOf(1, 2, 3, 4))
        println(list1.equal(list2))
    }
}


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

// F-Bounded Types，我们使用这种技术将子类型参数化，然后传递给父类

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
//    override fun renamed(newName: String) = Cat(newName) // 不小心又写错了，狗子又变成了猫
//}
//
//// 还有另一种可能
//
//class Kitty(name: String) : Cat(name)
//val kitty1 = Kitty("MiaoMiao")
//val kitty2 = kitty1.renamed("MiMi") // 小猫长大了！！

// Typeclass如何解决？

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
    override fun Cat.rename(newName: String) = copy() // 又又手滑了
}

val dog1 = Dog("WangCai")
//val dog2 = RenameDog.run { dog1.rename() } // 这下编译器不干了
//val dog3 = esquire(dog1, RenameDog) // 叫你不认真写代码！
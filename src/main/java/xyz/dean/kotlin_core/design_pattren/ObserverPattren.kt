package xyz.dean.kotlin_core.design_pattren

import java.util.*
import kotlin.properties.Delegates

interface StockWatcher {
    fun onRise(price: Int)
    fun onFall(price: Int)
}

class Stock : Observable() {
    var watchers = mutableListOf<StockWatcher>()
    val price: Int by Delegates.observable(0) { property, oldValue, newValue ->
        watchers.forEach {
            if (oldValue > newValue) it.onFall(newValue)
            else it.onRise(price)
        }
    }
}

class Investor : StockWatcher {
    override fun onRise(price: Int) {
        println("Stock price rose, buying!")
    }
    override fun onFall(price: Int) {
        println("Slump in stock, Selling!")
    }
}
package xyz.dean.kotlin_core.design_pattren

class Robot
public constructor(
    val code: String,
    val battery: String? = null,
    val height: Int? = null,
    val weight: Int? = null
) {
    init {
        require(weight == null || battery != null) {
            "Battery should be determined when setting weight."
        }
    }

    class Builder(private val code: String) {
        private var battery: String? = null
        private var height: Int? = null
        private var weight: Int? = null

        fun setBattery(battery: String?): Builder = this.apply {
            this.battery = battery
        }
        fun setHeight(height: Int?): Builder = this.apply {
            this.height = height
        }
        fun setWeight(weight: Int?): Builder = this.apply {
            this.weight = weight
        }
        fun build(): Robot = Robot(code, battery, height, weight)
    }
}

fun main() {
    val robot = Robot.Builder("android")
        .setBattery("5000mAh")
        .setHeight(100)
        .setWeight(60)
        .build()

    val robot1 = Robot(
        code = "android2.0",
        height = 120,
        weight = 80
    )
}
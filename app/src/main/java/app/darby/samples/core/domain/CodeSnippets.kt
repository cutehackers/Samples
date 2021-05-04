package app.darby.samples.core.domain

/**
 * test kotlin keyword 'in' and 'out'
 *
 * kotlin generic
 *  out: could assign a class of subtype to class of super-type
 *  in: could assign a class of super-type to a class of subtype
 *
 * java generic
 *  ? extends T: subtype can only read
 *  ? super T: super-type can only write
 */

// in and out --------------------------------------------------------------------------------------
open class Food
open class AsianFood : Food()
class KoreanFood : AsianFood()

// out ---------------------------------------------------------------------------------------------
// out == ? extends T, subtype can only read
// out: could assign a class of subtype to ca lass of super-type
interface Producer<out T> {
    fun produce(): T
}

class FoodProducer : Producer<Food> {
    override fun produce(): Food = Food()
}

class AsianFoodProducer : Producer<AsianFood> {
    override fun produce(): AsianFood = AsianFood()
}

class KoreanFoodProducer : Producer<KoreanFood> {
    override fun produce(): KoreanFood = KoreanFood()
}

//val foodProducer: Producer<KoreanFood> = FoodProducer()             // error
//val asianFoodProducer: Producer<KoreanFood> = AsianFoodProducer()   // error
//val koreanFoodProducer: Producer<KoreanFood> = KoreanFoodProducer()
val foodProducer: Producer<Food> = FoodProducer()
val asianFoodProducer: Producer<Food> = AsianFoodProducer()
val koreanFoodProducer: Producer<Food> = KoreanFoodProducer()

// in ----------------------------------------------------------------------------------------------
// in == ? super T, super-type can only write
// in: could assign a class of super-type to a class of subtype
interface Consumer<in T> {
    fun consume(type: T)
}

class FoodConsumer : Consumer<Food> {
    override fun consume(type: Food) {
    }
}

class AsianFoodConsumer : Consumer<AsianFood> {
    override fun consume(type: AsianFood) {
    }
}

class KoreanFoodConsumer : Consumer<KoreanFood> {
    override fun consume(type: KoreanFood) {
    }
}

//val foodConsumer: Consumer<Food> = FoodConsumer()
//val asianFoodConsumer: Consumer<Food> = AsianFoodConsumer()     // error
//val koreanFoodConsumer: Consumer<Food> = KoreanFoodConsumer()   // error
val foodConsumer: Consumer<KoreanFood> = FoodConsumer()
val asianFoodConsumer: Consumer<KoreanFood> = AsianFoodConsumer()
val koreanFoodConsumer: Consumer<KoreanFood> = KoreanFoodConsumer()
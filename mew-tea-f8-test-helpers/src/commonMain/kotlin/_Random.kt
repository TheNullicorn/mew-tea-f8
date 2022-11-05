package me.nullicorn.mewteaf8

import kotlin.random.Random

private val RANDOM_SEED = "mew-tea-f8".hashCode()

fun createReproducibleRandom(): Random =
    Random(seed = RANDOM_SEED)

inline fun <E, C : MutableCollection<E>> C.addRandomlyWhile(
    condition: (C) -> Boolean,
    generate: (Random) -> E,
    random: Random = createReproducibleRandom()
) {
    while (condition(this))
        this += generate(random)
}
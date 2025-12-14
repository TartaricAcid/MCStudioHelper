package com.github.tartaricacid.nemc.setting

enum class LevelType {
    DEFAULT,
    FLAT;

    val code: Int
        get() = when (this) {
            DEFAULT -> 1
            FLAT -> 2
        }

    val displayName: String
        get() = when (this) {
            DEFAULT -> "默认"
            FLAT -> "超平坦"
        }
}

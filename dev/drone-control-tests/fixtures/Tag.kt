package net.minecraft.nbt

class CompoundTag(
    private val values: MutableMap<String, Any> = mutableMapOf(),
) {
    fun getBoolean(key: String) = values[key] as? Boolean ?: false

    fun getString(key: String) = values[key] as? String ?: ""

    fun putBoolean(
        key: String,
        value: Boolean,
    ) {
        values[key] = value
    }

    fun putString(
        key: String,
        value: String,
    ) {
        values[key] = value
    }

    fun copy() = CompoundTag(values.toMutableMap())
}

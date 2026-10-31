package com.atsuishio.superbwarfare

import com.atsuishio.superbwarfare.tools.SeekTool
import net.minecraft.nbt.CompoundTag
import java.io.DataInputStream

fun iffNbtChecks() {
    // 空目标永远不是友方无人机, 且不碰 NBT。
    check(!SeekTool.IS_FRIENDLY_DRONE.test(null, null))

    // linkedDroneId 真值表: 只有 Using + Linked 同时为 true 才返回配对 UUID。
    check(SeekTool.linkedDroneId(CompoundTag()) == null)
    check(SeekTool.linkedDroneId(CompoundTag().apply { putBoolean("Using", true) }) == null)
    check(SeekTool.linkedDroneId(CompoundTag().apply { putBoolean("Linked", true) }) == null)
    val paired =
        CompoundTag().apply {
            putBoolean("Using", true)
            putBoolean("Linked", true)
            putString("LinkedDrone", "123e4567-e89b-12d3-a456-426614174000")
        }
    check(SeekTool.linkedDroneId(paired) == "123e4567-e89b-12d3-a456-426614174000")

    // IffItem.onServerTick 每 tick 对每对玩家调用 IS_FRIENDLY(_DRONE):
    // 玩家对玩家根本不是无人机, 这里不许再经过拷贝式 NBT 读取
    // (NBTTool.getTag 全量 copyTag, 30x30 对每同步 tick)。
    // Lambda 是 invokedynamic 生成的, 没有独立类文件: 扫宿主 SeekTool 类。
    check(!callsCopyingGetTag(SeekTool::class.java)) {
        "IS_FRIENDLY_DRONE must not call NBTTool.getTag"
    }
    println("IffItem NBT checks passed")
}

/**
 * 给定类字节码的常量池里找 (NBTTool, getTag) 方法引用。
 * 无 ASM 依赖; 找不到类字节码或池格式变化时直接报错, 不静默通过。
 */
private fun callsCopyingGetTag(cls: Class<*>): Boolean {
    val path = "/" + cls.name.replace('.', '/') + ".class"
    val bytes =
        cls.getResourceAsStream(path)?.readBytes()
            ?: error("cannot read own bytecode: $path")
    val input = DataInputStream(bytes.inputStream())
    check(input.readInt() == 0xCAFEBABE.toInt()) { "bad class magic: $path" }
    input.readUnsignedShort()
    input.readUnsignedShort()
    val cpCount = input.readUnsignedShort()
    val utf8 = arrayOfNulls<String>(cpCount)
    val clazz = IntArray(cpCount)
    val methodClass = IntArray(cpCount)
    val methodNameType = IntArray(cpCount)
    val nameAndTypeName = IntArray(cpCount)
    var i = 1
    while (i < cpCount) {
        when (val tag = input.readUnsignedByte()) {
            1 -> {
                val length = input.readUnsignedShort()
                val raw = ByteArray(length)
                input.readFully(raw)
                utf8[i] = String(raw, Charsets.UTF_8)
            }

            3, 4 -> {
                input.skipBytes(4)
            }

            5, 6 -> {
                input.skipBytes(8)
                i++
            }

            7 -> {
                clazz[i] = input.readUnsignedShort()
            }

            8 -> {
                input.skipBytes(2)
            }

            9, 10, 11 -> {
                methodClass[i] = input.readUnsignedShort()
                methodNameType[i] = input.readUnsignedShort()
            }

            12 -> {
                nameAndTypeName[i] = input.readUnsignedShort()
                input.skipBytes(2)
            }

            15 -> {
                input.skipBytes(3)
            }

            16 -> {
                input.skipBytes(2)
            }

            17, 18 -> {
                input.skipBytes(4)
            }

            19, 20 -> {
                input.skipBytes(2)
            }

            else -> {
                error("unknown constant pool tag $tag in $path")
            }
        }
        i++
    }
    for (index in 1 until cpCount) {
        if (methodClass[index] == 0) continue
        val owner = utf8[clazz[methodClass[index]]]
        val name = utf8[nameAndTypeName[methodNameType[index]]]
        if (owner == "com/atsuishio/superbwarfare/tools/NBTTool" && name == "getTag") return true
    }
    return false
}

package com.atsuishio.superbwarfare

import com.atsuishio.superbwarfare.compat.tacz.TaczHeadshotCompat
import com.atsuishio.superbwarfare.tools.DamageTypeTool
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.damagesource.DamageTypes

fun main() {
    deferredRegisterChecks()
    TaczHeadshotCompat.recordHit(10, true)
    check(TaczHeadshotCompat.isHeadshot(10))
    check(!TaczHeadshotCompat.isHeadshot(11))
    check(!TaczHeadshotCompat.isHeadshot(null as Int?))
    TaczHeadshotCompat.recordHit(10, false)
    check(!TaczHeadshotCompat.isHeadshot(10))
    TaczHeadshotCompat.recordHit(11, true)
    check(!TaczHeadshotCompat.isHeadshot(10))
    check(TaczHeadshotCompat.isHeadshot(11))
    check(DamageTypeTool.isKnifeDamage(DamageTypes.PLAYER_ATTACK))
    check(DamageTypeTool.isKnifeDamage(ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.parse("blockfield:knife"))))
    check(!DamageTypeTool.isKnifeDamage(DamageTypes.ARROW))
    check(!DamageTypeTool.isKnifeDamage(ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.parse("tacz:bullet"))))
    meleeReachChecks()
    println("Kill feed, melee reach and deferred registration checks passed")
}

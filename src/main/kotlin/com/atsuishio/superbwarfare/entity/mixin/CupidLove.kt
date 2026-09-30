package com.atsuishio.superbwarfare.entity.mixin

import net.minecraft.world.entity.npc.Villager

@Suppress("FunctionName")
interface CupidLove {
    fun `superbwarfare$setCupidLove`(love: Boolean)

    fun `superbwarfare$getCupidLove`(): Boolean

    companion object {
        fun getInstance(villager: Villager): CupidLove = villager as CupidLove
    }
}

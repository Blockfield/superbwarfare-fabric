package com.atsuishio.superbwarfare.tools

/**
 * Pure killfeed row layout decisions. No Minecraft objects here on purpose:
 * covered by `KillFeedChecks`, which runs without a bootstrapped game.
 */
object KillFeedLayout {
    const val WEAPON_ICON_ADVANCE_PX = 36f
    const val DAMAGE_ICON_ADVANCE_PX = 18f

    // TaCZ/EliteX/LR rifles share the single TaCZ gun item; a vanilla
    // descriptionId always carries the `item.` prefix, so the raw id never matches.
    // shortcut: single-item assumption breaks if LR/EliteX ever register their own gun item; then match their ids here too.
    fun isCompatGunStack(
        stackDescriptionId: String,
        isSbwGun: Boolean,
    ): Boolean = isSbwGun || stackDescriptionId == "item.tacz.modern_kinetic_gun"

    // Rows without an icon reserve no advance, so text-only rows stay uniform.
    fun weaponAdvancePx(hasWeaponIcon: Boolean): Float = if (hasWeaponIcon) WEAPON_ICON_ADVANCE_PX else 0f

    fun damageAdvancePx(hasDamageIcon: Boolean): Float = if (hasDamageIcon) DAMAGE_ICON_ADVANCE_PX else 0f

    // The headshot marker and the compat-gun item sprite share the 12px icon box.
    fun headshotAdvancePx(hasHeadshotIcon: Boolean): Float = damageAdvancePx(hasHeadshotIcon)

    fun compatWeaponAdvancePx(hasCompatWeapon: Boolean): Float = damageAdvancePx(hasCompatWeapon)
}

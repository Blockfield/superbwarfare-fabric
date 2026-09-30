package com.atsuishio.superbwarfare.fabric

import com.atsuishio.superbwarfare.block.entity.BlueprintResearchTableBlockEntity
import com.atsuishio.superbwarfare.block.entity.ChargingStationBlockEntity
import com.atsuishio.superbwarfare.block.entity.CreativeChargingStationBlockEntity
import com.atsuishio.superbwarfare.block.entity.FuMO25BlockEntity
import com.atsuishio.superbwarfare.block.entity.SuperbItemInterfaceBlockEntity
import com.atsuishio.superbwarfare.capability.energy.ItemEnergyStorage
import com.atsuishio.superbwarfare.entity.living.DPSGeneratorEntity
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.item.EnergyStorageItem
import com.atsuishio.superbwarfare.item.blockitem.CreativeChargingStationBlockItem
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.Container
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntity

/** Маркер capability предмета: сам знает, как достать реализацию из стека. */
class ItemCapability<T : Any> internal constructor(
    private val provider: (ItemStack) -> T?,
) {
    /** Для Java-вызовов: Capabilities.EnergyStorage.ITEM.get(stack). */
    fun get(stack: ItemStack): T? = provider(stack)
}

class EntityCapability<T : Any> internal constructor(
    private val provider: (Entity, Direction?) -> T?,
) {
    @JvmOverloads
    fun get(
        entity: Entity,
        context: Direction? = null,
    ): T? = provider(entity, context)
}

class BlockCapability<T : Any> internal constructor(
    private val provider: (BlockEntity, Direction?) -> T?,
) {
    @JvmOverloads
    fun get(
        blockEntity: BlockEntity,
        context: Direction? = null,
    ): T? = provider(blockEntity, context)
}

fun <T : Any> ItemStack.getCapability(capability: ItemCapability<T>): T? = capability.get(this)

fun <T : Any> Entity.getCapability(
    capability: EntityCapability<T>,
    context: Direction? = null,
): T? = capability.get(this, context)

fun <T : Any> BlockEntity.getCapability(
    capability: BlockCapability<T>,
    context: Direction? = null,
): T? = capability.get(this, context)

/**
 * Аналог ILevelExtension.getCapability. Все поставщики мода живут на BlockEntity, поэтому блок
 * без BlockEntity capability не отдаёт.
 */
fun <T : Any> Level.getCapability(
    capability: BlockCapability<T>,
    pos: BlockPos,
    context: Direction? = null,
): T? = this.getBlockEntity(pos)?.let { capability.get(it, context) }

/**
 * Замена системы capabilities из NeoForge с той же формой вызовов.
 *
 * На NeoForge поставщики регистрировались в RegisterCapabilitiesEvent, а потребитель спрашивал
 * `stack.getCapability(Capabilities.EnergyStorage.ITEM)`. Прямого аналога на Fabric нет, а Fabric
 * Transfer API с его транзакциями и дробными единицами пришлось бы натягивать на ~70 мест вызова.
 *
 * Здесь маркер сам знает, как достать реализацию: `Capabilities.*` -- это не ключи реестра, а
 * готовые функции поиска, повторяющие init/ModCapabilities апстрима один в один. Регистрировать
 * нечего, порядок инициализации мода не важен, а места вызова не меняются -- только импорт.
 *
 * ponytail: поставщики разбираются через instanceof на каждый вызов вместо таблицы по типу
 * блока/предмета. Набор закрытый (три блока, два вида предметов, техника, DPS-генератор), цепочка
 * из пяти проверок дешевле промаха кэша. Если сюда доберутся аддоны со своими блоками --
 * заменить `when` на Map<Class<*>, provider>.
 *
 * ponytail: энергия и инвентари мода не видны другим модам, и наоборот -- чужие сундуки и
 * генераторы для SBW пустые. У апстрима межмодовый обмен формально был (общий IEnergyStorage
 * NeoForge), но сам мод им не пользовался. Мост наружу вешать на Fabric Transfer API /
 * team_reborn Energy поверх этих же интерфейсов, когда появится мод, с которым надо дружить.
 */
object Capabilities {
    object EnergyStorage {
        /** Креативная зарядная станция как предмет + любой EnergyStorageItem. */
        @JvmField
        val ITEM: ItemCapability<IEnergyStorage> =
            ItemCapability { stack ->
                when (val item = stack.item) {
                    is CreativeChargingStationBlockItem -> {
                        item.energyStorage
                    }

                    is EnergyStorageItem -> {
                        ItemEnergyStorage(
                            stack,
                            { s -> item.getMaxEnergy(s) },
                            { s -> item.getMaxReceiveEnergy(s) },
                            { s -> item.getMaxExtractEnergy(s) },
                        )
                    }

                    else -> {
                        null
                    }
                }
            }

        /** Зарядная станция, креативная зарядная станция, FuMO25. */
        @JvmField
        val BLOCK: BlockCapability<IEnergyStorage> =
            BlockCapability { blockEntity, side ->
                when (blockEntity) {
                    is ChargingStationBlockEntity -> blockEntity.getEnergyStorage(side)
                    is CreativeChargingStationBlockEntity -> blockEntity.getEnergyStorage(side)
                    is FuMO25BlockEntity -> blockEntity.getEnergyStorage()
                    else -> null
                }
            }

        /** DPS-генератор и любая техника с батареей. */
        @JvmField
        val ENTITY: EntityCapability<IEnergyStorage> =
            EntityCapability { entity, _ ->
                when (entity) {
                    is DPSGeneratorEntity -> entity.energyStorage
                    is VehicleEntity -> if (entity.hasEnergyStorage()) entity.getEnergyStorage() else null
                    else -> null
                }
            }
    }

    object ItemHandler {
        /**
         * Зарядная станция, стол исследования чертежей,卓越物品接口.
         *
         * ponytail: внутри мода этот маркер не спрашивает никто -- на NeoForge он существовал,
         * чтобы до этих блоков дотягивались воронки и чужие моды. Ванильные воронки не пострадали:
         * все три блока и так WorldlyContainer/Container, а с ними ваниль работает напрямую.
         * Чужим модам на Fabric нужен ItemStorage.SIDED из fabric-transfer-api -- регистрировать
         * туда, когда появится мод, с которым надо стыковаться.
         */
        @JvmField
        val BLOCK: BlockCapability<IItemHandler> =
            BlockCapability { blockEntity, side ->
                when (blockEntity) {
                    is ChargingStationBlockEntity -> {
                        when {
                            side == null || blockEntity.isRemoved -> null
                            side == Direction.UP || side == Direction.DOWN -> SidedInvWrapper(blockEntity, side)
                            else -> SidedInvWrapper(blockEntity, Direction.NORTH)
                        }
                    }

                    is BlueprintResearchTableBlockEntity -> {
                        when {
                            blockEntity.isRemoved -> null

                            side == Direction.UP || side == Direction.DOWN ||
                                side == Direction.NORTH || side == Direction.SOUTH -> SidedInvWrapper(blockEntity, side)

                            else -> SidedInvWrapper(blockEntity, Direction.EAST)
                        }
                    }

                    is SuperbItemInterfaceBlockEntity -> {
                        InvWrapper(blockEntity)
                    }

                    else -> {
                        null
                    }
                }
            }

        /**
         * Техника с грузовым отсеком плюс то, что на NeoForge отдавал сам загрузчик: инвентарь
         * игрока (мод считает по нему патроны) и ванильные контейнеры-сущности -- сундук-лодка,
         * вагонетки.
         *
         * ponytail: у мобов без Container рюкзака нет -- NeoForge отдавал им руки и броню, здесь
         * getCapability вернёт null. Ни одна стрельба мода из этого не исходит: патроны считаются
         * у игрока, у техники и у ящиков. Понадобятся стреляющие мобы -- добавить сюда обёртку
         * над EquipmentSlot.
         */
        @JvmField
        val ENTITY: EntityCapability<IItemHandler> =
            EntityCapability { entity, _ ->
                when (entity) {
                    is VehicleEntity -> if (entity.hasContainer()) entity.inventory else null
                    is Player -> InvWrapper(entity.inventory)
                    is Container -> InvWrapper(entity)
                    else -> null
                }
            }
    }
}

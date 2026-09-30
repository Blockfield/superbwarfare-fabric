package com.atsuishio.superbwarfare.fabric

/**
 * Собственная шина событий мода вместо NeoForge.EVENT_BUS.
 *
 * У SBW девять своих событий в `api/event`, и все они рассылались через один хелпер postEvent.
 * Fabric своей шины не даёт, а заводить по Event<Callback> на каждое событие значило бы менять
 * их публичную форму. Здесь сохранена семантика NeoForge: подписка по классу события,
 * рассылка возвращает то же событие, отменяемые события несут флаг.
 *
 * Приоритет -- замена EventPriority: больше число -- раньше вызов, при равном -- порядок
 * регистрации. 0 соответствует NORMAL.
 */
object ModEventBus {
    private val listeners = mutableMapOf<Class<*>, MutableList<Pair<Int, (Any) -> Unit>>>()

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> register(
        type: Class<T>,
        priority: Int = 0,
        listener: (T) -> Unit,
    ) {
        val list = listeners.getOrPut(type) { mutableListOf() }
        val index = list.indexOfFirst { it.first < priority }.takeIf { it >= 0 } ?: list.size
        list.add(index, priority to (listener as (Any) -> Unit))
    }

    inline fun <reified T : Any> register(
        priority: Int = 0,
        noinline listener: (T) -> Unit,
    ) = register(T::class.java, priority, listener)

    fun <T : Any> post(event: T): T {
        var type: Class<*>? = event::class.java
        while (type != null && type != Any::class.java) {
            listeners[type]?.forEach { it.second(event) }
            type = type.superclass
        }
        return event
    }
}

/**
 * Замена ICancellableEvent из NeoForge с той же формой обращения.
 *
 * Именно класс, а не интерфейс: интерфейс в Kotlin не несёт состояния, а наследники
 * (PreKillEvent, LoadingDataEvent, LoadingJsonEvent, ProjectileHitEvent, RenderPlayerArmEvent)
 * флаг отмены не объявляют — у NeoForge он приходил из базового Event.
 */
open class CancellableEvent {
    var canceled: Boolean = false

    // Только isCanceled(): setCanceled(boolean) для Java уже генерирует сам сеттер свойства,
    // а явный метод дал бы вторую функцию с той же JVM-сигнатурой.
    fun isCanceled(): Boolean = canceled
}

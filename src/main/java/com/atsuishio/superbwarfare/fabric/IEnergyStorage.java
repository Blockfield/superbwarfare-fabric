package com.atsuishio.superbwarfare.fabric;

/**
 * Замена net.neoforged.neoforge.energy.IEnergyStorage.
 *
 * <p>Объявлено на Java, а не на Kotlin, намеренно: Kotlin превращает в синтетические свойства
 * только Java-геттеры. Из Kotlin-интерфейса с fun getEnergyStored() обращения вида cap.energyStored
 * (а их в моде ~70) перестали бы компилироваться.
 *
 * <p>ponytail: своя энергосистема, не Fabric Transfer API и не teamreborn:energy. Энергию мода
 * видят только блоки, предметы и техника самого мода, межмодового обмена нет -- у апстрима его тоже
 * не было. Если понадобится отдавать энергию наружу, приделывать мост к
 * team_reborn.energy.EnergyStorage поверх этого интерфейса, ничего не переписывая.
 */
public interface IEnergyStorage {
    int receiveEnergy(int toReceive, boolean simulate);

    int extractEnergy(int toExtract, boolean simulate);

    int getEnergyStored();

    int getMaxEnergyStored();

    boolean canExtract();

    boolean canReceive();
}

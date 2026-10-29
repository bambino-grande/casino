package ru.pogosian.kazik.games.business.slots

import ru.pogosian.kazik.games.business.exceptions.SlotMachineNotFoundException

class SlotMachineCatalog(machines: List<SlotMachine>) {
    private val machinesByCode = machines.associateBy { it.code }

    init {
        require(machines.isNotEmpty()) { "at least one slot machine must be configured" }
        require(machinesByCode.size == machines.size) { "slot machine codes must be unique" }
    }

    fun all(): List<SlotMachine> = machinesByCode.values.toList()

    fun get(code: String): SlotMachine = machinesByCode[code] ?: throw SlotMachineNotFoundException(code)
}

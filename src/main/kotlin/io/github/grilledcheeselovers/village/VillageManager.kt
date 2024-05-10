package io.github.grilledcheeselovers.village

import java.util.Collections

class VillageManager(
    private val villages: MutableMap<String, Village> = hashMapOf()
) {

    fun getVillage(id: String) : Village? {
        return this.villages[id]
    }

    fun addVillage(village: Village) {
        this.villages[village.id] = village
    }

    fun getVillages(): Map<String, Village> {
        return Collections.unmodifiableMap(this.villages)
    }

    fun clearVillages() {
        this.villages.clear()
    }

}
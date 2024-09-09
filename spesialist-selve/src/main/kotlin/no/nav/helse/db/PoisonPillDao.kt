package no.nav.helse.db

import no.nav.helse.HelseDao
import javax.sql.DataSource

internal class PoisonPillDao(dataSource: DataSource) : HelseDao(dataSource) {
    internal fun poisonPills(): Map<String, Set<String>> {
        return asSQL("SELECT identifikator, feltnavn FROM poison_pill")
            .list {
                it.string("feltnavn") to it.string("identifikator")
            }
            .groupBy({ it.first }, { it.second })
            .mapValues { (_, values) -> values.toSet() }
    }
}

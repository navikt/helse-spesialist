package no.nav.helse.db

import no.nav.helse.HelseDao
import no.nav.helse.mediator.meldinger.PoisonPills
import javax.sql.DataSource

internal class PoisonPillDao(dataSource: DataSource) : HelseDao(dataSource) {
    internal fun poisonPills(): PoisonPills {
        val lagredeVerdier =
            asSQL("SELECT identifikator, feltnavn FROM poison_pill")
                .list {
                    it.string("feltnavn") to it.string("identifikator")
                }
                .groupBy({ it.first }, { it.second })
                .mapValues { (_, values) -> values.toSet() }
        return PoisonPills(lagredeVerdier)
    }
}

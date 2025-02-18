package no.nav.helse.spesialist.db.dao

import no.nav.helse.db.HelseDao
import no.nav.helse.db.PoisonPillDao
import no.nav.helse.mediator.meldinger.PoisonPills
import javax.sql.DataSource

class PgPoisonPillDao internal constructor(dataSource: DataSource) : HelseDao(dataSource), PoisonPillDao {
    override fun poisonPills(): PoisonPills {
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

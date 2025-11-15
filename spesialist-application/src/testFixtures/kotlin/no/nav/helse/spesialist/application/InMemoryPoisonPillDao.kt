package no.nav.helse.spesialist.application

import no.nav.helse.db.PoisonPillDao
import no.nav.helse.mediator.meldinger.PoisonPills

class InMemoryPoisonPillDao : PoisonPillDao {
    override fun poisonPills() = PoisonPills(emptyMap())
}

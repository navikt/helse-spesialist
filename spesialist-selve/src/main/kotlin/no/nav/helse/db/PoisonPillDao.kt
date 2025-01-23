package no.nav.helse.db

import no.nav.helse.mediator.meldinger.PoisonPills

interface PoisonPillDao {
    fun poisonPills(): PoisonPills
}

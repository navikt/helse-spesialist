package no.nav.helse.db

import no.nav.helse.spesialist.application.OpptegnelseListener

interface ListenerFactory {
    fun opptegnelseListener(): OpptegnelseListener
}

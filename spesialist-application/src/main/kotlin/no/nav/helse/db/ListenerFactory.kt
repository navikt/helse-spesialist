package no.nav.helse.db

import no.nav.helse.spesialist.application.OpptegnelseListener

interface ListenerFactory {
    suspend fun opptegnelseListener(block: suspend OpptegnelseListener.() -> Unit)
}

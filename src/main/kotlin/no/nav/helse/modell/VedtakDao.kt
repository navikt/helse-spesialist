package no.nav.helse.modell

import kotliquery.Session
import no.nav.helse.modell.vedtak.findVedtak
import java.util.*

internal class VedtakDao(private val session: Session) {
    fun findVedtak(id: UUID) = session.findVedtak(id)
}

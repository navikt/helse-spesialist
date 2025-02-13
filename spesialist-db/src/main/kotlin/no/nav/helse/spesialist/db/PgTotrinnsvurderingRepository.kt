package no.nav.helse.spesialist.db

import kotliquery.Session
import no.nav.helse.db.PgSaksbehandlerDao
import no.nav.helse.db.PgTotrinnsvurderingDao
import no.nav.helse.db.TotrinnsvurderingFraDatabase
import no.nav.helse.modell.saksbehandler.Tilgangskontroll
import no.nav.helse.modell.saksbehandler.handlinger.Overstyring
import no.nav.helse.modell.totrinnsvurdering.Totrinnsvurdering
import no.nav.helse.spesialist.application.TotrinnsvurderingRepository
import java.util.UUID

class PgTotrinnsvurderingRepository(
    session: Session,
    tilgangskontroll: Tilgangskontroll,
) : TotrinnsvurderingRepository {
    private val overstyringRepository = PgOverstyringRepository(session)
    private val totrinnsvurderingDao = PgTotrinnsvurderingDao(session)
    private val saksbehandlerDao = PgSaksbehandlerDao(session, tilgangskontroll)

    override fun finn(fødselsnummer: String): Totrinnsvurdering? {
        val (id, totrinnsvurderingFraDatabase) =
            totrinnsvurderingDao.hentAktivTotrinnsvurdering(fødselsnummer)
                ?: return null

        val overstyringer = overstyringRepository.finn(fødselsnummer)

        return totrinnsvurderingFraDatabase.tilDomene(id, overstyringer)
    }

    @Deprecated("Skal fjernes, midlertidig i bruk for å tette et hull")
    override fun finn(vedtaksperiodeId: UUID): Totrinnsvurdering? {
        val (id, totrinnsvurderingFraDatabase) =
            totrinnsvurderingDao.hentAktivTotrinnsvurdering(vedtaksperiodeId)
                ?: return null

        return totrinnsvurderingFraDatabase.tilDomene(id, emptyList())
    }

    override fun lagre(
        totrinnsvurdering: Totrinnsvurdering,
        fødselsnummer: String,
    ) {
        val totrinnsvurderingFraDatabase = totrinnsvurdering.tilDatabase()
        if (totrinnsvurdering.harFåttTildeltId()) {
            totrinnsvurderingDao.update(totrinnsvurdering.id(), totrinnsvurderingFraDatabase)
        } else {
            totrinnsvurderingDao.insert(totrinnsvurderingFraDatabase).also { totrinnsvurdering.tildelId(it) }
        }

        overstyringRepository.lagre(totrinnsvurdering.overstyringer)
    }

    private fun TotrinnsvurderingFraDatabase.tilDomene(
        id: Long,
        overstyringer: List<Overstyring>,
    ): Totrinnsvurdering {
        return Totrinnsvurdering.Companion.fraLagring(
            id = id,
            vedtaksperiodeId = this.vedtaksperiodeId,
            erRetur = this.erRetur,
            saksbehandler =
                this.saksbehandler?.let {
                    saksbehandlerDao.finnSaksbehandler(
                        it,
                    )
                },
            beslutter =
                this.beslutter?.let {
                    saksbehandlerDao.finnSaksbehandler(
                        it,
                    )
                },
            utbetalingId = this.utbetalingId,
            opprettet = this.opprettet,
            oppdatert = this.oppdatert,
            ferdigstilt = false,
            overstyringer = overstyringer,
        )
    }

    private fun Totrinnsvurdering.tilDatabase() =
        TotrinnsvurderingFraDatabase(
            vedtaksperiodeId = vedtaksperiodeId,
            erRetur = erRetur,
            saksbehandler = saksbehandler?.oid,
            beslutter = beslutter?.oid,
            utbetalingId = utbetalingId,
            opprettet = opprettet,
            oppdatert = oppdatert,
        )
}

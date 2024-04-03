package no.nav.helse.modell.saksbehandler.handlinger

import no.nav.helse.modell.saksbehandler.AnnullertUtbetalingEvent
import no.nav.helse.modell.saksbehandler.Saksbehandler
import java.util.UUID

class Annullering(
    private val aktørId: String,
    private val fødselsnummer: String,
    private val organisasjonsnummer: String,
    private val fagsystemId: String?,
    private val utbetalingId: String?,
    private val begrunnelser: List<String> = emptyList(),
    private val kommentar: String?,
) : Handling {
    override fun utførAv(saksbehandler: Saksbehandler) {
        saksbehandler.håndter(this)
    }

    override fun loggnavn(): String = "annuller_utbetaling"

    internal fun byggEvent(
        oid: UUID,
        navn: String,
        epost: String,
        ident: String,
    ): AnnullertUtbetalingEvent {
        return AnnullertUtbetalingEvent(
            fødselsnummer = fødselsnummer,
            aktørId = aktørId,
            organisasjonsnummer = organisasjonsnummer,
            saksbehandlerOid = oid,
            saksbehandlerNavn = navn,
            saksbehandlerIdent = ident,
            saksbehandlerEpost = epost,
            fagsystemId = fagsystemId,
            utbetalingId = utbetalingId,
            begrunnelser = begrunnelser,
            kommentar = kommentar,
        )
    }
}

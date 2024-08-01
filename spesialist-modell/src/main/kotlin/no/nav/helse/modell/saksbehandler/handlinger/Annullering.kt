package no.nav.helse.modell.saksbehandler.handlinger

import no.nav.helse.modell.saksbehandler.AnnullertUtbetalingEvent
import no.nav.helse.modell.saksbehandler.Saksbehandler
import java.util.UUID

class Annullering(
    private val aktørId: String,
    private val fødselsnummer: String,
    private val organisasjonsnummer: String,
    private val vedtaksperiodeId: UUID,
    private val utbetalingId: UUID,
    private val årsaker: List<String> = emptyList(),
    private val begrunnelse: String?,
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
            vedtaksperiodeId = vedtaksperiodeId,
            utbetalingId = utbetalingId,
            begrunnelser = årsaker,
            kommentar = begrunnelse,
        )
    }

    fun toDto() =
        AnnulleringDto(
            aktørId = aktørId,
            fødselsnummer = fødselsnummer,
            organisasjonsnummer = organisasjonsnummer,
            vedtaksperiodeId = vedtaksperiodeId,
            utbetalingId = utbetalingId,
            årsaker = årsaker,
            begrunnelse = begrunnelse,
        )
}

data class AnnulleringDto(
    val aktørId: String,
    val fødselsnummer: String,
    val organisasjonsnummer: String,
    val vedtaksperiodeId: UUID,
    val utbetalingId: UUID,
    val årsaker: List<String> = emptyList(),
    val begrunnelse: String?,
)

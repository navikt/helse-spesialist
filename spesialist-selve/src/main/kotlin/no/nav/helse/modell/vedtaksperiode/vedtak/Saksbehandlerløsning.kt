package no.nav.helse.modell.vedtaksperiode.vedtak

import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.mediator.meldinger.Personhendelse

/**
 * Behandler input til godkjenningsbehov fra saksbehandler som har blitt lagt på rapid-en av API-biten av spesialist.
 */
internal class Saksbehandlerløsning(
    override val id: UUID,
    val behandlingId: UUID,
    private val fødselsnummer: String,
    private val json: String,
    val godkjent: Boolean,
    val saksbehandlerIdent: String,
    val epostadresse: String,
    val godkjenttidspunkt: LocalDateTime,
    val årsak: String?,
    val begrunnelser: List<String>?,
    val kommentar: String?,
    val saksbehandleroverstyringer: List<UUID>,
    val oppgaveId: Long,
    val godkjenningsbehovhendelseId: UUID,
) : Personhendelse {
    override fun fødselsnummer() = fødselsnummer
    override fun toJson() = json
}

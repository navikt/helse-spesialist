package no.nav.helse.modell.hendelse

import java.util.UUID

sealed interface UtgåendeHendelse {
    data class VedtaksperiodeGodkjentManuelt(
        val fødselsnummer: String,
        val vedtaksperiodeId: UUID,
        val behandlingId: UUID,
        val periodetype: String,
        val saksbehandlerIdent: String,
        val saksbehandlerEpost: String,
        val beslutterIdent: String?,
        val beslutterEpost: String?,
    ) : UtgåendeHendelse

    data class VedtaksperiodeGodkjentAutomatisk(
        val fødselsnummer: String,
        val vedtaksperiodeId: UUID,
        val behandlingId: UUID,
        val periodetype: String,
    ) : UtgåendeHendelse

    data class VedtaksperiodeAvvistManuelt(
        val fødselsnummer: String,
        val vedtaksperiodeId: UUID,
        val behandlingId: UUID,
        val periodetype: String,
        val saksbehandlerIdent: String,
        val saksbehandlerEpost: String,
        val årsak: String?,
        val begrunnelser: List<String>?,
        val kommentar: String?,
    ) : UtgåendeHendelse

    data class VedtaksperiodeAvvistAutomatisk(
        val fødselsnummer: String,
        val vedtaksperiodeId: UUID,
        val behandlingId: UUID,
        val periodetype: String,
        val årsak: String?,
        val begrunnelser: List<String>?,
        val kommentar: String?,
    ) : UtgåendeHendelse
}

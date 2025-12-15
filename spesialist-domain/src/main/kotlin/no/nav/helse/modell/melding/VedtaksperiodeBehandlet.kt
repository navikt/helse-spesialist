package no.nav.helse.modell.melding

import no.nav.helse.modell.vedtaksperiode.Yrkesaktivitetstype
import no.nav.helse.spesialist.domain.NAVIdent
import java.util.UUID

data class VedtaksperiodeAvvistAutomatisk(
    val fødselsnummer: String,
    val vedtaksperiodeId: UUID,
    val behandlingId: UUID,
    val yrkesaktivitetstype: Yrkesaktivitetstype,
    val periodetype: String,
    val årsak: String?,
    val begrunnelser: List<String>?,
) : UtgåendeHendelse

data class VedtaksperiodeAvvistManuelt(
    val fødselsnummer: String,
    val vedtaksperiodeId: UUID,
    val behandlingId: UUID,
    val yrkesaktivitetstype: Yrkesaktivitetstype,
    val periodetype: String,
    val saksbehandlerIdent: NAVIdent,
    val saksbehandlerEpost: String,
    val årsak: String?,
    val begrunnelser: List<String>?,
    val kommentar: String?,
) : UtgåendeHendelse

data class VedtaksperiodeGodkjentAutomatisk(
    val fødselsnummer: String,
    val vedtaksperiodeId: UUID,
    val behandlingId: UUID,
    val yrkesaktivitetstype: Yrkesaktivitetstype,
    val periodetype: String,
) : UtgåendeHendelse

data class VedtaksperiodeGodkjentManuelt(
    val fødselsnummer: String,
    val vedtaksperiodeId: UUID,
    val behandlingId: UUID,
    val yrkesaktivitetstype: Yrkesaktivitetstype,
    val periodetype: String,
    val saksbehandlerIdent: NAVIdent,
    val saksbehandlerEpost: String,
    val beslutterIdent: NAVIdent?,
    val beslutterEpost: String?,
) : UtgåendeHendelse

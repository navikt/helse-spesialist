package no.nav.helse.spesialist.api.saksbehandler.handlinger

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.time.LocalDate
import java.util.UUID

interface HandlingFraApi

data class OpphevStans(val fødselsnummer: String, val begrunnelse: String) : HandlingFraApi

data class TildelOppgave(val oppgaveId: Long) : HandlingFraApi

data class AvmeldOppgave(val oppgaveId: Long) : HandlingFraApi

@JsonIgnoreProperties
data class AnnulleringHandlingFraApi(
    val aktørId: String,
    val fødselsnummer: String,
    val organisasjonsnummer: String,
    val vedtaksperiodeId: UUID,
    val utbetalingId: UUID,
    val begrunnelser: List<String> = emptyList(),
    val kommentar: String?,
) : HandlingFraApi

data class LeggPåVent(
    val oppgaveId: Long,
    val saksbehandlerOid: UUID,
    val frist: LocalDate,
    val skalTildeles: Boolean,
    val begrunnelse: String?,
) : HandlingFraApi

data class FjernPåVent(
    val oppgaveId: Long,
) : HandlingFraApi

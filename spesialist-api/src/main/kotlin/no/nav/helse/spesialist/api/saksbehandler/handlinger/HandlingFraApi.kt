package no.nav.helse.spesialist.api.saksbehandler.handlinger

import java.time.LocalDate
import java.util.UUID

interface HandlingFraApi

data class OpphevStans(val fødselsnummer: String, val begrunnelse: String) : HandlingFraApi

data class TildelOppgave(val oppgaveId: Long) : HandlingFraApi

data class AvmeldOppgave(val oppgaveId: Long) : HandlingFraApi

interface PåVent : HandlingFraApi

data class LeggPåVent(
    val oppgaveId: Long,
    val saksbehandlerOid: UUID,
    val frist: LocalDate,
    val skalTildeles: Boolean,
    val begrunnelse: String?,
    val notatTekst: String,
) : PåVent

data class FjernPåVent(
    val oppgaveId: Long,
) : PåVent

data class FjernPåVentUtenHistorikkinnslag(
    val oppgaveId: Long,
) : PåVent

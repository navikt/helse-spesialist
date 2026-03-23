package no.nav.helse.spesialist.api.saksbehandler.handlinger

interface HandlingFraApi

data class TildelOppgave(
    val oppgaveId: Long,
) : HandlingFraApi

data class AvmeldOppgave(
    val oppgaveId: Long,
) : HandlingFraApi

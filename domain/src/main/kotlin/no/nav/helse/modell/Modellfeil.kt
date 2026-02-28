package no.nav.helse.modell

import java.util.UUID

sealed class Modellfeil : RuntimeException()

class OppgaveIkkeTildelt(
    val oppgaveId: Long,
) : Modellfeil()

class OppgaveTildeltNoenAndre(
    val saksbehandlerOid: UUID,
    val påVent: Boolean,
) : Modellfeil()

class OppgaveAlleredeSendtBeslutter(
    val oppgaveId: Long,
) : Modellfeil()

class OppgaveAlleredeSendtIRetur(
    val oppgaveId: Long,
) : Modellfeil()

class OppgaveKreverVurderingAvToSaksbehandlere(
    val oppgaveId: Long,
) : Modellfeil()

class ManglerTilgang(
    val oid: UUID,
    val oppgaveId: Long,
) : Modellfeil()

class FinnerIkkePåVent(
    val oppgaveId: Long,
) : Modellfeil()

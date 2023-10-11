package no.nav.helse.modell

import no.nav.helse.modell.saksbehandler.Saksbehandler

sealed class Modellfeil: RuntimeException()

class OppgaveIkkeTildelt(val oppgaveId: Long): Modellfeil()

class OppgaveTildeltNoenAndre(val saksbehandler: Saksbehandler, val påVent: Boolean): Modellfeil()

class OppgaveAlleredeSendtBeslutter(val oppgaveId: Long): Modellfeil()

class OppgaveAlleredeSendtIRetur(val oppgaveId: Long): Modellfeil()

class OppgaveKreverVurderingAvToSaksbehandlere(val oppgaveId: Long): Modellfeil()
package no.nav.helse.modell

sealed class Modellfeil: RuntimeException()

class OppgaveIkkeTildelt(val oppgaveId: Long): Modellfeil()

class OppgaveTildeltNoenAndre: Modellfeil()

class OppgaveAlleredeSendtBeslutter(val oppgaveId: Long): Modellfeil()

class OppgaveAlleredeSendtIRetur(val oppgaveId: Long): Modellfeil()

class OppgaveKreverVurderingAvToSaksbehandlere(val oppgaveId: Long): Modellfeil()
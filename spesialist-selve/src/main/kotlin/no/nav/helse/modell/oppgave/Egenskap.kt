package no.nav.helse.modell.oppgave

import java.util.UUID
import no.nav.helse.modell.saksbehandler.Saksbehandler

sealed interface Egenskap

sealed class TilgangsstyrtEgenskap(private val tilgangsgruppe: UUID): Egenskap {
    fun kanBehandlesAv(saksbehandler: Saksbehandler): Boolean {
        TODO()
    }
}

data object RISK_QA: TilgangsstyrtEgenskap(UUID.randomUUID())

data object FORTROLIG_ADRESSE: TilgangsstyrtEgenskap(UUID.randomUUID())

data object EGEN_ANSATT: TilgangsstyrtEgenskap(UUID.randomUUID())

data object REVURDERING: Egenskap

data object SØKNAD: Egenskap

data object STIKKPRØVE: Egenskap

data object UTBETALING_TIL_SYKMELDT: Egenskap

data object DELVIS_REFUSJON: Egenskap

data object UTBETALING_TIL_ARBEIDSGIVER: Egenskap

data object INGEN_UTBETALING: Egenskap
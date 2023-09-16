package no.nav.helse.mediator.saksbehandler

import java.util.UUID
import no.nav.helse.Tilgangsgrupper
import no.nav.helse.modell.oppgave.EGEN_ANSATT
import no.nav.helse.modell.oppgave.FORTROLIG_ADRESSE
import no.nav.helse.modell.oppgave.RISK_QA
import no.nav.helse.modell.oppgave.TilgangsstyrtEgenskap

internal interface TilgangskontrollHandler {
    suspend fun harTilgangTil(egenskap: TilgangsstyrtEgenskap, oid: UUID): Boolean
}

internal class Tilgangskontrollør(
    private val tilgangsgrupper: Tilgangsgrupper,
    private val msGraphClient: IMsGraphClient
): TilgangskontrollHandler {
    override suspend fun harTilgangTil(egenskap: TilgangsstyrtEgenskap, oid: UUID): Boolean {
        return when (egenskap) {
            EGEN_ANSATT -> msGraphClient.erIGruppe(oid, tilgangsgrupper.skjermedePersonerGruppeId)
            FORTROLIG_ADRESSE -> msGraphClient.erIGruppe(oid, tilgangsgrupper.kode7GruppeId)
            RISK_QA -> msGraphClient.erIGruppe(oid, tilgangsgrupper.riskQaGruppeId)
        }
    }
}

internal interface IMsGraphClient {
    suspend fun erIGruppe(oid: UUID, gruppeId: UUID): Boolean
}
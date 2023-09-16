package no.nav.helse.mediator.saksbehandler

import java.util.UUID
import kotlinx.coroutines.runBlocking
import no.nav.helse.Tilgangsgrupper
import no.nav.helse.modell.oppgave.EGEN_ANSATT
import no.nav.helse.modell.oppgave.FORTROLIG_ADRESSE
import no.nav.helse.modell.oppgave.RISK_QA
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TilgangskontrollørTest {

    private companion object {
        private val saksbehandlerOidMedKode7Tilgang = UUID.randomUUID()
        private val saksbehandlerOidMedEgenAnsattTilgang = UUID.randomUUID()
        private val saksbehandlerOidMedRiskQaTilgang = UUID.randomUUID()
        private val kode7GruppeId = UUID.randomUUID()
        private val riskQaGruppeId = UUID.randomUUID()
        private val beslutterGruppeId = UUID.randomUUID()
        private val egenAnsattGruppeId = UUID.randomUUID()
    }

    @Test
    fun `tilgang til skjermede`() {
        val tilgangskontrollør = Tilgangskontrollør(tilgangsgrupper, msGraphClient)
        runBlocking {
            assertEquals(true, tilgangskontrollør.harTilgangTil(FORTROLIG_ADRESSE, saksbehandlerOidMedKode7Tilgang))
        }
    }

    @Test
    fun `tilgang til egen ansatt`() {
        val tilgangskontrollør = Tilgangskontrollør(tilgangsgrupper, msGraphClient)
        runBlocking {
            assertEquals(true, tilgangskontrollør.harTilgangTil(EGEN_ANSATT, saksbehandlerOidMedEgenAnsattTilgang))
        }
    }

    @Test
    fun `tilgang til risk QA`() {
        val tilgangskontrollør = Tilgangskontrollør(tilgangsgrupper, msGraphClient)
        runBlocking {
            assertEquals(true, tilgangskontrollør.harTilgangTil(RISK_QA, saksbehandlerOidMedRiskQaTilgang))
        }
    }

    @Test
    fun `ikke tilgang`() {
        val tilgangskontrollør = Tilgangskontrollør(tilgangsgrupper, msGraphClient)
        runBlocking {
            assertEquals(false, tilgangskontrollør.harTilgangTil(FORTROLIG_ADRESSE, UUID.randomUUID()))
            assertEquals(false, tilgangskontrollør.harTilgangTil(EGEN_ANSATT, UUID.randomUUID()))
            assertEquals(false, tilgangskontrollør.harTilgangTil(RISK_QA, UUID.randomUUID()))
        }
    }

    private val tilgangsgrupper = Tilgangsgrupper(
        mapOf(
            "RISK_SUPERSAKSBEHANDLER_GROUP" to "$riskQaGruppeId",
            "KODE7_SAKSBEHANDLER_GROUP" to "$kode7GruppeId",
            "BESLUTTER_SAKSBEHANDLER_GROUP" to "$beslutterGruppeId",
            "SKJERMEDE_PERSONER_GROUP" to "$egenAnsattGruppeId",
        )
    )

    private val msGraphClient = object : IMsGraphClient {
        val saksbehandlertilganger: Map<UUID, List<UUID>> = mapOf(
            saksbehandlerOidMedKode7Tilgang to listOf(kode7GruppeId),
            saksbehandlerOidMedEgenAnsattTilgang to listOf(egenAnsattGruppeId),
            saksbehandlerOidMedRiskQaTilgang to listOf(riskQaGruppeId)
        )

        override suspend fun erIGruppe(oid: UUID, gruppeId: UUID): Boolean {
            return saksbehandlertilganger[oid]?.contains(gruppeId) == true
        }
    }
}
package no.nav.helse.spesialist.api.graphql.mutation

import no.nav.helse.spesialist.api.AbstractGraphQLApiTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID

internal class VarselMutationHandlerTest : AbstractGraphQLApiTest() {

    @Test
    fun `sett status VURDERT`() {
        val definisjonId = UUID.randomUUID()
        opprettSaksbehandler()
        opprettPerson()
        opprettArbeidsgiver()
        val spleisBehandlingId = UUID.randomUUID()
        opprettVedtaksperiode(spleisBehandlingId = spleisBehandlingId)
        val behandlingUnikId = finnBehandlingUnikId(spleisBehandlingId)
        opprettVarseldefinisjon(definisjonId = definisjonId)
        nyttVarsel(kode = "EN_KODE", status = "AKTIV")

        val body = runQuery(
            """
            mutation SettVarselstatus {
                settVarselstatus(
                    generasjonIdString: "$behandlingUnikId", 
                    definisjonIdString: "$definisjonId",
                    varselkode: "EN_KODE", 
                    ident: "${SAKSBEHANDLER.oid}" 
                ) {
                    kode
                    vurdering {
                        status
                    }
                }
            }
        """
        )

        assertEquals("VURDERT", body["data"]["settVarselstatus"]["vurdering"]["status"].asText())
    }

    @Test
    fun `sett status AKTIV`() {
        val definisjonId = UUID.randomUUID()
        opprettSaksbehandler()
        opprettPerson()
        opprettArbeidsgiver()
        val spleisBehandlingId = UUID.randomUUID()
        opprettVedtaksperiode(spleisBehandlingId = spleisBehandlingId)
        val behandlingUnikId = finnBehandlingUnikId(spleisBehandlingId)
        opprettVarseldefinisjon(definisjonId = definisjonId)
        nyttVarsel(kode = "EN_KODE", status = "AKTIV")

        val body = runQuery(
            """
            mutation SettVarselstatus {
                settVarselstatus(
                    generasjonIdString: "$behandlingUnikId", 
                    varselkode: "EN_KODE", 
                    ident: "${SAKSBEHANDLER.oid}" 
                ) {
                    kode
                    vurdering {
                        status
                    }
                }
            }
        """
        )

        assertTrue(body["data"]["settVarselstatus"]["vurdering"].isNull)
    }

    @Test
    fun `får 409-feil hvis status på varselet er noe annet enn AKTIV når man vurderer varsel`() {
        val definisjonId = UUID.randomUUID()
        opprettSaksbehandler()
        opprettPerson()
        opprettArbeidsgiver()
        val spleisBehandlingId = UUID.randomUUID()
        opprettVedtaksperiode(spleisBehandlingId = spleisBehandlingId)
        val behandlingUnikId = finnBehandlingUnikId(spleisBehandlingId)
        opprettVarseldefinisjon(definisjonId = definisjonId)
        nyttVarsel(kode = "EN_KODE", status = "GODKJENT")

        val body = runQuery(
            """
            mutation SettVarselstatus {
                settVarselstatus(
                    generasjonIdString: "$behandlingUnikId", 
                    definisjonIdString: "$definisjonId",
                    varselkode: "EN_KODE", 
                    ident: "${SAKSBEHANDLER.oid}" 
                ) {
                    kode
                    vurdering {
                        status
                    }
                }
            }
        """
        )

        assertEquals(409, body["errors"].first()["extensions"]["code"].asInt())
    }

    @Test
    fun `får 409-feil hvis status på varselet er noe annet enn GODKJENT når gjør varsel aktivt`() {
        opprettSaksbehandler()
        opprettPerson()
        opprettArbeidsgiver()
        val spleisBehandlingId = UUID.randomUUID()
        opprettVedtaksperiode(spleisBehandlingId = spleisBehandlingId)
        opprettVarseldefinisjon(definisjonId = UUID.randomUUID())
        nyttVarsel(kode = "EN_KODE", status = "GODKJENT")
        val behandlingUnikId = finnBehandlingUnikId(spleisBehandlingId)
        val body = runQuery(
            """
            mutation SettVarselstatus {
                settVarselstatus(
                    generasjonIdString: "$behandlingUnikId", 
                    varselkode: "EN_KODE", 
                    ident: "${SAKSBEHANDLER.oid}" 
                ) {
                    kode
                    vurdering {
                        status
                    }
                }
            }
        """
        )

        assertEquals(409, body["errors"].first()["extensions"]["code"].asInt())
    }

    @Test
    fun `får 500-feil dersom oppdateringen tryner for settVarselstatus`() {
        opprettSaksbehandler()
        opprettPerson()
        opprettArbeidsgiver()
        val spleisBehandlingId = UUID.randomUUID()
        opprettVedtaksperiode(spleisBehandlingId = spleisBehandlingId)
        val behandlingUnikId = finnBehandlingUnikId(spleisBehandlingId)

        val kode = "DENNE_KODEN_FINNES_FORHÅPENTLIGVIS_IKKE_I_DATABASEN"
        nyttVarsel(kode = kode, status = "AKTIV")

        val body = runQuery(
            """
            mutation SettVarselstatus {
                settVarselstatus(
                    generasjonIdString: "$behandlingUnikId", 
                    varselkode: "$kode", 
                    ident: "${SAKSBEHANDLER.oid}" 
                ) {
                    kode
                    vurdering {
                        status
                    }
                }
            }
        """
        )

        assertEquals(500, body["errors"].first()["extensions"]["code"].asInt())
    }

    @Test
    fun `får 404-feil hvis varselet ikke finnes for settVarselstatus`() {
        opprettSaksbehandler()
        opprettPerson()
        opprettArbeidsgiver()
        val spleisBehandlingId = UUID.randomUUID()
        opprettVedtaksperiode(spleisBehandlingId = spleisBehandlingId)
        val behandlingUnikId = finnBehandlingUnikId(spleisBehandlingId)

        val body = runQuery(
            """
            mutation SettVarselstatus {
                settVarselstatus(
                    generasjonIdString: "$behandlingUnikId", 
                    varselkode: "EN_KODE", 
                    ident: "${SAKSBEHANDLER.oid}" 
                ) {
                    kode
                    vurdering {
                        status
                    }
                }
            }
        """
        )

        assertEquals(404, body["errors"].first()["extensions"]["code"].asInt())
    }
}

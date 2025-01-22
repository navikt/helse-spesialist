package no.nav.helse.spesialist.api.graphql.mutation

import no.nav.helse.spesialist.api.AbstractGraphQLApiTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID

internal class VarselMutationTest : AbstractGraphQLApiTest() {

    @Test
    fun `sett status VURDERT`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val generasjonId = UUID.randomUUID()
        val definisjonId = UUID.randomUUID()
        opprettSaksbehandler()
        opprettVedtaksperiode(opprettPerson(), opprettArbeidsgiver())
        opprettVarseldefinisjon(definisjonId = definisjonId)
        val generasjonRef = nyGenerasjon(generasjonId = generasjonId, vedtaksperiodeId = vedtaksperiodeId)
        nyttVarsel(kode = "EN_KODE", vedtaksperiodeId = vedtaksperiodeId, generasjonRef = generasjonRef)

        val body = runQuery(
            """
            mutation SettVarselstatus {
                settVarselstatus(
                    generasjonIdString: "$generasjonId", 
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
        val vedtaksperiodeId = UUID.randomUUID()
        val generasjonId = UUID.randomUUID()
        val definisjonId = UUID.randomUUID()
        opprettSaksbehandler()
        opprettVedtaksperiode(opprettPerson(), opprettArbeidsgiver())
        opprettVarseldefinisjon(definisjonId = definisjonId)
        val generasjonRef = nyGenerasjon(generasjonId = generasjonId, vedtaksperiodeId = vedtaksperiodeId)
        nyttVarsel(kode = "EN_KODE", vedtaksperiodeId = vedtaksperiodeId, generasjonRef = generasjonRef)

        val body = runQuery(
            """
            mutation SettVarselstatus {
                settVarselstatus(
                    generasjonIdString: "$generasjonId", 
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
        val vedtaksperiodeId = UUID.randomUUID()
        val generasjonId = UUID.randomUUID()
        val definisjonId = UUID.randomUUID()
        opprettSaksbehandler()
        opprettVedtaksperiode(opprettPerson(), opprettArbeidsgiver())
        opprettVarseldefinisjon(definisjonId = definisjonId)
        val generasjonRef = nyGenerasjon(generasjonId = generasjonId, vedtaksperiodeId = vedtaksperiodeId)
        nyttVarsel(kode = "EN_KODE", vedtaksperiodeId = vedtaksperiodeId, generasjonRef = generasjonRef, status = "GODKJENT")

        val body = runQuery(
            """
            mutation SettVarselstatus {
                settVarselstatus(
                    generasjonIdString: "$generasjonId", 
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
        val vedtaksperiodeId = UUID.randomUUID()
        val generasjonId = UUID.randomUUID()
        val definisjonId = UUID.randomUUID()
        opprettSaksbehandler()
        opprettVedtaksperiode(opprettPerson(), opprettArbeidsgiver())
        opprettVarseldefinisjon(definisjonId = definisjonId)
        val generasjonRef = nyGenerasjon(generasjonId = generasjonId, vedtaksperiodeId = vedtaksperiodeId)
        nyttVarsel(kode = "EN_KODE", vedtaksperiodeId = vedtaksperiodeId, generasjonRef = generasjonRef, status = "GODKJENT")

        val body = runQuery(
            """
            mutation SettVarselstatus {
                settVarselstatus(
                    generasjonIdString: "$generasjonId", 
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
        val vedtaksperiodeId = UUID.randomUUID()
        val generasjonId = UUID.randomUUID()
        opprettSaksbehandler()
        opprettVedtaksperiode(opprettPerson(), opprettArbeidsgiver())
        val generasjonRef = nyGenerasjon(generasjonId = generasjonId, vedtaksperiodeId = vedtaksperiodeId)
        val kode = "DENNE_KODEN_FINNES_FORHÅPENTLIGVIS_IKKE_I_DATABASEN"
        nyttVarsel(kode = kode, vedtaksperiodeId = vedtaksperiodeId, generasjonRef = generasjonRef)

        val body = runQuery(
            """
            mutation SettVarselstatus {
                settVarselstatus(
                    generasjonIdString: "$generasjonId", 
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
        val vedtaksperiodeId = UUID.randomUUID()
        val generasjonId = UUID.randomUUID()
        opprettSaksbehandler()
        opprettVedtaksperiode(opprettPerson(), opprettArbeidsgiver())
        nyGenerasjon(generasjonId = generasjonId, vedtaksperiodeId = vedtaksperiodeId)

        val body = runQuery(
            """
            mutation SettVarselstatus {
                settVarselstatus(
                    generasjonIdString: "$generasjonId", 
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

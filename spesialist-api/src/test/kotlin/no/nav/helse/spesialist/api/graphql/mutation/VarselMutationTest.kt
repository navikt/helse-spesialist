package no.nav.helse.spesialist.api.graphql.mutation

import java.util.UUID
import no.nav.helse.spesialist.api.AbstractGraphQLApiTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

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
            mutation SettVarselstatusVurdert {
                settVarselstatusVurdert(
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

        assertEquals("VURDERT", body["data"]["settVarselstatusVurdert"]["vurdering"]["status"].asText())
    }

    @Test
    fun `får 409-feil hvis status på varselet er noe annet enn AKTIV`() {
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
            mutation SettVarselstatusVurdert {
                settVarselstatusVurdert(
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
    fun `får 500-feil dersom oppdateringen tryner for settVarselstatusVurdert`() {
        // Vi lar være å opprette definisjon for å fremprovosere at oppdateringen feiler
        val vedtaksperiodeId = UUID.randomUUID()
        val generasjonId = UUID.randomUUID()
        val definisjonId = UUID.randomUUID()
        opprettSaksbehandler()
        opprettVedtaksperiode(opprettPerson(), opprettArbeidsgiver())
        val generasjonRef = nyGenerasjon(generasjonId = generasjonId, vedtaksperiodeId = vedtaksperiodeId)
        nyttVarsel(kode = "EN_KODE", vedtaksperiodeId = vedtaksperiodeId, generasjonRef = generasjonRef)

        val body = runQuery(
            """
            mutation SettVarselstatusVurdert {
                settVarselstatusVurdert(
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

        assertEquals(500, body["errors"].first()["extensions"]["code"].asInt())
    }

    @Test
    fun `får 404-feil dersom varselet ikke finnes for settVarselstatusVurdert`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val generasjonId = UUID.randomUUID()
        val definisjonId = UUID.randomUUID()
        opprettSaksbehandler()
        opprettVedtaksperiode(opprettPerson(), opprettArbeidsgiver())
        nyGenerasjon(generasjonId = generasjonId, vedtaksperiodeId = vedtaksperiodeId)

        val body = runQuery(
            """
            mutation SettVarselstatusVurdert {
                settVarselstatusVurdert(
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

        assertEquals(404, body["errors"].first()["extensions"]["code"].asInt())
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
            mutation SettVarselstatusAktiv {
                settVarselstatusAktiv(
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

        assertTrue(body["data"]["settVarselstatusAktiv"]["vurdering"].isNull)
    }

    @Test
    fun `får 409-feil hvis status på varselet er noe annet enn GODKJENT for settVarselstatusAktiv`() {
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
            mutation SettVarselstatusAktiv {
                settVarselstatusAktiv(
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
    fun `får 500-feil dersom oppdateringen tryner for settVarselstatusAktiv`() {
        // Vi lar være å opprette definisjon for å fremprovosere at oppdateringen feiler
        val vedtaksperiodeId = UUID.randomUUID()
        val generasjonId = UUID.randomUUID()
        opprettSaksbehandler()
        opprettVedtaksperiode(opprettPerson(), opprettArbeidsgiver())
        val generasjonRef = nyGenerasjon(generasjonId = generasjonId, vedtaksperiodeId = vedtaksperiodeId)
        nyttVarsel(kode = "EN_KODE", vedtaksperiodeId = vedtaksperiodeId, generasjonRef = generasjonRef)

        val body = runQuery(
            """
            mutation SettVarselstatusAktiv {
                settVarselstatusAktiv(
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

        assertEquals(500, body["errors"].first()["extensions"]["code"].asInt())
    }

    @Test
    fun `får 404-feil hvis varselet ikke finnes for settVarselstatusAktiv`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val generasjonId = UUID.randomUUID()
        opprettSaksbehandler()
        opprettVedtaksperiode(opprettPerson(), opprettArbeidsgiver())
        nyGenerasjon(generasjonId = generasjonId, vedtaksperiodeId = vedtaksperiodeId)

        val body = runQuery(
            """
            mutation SettVarselstatusAktiv {
                settVarselstatusAktiv(
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





    // Alt under her skal bort

    @Test
    fun `sett status VURDERT old`() {
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
            mutation SettStatusVurdert {
                settStatusVurdert(
                    generasjonId: "$generasjonId", 
                    definisjonId: "$definisjonId",
                    varselkode: "EN_KODE", 
                    ident: "${SAKSBEHANDLER.oid}" 
                )
            }
        """
        )

        assertTrue(body["data"]["settStatusVurdert"].asBoolean())
    }

    @Test
    fun `sett status AKTIV old`() {
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
            mutation SettStatusAktiv {
                settStatusAktiv(
                    generasjonId: "$generasjonId", 
                    varselkode: "EN_KODE", 
                    ident: "${SAKSBEHANDLER.oid}" 
                )
            }
        """
        )

        assertTrue(body["data"]["settStatusAktiv"].asBoolean())
    }
}
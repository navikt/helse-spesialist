package no.nav.helse.spesialist.api.graphql.mutation

import java.util.UUID
import no.nav.helse.spesialist.api.AbstractGraphQLApiTest
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
    fun `sett status AKTIV`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val generasjonId = UUID.randomUUID()
        opprettSaksbehandler()
        opprettVedtaksperiode(opprettPerson(), opprettArbeidsgiver())
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
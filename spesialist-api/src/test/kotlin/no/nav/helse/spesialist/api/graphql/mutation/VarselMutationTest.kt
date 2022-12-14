package no.nav.helse.spesialist.api.graphql.mutation

import java.time.LocalDateTime
import java.util.UUID
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.spesialist.api.AbstractGraphQLApiTest
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.*
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

    private fun nyGenerasjon(
        vedtaksperiodeId: UUID = UUID.randomUUID(),
        generasjonId: UUID = UUID.randomUUID(),
        utbetalingId: UUID = UUID.randomUUID(),
        låstTidspunkt: LocalDateTime? = null,
    ): Long = sessionOf(dataSource, returnGeneratedKey = true).use { session ->
        @Language("PostgreSQL")
        val query = """
            INSERT INTO selve_vedtaksperiode_generasjon(vedtaksperiode_id, unik_id, utbetaling_id, opprettet_av_hendelse, låst_tidspunkt, låst_av_hendelse) 
            VALUES (?, ?, ?, ?, ?, ?)
        """
        return requireNotNull(
            session.run(
                queryOf(
                    query,
                    vedtaksperiodeId,
                    generasjonId,
                    utbetalingId,
                    UUID.randomUUID(),
                    låstTidspunkt,
                    UUID.randomUUID()
                ).asUpdateAndReturnGeneratedKey
            )
        )
    }

    private fun nyttVarsel(
        id: UUID = UUID.randomUUID(),
        vedtaksperiodeId: UUID = UUID.randomUUID(),
        kode: String = "EN_KODE",
        generasjonRef: Long,
        definisjonRef: Long? = null,
    ) = sessionOf(dataSource).use { session ->
        @Language("PostgreSQL")
        val query = """
            INSERT INTO selve_varsel(unik_id, kode, vedtaksperiode_id, generasjon_ref, definisjon_ref, opprettet, status_endret_ident, status_endret_tidspunkt) 
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """
        session.run(
            queryOf(
                query,
                id,
                kode,
                vedtaksperiodeId,
                generasjonRef,
                definisjonRef,
                LocalDateTime.now(),
                null,
                null
            ).asExecute
        )
    }
}
package no.nav.helse.sidegig

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDateTime
import java.util.Random
import java.util.UUID

internal class MyDaoTest : AbstractDatabaseTest() {
    val SAKSBEHANDLEROID = UUID.randomUUID()
    private val myDao = MyDao(dataSource)


    @Test
    fun `Returnerer 10 annulleringer`() {
        lagSaksbehandler(SAKSBEHANDLEROID)
        repeat(10) {
            lagAnnullertAvSaksbehandler(
                saksbehandlerRef = SAKSBEHANDLEROID,
                arbeidsgiverFagsystemId = fagsystemId(),
                personFagsystemId = fagsystemId()
            )
            lagAnnullertAvSaksbehandler(
                saksbehandlerRef = SAKSBEHANDLEROID,
                arbeidsgiverFagsystemId = null,
                personFagsystemId = null
            )
        }
        val result = myDao.find10Annulleringer()
        assertEquals(10, result.size)
    }

    private fun lagSaksbehandler(
        saksbehandlerIdent: UUID = UUID.randomUUID(),
    ) = insert(
        query = """
            INSERT INTO saksbehandler (oid, navn, epost, ident, siste_handling_utført_tidspunkt)
            VALUES (:oid, :navn, :epost, :ident, :siste_handling_utfort_tidspunkt)
        """.trimIndent(),
        paramMap = mapOf(
            "oid" to saksbehandlerIdent,
            "navn" to "",
            "epost" to "",
            "ident" to "",
            "siste_handling_utfort_tidspunkt" to LocalDateTime.now()
        )
    )

    private fun lagAnnullertAvSaksbehandler(
        saksbehandlerRef: UUID,
        arbeidsgiverFagsystemId: String? = UUID.randomUUID().toString(),
        personFagsystemId: String? = UUID.randomUUID().toString()
    ) = insert(
        query = """
            INSERT INTO annullert_av_saksbehandler (
                annullert_tidspunkt,
                saksbehandler_ref,
                årsaker,
                begrunnelse_ref,
                arbeidsgiver_fagsystem_id,
                person_fagsystem_id
            )
            VALUES (
                :annullert_tidspunkt,
                :saksbehandler_ref,
                :arsaker,
                :begrunnelse_ref,
                :arbeidsgiver_fagsystem_id,
                :person_fagsystem_id
            )
            """.trimIndent(),
        paramMap = mapOf(
            "annullert_tidspunkt" to Instant.now(),
            "saksbehandler_ref" to saksbehandlerRef,
            "arsaker" to emptyArray<String>(),
            "begrunnelse_ref" to 0,
            "arbeidsgiver_fagsystem_id" to arbeidsgiverFagsystemId,
            "person_fagsystem_id" to personFagsystemId
        )
    )

    private fun fagsystemId() = (0..31).map { 'A' + Random().nextInt('Z' - 'A') }.joinToString("")
}

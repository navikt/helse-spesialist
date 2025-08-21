package no.nav.helse.sidegig

import no.nav.helse.modell.person.vedtaksperiode.TilstandDto
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Random
import java.util.UUID
import kotlin.random.Random.Default.nextLong

internal class MyDaoTest : AbstractDatabaseTest() {
    val SAKSBEHANDLEROID = UUID.randomUUID()
    private val myDao = MyDao(dataSource)


    @Test
    fun `Returnerer 10 annulleringer`() {
        lagSaksbehandler(SAKSBEHANDLEROID)
        repeat(10) {
            lagAnnullertAvSaksbehandler(
                saksbehandlerRef = SAKSBEHANDLEROID,
                arbeidsgiverFagsystemId = lagFagsystemId(),
                personFagsystemId = lagFagsystemId()
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

    @Test
    fun `Finn utbetalingid for arbeidsgiver- og personfagsystemider`() {
        val arbeidsgiverFagsystemId = lagFagsystemId()
        val oppdragId = lagOppdrag(arbeidsgiverFagsystemId)
        val personFagsystemId = lagFagsystemId()
        val oppdragId2 = lagOppdrag(personFagsystemId)

        val personRef = lagPerson()
        val utbetalingId = UUID.randomUUID()
        lagUtbetalingId(
            utbetalingId = utbetalingId,
            personRef = personRef,
            arbeidsgiverFagsystemId = oppdragId,
            personFagsystemId = oppdragId2,
            arbeidsgiverIdentifikator = lagOrganisasjonsnummer()
        )

        val result = myDao.findUtbetalingId(
            arbeidsgiverFagsystemId = arbeidsgiverFagsystemId,
            personFagsystemId = personFagsystemId
        )
        assertEquals(utbetalingId, result)
    }

    @Test
    fun `Finn behandling for utbetaling i sykefraværstilfelle`() {
        val arbeidsgiverFagsystemId = lagFagsystemId()
        val oppdragId = lagOppdrag(arbeidsgiverFagsystemId)
        val personFagsystemId = lagFagsystemId()
        val oppdragId2 = lagOppdrag(personFagsystemId)

        val personRef = requireNotNull(lagPerson())
        val utbetalingId = UUID.randomUUID()
        lagUtbetalingId(
            utbetalingId = utbetalingId,
            personRef = personRef,
            arbeidsgiverFagsystemId = oppdragId,
            personFagsystemId = oppdragId2,
            arbeidsgiverIdentifikator = lagOrganisasjonsnummer()
        )
        val vedtakperiodeId = UUID.randomUUID()
        val arbeidsgiverIdentifikator = lagOrganisasjonsnummer()
        lagVedtak(vedtakperiodeId, personRef, arbeidsgiverIdentifikator)
        val skjæringstidspunkt = LocalDate.now()
        val behandlingId = requireNotNull(lagBehandling(vedtakperiodeId, utbetalingId, skjæringstidspunkt))

        val result = myDao.finnBehandlingISykefraværstilfelle(utbetalingId)
        assertEquals(
            MyDao.BehandlingISykefraværstilfelleRow(
                behandlingId = behandlingId,
                vedtaksperiodeId = vedtakperiodeId,
                skjæringstidspunkt = skjæringstidspunkt,
                personId = personRef,
                arbeidsgiverId = arbeidsgiverIdentifikator
            ),
            result
        )
    }

    @Test
    fun `Finn første vedtaksperiodeid for ett sykefraværstilfelle`() {
        val arbeidsgiverFagsystemId = lagFagsystemId()
        val oppdragId = lagOppdrag(arbeidsgiverFagsystemId)
        val personFagsystemId = lagFagsystemId()
        val oppdragId2 = lagOppdrag(personFagsystemId)

        val personRef = requireNotNull(lagPerson())
        val utbetalingId = UUID.randomUUID()
        val arbeidsgiverIdentifikator = lagOrganisasjonsnummer()
        lagUtbetalingId(
            utbetalingId = utbetalingId,
            personRef = personRef,
            arbeidsgiverFagsystemId = oppdragId,
            personFagsystemId = oppdragId2,
            arbeidsgiverIdentifikator = arbeidsgiverIdentifikator
        )
        val vedtakperiodeId = UUID.randomUUID()
        lagVedtak(vedtakperiodeId, personRef, arbeidsgiverIdentifikator)
        lagBehandling(vedtakperiodeId, utbetalingId, LocalDate.now())

        val behandling = requireNotNull(myDao.finnBehandlingISykefraværstilfelle(utbetalingId))

        val result = myDao.finnFørsteVedtaksperiodeIdForEttSykefraværstilfelle(behandling)

        assertEquals(vedtakperiodeId, result)
    }

    private fun lagBehandling(vedtakperiodeId: UUID, utbetalingId: UUID, skjæringstidspunkt: LocalDate) =
        insertAndReturnGeneratedKey(
            query = """
            INSERT INTO behandling(
                vedtaksperiode_id,
                utbetaling_id,
                opprettet_av_hendelse,
                tilstand_endret_tidspunkt,
                tilstand_endret_av_hendelse,
                fom,
                tom,
                skjæringstidspunkt,
                tilstand,
                spleis_behandling_id
            ) VALUES (
                :vedtaksperiodeId,
                :utbetalingId,
                :opprettetAvHendelse,
                now(),
                :tilstandEndretAvHendelse,
                now(),
                now(),
                :skjaringstidspunkt,
                :tilstand::generasjon_tilstand,
                :spleisBehandlingId
            )
        """.trimIndent(),
            paramMap = mapOf(
                "vedtaksperiodeId" to vedtakperiodeId,
                "utbetalingId" to utbetalingId,
                "opprettetAvHendelse" to UUID.randomUUID(),
                "tilstandEndretAvHendelse" to UUID.randomUUID(),
                "skjaringstidspunkt" to skjæringstidspunkt,
                "tilstand" to TilstandDto.AvsluttetUtenVedtak.name,
                "spleisBehandlingId" to UUID.randomUUID(),
            )
        )

    private fun lagVedtak(
        vedtakperiodeId: UUID,
        personRef: Long?,
        arbeidsgiverIdentifikator: String
    ) = insert(
        query = """
            INSERT INTO vedtak (
                vedtaksperiode_id,
                fom,
                tom,
                person_ref,
                forkastet,
                forkastet_tidspunkt,
                forkastet_av_hendelse,
                arbeidsgiver_identifikator
            ) 
            VALUES (
                :vedtaksperiodeId,
                now(),
                now(),
                :personRef,
                :forkastet,
                now(),
                :forkastetAvHendelse,
                :arbeidsgiverIdentifikator
            ) 
        """.trimIndent(),
        paramMap = mapOf(
            "vedtaksperiodeId" to vedtakperiodeId,
            "personRef" to personRef,
            "forkastet" to false,
            "forkastetAvHendelse" to UUID.randomUUID(),
            "arbeidsgiverIdentifikator" to arbeidsgiverIdentifikator
        )
    )

    private fun lagPerson() = insertAndReturnGeneratedKey(
        query = """
            INSERT INTO person (
                fødselsnummer, 
                aktør_id,
                info_ref,
                enhet_ref,
                enhet_ref_oppdatert,
                personinfo_oppdatert,
                infotrygdutbetalinger_ref,
                infotrygdutbetalinger_oppdatert
            ) 
            VALUES (
                :fodselsnummer,
                :aktorId,
                :infoRef,
                :enhetRef,
                now(),
                now(),
                :infotrygdUtbetalingerRef,
                now()
            )
        """.trimIndent(),
        paramMap = mapOf(
            "fodselsnummer" to lagFødselsnummer(),
            "aktorId" to lagAktørId(),
            "infoRef" to null,
            "enhetRef" to null,
            "infotrygdUtbetalingerRef" to null
        )
    )

    private fun lagUtbetalingId(
        utbetalingId: UUID,
        personRef: Long?,
        arbeidsgiverFagsystemId: Long?,
        personFagsystemId: Long?,
        arbeidsgiverIdentifikator: String
    ) = insert(
        query = """
            INSERT INTO utbetaling_id (utbetaling_id, person_ref, arbeidsgiver_fagsystem_id_ref, person_fagsystem_id_ref, type, opprettet, arbeidsgiverbeløp, personbeløp, arbeidsgiver_identifikator)
            VALUES (
                :utbetalingId,
                :personRef,
                :arbeidsgiverFagsystemIdRef,
                :personFagsystemIdRef,
                CAST(:type as utbetaling_type),
                :opprettet,
                :arbeidsgiverbelop,
                :personbelop,
                :arbeidsgiverIdentifikator
            )
        """.trimMargin(),
        paramMap = mapOf(
            "utbetalingId" to utbetalingId,
            "personRef" to personRef,
            "arbeidsgiverFagsystemIdRef" to arbeidsgiverFagsystemId,
            "personFagsystemIdRef" to personFagsystemId,
            "type" to "UTBETALING",
            "opprettet" to Instant.now(),
            "arbeidsgiverbelop" to 0,
            "personbelop" to 0,
            "arbeidsgiverIdentifikator" to arbeidsgiverIdentifikator,
        )
    )

    private fun lagOppdrag(fagsystemId: String = lagFagsystemId()) = insertAndReturnGeneratedKey(
        query = """
            INSERT INTO oppdrag (fagsystem_id, mottaker) 
            VALUES (:fagsystemId, :mottaker)
        """.trimMargin(),
        paramMap = mapOf(
            "fagsystemId" to fagsystemId,
            "mottaker" to ""
        )
    )

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

    private fun lagFagsystemId() = (0..31).map { 'A' + Random().nextInt('Z' - 'A') }.joinToString("")

    private fun lagFødselsnummer() = nextLong(from = 10000_00000, until = 319999_99999).toString().padStart(11, '0')

    private fun lagAktørId() = nextLong(from = 1_000_000_000_000, until = 1_000_099_999_999).toString()

    private fun lagOrganisasjonsnummer() = nextLong(from = 800_000_000, until = 999_999_999).toString()
}

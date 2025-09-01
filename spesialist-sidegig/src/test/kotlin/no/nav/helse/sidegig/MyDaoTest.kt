package no.nav.helse.sidegig

import no.nav.helse.db.BehandlingISykefraværstilfelleRow
import no.nav.helse.modell.person.vedtaksperiode.TilstandDto
import no.nav.helse.spesialist.db.DbQuery
import no.nav.helse.spesialist.db.dao.PgAnnulleringDao
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNull
import org.junit.jupiter.api.parallel.Isolated
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Random
import java.util.UUID
import kotlin.random.Random.Default.nextLong

@Isolated
internal class MyDaoTest : AbstractDatabaseTest() {
    val SAKSBEHANDLEROID = UUID.randomUUID()
    private val pgAnnulleringDao = PgAnnulleringDao(dataSource)

    @Test
    fun `Returnerer 10 annulleringer`() {
        DbQuery(dataSource).execute("TRUNCATE annullert_av_saksbehandler CASCADE")
        lagSaksbehandler(SAKSBEHANDLEROID)
        repeat(5) {
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
        val result = pgAnnulleringDao.find10Annulleringer()
        assertEquals(5, result.size)
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

        val result = pgAnnulleringDao.findUtbetalingId(
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

        val result = pgAnnulleringDao.finnBehandlingISykefraværstilfelle(utbetalingId)
        assertEquals(
            BehandlingISykefraværstilfelleRow(
                behandlingId = behandlingId,
                vedtaksperiodeId = vedtakperiodeId,
                skjæringstidspunkt = skjæringstidspunkt,
                personId = personRef,
                arbeidsgiverId = arbeidsgiverIdentifikator,
                utbetalingId = utbetalingId,
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

        val behandling = requireNotNull(pgAnnulleringDao.finnBehandlingISykefraværstilfelle(utbetalingId))

        val result = pgAnnulleringDao.finnFørsteVedtaksperiodeIdForEttSykefraværstilfelle(behandling)

        assertEquals(vedtakperiodeId, result)
    }

    @Test
    fun `Finn første vedtaksperiodeid for sykefraværstilfelle med fler perioder og samme arbeidsgiver og person fagsystemId for alle utbetalinger`() {
        // Given:
        val arbeidsgiverFagsystemId = lagFagsystemId()
        val arbeidsgiverOppdragId = lagOppdrag(arbeidsgiverFagsystemId)
        val personFagsystemId = lagFagsystemId()
        val personOppdragId = lagOppdrag(personFagsystemId)

        val personRef = requireNotNull(lagPerson())
        val arbeidsgiverIdentifikator = lagOrganisasjonsnummer()

        val utbetalingId1 = UUID.randomUUID()
        val utbetalingId2 = UUID.randomUUID()
        val utbetalingId3 = UUID.randomUUID()

        val skjæringstidspunkt = LocalDate.now()
        val vedtakperiodeId1 = UUID.randomUUID()
        val vedtakperiodeId2 = UUID.randomUUID()
        val vedtakperiodeId3 = UUID.randomUUID()


        // When:
        listOf(
            vedtakperiodeId1 to utbetalingId1,
            vedtakperiodeId2 to utbetalingId2,
            vedtakperiodeId3 to utbetalingId3
        ).forEachIndexed { i, (vedtaksperiodeId, utbetalingId) ->
            val fom = skjæringstidspunkt.plusDays(i.toLong())
            lagVedtak(
                vedtakperiodeId = vedtaksperiodeId,
                personRef = personRef,
                arbeidsgiverIdentifikator = arbeidsgiverIdentifikator,
                fom = fom,
                tom = fom
            )
            lagUtbetalingId(
                utbetalingId = utbetalingId,
                personRef = personRef,
                arbeidsgiverFagsystemId = arbeidsgiverOppdragId,
                personFagsystemId = personOppdragId,
                arbeidsgiverIdentifikator = arbeidsgiverIdentifikator,
            )
            lagBehandling(
                vedtakperiodeId = vedtaksperiodeId,
                utbetalingId = utbetalingId,
                skjæringstidspunkt = skjæringstidspunkt,
                fom = fom,
                tom = fom,
            )
        }

        // Then:
        val behandling = requireNotNull(pgAnnulleringDao.finnBehandlingISykefraværstilfelle(utbetalingId2))
        val result = pgAnnulleringDao.finnFørsteVedtaksperiodeIdForEttSykefraværstilfelle(behandling)
        assertEquals(vedtakperiodeId1, result)
    }

    @Test
    fun `Finn første vedtaksperiodeid for sykefraværstilfelle med fler perioder og ny arbeidsgiver og person fagsystemId for hver utbetaling`() {
        // Given:
        val personRef = requireNotNull(lagPerson())
        val arbeidsgiverIdentifikator = lagOrganisasjonsnummer()

        val utbetalingId1 = UUID.randomUUID()
        val utbetalingId2 = UUID.randomUUID()
        val utbetalingId3 = UUID.randomUUID()

        val skjæringstidspunkt = LocalDate.now()
        val vedtakperiodeId1 = UUID.randomUUID()
        val vedtakperiodeId2 = UUID.randomUUID()
        val vedtakperiodeId3 = UUID.randomUUID()


        // When:
        listOf(
            vedtakperiodeId1 to utbetalingId1,
            vedtakperiodeId2 to utbetalingId2,
            vedtakperiodeId3 to utbetalingId3
        ).forEachIndexed { i, (vedtaksperiodeId, utbetalingId) ->
            val fom = skjæringstidspunkt.plusDays(i.toLong())
            val arbeidsgiverFagsystemId = lagFagsystemId()
            val arbeidsgiverOppdragId = lagOppdrag(arbeidsgiverFagsystemId)
            val personFagsystemId = lagFagsystemId()
            val personOppdragId = lagOppdrag(personFagsystemId)
            lagVedtak(
                vedtakperiodeId = vedtaksperiodeId,
                personRef = personRef,
                arbeidsgiverIdentifikator = arbeidsgiverIdentifikator,
                fom = fom,
                tom = fom
            )
            lagUtbetalingId(
                utbetalingId = utbetalingId,
                personRef = personRef,
                arbeidsgiverFagsystemId = arbeidsgiverOppdragId,
                personFagsystemId = personOppdragId,
                arbeidsgiverIdentifikator = arbeidsgiverIdentifikator,
            )
            lagBehandling(
                vedtakperiodeId = vedtaksperiodeId,
                utbetalingId = utbetalingId,
                skjæringstidspunkt = skjæringstidspunkt,
                fom = fom,
                tom = fom,
            )
        }

        // Then:
        val behandling = requireNotNull(pgAnnulleringDao.finnBehandlingISykefraværstilfelle(utbetalingId2))
        val result = pgAnnulleringDao.finnFørsteVedtaksperiodeIdForEttSykefraværstilfelle(behandling)
        assertEquals(vedtakperiodeId1, result)
    }

    @Test
    fun `Gammel utbetalingsrigg - Velg første vedtaksperiodeid for sykefraværstilfelle der det er ett opphold i sykefraværstilfelle på mindre enn 16 dager`() {
        // Given:
        val personRef = requireNotNull(lagPerson())
        val arbeidsgiverIdentifikator = lagOrganisasjonsnummer()

        val utbetalingId1 = UUID.randomUUID()
        val utbetalingId2 = UUID.randomUUID()
        val utbetalingId3 = UUID.randomUUID()

        val førsteSkjæringstidspunkt = LocalDate.now()
        val andreSkjæringstidspunkt = førsteSkjæringstidspunkt.plusDays(4)
        val vedtaksperiodeId1 = UUID.randomUUID()
        val vedtaksperiodeId2 = UUID.randomUUID()
        val vedtaksperiodeId3 = UUID.randomUUID()


        // When:
        val arbeidsgiverFagsystemId = lagFagsystemId()
        val arbeidsgiverOppdragId = lagOppdrag(arbeidsgiverFagsystemId)
        val personFagsystemId = lagFagsystemId()
        val personOppdragId = lagOppdrag(personFagsystemId)

        // Første periode
        opprettUtbetaltVedtak(
            vedtaksperiodeId = vedtaksperiodeId1,
            personRef = personRef,
            arbeidsgiverIdentifikator = arbeidsgiverIdentifikator,
            skjæringstidspunkt = førsteSkjæringstidspunkt,
            fom = førsteSkjæringstidspunkt,
            utbetalingId = utbetalingId1,
            arbeidsgiverOppdragId = arbeidsgiverOppdragId,
            personOppdragId = personOppdragId
        )
        // Andre periode
        opprettUtbetaltVedtak(
            vedtaksperiodeId = vedtaksperiodeId2,
            personRef = personRef,
            arbeidsgiverIdentifikator = arbeidsgiverIdentifikator,
            skjæringstidspunkt = andreSkjæringstidspunkt,
            fom = andreSkjæringstidspunkt,
            utbetalingId = utbetalingId2,
            arbeidsgiverOppdragId = arbeidsgiverOppdragId,
            personOppdragId = personOppdragId
        )
        // Tredje periode
        opprettUtbetaltVedtak(
            vedtaksperiodeId = vedtaksperiodeId3,
            personRef = personRef,
            arbeidsgiverIdentifikator = arbeidsgiverIdentifikator,
            skjæringstidspunkt = andreSkjæringstidspunkt,
            fom = andreSkjæringstidspunkt.plusDays(2),
            utbetalingId = utbetalingId3,
            arbeidsgiverOppdragId = arbeidsgiverOppdragId,
            personOppdragId = personOppdragId
        )

        // Then:
        val behandling = requireNotNull(pgAnnulleringDao.finnBehandlingISykefraværstilfelle(utbetalingId3))
        val result = pgAnnulleringDao.finnFørsteVedtaksperiodeIdForEttSykefraværstilfelle(behandling)
        assertEquals(vedtaksperiodeId1, result)
    }

    @Test
    fun `Ny utbetalingsrigg - Velg første vedtaksperiodeid for sykefraværstilfelle der det er flere opphold i sykefraværstilfelle der hver er mindre enn 16 dager`() {
        // Given:
        val personRef = requireNotNull(lagPerson())
        val arbeidsgiverIdentifikator = lagOrganisasjonsnummer()

        val utbetalingId1 = UUID.randomUUID()
        val utbetalingId2 = UUID.randomUUID()
        val utbetalingId3 = UUID.randomUUID()

        val førsteSkjæringstidspunkt = LocalDate.now()
        val andreSkjæringstidspunkt = førsteSkjæringstidspunkt.plusDays(4)
        val vedtaksperiodeId1 = UUID.randomUUID()
        val vedtaksperiodeId2 = UUID.randomUUID()
        val vedtaksperiodeId3 = UUID.randomUUID()


        // When:
        // Første periode
        opprettUtbetaltVedtak(
            vedtaksperiodeId = vedtaksperiodeId1,
            personRef = personRef,
            arbeidsgiverIdentifikator = arbeidsgiverIdentifikator,
            skjæringstidspunkt = førsteSkjæringstidspunkt,
            fom = førsteSkjæringstidspunkt,
            utbetalingId = utbetalingId1,
        )
        // Andre periode
        opprettUtbetaltVedtak(
            vedtaksperiodeId = vedtaksperiodeId2,
            personRef = personRef,
            arbeidsgiverIdentifikator = arbeidsgiverIdentifikator,
            skjæringstidspunkt = andreSkjæringstidspunkt,
            fom = andreSkjæringstidspunkt,
            utbetalingId = utbetalingId2,
        )
        // Tredje periode
        opprettUtbetaltVedtak(
            vedtaksperiodeId = vedtaksperiodeId3,
            personRef = personRef,
            arbeidsgiverIdentifikator = arbeidsgiverIdentifikator,
            skjæringstidspunkt = andreSkjæringstidspunkt,
            fom = andreSkjæringstidspunkt.plusDays(2),
            utbetalingId = utbetalingId3,
        )

        // Then:
        val behandling = requireNotNull(pgAnnulleringDao.finnBehandlingISykefraværstilfelle(utbetalingId3))
        val result = pgAnnulleringDao.finnFørsteVedtaksperiodeIdForEttSykefraværstilfelle(behandling)
        assertEquals(vedtaksperiodeId1, result)
    }

    @Test
    fun `Håndterer manglende utbetalingId på behandling`() {
        // Given:
        val personRef = requireNotNull(lagPerson())
        val arbeidsgiverIdentifikator = lagOrganisasjonsnummer()

        val utbetalingId1 = UUID.randomUUID()
        val utbetalingId3 = UUID.randomUUID()

        val førsteSkjæringstidspunkt = LocalDate.now()
        val andreSkjæringstidspunkt = førsteSkjæringstidspunkt.plusDays(4)
        val vedtaksperiodeId1 = UUID.randomUUID()
        val vedtaksperiodeId2 = UUID.randomUUID()
        val vedtaksperiodeId3 = UUID.randomUUID()


        // When:
        // Første periode
        opprettUtbetaltVedtak(
            vedtaksperiodeId = vedtaksperiodeId1,
            personRef = personRef,
            arbeidsgiverIdentifikator = arbeidsgiverIdentifikator,
            skjæringstidspunkt = førsteSkjæringstidspunkt,
            fom = førsteSkjæringstidspunkt,
            utbetalingId = utbetalingId1,
        )
        // Andre periode
        opprettUtbetaltVedtak(
            vedtaksperiodeId = vedtaksperiodeId2,
            personRef = personRef,
            arbeidsgiverIdentifikator = arbeidsgiverIdentifikator,
            skjæringstidspunkt = andreSkjæringstidspunkt,
            fom = andreSkjæringstidspunkt,
            utbetalingId = null,
        )
        // Tredje periode
        opprettUtbetaltVedtak(
            vedtaksperiodeId = vedtaksperiodeId3,
            personRef = personRef,
            arbeidsgiverIdentifikator = arbeidsgiverIdentifikator,
            skjæringstidspunkt = andreSkjæringstidspunkt,
            fom = andreSkjæringstidspunkt.plusDays(2),
            utbetalingId = utbetalingId3,
        )

        // Then:
        val behandling = requireNotNull(pgAnnulleringDao.finnBehandlingISykefraværstilfelle(utbetalingId3))
        val result = pgAnnulleringDao.finnFørsteVedtaksperiodeIdForEttSykefraværstilfelle(behandling)
        assertEquals(vedtaksperiodeId1, result)
    }

    @Test
    fun `Henter eldste vedtaksperiodeid for sykefraværstilfelle som er annuller det er flere sykefraværstilfeller`(){
        // Given:
        val personRef = requireNotNull(lagPerson())
        val arbeidsgiverIdentifikator = lagOrganisasjonsnummer()

        // When:
        // Sykefraværstilfelle 1
        val utbetalingId1 = UUID.randomUUID()
        val utbetalingId2 = UUID.randomUUID()
        val utbetalingId3 = UUID.randomUUID()

        val skjæringstidspunkt1 = LocalDate.now()
        val vedtaksperiodeId1 = UUID.randomUUID()
        val vedtaksperiodeId2 = UUID.randomUUID()
        val vedtaksperiodeId3 = UUID.randomUUID()
        // Første periode
        opprettUtbetaltVedtak(
            vedtaksperiodeId = vedtaksperiodeId1,
            personRef = personRef,
            arbeidsgiverIdentifikator = arbeidsgiverIdentifikator,
            skjæringstidspunkt = skjæringstidspunkt1,
            fom = skjæringstidspunkt1,
            utbetalingId = utbetalingId1,
        )
        // Andre periode
        opprettUtbetaltVedtak(
            vedtaksperiodeId = vedtaksperiodeId2,
            personRef = personRef,
            arbeidsgiverIdentifikator = arbeidsgiverIdentifikator,
            skjæringstidspunkt = skjæringstidspunkt1,
            fom = skjæringstidspunkt1.plusDays(2),
            utbetalingId = utbetalingId2,
        )
        // Tredje periode
        opprettUtbetaltVedtak(
            vedtaksperiodeId = vedtaksperiodeId3,
            personRef = personRef,
            arbeidsgiverIdentifikator = arbeidsgiverIdentifikator,
            skjæringstidspunkt = skjæringstidspunkt1,
            fom = skjæringstidspunkt1.plusDays(4),
            utbetalingId = utbetalingId3,
        )

        // Sykefraværstilfelle 2
        val utbetalingId4 = UUID.randomUUID()
        val utbetalingId5 = UUID.randomUUID()
        val utbetalingId6 = UUID.randomUUID()

        val skjæringstidspunkt2 = LocalDate.now().plusDays(30)
        val vedtaksperiodeId4 = UUID.randomUUID()
        val vedtaksperiodeId5 = UUID.randomUUID()
        val vedtaksperiodeId6 = UUID.randomUUID()
        // Første periode
        opprettUtbetaltVedtak(
            vedtaksperiodeId = vedtaksperiodeId4,
            personRef = personRef,
            arbeidsgiverIdentifikator = arbeidsgiverIdentifikator,
            skjæringstidspunkt = skjæringstidspunkt2,
            fom = skjæringstidspunkt2,
            utbetalingId = utbetalingId4,
        )
        // Andre periode
        opprettUtbetaltVedtak(
            vedtaksperiodeId = vedtaksperiodeId5,
            personRef = personRef,
            arbeidsgiverIdentifikator = arbeidsgiverIdentifikator,
            skjæringstidspunkt = skjæringstidspunkt2,
            fom = skjæringstidspunkt2.plusDays(2),
            utbetalingId = utbetalingId5,
        )
        // Tredje periode
        opprettUtbetaltVedtak(
            vedtaksperiodeId = vedtaksperiodeId6,
            personRef = personRef,
            arbeidsgiverIdentifikator = arbeidsgiverIdentifikator,
            skjæringstidspunkt = skjæringstidspunkt2,
            fom = skjæringstidspunkt2.plusDays(4),
            utbetalingId = utbetalingId6,
        )

        // Then:
        val behandling = requireNotNull(pgAnnulleringDao.finnBehandlingISykefraværstilfelle(utbetalingId6))
        val result = pgAnnulleringDao.finnFørsteVedtaksperiodeIdForEttSykefraværstilfelle(behandling)
        assertEquals(vedtaksperiodeId4, result)
    }

    @Test
    fun `Oppdaterer annullering med vedtaksperiodeid`() {
        DbQuery(dataSource).execute("TRUNCATE annullert_av_saksbehandler CASCADE")
        lagSaksbehandler(SAKSBEHANDLEROID)
        val vedtakperiodeId = UUID.randomUUID()
        val annulleringId = lagAnnullertAvSaksbehandler(SAKSBEHANDLEROID)
        val annullering = hentAnnullering(annulleringId)
        assertNull(annullering?.vedtaksperiodeId)
        pgAnnulleringDao.oppdaterAnnulleringMedVedtaksperiodeId(
            annulleringId = annulleringId,
            vedtaksperiodeId = vedtakperiodeId
        )
        val result = hentAnnullering(annulleringId)
        assertEquals(vedtakperiodeId, result?.vedtaksperiodeId)
    }

    private fun hentAnnullering(annulleringId: Int) = query(
        query = """
            SELECT * from annullert_av_saksbehandler 
            WHERE id = :annulleringId
            """.trimMargin(),
        paramMap = mapOf(
            "annulleringId" to annulleringId
        )
    ) { row ->
        AnnulleringMedVedtaksperiodeId(
            id = row.long("id"),
            vedtaksperiodeId = row.uuidOrNull("vedtaksperiode_id")
        )
    }


    data class AnnulleringMedVedtaksperiodeId(
        val id: Long,
        val vedtaksperiodeId: UUID?
    )

    private fun opprettUtbetaltVedtak(
        vedtaksperiodeId: UUID,
        personRef: Long,
        arbeidsgiverIdentifikator: String,
        skjæringstidspunkt: LocalDate,
        fom: LocalDate,
        utbetalingId: UUID?,
        arbeidsgiverOppdragId: Long? = lagOppdrag(lagFagsystemId()),
        personOppdragId: Long? = lagOppdrag(lagFagsystemId())
    ) {
        lagVedtak(
            vedtakperiodeId = vedtaksperiodeId,
            personRef = personRef,
            arbeidsgiverIdentifikator = arbeidsgiverIdentifikator,
            fom = fom,
            tom = fom.plusDays(1)
        )
        utbetalingId?.let {
            lagUtbetalingId(
                utbetalingId = utbetalingId,
                personRef = personRef,
                arbeidsgiverFagsystemId = arbeidsgiverOppdragId,
                personFagsystemId = personOppdragId,
                arbeidsgiverIdentifikator = arbeidsgiverIdentifikator,
            )
        }
        lagBehandling(
            vedtakperiodeId = vedtaksperiodeId,
            utbetalingId = utbetalingId,
            skjæringstidspunkt = skjæringstidspunkt,
            fom = fom,
            tom = fom.plusDays(1)
        )
    }

    private fun lagBehandling(
        vedtakperiodeId: UUID,
        utbetalingId: UUID?,
        skjæringstidspunkt: LocalDate,
        fom: LocalDate = LocalDate.now(),
        tom: LocalDate = LocalDate.now()
    ) =
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
                :fom,
                :tom,
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
                "fom" to fom,
                "tom" to tom,
                "skjaringstidspunkt" to skjæringstidspunkt,
                "tilstand" to TilstandDto.AvsluttetUtenVedtak.name,
                "spleisBehandlingId" to UUID.randomUUID(),
            )
        )

    private fun lagVedtak(
        vedtakperiodeId: UUID,
        personRef: Long?,
        arbeidsgiverIdentifikator: String,
        fom: LocalDate = LocalDate.now(),
        tom: LocalDate = LocalDate.now(),
    ) = insert(
        query = """
            INSERT INTO vedtak (
                vedtaksperiode_id,
                fom,
                tom,
                person_ref,
                forkastet,
                arbeidsgiver_identifikator
            ) 
            VALUES (
                :vedtaksperiodeId,
                :fom,
                :tom,
                :personRef,
                :forkastet,
                :arbeidsgiverIdentifikator
            ) 
        """.trimIndent(),
        paramMap = mapOf(
            "vedtaksperiodeId" to vedtakperiodeId,
            "fom" to fom,
            "tom" to tom,
            "personRef" to personRef,
            "forkastet" to false,
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

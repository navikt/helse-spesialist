package no.nav.helse.spesialist.db.repository

import no.nav.helse.db.SorteringsnøkkelForDatabase
import no.nav.helse.db.SorteringsnøkkelForDatabase.BEHANDLING_OPPRETTET_TIDSPUNKT
import no.nav.helse.db.SorteringsnøkkelForDatabase.OPPRETTET
import no.nav.helse.db.SorteringsnøkkelForDatabase.TIDSFRIST
import no.nav.helse.db.SorteringsnøkkelForDatabase.TILDELT_TIL
import no.nav.helse.db.Sorteringsrekkefølge
import no.nav.helse.db.Sorteringsrekkefølge.STIGENDE
import no.nav.helse.db.Sorteringsrekkefølge.SYNKENDE
import no.nav.helse.mediator.oppgave.OppgaveRepository
import no.nav.helse.modell.oppgave.Egenskap
import no.nav.helse.modell.oppgave.Inntektsforhold
import no.nav.helse.modell.oppgave.Mottaker
import no.nav.helse.modell.oppgave.Oppgave
import no.nav.helse.modell.oppgave.Oppgavetype
import no.nav.helse.modell.vedtaksperiode.Inntektskilde
import no.nav.helse.modell.vedtaksperiode.Periodetype
import no.nav.helse.spesialist.db.AbstractDBIntegrationTest
import no.nav.helse.spesialist.db.HelseDao.Companion.somDbArray
import no.nav.helse.spesialist.domain.SaksbehandlerOid
import no.nav.helse.spesialist.domain.UtbetalingId
import no.nav.helse.spesialist.domain.Vedtaksperiode
import no.nav.helse.spesialist.domain.legacy.SaksbehandlerWrapper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Isolated
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.UUID
import kotlin.math.absoluteValue
import kotlin.random.Random
import kotlin.test.assertEquals

@Isolated
class PgOppgaveRepositorySorteringTest : AbstractDBIntegrationTest() {
    private val repository = PgOppgaveRepository(session)

    @BeforeEach
    fun tømTabeller() {
        dbQuery.execute("TRUNCATE oppgave CASCADE")
    }

    @Test
    fun `sorterer oppgaver riktig på tvers av paginering`() {
        val opprettedeOppgaver = mutableListOf<Oppgave>()
        val saksbehandlere = mutableListOf<SaksbehandlerWrapper>()
        repeat(25) {
            val saksbehandler = nyLegacySaksbehandler().also { saksbehandlere.add(it) }
            opprettOppgave().also { it.tildelOgLagre(saksbehandler) }.also(opprettedeOppgaver::add)
        }

        assertEquals(
            opprettedeOppgaver.map { it.opprettet.somInstantISystemetsTidssone() }.sorted(),
            hentOppgaverTiOgTi(OPPRETTET, STIGENDE).map { it.opprettetTidspunkt })
        assertEquals(
            opprettedeOppgaver.map { it.opprettet.somInstantISystemetsTidssone() }.sorted().reversed(),
            hentOppgaverTiOgTi(OPPRETTET, SYNKENDE).map { it.opprettetTidspunkt })

        assertEquals(
            finnBehandlingOpprettetTidspunkt(opprettedeOppgaver.map { it.behandlingId }).sorted(),
            hentOppgaverTiOgTi(BEHANDLING_OPPRETTET_TIDSPUNKT, STIGENDE).map { it.behandlingOpprettetTidspunkt })
        assertEquals(
            finnBehandlingOpprettetTidspunkt(opprettedeOppgaver.map { it.behandlingId }).sorted().reversed(),
            hentOppgaverTiOgTi(BEHANDLING_OPPRETTET_TIDSPUNKT, SYNKENDE).map { it.behandlingOpprettetTidspunkt })

        assertEquals(
            finnTildeltTilNavn(opprettedeOppgaver.map { it.id }).sorted(),
            hentOppgaverTiOgTi(TILDELT_TIL, STIGENDE).map { finnSaksbehandlernavn(it.tildeltTilOid!!) }
        )
        assertEquals(
            finnTildeltTilNavn(opprettedeOppgaver.map { it.id }.sorted().reversed()),
            finnTildeltTilNavn(oppgaveIds = hentOppgaverTiOgTi(TILDELT_TIL, SYNKENDE).map { it.id })
        )

        val frister = mutableListOf<LocalDate>()
        opprettedeOppgaver.forEachIndexed { index, oppgave ->
            frister.finnLedigDato().let {
                frister.add(it)
                oppgave.leggPåVentOgLagre(saksbehandlere[index], frist = it)
            }
        }

        assertEquals(
            finnPåVentId(opprettedeOppgaver.zip(frister).sortedBy { it.second }.map { it.first.id }),
            hentOppgaverTiOgTi(TIDSFRIST, STIGENDE, inkluderOppgaverPåVent = true).map { it.påVentId?.value }
        )
        assertEquals(
            finnPåVentId(opprettedeOppgaver.zip(frister).sortedBy { it.second }.reversed().map { it.first.id }),
            hentOppgaverTiOgTi(TIDSFRIST, SYNKENDE, inkluderOppgaverPåVent = true).map { it.påVentId?.value }
        )
    }

    fun hentOppgaverTiOgTi(
        nøkkel: SorteringsnøkkelForDatabase,
        rekkefolge: Sorteringsrekkefølge,
        inkluderOppgaverPåVent: Boolean = false,
    ) = sequence {
        var side = 1
        val antallPerSide = 10
        while (true) {
            val elementer = hentOppgaver(nøkkel, rekkefolge, inkluderOppgaverPåVent, side, antallPerSide).elementer
            if (elementer.isEmpty()) break
            yieldAll(elementer)
            if (elementer.size < antallPerSide) break
            side += 1
        }
    }.toList()

    private fun hentOppgaver(
        nøkkel: SorteringsnøkkelForDatabase,
        rekkefølge: Sorteringsrekkefølge,
        inkluderOppgaverPåVent: Boolean = false,
        side: Int = 1,
        antallRader: Int = 10,
    ): OppgaveRepository.Side<OppgaveRepository.OppgaveProjeksjon> = repository.finnOppgaveProjeksjoner(
        minstEnAvEgenskapene = listOf(buildSet { if (inkluderOppgaverPåVent) add(Egenskap.PÅ_VENT) }),
        ingenAvEgenskapene = emptySet(),
        erTildelt = null,
        tildeltTilOid = null,
        erPåVent = inkluderOppgaverPåVent,
        ikkeSendtTilBeslutterAvOid = null,
        sorterPå = nøkkel,
        sorteringsrekkefølge = rekkefølge,
        sidetall = side,
        sidestørrelse = antallRader,
    )

    fun opprettOppgave(): Oppgave {
        val person = opprettPerson()
        val arbeidsgiver = opprettArbeidsgiver()
        val vedtaksperiode = opprettVedtaksperiode(person, arbeidsgiver)
        val utbetalingId: UUID = UUID.randomUUID()
        val behandlingId = opprettBehandling(vedtaksperiode, utbetalingId)

        val godkjenningsbehovId: UUID = UUID.randomUUID()

        val oppgave = Oppgave.ny(
            id = Random.nextLong(),
            førsteOpprettet = repository.førsteOpprettetForBehandlingId(behandlingId),
            vedtaksperiodeId = vedtaksperiode.id.value,
            behandlingId = behandlingId,
            utbetalingId = utbetalingId,
            hendelseId = godkjenningsbehovId,
            kanAvvises = true,
            egenskaper = setOf(Egenskap.SØKNAD),
            mottaker = Mottaker.UtbetalingTilArbeidsgiver,
            type = Oppgavetype.Søknad,
            inntektskilde = Inntektskilde.EN_ARBEIDSGIVER,
            inntektsforhold = Inntektsforhold.Arbeidstaker,
            periodetype = Periodetype.FØRSTEGANGSBEHANDLING,
        )
        repository.lagre(oppgave)
        return oppgave
    }

    fun opprettBehandling(vedtaksperiode: Vedtaksperiode, utbetalingId: UUID): UUID {
        val behandling = opprettBehandling(vedtaksperiode, utbetalingId = UtbetalingId(utbetalingId))
        return behandling.spleisBehandlingId!!.value
    }

    fun finnBehandlingOpprettetTidspunkt(utbetalingIds: List<UUID>) = dbQuery.list(
        """
        select opprettet_tidspunkt from behandling where spleis_behandling_id = any(:ider::uuid[])
        """.trimIndent(), "ider" to utbetalingIds.somDbArray()
    ) {
        it.localDateTime("opprettet_tidspunkt").somInstantISystemetsTidssone()
    }

    fun finnTildeltTilNavn(oppgaveIds: List<Long>) = dbQuery.list(
        """
        select navn from tildeling
        join oppgave on oppgave_id_ref = id
        join saksbehandler on saksbehandler_ref = oid
        where id = any(:ider::bigint[])
        """.trimIndent(), "ider" to oppgaveIds.somDbArray()
    ) {
        it.string("navn")
    }

    fun finnPåVentId(oppgaveIds: List<Long>) = oppgaveIds.map { oppgaveId ->
        dbQuery.single(
        """
        select pv.id from pa_vent pv
        join vedtaksperiode v on pv.vedtaksperiode_id = v.vedtaksperiode_id
        join oppgave o on v.id = o.vedtak_ref
        where o.id = :oppgaveId
        """.trimIndent(), "oppgaveId" to oppgaveId
    ) {
        it.int("id")
    } }

    fun finnSaksbehandlernavn(oid: SaksbehandlerOid): String =
        dbQuery.single("select navn from saksbehandler where oid = :oid", "oid" to oid.value) {
            it.string("navn")
        }

    private fun MutableList<LocalDate>.finnLedigDato(): LocalDate {
        var kandidat: LocalDate
        do {
            kandidat = LocalDate.now().plusDays(Random.nextLong().absoluteValue % 365)
        } while (contains(kandidat))
        return kandidat
    }
}

private fun LocalDateTime.somInstantISystemetsTidssone(): Instant = atZone(ZoneId.systemDefault()).toInstant().roundToMicroseconds()

private fun Instant.roundToMicroseconds() = plus(500, ChronoUnit.NANOS).truncatedTo(ChronoUnit.MICROS)

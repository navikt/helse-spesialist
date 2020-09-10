package no.nav.helse.tildeling

import AbstractEndToEndTest
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotliquery.sessionOf
import no.nav.helse.Oppgavestatus
import no.nav.helse.modell.SnapshotDao
import no.nav.helse.modell.VedtakDao
import no.nav.helse.modell.arbeidsgiver.ArbeidsgiverDao
import no.nav.helse.modell.command.OppgaveDao
import no.nav.helse.modell.person.Kjønn
import no.nav.helse.modell.person.PersonDao
import no.nav.helse.modell.saksbehandler.persisterSaksbehandler
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import java.time.LocalDate
import java.util.*
import kotlin.test.assertEquals

class TildelingDaoTest : AbstractEndToEndTest() {

    private companion object {
        private const val FNR = "12345678911"
        private const val AKTØR = "4321098765432"
        private const val ORGNR = "123456789"
        private const val ORGNAVN = "Bedrift AS"
        private const val FORNAVN = "KARI"
        private const val MELLOMNAVN = "Mellomnavn"
        private const val ETTERNAVN = "Nordmann"
        private const val ENHET_OSLO = "0301"
        private val FØDSELSDATO = LocalDate.EPOCH
        private val KJØNN = Kjønn.Kvinne

        private val objectMapper = jacksonObjectMapper()

        private val HENDELSE_ID = UUID.randomUUID()
        private val VEDTAKSPERIODE_ID = UUID.randomUUID()
        private val FOM = LocalDate.of(2020, 1, 1)
        private val TOM = LocalDate.of(2020, 1, 31)

        private val SAKSBEHANDLER_OID = UUID.randomUUID()
        private val SAKSBEHANDLEREPOST = "sara.saksbehandler@nav.no"
    }

    private lateinit var personDao: PersonDao
    private lateinit var arbeidsgiverDao: ArbeidsgiverDao
    private lateinit var snapshotDao: SnapshotDao
    private lateinit var vedtakDao: VedtakDao
    private lateinit var oppgaveDao: OppgaveDao

    @BeforeEach
    fun setup() {
        personDao = PersonDao(dataSource)
        arbeidsgiverDao = ArbeidsgiverDao(dataSource)
        snapshotDao = SnapshotDao(dataSource)
        vedtakDao = VedtakDao(dataSource)
        oppgaveDao = OppgaveDao(dataSource)
    }

    @Test
    fun `henter saksbehandlerepost for tildeling med fødselsnummer`() {
        opprettTabeller()
        val saksbehandlerepost = sessionOf(dataSource).use {
            it.tildelingForPerson(FNR)
        }
        assertEquals(SAKSBEHANDLEREPOST, saksbehandlerepost)
    }

    private fun opprettTabeller() {
        val personinfoRef = personDao.insertPersoninfo(
            TildelingDaoTest.FORNAVN,
            TildelingDaoTest.MELLOMNAVN,
            TildelingDaoTest.ETTERNAVN,
            TildelingDaoTest.FØDSELSDATO,
            TildelingDaoTest.KJØNN
        )
        val utbetalingerRef = personDao.insertInfotrygdutbetalinger(TildelingDaoTest.objectMapper.createObjectNode())
        val personRef = personDao.insertPerson(
            TildelingDaoTest.FNR,
            TildelingDaoTest.AKTØR, personinfoRef, TildelingDaoTest.ENHET_OSLO.toInt(), utbetalingerRef
        ) ?: fail { "Kunne ikke opprette person" }
        val arbeidsgiverRef = arbeidsgiverDao.insertArbeidsgiver(TildelingDaoTest.ORGNR, TildelingDaoTest.ORGNAVN)
            ?: fail { "Kunne ikke opprette arbeidsgiver" }
        val snapshotRef = snapshotDao.insertSpeilSnapshot("{}")
        val vedtakId = vedtakDao.upsertVedtak(
            TildelingDaoTest.VEDTAKSPERIODE_ID,
            TildelingDaoTest.FOM,
            TildelingDaoTest.TOM,
            personRef.toInt(),
            arbeidsgiverRef.toInt(),
            snapshotRef.toInt()
        )
        oppgaveDao.insertOppgave(HENDELSE_ID, UUID.randomUUID(), "en oppgave", Oppgavestatus.AvventerSaksbehandler, null, SAKSBEHANDLER_OID, vedtakId)
        sessionOf(dataSource).use {
            it.persisterSaksbehandler(SAKSBEHANDLER_OID, "Sara Saksbehandler", SAKSBEHANDLEREPOST)
            it.tildelOppgave(HENDELSE_ID, SAKSBEHANDLER_OID)
        }
    }

}

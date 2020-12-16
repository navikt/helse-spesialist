import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.helse.mediator.meldinger.Kjønn
import no.nav.helse.modell.*
import no.nav.helse.modell.arbeidsforhold.ArbeidsforholdDao
import no.nav.helse.modell.abonnement.OpptegnelseDao
import no.nav.helse.modell.arbeidsgiver.ArbeidsgiverDao
import no.nav.helse.modell.automatisering.AutomatiseringDao
import no.nav.helse.modell.dkif.DigitalKontaktinformasjonDao
import no.nav.helse.modell.egenansatt.EgenAnsattDao
import no.nav.helse.modell.gosysoppgaver.ÅpneGosysOppgaverDao
import no.nav.helse.modell.kommando.TestHendelse
import no.nav.helse.modell.overstyring.OverstyringDao
import no.nav.helse.modell.person.PersonDao
import no.nav.helse.modell.risiko.RisikovurderingDao
import no.nav.helse.modell.saksbehandler.SaksbehandlerDao
import no.nav.helse.modell.tildeling.ReservasjonDao
import no.nav.helse.modell.tildeling.TildelingDao
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.fail
import java.time.LocalDate
import java.util.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class DatabaseIntegrationTest : AbstractDatabaseTest() {
    protected companion object {
        internal val objectMapper = jacksonObjectMapper()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .registerModule(JavaTimeModule())
        internal val HENDELSE_ID = UUID.randomUUID()

        internal val VEDTAKSPERIODE = UUID.randomUUID()

        internal const val OPPGAVETYPE = "EN OPPGAVE"
        internal val OPPGAVESTATUS = Oppgavestatus.AvventerSaksbehandler

        internal const val ORGNUMMER = "123456789"
        internal const val ORGNAVN = "NAVN AS"
        internal const val BRANSJER = "EN BRANSJE"

        internal const val FNR = "02345678911"
        internal const val AKTØR = "4321098765432"
        internal const val FORNAVN = "Kari"
        internal const val MELLOMNAVN = "Mellomnavn"
        internal const val ETTERNAVN = "Nordmann"
        internal val FØDSELSDATO = LocalDate.EPOCH
        internal val KJØNN = Kjønn.Kvinne
        internal const val ENHET = "0301"

        internal val FOM = LocalDate.of(2018, 1, 1)

        internal val TOM = LocalDate.of(2018, 1, 31)
        internal val SAKSBEHANDLER_OID = UUID.randomUUID()

        internal const val SAKSBEHANDLEREPOST = "sara.saksbehandler@nav.no"
    }

    internal var personId: Long = -1
        private set
    internal var arbeidsgiverId: Long = -1
        private set
    internal var snapshotId: Int = -1
        private set
    internal var vedtakId: Long = -1
        private set
    internal var oppgaveId: Long = -1
        private set

    internal val personDao = PersonDao(dataSource)
    internal val oppgaveDao = OppgaveDao(dataSource)
    internal val arbeidsforholdDao = ArbeidsforholdDao(dataSource)
    internal val arbeidsgiverDao = ArbeidsgiverDao(dataSource)
    internal val snapshotDao = SnapshotDao(dataSource)
    internal val vedtakDao = VedtakDao(dataSource)
    internal val warningDao = WarningDao(dataSource)
    internal val commandContextDao = CommandContextDao(dataSource)
    internal val tildelingDao = TildelingDao(dataSource)
    internal val saksbehandlerDao = SaksbehandlerDao(dataSource)
    internal val overstyringDao = OverstyringDao(dataSource)
    internal val reservasjonDao = ReservasjonDao(dataSource)
    internal val hendelseDao = HendelseDao(dataSource)
    internal val risikovurderingDao = RisikovurderingDao(dataSource)
    internal val automatiseringDao = AutomatiseringDao(dataSource)
    internal val digitalKontaktinformasjonDao = DigitalKontaktinformasjonDao(dataSource)
    internal val åpneGosysOppgaverDao = ÅpneGosysOppgaverDao(dataSource)
    internal val egenAnsattDao = EgenAnsattDao(dataSource)
    internal val opptegnelseDao = OpptegnelseDao(dataSource)

    internal fun testhendelse(
        hendelseId: UUID = HENDELSE_ID,
        vedtaksperiodeId: UUID? = VEDTAKSPERIODE,
        fødselsnummer: String = FNR,
        type: String = "GODKJENNING"
    ) = TestHendelse(hendelseId, vedtaksperiodeId, fødselsnummer).also {
        lagreHendelse(it.id, it.fødselsnummer(), type)
    }

    protected fun godkjenningsbehov(
        hendelseId: UUID,
        fødselsnummer: String = FNR,
        json: String = "{}"
    ) {
        lagreHendelse(hendelseId, fødselsnummer, "GODKJENNING", json)
    }

    private fun lagreHendelse(
        hendelseId: UUID,
        fødselsnummer: String = FNR,
        type: String,
        json: String = "{}"
    ) {
        using(sessionOf(dataSource)) {
            it.run(
                queryOf(
                    "INSERT INTO hendelse(id, fodselsnummer, data, type) VALUES(?, ?, ?::json, ?)",
                    hendelseId,
                    fødselsnummer.toLong(),
                    json,
                    type
                ).asExecute
            )
        }
    }

    protected fun nyttAutomatiseringsinnslag(automatisert: Boolean) {
        automatiseringDao.lagre(automatisert, if (automatisert) emptyList() else listOf("Dårlig ånde"), VEDTAKSPERIODE, HENDELSE_ID)
    }

    protected fun nyPerson() {
        opprettPerson()
        opprettArbeidsgiver()
        opprettVedtaksperiode()
        opprettOppgave(vedtakId = vedtakId)
    }

    protected fun opprettPerson(fødselsnummer: String = FNR, aktørId: String = AKTØR): Persondata {
        val personinfoId = personDao.insertPersoninfo(FORNAVN, MELLOMNAVN, ETTERNAVN, FØDSELSDATO, KJØNN)
        val infotrygdutbetalingerId = personDao.insertInfotrygdutbetalinger(objectMapper.createObjectNode())
        val enhetId = ENHET.toInt()
        personId = personDao.insertPerson(fødselsnummer, aktørId, personinfoId, enhetId, infotrygdutbetalingerId)
        return Persondata(
            personId = personId,
            personinfoId = personinfoId,
            enhetId = enhetId,
            infotrygdutbetalingerId = infotrygdutbetalingerId
        )
    }

    protected fun opprettSaksbehandler(
        saksbehandlerOID: UUID = SAKSBEHANDLER_OID,
        navn: String = "SAKSBEHANDLER SAKSBEHANDLERSEN",
        epost: String = "epost@nav.no"
    ) {
        saksbehandlerDao.opprettSaksbehandler(saksbehandlerOID, navn, epost)
    }

    protected fun opprettArbeidsgiver(
        organisasjonsnummer: String = ORGNUMMER,
        navn: String = ORGNAVN,
        bransjer: String = BRANSJER
    ): Long {
        return arbeidsgiverDao.insertArbeidsgiver(organisasjonsnummer, navn, bransjer)!!.also { arbeidsgiverId = it }
    }

    protected fun opprettSnapshot(personBlob: String = "{}") {
        snapshotId = snapshotDao.insertSpeilSnapshot(personBlob)
    }

    protected fun opprettVedtaksperiode(
        vedtaksperiodeId: UUID = VEDTAKSPERIODE,
        fom: LocalDate = FOM,
        tom: LocalDate = TOM
    ): Long {
        opprettSnapshot()
        return vedtakDao.opprett(vedtaksperiodeId, fom, tom, personId, arbeidsgiverId, snapshotId)
            .let { vedtakDao.finnVedtakId(vedtaksperiodeId) }
            ?.also { vedtakId = it }
            ?: fail { "Kunne ikke opprette vedtak" }
    }

    protected fun opprettOppgave(
        contextId: UUID = UUID.randomUUID(),
        vedtakId: Long? = null
    ) {
        oppgaveId = oppgaveDao.opprettOppgave(
            contextId,
            OPPGAVETYPE,
            vedtakId
        )
    }

    protected data class Persondata(
        val personId: Long,
        val personinfoId: Long,
        val enhetId: Int,
        val infotrygdutbetalingerId: Long
    )
}

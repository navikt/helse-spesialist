package no.nav.helse.modell

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.modell.dao.*
import no.nav.helse.modell.løsning.ArbeidsgiverLøsning
import no.nav.helse.modell.løsning.HentEnhetLøsning
import no.nav.helse.modell.løsning.HentPersoninfoLøsning
import no.nav.helse.modell.løsning.SaksbehandlerLøsning
import no.nav.helse.modell.oppgave.*
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.util.*
import kotlin.reflect.KClass

internal class Spleisbehov(
    internal val id: UUID,
    internal val fødselsnummer: String,
    internal val periodeFom: LocalDate,
    internal val periodeTom: LocalDate,
    internal val vedtaksperiodeId: UUID,
    internal val aktørId: String,
    internal val orgnummer: String,
    nåværendeOppgavenavn: String = OpprettPersonCommand::class.simpleName!!,
    personDao: PersonDao,
    arbeidsgiverDao: ArbeidsgiverDao,
    vedtakDao: VedtakDao,
    snapshotDao: SnapshotDao,
    speilSnapshotRestDao: SpeilSnapshotRestDao,
    oppgaveDao: OppgaveDao
) {
    private val log = LoggerFactory.getLogger(Spleisbehov::class.java)
    private var nåværendeOppgave: KClass<out Command>
    private val oppgaver: List<Command> = listOf(
        OpprettPersonCommand(this, personDao),
        OppdaterPersonCommand(this, personDao),
        OpprettArbeidsgiverCommand(this, arbeidsgiverDao),
        OppdatertArbeidsgiverCommand(this, arbeidsgiverDao),
        OpprettVedtakCommand(this, personDao, arbeidsgiverDao, vedtakDao, snapshotDao, speilSnapshotRestDao),
        SaksbehandlerGodkjenningCommand(this, oppgaveDao)
    )

    init {
        nåværendeOppgave = oppgaver.first { it::class.simpleName == nåværendeOppgavenavn }::class
    }

    private val behovstyper: MutableList<Behovtype> = mutableListOf()

    internal fun execute() {
        behovstyper.clear()
        oppgaver.asSequence().dropWhile { it::class != nåværendeOppgave }
            .onEach {
                nåværendeOppgave = it::class
                it.execute()
            }
            .takeWhile { it.ferdigstilt != null }
            .forEach {
                log.info("Oppgave ${it::class.simpleName} utført. Nåværende oppgave er ${nåværendeOppgave.simpleName}")
            }
    }

    internal fun håndter(behovtype: Behovtype) {
        behovstyper.add(behovtype)
    }

    internal fun fortsett(løsning: HentEnhetLøsning) {
        current().fortsett(løsning)
    }

    internal fun fortsett(løsning: HentPersoninfoLøsning) {
        current().fortsett(løsning)
    }

    fun fortsett(løsning: ArbeidsgiverLøsning) {
        current().fortsett(løsning)
    }

    fun fortsett(løsning: SaksbehandlerLøsning) {
        current().fortsett(løsning)
    }

    private fun current() = oppgaver.first { it::class == nåværendeOppgave }


    private fun behov() = behovstyper.takeIf { it.isNotEmpty() }?.let { typer ->
        Behov(
            typer = typer,
            fødselsnummer = fødselsnummer,
            orgnummer = orgnummer,
            spleisBehovId = id
        )
    }

    fun toJson() =
        jacksonObjectMapper().writeValueAsString(
            SpleisbehovDTO(
                id,
                fødselsnummer,
                periodeFom,
                periodeTom,
                vedtaksperiodeId,
                aktørId,
                orgnummer,
                nåværendeOppgave.simpleName!!
            )
        )

    companion object {
        fun restore(
            id: UUID, data: String,
            personDao: PersonDao,
            arbeidsgiverDao: ArbeidsgiverDao,
            vedtakDao: VedtakDao,
            snapshotDao: SnapshotDao,
            speilSnapshotRestDao: SpeilSnapshotRestDao,
            oppgaveDao: OppgaveDao
        ): Spleisbehov {
            val spleisbehovDTO = jacksonObjectMapper().readValue<SpleisbehovDTO>(data)
            return Spleisbehov(
                id = id,
                fødselsnummer = spleisbehovDTO.fødselsnummer,
                periodeFom = spleisbehovDTO.periodeFom,
                periodeTom = spleisbehovDTO.periodeTom,
                vedtaksperiodeId = spleisbehovDTO.vedtaksperiodeId,
                aktørId = spleisbehovDTO.aktørId,
                orgnummer = spleisbehovDTO.orgnummer,
                nåværendeOppgavenavn = spleisbehovDTO.oppgavenavn,
                personDao = personDao,
                arbeidsgiverDao = arbeidsgiverDao,
                vedtakDao = vedtakDao,
                snapshotDao = snapshotDao,
                speilSnapshotRestDao = speilSnapshotRestDao,
                oppgaveDao = oppgaveDao
            )
        }
    }
}

data class SpleisbehovDTO(
    val id: UUID,
    val fødselsnummer: String,
    val periodeFom: LocalDate,
    val periodeTom: LocalDate,
    val vedtaksperiodeId: UUID,
    val aktørId: String,
    val orgnummer: String,
    val oppgavenavn: String
)

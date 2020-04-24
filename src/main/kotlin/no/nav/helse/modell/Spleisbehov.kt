package no.nav.helse.modell

import com.fasterxml.jackson.module.kotlin.readValue
import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.helse.Oppgavestatus
import no.nav.helse.modell.dao.ArbeidsgiverDao
import no.nav.helse.modell.dao.OppgaveDao
import no.nav.helse.modell.dao.PersonDao
import no.nav.helse.modell.dao.SnapshotDao
import no.nav.helse.modell.dao.SpeilSnapshotRestDao
import no.nav.helse.modell.dao.VedtakDao
import no.nav.helse.modell.dto.OppgaveDto
import no.nav.helse.modell.løsning.ArbeidsgiverLøsning
import no.nav.helse.modell.løsning.HentEnhetLøsning
import no.nav.helse.modell.løsning.HentPersoninfoLøsning
import no.nav.helse.modell.løsning.SaksbehandlerLøsning
import no.nav.helse.modell.oppgave.Command
import no.nav.helse.modell.oppgave.OppdaterPersonCommand
import no.nav.helse.modell.oppgave.OppdatertArbeidsgiverCommand
import no.nav.helse.modell.oppgave.OpprettArbeidsgiverCommand
import no.nav.helse.modell.oppgave.OpprettPersonCommand
import no.nav.helse.modell.oppgave.OpprettVedtakCommand
import no.nav.helse.modell.oppgave.SaksbehandlerGodkjenningCommand
import no.nav.helse.objectMapper
import java.time.Duration
import java.time.LocalDate
import java.util.UUID

internal class Spleisbehov(
    private val id: UUID,
    internal val fødselsnummer: String,
    private val periodeFom: LocalDate,
    private val periodeTom: LocalDate,
    internal val vedtaksperiodeId: UUID,
    private val aktørId: String,
    private val orgnummer: String,
    nåværendeOppgave: OppgaveDto?,
    internal var vedtaksperiodeReferanse: Int?,
    personDao: PersonDao,
    arbeidsgiverDao: ArbeidsgiverDao,
    vedtakDao: VedtakDao,
    snapshotDao: SnapshotDao,
    speilSnapshotRestDao: SpeilSnapshotRestDao,
    private val oppgaveDao: OppgaveDao
) : Command(id, Oppgavestatus.AvventerSystem, null, Duration.ofDays(14)) {
    override val oppgaver = setOf(
        OpprettPersonCommand(this, personDao, fødselsnummer, aktørId, id, this),
        OppdaterPersonCommand(this, personDao, fødselsnummer, id, this),
        OpprettArbeidsgiverCommand(this, arbeidsgiverDao, orgnummer, id, this),
        OppdatertArbeidsgiverCommand(this, arbeidsgiverDao, orgnummer, id, this),
        OpprettVedtakCommand(
            personDao,
            arbeidsgiverDao,
            vedtakDao,
            snapshotDao,
            speilSnapshotRestDao,
            fødselsnummer,
            orgnummer,
            vedtaksperiodeId,
            periodeFom,
            periodeTom,
            id,
            this
        ),
        SaksbehandlerGodkjenningCommand(id, this)
    )
    private var nåværendeOppgavetype = nåværendeOppgave?.oppgaveType ?: oppgaver.first().oppgavetype

    private val behovstyper: MutableList<Behovtype> = mutableListOf()
    private var løsning: Map<String, Any>? = null

    override fun execute() {
        val førsteOppgave = current()
        behovstyper.clear()
        try {
            oppgaver.asSequence()
                .dropWhile { it.oppgavetype != nåværendeOppgavetype }
                .onEach(Command::execute)
                .onEach { nåværendeOppgavetype = it.oppgavetype }
                .takeWhile { !it.trengerExecute() }
                .forEach {
                    log.info(
                        "Oppgave ${it::class.simpleName} ble executed. Nåværende oppgave er $nåværendeOppgavetype, {}, {}",
                        keyValue("vedtaksperiodeId", vedtaksperiodeId),
                        keyValue("behovId", id)
                    )
                    it.persisterEndring(oppgaveDao)
                }
        } finally {
            current().persister(oppgaveDao, vedtaksperiodeReferanse)
        }

        log.info("Oppgaver utført, gikk fra ${førsteOppgave.oppgavetype} til ${current().oppgavetype}, {}, {}",
            keyValue("vedtaksperiodeId", vedtaksperiodeId),
            keyValue("behovId", id))
    }

    internal fun håndter(behovtype: Behovtype) {
        behovstyper.add(behovtype)
    }

    override fun fortsett(løsning: HentEnhetLøsning) {
        current().fortsett(løsning)
    }

    override fun fortsett(løsning: HentPersoninfoLøsning) {
        current().fortsett(løsning)
    }

    override fun fortsett(løsning: ArbeidsgiverLøsning) {
        current().fortsett(løsning)
    }

    override fun fortsett(løsning: SaksbehandlerLøsning) {
        current().fortsett(løsning)
        this.løsning = mapOf(
            "Godkjenning" to mapOf(
                "godkjent" to løsning.godkjent,
                "saksbehandlerIdent" to løsning.saksbehandlerIdent,
                "godkjenttidspunkt" to løsning.godkjenttidspunkt
            )
        )
    }

    private fun current() = oppgaver.first { it.oppgavetype == nåværendeOppgavetype }


    internal fun behov() = behovstyper.takeIf { it.isNotEmpty() }?.let { typer ->
        Behov(
            typer = typer,
            fødselsnummer = fødselsnummer,
            orgnummer = orgnummer,
            spleisBehovId = id,
            vedtaksperiodeId = vedtaksperiodeId
        )
    }

    fun currentTimeout() = current().timeout

    fun ferdigstilt() = !current().trengerExecute()

    internal fun løsning(): Map<String, Any>? = løsning

    fun toJson() =
        objectMapper.writeValueAsString(
            SpleisbehovDTO(
                id,
                fødselsnummer,
                periodeFom,
                periodeTom,
                aktørId,
                orgnummer,
                vedtaksperiodeReferanse
            )
        )

    companion object {
        fun restore(
            id: UUID,
            vedtaksperiodeId: UUID,
            data: String,
            personDao: PersonDao,
            arbeidsgiverDao: ArbeidsgiverDao,
            vedtakDao: VedtakDao,
            snapshotDao: SnapshotDao,
            speilSnapshotRestDao: SpeilSnapshotRestDao,
            oppgaveDao: OppgaveDao,
            nåværendeOppgave: OppgaveDto
        ): Spleisbehov {
            val spleisbehovDTO = objectMapper.readValue<SpleisbehovDTO>(data)
            return Spleisbehov(
                id = id,
                vedtaksperiodeId = vedtaksperiodeId,
                fødselsnummer = spleisbehovDTO.fødselsnummer,
                periodeFom = spleisbehovDTO.periodeFom,
                periodeTom = spleisbehovDTO.periodeTom,
                aktørId = spleisbehovDTO.aktørId,
                orgnummer = spleisbehovDTO.orgnummer,
                personDao = personDao,
                vedtaksperiodeReferanse = spleisbehovDTO.vedtakRef,
                arbeidsgiverDao = arbeidsgiverDao,
                vedtakDao = vedtakDao,
                snapshotDao = snapshotDao,
                speilSnapshotRestDao = speilSnapshotRestDao,
                oppgaveDao = oppgaveDao,
                nåværendeOppgave = nåværendeOppgave
            )
        }
    }
}

data class SpleisbehovDTO(
    val id: UUID,
    val fødselsnummer: String,
    val periodeFom: LocalDate,
    val periodeTom: LocalDate,
    val aktørId: String,
    val orgnummer: String,
    val vedtakRef: Int?
)

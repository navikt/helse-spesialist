package no.nav.helse.modell

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.modell.dao.*
import no.nav.helse.modell.oppgave.*
import no.nav.helse.objectMapper
import java.time.Duration
import java.time.LocalDate
import java.util.*

internal class Godkjenningsbehov(
    private val id: UUID,
    private val periodeFom: LocalDate,
    private val periodeTom: LocalDate,
    private val aktørId: String,
    override val fødselsnummer: String,
    override val orgnummer: String,
    override val vedtaksperiodeId: UUID,
    personDao: PersonDao,
    arbeidsgiverDao: ArbeidsgiverDao,
    vedtakDao: VedtakDao,
    snapshotDao: SnapshotDao,
    speilSnapshotRestDao: SpeilSnapshotRestDao
) : RootCommand(
    behovId = id,
    timeout = Duration.ofDays(14)
) {
    override val oppgaver = setOf(
        OpprettPersonCommand(personDao, fødselsnummer, aktørId, id, this),
        OppdaterPersonCommand(personDao, fødselsnummer, id, this),
        OpprettArbeidsgiverCommand(arbeidsgiverDao, orgnummer, id, this),
        OppdatertArbeidsgiverCommand(arbeidsgiverDao, orgnummer, id, this),
        OpprettVedtakCommand(
            personDao = personDao,
            arbeidsgiverDao = arbeidsgiverDao,
            vedtakDao = vedtakDao,
            snapshotDao = snapshotDao,
            speilSnapshotRestDao = speilSnapshotRestDao,
            fødselsnummer = fødselsnummer,
            orgnummer = orgnummer,
            vedtaksperiodeId = vedtaksperiodeId,
            periodeFom = periodeFom,
            periodeTom = periodeTom,
            behovId = id,
            parent = this
        ),
        SaksbehandlerGodkjenningCommand(id, this)
    )

    override fun execute(): Resultat = Resultat.Ok.System

    override fun toJson() =
        objectMapper.writeValueAsString(
            SpleisbehovDTO(
                id = id,
                fødselsnummer = fødselsnummer,
                periodeFom = periodeFom,
                periodeTom = periodeTom,
                aktørId = aktørId,
                orgnummer = orgnummer
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
            speilSnapshotRestDao: SpeilSnapshotRestDao
        ): Godkjenningsbehov {
            val spleisbehovDTO = objectMapper.readValue<SpleisbehovDTO>(data)
            return Godkjenningsbehov(
                id = id,
                vedtaksperiodeId = vedtaksperiodeId,
                fødselsnummer = spleisbehovDTO.fødselsnummer,
                periodeFom = spleisbehovDTO.periodeFom,
                periodeTom = spleisbehovDTO.periodeTom,
                aktørId = spleisbehovDTO.aktørId,
                orgnummer = spleisbehovDTO.orgnummer,
                personDao = personDao,
                arbeidsgiverDao = arbeidsgiverDao,
                vedtakDao = vedtakDao,
                snapshotDao = snapshotDao,
                speilSnapshotRestDao = speilSnapshotRestDao
            )
        }
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class SpleisbehovDTO(
    val id: UUID,
    val fødselsnummer: String,
    val periodeFom: LocalDate,
    val periodeTom: LocalDate,
    val aktørId: String,
    val orgnummer: String
)

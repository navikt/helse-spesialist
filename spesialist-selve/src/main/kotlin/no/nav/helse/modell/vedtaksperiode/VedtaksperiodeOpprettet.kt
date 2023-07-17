package no.nav.helse.modell.vedtaksperiode

import java.time.LocalDate
import java.util.UUID
import no.nav.helse.mediator.meldinger.Hendelse
import no.nav.helse.modell.VedtakDao
import no.nav.helse.modell.arbeidsgiver.ArbeidsgiverDao
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.MacroCommand
import no.nav.helse.modell.kommando.OpprettFørsteVedtaksperiodeGenerasjonCommand
import no.nav.helse.modell.kommando.OpprettMinimaltVedtakCommand
import no.nav.helse.modell.person.PersonDao

internal class VedtaksperiodeOpprettet(
    override val id: UUID,
    private val fødselsnummer: String,
    organisasjonsnummer: String,
    vedtaksperiodeId: UUID,
    fom: LocalDate,
    tom: LocalDate,
    personDao: PersonDao,
    arbeidsgiverDao: ArbeidsgiverDao,
    vedtakDao: VedtakDao,
    generasjon: Generasjon,
    private val json: String,
) : Hendelse, MacroCommand() {

    override fun fødselsnummer() = fødselsnummer
    override fun toJson(): String = json

    override val commands: List<Command> = listOf(
        OpprettMinimaltVedtakCommand(fødselsnummer, organisasjonsnummer, vedtaksperiodeId, fom, tom, personDao, arbeidsgiverDao, vedtakDao),
        OpprettFørsteVedtaksperiodeGenerasjonCommand(hendelseId = id, generasjon = generasjon)
    )

}

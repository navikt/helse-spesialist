package no.nav.helse.modell.vedtaksperiode

import java.time.LocalDate
import java.util.UUID
import no.nav.helse.mediator.meldinger.Vedtaksperiodemelding
import no.nav.helse.modell.VedtakDao
import no.nav.helse.modell.arbeidsgiver.ArbeidsgiverDao
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.MacroCommand
import no.nav.helse.modell.kommando.OpprettFørsteVedtaksperiodeGenerasjonCommand
import no.nav.helse.modell.kommando.OpprettMinimaltVedtakCommand
import no.nav.helse.modell.person.PersonDao
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.asLocalDate

internal class VedtaksperiodeOpprettet internal constructor(
    override val id: UUID,
    private val fødselsnummer: String,
    private val vedtaksperiodeId: UUID,
    internal val organisasjonsnummer: String,
    internal val fom: LocalDate,
    internal val tom: LocalDate,
    internal val skjæringstidspunkt: LocalDate,
    private val json: String,
) : Vedtaksperiodemelding {

    internal constructor(packet: JsonMessage): this(
        id = UUID.fromString(packet["@id"].asText()),
        fødselsnummer = packet["fødselsnummer"].asText(),
        vedtaksperiodeId = UUID.fromString(packet["vedtaksperiodeId"].asText()),
        organisasjonsnummer = packet["organisasjonsnummer"].asText(),
        fom = packet["fom"].asLocalDate(),
        tom = packet["tom"].asLocalDate(),
        skjæringstidspunkt = packet["skjæringstidspunkt"].asLocalDate(),
        json = packet.toJson()
    )

    override fun fødselsnummer() = fødselsnummer
    override fun toJson(): String = json
    override fun vedtaksperiodeId(): UUID = vedtaksperiodeId
}

internal class OpprettVedtaksperiodeCommand(
    id: UUID,
    generasjon: Generasjon,
    fødselsnummer: String,
    organisasjonsnummer: String,
    vedtaksperiodeId: UUID,
    fom: LocalDate,
    tom: LocalDate,
    personDao: PersonDao,
    arbeidsgiverDao: ArbeidsgiverDao,
    vedtakDao: VedtakDao
): MacroCommand() {
    override val commands: List<Command> = listOf(
        OpprettMinimaltVedtakCommand(fødselsnummer, organisasjonsnummer, vedtaksperiodeId, fom, tom, personDao, arbeidsgiverDao, vedtakDao),
        OpprettFørsteVedtaksperiodeGenerasjonCommand(hendelseId = id, generasjon = generasjon)
    )
}


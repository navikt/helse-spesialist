package no.nav.helse.modell.vedtaksperiode

import java.time.LocalDate
import java.util.UUID
import no.nav.helse.mediator.Kommandofabrikk
import no.nav.helse.mediator.asUUID
import no.nav.helse.mediator.meldinger.VedtaksperiodemeldingOld
import no.nav.helse.modell.person.Person
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.asLocalDate

data class SpleisBehandling(
    val organisasjonsnummer: String,
    val vedtaksperiodeId: UUID,
    val spleisBehandlingId: UUID,
    val fom: LocalDate,
    val tom: LocalDate
) {
    internal fun erRelevantFor(vedtaksperiodeId: UUID) = this.vedtaksperiodeId == vedtaksperiodeId
}

internal class BehandlingOpprettet private constructor(
    override val id: UUID,
    private val fødselsnummer: String,
    private val organisasjonsnummer: String,
    private val vedtaksperiodeId: UUID,
    private val spleisBehandlingId: UUID,
    private val fom: LocalDate,
    private val tom: LocalDate,
    private val json: String
): VedtaksperiodemeldingOld {

    internal constructor(packet: JsonMessage): this(
        id = packet["@id"].asUUID(),
        fødselsnummer = packet["fødselsnummer"].asText(),
        organisasjonsnummer = packet["organisasjonsnummer"].asText(),
        vedtaksperiodeId = packet["vedtaksperiodeId"].asUUID(),
        spleisBehandlingId = packet["behandlingId"].asUUID(),
        fom = packet["fom"].asLocalDate(),
        tom = packet["tom"].asLocalDate(),
        json = packet.toJson()
    )

    override fun behandle(person: Person, kommandofabrikk: Kommandofabrikk) {
        person.nySpleisBehandling(SpleisBehandling(organisasjonsnummer, vedtaksperiodeId, spleisBehandlingId, fom, tom))
    }

    override fun fødselsnummer(): String = fødselsnummer
    override fun vedtaksperiodeId(): UUID = vedtaksperiodeId
    override fun toJson(): String = json
}
package no.nav.helse.modell.vedtaksperiode

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.db.SessionContext
import no.nav.helse.mediator.Kommandostarter
import no.nav.helse.mediator.asUUID
import no.nav.helse.mediator.meldinger.Vedtaksperiodemelding
import no.nav.helse.modell.person.Person
import no.nav.helse.modell.person.vedtaksperiode.SpleisBehandling
import java.time.LocalDate
import java.util.UUID

class BehandlingOpprettet(
    override val id: UUID,
    private val fødselsnummer: String,
    private val organisasjonsnummer: String,
    private val vedtaksperiodeId: UUID,
    private val spleisBehandlingId: UUID,
    private val fom: LocalDate,
    private val tom: LocalDate,
    private val json: String,
) : Vedtaksperiodemelding {
    constructor(jsonNode: JsonNode) : this(
        id = jsonNode["@id"].asUUID(),
        fødselsnummer = jsonNode["fødselsnummer"].asText(),
        organisasjonsnummer = jsonNode["organisasjonsnummer"].asText(),
        vedtaksperiodeId = jsonNode["vedtaksperiodeId"].asUUID(),
        spleisBehandlingId = jsonNode["behandlingId"].asUUID(),
        fom = jsonNode["fom"].asText().let(LocalDate::parse),
        tom = jsonNode["tom"].asText().let(LocalDate::parse),
        json = jsonNode.toString(),
    )

    override fun behandle(
        person: Person,
        kommandostarter: Kommandostarter,
        sessionContext: SessionContext,
        syncPersonTilDatabase: () -> Unit,
    ) {
        person.nySpleisBehandling(SpleisBehandling(organisasjonsnummer, vedtaksperiodeId, spleisBehandlingId, fom, tom))
        kommandostarter {
            opprettArbeidsgiver(organisasjonsnummer, sessionContext)
        }
    }

    override fun fødselsnummer(): String = fødselsnummer

    override fun vedtaksperiodeId(): UUID = vedtaksperiodeId

    override fun toJson(): String = json
}

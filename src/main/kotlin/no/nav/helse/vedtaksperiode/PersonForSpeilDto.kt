package no.nav.helse.vedtaksperiode

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.mediator.kafka.meldinger.Dagtype
import no.nav.helse.modell.vedtak.EnhetDto
import no.nav.helse.modell.vedtak.PersoninfoDto
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*


data class PersonForSpeilDto(
    val aktørId: String,
    val fødselsnummer: String,
    val personinfo: PersoninfoDto,
    val arbeidsgivere: List<ArbeidsgiverForSpeilDto>,
    val infotrygdutbetalinger: JsonNode?,
    val enhet: EnhetDto
)

data class ArbeidsgiverForSpeilDto(
    val organisasjonsnummer: String,
    val navn: String,
    val id: UUID,
    val vedtaksperioder: List<JsonNode>,
    val overstyringer: List<OverstyringForSpeilDto>
)

data class OverstyringForSpeilDto(
    val hendelseId: UUID,
    val begrunnelse: String,
    val timestamp: LocalDateTime,
    val overstyrteDager: List<OverstyringDagForSpeilDto>,
    val saksbehandlerNavn: String
)

data class OverstyringDagForSpeilDto(
    val dato: LocalDate,
    val dagtype: Dagtype,
    val grad: Int?
)

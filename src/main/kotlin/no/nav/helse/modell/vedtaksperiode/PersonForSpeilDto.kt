package no.nav.helse.modell.vedtaksperiode

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.modell.overstyring.Dagtype
import no.nav.helse.modell.vedtak.EnhetDto
import no.nav.helse.modell.vedtak.PersoninfoDto
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*


data class PersonForSpeilDto(
    val utbetalinger: List<UtbetalingForSpeilDto>,
    val aktørId: String,
    val fødselsnummer: String,
    val personinfo: PersoninfoDto,
    val arbeidsgivere: List<ArbeidsgiverForSpeilDto>,
    val infotrygdutbetalinger: JsonNode?,
    val enhet: EnhetDto,
    val saksbehandlerepost: String?,
    val arbeidsforhold: List<ArbeidsforholdForSpeilDto>,
    val inntektsgrunnlag: JsonNode
)

data class ArbeidsforholdForSpeilDto(
    val organisasjonsnummer: String,
    val stillingstittel: String,
    val stillingsprosent: Int,
    val startdato: LocalDate,
    val sluttdato: LocalDate?
)

data class ArbeidsgiverForSpeilDto(
    val organisasjonsnummer: String,
    val navn: String,
    val id: UUID,
    val vedtaksperioder: List<JsonNode>,
    val overstyringer: List<OverstyringForSpeilDto>,
    val bransjer: List<String>
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

data class RisikovurderingForSpeilDto(
    val funn: List<JsonNode>,
    val kontrollertOk: List<JsonNode>,
) {
    val arbeidsuførhetvurdering: List<String> = funn.map { it["beskrivelse"].asText() }
    val ufullstendig: Boolean = false
}

data class UtbetalingForSpeilDto(
    val type: String,
    val status: String,
    val arbeidsgiverOppdrag: OppdragForSpeilDto
)

data class OppdragForSpeilDto(
    val organisasjonsnummer: String,
    val fagsystemId: String,
    val utbetalingslinjer: List<UtbetalingslinjeForSpeilDto>
)

data class UtbetalingslinjeForSpeilDto(
    val fom: LocalDate,
    val tom: LocalDate
)

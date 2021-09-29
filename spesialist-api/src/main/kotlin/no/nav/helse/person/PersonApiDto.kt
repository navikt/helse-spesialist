package no.nav.helse.person

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.arbeidsgiver.ArbeidsforholdApiDto
import no.nav.helse.arbeidsgiver.ArbeidsgiverApiDto
import no.nav.helse.tildeling.TildelingApiDto
import no.nav.helse.utbetaling.UtbetalingApiDto
import no.nav.helse.vedtaksperiode.EnhetDto
import java.time.LocalDate

data class PersoninfoApiDto(
    val fornavn: String,
    val mellomnavn: String?,
    val etternavn: String,
    val fødselsdato: LocalDate?,
    val kjønn: Kjønn?
)

data class PersonMetadataApiDto(
    val fødselsnummer: String,
    val aktørId: String,
    val personinfo: PersoninfoApiDto
)

enum class Kjønn { Mann, Kvinne, Ukjent }

data class PersonForSpeilDto(
    val utbetalinger: List<UtbetalingApiDto>,
    val aktørId: String,
    val fødselsnummer: String,
    val dødsdato: LocalDate?,
    val personinfo: PersoninfoApiDto,
    val arbeidsgivere: List<ArbeidsgiverApiDto>,
    val infotrygdutbetalinger: JsonNode?,
    val enhet: EnhetDto,
    val arbeidsforhold: List<ArbeidsforholdApiDto>,
    val inntektsgrunnlag: JsonNode,
    val vilkårsgrunnlagHistorikk: JsonNode?,
    val tildeling: TildelingApiDto?
)

package no.nav.helse.spesialist.api.person

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.spesialist.api.arbeidsgiver.ArbeidsforholdApiDto
import no.nav.helse.spesialist.api.arbeidsgiver.ArbeidsgiverApiDto
import no.nav.helse.spesialist.api.tildeling.TildelingApiDto
import no.nav.helse.spesialist.api.utbetaling.UtbetalingApiDto
import no.nav.helse.spesialist.api.vedtaksperiode.EnhetDto
import java.time.LocalDate

data class PersoninfoApiDto(
    val fornavn: String,
    val mellomnavn: String?,
    val etternavn: String,
    val fødselsdato: LocalDate?,
    val kjønn: Kjønn?,
    val adressebeskyttelse: Adressebeskyttelse
)

data class PersonMetadataApiDto(
    val fødselsnummer: String,
    val aktørId: String,
    val personinfo: PersoninfoApiDto
)

enum class Kjønn { Mann, Kvinne, Ukjent }

enum class Adressebeskyttelse {
    Ugradert,
    Fortrolig,
    StrengtFortrolig,
    StrengtFortroligUtland,
    Ukjent
}

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

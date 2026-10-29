package no.nav.helse.spesialist.api.rest.personer

import io.ktor.http.HttpStatusCode
import no.nav.helse.mediator.meldinger.løsninger.Inntekter
import no.nav.helse.spesialist.api.rest.ApiErrorCode
import no.nav.helse.spesialist.api.rest.ApiInntekterFraAordningenForArbeidsgiver
import no.nav.helse.spesialist.api.rest.GetBehandler
import no.nav.helse.spesialist.api.rest.KallKontekst
import no.nav.helse.spesialist.api.rest.RestResponse
import no.nav.helse.spesialist.api.rest.Tags
import no.nav.helse.spesialist.api.rest.resources.Personer
import no.nav.helse.spesialist.api.rest.saksbilde.ApiInntektFraAOrdningen
import no.nav.helse.spesialist.application.PersonPseudoId
import java.time.LocalDate

class GetInntekterFraAordningenForPersonBehandler : GetBehandler<Personer.PersonPseudoId.InntekterFraAordningen, List<ApiInntekterFraAordningenForArbeidsgiver>, ApiGetInntekterFraAordningenForPersonErrorCode> {
    override val tag = Tags.PERSONER

    override fun behandle(
        resource: Personer.PersonPseudoId.InntekterFraAordningen,
        kallKontekst: KallKontekst,
    ): RestResponse<List<ApiInntekterFraAordningenForArbeidsgiver>, ApiGetInntekterFraAordningenForPersonErrorCode> =
        kallKontekst.medPerson(
            personPseudoId = PersonPseudoId.fraUUID(resource.parent.pseudoId),
            personPseudoIdIkkeFunnet = { ApiGetInntekterFraAordningenForPersonErrorCode.PERSON_PSEUDO_ID_IKKE_FUNNET },
            manglerTilgangTilPerson = { ApiGetInntekterFraAordningenForPersonErrorCode.MANGLER_TILGANG_TIL_PERSON },
        ) { person ->
            RestResponse.OK(
                kallKontekst.transaksjon.personDao
                    .finnInntekterFraAordningen(person.id.value)
                    .flatMap { (skjæringstidspunkt, inntekter) -> inntekter.perArbeidsgiver(skjæringstidspunkt) },
            )
        }
}

internal fun List<Inntekter>.perArbeidsgiver(skjæringstidspunkt: LocalDate): List<ApiInntekterFraAordningenForArbeidsgiver> =
    flatMap { måned -> måned.inntektsliste.map { it.orgnummer } }
        .distinct()
        .map { organisasjonsnummer ->
            ApiInntekterFraAordningenForArbeidsgiver(
                organisasjonsnummer = organisasjonsnummer,
                skjaeringstidspunkt = skjæringstidspunkt,
                inntekter =
                    mapNotNull { måned ->
                        måned.inntektsliste
                            .filter { it.orgnummer == organisasjonsnummer }
                            .takeUnless { it.isEmpty() }
                            ?.let { ApiInntektFraAOrdningen(maned = måned.årMåned, sum = it.sumOf { inntekt -> inntekt.beløp }) }
                    },
            )
        }

enum class ApiGetInntekterFraAordningenForPersonErrorCode(
    override val title: String,
    override val statusCode: HttpStatusCode,
) : ApiErrorCode {
    PERSON_PSEUDO_ID_IKKE_FUNNET("PersonPseudoId har utløpt (eller aldri eksistert)", HttpStatusCode.NotFound),
    MANGLER_TILGANG_TIL_PERSON("Mangler tilgang til person", HttpStatusCode.Forbidden),
}

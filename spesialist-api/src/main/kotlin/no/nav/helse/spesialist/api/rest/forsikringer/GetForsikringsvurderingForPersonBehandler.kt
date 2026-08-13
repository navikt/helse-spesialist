package no.nav.helse.spesialist.api.rest.forsikringer

import io.ktor.http.HttpStatusCode
import no.nav.helse.spesialist.api.rest.ApiEkskluderingsbegrunnelse
import no.nav.helse.spesialist.api.rest.ApiEkskluderingsårsak
import no.nav.helse.spesialist.api.rest.ApiEkskludertForsikring
import no.nav.helse.spesialist.api.rest.ApiErrorCode
import no.nav.helse.spesialist.api.rest.ApiFolketrygdlovenreferanse
import no.nav.helse.spesialist.api.rest.ApiForsikring
import no.nav.helse.spesialist.api.rest.ApiForsikringsvurdering
import no.nav.helse.spesialist.api.rest.ForsikringInnhold
import no.nav.helse.spesialist.api.rest.GetBehandler
import no.nav.helse.spesialist.api.rest.KallKontekst
import no.nav.helse.spesialist.api.rest.RestResponse
import no.nav.helse.spesialist.api.rest.Tags
import no.nav.helse.spesialist.api.rest.resources.Personer
import no.nav.helse.spesialist.application.Ekskluderingsårsak
import no.nav.helse.spesialist.application.EkskludertForsikring
import no.nav.helse.spesialist.application.Folketrygdlovenreferanse
import no.nav.helse.spesialist.application.Forsikring
import no.nav.helse.spesialist.application.Forsikringsvurdering
import no.nav.helse.spesialist.application.ForsikringsvurderingHenter
import no.nav.helse.spesialist.application.PersonPseudoId
import no.nav.helse.spesialist.application.logg.loggWarn
import no.nav.helse.spesialist.domain.ForsikringsvurderingId
import no.nav.helse.spesialist.domain.Person
import java.util.UUID

class GetForsikringsvurderingForPersonBehandler(
    private val forsikringsvurderingHenter: ForsikringsvurderingHenter,
) : GetBehandler<Personer.PersonPseudoId.Forsikringsvurderinger.ForsikringvurderingId, ApiForsikringsvurdering, ApiGetForsikringsvurderingForPersonErrorCode> {
    override val tag = Tags.FORSIKRINGER

    override fun behandle(
        resource: Personer.PersonPseudoId.Forsikringsvurderinger.ForsikringvurderingId,
        kallKontekst: KallKontekst,
    ): RestResponse<ApiForsikringsvurdering, ApiGetForsikringsvurderingForPersonErrorCode> =
        kallKontekst.medPerson(
            personPseudoId = PersonPseudoId.fraString(resource.parent.parent.pseudoId),
            personPseudoIdIkkeFunnet = { ApiGetForsikringsvurderingForPersonErrorCode.PERSON_PSEUDO_ID_IKKE_FUNNET },
            manglerTilgangTilPerson = { ApiGetForsikringsvurderingForPersonErrorCode.MANGLER_TILGANG_TIL_PERSON },
        ) { person ->
            behandleForPerson(person, resource.forsikringvurderingId)
        }

    private fun behandleForPerson(
        person: Person,
        forsikringsvurderingId: UUID,
    ): RestResponse<ApiForsikringsvurdering, ApiGetForsikringsvurderingForPersonErrorCode> {
        val forsikringsvurdering =
            runCatching {
                forsikringsvurderingHenter.hent(ForsikringsvurderingId(forsikringsvurderingId))
            }.getOrElse { throwable ->
                loggWarn("Feil ved videre kall", throwable)
                return RestResponse.Error(ApiGetForsikringsvurderingForPersonErrorCode.FEIL_VED_VIDERE_KALL)
            }

        if (forsikringsvurdering == null || forsikringsvurdering.identitetsnummer != person.id) {
            return RestResponse.Error(ApiGetForsikringsvurderingForPersonErrorCode.FORSIKRINGSVURDERING_IKKE_FUNNET)
        }

        return RestResponse.OK(forsikringsvurdering.tilApiForsikringsvurdering())
    }
}

fun EkskludertForsikring.tilApiEkskludertForsikring(): ApiEkskludertForsikring =
    ApiEkskludertForsikring(
        virkningsdato = virkningsdato,
        opphørsdato = opphørsdato,
        dekningsgrad = dekningsgrad,
        dekningIVentetid = dekningIVentetid,
        navn = navn,
        folketrygdlovenreferanse = folketrygdlovenreferanse.tilApiFolketrygdlovenreferanse(),
        ekskluderingsårsak =
            when (ekskluderingsårsak) {
                Ekskluderingsårsak.SKJÆRINGSTIDSPUNKT_INNEN_28_DAGER_FØR_VIRKNINGSDATO -> ApiEkskluderingsårsak.SKJÆRINGSTIDSPUNKT_INNEN_28_DAGER_FØR_VIRKNINGSDATO
                Ekskluderingsårsak.SKJÆRINGSTIDSPUNKT_MER_ENN_28_DAGER_FØR_VIRKNINGSDATO -> ApiEkskluderingsårsak.SKJÆRINGSTIDSPUNKT_MER_ENN_28_DAGER_FØR_VIRKNINGSDATO
                Ekskluderingsårsak.OPPHØRT_PÅ_SKJÆRINGSTIDSPUNKT -> ApiEkskluderingsårsak.OPPHØRT_PÅ_SKJÆRINGSTIDSPUNKT
                Ekskluderingsårsak.ALDRI_BETALT -> ApiEkskluderingsårsak.ALDRI_BETALT
            },
        ekskluderingsbegrunnelse =
            ApiEkskluderingsbegrunnelse(
                forklaring = ekskluderingsbegrunnelse.forklaring,
                folketrygdlovenreferanse = ekskluderingsbegrunnelse.folketrygdlovenreferanse?.tilApiFolketrygdlovenreferanse(),
            ),
    )

fun Forsikring.tilApiForsikring(): ApiForsikring =
    ApiForsikring(
        virkningsdato = virkningsdato,
        opphørsdato = opphørsdato,
        dekningsgrad = dekningsgrad,
        dekningIVentetid = dekningIVentetid,
        navn = navn,
        folketrygdlovenreferanse = folketrygdlovenreferanse.tilApiFolketrygdlovenreferanse(),
    )

private fun Folketrygdlovenreferanse.tilApiFolketrygdlovenreferanse(): ApiFolketrygdlovenreferanse =
    ApiFolketrygdlovenreferanse(
        kapittel = kapittel,
        paragrafIKapittel = paragrafIKapittel,
        ledd = ledd,
        bokstav = bokstav,
    )

private fun Forsikringsvurdering.tilApiForsikringsvurdering(): ApiForsikringsvurdering =
    ApiForsikringsvurdering(
        eksisterer = harForsikring,
        forsikringInnhold =
            dekning?.let {
                ForsikringInnhold(
                    dekningsgrad = it.grad,
                    gjelderFraDag = it.fraDag,
                )
            },
        ekskluderteForsikringer = ekskluderteForsikringer.map { it.tilApiEkskludertForsikring() },
        gjeldendeForsikring = gjeldendeForsikring?.tilApiForsikring(),
        dataHentetTidspunkt = dataHentetTidspunkt,
    )

enum class ApiGetForsikringsvurderingForPersonErrorCode(
    override val title: String,
    override val statusCode: HttpStatusCode,
) : ApiErrorCode {
    PERSON_PSEUDO_ID_IKKE_FUNNET("PersonPseudoId har utløpt (eller aldri eksistert)", HttpStatusCode.NotFound),
    MANGLER_TILGANG_TIL_PERSON("Mangler tilgang til person", HttpStatusCode.Forbidden),
    FORSIKRINGSVURDERING_IKKE_FUNNET("Forsikringsvurderingen ble ikke funnet", HttpStatusCode.NotFound),
    FEIL_VED_VIDERE_KALL("Klarte ikke kommunisere med bakomforliggende system", HttpStatusCode.InternalServerError),
}

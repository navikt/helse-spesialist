package no.nav.helse.spesialist.api.rest.personer

import io.ktor.http.HttpStatusCode
import no.nav.helse.spesialist.api.rest.ApiErrorCode
import no.nav.helse.spesialist.api.rest.ApiNotatType
import no.nav.helse.spesialist.api.rest.ApiNotatVedtaksperiodeId
import no.nav.helse.spesialist.api.rest.GetBehandler
import no.nav.helse.spesialist.api.rest.KallKontekst
import no.nav.helse.spesialist.api.rest.RestResponse
import no.nav.helse.spesialist.api.rest.Tags
import no.nav.helse.spesialist.api.rest.resources.Personer
import no.nav.helse.spesialist.application.PersonPseudoId
import no.nav.helse.spesialist.application.logg.loggInfo
import no.nav.helse.spesialist.domain.Notat
import no.nav.helse.spesialist.domain.NotatType
import no.nav.helse.spesialist.domain.Person

class GetNotatVedtaksperiodeIderForPersonBehandler : GetBehandler<Personer.PersonPseudoId.NotatVedtaksperiodeIder, List<ApiNotatVedtaksperiodeId>, ApiGetNotatVedtaksperiodeIderErrorCode> {
    override val tag = Tags.NOTATER

    override fun behandle(
        resource: Personer.PersonPseudoId.NotatVedtaksperiodeIder,
        kallKontekst: KallKontekst,
    ): RestResponse<List<ApiNotatVedtaksperiodeId>, ApiGetNotatVedtaksperiodeIderErrorCode> =
        kallKontekst.medPerson(
            personPseudoId = PersonPseudoId.fraString(resource.parent.pseudoId),
            personPseudoIdIkkeFunnet = { ApiGetNotatVedtaksperiodeIderErrorCode.PERSON_PSEUDO_ID_IKKE_FUNNET },
            manglerTilgangTilPerson = { ApiGetNotatVedtaksperiodeIderErrorCode.MANGLER_TILGANG_TIL_PERSON },
        ) { person -> behandleForPerson(person, kallKontekst) }

    private fun behandleForPerson(
        person: Person,
        kallKontekst: KallKontekst,
    ): RestResponse<List<ApiNotatVedtaksperiodeId>, ApiGetNotatVedtaksperiodeIderErrorCode> {
        val vedtaksperiodeIder = kallKontekst.transaksjon.vedtaksperiodeRepository.finnAlleIderForPerson(person.id)
        val notater = kallKontekst.transaksjon.notatRepository.finnAlleForVedtaksperioder(vedtaksperiodeIder)

        val resultat =
            notater
                .groupBy(Notat::vedtaksperiodeId)
                .map { (vedtaksperiodeId, notaterForVedtaksperiode) ->
                    ApiNotatVedtaksperiodeId(
                        vedtaksperiodeId = vedtaksperiodeId,
                        notattyper =
                            notaterForVedtaksperiode
                                .map(Notat::type)
                                .distinct()
                                .map(NotatType::tilApiNotatType)
                                .sorted(),
                    )
                }.sortedBy(ApiNotatVedtaksperiodeId::vedtaksperiodeId)

        loggInfo("Hentet ${resultat.size} vedtaksperioder med notater for person")

        return RestResponse.OK(resultat)
    }
}

private fun NotatType.tilApiNotatType() =
    when (this) {
        NotatType.Generelt -> ApiNotatType.Generelt
        NotatType.OpphevStans -> ApiNotatType.OpphevStans
    }

enum class ApiGetNotatVedtaksperiodeIderErrorCode(
    override val statusCode: HttpStatusCode,
    override val title: String,
) : ApiErrorCode {
    PERSON_PSEUDO_ID_IKKE_FUNNET(HttpStatusCode.NotFound, "PersonPseudoId har utløpt (eller aldri eksistert)"),
    MANGLER_TILGANG_TIL_PERSON(HttpStatusCode.Forbidden, "Mangler tilgang til person"),
}

package no.nav.helse.spesialist.api.rest.notater

import no.nav.helse.spesialist.api.rest.*
import no.nav.helse.spesialist.api.rest.resources.Personer
import no.nav.helse.spesialist.application.PersonPseudoId
import no.nav.helse.spesialist.application.logg.loggInfo
import no.nav.helse.spesialist.domain.Notat
import no.nav.helse.spesialist.domain.NotatType
import no.nav.helse.spesialist.domain.Person

class GetNotatVedtaksperiodeIderForPersonBehandler : GetBehandler<Personer.PersonPseudoId.NotatVedtaksperiodeIder, List<ApiNotatVedtaksperiodeId>, PersonErrorCode> {
    override val tag = Tags.NOTATER

    override fun behandle(
        resource: Personer.PersonPseudoId.NotatVedtaksperiodeIder,
        kallKontekst: KallKontekst,
    ): RestResponse<List<ApiNotatVedtaksperiodeId>, PersonErrorCode> =
        kallKontekst.medPerson(
            personPseudoId = PersonPseudoId.fraString(resource.parent.pseudoId),
        ) { person -> behandleForPerson(person, kallKontekst) }

    private fun behandleForPerson(
        person: Person,
        kallKontekst: KallKontekst,
    ): RestResponse<List<ApiNotatVedtaksperiodeId>, PersonErrorCode> {
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

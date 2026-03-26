package no.nav.helse.spesialist.api.rest.personer

import io.ktor.http.HttpStatusCode
import no.nav.helse.spesialist.api.rest.ApiErrorCode
import no.nav.helse.spesialist.api.rest.ApiInfotrygdperiode
import no.nav.helse.spesialist.api.rest.GetBehandler
import no.nav.helse.spesialist.api.rest.KallKontekst
import no.nav.helse.spesialist.api.rest.RestResponse
import no.nav.helse.spesialist.api.rest.Tags
import no.nav.helse.spesialist.api.rest.resources.Personer
import no.nav.helse.spesialist.application.InfotrygdperiodeHenter
import no.nav.helse.spesialist.application.PersonPseudoId
import no.nav.helse.spesialist.application.logg.loggInfo
import no.nav.helse.spesialist.domain.Infotrygdperiode
import no.nav.helse.spesialist.domain.Person

class GetInfotrygdperioderForPersonBehandler(
    private val infotrygdperiodeHenter: InfotrygdperiodeHenter,
) : GetBehandler<Personer.PersonPseudoId.Infotrygdperioder, List<ApiInfotrygdperiode>, ApiGetInfotrygdperioderForPersonErrorCode> {
    override val tag = Tags.PERSONER

    override fun behandle(
        resource: Personer.PersonPseudoId.Infotrygdperioder,
        kallKontekst: KallKontekst,
    ): RestResponse<List<ApiInfotrygdperiode>, ApiGetInfotrygdperioderForPersonErrorCode> =
        kallKontekst.medPerson(
            personPseudoId = PersonPseudoId.fraString(resource.parent.pseudoId),
            personPseudoIdIkkeFunnet = { ApiGetInfotrygdperioderForPersonErrorCode.PERSON_PSEUDO_ID_IKKE_FUNNET },
            manglerTilgangTilPerson = { ApiGetInfotrygdperioderForPersonErrorCode.MANGLER_TILGANG_TIL_PERSON },
        ) { person -> behandleForPerson(person, kallKontekst) }

    private fun behandleForPerson(
        person: Person,
        kallKontekst: KallKontekst,
    ): RestResponse<List<ApiInfotrygdperiode>, ApiGetInfotrygdperioderForPersonErrorCode> {
        val fom =
            kallKontekst.transaksjon.legacyVedtaksperiodeRepository
                .førsteKjenteDag(person.id.value)
                ?.minusYears(3)
                ?: return RestResponse.OK(emptyList())

        val perioder =
            infotrygdperiodeHenter
                .hentFor(identitetsnummer = person.id, fom = fom)
                .filterNot { it.type in setOf("SANKSJON", "TILBAKEFØRT", "UKJENT") }
                .sortedBy { it.fom }
                .sammenslå()
                .map { it.tilApiInfotrygdperiode() }

        loggInfo("Hentet ${perioder.size} infotrygdperioder")

        return RestResponse.OK(perioder)
    }

    private fun List<Infotrygdperiode>.sammenslå(): List<Infotrygdperiode> {
        if (isEmpty()) return emptyList()

        val resultat = mutableListOf<Infotrygdperiode>()
        var infotrygdperiode = first()

        while (true) {
            var tom = infotrygdperiode.tom
            var utvideTom = true

            while (utvideTom) {
                val senesteTom = filter { it.fom <= tom.plusDays(1) }.maxOfOrNull { it.tom }
                if (senesteTom != null && senesteTom > tom) {
                    tom = senesteTom
                } else {
                    utvideTom = false
                }
            }

            resultat.add(infotrygdperiode.copy(tom = tom))
            infotrygdperiode = firstOrNull { it.fom > tom.plusDays(1) } ?: break
        }

        return resultat
    }

    private fun Infotrygdperiode.tilApiInfotrygdperiode(): ApiInfotrygdperiode =
        ApiInfotrygdperiode(
            fom = fom,
            tom = tom,
        )
}

enum class ApiGetInfotrygdperioderForPersonErrorCode(
    override val title: String,
    override val statusCode: HttpStatusCode,
) : ApiErrorCode {
    PERSON_PSEUDO_ID_IKKE_FUNNET("PersonPseudoId har utløpt (eller aldri eksistert)", HttpStatusCode.NotFound),
    MANGLER_TILGANG_TIL_PERSON("Mangler tilgang til person", HttpStatusCode.Forbidden),
}

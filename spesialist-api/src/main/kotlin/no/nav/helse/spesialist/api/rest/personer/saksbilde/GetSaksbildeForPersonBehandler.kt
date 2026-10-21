package no.nav.helse.spesialist.api.rest.personer.saksbilde

import io.ktor.http.HttpStatusCode
import no.nav.helse.spesialist.api.AuditLogger
import no.nav.helse.spesialist.api.rest.ApiErrorCode
import no.nav.helse.spesialist.api.rest.GetBehandler
import no.nav.helse.spesialist.api.rest.KallKontekst
import no.nav.helse.spesialist.api.rest.RestResponse
import no.nav.helse.spesialist.api.rest.Tags
import no.nav.helse.spesialist.api.rest.resources.Personer
import no.nav.helse.spesialist.api.rest.saksbilde.ApiSaksbildePerson
import no.nav.helse.spesialist.application.PersonPseudoId
import no.nav.helse.spesialist.application.logg.MdcKey
import no.nav.helse.spesialist.application.logg.loggWarn

class GetSaksbildeForPersonBehandler(
    private val saksbildeHenter: SaksbildeHenter,
) : GetBehandler<Personer.PersonPseudoId.Saksbilde, ApiSaksbildePerson, ApiGetSaksbildeForPersonErrorCode> {
    override val tag = Tags.PERSONER

    override fun behandle(
        resource: Personer.PersonPseudoId.Saksbilde,
        kallKontekst: KallKontekst,
    ): RestResponse<ApiSaksbildePerson, ApiGetSaksbildeForPersonErrorCode> {
        val personPseudoId = PersonPseudoId.fraUUID(resource.parent.pseudoId)
        return kallKontekst.medMdcOgAttribute(MdcKey.PERSON_PSEUDO_ID to personPseudoId.value.toString()) {
            val identitetsnummer =
                kallKontekst.personPseudoIdProvider.hentIdentitetsnummer(personPseudoId)
                    ?: run {
                        loggWarn("Identitetsnummeret ble ikke funnet", "personPseudoId" to personPseudoId)
                        return@medMdcOgAttribute RestResponse.Error(ApiGetSaksbildeForPersonErrorCode.PERSON_PSEUDO_ID_IKKE_FUNNET)
                    }

            kallKontekst.medPerson(
                identitetsnummer = identitetsnummer,
                personIkkeFunnet = { ApiGetSaksbildeForPersonErrorCode.PERSON_IKKE_FUNNET },
                manglerTilgangTilPerson = {
                    AuditLogger.loggManglendeTilgang(saksbehandler = kallKontekst.saksbehandler, identitetsnummer = identitetsnummer)
                    ApiGetSaksbildeForPersonErrorCode.MANGLER_TILGANG_TIL_PERSON
                },
            ) { person ->
                val saksbilde =
                    try {
                        saksbildeHenter.hent(personEntity = person, transaction = kallKontekst.transaksjon)
                    } catch (_: SnapshotKunneIkkeHentesException) {
                        return@medPerson RestResponse.Error(ApiGetSaksbildeForPersonErrorCode.SNAPSHOT_KUNNE_IKKE_HENTES)
                    }
                AuditLogger.loggOk(saksbehandler = kallKontekst.saksbehandler, identitetsnummer = identitetsnummer)
                RestResponse.OK(saksbilde)
            }
        }
    }
}

enum class ApiGetSaksbildeForPersonErrorCode(
    override val title: String,
    override val statusCode: HttpStatusCode,
) : ApiErrorCode {
    PERSON_PSEUDO_ID_IKKE_FUNNET("PersonPseudoId har utløpt (eller aldri eksistert)", HttpStatusCode.NotFound),
    PERSON_IKKE_FUNNET("Fant ikke data for person", HttpStatusCode.NotFound),
    MANGLER_TILGANG_TIL_PERSON("Mangler tilgang til person", HttpStatusCode.Forbidden),
    SNAPSHOT_KUNNE_IKKE_HENTES("Feil ved henting av snapshot for person", HttpStatusCode.InternalServerError),
}

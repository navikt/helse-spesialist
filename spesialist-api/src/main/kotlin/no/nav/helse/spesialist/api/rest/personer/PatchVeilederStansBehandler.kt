package no.nav.helse.spesialist.api.rest.personer

import io.ktor.http.HttpStatusCode
import no.nav.helse.spesialist.api.rest.ApiErrorCode
import no.nav.helse.spesialist.api.rest.ApiStansRequest
import no.nav.helse.spesialist.api.rest.KallKontekst
import no.nav.helse.spesialist.api.rest.PatchBehandler
import no.nav.helse.spesialist.api.rest.RestResponse
import no.nav.helse.spesialist.api.rest.Tags
import no.nav.helse.spesialist.api.rest.resources.Personer
import no.nav.helse.spesialist.application.PersonPseudoId
import no.nav.helse.spesialist.application.logg.loggInfo
import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.NotatType
import no.nav.helse.spesialist.domain.Person

class PatchVeilederStansBehandler : PatchBehandler<Personer.PersonPseudoId.Stans.Veileder, ApiStansRequest, Unit, ApiPatchVeilederStansErrorCode> {
    override val tag = Tags.PERSONER

    override fun behandle(
        resource: Personer.PersonPseudoId.Stans.Veileder,
        request: ApiStansRequest,
        kallKontekst: KallKontekst,
    ): RestResponse<Unit, ApiPatchVeilederStansErrorCode> {
        if (request.stans) return RestResponse.Error(ApiPatchVeilederStansErrorCode.KAN_IKKE_OPPRETTE_VEILEDER_STANS)

        return kallKontekst.medPerson(
            personPseudoId = PersonPseudoId.fraString(resource.parent.parent.pseudoId),
            personPseudoIdIkkeFunnet = { ApiPatchVeilederStansErrorCode.PERSON_PSEUDO_ID_IKKE_FUNNET },
            manglerTilgangTilPerson = { ApiPatchVeilederStansErrorCode.MANGLER_TILGANG_TIL_PERSON },
        ) { person ->
            if (!request.stans) {
                opphevVeilederStans(request.begrunnelse, person, kallKontekst)
                opphevVeilederStansV2(request.begrunnelse, person, kallKontekst)
            }
            RestResponse.NoContent()
        }
    }

    private fun opphevVeilederStans(
        begrunnelse: String,
        person: Person,
        kallKontekst: KallKontekst,
    ) {
        kallKontekst.transaksjon.stansAutomatiskBehandlingDao.lagreFraSpeil(fødselsnummer = person.id.value)
        kallKontekst.transaksjon.notatDao.lagreForOppgaveId(
            oppgaveId =
                kallKontekst.transaksjon.oppgaveDao.finnOppgaveId(fødselsnummer = person.id.value)
                    ?: kallKontekst.transaksjon.oppgaveDao.finnOppgaveIdUansettStatus(fødselsnummer = person.id.value),
            tekst = begrunnelse,
            saksbehandlerOid = kallKontekst.saksbehandler.id.value,
            notatType = NotatType.OpphevStans,
            dialogRef = kallKontekst.transaksjon.dialogDao.lagre(),
        )
        loggInfo("Opphevet veileder-stans for person")
    }

    private fun opphevVeilederStansV2(
        begrunnelse: String,
        person: Person,
        kallKontekst: KallKontekst,
    ) {
        val identitetsnummer = Identitetsnummer.fraString(person.id.value)

        val aktivVeilederStans =
            kallKontekst.transaksjon.veilederStansRepository.finnAktiv(identitetsnummer)

        if (aktivVeilederStans != null) {
            aktivVeilederStans.opphevStans(
                opphevetAvSaksbehandlerIdent = kallKontekst.saksbehandler.ident,
                begrunnelse = begrunnelse,
            )
            kallKontekst.transaksjon.veilederStansRepository.lagre(aktivVeilederStans)
            loggInfo("Opphevet veileder-stans for person")
        }
    }
}

enum class ApiPatchVeilederStansErrorCode(
    override val title: String,
    override val statusCode: HttpStatusCode,
) : ApiErrorCode {
    PERSON_PSEUDO_ID_IKKE_FUNNET("PersonPseudoId har utløpt (eller aldri eksistert)", HttpStatusCode.NotFound),
    MANGLER_TILGANG_TIL_PERSON("Mangler tilgang til person", HttpStatusCode.Forbidden),
    KAN_IKKE_OPPRETTE_VEILEDER_STANS("Kan ikke opprette veilederstans", HttpStatusCode.BadRequest),
}

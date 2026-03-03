package no.nav.helse.spesialist.api.rest.personer

import io.github.smiley4.ktoropenapi.config.RouteConfig
import io.ktor.http.HttpStatusCode
import no.nav.helse.db.OppgaveDao
import no.nav.helse.modell.periodehistorikk.Historikkinnslag
import no.nav.helse.spesialist.api.rest.ApiErrorCode
import no.nav.helse.spesialist.api.rest.ApiStansRequest
import no.nav.helse.spesialist.api.rest.KallKontekst
import no.nav.helse.spesialist.api.rest.PatchBehandler
import no.nav.helse.spesialist.api.rest.RestResponse
import no.nav.helse.spesialist.api.rest.resources.Personer
import no.nav.helse.spesialist.application.PersonPseudoId
import no.nav.helse.spesialist.application.logg.loggInfo
import no.nav.helse.spesialist.domain.Dialog
import no.nav.helse.spesialist.domain.NotatType
import no.nav.helse.spesialist.domain.Person
import no.nav.helse.spesialist.domain.tilgangskontroll.Tilgang

class PatchStansBehandler : PatchBehandler<Personer.PersonPseudoId.Stans, ApiStansRequest, Unit, ApiPatchStansErrorCode> {
    override fun behandle(
        resource: Personer.PersonPseudoId.Stans,
        request: ApiStansRequest,
        kallKontekst: KallKontekst,
    ): RestResponse<Unit, ApiPatchStansErrorCode> {
        if (request.veilederStans) return RestResponse.Error(ApiPatchStansErrorCode.KAN_IKKE_OPPRETTE_VEILEDER_STANS)

        return kallKontekst.medPerson(
            personPseudoId = PersonPseudoId.fraString(resource.parent.pseudoId),
            personPseudoIdIkkeFunnet = { ApiPatchStansErrorCode.PERSON_PSEUDO_ID_IKKE_FUNNET },
            manglerTilgangTilPerson = { ApiPatchStansErrorCode.MANGLER_TILGANG_TIL_PERSON },
        ) { person ->
            if (!request.veilederStans) {
                opphevVeilederStans(request, person, kallKontekst)
            }
            if (request.saksbehandlerStans) {
                opprettSaksbehandlerstans(request, person, kallKontekst)
            } else {
                opphevSaksbehandlerstans(request, person, kallKontekst)
            }
            RestResponse.NoContent()
        }
    }

    private fun opprettSaksbehandlerstans(
        request: ApiStansRequest,
        person: Person,
        kallKontekst: KallKontekst,
    ) {
        kallKontekst.transaksjon.stansAutomatiskBehandlingSaksbehandlerDao.lagreStans(person.id.value)
        lagrePeriodehistorikkForSaksbehandlerstans(kallKontekst, person, request)
        loggInfo("Opprettet saksbehandler-stans for person")
    }

    private fun opphevSaksbehandlerstans(
        request: ApiStansRequest,
        person: Person,
        kallKontekst: KallKontekst,
    ) {
        kallKontekst.transaksjon.stansAutomatiskBehandlingSaksbehandlerDao.opphevStans(person.id.value)
        lagrePeriodehistorikkForOpphevelseAvSaksbehandlerstans(kallKontekst, person, request)
        loggInfo("Opphevet saksbehandler-stans for person")
    }

    private fun opphevVeilederStans(
        request: ApiStansRequest,
        person: Person,
        kallKontekst: KallKontekst,
    ) {
        kallKontekst.transaksjon.stansAutomatiskBehandlingDao.lagreFraSpeil(fødselsnummer = person.id.value)
        kallKontekst.transaksjon.notatDao.lagreForOppgaveId(
            oppgaveId =
                kallKontekst.transaksjon.oppgaveDao.finnOppgaveId(fødselsnummer = person.id.value)
                    ?: kallKontekst.transaksjon.oppgaveDao.finnOppgaveIdUansettStatus(fødselsnummer = person.id.value),
            tekst = request.begrunnelse,
            saksbehandlerOid = kallKontekst.saksbehandler.id.value,
            notatType = NotatType.OpphevStans,
            dialogRef = kallKontekst.transaksjon.dialogDao.lagre(),
        )

        loggInfo("Opphevet veileder-stans for person")
    }

    private fun lagrePeriodehistorikkForSaksbehandlerstans(
        kallKontekst: KallKontekst,
        person: Person,
        request: ApiStansRequest,
    ) {
        val oppgaveId = kallKontekst.transaksjon.oppgaveDao.oppgaveId(fødselsnummer = person.id.value)
        val dialog = Dialog.Factory.ny()
        kallKontekst.transaksjon.dialogRepository.lagre(dialog)

        val innslag =
            Historikkinnslag.automatiskBehandlingStansetAvSaksbehandler(
                saksbehandler = kallKontekst.saksbehandler,
                begrunnelse = request.begrunnelse,
                dialogId = dialog.id(),
            )
        kallKontekst.transaksjon.periodehistorikkDao.lagreMedOppgaveId(innslag, oppgaveId)
    }

    private fun lagrePeriodehistorikkForOpphevelseAvSaksbehandlerstans(
        kallKontekst: KallKontekst,
        person: Person,
        request: ApiStansRequest,
    ) {
        val oppgaveId = kallKontekst.transaksjon.oppgaveDao.oppgaveId(fødselsnummer = person.id.value)
        val dialog = Dialog.Factory.ny()
        kallKontekst.transaksjon.dialogRepository.lagre(dialog)

        val innslag =
            Historikkinnslag.opphevStansAvSaksbehandler(
                saksbehandler = kallKontekst.saksbehandler,
                begrunnelse = request.begrunnelse,
                dialogId = dialog.id(),
            )
        kallKontekst.transaksjon.periodehistorikkDao.lagreMedOppgaveId(innslag, oppgaveId)
    }

    private fun OppgaveDao.oppgaveId(fødselsnummer: String) = this.finnOppgaveId(fødselsnummer) ?: this.finnOppgaveIdUansettStatus(fødselsnummer)

    override fun openApi(config: RouteConfig) {
        with(config) {
            tags = setOf("Stans av automatisering")
        }
    }

    override val påkrevdTilgang: Tilgang = Tilgang.Skriv
}

enum class ApiPatchStansErrorCode(
    override val title: String,
    override val statusCode: HttpStatusCode,
) : ApiErrorCode {
    PERSON_PSEUDO_ID_IKKE_FUNNET("PersonPseudoId har utløpt (eller aldri eksistert)", HttpStatusCode.NotFound),
    MANGLER_TILGANG_TIL_PERSON("Mangler tilgang til person", HttpStatusCode.Forbidden),
    KAN_IKKE_OPPRETTE_VEILEDER_STANS("Kan ikke opprette veilederstans", HttpStatusCode.BadRequest),
}

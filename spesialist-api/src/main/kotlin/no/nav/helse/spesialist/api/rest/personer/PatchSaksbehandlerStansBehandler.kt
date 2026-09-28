package no.nav.helse.spesialist.api.rest.personer

import io.ktor.http.*
import no.nav.helse.modell.periodehistorikk.Historikkinnslag
import no.nav.helse.spesialist.api.rest.*
import no.nav.helse.spesialist.api.rest.resources.Personer
import no.nav.helse.spesialist.application.PersonPseudoId
import no.nav.helse.spesialist.application.logg.loggInfo
import no.nav.helse.spesialist.application.logg.loggWarn
import no.nav.helse.spesialist.domain.Dialog
import no.nav.helse.spesialist.domain.DialogId
import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.Person
import no.nav.helse.spesialist.domain.saksbehandlerstans.SaksbehandlerStans

class PatchSaksbehandlerStansBehandler : PatchBehandler<Personer.PersonPseudoId.Stans.Saksbehandler, ApiStansRequest, Unit, ApiPatchSaksbehandlerStansErrorCode> {
    override val tag = Tags.PERSONER

    override fun behandle(
        resource: Personer.PersonPseudoId.Stans.Saksbehandler,
        request: ApiStansRequest,
        kallKontekst: KallKontekst,
    ): RestResponse<Unit, ApiPatchSaksbehandlerStansErrorCode> =
        kallKontekst.medPerson(
            personPseudoId = PersonPseudoId.fraString(resource.parent.parent.pseudoId),
            personPseudoIdIkkeFunnet = { ApiPatchSaksbehandlerStansErrorCode.PERSON_PSEUDO_ID_IKKE_FUNNET },
            manglerTilgangTilPerson = { ApiPatchSaksbehandlerStansErrorCode.MANGLER_TILGANG_TIL_PERSON },
        ) { person ->
            if (request.stans) {
                opprettSaksbehandlerstansV2(request.begrunnelse, person, kallKontekst)
            } else {
                opphevSaksbehandlerstansV2(request.begrunnelse, person, kallKontekst)
            }
            RestResponse.NoContent()
        }

    private fun opprettSaksbehandlerstansV2(
        begrunnelse: String,
        person: Person,
        kallKontekst: KallKontekst,
    ) {
        val identitetsnummer = Identitetsnummer.fraString(person.id.value)
        val saksbehandlerIdent = kallKontekst.saksbehandler.ident

        val eksisterendeAktivStans = kallKontekst.transaksjon.saksbehandlerStansRepository.finnAktiv(identitetsnummer)
        if (eksisterendeAktivStans != null) return

        val stans =
            SaksbehandlerStans.ny(
                utførtAvSaksbehandlerIdent = saksbehandlerIdent,
                begrunnelse = begrunnelse,
                identitetsnummer = identitetsnummer,
            )
        kallKontekst.transaksjon.saksbehandlerStansRepository.lagre(stans)
        lagrePeriodehistorikkForSaksbehandlerstans(kallKontekst, person, begrunnelse)
        loggInfo("Opprettet saksbehandler-stans for person med aggregat")
    }

    private fun opphevSaksbehandlerstansV2(
        begrunnelse: String,
        person: Person,
        kallKontekst: KallKontekst,
    ) {
        val identitetsnummer = Identitetsnummer.fraString(person.id.value)
        val aktivStans = kallKontekst.transaksjon.saksbehandlerStansRepository.finnAktiv(identitetsnummer)

        if (aktivStans != null) {
            aktivStans.opphevStans(
                utførtAvSaksbehandlerIdent = kallKontekst.saksbehandler.ident,
                begrunnelse = begrunnelse,
            )
            kallKontekst.transaksjon.saksbehandlerStansRepository.lagre(aktivStans)
        }
        lagrePeriodehistorikkForOpphevelseAvSaksbehandlerstans(kallKontekst, person, begrunnelse)
        loggInfo("Opphevet saksbehandler-stans for person med aggregat")
    }

    private fun lagrePeriodehistorikkForSaksbehandlerstans(
        kallKontekst: KallKontekst,
        person: Person,
        begrunnelse: String,
    ) = lagrePeriodehistorikk(kallKontekst, person) { dialogId ->
        Historikkinnslag.automatiskBehandlingStansetAvSaksbehandler(
            saksbehandler = kallKontekst.saksbehandler,
            begrunnelse = begrunnelse,
            dialogId = dialogId,
        )
    }

    private fun lagrePeriodehistorikkForOpphevelseAvSaksbehandlerstans(
        kallKontekst: KallKontekst,
        person: Person,
        begrunnelse: String,
    ) = lagrePeriodehistorikk(kallKontekst, person) { dialogId ->
        Historikkinnslag.opphevStansAvSaksbehandler(
            saksbehandler = kallKontekst.saksbehandler,
            begrunnelse = begrunnelse,
            dialogId = dialogId,
        )
    }

    private fun lagrePeriodehistorikk(
        kallKontekst: KallKontekst,
        person: Person,
        innslag: (DialogId) -> Historikkinnslag,
    ) {
        val behandling = kallKontekst.transaksjon.behandlingRepository.finnNyeste(person.id)
        if (behandling == null) {
            loggWarn("Fant ingen behandling for personen, lagrer ikke periodehistorikk for saksbehandler-stans")
            return
        }
        val dialog = Dialog.Factory.ny()
        kallKontekst.transaksjon.dialogRepository.lagre(dialog)

        kallKontekst.transaksjon.periodehistorikkDao.lagre(innslag(dialog.id()), behandling.id.value)
    }
}

enum class ApiPatchSaksbehandlerStansErrorCode(
    override val title: String,
    override val statusCode: HttpStatusCode,
) : ApiErrorCode {
    PERSON_PSEUDO_ID_IKKE_FUNNET("PersonPseudoId har utløpt (eller aldri eksistert)", HttpStatusCode.NotFound),
    MANGLER_TILGANG_TIL_PERSON("Mangler tilgang til person", HttpStatusCode.Forbidden),
}

package no.nav.helse.spesialist.api.rest.personer

import io.ktor.http.HttpStatusCode
import no.nav.helse.db.OppgaveDao
import no.nav.helse.modell.periodehistorikk.Historikkinnslag
import no.nav.helse.spesialist.api.rest.ApiErrorCode
import no.nav.helse.spesialist.api.rest.ApiStansRequest
import no.nav.helse.spesialist.api.rest.KallKontekst
import no.nav.helse.spesialist.api.rest.PatchBehandler
import no.nav.helse.spesialist.api.rest.RestResponse
import no.nav.helse.spesialist.api.rest.Tags
import no.nav.helse.spesialist.api.rest.resources.Personer
import no.nav.helse.spesialist.application.PersonPseudoId
import no.nav.helse.spesialist.application.logg.loggInfo
import no.nav.helse.spesialist.domain.Dialog
import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.Person
import no.nav.helse.spesialist.domain.saksbehandlerstans.SaksbehandlerStans

class PatchStansSaksbehandlerBehandler : PatchBehandler<Personer.PersonPseudoId.Stans.Saksbehandler, ApiStansRequest, Unit, ApiPatchStansSaksbehandlerErrorCode> {
    override val tag = Tags.PERSONER

    override fun behandle(
        resource: Personer.PersonPseudoId.Stans.Saksbehandler,
        request: ApiStansRequest,
        kallKontekst: KallKontekst,
    ): RestResponse<Unit, ApiPatchStansSaksbehandlerErrorCode> =
        kallKontekst.medPerson(
            personPseudoId = PersonPseudoId.fraString(resource.parent.parent.pseudoId),
            personPseudoIdIkkeFunnet = { ApiPatchStansSaksbehandlerErrorCode.PERSON_PSEUDO_ID_IKKE_FUNNET },
            manglerTilgangTilPerson = { ApiPatchStansSaksbehandlerErrorCode.MANGLER_TILGANG_TIL_PERSON },
        ) { person ->
            if (request.stans) {
                opprettSaksbehandlerstans(request.begrunnelse, person, kallKontekst)
                opprettSaksbehandlerstansV2(request.begrunnelse, person, kallKontekst)
            } else {
                opphevSaksbehandlerstans(request.begrunnelse, person, kallKontekst)
                opphevSaksbehandlerstansV2(request.begrunnelse, person, kallKontekst)
            }
            RestResponse.NoContent()
        }

    private fun opprettSaksbehandlerstans(
        begrunnelse: String,
        person: Person,
        kallKontekst: KallKontekst,
    ) {
        kallKontekst.transaksjon.stansAutomatiskBehandlingSaksbehandlerDao.lagreStans(person.id.value)
        lagrePeriodehistorikkForSaksbehandlerstans(kallKontekst, person, begrunnelse)
        loggInfo("Opprettet saksbehandler-stans for person")
    }

    private fun opphevSaksbehandlerstans(
        begrunnelse: String,
        person: Person,
        kallKontekst: KallKontekst,
    ) {
        kallKontekst.transaksjon.stansAutomatiskBehandlingSaksbehandlerDao.opphevStans(person.id.value)
        lagrePeriodehistorikkForOpphevelseAvSaksbehandlerstans(kallKontekst, person, begrunnelse)
        loggInfo("Opphevet saksbehandler-stans for person")
    }

    private fun opprettSaksbehandlerstansV2(
        begrunnelse: String,
        person: Person,
        kallKontekst: KallKontekst,
    ) {
        val identitetsnummer = Identitetsnummer.fraString(person.id.value)
        val saksbehandlerIdent = kallKontekst.saksbehandler.ident

        val eksisterendeStans = kallKontekst.transaksjon.saksbehandlerStansRepository.finn(identitetsnummer)
        val stans =
            if (eksisterendeStans != null) {
                if (!eksisterendeStans.erStanset) {
                    eksisterendeStans.opprettStans(
                        utførtAvSaksbehandlerIdent = saksbehandlerIdent,
                        begrunnelse = begrunnelse,
                    )
                }
                eksisterendeStans
            } else {
                SaksbehandlerStans.ny(
                    utførtAvSaksbehandlerIdent = saksbehandlerIdent,
                    begrunnelse = begrunnelse,
                    identitetsnummer = identitetsnummer,
                )
            }
        kallKontekst.transaksjon.saksbehandlerStansRepository.lagre(stans)
        loggInfo("Opprettet saksbehandler-stans for person med aggregat")
    }

    private fun opphevSaksbehandlerstansV2(
        begrunnelse: String,
        person: Person,
        kallKontekst: KallKontekst,
    ) {
        val identitetsnummer = Identitetsnummer.fraString(person.id.value)
        val eksisterendeStans = kallKontekst.transaksjon.saksbehandlerStansRepository.finn(identitetsnummer)

        if (eksisterendeStans != null && eksisterendeStans.erStanset) {
            eksisterendeStans.opphevStans(
                utførtAvSaksbehandlerIdent = kallKontekst.saksbehandler.ident,
                begrunnelse = begrunnelse,
            )
            kallKontekst.transaksjon.saksbehandlerStansRepository.lagre(eksisterendeStans)
        }
        loggInfo("Opphevet saksbehandler-stans for person med aggregat")
    }

    private fun lagrePeriodehistorikkForSaksbehandlerstans(
        kallKontekst: KallKontekst,
        person: Person,
        begrunnelse: String,
    ) {
        val oppgaveId = kallKontekst.transaksjon.oppgaveDao.oppgaveId(fødselsnummer = person.id.value)
        val dialog = Dialog.Factory.ny()
        kallKontekst.transaksjon.dialogRepository.lagre(dialog)

        val innslag =
            Historikkinnslag.automatiskBehandlingStansetAvSaksbehandler(
                saksbehandler = kallKontekst.saksbehandler,
                begrunnelse = begrunnelse,
                dialogId = dialog.id(),
            )
        kallKontekst.transaksjon.periodehistorikkDao.lagreMedOppgaveId(innslag, oppgaveId)
    }

    private fun lagrePeriodehistorikkForOpphevelseAvSaksbehandlerstans(
        kallKontekst: KallKontekst,
        person: Person,
        begrunnelse: String,
    ) {
        val oppgaveId = kallKontekst.transaksjon.oppgaveDao.oppgaveId(fødselsnummer = person.id.value)
        val dialog = Dialog.Factory.ny()
        kallKontekst.transaksjon.dialogRepository.lagre(dialog)

        val innslag =
            Historikkinnslag.opphevStansAvSaksbehandler(
                saksbehandler = kallKontekst.saksbehandler,
                begrunnelse = begrunnelse,
                dialogId = dialog.id(),
            )
        kallKontekst.transaksjon.periodehistorikkDao.lagreMedOppgaveId(innslag, oppgaveId)
    }

    private fun OppgaveDao.oppgaveId(fødselsnummer: String) = this.finnOppgaveId(fødselsnummer) ?: this.finnOppgaveIdUansettStatus(fødselsnummer)
}

enum class ApiPatchStansSaksbehandlerErrorCode(
    override val title: String,
    override val statusCode: HttpStatusCode,
) : ApiErrorCode {
    PERSON_PSEUDO_ID_IKKE_FUNNET("PersonPseudoId har utløpt (eller aldri eksistert)", HttpStatusCode.NotFound),
    MANGLER_TILGANG_TIL_PERSON("Mangler tilgang til person", HttpStatusCode.Forbidden),
}

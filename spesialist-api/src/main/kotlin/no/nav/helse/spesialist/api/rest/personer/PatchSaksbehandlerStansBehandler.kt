package no.nav.helse.spesialist.api.rest.personer

import no.nav.helse.db.OppgaveDao
import no.nav.helse.modell.periodehistorikk.Historikkinnslag
import no.nav.helse.spesialist.api.rest.*
import no.nav.helse.spesialist.api.rest.resources.Personer
import no.nav.helse.spesialist.application.PersonPseudoId
import no.nav.helse.spesialist.application.logg.loggInfo
import no.nav.helse.spesialist.domain.Dialog
import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.Person
import no.nav.helse.spesialist.domain.saksbehandlerstans.SaksbehandlerStans

class PatchSaksbehandlerStansBehandler : PatchBehandler<Personer.PersonPseudoId.Stans.Saksbehandler, ApiStansRequest, Unit, PersonErrorCode> {
    override val tag = Tags.PERSONER

    override fun behandle(
        resource: Personer.PersonPseudoId.Stans.Saksbehandler,
        request: ApiStansRequest,
        kallKontekst: KallKontekst,
    ): RestResponse<Unit, PersonErrorCode> =
        kallKontekst.medPerson(
            personPseudoId = PersonPseudoId.fraString(resource.parent.parent.pseudoId),
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

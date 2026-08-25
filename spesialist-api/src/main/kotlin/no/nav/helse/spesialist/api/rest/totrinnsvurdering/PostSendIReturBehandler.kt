package no.nav.helse.spesialist.api.rest.totrinnsvurdering

import io.ktor.http.HttpStatusCode
import no.nav.helse.modell.Modellfeil
import no.nav.helse.modell.OppgaveAlleredeSendtIRetur
import no.nav.helse.modell.OppgaveKreverVurderingAvToSaksbehandlere
import no.nav.helse.modell.periodehistorikk.Historikkinnslag
import no.nav.helse.spesialist.api.rest.ApiErrorCode
import no.nav.helse.spesialist.api.rest.ApiSendIReturRequest
import no.nav.helse.spesialist.api.rest.KallKontekst
import no.nav.helse.spesialist.api.rest.PostBehandler
import no.nav.helse.spesialist.api.rest.RestResponse
import no.nav.helse.spesialist.api.rest.Tags
import no.nav.helse.spesialist.api.rest.resources.OppgaverBase
import no.nav.helse.spesialist.domain.oppgave.OppgaveId

class PostSendIReturBehandler : PostBehandler<OppgaverBase.OppgaveId.Totrinnsvurdering.SendIRetur, ApiSendIReturRequest, Unit, ApiPostSendIReturErrorCode> {
    override val tag = Tags.OPPGAVER

    override fun behandle(
        resource: OppgaverBase.OppgaveId.Totrinnsvurdering.SendIRetur,
        request: ApiSendIReturRequest,
        kallKontekst: KallKontekst,
    ): RestResponse<Unit, ApiPostSendIReturErrorCode> =
        kallKontekst.medOppgave(
            oppgaveId = OppgaveId(resource.parent.parent.oppgaveId),
            oppgaveIkkeFunnet = { ApiPostSendIReturErrorCode.OPPGAVE_IKKE_FUNNET },
        ) { oppgave, _, _, person ->
            val totrinnsvurdering =
                kallKontekst.transaksjon.totrinnsvurderingRepository.finnAktivForPerson(person.id.value)
                    ?: return@medOppgave RestResponse.Error(ApiPostSendIReturErrorCode.TOTRINNSVURDERING_IKKE_FUNNET)

            val opprinneligSaksbehandler =
                totrinnsvurdering.saksbehandler
                    ?.let(kallKontekst.transaksjon.saksbehandlerRepository::finn)
                    ?: return@medOppgave RestResponse.Error(ApiPostSendIReturErrorCode.TOTRINNSVURDERING_MANGLER_SAKSBEHANDLER)

            try {
                oppgave.sendIRetur(opprinneligSaksbehandler)
                totrinnsvurdering.sendIRetur(oppgave.id.value, kallKontekst.saksbehandler.id)
            } catch (modellfeil: Modellfeil) {
                return@medOppgave RestResponse.Error(modellfeil.tilErrorCode())
            }
            kallKontekst.transaksjon.oppgaveRepository.lagre(oppgave)
            kallKontekst.transaksjon.totrinnsvurderingRepository.lagre(totrinnsvurdering)

            kallKontekst.transaksjon.påVentRepository.finnFor(oppgave.vedtaksperiodeId)?.let {
                oppgave.fjernFraPåVent()
                kallKontekst.transaksjon.påVentRepository.slett(it.id())
                kallKontekst.transaksjon.oppgaveRepository.lagre(oppgave)
            }

            try {
                val dialogRef = kallKontekst.transaksjon.dialogDao.lagre()
                val innslag =
                    Historikkinnslag.totrinnsvurderingRetur(
                        notattekst = request.notatTekst,
                        saksbehandler = kallKontekst.saksbehandler,
                        dialogRef = dialogRef,
                    )
                kallKontekst.transaksjon.periodehistorikkDao.lagreMedOppgaveId(innslag, oppgave.id.value)
            } catch (_: Exception) {
                return@medOppgave RestResponse.Error(ApiPostSendIReturErrorCode.KUNNE_IKKE_OPPRETTE_HISTORIKKINNSLAG)
            }

            RestResponse.NoContent()
        }

    private fun Modellfeil.tilErrorCode(): ApiPostSendIReturErrorCode =
        when (this) {
            is OppgaveAlleredeSendtIRetur -> ApiPostSendIReturErrorCode.OPPGAVE_ALLEREDE_SENDT_I_RETUR
            is OppgaveKreverVurderingAvToSaksbehandlere -> ApiPostSendIReturErrorCode.KREVER_TOTRINNSVURDERING_AV_ANNEN
            else -> ApiPostSendIReturErrorCode.UVENTET_MODELLFEIL
        }
}

enum class ApiPostSendIReturErrorCode(
    override val title: String,
    override val statusCode: HttpStatusCode,
) : ApiErrorCode {
    OPPGAVE_IKKE_FUNNET("Oppgave ikke funnet", HttpStatusCode.NotFound),
    TOTRINNSVURDERING_IKKE_FUNNET("Aktiv totrinnsvurdering mangler for oppgaven", HttpStatusCode.Conflict),
    TOTRINNSVURDERING_MANGLER_SAKSBEHANDLER("Totrinnsvurdering mangler opprinnelig saksbehandler", HttpStatusCode.Conflict),
    OPPGAVE_ALLEREDE_SENDT_I_RETUR("Oppgaven er allerede sendt i retur", HttpStatusCode.Conflict),
    KREVER_TOTRINNSVURDERING_AV_ANNEN("Oppgaven krever totrinnsvurdering av annen saksbehandler", HttpStatusCode.Conflict),
    KUNNE_IKKE_OPPRETTE_HISTORIKKINNSLAG("Kunne ikke opprette historikkinnslag", HttpStatusCode.InternalServerError),
    UVENTET_MODELLFEIL("Kunne ikke sende oppgaven i retur", HttpStatusCode.BadRequest),
}

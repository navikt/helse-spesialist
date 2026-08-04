package no.nav.helse.spesialist.api.rest.totrinnsvurdering

import io.ktor.http.HttpStatusCode
import no.nav.helse.modell.Modellfeil
import no.nav.helse.modell.OppgaveAlleredeSendtBeslutter
import no.nav.helse.modell.OppgaveKreverVurderingAvToSaksbehandlere
import no.nav.helse.modell.periodehistorikk.Historikkinnslag
import no.nav.helse.spesialist.api.rest.ApiErrorCode
import no.nav.helse.spesialist.api.rest.ApiSendTilGodkjenningRequest
import no.nav.helse.spesialist.api.rest.KallKontekst
import no.nav.helse.spesialist.api.rest.PostBehandler
import no.nav.helse.spesialist.api.rest.RestResponse
import no.nav.helse.spesialist.api.rest.Tags
import no.nav.helse.spesialist.api.rest.resources.OppgaverBase
import no.nav.helse.spesialist.domain.oppgave.OppgaveId

class PostSendTilGodkjenningBehandler :
    PostBehandler<OppgaverBase.OppgaveId.Totrinnsvurdering.SendTilGodkjenning, ApiSendTilGodkjenningRequest, Unit, ApiPostSendTilGodkjenningErrorCode> {
    override val tag = Tags.OPPGAVER

    override fun behandle(
        resource: OppgaverBase.OppgaveId.Totrinnsvurdering.SendTilGodkjenning,
        request: ApiSendTilGodkjenningRequest,
        kallKontekst: KallKontekst,
    ): RestResponse<Unit, ApiPostSendTilGodkjenningErrorCode> =
        kallKontekst.medOppgave(
            oppgaveId = OppgaveId(resource.parent.parent.oppgaveId),
            oppgaveIkkeFunnet = { ApiPostSendTilGodkjenningErrorCode.OPPGAVE_IKKE_FUNNET },
            manglerTilgangTilPerson = { ApiPostSendTilGodkjenningErrorCode.MANGLER_TILGANG_TIL_PERSON },
        ) { oppgave, _, _, person ->
            val totrinnsvurdering =
                kallKontekst.transaksjon.totrinnsvurderingRepository.finnAktivForPerson(person.id.value)
                    ?: return@medOppgave RestResponse.Error(ApiPostSendTilGodkjenningErrorCode.TOTRINNSVURDERING_IKKE_FUNNET)
            try {
                val beslutter =
                    totrinnsvurdering.beslutter
                        ?.let(kallKontekst.transaksjon.saksbehandlerRepository::finn)
                oppgave.sendTilBeslutter(beslutter)
                totrinnsvurdering.sendTilBeslutter(oppgave.id.value, kallKontekst.saksbehandler.id)
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

            val innslag = Historikkinnslag.avventerTotrinnsvurdering(kallKontekst.saksbehandler)
            kallKontekst.transaksjon.periodehistorikkDao.lagreMedOppgaveId(innslag, oppgave.id.value)

            RestResponse.NoContent()
        }

    private fun Modellfeil.tilErrorCode(): ApiPostSendTilGodkjenningErrorCode =
        when (this) {
            is OppgaveAlleredeSendtBeslutter -> ApiPostSendTilGodkjenningErrorCode.OPPGAVE_ALLEREDE_SENDT_TIL_BESLUTTER
            is OppgaveKreverVurderingAvToSaksbehandlere -> ApiPostSendTilGodkjenningErrorCode.KREVER_TOTRINNSVURDERING_AV_ANNEN
            else -> ApiPostSendTilGodkjenningErrorCode.UVENTET_MODELLFEIL
        }
}

enum class ApiPostSendTilGodkjenningErrorCode(
    override val title: String,
    override val statusCode: HttpStatusCode,
) : ApiErrorCode {
    OPPGAVE_IKKE_FUNNET("Oppgave ikke funnet", HttpStatusCode.NotFound),
    MANGLER_TILGANG_TIL_PERSON("Mangler tilgang til person", HttpStatusCode.Forbidden),
    TOTRINNSVURDERING_IKKE_FUNNET("Aktiv totrinnsvurdering mangler for oppgaven", HttpStatusCode.Conflict),
    OPPGAVE_ALLEREDE_SENDT_TIL_BESLUTTER("Oppgaven er allerede sendt til beslutter", HttpStatusCode.Conflict),
    KREVER_TOTRINNSVURDERING_AV_ANNEN("Oppgaven krever totrinnsvurdering av annen saksbehandler", HttpStatusCode.Conflict),
    UVENTET_MODELLFEIL("Kunne ikke sende oppgaven til godkjenning", HttpStatusCode.BadRequest),
}

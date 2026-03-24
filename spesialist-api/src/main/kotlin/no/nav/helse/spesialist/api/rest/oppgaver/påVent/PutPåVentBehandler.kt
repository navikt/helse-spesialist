package no.nav.helse.spesialist.api.rest.oppgaver.påVent

import io.ktor.http.HttpStatusCode
import no.nav.helse.mediator.oppgave.tilUtgåendeHendelse
import no.nav.helse.modell.melding.LagtPåVentEvent
import no.nav.helse.modell.periodehistorikk.Historikkinnslag
import no.nav.helse.modell.saksbehandler.handlinger.PåVentÅrsak
import no.nav.helse.spesialist.api.rest.ApiErrorCode
import no.nav.helse.spesialist.api.rest.ApiPutPåVentRequest
import no.nav.helse.spesialist.api.rest.KallKontekst
import no.nav.helse.spesialist.api.rest.PutBehandler
import no.nav.helse.spesialist.api.rest.RestResponse
import no.nav.helse.spesialist.api.rest.Tags
import no.nav.helse.spesialist.api.rest.resources.OppgaverBase
import no.nav.helse.spesialist.application.Outbox
import no.nav.helse.spesialist.domain.Dialog
import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.Person
import no.nav.helse.spesialist.domain.PåVent
import no.nav.helse.spesialist.domain.Saksbehandler
import no.nav.helse.spesialist.domain.oppgave.Oppgave
import no.nav.helse.spesialist.domain.oppgave.OppgaveId
import no.nav.helse.spesialist.domain.oppgave.Oppgavehendelse

class PutPåVentBehandler : PutBehandler<OppgaverBase.OppgaveId.PåVent, ApiPutPåVentRequest, Unit, ApiPutPåVentErrorCode> {
    override val tag = Tags.OPPGAVER

    override fun behandle(
        resource: OppgaverBase.OppgaveId.PåVent,
        request: ApiPutPåVentRequest,
        kallKontekst: KallKontekst,
    ): RestResponse<Unit, ApiPutPåVentErrorCode> =
        kallKontekst.medOppgave(
            oppgaveId = OppgaveId(resource.parent.oppgaveId),
            oppgaveIkkeFunnet = { ApiPutPåVentErrorCode.OPPGAVE_IKKE_FUNNET },
            manglerTilgangTilPerson = { ApiPutPåVentErrorCode.MANGLER_TILGANG_TIL_PERSON },
        ) { oppgave, behandling, _, person ->
            val dialog = kallKontekst.opprettOgLagreNyDialog()
            val påVent =
                kallKontekst.transaksjon.påVentRepository
                    .finnFor(oppgave.vedtaksperiodeId)
                    ?: nyPåVent(oppgave, kallKontekst, request, dialog)

            val påVentEndres = påVent.harFåttTildeltId()

            val innslag: Historikkinnslag
            if (påVentEndres) {
                påVent.nyFrist(request.frist)
                påVent.nyNotattekst(request.notattekst)
                påVent.nyeÅrsaker(request.årsaker.map { årsak -> årsak.årsak })

                oppgave.endrePåVent(request.skalTildeles, saksbehandler = kallKontekst.saksbehandler)
                innslag = endrePåVentInnslag(kallKontekst, request, dialog)
            } else {
                oppgave.leggPåVent(request.skalTildeles, saksbehandler = kallKontekst.saksbehandler)
                innslag = leggPåVentInnslag(kallKontekst, request, dialog)
            }

            kallKontekst.transaksjon.periodehistorikkDao.lagre(innslag, behandling.id.value)
            kallKontekst.transaksjon.oppgaveRepository.lagre(oppgave)
            kallKontekst.transaksjon.påVentRepository.lagre(påVent)

            oppgave.konsumerHendelser().leggTilIOutbox(identitetsnummer = person.id, kallKontekst = kallKontekst)
            kallKontekst.outbox.leggTilPåVentEvent(person, oppgave, påVent, request, kallKontekst.saksbehandler)

            RestResponse.NoContent()
        }

    private fun nyPåVent(
        oppgave: Oppgave,
        kallKontekst: KallKontekst,
        request: ApiPutPåVentRequest,
        dialog: Dialog,
    ) = PåVent.Factory.ny(
        vedtaksperiodeId = oppgave.vedtaksperiodeId,
        saksbehandlerOid = kallKontekst.saksbehandler.id,
        frist = request.frist,
        årsaker = request.årsaker.map { it.årsak },
        notattekst = request.notattekst,
        dialogRef = dialog.id(),
    )

    private fun KallKontekst.opprettOgLagreNyDialog() = Dialog.Factory.ny().also(this.transaksjon.dialogRepository::lagre)

    private fun leggPåVentInnslag(
        kallKontekst: KallKontekst,
        request: ApiPutPåVentRequest,
        dialog: Dialog,
    ) = Historikkinnslag.lagtPåVentInnslag(
        notattekst = request.notattekst,
        saksbehandler = kallKontekst.saksbehandler,
        årsaker =
            request.årsaker.map {
                PåVentÅrsak(key = it.key, årsak = it.årsak)
            },
        frist = request.frist,
        dialogRef = dialog.id().value,
    )

    private fun endrePåVentInnslag(
        kallKontekst: KallKontekst,
        request: ApiPutPåVentRequest,
        dialog: Dialog,
    ) = Historikkinnslag.endrePåVentInnslag(
        notattekst = request.notattekst,
        saksbehandler = kallKontekst.saksbehandler,
        årsaker =
            request.årsaker.map {
                PåVentÅrsak(key = it.key, årsak = it.årsak)
            },
        frist = request.frist,
        dialogRef = dialog.id().value,
    )

    private fun List<Oppgavehendelse>.leggTilIOutbox(
        identitetsnummer: Identitetsnummer,
        kallKontekst: KallKontekst,
    ) = this
        .forEach { kallKontekst.outbox.leggTil(identitetsnummer, it.tilUtgåendeHendelse(), "Oppgave lagt på vent") }

    private fun Outbox.leggTilPåVentEvent(
        person: Person,
        oppgave: Oppgave,
        påVent: PåVent,
        request: ApiPutPåVentRequest,
        saksbehandler: Saksbehandler,
    ) {
        leggTil(
            person.id,
            LagtPåVentEvent(
                person.id.value,
                oppgave.id.value,
                oppgave.behandlingId.value,
                request.skalTildeles,
                frist = påVent.frist,
                påVent.notattekst,
                request.årsaker.map { PåVentÅrsak(it.key, it.årsak) },
                saksbehandlerOid = saksbehandler.id.value,
                saksbehandlerIdent = saksbehandler.ident.value,
            ),
            "Lagt på vent",
        )
    }
}

enum class ApiPutPåVentErrorCode(
    override val title: String,
    override val statusCode: HttpStatusCode,
) : ApiErrorCode {
    OPPGAVE_IKKE_FUNNET("Oppgave ikke funnet", HttpStatusCode.NotFound),
    MANGLER_TILGANG_TIL_PERSON("Mangler tilgang til person", HttpStatusCode.Forbidden),
}

package no.nav.helse.spesialist.api.rest.oppgaver.påVent

import io.ktor.http.HttpStatusCode
import no.nav.helse.modell.periodehistorikk.Historikkinnslag
import no.nav.helse.spesialist.api.rest.ApiErrorCode
import no.nav.helse.spesialist.api.rest.DeleteBehandler
import no.nav.helse.spesialist.api.rest.KallKontekst
import no.nav.helse.spesialist.api.rest.RestResponse
import no.nav.helse.spesialist.api.rest.Tags
import no.nav.helse.spesialist.api.rest.oppgaver.leggTilIOutbox
import no.nav.helse.spesialist.api.rest.resources.OppgaverBase
import no.nav.helse.spesialist.domain.oppgave.OppgaveId

class DeletePåVentBehandler : DeleteBehandler<OppgaverBase.OppgaveId.PåVent, Unit, ApiDeletePåVentErrorCode> {
    override val tag: Tags = Tags.OPPGAVER

    override fun behandle(
        resource: OppgaverBase.OppgaveId.PåVent,
        kallKontekst: KallKontekst,
    ): RestResponse<Unit, ApiDeletePåVentErrorCode> {
        return kallKontekst.medOppgave(
            oppgaveId = OppgaveId(resource.parent.oppgaveId),
            oppgaveIkkeFunnet = { ApiDeletePåVentErrorCode.OPPGAVE_IKKE_FUNNET },
            manglerTilgangTilPerson = { ApiDeletePåVentErrorCode.MANGLER_TILGANG_TIL_PERSON },
        ) { oppgave, behandling, vedtaksperiode, person ->
            val påVent = kallKontekst.transaksjon.påVentRepository.finnFor(vedtaksperiode.id) ?: return@medOppgave RestResponse.NoContent()
            val innslag = Historikkinnslag.fjernetFraPåVentInnslag(kallKontekst.saksbehandler)
            oppgave.fjernFraPåVent()
            kallKontekst.transaksjon.periodehistorikkDao.lagre(innslag, behandling.id.value)
            kallKontekst.transaksjon.oppgaveRepository.lagre(oppgave)
            kallKontekst.transaksjon.påVentRepository.slett(påVent.id())

            oppgave.konsumerHendelser().leggTilIOutbox(person.id, kallKontekst, "Oppgave fjernet fra på vent")
            RestResponse.NoContent()
        }
    }
}

enum class ApiDeletePåVentErrorCode(
    override val title: String,
    override val statusCode: HttpStatusCode,
) : ApiErrorCode {
    OPPGAVE_IKKE_FUNNET("Oppgave ikke funnet", HttpStatusCode.NotFound),
    MANGLER_TILGANG_TIL_PERSON("Mangler tilgang til person", HttpStatusCode.Forbidden),
}

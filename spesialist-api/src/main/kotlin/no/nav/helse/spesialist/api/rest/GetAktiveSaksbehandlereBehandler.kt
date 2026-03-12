package no.nav.helse.spesialist.api.rest

import no.nav.helse.spesialist.api.rest.resources.AktiveSaksbehandlere
import no.nav.helse.spesialist.application.logg.loggInfo

class GetAktiveSaksbehandlereBehandler : GetBehandler<AktiveSaksbehandlere, List<ApiAktivSaksbehandler>, ApiGetAktiveSaksbehandlereErrorCode> {
    override val tag = Tags.SAKSBEHANDLERE

    override fun behandle(
        resource: AktiveSaksbehandlere,
        kallKontekst: KallKontekst,
    ): RestResponse<List<ApiAktivSaksbehandler>, ApiGetAktiveSaksbehandlereErrorCode> {
        val saksbehandlere =
            kallKontekst.transaksjon.saksbehandlerDao
                .hentAlleAktiveSisteTreMnderEllerHarTildelteOppgaver()
                .map { ApiAktivSaksbehandler(ident = it.ident.value, navn = it.navn, oid = it.id.value) }

        loggInfo("Hentet ${saksbehandlere.size} aktive saksbehandlere")

        return RestResponse.OK(saksbehandlere)
    }
}

enum class ApiGetAktiveSaksbehandlereErrorCode : ApiErrorCode

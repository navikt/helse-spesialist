package no.nav.helse.spesialist.api.rest.saksbehandlere

import no.nav.helse.spesialist.api.rest.ApiAktivSaksbehandler
import no.nav.helse.spesialist.api.rest.ApiErrorCode
import no.nav.helse.spesialist.api.rest.GetBehandler
import no.nav.helse.spesialist.api.rest.KallKontekst
import no.nav.helse.spesialist.api.rest.RestResponse
import no.nav.helse.spesialist.api.rest.Tags
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
                .map {
                    ApiAktivSaksbehandler(
                        ident = it.ident.value,
                        navn = it.navn,
                        oid = it.id.value,
                    )
                }

        loggInfo("Hentet ${saksbehandlere.size} aktive saksbehandlere")

        return RestResponse.OK(saksbehandlere)
    }
}

enum class ApiGetAktiveSaksbehandlereErrorCode : ApiErrorCode

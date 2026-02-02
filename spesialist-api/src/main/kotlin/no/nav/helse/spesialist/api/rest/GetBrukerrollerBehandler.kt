package no.nav.helse.spesialist.api.rest

import io.github.smiley4.ktoropenapi.config.RouteConfig
import no.nav.helse.spesialist.api.rest.resources.Brukerroller

class GetBrukerrollerBehandler : GetBehandler<Brukerroller, List<String>, GetBrukerrollerErrorCode> {
    override fun behandle(
        resource: Brukerroller,
        kallKontekst: KallKontekst,
    ): RestResponse<List<String>, GetBrukerrollerErrorCode> =
        RestResponse.OK(
            kallKontekst.brukerroller.map { it.name },
        )

    override fun openApi(config: RouteConfig) {
        with(config) {
            tags = setOf("Saksbehandlere")
        }
    }
}

enum class GetBrukerrollerErrorCode : ApiErrorCode

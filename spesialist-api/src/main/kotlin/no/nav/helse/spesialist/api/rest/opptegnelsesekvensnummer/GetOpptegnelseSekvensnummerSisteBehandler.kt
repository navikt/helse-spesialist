package no.nav.helse.spesialist.api.rest.opptegnelsesekvensnummer

import io.github.smiley4.ktoropenapi.config.RouteConfig
import io.ktor.http.HttpStatusCode
import no.nav.helse.spesialist.api.rest.ApiErrorCode
import no.nav.helse.spesialist.api.rest.GetBehandler
import no.nav.helse.spesialist.api.rest.KallKontekst
import no.nav.helse.spesialist.api.rest.RestResponse
import no.nav.helse.spesialist.api.rest.resources.OpptegnelseSekvensnummer

class GetOpptegnelseSekvensnummerSisteBehandler : GetBehandler<OpptegnelseSekvensnummer.Siste, Int, ApiGetOpptegnelseSekvensnummerSisteErrorCode> {
    override fun behandle(
        resource: OpptegnelseSekvensnummer.Siste,
        kallKontekst: KallKontekst,
    ): RestResponse<Int, ApiGetOpptegnelseSekvensnummerSisteErrorCode> =
        RestResponse.OK(
            kallKontekst.transaksjon.opptegnelseRepository
                .finnNyesteSekvensnummer()
                .value,
        )

    override fun openApi(config: RouteConfig) {
        with(config) {
            tags = setOf("Opptegnelser")
        }
    }
}

enum class ApiGetOpptegnelseSekvensnummerSisteErrorCode(
    override val title: String,
    override val statusCode: HttpStatusCode,
) : ApiErrorCode

package no.nav.helse.spesialist.api.rest.opptegnelsesekvensnummer

import io.github.smiley4.ktoropenapi.config.RouteConfig
import io.ktor.http.HttpStatusCode
import no.nav.helse.spesialist.api.rest.ApiErrorCode
import no.nav.helse.spesialist.api.rest.GetBehandler
import no.nav.helse.spesialist.api.rest.KallKontekst
import no.nav.helse.spesialist.api.rest.RestResponse
import no.nav.helse.spesialist.api.rest.resources.OpptegnelseSekvensnummer
import no.nav.helse.spesialist.application.logg.loggInfo
import no.nav.helse.spesialist.domain.tilgangskontroll.Tilgang

class GetOpptegnelseSekvensnummerSisteBehandler : GetBehandler<OpptegnelseSekvensnummer.Siste, Int, ApiGetOpptegnelseSekvensnummerSisteErrorCode> {
    override val påkrevdTilgang = Tilgang.Les

    override fun behandle(
        resource: OpptegnelseSekvensnummer.Siste,
        kallKontekst: KallKontekst,
    ): RestResponse<Int, ApiGetOpptegnelseSekvensnummerSisteErrorCode> {
        val sekvensnummer =
            kallKontekst.transaksjon.opptegnelseRepository
                .finnNyesteSekvensnummer()
                .value

        loggInfo("Hentet siste sekvensnummer for opptegnelser ($sekvensnummer)")

        return RestResponse.OK(sekvensnummer)
    }

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

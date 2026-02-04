package no.nav.helse.spesialist.api.rest

import io.github.smiley4.ktoropenapi.config.RouteConfig
import no.nav.helse.spesialist.api.rest.resources.Brukerroller
import no.nav.helse.spesialist.domain.tilgangskontroll.Brukerrolle
import no.nav.helse.spesialist.domain.tilgangskontroll.Tilgang

class GetBrukerrollerBehandler : GetBehandler<Brukerroller, List<ApiBrukerrolle>, GetBrukerrollerErrorCode> {
    override val påkrevdeTilganger: Set<Tilgang> = Tilgang.entries.toSet()

    override fun behandle(
        resource: Brukerroller,
        kallKontekst: KallKontekst,
    ): RestResponse<List<ApiBrukerrolle>, GetBrukerrollerErrorCode> =
        RestResponse.OK(
            kallKontekst.brukerroller.map {
                when (it) {
                    Brukerrolle.SELVSTENDIG_NÆRINGSDRIVENDE_BETA -> ApiBrukerrolle.SELVSTENDIG_NÆRINGSDRIVENDE_BETA
                    Brukerrolle.BESLUTTER -> ApiBrukerrolle.BESLUTTER
                    Brukerrolle.EGEN_ANSATT -> ApiBrukerrolle.EGEN_ANSATT
                    Brukerrolle.KODE_7 -> ApiBrukerrolle.KODE_7
                    Brukerrolle.STIKKPRØVE -> ApiBrukerrolle.STIKKPRØVE
                    Brukerrolle.UTVIKLER -> ApiBrukerrolle.UTVIKLER
                }
            } +
                kallKontekst.tilganger.map {
                    when (it) {
                        Tilgang.SAKSBEHANDLER -> ApiBrukerrolle.SAKSBEHANDLER
                        Tilgang.LESETILGANG -> ApiBrukerrolle.LESETILGANG
                    }
                },
        )

    override fun openApi(config: RouteConfig) {
        with(config) {
            tags = setOf("Saksbehandlere")
        }
    }
}

enum class GetBrukerrollerErrorCode : ApiErrorCode

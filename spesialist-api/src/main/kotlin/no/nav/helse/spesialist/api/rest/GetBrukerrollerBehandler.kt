package no.nav.helse.spesialist.api.rest

import io.github.smiley4.ktoropenapi.config.RouteConfig
import no.nav.helse.spesialist.api.rest.resources.Brukerroller
import no.nav.helse.spesialist.domain.tilgangskontroll.Brukerrolle
import no.nav.helse.spesialist.domain.tilgangskontroll.Tilgang

class GetBrukerrollerBehandler : GetBehandler<Brukerroller, List<ApiBrukerrolle>, GetBrukerrollerErrorCode> {
    override val påkrevdTilgang = Tilgang.Les

    override fun behandle(
        resource: Brukerroller,
        kallKontekst: KallKontekst,
    ): RestResponse<List<ApiBrukerrolle>, GetBrukerrollerErrorCode> =
        RestResponse.OK(
            kallKontekst.brukerroller.map {
                when (it) {
                    Brukerrolle.SelvstendigNæringsdrivendeBeta -> ApiBrukerrolle.SELVSTENDIG_NÆRINGSDRIVENDE_BETA
                    Brukerrolle.Beslutter -> ApiBrukerrolle.BESLUTTER
                    Brukerrolle.EgenAnsatt -> ApiBrukerrolle.EGEN_ANSATT
                    Brukerrolle.Kode7 -> ApiBrukerrolle.KODE_7
                    Brukerrolle.Stikkprøve -> ApiBrukerrolle.STIKKPRØVE
                    Brukerrolle.Utvikler -> ApiBrukerrolle.UTVIKLER
                }
            } +
                kallKontekst.tilganger.map {
                    when (it) {
                        Tilgang.Skriv -> ApiBrukerrolle.SAKSBEHANDLER
                        Tilgang.Les -> ApiBrukerrolle.LESETILGANG
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

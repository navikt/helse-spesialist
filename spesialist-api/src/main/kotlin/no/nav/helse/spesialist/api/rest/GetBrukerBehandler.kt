package no.nav.helse.spesialist.api.rest

import io.github.smiley4.ktoropenapi.config.RouteConfig
import no.nav.helse.spesialist.api.rest.resources.Bruker
import no.nav.helse.spesialist.domain.tilgangskontroll.Brukerrolle
import no.nav.helse.spesialist.domain.tilgangskontroll.Tilgang

class GetBrukerBehandler : GetBehandler<Bruker, ApiBruker, GetBrukerErrorCode> {
    override val påkrevdTilgang = Tilgang.Les

    override fun behandle(
        resource: Bruker,
        kallKontekst: KallKontekst,
    ): RestResponse<ApiBruker, GetBrukerErrorCode> =
        RestResponse.OK(
            ApiBruker(
                brukerroller =
                    kallKontekst.brukerroller
                        .map {
                            when (it) {
                                Brukerrolle.SelvstendigNæringsdrivendeBeta -> ApiBrukerrolle.SELVSTENDIG_NÆRINGSDRIVENDE_BETA
                                Brukerrolle.Beslutter -> ApiBrukerrolle.BESLUTTER
                                Brukerrolle.EgenAnsatt -> ApiBrukerrolle.EGEN_ANSATT
                                Brukerrolle.Kode7 -> ApiBrukerrolle.KODE_7
                                Brukerrolle.Stikkprøve -> ApiBrukerrolle.STIKKPRØVE
                                Brukerrolle.Utvikler -> ApiBrukerrolle.UTVIKLER
                            }
                        }.toSet(),
                tilganger =
                    kallKontekst.tilganger
                        .map {
                            when (it) {
                                Tilgang.Les -> ApiTilgang.LES
                                Tilgang.Skriv -> ApiTilgang.SKRIV
                            }
                        }.toSet(),
            ),
        )

    override fun openApi(config: RouteConfig) {
        with(config) {
            tags = setOf("Saksbehandlere")
        }
    }
}

enum class GetBrukerErrorCode : ApiErrorCode

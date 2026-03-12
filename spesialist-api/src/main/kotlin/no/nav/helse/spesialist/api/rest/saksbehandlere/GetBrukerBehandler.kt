package no.nav.helse.spesialist.api.rest.saksbehandlere

import no.nav.helse.spesialist.api.rest.ApiBruker
import no.nav.helse.spesialist.api.rest.ApiBrukerrolle
import no.nav.helse.spesialist.api.rest.ApiErrorCode
import no.nav.helse.spesialist.api.rest.ApiTilgang
import no.nav.helse.spesialist.api.rest.GetBehandler
import no.nav.helse.spesialist.api.rest.KallKontekst
import no.nav.helse.spesialist.api.rest.RestResponse
import no.nav.helse.spesialist.api.rest.Tags
import no.nav.helse.spesialist.api.rest.resources.Bruker
import no.nav.helse.spesialist.application.logg.loggInfo
import no.nav.helse.spesialist.domain.tilgangskontroll.Brukerrolle
import no.nav.helse.spesialist.domain.tilgangskontroll.Tilgang

class GetBrukerBehandler : GetBehandler<Bruker, ApiBruker, GetBrukerErrorCode> {
    override val tag = Tags.SAKSBEHANDLERE

    override fun behandle(
        resource: Bruker,
        kallKontekst: KallKontekst,
    ): RestResponse<ApiBruker, GetBrukerErrorCode> {
        val apiBruker =
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
            )

        loggInfo("Hentet informasjon om innlogget bruker")

        return RestResponse.OK(apiBruker)
    }
}

enum class GetBrukerErrorCode : ApiErrorCode

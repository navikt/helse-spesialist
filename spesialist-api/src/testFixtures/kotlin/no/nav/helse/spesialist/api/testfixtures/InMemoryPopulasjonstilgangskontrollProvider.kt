package no.nav.helse.spesialist.api.testfixtures

import com.github.navikt.tbd_libs.populasjonstilgang.api.PopulasjonstilgangskontrollProvider
import com.github.navikt.tbd_libs.populasjonstilgang.api.TilgangskontrollResultat

class InMemoryPopulasjonstilgangskontrollProvider : PopulasjonstilgangskontrollProvider {
    var resultat: TilgangskontrollResultat = TilgangskontrollResultat.Ok

    override fun kontrollerKomplettTilgang(
        accessToken: String,
        fødselsnummer: String,
    ): TilgangskontrollResultat = resultat

    override fun kontrollerKjerneTilgang(
        accessToken: String,
        fødselsnummer: String,
    ): TilgangskontrollResultat = resultat

    override fun kontrollerKjerneTilgangForAnsatt(
        ansattId: String,
        fødselsnummer: String,
    ): TilgangskontrollResultat = resultat
}

package no.nav.helse.spesialist.bootstrap

import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import no.nav.helse.spesialist.bootstrap.context.TestContext
import no.nav.helse.spesialist.bootstrap.context.Vedtaksperiode

class TilleggsmeldingReceiver(
    private val rapidsConnection: RapidsConnection,
    private val testContext: TestContext,
    private val vedtaksperiode: Vedtaksperiode,
) {
    fun aktivitetsloggNyAktivitet(varselkoder: List<String>) {
        rapidsConnection.publish(
            testContext.person.fødselsnummer,
            Meldingsbygger.byggAktivitetsloggNyAktivitetMedVarsler(
                varselkoder = varselkoder,
                person = testContext.person,
                arbeidsgiver = testContext.arbeidsgiver,
                vedtaksperiode = vedtaksperiode,
            ),
        )
    }
}

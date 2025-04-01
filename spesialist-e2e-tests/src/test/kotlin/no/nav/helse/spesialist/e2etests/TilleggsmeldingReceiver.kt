package no.nav.helse.spesialist.e2etests

import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import no.nav.helse.spesialist.e2etests.context.TestContext
import no.nav.helse.spesialist.e2etests.context.VårVedtaksperiode

class TilleggsmeldingReceiver(
    private val rapidsConnection: RapidsConnection,
    private val testContext: TestContext,
    private val vedtaksperiode: VårVedtaksperiode
) {
    fun aktivitetsloggNyAktivitet(varselkoder: List<String>) {
        rapidsConnection.publish(
            VårMeldingsbygger.byggAktivitetsloggNyAktivitetMedVarsler(
                varselkoder = varselkoder,
                person = testContext.person,
                arbeidsgiver = testContext.arbeidsgiver,
                vedtaksperiode = vedtaksperiode
            )
        )
    }
}

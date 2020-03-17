package no.nav.helse

import io.ktor.util.KtorExperimentalAPI
import kotlinx.coroutines.runBlocking
import no.nav.helse.mediator.kafka.SpleisBehovMediator
import no.nav.helse.meldinger.AvventerGodkjenningBehov
import no.nav.helse.meldinger.PotensieltSvarPåBegge3Behovene
import no.nav.helse.rapids_rivers.RapidApplication

@KtorExperimentalAPI
fun main(): Unit = runBlocking {
    val dataSourceBuilder = DataSourceBuilder(System.getenv())
    dataSourceBuilder.migrate()
    val dataSource = dataSourceBuilder.getDataSource(DataSourceBuilder.Role.User)

    RapidApplication.create(System.getenv()).apply {
        val spleisBehovMediator = SpleisBehovMediator()

        AvventerGodkjenningBehov.Factory(this, spleisBehovMediator)
        PotensieltSvarPåBegge3Behovene.Factory(this, spleisBehovMediator)
    }.start()
}

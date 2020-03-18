package no.nav.helse

import io.ktor.util.KtorExperimentalAPI
import kotlinx.coroutines.runBlocking
import no.nav.helse.mediator.kafka.SpleisBehovMediator
import no.nav.helse.mediator.kafka.meldinger.GodkjenningMessage
import no.nav.helse.mediator.kafka.meldinger.PersoninfoMessage
import no.nav.helse.modell.dao.ArbeidsgiverDao
import no.nav.helse.modell.dao.PersonDao
import no.nav.helse.modell.dao.VedtakDao
import no.nav.helse.rapids_rivers.RapidApplication

@KtorExperimentalAPI
fun main(): Unit = runBlocking {
    val dataSourceBuilder = DataSourceBuilder(System.getenv())
    dataSourceBuilder.migrate()
    val dataSource = dataSourceBuilder.getDataSource(DataSourceBuilder.Role.User)
    val personDao = PersonDao(dataSource)
    val arbeidsgiverDao = ArbeidsgiverDao(dataSource)
    val vedtakDao = VedtakDao(dataSource)

    RapidApplication.create(System.getenv()).apply {
        val spleisBehovMediator = SpleisBehovMediator()

        GodkjenningMessage.Factory(this, personDao, arbeidsgiverDao, vedtakDao, spleisBehovMediator)
        PersoninfoMessage.Factory(this, spleisBehovMediator)
    }.start()
}



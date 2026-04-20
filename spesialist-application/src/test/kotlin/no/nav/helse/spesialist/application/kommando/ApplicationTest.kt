package no.nav.helse.spesialist.application.kommando

import no.nav.helse.modell.melding.UtgåendeHendelse
import no.nav.helse.spesialist.application.InMemoryMeldingPubliserer
import no.nav.helse.spesialist.application.InMemoryRepositoriesAndDaos
import no.nav.helse.spesialist.application.Outbox
import kotlin.test.assertNotNull

abstract class ApplicationTest {
    private val inMemoryRepositoriesAndDaos = InMemoryRepositoriesAndDaos()
    val sessionContext = inMemoryRepositoriesAndDaos.sessionFactory.sessionContext
    val outbox = Outbox("1.0.0")

    inline fun <reified T : UtgåendeHendelse> assertUtgåendeHendelse() {
        val meldingPubliserer = InMemoryMeldingPubliserer()
        outbox.sendAlle(meldingPubliserer)
        assertNotNull(meldingPubliserer.publiserteUtgåendeHendelser.map { it.hendelse }.singleOrNull { it is T })
    }
}

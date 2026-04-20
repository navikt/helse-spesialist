package no.nav.helse.spesialist.application.kommando

import no.nav.helse.modell.melding.UtgåendeHendelse
import no.nav.helse.spesialist.application.InMemoryMeldingPubliserer
import no.nav.helse.spesialist.application.InMemoryRepositoriesAndDaos
import no.nav.helse.spesialist.application.Outbox
import org.junit.jupiter.api.assertNull
import kotlin.test.assertNotNull

abstract class ApplicationTest {
    private val inMemoryRepositoriesAndDaos = InMemoryRepositoriesAndDaos()
    val sessionContext = inMemoryRepositoriesAndDaos.sessionFactory.sessionContext
    val outbox = Outbox("1.0.0")

    inline fun <reified T : UtgåendeHendelse> assertUtgåendeHendelse(assertBlock: (T) -> Unit = {}) {
        val meldingPubliserer = InMemoryMeldingPubliserer()
        outbox.sendAlle(meldingPubliserer)
        val melding =
            meldingPubliserer.publiserteUtgåendeHendelser
                .map { it.hendelse }
                .filterIsInstance<T>()
                .singleOrNull()
        assertNotNull(melding)
        assertBlock(melding)
    }

    inline fun <reified T : UtgåendeHendelse> assertIkkeUtgåendeHendelse() {
        val meldingPubliserer = InMemoryMeldingPubliserer()
        outbox.sendAlle(meldingPubliserer)
        val melding =
            meldingPubliserer.publiserteUtgåendeHendelser
                .map { it.hendelse }
                .filterIsInstance<T>()
                .singleOrNull()
        assertNull(melding)
    }
}

package no.nav.helse.spesialist.db.dao

import no.nav.helse.spesialist.db.AbstractDBIntegrationTest
import no.nav.helse.spesialist.domain.Fødselsnummer
import no.nav.helse.spesialist.domain.testfixtures.lagFødselsnummer
import kotlin.test.Test
import kotlin.test.assertEquals

class PgPersonPseudoIdDaoTest : AbstractDBIntegrationTest() {

    @Test
    fun `generer og få tilbake id`() {
        val dao = PgPersonPseudoIdDao(session)
        val identitetsnummer = lagFødselsnummer()
        val personPseudoId = dao.nyPersonPseudoId(Fødselsnummer(identitetsnummer))
        assertEquals(identitetsnummer, dao.hentIdentitetsnummer(personPseudoId)?.value)
    }
}

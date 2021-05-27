package no.nav.helse.person

import no.nav.helse.DatabaseIntegrationTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class PersonsnapshotDaoTest: DatabaseIntegrationTest() {

    @Test
    fun `finn fnr fra aktørid`() {
        nyVedtaksperiode()
        assertEquals(FØDSELSNUMMER, personsnapshotDao.finnFnrByAktørId(AKTØRID))
    }

    @Test
    fun `finn fnr fra vedtaksperiodeid`() {
        val (id, _, _) = PERIODE
        nyVedtaksperiode()
        assertEquals(FØDSELSNUMMER, personsnapshotDao.finnFnrByVedtaksperiodeId(id))
    }
}

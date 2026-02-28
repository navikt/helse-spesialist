package no.nav.helse.spesialist.db.repository

import no.nav.helse.spesialist.application.testing.assertEqualsByMicrosecond
import no.nav.helse.spesialist.db.AbstractDBIntegrationTest
import no.nav.helse.spesialist.domain.Person
import no.nav.helse.spesialist.domain.Personinfo
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagAktørId
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagEtternavn
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagFornavn
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagFødselsdato
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagIdentitetsnummer
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagMellomnavn
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class PgPersonRepositoryTest : AbstractDBIntegrationTest() {
    @Test
    fun `lagre og finn minimal person`() {
        // given
        val person = Person.Factory.ny(lagIdentitetsnummer(), lagAktørId(), null, null)

        // when
        sessionContext.personRepository.lagre(person)

        // then
        val funnet = sessionContext.personRepository.finn(person.id)
        assertNotNull(funnet)
        assertEquals(person.id, funnet.id)
        assertEquals(person.aktørId, funnet.aktørId)
        assertEquals(null, funnet.egenAnsattStatus)
        assertEquals(null, funnet.enhetRef)
        assertEquals(null, funnet.enhetRefOppdatert)
        assertEquals(null, funnet.info)
        assertEquals(null, funnet.infoOppdatert)
        assertEquals(null, funnet.infotrygdutbetalingerRef)
        assertEquals(null, funnet.infotrygdutbetalingerOppdatert)
    }

    @Test
    fun `lagre og finn person`() {
        // given
        val person = Person.Factory.ny(lagIdentitetsnummer(), lagAktørId(), null, null)
        val personinfo =
            Personinfo(
                lagFornavn(),
                lagMellomnavn(),
                lagEtternavn(),
                lagFødselsdato(),
                Personinfo.Kjønn.Ukjent,
                adressebeskyttelse = Personinfo.Adressebeskyttelse.Ugradert,
            )
        person.oppdaterInfo(personinfo)
        val egenAnsattStatusOppdatert = Instant.now()
        person.oppdaterEgenAnsattStatus(true, egenAnsattStatusOppdatert)

        // when
        sessionContext.personRepository.lagre(person)

        // then
        val funnet = sessionContext.personRepository.finn(person.id)
        assertNotNull(funnet)
        assertEquals(person.id, funnet.id)
        assertEquals(person.aktørId, funnet.aktørId)
        assertEquals(true, funnet.egenAnsattStatus?.erEgenAnsatt)
        assertEqualsByMicrosecond(egenAnsattStatusOppdatert, funnet.egenAnsattStatus?.oppdatertTidspunkt)
        assertEquals(person.enhetRef, funnet.enhetRef)
        assertEquals(person.enhetRefOppdatert, funnet.enhetRefOppdatert)
        assertEquals(personinfo, funnet.info)
        assertEquals(person.infoOppdatert, funnet.infoOppdatert)
        assertEquals(person.infotrygdutbetalingerRef, funnet.infotrygdutbetalingerRef)
        assertEquals(person.infotrygdutbetalingerOppdatert, funnet.infotrygdutbetalingerOppdatert)
    }

    @Test
    fun `oppdater person`() {
        // given
        val person = Person.Factory.ny(lagIdentitetsnummer(), lagAktørId(), null, null)
        sessionContext.personRepository.lagre(person)

        // when
        val personinfo =
            Personinfo(
                lagFornavn(),
                lagMellomnavn(),
                lagEtternavn(),
                lagFødselsdato(),
                Personinfo.Kjønn.Ukjent,
                adressebeskyttelse = Personinfo.Adressebeskyttelse.Ugradert,
            )
        person.oppdaterInfo(personinfo)
        val egenAnsattStatusOppdatert = Instant.now()
        person.oppdaterEgenAnsattStatus(true, egenAnsattStatusOppdatert)
        sessionContext.personRepository.lagre(person)

        // then
        val funnet = sessionContext.personRepository.finn(person.id)
        assertNotNull(funnet)
        assertEquals(person.id, funnet.id)
        assertEquals(person.aktørId, funnet.aktørId)
        assertEquals(true, funnet.egenAnsattStatus?.erEgenAnsatt)
        assertEqualsByMicrosecond(egenAnsattStatusOppdatert, funnet.egenAnsattStatus?.oppdatertTidspunkt)
        assertEquals(person.enhetRef, funnet.enhetRef)
        assertEquals(person.enhetRefOppdatert, funnet.enhetRefOppdatert)
        assertEquals(personinfo, funnet.info)
        assertEquals(person.infoOppdatert, funnet.infoOppdatert)
        assertEquals(person.infotrygdutbetalingerRef, funnet.infotrygdutbetalingerRef)
        assertEquals(person.infotrygdutbetalingerOppdatert, funnet.infotrygdutbetalingerOppdatert)
    }
}

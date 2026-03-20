package no.nav.helse.spesialist.db.repository

import no.nav.helse.spesialist.application.testing.assertEqualsByMicrosecond
import no.nav.helse.spesialist.db.AbstractDBIntegrationTest
import no.nav.helse.spesialist.domain.PåVent
import java.time.Instant
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class PgPåVentRepositoryTest : AbstractDBIntegrationTest() {
    private val person = opprettPerson()
    private val arbeidsgiver = opprettArbeidsgiver()
    private val vedtaksperiode = opprettVedtaksperiode(person, arbeidsgiver)
    private val saksbehandler = opprettSaksbehandler()
    private val dialog = opprettDialog()
    private val repository = sessionContext.påVentRepository

    @Test
    fun `får tildelt id ved lagring`() {
        val påVent =
            PåVent.Factory.ny(
                vedtaksperiodeId = vedtaksperiode.id,
                saksbehandlerOid = saksbehandler.id,
                frist = LocalDate.now().plusDays(1),
                dialogRef = dialog.id(),
                årsaker = listOf("En årsak"),
                notattekst = "notattekst",
            )
        repository.lagre(påVent)
        assertTrue(påVent.harFåttTildeltId())
    }

    @Test
    fun `lagre og finn`() {
        val påVent =
            PåVent.Factory.ny(
                vedtaksperiodeId = vedtaksperiode.id,
                saksbehandlerOid = saksbehandler.id,
                frist = LocalDate.now().plusDays(1),
                dialogRef = dialog.id(),
                årsaker = listOf("En årsak"),
                notattekst = "notattekst",
            )
        repository.lagre(påVent)
        val funnet = repository.finnFor(vedtaksperiode.id)
        assertNotNull(funnet)
        assertEquals(påVent, funnet)
        assertEquals(påVent.vedtaksperiodeId, funnet.vedtaksperiodeId)
        assertEquals(påVent.saksbehandlerOid, funnet.saksbehandlerOid)
        assertEquals(påVent.frist, funnet.frist)
        assertEquals(påVent.dialogRef, funnet.dialogRef)
        assertEquals(påVent.årsaker, funnet.årsaker)
        assertEquals(påVent.notattekst, funnet.notattekst)
        assertEqualsByMicrosecond(påVent.opprettetTidspunkt, funnet.opprettetTidspunkt)
    }

    @Test
    fun oppdater() {
        // given
        val annenSaksbehandler = opprettSaksbehandler()
        val opprinneligPåVent =
            PåVent.Factory.ny(
                vedtaksperiodeId = vedtaksperiode.id,
                saksbehandlerOid = saksbehandler.id,
                frist = LocalDate.now().plusDays(1),
                dialogRef = dialog.id(),
                årsaker = listOf("En årsak"),
                notattekst = "notattekst",
            )
        repository.lagre(opprinneligPåVent)

        // when
        val oppdatertPåVent =
            PåVent.Factory.fraLagring(
                id = opprinneligPåVent.id(),
                vedtaksperiodeId = vedtaksperiode.id,
                saksbehandlerOid = annenSaksbehandler.id,
                frist = LocalDate.now().plusDays(2),
                årsaker = listOf("En annen årsak"),
                notattekst = "En annen notattekst",
                opprettetTidspunkt = Instant.now(),
                dialogRef = dialog.id(),
            )
        repository.lagre(oppdatertPåVent)

        // then
        val funnet = repository.finnFor(vedtaksperiode.id)
        assertNotNull(funnet)
        assertEquals(opprinneligPåVent, funnet)
        assertEquals(opprinneligPåVent.vedtaksperiodeId, funnet.vedtaksperiodeId)
        assertEquals(oppdatertPåVent.saksbehandlerOid, funnet.saksbehandlerOid)
        assertEquals(oppdatertPåVent.frist, funnet.frist)
        assertEquals(oppdatertPåVent.dialogRef, funnet.dialogRef)
        assertEquals(oppdatertPåVent.årsaker, funnet.årsaker)
        assertEquals(oppdatertPåVent.notattekst, funnet.notattekst)
        assertEqualsByMicrosecond(opprinneligPåVent.opprettetTidspunkt, funnet.opprettetTidspunkt)
    }
}

package no.nav.helse.modell.vedtaksperiode

import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.januar
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.varsel.VarselRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class InvaliderUtbetalingForGenerasjonerCommandTest {
    private val utbetalingId = UUID.randomUUID()

    private fun command(generasjoner: List<Generasjon>) = InvaliderUtbetalingForGenerasjonerCommand(utbetalingId, generasjoner)

    @Test
    fun `invaliderer utbetalingId`() {
        val generasjon = nyGenerasjon()
        generasjon.registrer(observer)
        generasjon.håndterNyUtbetaling(UUID.randomUUID(), utbetalingId, varselRepository)
        assertEquals(1, observer.kobledeUtbetalinger.size)
        command(listOf(generasjon)).execute(CommandContext(UUID.randomUUID()))
        assertEquals(1, observer.forkastedeUtbetalinger.size)
    }

    @Test
    fun `invaliderer utbetalingId for flere generasjoner`() {
        val generasjon1 = nyGenerasjon()
        val generasjon2 = nyGenerasjon()
        generasjon1.registrer(observer)
        generasjon2.registrer(observer)
        generasjon1.håndterNyUtbetaling(UUID.randomUUID(), utbetalingId, varselRepository)
        generasjon2.håndterNyUtbetaling(UUID.randomUUID(), utbetalingId, varselRepository)
        assertEquals(2, observer.kobledeUtbetalinger.size)
        command(listOf(generasjon1, generasjon2)).execute(CommandContext(UUID.randomUUID()))
        assertEquals(2, observer.forkastedeUtbetalinger.size)
    }

    private val observer = object : IVedtaksperiodeObserver {
        val kobledeUtbetalinger = mutableMapOf<UUID, UUID>()
        val forkastedeUtbetalinger = mutableMapOf<UUID, UUID>()
        override fun nyUtbetaling(generasjonId: UUID, utbetalingId: UUID) {
            kobledeUtbetalinger[generasjonId] = utbetalingId
        }

        override fun utbetalingForkastet(generasjonId: UUID, utbetalingId: UUID) {
            forkastedeUtbetalinger[generasjonId] = utbetalingId
        }
    }

    private fun nyGenerasjon() = Generasjon(
        id = UUID.randomUUID(),
        vedtaksperiodeId = UUID.randomUUID(),
        fom = 1.januar,
        tom = 31.januar,
        skjæringstidspunkt = 1.januar
    )


    private val varselRepository get() = object : VarselRepository {
        override fun godkjennFor(vedtaksperiodeId: UUID, generasjonId: UUID, varselkode: String, ident: String, definisjonId: UUID?):Unit = TODO("Not yet implemented")
        override fun avvisFor(vedtaksperiodeId: UUID, generasjonId: UUID, varselkode: String, ident: String, definisjonId: UUID?):Unit = TODO("Not yet implemented")
        override fun lagreDefinisjon(id: UUID, varselkode: String, tittel: String, forklaring: String?, handling: String?, avviklet: Boolean, opprettet: LocalDateTime):Unit = TODO("Not yet implemented")
        override fun oppdaterGenerasjonFor(id: UUID, gammelGenerasjonId: UUID, nyGenerasjonId: UUID):Unit = TODO("Not yet implemented")
    }
}
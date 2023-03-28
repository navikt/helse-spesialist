package no.nav.helse.modell.vedtaksperiode

import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.varsel.VarselRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class InvaliderUtbetalingForGenerasjonerCommandTest {
    private val generasjoner = mutableMapOf<UUID, Generasjon>()
    private val generasjonerMedUtbetaling = mutableMapOf<UUID, UUID>()
    private val generasjonerSomHarFåttFjernetUtbetaling = mutableListOf<UUID>()
    private val utbetalingId = UUID.randomUUID()

    private val command = InvaliderUtbetalingForGenerasjonerCommand(utbetalingId, generasjonRepository)

    @Test
    fun `invaliderer utbetalingId`() {
        val generasjon = generasjonRepository.opprettFørste(UUID.randomUUID(), UUID.randomUUID())
        generasjon.håndterNyUtbetaling(UUID.randomUUID(), utbetalingId, varselRepository)
        assertEquals(1, generasjonerMedUtbetaling.size)
        command.execute(CommandContext(UUID.randomUUID()))
        assertEquals(0, generasjonerMedUtbetaling.size)
        assertEquals(1, generasjonerSomHarFåttFjernetUtbetaling.size)
    }
    @Test
    fun `invaliderer utbetalingId for flere generasjoner`() {
        val generasjon1 = generasjonRepository.opprettFørste(UUID.randomUUID(), UUID.randomUUID())
        val generasjon2 = generasjonRepository.opprettFørste(UUID.randomUUID(), UUID.randomUUID())
        generasjon1.håndterNyUtbetaling(UUID.randomUUID(), utbetalingId, varselRepository)
        generasjon2.håndterNyUtbetaling(UUID.randomUUID(), utbetalingId, varselRepository)
        assertEquals(2, generasjonerMedUtbetaling.size)
        command.execute(CommandContext(UUID.randomUUID()))
        assertEquals(0, generasjonerMedUtbetaling.size)
        assertEquals(2, generasjonerSomHarFåttFjernetUtbetaling.size)
    }

    private val generasjonRepository get() = object : GenerasjonRepository {
        override fun låsFor(generasjonId: UUID, hendelseId: UUID):Unit = TODO("Not yet implemented")
        override fun sisteFor(vedtaksperiodeId: UUID): Generasjon = TODO("Not yet implemented")

        override fun opprettFørste(vedtaksperiodeId: UUID, hendelseId: UUID, id: UUID): Generasjon {
            val generasjonId = UUID.randomUUID()
            return Generasjon(generasjonId, vedtaksperiodeId, this).also {
                generasjoner[generasjonId] = it
            }
        }
        override fun utbetalingFor(generasjonId: UUID, utbetalingId: UUID) {
            generasjonerMedUtbetaling[generasjonId] = utbetalingId
        }
        override fun tilhørendeFor(utbetalingId: UUID): List<Generasjon> {
            return generasjonerMedUtbetaling
                .filterValues { it == utbetalingId }
                .map { (generasjonId, _) -> generasjoner.getValue(generasjonId) }
        }
        override fun fjernUtbetalingFor(generasjonId: UUID) {
            generasjonerMedUtbetaling.remove(generasjonId)
            generasjonerSomHarFåttFjernetUtbetaling.add(generasjonId)
        }

        override fun finnVedtaksperioder(vedtaksperiodeIder: List<UUID>): List<Vedtaksperiode> {
            TODO("Not yet implemented")
        }
    }

    private val varselRepository get() = object : VarselRepository {
        override fun deaktiverFor(vedtaksperiodeId: UUID, generasjonId: UUID, varselkode: String, definisjonId: UUID?) {
            TODO("Not yet implemented")
        }

        override fun reaktiverFor(vedtaksperiodeId: UUID, generasjonId: UUID, varselkode: String) {
            TODO("Not yet implemented")
        }

        override fun godkjennFor(
            vedtaksperiodeId: UUID,
            generasjonId: UUID,
            varselkode: String,
            ident: String,
            definisjonId: UUID?,
        ) {
            TODO("Not yet implemented")
        }

        override fun avvisFor(
            vedtaksperiodeId: UUID,
            generasjonId: UUID,
            varselkode: String,
            ident: String,
            definisjonId: UUID?,
        ) {
            TODO("Not yet implemented")
        }

        override fun lagreVarsel(
            id: UUID,
            generasjonId: UUID,
            varselkode: String,
            opprettet: LocalDateTime,
            vedtaksperiodeId: UUID,
        ) {
            TODO("Not yet implemented")
        }

        override fun lagreDefinisjon(
            id: UUID,
            varselkode: String,
            tittel: String,
            forklaring: String?,
            handling: String?,
            avviklet: Boolean,
            opprettet: LocalDateTime,
        ) {
            TODO("Not yet implemented")
        }

        override fun oppdaterGenerasjonFor(id: UUID, gammelGenerasjonId: UUID, nyGenerasjonId: UUID) {
            TODO("Not yet implemented")
        }

    }
}
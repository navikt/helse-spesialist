package no.nav.helse.modell.vedtaksperiode

import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.modell.vedtaksperiode.vedtak.Sykepengevedtak
import org.junit.jupiter.api.Assertions.assertEquals

internal class GenerasjonTestObserver: IVedtaksperiodeObserver {

    private val tilstandsendringer = mutableMapOf<UUID, MutableList<Pair<Generasjon.Tilstand, Generasjon.Tilstand>>>()
    val utbetalingerPåGenerasjoner = mutableMapOf<UUID, UUID?>()
    val opprettedeVarsler = mutableMapOf<UUID, MutableList<String>>()
    val vedtakFattet = mutableListOf<Sykepengevedtak>()

    override fun vedtakFattet(sykepengevedtak: Sykepengevedtak) {
        vedtakFattet.add(sykepengevedtak)
    }

    override fun tilstandEndret(
        generasjonId: UUID,
        vedtaksperiodeId: UUID,
        gammel: Generasjon.Tilstand,
        ny: Generasjon.Tilstand,
        hendelseId: UUID
    ) {
        tilstandsendringer.getOrPut(generasjonId) { mutableListOf() }.add(gammel to ny)
    }

    override fun varselOpprettet(
        varselId: UUID,
        vedtaksperiodeId: UUID,
        generasjonId: UUID,
        varselkode: String,
        opprettet: LocalDateTime
    ) {
        opprettedeVarsler.getOrPut(generasjonId) { mutableListOf() }.add(varselkode)
    }

    fun assertUtbetaling(
        generasjonId: UUID,
        forventetUtbetalingId: UUID?
    ) {
        assertEquals(forventetUtbetalingId, utbetalingerPåGenerasjoner[generasjonId])
    }

    fun assertTilstandsendring(
        generasjonId: UUID,
        forventetGammel: Generasjon.Tilstand,
        forventetNy: Generasjon.Tilstand,
        index: Int
    ) {
        val (gammel, ny) = tilstandsendringer[generasjonId]!![index]
        assertEquals(forventetGammel, gammel)
        assertEquals(forventetNy, ny)
    }

    fun assertGjeldendeTilstand(generasjonId: UUID, forventetTilstand: Generasjon.Tilstand) {
        val tilstand = tilstandsendringer[generasjonId]?.last()
        assertEquals(forventetTilstand, tilstand?.second)
    }
}
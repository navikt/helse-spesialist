package no.nav.helse.modell.vedtaksperiode

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull

internal class GenerasjonTestObserver: IVedtaksperiodeObserver {
    internal class Tidslinjeendring(
        val fom: LocalDate,
        val tom: LocalDate,
        val skjæringstidspunkt: LocalDate
    )

    internal class Opprettelse(
        val generasjonId: UUID,
        val vedtaksperiodeId: UUID,
        val hendelseId: UUID,
        val fom: LocalDate?,
        val tom: LocalDate?,
        val skjæringstidspunkt: LocalDate?
    )

    val låsteGenerasjoner = mutableListOf<UUID>()
    val tilstandsendringer = mutableMapOf<UUID, MutableList<Pair<Generasjon.Tilstand, Generasjon.Tilstand>>>()
    val utbetalingerPåGenerasjoner = mutableMapOf<UUID, UUID?>()
    val opprettedeGenerasjoner = mutableMapOf<UUID, Opprettelse>()
    val oppdaterteGenerasjoner = mutableMapOf<UUID, Tidslinjeendring>()
    val opprettedeVarsler = mutableMapOf<UUID, MutableList<String>>()
    val godkjenteVarsler = mutableListOf<UUID>()
    val avvisteVarsler = mutableListOf<UUID>()

    override fun vedtakFattet(generasjonId: UUID, hendelseId: UUID) {
        låsteGenerasjoner.add(generasjonId)
    }

    override fun nyUtbetaling(generasjonId: UUID, utbetalingId: UUID) {
        utbetalingerPåGenerasjoner[generasjonId] = utbetalingId
    }

    override fun utbetalingForkastet(generasjonId: UUID, utbetalingId: UUID) {
        utbetalingerPåGenerasjoner[generasjonId] = null
    }

    override fun generasjonOpprettet(
        generasjonId: UUID,
        vedtaksperiodeId: UUID,
        hendelseId: UUID,
        fom: LocalDate,
        tom: LocalDate,
        skjæringstidspunkt: LocalDate,
        tilstand: Generasjon.Tilstand
    ) {
        opprettedeGenerasjoner[generasjonId] =
            Opprettelse(generasjonId, vedtaksperiodeId, hendelseId, fom, tom, skjæringstidspunkt)
    }

    override fun tidslinjeOppdatert(
        generasjonId: UUID,
        fom: LocalDate,
        tom: LocalDate,
        skjæringstidspunkt: LocalDate
    ) {
        oppdaterteGenerasjoner[generasjonId] = Tidslinjeendring(fom, tom, skjæringstidspunkt)
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

    override fun varselGodkjent(
        varselId: UUID,
        vedtaksperiodeId: UUID,
        generasjonId: UUID,
        varselkode: String,
        ident: String
    ) {
        godkjenteVarsler.add(varselId)
    }

    override fun varselAvvist(
        varselId: UUID,
        vedtaksperiodeId: UUID,
        generasjonId: UUID,
        varselkode: String,
        ident: String
    ) {
        avvisteVarsler.add(varselId)
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

    fun assertTidslinjeendring(
        generasjonId: UUID,
        forventetFom: LocalDate,
        forventetTom: LocalDate,
        forventetSkjæringstidspunkt: LocalDate
    ) {
        val tidslinjeendring = oppdaterteGenerasjoner[generasjonId]
        assertEquals(forventetFom, tidslinjeendring?.fom)
        assertEquals(forventetTom, tidslinjeendring?.tom)
        assertEquals(forventetSkjæringstidspunkt, tidslinjeendring?.skjæringstidspunkt)
    }

    fun assertOpprettelse(
        forventetGenerasjonId: UUID,
        forventetVedtaksperiodeId: UUID,
        forventetHendelseId: UUID,
        forventetFom: LocalDate,
        forventetTom: LocalDate,
        forventetSkjæringstidspunkt: LocalDate
    ) {
        val opprettelse = opprettedeGenerasjoner[forventetGenerasjonId]
        assertNotNull(opprettelse)
        requireNotNull(opprettelse)
        assertEquals(forventetGenerasjonId, opprettelse.generasjonId)
        assertEquals(forventetVedtaksperiodeId, opprettelse.vedtaksperiodeId)
        assertEquals(forventetHendelseId, opprettelse.hendelseId)
        assertEquals(forventetFom, opprettelse.fom)
        assertEquals(forventetTom, opprettelse.tom)
        assertEquals(forventetSkjæringstidspunkt, opprettelse.skjæringstidspunkt)
    }

    fun assertOpprettelse(
        forventetVedtaksperiodeId: UUID,
        forventetHendelseId: UUID,
        forventetFom: LocalDate,
        forventetTom: LocalDate,
        forventetSkjæringstidspunkt: LocalDate
    ) {
        val opprettelser = opprettedeGenerasjoner.values.filter { it.vedtaksperiodeId == forventetVedtaksperiodeId }
        assertEquals(1, opprettelser.size)
        val opprettelse = opprettelser[0]
        assertEquals(forventetVedtaksperiodeId, opprettelse.vedtaksperiodeId)
        assertEquals(forventetHendelseId, opprettelse.hendelseId)
        assertEquals(forventetFom, opprettelse.fom)
        assertEquals(forventetTom, opprettelse.tom)
        assertEquals(forventetSkjæringstidspunkt, opprettelse.skjæringstidspunkt)
    }

    fun assertGjeldendeTilstand(generasjonId: UUID, forventetTilstand: Generasjon.Tilstand) {
        val tilstand = tilstandsendringer[generasjonId]?.last()
        assertEquals(forventetTilstand, tilstand?.second)
    }
}
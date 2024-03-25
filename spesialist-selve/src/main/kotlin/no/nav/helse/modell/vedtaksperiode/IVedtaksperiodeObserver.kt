package no.nav.helse.modell.vedtaksperiode

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.modell.person.PersonObserver

internal interface IVedtaksperiodeObserver: PersonObserver {

    fun generasjonOpprettet(
        generasjonId: UUID,
        vedtaksperiodeId: UUID,
        hendelseId: UUID,
        fom: LocalDate,
        tom: LocalDate,
        skjæringstidspunkt: LocalDate,
        tilstand: Generasjon.Tilstand
    ) {}
    fun nyUtbetaling(generasjonId: UUID, utbetalingId: UUID) {}
    fun utbetalingForkastet(generasjonId: UUID, utbetalingId: UUID) {}
    fun varselOpprettet(varselId: UUID, vedtaksperiodeId: UUID, generasjonId: UUID, varselkode: String, opprettet: LocalDateTime) {}
    fun varselReaktivert(varselId: UUID, varselkode: String, generasjonId: UUID, vedtaksperiodeId: UUID) {}
    fun varselDeaktivert(varselId: UUID, varselkode: String, generasjonId: UUID, vedtaksperiodeId: UUID) {}
    fun varselSlettet(varselId: UUID, varselkode: String, generasjonId: UUID, vedtaksperiodeId: UUID) {}
    fun varselGodkjent(varselId: UUID, varselkode: String, generasjonId: UUID, vedtaksperiodeId: UUID, statusEndretAv: String) {}
    fun varselFlyttet(varselId: UUID, gammelGenerasjonId: UUID, nyGenerasjonId: UUID) {}
    fun tilstandEndret(generasjonId: UUID, vedtaksperiodeId: UUID, gammel: Generasjon.Tilstand, ny: Generasjon.Tilstand, hendelseId: UUID) {}
}
package no.nav.helse.modell.vedtaksperiode

import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.modell.person.PersonObserver

internal interface IVedtaksperiodeObserver: PersonObserver {
    fun varselOpprettet(varselId: UUID, vedtaksperiodeId: UUID, generasjonId: UUID, varselkode: String, opprettet: LocalDateTime) {}
    fun varselReaktivert(varselId: UUID, varselkode: String, generasjonId: UUID, vedtaksperiodeId: UUID) {}
    fun varselDeaktivert(varselId: UUID, varselkode: String, generasjonId: UUID, vedtaksperiodeId: UUID) {}
    fun varselGodkjent(varselId: UUID, varselkode: String, generasjonId: UUID, vedtaksperiodeId: UUID, statusEndretAv: String) {}
    fun tilstandEndret(generasjonId: UUID, vedtaksperiodeId: UUID, gammel: Generasjon.Tilstand, ny: Generasjon.Tilstand, hendelseId: UUID) {}
}
package no.nav.helse.db.api

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import no.nav.helse.spesialist.api.notat.KommentarDto
import java.time.LocalDateTime
import java.util.UUID

interface NotatApiDao {
    fun opprettNotat(
        vedtaksperiodeId: UUID,
        tekst: String,
        saksbehandlerOid: UUID,
        type: NotatType = NotatType.Generelt,
    ): NotatDto?

    fun leggTilKommentar(
        dialogRef: Int,
        tekst: String,
        saksbehandlerident: String,
    ): KommentarDto?

    // PåVent-notater og Retur-notater lagres nå i periodehistorikk, og skal ikke være med til speil som en del av notater
    fun finnNotater(vedtaksperiodeId: UUID): List<NotatDto>

    fun feilregistrerNotat(notatId: Int): NotatDto?

    fun feilregistrerKommentar(kommentarId: Int): KommentarDto?

    fun finnKommentarer(dialogRef: Long): List<KommentarDto>

    @JsonIgnoreProperties
    data class NotatDto(
        val id: Int,
        val dialogRef: Int,
        val tekst: String,
        val type: NotatType,
        val opprettet: LocalDateTime,
        val saksbehandlerOid: UUID,
        val saksbehandlerNavn: String,
        val saksbehandlerEpost: String,
        val saksbehandlerIdent: String,
        val vedtaksperiodeId: UUID,
        val feilregistrert: Boolean,
        val feilregistrert_tidspunkt: LocalDateTime?,
        val kommentarer: List<KommentarDto>,
    )

    enum class NotatType {
        Retur,
        Generelt,
        PaaVent,
        OpphevStans,
    }
}

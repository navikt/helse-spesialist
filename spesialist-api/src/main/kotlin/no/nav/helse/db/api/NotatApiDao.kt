package no.nav.helse.db.api

import no.nav.helse.spesialist.api.graphql.schema.NotatType
import no.nav.helse.spesialist.api.notat.KommentarDto
import no.nav.helse.spesialist.api.notat.NotatDto
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
}

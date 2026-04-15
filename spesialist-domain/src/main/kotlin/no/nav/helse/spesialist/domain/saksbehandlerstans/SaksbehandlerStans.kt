package no.nav.helse.spesialist.domain.saksbehandlerstans

import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.NAVIdent
import no.nav.helse.spesialist.domain.ddd.AggregateRoot
import no.nav.helse.spesialist.domain.ddd.ValueObject
import java.time.Instant
import java.util.UUID

@JvmInline
value class SaksbehandlerStansId(
    val value: UUID,
) : ValueObject

class SaksbehandlerStans private constructor(
    id: SaksbehandlerStansId,
    val identitetsnummer: Identitetsnummer,
    val utførtAv: NAVIdent,
    val begrunnelse: String,
    val opprettet: Instant,
    stansOpphevet: StansOpphevet?,
) : AggregateRoot<SaksbehandlerStansId>(id) {
    var stansOpphevet: StansOpphevet? = stansOpphevet
        private set

    val erStanset: Boolean get() = stansOpphevet == null

    fun opphevStans(
        utførtAvSaksbehandlerIdent: NAVIdent,
        begrunnelse: String,
    ) {
        check(erStanset) { "Prøvde å oppheve stans som ikke er stanset!" }
        stansOpphevet =
            StansOpphevet(
                utførtAv = utførtAvSaksbehandlerIdent,
                begrunnelse = begrunnelse,
                tidspunkt = Instant.now(),
            )
    }

    data class StansOpphevet(
        val utførtAv: NAVIdent,
        val begrunnelse: String,
        val tidspunkt: Instant,
    )

    companion object {
        fun ny(
            utførtAvSaksbehandlerIdent: NAVIdent,
            begrunnelse: String,
            identitetsnummer: Identitetsnummer,
        ) = SaksbehandlerStans(
            id = SaksbehandlerStansId(UUID.randomUUID()),
            identitetsnummer = identitetsnummer,
            utførtAv = utførtAvSaksbehandlerIdent,
            begrunnelse = begrunnelse,
            opprettet = Instant.now(),
            stansOpphevet = null,
        )

        fun fraLagring(
            id: SaksbehandlerStansId,
            identitetsnummer: Identitetsnummer,
            utførtAv: NAVIdent,
            begrunnelse: String,
            opprettet: Instant,
            stansOpphevet: StansOpphevet?,
        ) = SaksbehandlerStans(
            id = id,
            identitetsnummer = identitetsnummer,
            utførtAv = utførtAv,
            begrunnelse = begrunnelse,
            opprettet = opprettet,
            stansOpphevet = stansOpphevet,
        )
    }
}

package no.nav.helse.spesialist.domain.andreytelser

import no.nav.helse.modell.vedtaksperiode.Yrkesaktivitetstype
import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.NAVIdent
import no.nav.helse.spesialist.domain.ddd.AggregateRoot
import no.nav.helse.spesialist.domain.ddd.ValueObject
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid

data class GraderteAndreYtelserId(
    val identitetsnummer: Identitetsnummer,
    val yrkesaktivitet: Yrkesaktivitetstype,
    val organisasjonsnummer: String?,
) : ValueObject

class GraderteAndreYtelser private constructor(
    id: GraderteAndreYtelserId,
) : AggregateRoot<GraderteAndreYtelserId>(id) {
    private val _events = mutableListOf<GraderteAndreYtelserEvent>()
    val events: List<GraderteAndreYtelserEvent> get() = _events

    private var dagerPerDato: Map<LocalDate, GraderteAndreYtelserDag> = emptyMap()
    val dager: List<GraderteAndreYtelserDag> get() = dagerPerDato.values.sortedBy { it.dato }

    val versjon: UUID get() = _events.last().versjon

    fun endre(
        dager: List<GraderteAndreYtelserDag>,
        saksbehandlerIdent: NAVIdent,
        notatTilBeslutter: String,
    ): GraderteAndreYtelserEvent? {
        val dagerMedEndring = dager.filter { dag -> dagerPerDato[dag.dato] != dag }
        if (dagerMedEndring.isEmpty()) return null

        val event =
            GraderteAndreYtelserEvent(
                versjon = nyVersjon(),
                tidspunkt = Instant.now(),
                utførtAvSaksbehandlerIdent = saksbehandlerIdent,
                notatTilBeslutter = notatTilBeslutter,
                dager = dagerMedEndring.map { dag -> dag.endretFra(dagerPerDato[dag.dato]) },
            )
        apply(event)
        return event
    }

    fun finnEvent(versjon: UUID): GraderteAndreYtelserEvent? = _events.find { it.versjon == versjon }

    private fun apply(event: GraderteAndreYtelserEvent) {
        dagerPerDato = dagerPerDato + event.dager.map { it.tilGraderteAndreYtelserDag() }.associateBy { it.dato }
        _events.add(event)
    }

    private fun GraderteAndreYtelserDag.endretFra(fra: GraderteAndreYtelserDag?) =
        GraderteAndreYtelserEndretDag(
            dato = dato,
            andreYtelserGrader = andreYtelserGrader,
            sykdomsgrad = sykdomsgrad,
            arbeidsgrad = arbeidsgrad,
            fraAndreYtelserGrader = fra?.andreYtelserGrader,
            fraSykdomsgrad = fra?.sykdomsgrad,
            fraArbeidsgrad = fra?.arbeidsgrad,
        )

    companion object {
        fun ny(
            id: GraderteAndreYtelserId,
            dager: List<GraderteAndreYtelserDag>,
            saksbehandlerIdent: NAVIdent,
            notatTilBeslutter: String,
        ): GraderteAndreYtelser =
            GraderteAndreYtelser(id).also {
                it.endre(dager, saksbehandlerIdent, notatTilBeslutter)
            }

        fun fraLagring(
            id: GraderteAndreYtelserId,
            events: List<GraderteAndreYtelserEvent>,
        ): GraderteAndreYtelser =
            GraderteAndreYtelser(id).also { graderteAndreYtelser ->
                events.sortedBy { it.versjon }.forEach(graderteAndreYtelser::apply)
            }

        @OptIn(ExperimentalUuidApi::class)
        private fun nyVersjon(): UUID = Uuid.generateV7().toJavaUuid()
    }
}

enum class GraderteAndreYtelserType {
    FORELDREPENGER,
    SVANGERSKAPSPENGER,
    OMSORGSPENGER,
    PLEIEPENGER,
    OPPLARINGSPENGER,
}

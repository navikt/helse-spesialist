package no.nav.helse.spleis.rest.hentperson

import java.time.LocalDate
import java.util.UUID

/**
 * Klient-side DTO-er for spleis sitt REST-endepunkt `POST /api/person`. Speiler
 * `no.nav.helse.spleis.rest.dto.*` i sykepenger-api-rest (spleis-repoet) felt for felt — se
 * `RestTilSnapshotMapping.kt` for hvordan disse mappes til `Snapshot*`-modellen.
 */
internal data class Person(
    val aktorId: String,
    val fodselsnummer: String,
    val arbeidsgivere: List<Arbeidsgiver>,
    val dodsdato: LocalDate?,
    val versjon: Int,
    val vilkarsgrunnlag: List<Vilkarsgrunnlag>,
)

internal data class Arbeidsgiver(
    val organisasjonsnummer: String,
    val generasjoner: List<Generasjon>,
    val ghostPerioder: List<GhostPeriode>,
)

internal data class Generasjon(
    val id: UUID,
    val perioder: List<Tidslinjeperiode>,
    val kildeTilGenerasjon: UUID,
)

internal data class GhostPeriode(
    val id: UUID,
    val fom: LocalDate,
    val tom: LocalDate,
    val skjaeringstidspunkt: LocalDate,
    val vilkarsgrunnlagId: UUID,
    val deaktivert: Boolean,
)

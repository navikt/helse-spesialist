@file:kotlinx.serialization.UseContextualSerialization(
    BigDecimal::class,
    Boolean::class,
    Instant::class,
    LocalDate::class,
    LocalDateTime::class,
    UUID::class,
)

package no.nav.helse.spesialist.api.rest.resources

import io.ktor.resources.Resource
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Resource("personer")
class Personer {
    @Resource("sok")
    class Sok(
        val parent: Personer = Personer(),
    )

    @Resource("{pseudoId}")
    class PersonPseudoId(
        val parent: Personer = Personer(),
        val pseudoId: String,
    ) {
        @Resource("opptegnelser")
        class Opptegnelser(
            val parent: PersonPseudoId,
            val etterSekvensnummer: Int,
        )

        @Resource("tilkomne-inntektskilder")
        class TilkomneInntektskilder(
            val parent: PersonPseudoId,
        )

        @Resource("vurderinger")
        class Vurderinger(
            val parent: PersonPseudoId,
        ) {
            @Resource("arbeidstid")
            class Arbeidstid(
                val parent: Vurderinger,
            )
        }

        @Resource("vurderte-inngangsvilkar")
        class VurderteInngangsvilkår(
            val parent: PersonPseudoId,
        ) {
            @Resource("{skjæringstidspunkt}")
            class Skjæringstidspunkt(
                val parent: VurderteInngangsvilkår,
                val skjæringstidspunkt: LocalDate,
            )
        }

        @Resource("dokumenter")
        class Dokumenter(
            val parent: PersonPseudoId,
        ) {
            @Resource("{dokumentId}")
            class DokumentId(
                val parent: Dokumenter,
                val dokumentId: UUID,
            ) {
                @Resource("soknad")
                class Soknad(
                    val parent: DokumentId,
                )

                @Resource("inntektsmelding")
                class Inntektsmelding(
                    val parent: DokumentId,
                )
            }
        }

        @Resource("krr-status")
        class KrrStatus(
            val parent: PersonPseudoId,
        )

        @Resource("krr-registrert-status")
        class KrrRegistrertStatus(
            val parent: PersonPseudoId,
        )
    }
}

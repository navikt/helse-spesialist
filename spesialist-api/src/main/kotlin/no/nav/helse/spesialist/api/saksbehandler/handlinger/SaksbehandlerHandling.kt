package no.nav.helse.spesialist.api.saksbehandler.handlinger

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.time.LocalDate
import no.nav.helse.spesialist.api.modell.Saksbehandler
import no.nav.helse.spesialist.api.modell.saksbehandling.hendelser.OverstyrtArbeidsgiver
import no.nav.helse.spesialist.api.modell.saksbehandling.hendelser.OverstyrtInntektOgRefusjon
import no.nav.helse.spesialist.api.modell.saksbehandling.hendelser.OverstyrtTidslinje
import no.nav.helse.spesialist.api.modell.saksbehandling.hendelser.OverstyrtTidslinjedag
import no.nav.helse.spesialist.api.modell.saksbehandling.hendelser.Refusjonselement
import no.nav.helse.spesialist.api.modell.saksbehandling.hendelser.Subsumsjon
import no.nav.helse.spesialist.api.overstyring.SubsumsjonDto

internal sealed interface SaksbehandlerHandling {
    fun loggnavn(): String
    fun utførAv(saksbehandler: Saksbehandler)
}

internal sealed interface OverstyringHandling: SaksbehandlerHandling {
    fun gjelderFødselsnummer(): String
}

@JsonIgnoreProperties
class OverstyrTidslinjeHandling(
    val organisasjonsnummer: String,
    val fødselsnummer: String,
    val aktørId: String,
    val begrunnelse: String,
    val dager: List<OverstyrDagDto>
): OverstyringHandling {

    private val overstyrtTidslinje get() = OverstyrtTidslinje(
        aktørId = aktørId,
        fødselsnummer = fødselsnummer,
        organisasjonsnummer = organisasjonsnummer,
        dager = dager.map { OverstyrtTidslinjedag(it.dato, it.type, it.fraType, it.grad, it.fraGrad) },
        begrunnelse = begrunnelse
    )

    override fun loggnavn(): String = "overstyr_tidslinje"

    override fun gjelderFødselsnummer() = fødselsnummer

    override fun utførAv(saksbehandler: Saksbehandler) {
        saksbehandler.håndter(overstyrtTidslinje)
    }

    @JsonIgnoreProperties
    class OverstyrDagDto(
        val dato: LocalDate,
        val type: String,
        val fraType: String,
        val grad: Int?,
        val fraGrad: Int?
    )
}

internal data class OverstyrInntektOgRefusjonHandling(
    val aktørId: String,
    val fødselsnummer: String,
    val skjæringstidspunkt: LocalDate,
    val arbeidsgivere: List<OverstyrArbeidsgiverDto>,
): OverstyringHandling {
    private val overstyrtInntektOgRefusjon get() = OverstyrtInntektOgRefusjon(
        aktørId = aktørId,
        fødselsnummer = fødselsnummer,
        skjæringstidspunkt = skjæringstidspunkt,
        arbeidsgivere = arbeidsgivere.map { overstyrArbeidsgiver ->
            OverstyrtArbeidsgiver(
                overstyrArbeidsgiver.organisasjonsnummer,
                overstyrArbeidsgiver.månedligInntekt,
                overstyrArbeidsgiver.fraMånedligInntekt,
                overstyrArbeidsgiver.refusjonsopplysninger?.map {
                    Refusjonselement(it.fom, it.tom, it.beløp)
                },
                overstyrArbeidsgiver.fraRefusjonsopplysninger?.map {
                    Refusjonselement(it.fom, it.tom, it.beløp)
                },
                begrunnelse = overstyrArbeidsgiver.begrunnelse,
                forklaring = overstyrArbeidsgiver.forklaring,
                subsumsjon = overstyrArbeidsgiver.subsumsjon?.let {
                    Subsumsjon(it.paragraf, it.ledd, it.bokstav)
                }
            )
        },
    )

    override fun loggnavn(): String = "overstyr_inntekt_og_refusjon"

    override fun gjelderFødselsnummer(): String = fødselsnummer

    override fun utførAv(saksbehandler: Saksbehandler) {
        saksbehandler.håndter(overstyrtInntektOgRefusjon)
    }

    internal data class OverstyrArbeidsgiverDto(
        val organisasjonsnummer: String,
        val månedligInntekt: Double,
        val fraMånedligInntekt: Double,
        val refusjonsopplysninger: List<RefusjonselementDto>?,
        val fraRefusjonsopplysninger: List<RefusjonselementDto>?,
        val begrunnelse: String,
        val forklaring: String,
        val subsumsjon: SubsumsjonDto?,
    ) {

        data class RefusjonselementDto(
            val fom: LocalDate,
            val tom: LocalDate? = null,
            val beløp: Double
        )
    }
}

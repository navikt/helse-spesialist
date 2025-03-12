package no.nav.helse.spesialist.api.testfixtures.mutation

import no.nav.helse.spesialist.api.graphql.schema.ApiArbeidsforholdOverstyringHandling
import no.nav.helse.spesialist.api.graphql.schema.ApiInntektOgRefusjonOverstyring
import no.nav.helse.spesialist.api.graphql.schema.ApiTidslinjeOverstyring
import no.nav.helse.spesialist.api.graphql.schema.ApiTilkommenInntektOverstyring
import org.intellij.lang.annotations.Language

internal fun asMutation(
    @Language("GraphQL") mutationString: String
): String = mutationString


fun overstyrTilkommenInntektMutation(overstyring: ApiTilkommenInntektOverstyring): String =
    asMutation(
        """mutation OverstyrTilkommenInntekt {
                overstyrTilkommenInntekt(
                    overstyring: {
                        fodselsnummer: "${overstyring.fodselsnummer}",
                        aktorId: "${overstyring.aktorId}",
                        begrunnelse: "Foobar",
                        fjernet: ${fjernet(overstyring)},
                        lagtTilEllerEndret: ${lagtTilEllerEndret(overstyring)},
                        vedtaksperiodeId: "${overstyring.vedtaksperiodeId}",
                    }
                )
            }
        """
    )

private fun lagtTilEllerEndret(overstyring: ApiTilkommenInntektOverstyring): List<String> =
    overstyring.lagtTilEllerEndret.map {
        fun perioder(): List<String> = it.perioder.map { periode ->
            """
                {
                    fom: "${periode.fom}",
                    tom: "${periode.tom}",
                    periodeBelop: ${periode.periodeBelop}
                }
            """
        }

        """
            {
                organisasjonsnummer: "${it.organisasjonsnummer}",
                perioder: ${perioder()}
            }
        """
    }

private fun fjernet(overstyring: ApiTilkommenInntektOverstyring): List<String> = overstyring.fjernet.map {
    fun perioder(): List<String> = it.perioder.map { periode ->
        """
            {
                fom: "${periode.fom}",
                tom: "${periode.tom}"
            }
        """
    }

    """
        {
            organisasjonsnummer: "${it.organisasjonsnummer}",
            perioder: ${perioder()}
        }
    """
}

fun overstyrInntektOgRefusjonMutation(overstyring: ApiInntektOgRefusjonOverstyring): String = asMutation(
    """
    mutation OverstyrInntektOgRefusjon {
        overstyrInntektOgRefusjon(overstyring: {
            aktorId: "${overstyring.aktorId}",
            skjaringstidspunkt: "${overstyring.skjaringstidspunkt}",
            fodselsnummer: "${overstyring.fodselsnummer}",
            arbeidsgivere: ${arbeidsgivere(overstyring)},
            vedtaksperiodeId: "${overstyring.vedtaksperiodeId}"
        })
    }
    """
)

private fun arbeidsgivere(overstyring: ApiInntektOgRefusjonOverstyring): List<String> = overstyring.arbeidsgivere.map {
    fun refusjonsopplysninger(): List<String>? = it.refusjonsopplysninger?.map { opplysning ->
        """ 
            {
                belop: ${opplysning.belop},
                fom: "${opplysning.fom}",
                tom: ${opplysning.tom?.let { tom -> """"$tom"""" }}
            } 
        """
    }

    fun fraRefusjonsopplysninger(): List<String>? = it.fraRefusjonsopplysninger?.map { opplysning ->
        """ 
            {
               belop: ${opplysning.belop},
               fom: "${opplysning.fom}",
               tom: ${opplysning.tom?.let { tom -> """"$tom"""" }}
            } 
        """
    }

    """ 
        {
            forklaring: "${it.forklaring}",
            begrunnelse: "${it.begrunnelse}",
            organisasjonsnummer: "${it.organisasjonsnummer}",
            fraManedligInntekt: ${it.fraManedligInntekt},
            manedligInntekt: ${it.manedligInntekt},
            lovhjemmel: {bokstav: "A", ledd: "Albue", lovverk: "Norske lover", lovverksversjon: "1803", paragraf: "8-30"},
            refusjonsopplysninger: ${refusjonsopplysninger()},
            fraRefusjonsopplysninger: ${fraRefusjonsopplysninger()}
        } 
    """
}

fun overstyrArbeidsforholdMutation(overstyring: ApiArbeidsforholdOverstyringHandling): String = asMutation(
    """
    mutation OverstyrArbeidsforhold {
        overstyrArbeidsforhold(overstyring: {
            aktorId: "${overstyring.aktorId}",
            skjaringstidspunkt: "${overstyring.skjaringstidspunkt}",
            fodselsnummer: "${overstyring.fodselsnummer}",
            overstyrteArbeidsforhold: ${overstyrteArbeidsforhold(overstyring)},
            vedtaksperiodeId: "${overstyring.vedtaksperiodeId}"
        })
    }
    """
)

private fun overstyrteArbeidsforhold(overstyring: ApiArbeidsforholdOverstyringHandling): List<String> =
    overstyring.overstyrteArbeidsforhold.map {
        """ 
            {
                begrunnelse: "${it.begrunnelse}",
                deaktivert: ${it.deaktivert},
                forklaring: "${it.forklaring}",
                orgnummer: "${it.orgnummer}",
            }
        """
    }

fun overstyrTidslinjeMutation(overstyring: ApiTidslinjeOverstyring): String = asMutation(
    """
    mutation OverstyrTidsline {
        overstyrDager(overstyring:{
            aktorId: "${overstyring.aktorId}", 
            begrunnelse: "${overstyring.begrunnelse}", 
            fodselsnummer: "${overstyring.fodselsnummer}", 
            organisasjonsnummer: "${overstyring.organisasjonsnummer}", 
            vedtaksperiodeId: "${overstyring.vedtaksperiodeId}",
            dager: ${dager(overstyring)}
        })
    }
    """
)

private fun dager(overstyring: ApiTidslinjeOverstyring): List<String> = overstyring.dager.map {
    """ 
        {
            dato: "${it.dato}",
            fraGrad: ${it.fraGrad},
            grad: ${it.grad},
            type: "${it.type}",
            fraType: "${it.fraType}",
            lovhjemmel: {bokstav: "A", ledd: "Albue", lovverk: "Norske lover", lovverksversjon: "1803", paragraf: "8-30"}
        }
    """
}
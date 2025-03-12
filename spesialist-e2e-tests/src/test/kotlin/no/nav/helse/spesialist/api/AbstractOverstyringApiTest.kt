package no.nav.helse.spesialist.api

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.spesialist.api.graphql.schema.ApiArbeidsforholdOverstyringHandling
import no.nav.helse.spesialist.api.graphql.schema.ApiInntektOgRefusjonOverstyring
import no.nav.helse.spesialist.api.graphql.schema.ApiTidslinjeOverstyring
import no.nav.helse.spesialist.api.graphql.schema.ApiTilkommenInntektOverstyring

internal abstract class AbstractOverstyringApiTest : AbstractGraphQLApiTest() {
    fun overstyrTidslinje(overstyring: ApiTidslinjeOverstyring): JsonNode =
        runQuery(
            """
            mutation OverstyrTidsline {
                overstyrDager(overstyring:{
                    aktorId: "${overstyring.aktorId}", 
                    begrunnelse: "${overstyring.begrunnelse}", 
                    fodselsnummer: "${overstyring.fodselsnummer}", 
                    organisasjonsnummer: "${overstyring.organisasjonsnummer}", 
                    vedtaksperiodeId: "${overstyring.vedtaksperiodeId}",
                    dager: ${overstyring.dager.map { """ {
                        dato: "${it.dato}",
                        fraGrad: ${it.fraGrad},
                        grad: ${it.grad},
                        type: "${it.type}",
                        fraType: "${it.fraType}",
                        lovhjemmel: {bokstav: "A", ledd: "Albue", lovverk: "Norske lover", lovverksversjon: "1803", paragraf: "8-30"}
                    } """ }}
                })
            }
            """,
        )

    fun overstyrArbeidsforhold(overstyring: ApiArbeidsforholdOverstyringHandling): JsonNode =
        runQuery(
            """
            mutation OverstyrArbeidsforhold {
                overstyrArbeidsforhold(overstyring: {
                    aktorId: "${overstyring.aktorId}",
                    skjaringstidspunkt: "${overstyring.skjaringstidspunkt}",
                    fodselsnummer: "${overstyring.fodselsnummer}",
                    overstyrteArbeidsforhold: ${overstyring.overstyrteArbeidsforhold.map { """ {
                        begrunnelse: "${it.begrunnelse}",
                        deaktivert: ${it.deaktivert},
                        forklaring: "${it.forklaring}",
                        orgnummer: "${it.orgnummer}",
                    } """ }},
                    vedtaksperiodeId: "${overstyring.vedtaksperiodeId}"
                })
            }
            """,
        )

    fun overstyrInntektOgRefusjon(overstyring: ApiInntektOgRefusjonOverstyring): JsonNode =
        runQuery(
            """
            mutation OverstyrInntektOgRefusjon {
                overstyrInntektOgRefusjon(overstyring: {
                    aktorId: "${overstyring.aktorId}",
                    skjaringstidspunkt: "${overstyring.skjaringstidspunkt}",
                    fodselsnummer: "${overstyring.fodselsnummer}",
                    arbeidsgivere: ${overstyring.arbeidsgivere.map {""" {
                            forklaring: "${it.forklaring}",
                            begrunnelse: "${it.begrunnelse}",
                            organisasjonsnummer: "${it.organisasjonsnummer}",
                            fraManedligInntekt: ${it.fraManedligInntekt},
                            manedligInntekt: ${it.manedligInntekt},
                            lovhjemmel: {bokstav: "A", ledd: "Albue", lovverk: "Norske lover", lovverksversjon: "1803", paragraf: "8-30"},
                            refusjonsopplysninger: ${it.refusjonsopplysninger?.map { opplysning -> """ {
                                belop: ${opplysning.belop},
                                fom: "${opplysning.fom}",
                                tom: ${opplysning.tom?.let { tom -> """"$tom"""" }}
                            } """ }},
                            fraRefusjonsopplysninger: ${it.fraRefusjonsopplysninger?.map { opplysning -> """ {
                               belop: ${opplysning.belop},
                               fom: "${opplysning.fom}",
                               tom: ${opplysning.tom?.let { tom -> """"$tom"""" }}
                            } """ }}
                    } """ }},
                    vedtaksperiodeId: "${overstyring.vedtaksperiodeId}"
                })
            }
            """,
        )

    fun overstyrTilkommenInntekt(overstyring: ApiTilkommenInntektOverstyring): JsonNode =
        runQuery(
            """mutation OverstyrTilkommenInntekt {
                        overstyrTilkommenInntekt(
                            overstyring: {
                                fodselsnummer: "${overstyring.fodselsnummer}",
                                aktorId: "${overstyring.aktorId}",
                                begrunnelse: "Foobar",
                                fjernet: ${overstyring.fjernet.map {
                                    """
                                        {
                                            organisasjonsnummer: "${it.organisasjonsnummer}",
                                            perioder: ${it.perioder.map { periode ->
                                                """
                                                    {
                                                        fom: "${periode.fom}",
                                                        tom: "${periode.tom}"
                                                    }
                                                """
                                                }
                                            }
                                        }
                                    """
                                }},
                                lagtTilEllerEndret: ${overstyring.lagtTilEllerEndret.map {
                                    """
                                        {
                                            organisasjonsnummer: "${it.organisasjonsnummer}",
                                            perioder: ${it.perioder.map { periode ->
                                                """
                                                    {
                                                        fom: "${periode.fom}",
                                                        tom: "${periode.tom}",
                                                        periodeBelop: ${periode.periodeBelop}
                                                    }
                                                """
                                                }
                                            }
                                        }
                                    """
                                }},
                                vedtaksperiodeId: "${overstyring.vedtaksperiodeId}",
                            }
                        )
                    }
            """,
        )
}

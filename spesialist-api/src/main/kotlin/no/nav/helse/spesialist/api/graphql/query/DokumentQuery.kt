package no.nav.helse.spesialist.api.graphql.query

import com.fasterxml.jackson.databind.JsonNode
import graphql.GraphQLError
import graphql.GraphqlErrorException
import graphql.execution.DataFetcherResult
import graphql.schema.DataFetchingEnvironment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import no.nav.helse.rapids_rivers.asLocalDate
import no.nav.helse.rapids_rivers.asLocalDateTime
import no.nav.helse.rapids_rivers.isMissingOrNull
import no.nav.helse.spesialist.api.Dokumenthåndterer
import no.nav.helse.spesialist.api.egenAnsatt.EgenAnsattApiDao
import no.nav.helse.spesialist.api.graphql.schema.AvsenderSystem
import no.nav.helse.spesialist.api.graphql.schema.DokumentInntektsmelding
import no.nav.helse.spesialist.api.graphql.schema.EndringIRefusjon
import no.nav.helse.spesialist.api.graphql.schema.GjenopptakelseNaturalytelse
import no.nav.helse.spesialist.api.graphql.schema.IMPeriode
import no.nav.helse.spesialist.api.graphql.schema.InntektEndringAarsak
import no.nav.helse.spesialist.api.graphql.schema.Naturalytelse
import no.nav.helse.spesialist.api.graphql.schema.OpphoerAvNaturalytelse
import no.nav.helse.spesialist.api.graphql.schema.Refusjon
import no.nav.helse.spesialist.api.graphql.schema.Soknad
import no.nav.helse.spesialist.api.graphql.schema.Soknadsperioder
import no.nav.helse.spesialist.api.graphql.schema.Soknadstype
import no.nav.helse.spesialist.api.graphql.schema.Sporsmal
import no.nav.helse.spesialist.api.graphql.schema.Svar
import no.nav.helse.spesialist.api.graphql.schema.Svartype
import no.nav.helse.spesialist.api.graphql.schema.Visningskriterium
import no.nav.helse.spesialist.api.person.PersonApiDao
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.UUID

class DokumentQuery(
    personApiDao: PersonApiDao,
    egenAnsattApiDao: EgenAnsattApiDao,
    private val dokumenthåndterer: Dokumenthåndterer,
) : AbstractPersonQuery(personApiDao, egenAnsattApiDao) {
    private val sikkerLogg: Logger = LoggerFactory.getLogger("tjenestekall")

    @Suppress("unused")
    suspend fun hentSoknad(
        fnr: String,
        dokumentId: String,
        env: DataFetchingEnvironment,
    ): DataFetcherResult<Soknad> {
        if (isForbidden(fnr, env)) {
            return DataFetcherResult.newResult<Soknad?>().error(getForbiddenError(fnr)).build()
        }

        if (dokumentId.isEmpty()) {
            return DataFetcherResult.newResult<Soknad>().error(getEmptyRequestError()).build()
        }

        val dokument =
            withContext(Dispatchers.IO) {
                dokumenthåndterer.håndter(fnr, UUID.fromString(dokumentId), DokumentType.SØKNAD.name)
            }.let {
                val error = it.path("error")?.takeUnless { error -> error.isMissingOrNull() }?.asInt()
                if (it.size() == 0) {
                    return DataFetcherResult.newResult<Soknad>().error(getExpectationFailedError()).build()
                } else if (error == 408) {
                    return DataFetcherResult.newResult<Soknad>().error(getEmptyResultTimeoutError()).build()
                }
                return@let it.tilSøknad()
            }

        return DataFetcherResult.newResult<Soknad>().data(dokument).build()
    }

    @Suppress("unused")
    suspend fun hentInntektsmelding(
        fnr: String,
        dokumentId: String,
        env: DataFetchingEnvironment,
    ): DataFetcherResult<DokumentInntektsmelding?> {
        if (isForbidden(fnr, env)) {
            return DataFetcherResult.newResult<DokumentInntektsmelding?>().error(getForbiddenError(fnr)).build()
        }

        if (dokumentId.isEmpty()) {
            return DataFetcherResult.newResult<DokumentInntektsmelding>().error(getEmptyRequestError()).build()
        }

        val dokument =
            withContext(Dispatchers.IO) {
                dokumenthåndterer.håndter(fnr, UUID.fromString(dokumentId), DokumentType.INNTEKTSMELDING.name)
            }.let {
                val error = it.path("error")?.takeUnless { error -> error.isMissingOrNull() }?.asInt()
                if (it.size() == 0) {
                    return DataFetcherResult.newResult<DokumentInntektsmelding>()
                        .error(getExpectationFailedError()).build()
                } else if (error == 404) {
                    return DataFetcherResult.newResult<DokumentInntektsmelding>()
                        .error(getNotFoundErrorEkstern()).build()
                } else if (error == 408) {
                    return DataFetcherResult.newResult<DokumentInntektsmelding>().error(getEmptyResultTimeoutError())
                        .build()
                }
                return@let it.tilInntektsmelding()
            }

        return DataFetcherResult.newResult<DokumentInntektsmelding>().data(dokument).build()
    }

    private fun getEmptyRequestError(): GraphQLError =
        GraphqlErrorException.newErrorException().message("Requesten mangler dokument-id.")
            .extensions(mapOf("code" to 400)).build()

    private fun getEmptyResultTimeoutError(): GraphQLError =
        GraphqlErrorException.newErrorException()
            .message("Det tar litt lengre tid enn forventet å hente dokumentet, vennligst prøv igjen.")
            .extensions(mapOf("code" to 408)).build()

    private fun getExpectationFailedError(): GraphQLError =
        GraphqlErrorException.newErrorException().message("Noe gikk galt, vennligst prøv igjen.")
            .extensions(mapOf("code" to 417)).build()

    private fun getNotFoundErrorEkstern(): GraphQLError =
        GraphqlErrorException.newErrorException()
            .message("Speil har ikke tilgang til denne inntektsmeldingen, den må åpnes i Gosys.")
            .extensions(mapOf("code" to 404)).build()

    private fun JsonNode.tilInntektsmelding(): DokumentInntektsmelding {
        return DokumentInntektsmelding(
            begrunnelseForReduksjonEllerIkkeUtbetalt =
            this.path("begrunnelseForReduksjonEllerIkkeUtbetalt")
                .takeUnless { it.isMissingOrNull() }?.asText(),
            bruttoUtbetalt = this.path("bruttoUtbetalt").takeUnless { it.isMissingOrNull() }?.asDouble(),
            beregnetInntekt = this.path("beregnetInntekt").takeUnless { it.isMissingOrNull() }?.asDouble(),
            inntektsdato = this.path("inntektsdato").takeUnless { it.isMissingOrNull() }?.asLocalDate(),
            refusjon =
            this.path("refusjon").takeUnless { it.isMissingOrNull() }?.let { refusjon ->
                Refusjon(
                    beloepPrMnd = refusjon["beloepPrMnd"].takeUnless { it.isMissingOrNull() }?.asDouble(),
                    opphoersdato = refusjon["opphoersdato"].takeUnless { it.isMissingOrNull() }?.asLocalDate(),
                )
            },
            endringIRefusjoner =
            this.path("endringIRefusjoner").takeUnless { it.isMissingOrNull() }
                ?.map { endringIRefusjon ->
                    EndringIRefusjon(
                        endringsdato =
                        endringIRefusjon["endringsdato"].takeUnless { it.isMissingOrNull() }?.asLocalDate(),
                        beloep = endringIRefusjon["beloep"].takeUnless { it.isMissingOrNull() }?.asDouble(),
                    )
                },
            opphoerAvNaturalytelser =
            this.path("opphoerAvNaturalytelser").takeUnless { it.isMissingOrNull() }
                ?.map { opphørAvNaturalytelse ->
                    OpphoerAvNaturalytelse(
                        opphørAvNaturalytelse["naturalytelse"].takeUnless { it.isMissingOrNull() }
                            ?.asText()?.tilNaturalytelse(),
                        fom = opphørAvNaturalytelse["fom"].takeUnless { it.isMissingOrNull() }?.asLocalDate(),
                        beloepPrMnd =
                        opphørAvNaturalytelse["beloepPrMnd"].takeUnless { it.isMissingOrNull() }
                            ?.asDouble(),
                    )
                },
            gjenopptakelseNaturalytelser =
            this.path("gjenopptakelseNaturalytelser").takeUnless { it.isMissingOrNull() }
                ?.map { gjenopptakelseNaturalytelse ->
                    GjenopptakelseNaturalytelse(
                        gjenopptakelseNaturalytelse["naturalytelse"].takeUnless { it.isMissingOrNull() }
                            ?.asText()?.tilNaturalytelse(),
                        fom = gjenopptakelseNaturalytelse["fom"].takeUnless { it.isMissingOrNull() }?.asLocalDate(),
                        beloepPrMnd =
                        gjenopptakelseNaturalytelse["beloepPrMnd"].takeUnless { it.isMissingOrNull() }
                            ?.asDouble(),
                    )
                },
            arbeidsgiverperioder =
            this.path("arbeidsgiverperioder").takeUnless { it.isMissingOrNull() }
                ?.map { arbeidsgiverperiode ->
                    IMPeriode(
                        fom = arbeidsgiverperiode["fom"].takeUnless { it.isMissingOrNull() }?.asLocalDate(),
                        tom = arbeidsgiverperiode["tom"].takeUnless { it.isMissingOrNull() }?.asLocalDate(),
                    )
                },
            ferieperioder =
            this.path("ferieperioder").takeUnless { it.isMissingOrNull() }?.map { ferieperiode ->
                IMPeriode(
                    fom = ferieperiode["fom"].takeUnless { it.isMissingOrNull() }?.asLocalDate(),
                    tom = ferieperiode["tom"].takeUnless { it.isMissingOrNull() }?.asLocalDate(),
                )
            },
            foersteFravaersdag = this.path("foersteFravaersdag").takeUnless { it.isMissingOrNull() }?.asLocalDate(),
            naerRelasjon = this.path("naerRelasjon").takeUnless { it.isMissingOrNull() }?.asBoolean(),
            innsenderFulltNavn = this.path("innsenderFulltNavn").takeUnless { it.isMissingOrNull() }?.asText(),
            innsenderTelefon = this.path("innsenderTelefon").takeUnless { it.isMissingOrNull() }?.asText(),
            inntektEndringAarsak =
            this.path("inntektEndringAarsak").takeUnless { it.isMissingOrNull() }?.let { endringAarsak ->
                InntektEndringAarsak(
                    aarsak = endringAarsak["aarsak"].asText(),
                    perioder =
                    endringAarsak["perioder"].takeUnless { it.isMissingOrNull() }?.map { periode ->
                        IMPeriode(
                            fom = periode["fom"].takeUnless { it.isMissingOrNull() }?.asLocalDate(),
                            tom = periode["tom"].takeUnless { it.isMissingOrNull() }?.asLocalDate(),
                        )
                    },
                    gjelderFra = endringAarsak["gjelderFra"].takeUnless { it.isMissingOrNull() }?.asLocalDate(),
                    bleKjent = endringAarsak["bleKjent"].takeUnless { it.isMissingOrNull() }?.asLocalDate(),
                )
            },
            avsenderSystem =
            this.path("avsenderSystem").takeUnless { it.isMissingOrNull() }?.let { avsenderSystem ->
                AvsenderSystem(
                    navn = avsenderSystem["navn"].takeUnless { it.isMissingOrNull() }?.asText(),
                    versjon = avsenderSystem["versjon"].takeUnless { it.isMissingOrNull() }?.asText(),
                )
            },
        )
    }

    private fun String.tilNaturalytelse(): Naturalytelse {
        return when (this) {
            "KOSTDOEGN" -> Naturalytelse.KOSTDOEGN
            "LOSJI" -> Naturalytelse.LOSJI
            "ANNET" -> Naturalytelse.ANNET
            "SKATTEPLIKTIGDELFORSIKRINGER" -> Naturalytelse.SKATTEPLIKTIGDELFORSIKRINGER
            "BIL" -> Naturalytelse.BIL
            "KOSTDAGER" -> Naturalytelse.KOSTDAGER
            "RENTEFORDELLAAN" -> Naturalytelse.RENTEFORDELLAAN
            "BOLIG" -> Naturalytelse.BOLIG
            "ELEKTRONISKKOMMUNIKASJON" -> Naturalytelse.ELEKTRONISKKOMMUNIKASJON
            "AKSJERGRUNNFONDSBEVISTILUNDERKURS" -> Naturalytelse.AKSJERGRUNNFONDSBEVISTILUNDERKURS
            "OPSJONER" -> Naturalytelse.OPSJONER
            "KOSTBESPARELSEIHJEMMET" -> Naturalytelse.KOSTBESPARELSEIHJEMMET
            "FRITRANSPORT" -> Naturalytelse.FRITRANSPORT
            "BEDRIFTSBARNEHAGEPLASS" -> Naturalytelse.BEDRIFTSBARNEHAGEPLASS
            "TILSKUDDBARNEHAGEPLASS" -> Naturalytelse.TILSKUDDBARNEHAGEPLASS
            "BESOEKSREISERHJEMMETANNET" -> Naturalytelse.BESOEKSREISERHJEMMETANNET
            "INNBETALINGTILUTENLANDSKPENSJONSORDNING" -> Naturalytelse.INNBETALINGTILUTENLANDSKPENSJONSORDNING
            "YRKEBILTJENESTLIGBEHOVLISTEPRIS" -> Naturalytelse.YRKEBILTJENESTLIGBEHOVLISTEPRIS
            "YRKEBILTJENESTLIGBEHOVKILOMETER" -> Naturalytelse.YRKEBILTJENESTLIGBEHOVKILOMETER
            else -> {
                sikkerLogg.error(
                    "Inntektsmelding har ny Naturalytelse som må støttes: {}, returnerer UKJENT enn så lenge",
                    this,
                )
                return Naturalytelse.UKJENT
            }
        }
    }

    private fun JsonNode.tilSøknad(): Soknad {
        val type = this.path("type").takeUnless { it.isMissingOrNull() }?.asText()?.tilSoknadstype()
        val arbeidGjenopptatt = this.path("arbeidGjenopptatt").takeUnless { it.isMissingOrNull() }?.asLocalDate()
        val sykmeldingSkrevet = this.path("sykmeldingSkrevet").takeUnless { it.isMissingOrNull() }?.asLocalDateTime()
        val egenmeldingsdagerFraSykmelding =
            this.path("egenmeldingsdagerFraSykmelding").takeUnless { it.isMissingOrNull() }?.map { it.asLocalDate() }
        val soknadsperioder =
            this.path("soknadsperioder").takeUnless { it.isMissingOrNull() }?.map { it.tilSøknadsperioder() }
        val sporsmal =
            this.path("sporsmal").takeUnless { it.isMissingOrNull() }?.map { it.tilSpørsmål() }
                ?.filter { it.skalVises() }
        return Soknad(
            type = type,
            arbeidGjenopptatt = arbeidGjenopptatt,
            sykmeldingSkrevet = sykmeldingSkrevet,
            egenmeldingsdagerFraSykmelding = egenmeldingsdagerFraSykmelding,
            soknadsperioder = soknadsperioder,
            sporsmal = sporsmal,
        )
    }

    private fun String.tilSoknadstype(): Soknadstype {
        return when (this) {
            "SELVSTENDIGE_OG_FRILANSERE" -> Soknadstype.Selvstendig_og_frilanser
            "OPPHOLD_UTLAND" -> Soknadstype.Opphold_utland
            "ARBEIDSTAKERE" -> Soknadstype.Arbeidstaker
            "ANNET_ARBEIDSFORHOLD" -> Soknadstype.Annet_arbeidsforhold
            "ARBEIDSLEDIG" -> Soknadstype.Arbeidsledig
            "BEHANDLINGSDAGER" -> Soknadstype.Behandlingsdager
            "REISETILSKUDD" -> Soknadstype.Reisetilskudd
            "GRADERT_REISETILSKUDD" -> Soknadstype.Gradert_reisetilskudd
            else -> {
                sikkerLogg.error(
                    "Søknad har ny Soknadstype som må støttes: {}, returnerer UKJENT enn så lenge",
                    this,
                )
                return Soknadstype.UKJENT
            }
        }
    }

    private fun JsonNode.tilSøknadsperioder(): Soknadsperioder {
        return Soknadsperioder(
            fom = this.path("fom").asLocalDate(),
            tom = this.path("tom").asLocalDate(),
            grad = this.path("grad").takeUnless { it.isMissingOrNull() }?.asInt(),
            sykmeldingsgrad = this.path("sykmeldingsgrad").takeUnless { it.isMissingOrNull() }?.asInt(),
            faktiskGrad = this.path("faktiskGrad").takeUnless { it.isMissingOrNull() }?.asInt(),
        )
    }

    private fun JsonNode.tilSpørsmål(): Sporsmal {
        val svar = this.path("svar").takeUnless { it.isMissingOrNull() }?.map { it.tilSvar() }
        val kriterieForVisningAvUndersporsmal =
            this.path("kriterieForVisningAvUndersporsmal").takeUnless { it.isMissingOrNull() }?.asText()
                ?.tilVisningskriterium()
        val undersporsmal =
            this.path("undersporsmal").takeUnless { it.isMissingOrNull() }?.map { it.tilSpørsmål() }
                ?.filter { it.skalVises(rotnivå = false) }

        return Sporsmal(
            tag = this.path("tag").takeUnless { it.isMissingOrNull() }?.asText(),
            sporsmalstekst = this.path("sporsmalstekst").takeUnless { it.isMissingOrNull() }?.asText(),
            undertekst = this.path("undertekst").takeUnless { it.isMissingOrNull() }?.asText(),
            svartype = this.path("svartype").takeUnless { it.isMissingOrNull() }?.asText()?.tilSvartype(),
            svar = svar,
            undersporsmal = undersporsmal,
            kriterieForVisningAvUndersporsmal = kriterieForVisningAvUndersporsmal,
        )
    }

    private fun Sporsmal.skalVises(rotnivå: Boolean = true): Boolean {
        val harTagSomSkalVises =
            when (this.tag) {
                "BEKREFT_OPPLYSNINGER" -> false
                "ANSVARSERKLARING" -> false
                "VAER_KLAR_OVER_AT" -> false
                "TIL_SLUTT" -> false
                else -> true
            }

        val harUnderspørsmål = !this.undersporsmal.isNullOrEmpty()
        val førsteSvar = this.svar?.firstOrNull()?.verdi
        val svartNeiPåRotnivå = førsteSvar == "NEI" && rotnivå
        val kriterieForVisningAvUndersporsmalOppfylt =
            (this.kriterieForVisningAvUndersporsmal == null || this.kriterieForVisningAvUndersporsmal.name == this.svar?.firstOrNull()?.verdi) && harUnderspørsmål

        return harTagSomSkalVises && (kriterieForVisningAvUndersporsmalOppfylt || (førsteSvar != null && !svartNeiPåRotnivå))
    }

    private fun JsonNode.tilSvar(): Svar {
        return Svar(
            verdi = this.path("verdi").takeUnless { it.isMissingOrNull() }?.asText(),
        )
    }

    private fun String.tilSvartype(): Svartype {
        return when (this) {
            "JA_NEI" -> Svartype.JA_NEI
            "CHECKBOX" -> Svartype.CHECKBOX
            "CHECKBOX_GRUPPE" -> Svartype.CHECKBOX_GRUPPE
            "CHECKBOX_PANEL" -> Svartype.CHECKBOX_PANEL
            "DATO" -> Svartype.DATO
            "PERIODE" -> Svartype.PERIODE
            "PERIODER" -> Svartype.PERIODER
            "TIMER" -> Svartype.TIMER
            "FRITEKST" -> Svartype.FRITEKST
            "IKKE_RELEVANT" -> Svartype.IKKE_RELEVANT
            "BEKREFTELSESPUNKTER" -> Svartype.BEKREFTELSESPUNKTER
            "PROSENT" -> Svartype.PROSENT
            "RADIO_GRUPPE" -> Svartype.RADIO_GRUPPE
            "RADIO_GRUPPE_TIMER_PROSENT" -> Svartype.RADIO_GRUPPE_TIMER_PROSENT
            "RADIO" -> Svartype.RADIO
            "TALL" -> Svartype.TALL
            "RADIO_GRUPPE_UKEKALENDER" -> Svartype.RADIO_GRUPPE_UKEKALENDER
            "LAND" -> Svartype.LAND
            "COMBOBOX_SINGLE" -> Svartype.COMBOBOX_SINGLE
            "COMBOBOX_MULTI" -> Svartype.COMBOBOX_MULTI
            "INFO_BEHANDLINGSDAGER" -> Svartype.INFO_BEHANDLINGSDAGER
            "KVITTERING" -> Svartype.KVITTERING
            "DATOER" -> Svartype.DATOER
            "BELOP" -> Svartype.BELOP
            "KILOMETER" -> Svartype.KILOMETER
            "GRUPPE_AV_UNDERSPORSMAL" -> Svartype.GRUPPE_AV_UNDERSPORSMAL
            else -> {
                sikkerLogg.error("Søknad har ny Svartype som må støttes: {}, returnerer UKJENT enn så lenge", this)
                return Svartype.UKJENT
            }
        }
    }

    private fun String.tilVisningskriterium(): Visningskriterium {
        return when (this) {
            "NEI" -> Visningskriterium.NEI
            "JA" -> Visningskriterium.JA
            "CHECKED" -> Visningskriterium.CHECKED
            else -> {
                sikkerLogg.error(
                    "Søknad har nytt Visningskriterium som må støttes: {}, returnerer UKJENT enn så lenge",
                    this,
                )
                return Visningskriterium.UKJENT
            }
        }
    }
}

enum class DokumentType {
    SØKNAD,
    INNTEKTSMELDING,
}

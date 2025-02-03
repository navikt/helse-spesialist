package no.nav.helse.spesialist.api.graphql.query

import com.expediagroup.graphql.server.operations.Query
import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.jackson.asLocalDate
import com.github.navikt.tbd_libs.jackson.asLocalDateOrNull
import com.github.navikt.tbd_libs.jackson.asLocalDateTimeOrNull
import com.github.navikt.tbd_libs.jackson.isMissingOrNull
import graphql.GraphQLError
import graphql.GraphqlErrorException
import graphql.execution.DataFetcherResult
import graphql.schema.DataFetchingEnvironment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import no.nav.helse.db.api.EgenAnsattApiDao
import no.nav.helse.db.api.PersonApiDao
import no.nav.helse.spesialist.api.Dokumenthåndterer
import no.nav.helse.spesialist.api.graphql.ContextValues.TILGANGER
import no.nav.helse.spesialist.api.graphql.forbiddenError
import no.nav.helse.spesialist.api.graphql.schema.ApiAvsenderSystem
import no.nav.helse.spesialist.api.graphql.schema.ApiDokumentInntektsmelding
import no.nav.helse.spesialist.api.graphql.schema.ApiEndringIRefusjon
import no.nav.helse.spesialist.api.graphql.schema.ApiGjenopptakelseNaturalytelse
import no.nav.helse.spesialist.api.graphql.schema.ApiIMPeriode
import no.nav.helse.spesialist.api.graphql.schema.ApiInntektEndringAarsak
import no.nav.helse.spesialist.api.graphql.schema.ApiNaturalytelse
import no.nav.helse.spesialist.api.graphql.schema.ApiOpphoerAvNaturalytelse
import no.nav.helse.spesialist.api.graphql.schema.ApiRefusjon
import no.nav.helse.spesialist.api.graphql.schema.ApiSoknad
import no.nav.helse.spesialist.api.graphql.schema.ApiSoknadsperioder
import no.nav.helse.spesialist.api.graphql.schema.ApiSoknadstype
import no.nav.helse.spesialist.api.graphql.schema.ApiSporsmal
import no.nav.helse.spesialist.api.graphql.schema.ApiSvar
import no.nav.helse.spesialist.api.graphql.schema.ApiSvartype
import no.nav.helse.spesialist.api.graphql.schema.ApiVisningskriterium
import no.nav.helse.spesialist.api.saksbehandler.manglerTilgang
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.UUID

class DokumentQuery(
    private val personApiDao: PersonApiDao,
    private val egenAnsattApiDao: EgenAnsattApiDao,
    private val dokumenthåndterer: Dokumenthåndterer,
) : Query {
    private val sikkerLogg: Logger = LoggerFactory.getLogger("tjenestekall")

    @Suppress("unused")
    suspend fun hentSoknad(
        fnr: String,
        dokumentId: String,
        env: DataFetchingEnvironment,
    ): DataFetcherResult<ApiSoknad?> {
        if (isForbidden(fnr, env)) {
            return DataFetcherResult.newResult<ApiSoknad?>().error(getForbiddenError(fnr)).build()
        }

        if (dokumentId.isEmpty()) {
            return DataFetcherResult.newResult<ApiSoknad>().error(getEmptyRequestError()).build()
        }

        val dokument =
            withContext(Dispatchers.IO) {
                dokumenthåndterer.håndter(fnr, UUID.fromString(dokumentId), DokumentType.SØKNAD.name)
            }.let {
                val error = it.path("error")?.takeUnless { error -> error.isMissingOrNull() }?.asInt()
                if (it.size() == 0) {
                    return DataFetcherResult.newResult<ApiSoknad>().error(getExpectationFailedError()).build()
                } else if (error == 408) {
                    return DataFetcherResult.newResult<ApiSoknad>().error(getEmptyResultTimeoutError()).build()
                }
                it.tilSøknad()
            }

        return DataFetcherResult.newResult<ApiSoknad>().data(dokument).build()
    }

    @Suppress("unused")
    suspend fun hentInntektsmelding(
        fnr: String,
        dokumentId: String,
        env: DataFetchingEnvironment,
    ): DataFetcherResult<ApiDokumentInntektsmelding?> {
        if (isForbidden(fnr, env)) {
            return DataFetcherResult.newResult<ApiDokumentInntektsmelding?>().error(getForbiddenError(fnr)).build()
        }

        if (dokumentId.isEmpty()) {
            return DataFetcherResult.newResult<ApiDokumentInntektsmelding>().error(getEmptyRequestError()).build()
        }

        val dokument =
            withContext(Dispatchers.IO) {
                dokumenthåndterer.håndter(fnr, UUID.fromString(dokumentId), DokumentType.INNTEKTSMELDING.name)
            }.let {
                val error = it.path("error")?.takeUnless { error -> error.isMissingOrNull() }?.asInt()
                if (it.size() == 0) {
                    return DataFetcherResult.newResult<ApiDokumentInntektsmelding>()
                        .error(getExpectationFailedError()).build()
                } else if (error == 404) {
                    return DataFetcherResult.newResult<ApiDokumentInntektsmelding>()
                        .error(getNotFoundErrorEkstern()).build()
                } else if (error == 408) {
                    return DataFetcherResult.newResult<ApiDokumentInntektsmelding>().error(getEmptyResultTimeoutError())
                        .build()
                }
                it.tilInntektsmelding()
            }

        return DataFetcherResult.newResult<ApiDokumentInntektsmelding>().data(dokument).build()
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

    private fun JsonNode.tilInntektsmelding(): ApiDokumentInntektsmelding {
        return ApiDokumentInntektsmelding(
            arbeidsforholdId = getIfNotNull("arbeidsforholdId")?.asText(),
            virksomhetsnummer = getIfNotNull("virksomhetsnummer")?.asText(),
            begrunnelseForReduksjonEllerIkkeUtbetalt = getIfNotNull("begrunnelseForReduksjonEllerIkkeUtbetalt")?.asText(),
            bruttoUtbetalt = getIfNotNull("bruttoUtbetalt")?.asDouble(),
            beregnetInntekt = getIfNotNull("beregnetInntekt")?.asDouble(),
            refusjon =
                getIfNotNull("refusjon")?.let { refusjon ->
                    ApiRefusjon(
                        beloepPrMnd = refusjon.getIfNotNull("beloepPrMnd")?.asDouble(),
                        opphoersdato = refusjon.getIfNotNull("opphoersdato")?.asLocalDate(),
                    )
                },
            endringIRefusjoner =
                getIfNotNull("endringIRefusjoner")?.map { endringIRefusjon ->
                    ApiEndringIRefusjon(
                        endringsdato = endringIRefusjon.getIfNotNull("endringsdato")?.asLocalDate(),
                        beloep = endringIRefusjon.getIfNotNull("beloep")?.asDouble(),
                    )
                },
            opphoerAvNaturalytelser =
                getIfNotNull("opphoerAvNaturalytelser")?.map { opphørAvNaturalytelse ->
                    ApiOpphoerAvNaturalytelse(
                        naturalytelse = opphørAvNaturalytelse.getIfNotNull("naturalytelse")?.asText()?.tilNaturalytelse(),
                        fom = opphørAvNaturalytelse.getIfNotNull("fom")?.asLocalDate(),
                        beloepPrMnd = opphørAvNaturalytelse.getIfNotNull("beloepPrMnd")?.asDouble(),
                    )
                },
            gjenopptakelseNaturalytelser =
                getIfNotNull("gjenopptakelseNaturalytelser")?.map { gjenopptakelseNaturalytelse ->
                    ApiGjenopptakelseNaturalytelse(
                        naturalytelse =
                            gjenopptakelseNaturalytelse.getIfNotNull("naturalytelse")?.asText()
                                ?.tilNaturalytelse(),
                        fom = gjenopptakelseNaturalytelse.getIfNotNull("fom")?.asLocalDate(),
                        beloepPrMnd = gjenopptakelseNaturalytelse.getIfNotNull("beloepPrMnd")?.asDouble(),
                    )
                },
            arbeidsgiverperioder =
                getIfNotNull("arbeidsgiverperioder")?.map { arbeidsgiverperiode ->
                    ApiIMPeriode(
                        fom = arbeidsgiverperiode.getIfNotNull("fom")?.asLocalDate(),
                        tom = arbeidsgiverperiode.getIfNotNull("tom")?.asLocalDate(),
                    )
                },
            ferieperioder =
                getIfNotNull("ferieperioder")?.map { ferieperiode ->
                    ApiIMPeriode(
                        fom = ferieperiode.getIfNotNull("fom")?.asLocalDate(),
                        tom = ferieperiode.getIfNotNull("tom")?.asLocalDate(),
                    )
                },
            foersteFravaersdag = getIfNotNull("foersteFravaersdag")?.asLocalDate(),
            naerRelasjon = getIfNotNull("naerRelasjon")?.asBoolean(),
            innsenderFulltNavn = getIfNotNull("innsenderFulltNavn")?.asText(),
            innsenderTelefon = getIfNotNull("innsenderTelefon")?.asText(),
            inntektEndringAarsak =
                getIfNotNull("inntektEndringAarsak")?.let { endringAarsak ->
                    ApiInntektEndringAarsak(
                        endringAarsak.get("aarsak").asText(),
                        endringAarsak.getIfNotNull("perioder")?.map { periode ->
                            ApiIMPeriode(
                                fom = periode.getIfNotNull("fom")?.asLocalDate(),
                                tom = periode.getIfNotNull("tom")?.asLocalDate(),
                            )
                        },
                        endringAarsak.getIfNotNull("gjelderFra")?.asLocalDate(),
                        endringAarsak.getIfNotNull("bleKjent")?.asLocalDate(),
                    )
                },
            avsenderSystem =
                getIfNotNull("avsenderSystem")?.let { avsenderSystem ->
                    ApiAvsenderSystem(
                        navn = avsenderSystem.getIfNotNull("navn")?.asText(),
                        versjon = avsenderSystem.getIfNotNull("versjon")?.asText(),
                    )
                },
        )
    }

    private fun String.tilNaturalytelse(): ApiNaturalytelse {
        return when (this) {
            "KOSTDOEGN" -> ApiNaturalytelse.KOSTDOEGN
            "LOSJI" -> ApiNaturalytelse.LOSJI
            "ANNET" -> ApiNaturalytelse.ANNET
            "SKATTEPLIKTIGDELFORSIKRINGER" -> ApiNaturalytelse.SKATTEPLIKTIGDELFORSIKRINGER
            "BIL" -> ApiNaturalytelse.BIL
            "KOSTDAGER" -> ApiNaturalytelse.KOSTDAGER
            "RENTEFORDELLAAN" -> ApiNaturalytelse.RENTEFORDELLAAN
            "BOLIG" -> ApiNaturalytelse.BOLIG
            "ELEKTRONISKKOMMUNIKASJON" -> ApiNaturalytelse.ELEKTRONISKKOMMUNIKASJON
            "AKSJERGRUNNFONDSBEVISTILUNDERKURS" -> ApiNaturalytelse.AKSJERGRUNNFONDSBEVISTILUNDERKURS
            "OPSJONER" -> ApiNaturalytelse.OPSJONER
            "KOSTBESPARELSEIHJEMMET" -> ApiNaturalytelse.KOSTBESPARELSEIHJEMMET
            "FRITRANSPORT" -> ApiNaturalytelse.FRITRANSPORT
            "BEDRIFTSBARNEHAGEPLASS" -> ApiNaturalytelse.BEDRIFTSBARNEHAGEPLASS
            "TILSKUDDBARNEHAGEPLASS" -> ApiNaturalytelse.TILSKUDDBARNEHAGEPLASS
            "BESOEKSREISERHJEMMETANNET" -> ApiNaturalytelse.BESOEKSREISERHJEMMETANNET
            "INNBETALINGTILUTENLANDSKPENSJONSORDNING" -> ApiNaturalytelse.INNBETALINGTILUTENLANDSKPENSJONSORDNING
            "YRKEBILTJENESTLIGBEHOVLISTEPRIS" -> ApiNaturalytelse.YRKEBILTJENESTLIGBEHOVLISTEPRIS
            "YRKEBILTJENESTLIGBEHOVKILOMETER" -> ApiNaturalytelse.YRKEBILTJENESTLIGBEHOVKILOMETER
            else -> {
                sikkerLogg.error(
                    "Inntektsmelding har ny Naturalytelse som må støttes: {}, returnerer UKJENT enn så lenge",
                    this,
                )
                return ApiNaturalytelse.UKJENT
            }
        }
    }

    private fun JsonNode.tilSøknad(): ApiSoknad {
        val type = getIfNotNull("type")?.asText()?.tilSoknadstype()
        val arbeidGjenopptatt = getIfNotNull("arbeidGjenopptatt")?.asLocalDateOrNull()
        val sykmeldingSkrevet = getIfNotNull("sykmeldingSkrevet")?.asLocalDateTimeOrNull()
        val egenmeldingsdagerFraSykmelding = getIfNotNull("egenmeldingsdagerFraSykmelding")?.map { it.asLocalDate() }
        val soknadsperioder = getIfNotNull("soknadsperioder")?.map { it.tilSøknadsperioder() }
        val sporsmal = getIfNotNull("sporsmal")?.map { it.tilSpørsmål() }?.filter { it.skalVises() }
        return ApiSoknad(
            type = type,
            arbeidGjenopptatt = arbeidGjenopptatt,
            sykmeldingSkrevet = sykmeldingSkrevet,
            egenmeldingsdagerFraSykmelding = egenmeldingsdagerFraSykmelding,
            soknadsperioder = soknadsperioder,
            sporsmal = sporsmal,
        )
    }

    private fun String.tilSoknadstype(): ApiSoknadstype {
        return when (this) {
            "SELVSTENDIGE_OG_FRILANSERE" -> ApiSoknadstype.Selvstendig_og_frilanser
            "OPPHOLD_UTLAND" -> ApiSoknadstype.Opphold_utland
            "ARBEIDSTAKERE" -> ApiSoknadstype.Arbeidstaker
            "ANNET_ARBEIDSFORHOLD" -> ApiSoknadstype.Annet_arbeidsforhold
            "ARBEIDSLEDIG" -> ApiSoknadstype.Arbeidsledig
            "BEHANDLINGSDAGER" -> ApiSoknadstype.Behandlingsdager
            "REISETILSKUDD" -> ApiSoknadstype.Reisetilskudd
            "GRADERT_REISETILSKUDD" -> ApiSoknadstype.Gradert_reisetilskudd
            else -> {
                sikkerLogg.error(
                    "Søknad har ny Soknadstype som må støttes: {}, returnerer UKJENT enn så lenge",
                    this,
                )
                return ApiSoknadstype.UKJENT
            }
        }
    }

    private fun JsonNode.tilSøknadsperioder(): ApiSoknadsperioder {
        return ApiSoknadsperioder(
            fom = get("fom").asLocalDate(),
            tom = get("tom").asLocalDate(),
            grad = getIfNotNull("grad")?.asInt(),
            sykmeldingsgrad = getIfNotNull("sykmeldingsgrad")?.asInt(),
            faktiskGrad = getIfNotNull("faktiskGrad")?.asInt(),
        )
    }

    private fun JsonNode.tilSpørsmål(): ApiSporsmal {
        val svar = getIfNotNull("svar")?.map { ApiSvar(it.getIfNotNull("verdi")?.asText()) }
        val kriterieForVisningAvUndersporsmal =
            getIfNotNull("kriterieForVisningAvUndersporsmal")?.asText()?.tilVisningskriterium()
        val undersporsmal =
            getIfNotNull("undersporsmal")?.map { it.tilSpørsmål() }?.filter { it.skalVises(rotnivå = false) }

        return ApiSporsmal(
            tag = getIfNotNull("tag")?.asText(),
            sporsmalstekst = getIfNotNull("sporsmalstekst")?.asText(),
            undertekst = getIfNotNull("undertekst")?.asText(),
            svartype = getIfNotNull("svartype")?.asText()?.tilSvartype(),
            svar = svar,
            undersporsmal = undersporsmal,
            kriterieForVisningAvUndersporsmal = kriterieForVisningAvUndersporsmal,
        )
    }

    private fun ApiSporsmal.skalVises(rotnivå: Boolean = true): Boolean {
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
        val kriterieForVisningAvUndersporsmal = this.kriterieForVisningAvUndersporsmal
        val kriterieForVisningAvUndersporsmalOppfylt =
            (kriterieForVisningAvUndersporsmal == null || kriterieForVisningAvUndersporsmal.name == this.svar?.firstOrNull()?.verdi) && harUnderspørsmål

        return harTagSomSkalVises && (kriterieForVisningAvUndersporsmalOppfylt || (førsteSvar != null && !svartNeiPåRotnivå))
    }

    private fun String.tilSvartype(): ApiSvartype {
        return when (this) {
            "JA_NEI" -> ApiSvartype.JA_NEI
            "CHECKBOX" -> ApiSvartype.CHECKBOX
            "CHECKBOX_GRUPPE" -> ApiSvartype.CHECKBOX_GRUPPE
            "CHECKBOX_PANEL" -> ApiSvartype.CHECKBOX_PANEL
            "DATO" -> ApiSvartype.DATO
            "PERIODE" -> ApiSvartype.PERIODE
            "PERIODER" -> ApiSvartype.PERIODER
            "TIMER" -> ApiSvartype.TIMER
            "FRITEKST" -> ApiSvartype.FRITEKST
            "IKKE_RELEVANT" -> ApiSvartype.IKKE_RELEVANT
            "BEKREFTELSESPUNKTER" -> ApiSvartype.BEKREFTELSESPUNKTER
            "OPPSUMMERING" -> ApiSvartype.OPPSUMMERING
            "PROSENT" -> ApiSvartype.PROSENT
            "RADIO_GRUPPE" -> ApiSvartype.RADIO_GRUPPE
            "RADIO_GRUPPE_TIMER_PROSENT" -> ApiSvartype.RADIO_GRUPPE_TIMER_PROSENT
            "RADIO" -> ApiSvartype.RADIO
            "TALL" -> ApiSvartype.TALL
            "RADIO_GRUPPE_UKEKALENDER" -> ApiSvartype.RADIO_GRUPPE_UKEKALENDER
            "LAND" -> ApiSvartype.LAND
            "COMBOBOX_SINGLE" -> ApiSvartype.COMBOBOX_SINGLE
            "COMBOBOX_MULTI" -> ApiSvartype.COMBOBOX_MULTI
            "INFO_BEHANDLINGSDAGER" -> ApiSvartype.INFO_BEHANDLINGSDAGER
            "KVITTERING" -> ApiSvartype.KVITTERING
            "DATOER" -> ApiSvartype.DATOER
            "BELOP" -> ApiSvartype.BELOP
            "KILOMETER" -> ApiSvartype.KILOMETER
            "GRUPPE_AV_UNDERSPORSMAL" -> ApiSvartype.GRUPPE_AV_UNDERSPORSMAL
            else -> {
                sikkerLogg.error("Søknad har ny Svartype som må støttes: {}, returnerer UKJENT enn så lenge", this)
                return ApiSvartype.UKJENT
            }
        }
    }

    private fun String.tilVisningskriterium(): ApiVisningskriterium {
        return when (this) {
            "NEI" -> ApiVisningskriterium.NEI
            "JA" -> ApiVisningskriterium.JA
            "CHECKED" -> ApiVisningskriterium.CHECKED
            else -> {
                sikkerLogg.error(
                    "Søknad har nytt Visningskriterium som må støttes: {}, returnerer UKJENT enn så lenge",
                    this,
                )
                return ApiVisningskriterium.UKJENT
            }
        }
    }

    private fun isForbidden(
        fnr: String,
        env: DataFetchingEnvironment,
    ): Boolean = manglerTilgang(egenAnsattApiDao, personApiDao, fnr, env.graphQlContext.get(TILGANGER))

    private fun getForbiddenError(fødselsnummer: String): GraphQLError = forbiddenError(fødselsnummer)
}

private fun JsonNode.getIfNotNull(key: String) = get(key).takeUnless { it.isMissingOrNull() }

enum class DokumentType {
    SØKNAD,
    INNTEKTSMELDING,
}

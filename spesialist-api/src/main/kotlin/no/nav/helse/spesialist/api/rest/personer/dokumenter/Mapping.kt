package no.nav.helse.spesialist.api.rest.personer.dokumenter

import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.jackson.asLocalDate
import com.github.navikt.tbd_libs.jackson.asLocalDateOrNull
import com.github.navikt.tbd_libs.jackson.asLocalDateTimeOrNull
import com.github.navikt.tbd_libs.jackson.isMissingOrNull
import no.nav.helse.spesialist.api.rest.ApiAvsenderSystem
import no.nav.helse.spesialist.api.rest.ApiDokumentInntektsmelding
import no.nav.helse.spesialist.api.rest.ApiEndringIRefusjon
import no.nav.helse.spesialist.api.rest.ApiGjenopptakelseNaturalytelse
import no.nav.helse.spesialist.api.rest.ApiIMPeriode
import no.nav.helse.spesialist.api.rest.ApiInntektEndringAarsak
import no.nav.helse.spesialist.api.rest.ApiNaturalytelse
import no.nav.helse.spesialist.api.rest.ApiOpphoerAvNaturalytelse
import no.nav.helse.spesialist.api.rest.ApiRefusjon
import no.nav.helse.spesialist.api.rest.ApiSoknad
import no.nav.helse.spesialist.api.rest.ApiSoknadSelvstendigNaringsdrivende
import no.nav.helse.spesialist.api.rest.ApiSoknadsperioder
import no.nav.helse.spesialist.api.rest.ApiSoknadstype
import no.nav.helse.spesialist.api.rest.ApiSporsmal
import no.nav.helse.spesialist.api.rest.ApiSvar
import no.nav.helse.spesialist.api.rest.ApiSvartype
import no.nav.helse.spesialist.api.rest.ApiVisningskriterium
import no.nav.helse.spesialist.application.logg.sikkerlogg

fun JsonNode.tilInntektsmelding(): ApiDokumentInntektsmelding =
    ApiDokumentInntektsmelding(
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
                    naturalytelse =
                        opphørAvNaturalytelse
                            .getIfNotNull("naturalytelse")
                            ?.asText()
                            ?.tilNaturalytelse(),
                    fom = opphørAvNaturalytelse.getIfNotNull("fom")?.asLocalDate(),
                    beloepPrMnd = opphørAvNaturalytelse.getIfNotNull("beloepPrMnd")?.asDouble(),
                )
            },
        gjenopptakelseNaturalytelser =
            getIfNotNull("gjenopptakelseNaturalytelser")?.map { gjenopptakelseNaturalytelse ->
                ApiGjenopptakelseNaturalytelse(
                    naturalytelse =
                        gjenopptakelseNaturalytelse
                            .getIfNotNull("naturalytelse")
                            ?.asText()
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
        inntektEndringAarsaker =
            getIfNotNull("inntektEndringAarsaker")?.let { endringAarsaker ->
                endringAarsaker.map { endringAarsak ->
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
                }
            },
        avsenderSystem =
            getIfNotNull("avsenderSystem")?.let { avsenderSystem ->
                ApiAvsenderSystem(
                    navn = avsenderSystem.getIfNotNull("navn")?.asText(),
                    versjon = avsenderSystem.getIfNotNull("versjon")?.asText(),
                )
            },
    )

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
            sikkerlogg.error(
                "Inntektsmelding har ny Naturalytelse som må støttes: {}, returnerer UKJENT enn så lenge",
                this,
            )
            return ApiNaturalytelse.UKJENT
        }
    }
}

fun JsonNode.tilSøknad() =
    ApiSoknad(
        type = getIfNotNull("type")?.asText()?.tilSoknadstype(),
        arbeidGjenopptatt = getIfNotNull("arbeidGjenopptatt")?.asLocalDateOrNull(),
        sykmeldingSkrevet = getIfNotNull("sykmeldingSkrevet")?.asLocalDateTimeOrNull(),
        egenmeldingsdagerFraSykmelding = getIfNotNull("egenmeldingsdagerFraSykmelding")?.map { it.asLocalDate() },
        soknadsperioder = getIfNotNull("soknadsperioder")?.map { it.tilSøknadsperioder() },
        sporsmal = getIfNotNull("sporsmal")?.map { it.tilSpørsmål() }?.filter { it.skalVises() },
        selvstendigNaringsdrivende = getIfNotNull("selvstendigNaringsdrivende")?.tilSelvstendigNæringsdrivende(),
    )

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
            sikkerlogg.error(
                "Søknad har ny Soknadstype som må støttes: {}, returnerer UKJENT enn så lenge",
                this,
            )
            return ApiSoknadstype.UKJENT
        }
    }
}

private fun JsonNode.tilSøknadsperioder(): ApiSoknadsperioder =
    ApiSoknadsperioder(
        fom = get("fom").asLocalDate(),
        tom = get("tom").asLocalDate(),
        grad = getIfNotNull("grad")?.asInt(),
        sykmeldingsgrad = getIfNotNull("sykmeldingsgrad")?.asInt(),
        faktiskGrad = getIfNotNull("faktiskGrad")?.asInt(),
    )

private fun JsonNode.tilSelvstendigNæringsdrivende(): ApiSoknadSelvstendigNaringsdrivende =
    ApiSoknadSelvstendigNaringsdrivende(
        inntekt =
            get("inntekt")["inntektsAar"].map { inntektsår ->
                val pensjonsgivendeInntekt = inntektsår.get("pensjonsgivendeInntekt")
                ApiSoknadSelvstendigNaringsdrivende.ApiInntektsar(
                    ar = inntektsår.get("aar").asText(),
                    pensjonsgivendeInntektAvLonnsinntekt = pensjonsgivendeInntekt["pensjonsgivendeInntektAvLoennsinntekt"].asInt(),
                    pensjonsgivendeInntektAvLonnsinntektBarePensjonsdel = pensjonsgivendeInntekt["pensjonsgivendeInntektAvLoennsinntektBarePensjonsdel"].asInt(),
                    pensjonsgivendeInntektAvNaringsinntekt = pensjonsgivendeInntekt["pensjonsgivendeInntektAvNaeringsinntekt"].asInt(),
                    pensjonsgivendeInntektAvNaringsinntektFraFiskeFangstEllerFamiliebarnehage = pensjonsgivendeInntekt["pensjonsgivendeInntektAvNaeringsinntektFraFiskeFangstEllerFamiliebarnehage"].asInt(),
                    erFerdigLignet = inntektsår.get("erFerdigLignet").asBoolean(),
                )
            },
    )

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
        "AAR_MANED",
        "AAR_MAANED",
        -> ApiSvartype.AAR_MAANED
        else -> {
            sikkerlogg.error("Søknad har ny Svartype som må støttes: {}, returnerer UKJENT enn så lenge", this)
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
            sikkerlogg.error(
                "Søknad har nytt Visningskriterium som må støttes: {}, returnerer UKJENT enn så lenge",
                this,
            )
            return ApiVisningskriterium.UKJENT
        }
    }
}

private fun JsonNode.getIfNotNull(key: String) = get(key).takeUnless { it.isMissingOrNull() }

package no.nav.helse.spesialist.api.graphql.query

import com.fasterxml.jackson.databind.JsonNode
import graphql.GraphQLError
import graphql.GraphqlErrorException
import graphql.execution.DataFetcherResult
import graphql.schema.DataFetchingEnvironment
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import no.nav.helse.rapids_rivers.isMissingOrNull
import no.nav.helse.spesialist.api.Dokumenthåndterer
import no.nav.helse.spesialist.api.egenAnsatt.EgenAnsattApiDao
import no.nav.helse.spesialist.api.graphql.schema.Soknad
import no.nav.helse.spesialist.api.graphql.schema.Soknadsperioder
import no.nav.helse.spesialist.api.graphql.schema.Sporsmal
import no.nav.helse.spesialist.api.graphql.schema.Svar
import no.nav.helse.spesialist.api.graphql.schema.Svartype
import no.nav.helse.spesialist.api.graphql.schema.Visningskriterium
import no.nav.helse.spesialist.api.person.PersonApiDao
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class DokumentQuery(
    personApiDao: PersonApiDao,
    egenAnsattApiDao: EgenAnsattApiDao,
    private val dokumenthåndterer: Dokumenthåndterer,
) : AbstractPersonQuery(personApiDao, egenAnsattApiDao) {

    private val sikkerLogg: Logger = LoggerFactory.getLogger("tjenestekall")

    @Suppress("unused")
    suspend fun hentSoknad(fnr: String, dokumentId: String, env: DataFetchingEnvironment): DataFetcherResult<Soknad> {
        if (isForbidden(fnr, env)) {
            return DataFetcherResult.newResult<Soknad?>().error(getForbiddenError(fnr)).build()
        }

        if (dokumentId.isEmpty()) {
            return DataFetcherResult.newResult<Soknad>().error(getEmptyRequestError()).build()
        }

        val dokument = withContext(Dispatchers.IO) {
            dokumenthåndterer.håndter(fnr, UUID.fromString(dokumentId), DokumentType.SØKNAD.name)
        }.let {
            if (it.size() == 0) return DataFetcherResult.newResult<Soknad>().error(getEmptyResultTimeoutError()).build()
            return@let it.tilSøknad()
        }

        return DataFetcherResult.newResult<Soknad>().data(dokument).build()
    }

    private fun getEmptyRequestError(): GraphQLError =
        GraphqlErrorException.newErrorException().message("Requesten mangler dokument-id")
            .extensions(mapOf("code" to 400)).build()

    private fun getEmptyResultTimeoutError(): GraphQLError =
        GraphqlErrorException.newErrorException().message("Noe gikk galt, vennligst prøv igjen.")
            .extensions(mapOf("code" to 408)).build()

    private fun JsonNode.tilSøknad(): Soknad {
        val arbeidGjenopptatt = this.path("arbeidGjenopptatt").takeUnless { it.isMissingOrNull() }?.asText()
        val sykmeldingSkrevet = this.path("sykmeldingSkrevet").takeUnless { it.isMissingOrNull() }?.asText()
        val egenmeldingsdagerFraSykmelding =
            this.path("egenmeldingsdagerFraSykmelding").takeUnless { it.isMissingOrNull() }?.map { it.asText() }
        val soknadsperioder =
            this.path("soknadsperioder").takeUnless { it.isMissingOrNull() }?.map { it.tilSøknadsperioder() }
        val sporsmal = this.path("sporsmal").takeUnless { it.isMissingOrNull() }?.map { it.tilSpørsmål() }
            ?.filter { it.skalVises() }
        return Soknad(
            arbeidGjenopptatt = arbeidGjenopptatt,
            sykmeldingSkrevet = sykmeldingSkrevet,
            egenmeldingsdagerFraSykmelding = egenmeldingsdagerFraSykmelding,
            soknadsperioder = soknadsperioder,
            sporsmal = sporsmal
        )
    }

    private fun JsonNode.tilSøknadsperioder(): Soknadsperioder {
        val faktiskGrad = this.path("faktiskGrad").takeUnless { it.isMissingOrNull() }?.asInt()
        return Soknadsperioder(
            fom = this.path("fom").asText(),
            tom = this.path("tom").asText(),
            grad = this.path("grad").asInt(),
            faktiskGrad = faktiskGrad
        )
    }

    private fun JsonNode.tilSpørsmål(): Sporsmal {
        val svar = this.path("svar").takeUnless { it.isMissingOrNull() }?.map { it.tilSvar() }
        val kriterieForVisningAvUndersporsmal =
            this.path("kriterieForVisningAvUndersporsmal").takeUnless { it.isMissingOrNull() }?.asText()
                ?.tilVisningskriterium()
        val undersporsmal = this.path("undersporsmal").takeUnless { it.isMissingOrNull() }?.map { it.tilSpørsmål() }
            ?.filter { it.skalVises(rotnivå = false) }

        return Sporsmal(
            tag = this.path("tag").takeUnless { it.isMissingOrNull() }?.asText(),
            sporsmalstekst = this.path("sporsmalstekst").takeUnless { it.isMissingOrNull() }?.asText(),
            undertekst = this.path("undertekst").takeUnless { it.isMissingOrNull() }?.asText(),
            svartype = this.path("svartype").takeUnless { it.isMissingOrNull() }?.asText()?.tilSvartype(),
            svar = svar,
            undersporsmal = undersporsmal,
            kriterieForVisningAvUndersporsmal = kriterieForVisningAvUndersporsmal
        )
    }

    private fun Sporsmal.skalVises(rotnivå: Boolean = true): Boolean {
        val harTagSomSkalVises = when (this.tag) {
            "BEKREFT_OPPLYSNINGER" -> false
            "ANSVARSERKLARING" -> false
            "VAER_KLAR_OVER_AT" -> false
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
            verdi = this.path("verdi").takeUnless { it.isMissingOrNull() }?.asText()
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
                    "Søknad har nytt Visningskriterium som må støttes: {}, returnerer UKJENT enn så lenge", this
                )
                return Visningskriterium.UKJENT
            }
        }
    }
}

enum class DokumentType {
    SØKNAD, INNTEKTSMELDING
}
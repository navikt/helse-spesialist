package no.nav.helse.kafka.messagebuilders

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.mediator.asUUID
import no.nav.helse.modell.melding.Behov
import no.nav.helse.modell.melding.InntektTilRisk
import no.nav.helse.modell.vilkårsprøving.OmregnetÅrsinntekt
import no.nav.helse.spesialist.kafka.objectMapper
import no.nav.helse.spesialist.testfixtures.jan
import no.nav.helse.spesialist.testfixtures.lagFødselsnummer
import no.nav.helse.spesialist.testfixtures.lagOrganisasjonsnummer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

class BehovMessageBuilderTest {
    private val fødselsnummer: String = lagFødselsnummer()
    private val commandContextId: UUID = UUID.randomUUID()
    private val hendelseId: UUID = UUID.randomUUID()
    private fun Behov.somJson() = listOf(this).somJsonMessage(commandContextId, fødselsnummer, hendelseId).toJson()

    @Test
    fun `Infotrygdutbetalinger-behov`() {
        val behov = Behov.Infotrygdutbetalinger(1 jan 2018, 31 jan 2018).somJson()
        behov.assertBehov(
            "HentInfotrygdutbetalinger", mapOf(
                "historikkFom" to (1 jan 2018),
                "historikkTom" to (31 jan 2018)
            )
        )
    }

    @Test
    fun `Personinfo-behov`() {
        val behov = Behov.Personinfo.somJson()
        behov.assertBehov("HentPersoninfoV2", emptyMap())
    }

    @Test
    fun `Enhet-behov`() {
        val behov = Behov.Enhet.somJson()
        behov.assertBehov("HentEnhet", emptyMap())
    }

    @Test
    fun `Arbeidsgiverinformasjon-behov for OrdinærArbeidsgiver`() {
        val organisasjonsnumre = listOf(lagOrganisasjonsnummer())
        val behov = Behov.Arbeidsgiverinformasjon.OrdinærArbeidsgiver(organisasjonsnumre).somJson()
        behov.assertBehov("Arbeidsgiverinformasjon", mapOf("organisasjonsnummer" to organisasjonsnumre))
    }

    @Test
    fun `Arbeidsgiverinformasjon-behov for Enkeltpersonforetak`() {
        val identer = listOf(lagFødselsnummer())
        val behov = Behov.Arbeidsgiverinformasjon.Enkeltpersonforetak(identer).somJson()
        behov.assertBehov("HentPersoninfoV2", mapOf("ident" to identer))
    }

    @Test
    fun `Arbeidsforhold-behov`() {
        val organisasjonsnummer = lagOrganisasjonsnummer()
        val behov = Behov.Arbeidsforhold(fødselsnummer, organisasjonsnummer).somJson()
        behov.assertBehov(
            "Arbeidsforhold",
            mapOf("organisasjonsnummer" to organisasjonsnummer, "fødselsnummer" to fødselsnummer)
        )
    }

    @Test
    fun `InntekterForSykepengegrunnlag-behov`() {
        val beregningStart = YearMonth.now().minusMonths(1)
        val beregningSlutt = YearMonth.now()
        val behov = Behov.InntekterForSykepengegrunnlag(beregningStart, beregningSlutt).somJson()
        behov.assertBehov(
            "InntekterForSykepengegrunnlag",
            mapOf("beregningStart" to beregningStart, "beregningSlutt" to beregningSlutt)
        )
    }

    @Test
    fun `Risikovurdering-behov`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val organisasjonsnummer = lagOrganisasjonsnummer()
        val behov = Behov.Risikovurdering(
            vedtaksperiodeId = vedtaksperiodeId,
            organisasjonsnummer = organisasjonsnummer,
            førstegangsbehandling = true,
            kunRefusjon = false,
            inntekt = InntektTilRisk(
                omregnetÅrsinntekt = 123456.7,
                inntektskilde = "Arbeidsgiver"
            )
        ).somJson()
        behov.assertBehov(
            "Risikovurdering",
            mapOf(
                "vedtaksperiodeId" to vedtaksperiodeId,
                "organisasjonsnummer" to organisasjonsnummer,
                "førstegangsbehandling" to true,
                "kunRefusjon" to false,
                "inntekt" to mapOf(
                    "omregnetÅrsinntekt" to 123456.7,
                    "inntektskilde" to "Arbeidsgiver",
                )
            )
        )
    }

    @Test
    fun `ÅpneOppgaver-behov`() {
        val behov = Behov.ÅpneOppgaver(ikkeEldreEnn = LocalDate.now()).somJson()
        behov.assertBehov("ÅpneOppgaver", mapOf("ikkeEldreEnn" to LocalDate.now()))
    }

    @Test
    fun `Vergemål-behov`() {
        val behov = Behov.Vergemål.somJson()
        behov.assertBehov("Vergemål", emptyMap())
    }

    @Test
    fun `Fullmakt-behov`() {
        val behov = Behov.Fullmakt.somJson()
        behov.assertBehov("Fullmakt", emptyMap())
    }

    @Test
    fun `EgenAnsatt-behov`() {
        val behov = Behov.EgenAnsatt.somJson()
        behov.assertBehov("EgenAnsatt", emptyMap())
    }

    @Test
    fun `Avviksvurdering-behov`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val vilkårsgrunnlagId = UUID.randomUUID()
        val skjæringstidspunkt = LocalDate.now()
        val organisasjonsnummer = lagOrganisasjonsnummer()
        val behov = Behov.Avviksvurdering(
            listOf(OmregnetÅrsinntekt(organisasjonsnummer, 1000.0)),
            vilkårsgrunnlagId,
            skjæringstidspunkt,
            organisasjonsnummer,
            vedtaksperiodeId
        ).somJson()
        behov.assertBehov(
            "Avviksvurdering",
            mapOf(
                "vedtaksperiodeId" to vedtaksperiodeId,
                "organisasjonsnummer" to organisasjonsnummer,
                "skjæringstidspunkt" to skjæringstidspunkt,
                "vilkårsgrunnlagId" to vilkårsgrunnlagId,
                "omregnedeÅrsinntekter" to listOf(
                    mapOf(
                        "organisasjonsnummer" to organisasjonsnummer,
                        "beløp" to 1000.0
                    )
                ),
            )
        )
    }

    private fun JsonNode.assertStandardfelter() {
        assertEquals("behov", this.path("@event_name").asText())
        assertEquals(fødselsnummer, this.path("fødselsnummer").asText())
        assertEquals(commandContextId, this.path("contextId").asUUID())
        assertEquals(hendelseId, this.path("hendelseId").asUUID())
    }

    private fun String.assertBehov(behovtype: String, payload: Map<String, Any>) {
        val jsonNode = objectMapper.readTree(this)
        jsonNode.assertStandardfelter()
        assertEquals(listOf(behovtype), jsonNode.path("@behov").map { it.asText() })
        assertEquals(objectMapper.valueToTree(payload), jsonNode.path(behovtype))
    }
}

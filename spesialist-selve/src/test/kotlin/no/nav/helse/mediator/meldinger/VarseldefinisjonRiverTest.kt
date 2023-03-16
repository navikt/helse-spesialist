package no.nav.helse.mediator.meldinger

import io.mockk.mockk
import io.mockk.verify
import java.util.UUID
import no.nav.helse.modell.varsel.VarselRepository
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class VarseldefinisjonRiverTest {
    private val testRapid = TestRapid()
    private val varselRepository = mockk<VarselRepository>(relaxed = true)

    init {
        Varseldefinisjon.River(testRapid, varselRepository)
        Varseldefinisjon.VarseldefinisjonRiver(testRapid, varselRepository)
    }

    @BeforeEach
    fun beforeEach() {
        testRapid.reset()
    }

    @Test
    fun `leser definisjoner fra kafka`() {
        testRapid.sendTestMessage(varseldefinisjoner)
        verify(exactly = 4) { varselRepository.lagreDefinisjon(any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `leser definisjon fra kafka`() {
        val id = UUID.fromString("ee7f8701-e70b-4752-a714-cfa76dba2f3a")
        testRapid.sendTestMessage(varseldefinisjon(id))
        verify(exactly = 1) { varselRepository.lagreDefinisjon(id, "XX_YY_1", "En tittel", "En forklaring", "En handling", false, any()) }
    }

    @Test
    fun `leser definisjon fra kafka uten forklaring og handling`() {
        val id = UUID.fromString("ee7f8701-e70b-4752-a714-cfa76dba2f3a")
        testRapid.sendTestMessage(varseldefinisjonUtenForklaringOgHandling(id))
        verify(exactly = 1) { varselRepository.lagreDefinisjon(id, "XX_YY_1", "En tittel", null, null, false, any()) }
    }

    @Language("JSON")
    private fun varseldefinisjon(id: UUID) = """
    {
      "@event_name": "varselkode_ny_definisjon",
      "varselkode": "XX_YY_1",
      "gjeldende_definisjon": {
        "id": "$id",
        "kode": "XX_YY_1",
        "tittel": "En tittel",
        "forklaring": "En forklaring",
        "handling": "En handling",
        "avviklet": false,
        "opprettet": "2023-03-16T00:00:00.000000"
      },
      "@id": "0993678d-dded-4edb-b032-02f668787206",
      "@opprettet": "2023-03-16T00:00:00.000000",
      "system_read_count": 0,
      "system_participating_services": [
        {
          "id": "a0dbf2f8-a107-4073-9669-69ce03e2f1fd",
          "time": "2023-03-16T00:00:00.000000"
        }
      ]
    } 
    """

    @Language("JSON")
    private fun varseldefinisjonUtenForklaringOgHandling(id: UUID) = """
    {
      "@event_name": "varselkode_ny_definisjon",
      "varselkode": "XX_YY_1",
      "gjeldende_definisjon": {
        "id": "$id",
        "kode": "XX_YY_1",
        "tittel": "En tittel",
        "forklaring": null,
        "handling": null,
        "avviklet": false,
        "opprettet": "2023-03-16T00:00:00.000000"
      },
      "@id": "0993678d-dded-4edb-b032-02f668787206",
      "@opprettet": "2023-03-16T00:00:00.000000",
      "system_read_count": 0,
      "system_participating_services": [
        {
          "id": "a0dbf2f8-a107-4073-9669-69ce03e2f1fd",
          "time": "2023-03-16T00:00:00.000000"
        }
      ]
    } 
    """

    @Language("JSON")
    private val varseldefinisjoner = """{
  "@event_name": "varseldefinisjoner_endret",
  "definisjoner": [
    {
      "id": "${UUID.randomUUID()}",
      "kode": "RV_SØ_1",
      "tittel": "Søknaden inneholder permittering. Vurder om permittering har konsekvens for rett til sykepenger",
      "forklaring": "Bruker har oppgitt permittering på søknad om sykepenger",
      "handling": "Kontrollér at permitteringen ikke påvirker sykepengerettighetene",
      "avviklet": false,
      "opprettet": "2022-11-01T14:04:27.271588"
    },
    {
      "id": "${UUID.randomUUID()}",
      "kode": "RV_SØ_2",
      "tittel": "Minst én dag er avslått på grunn av foreldelse. Vurder å sende vedtaksbrev fra Infotrygd",
      "forklaring": "Søknaden har kommet inn mer enn 3 måneder før dagen/dagene.",
      "handling": "Hvis det ikke finnes noe unntak fra foreldelsesfristen skal du godkjenne i Speil og sende vedtaksbrev fra Infotrygd. Hvis forslaget er feil må du avvise saken i Speil og behandle i Infotrygd. Dersom dagen/dagene som er avslått er innenfor arbeidsgiverperioden trenger du ikke å sende avslag fra Infotrygd.",
      "avviklet": false,
      "opprettet": "2022-11-01T14:04:27.271588"
    },
    {
      "id": "${UUID.randomUUID()}",
      "kode": "RV_SØ_3",
      "tittel": "Sykmeldingen er tilbakedatert, vurder fra og med dato for utbetaling.",
      "forklaring": "Sykmeldingen er tilbakedatert.",
      "handling": "Hvis tilbakedateringen er godkjent, skal du finne notat i Gosys eller stå i fritekstfelt på SP-UB. Da kan du godkjenne saken i Speil. Hvis tilbakedateringen er under vurdering, skal det være en åpen Gosys-oppgave. I slike tilfeller skal du legge saken på vent til vurderingen er ferdig. Hvis tilbakedateringen er avslått, skal du avvise saken i Speil og sende vedtak. Registerer avslaget i SP-SA. Husk også å lukke oppgaven på søknaden/inntektsmeldingen i Gosys.",
      "avviklet": false,
      "opprettet": "2022-11-01T14:04:27.271588"
    },
    {
      "id": "${UUID.randomUUID()}",
      "kode": "RV_SØ_4",
      "tittel": "Utdanning oppgitt i perioden i søknaden.",
      "forklaring": null,
      "handling": null,
      "avviklet": false,
      "opprettet": "2022-11-01T14:04:27.271588"
    }
  ]
}
    """
}
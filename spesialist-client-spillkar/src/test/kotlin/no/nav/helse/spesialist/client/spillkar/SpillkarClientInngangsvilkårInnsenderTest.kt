package no.nav.helse.spesialist.client.spillkar

import com.github.tomakehurst.wiremock.client.WireMock.noContent
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.serverError
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import com.github.tomakehurst.wiremock.junit5.WireMockExtension
import no.nav.helse.spesialist.application.spillkar.`ManuellInngangsvilkårVurdering`
import no.nav.helse.spesialist.application.spillkar.`ManuelleInngangsvilkårVurderinger`
import org.junit.jupiter.api.extension.RegisterExtension
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertNotNull

class SpillkarClientInngangsvilkårInnsenderTest {
    @Suppress("JUnitMalformedDeclaration")
    @RegisterExtension
    private val wireMock: WireMockExtension =
        WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build()

    private val vurderinger =
        ManuelleInngangsvilkårVurderinger(
            personidentifikator = "11111111111",
            skjæringstidspunkt = LocalDate.of(2021, 1, 1),
            versjon = 1,
            vurderinger =
                listOf(
                    ManuellInngangsvilkårVurdering(
                        vilkårskode = "MEDLEMSKAP",
                        vurderingskode = "MEDLEMSKAP_JA",
                        tidspunkt = LocalDateTime.of(2024, 1, 15, 10, 0, 0).atOffset(ZoneOffset.UTC).toInstant(),
                        begrunnelse = "Dokumentasjon foreligger",
                    ),
                ),
        )

    @Test
    fun `sender manuelle vurderinger og håndterer 204 No Content`() {
        wireMock.stubFor(post(urlEqualTo("/vurderte-inngangsvilkar/manuelle-vurderinger")).willReturn(noContent()))

        lagKlient().sendManuelleVurderinger(vurderinger)
        // forventer ingen exception
    }

    @Test
    fun `feiler ved HTTP 500`() {
        wireMock.stubFor(post(urlEqualTo("/vurderte-inngangsvilkar/manuelle-vurderinger")).willReturn(serverError().withBody("Intern feil")))

        val exception =
            runCatching {
                lagKlient().sendManuelleVurderinger(vurderinger)
            }.exceptionOrNull()

        assertNotNull(exception)
        assertIs<RuntimeException>(exception)
    }

    private fun lagKlient() =
        SpillkarClientInngangsvilkårInnsender(
            configuration =
                ClientSpillkarModule.Configuration(
                    apiUrl = wireMock.runtimeInfo.httpBaseUrl,
                    scope = "scoap",
                ),
            accessTokenGenerator = { "test-token" },
        )
}

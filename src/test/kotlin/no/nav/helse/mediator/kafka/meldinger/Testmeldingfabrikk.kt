package no.nav.helse.mediator.kafka.meldinger

import no.nav.helse.modell.overstyring.OverstyringDagDto
import no.nav.helse.modell.vedtak.Saksbehandleroppgavetype
import no.nav.helse.rapids_rivers.JsonMessage
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import kotlin.random.Random.Default.nextLong

class Testmeldingfabrikk(private val fødselsnummer: String, private val aktørId: String) {
    companion object {
        const val OSLO = "0301"
    }

    fun lagVedtaksperiodeEndret(
        id: UUID = UUID.randomUUID(),
        vedtaksperiodeId: UUID = UUID.randomUUID(),
        organisasjonsnummer: String = "orgnr",
        forrigeTilstand: String = "FORRIGE_TILSTAND",
        gjeldendeTilstand: String = "GJELDENDE_TILSTAND"
    ) =
        nyHendelse(
            id, "vedtaksperiode_endret", mapOf(
                "vedtaksperiodeId" to "$vedtaksperiodeId",
                "fødselsnummer" to fødselsnummer,
                "aktørId" to aktørId,
                "organisasjonsnummer" to organisasjonsnummer,
                "gjeldendeTilstand" to gjeldendeTilstand,
                "forrigeTilstand" to forrigeTilstand
            )
        )

    fun lagTilbakerulling(
        id: UUID = UUID.randomUUID(),
        vedtaksperioderSlettet: List<UUID>
    ) = nyHendelse(
        id, "person_rullet_tilbake", mapOf(
            "fødselsnummer" to fødselsnummer,
            "aktørId" to aktørId,
            "vedtaksperioderSlettet" to vedtaksperioderSlettet
        )
    )


    fun lagVedtaksperiodeForkastet(
        id: UUID = UUID.randomUUID(),
        vedtaksperiodeId: UUID = UUID.randomUUID(),
        organisasjonsnummer: String = "orgnr"
    ) =
        nyHendelse(
            id, "vedtaksperiode_forkastet", mapOf(
                "vedtaksperiodeId" to "$vedtaksperiodeId",
                "fødselsnummer" to fødselsnummer,
                "aktørId" to aktørId,
                "organisasjonsnummer" to organisasjonsnummer
            )
        )

    fun lagGodkjenningsbehov(
        id: UUID = UUID.randomUUID(),
        vedtaksperiodeId: UUID = UUID.randomUUID(),
        organisasjonsnummer: String = "orgnr",
        periodeFom: LocalDate = LocalDate.now(),
        periodeTom: LocalDate = LocalDate.now(),
        warnings: List<String> = emptyList(),
        periodetype: Saksbehandleroppgavetype = Saksbehandleroppgavetype.FØRSTEGANGSBEHANDLING
    ) =
        nyHendelse(
            id, "behov",
            mapOf(
                "@behov" to listOf("Godkjenning"),
                "aktørId" to aktørId,
                "fødselsnummer" to fødselsnummer,
                "organisasjonsnummer" to organisasjonsnummer,
                "vedtaksperiodeId" to "$vedtaksperiodeId",
                "periodeFom" to "$periodeFom",
                "periodeTom" to "$periodeTom",
                "warnings" to mapOf(
                    "aktiviteter" to warnings.map { mapOf("melding" to it) },
                    "kontekster" to emptyList<Any>()
                ),
                "periodetype" to periodetype.name
            )
        )

    fun lagPersoninfoløsning(
        id: UUID = UUID.randomUUID(),
        hendelseId: UUID = UUID.randomUUID(),
        contextId: UUID = UUID.randomUUID(),
        vedtaksperiodeId: UUID = UUID.randomUUID(),
        organisasjonsnummer: String = "orgnr"
    ) =
        nyHendelse(
            id, "behov", mapOf(
                "@final" to true,
                "@behov" to listOf("HentEnhet", "HentPersoninfo", "HentInfotrygdutbetalinger"),
                "hendelseId" to "$hendelseId",
                "contextId" to "$contextId",
                "vedtaksperiodeId" to "$vedtaksperiodeId",
                "fødselsnummer" to fødselsnummer,
                "aktørId" to aktørId,
                "orgnummer" to organisasjonsnummer,
                "HentInfotrygdutbetalinger" to mapOf(
                    "historikkFom" to "2017-01-01",
                    "historikkTom" to "2020-12-31"
                ),
                "@løsning" to mapOf(
                    "HentInfotrygdutbetalinger" to listOf(
                        mapOf(
                            "fom" to "2018-01-01",
                            "tom" to "2018-01-31",
                            "dagsats" to "1000.0",
                            "grad" to "100",
                            "typetekst" to "ArbRef",
                            "organisasjonsnummer" to organisasjonsnummer
                        )
                    ),
                    "HentEnhet" to "0301",
                    "HentPersoninfo" to mapOf(
                        "fornavn" to "Kari",
                        "mellomnavn" to "",
                        "etternavn" to "Nordmann",
                        "fødselsdato" to "1970-01-01",
                        "kjønn" to "Kvinne"
                    )
                )
            )
        )

    fun lagHentInfotrygdutbetalingerløsning(
        id: UUID = UUID.randomUUID(),
        hendelseId: UUID = UUID.randomUUID(),
        contextId: UUID = UUID.randomUUID(),
        vedtaksperiodeId: UUID = UUID.randomUUID(),
        organisasjonsnummer: String = "orgnr"
    ) =
        nyHendelse(
            id, "behov", mapOf(
                "@final" to true,
                "@behov" to listOf("HentInfotrygdutbetalinger"),
                "hendelseId" to "$hendelseId",
                "contextId" to "$contextId",
                "vedtaksperiodeId" to "$vedtaksperiodeId",
                "fødselsnummer" to fødselsnummer,
                "aktørId" to aktørId,
                "orgnummer" to organisasjonsnummer,
                "HentInfotrygdutbetalinger" to mapOf(
                    "historikkFom" to "2017-01-01",
                    "historikkTom" to "2020-12-31"
                ),
                "@løsning" to mapOf(
                    "HentInfotrygdutbetalinger" to listOf(
                        mapOf(
                            "fom" to "2018-01-01",
                            "tom" to "2018-01-31",
                            "dagsats" to "1000.0",
                            "grad" to "100",
                            "typetekst" to "ArbRef",
                            "organisasjonsnummer" to organisasjonsnummer
                        )
                    )
                )
            )
        )

    fun lagHentPersoninfoløsning(
        id: UUID = UUID.randomUUID(),
        hendelseId: UUID = UUID.randomUUID(),
        contextId: UUID = UUID.randomUUID(),
        vedtaksperiodeId: UUID = UUID.randomUUID(),
        organisasjonsnummer: String = "orgnr"
    ) =
        nyHendelse(
            id, "behov", mapOf(
                "@final" to true,
                "@behov" to listOf("HentPersoninfo"),
                "hendelseId" to "$hendelseId",
                "contextId" to "$contextId",
                "vedtaksperiodeId" to "$vedtaksperiodeId",
                "fødselsnummer" to fødselsnummer,
                "aktørId" to aktørId,
                "orgnummer" to organisasjonsnummer,
                "@løsning" to mapOf(
                    "HentPersoninfo" to mapOf(
                        "fornavn" to "Kari",
                        "mellomnavn" to "",
                        "etternavn" to "Nordmann",
                        "fødselsdato" to "1970-01-01",
                        "kjønn" to "Kvinne"
                    )
                )
            )
        )

    fun lagHentEnhetløsning(
        id: UUID = UUID.randomUUID(),
        hendelseId: UUID = UUID.randomUUID(),
        contextId: UUID = UUID.randomUUID(),
        vedtaksperiodeId: UUID = UUID.randomUUID(),
        organisasjonsnummer: String = "orgnr",
        enhet: String = OSLO
    ) =
        nyHendelse(
            id, "behov", mapOf(
                "@final" to true,
                "@behov" to listOf("HentEnhet"),
                "hendelseId" to "$hendelseId",
                "contextId" to contextId,
                "vedtaksperiodeId" to "$vedtaksperiodeId",
                "fødselsnummer" to fødselsnummer,
                "aktørId" to aktørId,
                "orgnummer" to organisasjonsnummer,
                "@løsning" to mapOf(
                    "HentEnhet" to enhet
                )
            )
        )

    fun lagSaksbehandlerløsning(
        id: UUID = UUID.randomUUID(),
        hendelseId: UUID = UUID.randomUUID(),
        contextId: UUID = UUID.randomUUID(),
        oppgaveId: Long = nextLong(),
        godkjent: Boolean = true,
        godkjenttidspunkt: LocalDateTime = LocalDateTime.now(),
        saksbehandlerident: String = "Z999999",
        saksbehandlerepost: String = "saksbehandler@nav.no",
        saksbehandlerOID: UUID = UUID.randomUUID(),
        årsak: String? = null,
        begrunnelser: List<String>? = null,
        kommentar: String? = null
    ) =
        nyHendelse(
            id, "saksbehandler_løsning", mutableMapOf<String, Any>(
                "fødselsnummer" to fødselsnummer,
                "hendelseId" to hendelseId,
                "contextId" to contextId,
                "oppgaveId" to oppgaveId,
                "godkjent" to godkjent,
                "godkjenttidspunkt" to godkjenttidspunkt,
                "saksbehandlerident" to saksbehandlerident,
                "saksbehandlerepost" to saksbehandlerepost,
                "saksbehandleroid" to saksbehandlerOID
            ).apply {
                årsak?.also { put("årsak", it) }
                begrunnelser?.also { put("begrunnelser", it) }
                kommentar?.also { put("kommentar", it) }
            }
        )

    fun lagDigitalKontaktinformasjonløsning(
        id: UUID = UUID.randomUUID(),
        hendelseId: UUID = UUID.randomUUID(),
        contextId: UUID = UUID.randomUUID(),
        erDigital: Boolean = true
    ): String =
        nyHendelse(
            id,
            "behov", mutableMapOf(
                "fødselsnummer" to fødselsnummer,
                "@final" to true,
                "@behov" to listOf("DigitalKontaktinformasjon"),
                "contextId" to contextId,
                "hendelseId" to hendelseId,
                "@løsning" to mapOf(
                    "DigitalKontaktinformasjon" to mapOf(
                        "erDigital" to erDigital
                    )
                )
            )
        )

    fun lagÅpneGosysOppgaverløsning(
        id: UUID = UUID.randomUUID(),
        hendelseId: UUID = UUID.randomUUID(),
        contextId: UUID = UUID.randomUUID(),
        antall: Int = 0,
        oppslagFeilet: Boolean = false
    ): String =
        nyHendelse(
            id,
            "behov", mutableMapOf(
                "fødselsnummer" to fødselsnummer,
                "@final" to true,
                "@behov" to listOf("ÅpneOppgaver"),
                "contextId" to contextId,
                "hendelseId" to hendelseId,
                "@løsning" to mapOf(
                    "ÅpneOppgaver" to mapOf(
                        "antall" to antall,
                        "oppslagFeilet" to oppslagFeilet
                    )
                )
            )
        )

    fun lagRisikovurderingløsning(
        id: UUID = UUID.randomUUID(),
        hendelseId: UUID = UUID.randomUUID(),
        contextId: UUID = UUID.randomUUID(),
        vedtaksperiodeId: UUID,
        begrunnelserSomAleneKreverManuellBehandling: List<String> = emptyList()
    ): String =
        nyHendelse(
            id,
            "behov", mutableMapOf(
                "fødselsnummer" to fødselsnummer,
                "@final" to true,
                "@behov" to listOf("Risikovurdering"),
                "contextId" to contextId,
                "hendelseId" to hendelseId,
                "Risikovurdering" to mapOf(
                    "vedtaksperiodeId" to vedtaksperiodeId.toString(),
                    "organisasjonsnummer" to "815493000"
                ),
                "@løsning" to mapOf(
                    "Risikovurdering" to mapOf(
                        "begrunnelserSomAleneKreverManuellBehandling" to begrunnelserSomAleneKreverManuellBehandling,
                        "samletScore" to 10.0,
                        "begrunnelser" to emptyList<String>() + begrunnelserSomAleneKreverManuellBehandling,
                        "ufullstendig" to false
                    )
                )
            )
        )

    fun lagOverstyring(
        id: UUID = UUID.randomUUID(),
        organisasjonsnummer: String = "orgnr",
        dager: List<OverstyringDagDto> = emptyList(),
        begrunnelse: String = "begrunnelse",
        saksbehandlerOid: UUID = UUID.randomUUID(),
        saksbehandlerNavn: String = "saksbehandler",
        saksbehandlerEpost: String = "saksbehandler@nav.no"
    ) = nyHendelse(
        id, "overstyr_tidslinje", mapOf(
            "aktørId" to aktørId,
            "fødselsnummer" to fødselsnummer,
            "organisasjonsnummer" to organisasjonsnummer,
            "dager" to dager,
            "begrunnelse" to begrunnelse,
            "saksbehandlerOid" to saksbehandlerOid,
            "saksbehandlerNavn" to saksbehandlerNavn,
            "saksbehandlerEpost" to saksbehandlerEpost
        )
    )

    private fun nyHendelse(id: UUID, navn: String, hendelse: Map<String, Any>) =
        JsonMessage.newMessage(nyHendelse(id, navn) + hendelse).toJson()

    private fun nyHendelse(id: UUID, navn: String) = mutableMapOf(
        "@event_name" to navn,
        "@id" to id,
        "@opprettet" to LocalDateTime.now()
    )
}

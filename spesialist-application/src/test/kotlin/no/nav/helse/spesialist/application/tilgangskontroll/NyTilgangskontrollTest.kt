package no.nav.helse.spesialist.application.tilgangskontroll

import no.nav.helse.db.PartialDaos
import no.nav.helse.db.api.EgenAnsattApiDao
import no.nav.helse.db.api.PartialPersonApiDao
import no.nav.helse.mediator.TilgangskontrollørForApi
import no.nav.helse.modell.oppgave.Egenskap
import no.nav.helse.modell.saksbehandler.Tilgangskontroll
import no.nav.helse.spesialist.api.bootstrap.Gruppe
import no.nav.helse.spesialist.api.bootstrap.Tilgangsgrupper
import no.nav.helse.spesialist.api.person.Adressebeskyttelse
import no.nav.helse.spesialist.domain.Saksbehandler
import no.nav.helse.spesialist.domain.SaksbehandlerOid
import no.nav.helse.spesialist.domain.legacy.LegacySaksbehandler
import no.nav.helse.spesialist.domain.testfixtures.lagEpostadresseFraFulltNavn
import no.nav.helse.spesialist.domain.testfixtures.lagEtternavn
import no.nav.helse.spesialist.domain.testfixtures.lagFornavn
import no.nav.helse.spesialist.domain.testfixtures.lagFødselsnummer
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.UUID
import java.util.stream.Stream


class NyTilgangskontrollTest {

    private val fødselsnummer = lagFødselsnummer()

    @ParameterizedTest(name = "egenAnsatt={0}, adressebeskyttelse={1}")
    @MethodSource("personKombinasjonerIngenHarTilgangTil")
    fun `saksbehandler har ikke tilgang til person selv med alle tilganger`(
        egenAnsatt: Boolean?,
        adressebeskyttelse: Adressebeskyttelse?,
    ) {
        personErEgenAnsattMap[fødselsnummer] = egenAnsatt
        personAdressebeskyttelseMap[fødselsnummer] = adressebeskyttelse

        assertFalse(harTilgangTilPersonGittGrupper(fødselsnummer, SaksbehandlerGruppe.entries))
    }

    @ParameterizedTest(name = "egenAnsatt={0}, adressebeskyttelse={1}, grupper={2}")
    @MethodSource("personKombinasjonerSomGirTilgang")
    fun `saksbehandler med visse grupper har tilgang til visse personer`(
        egenAnsatt: Boolean,
        adressebeskyttelse: Adressebeskyttelse,
        grupper: List<SaksbehandlerGruppe>
    ) {
        personErEgenAnsattMap[fødselsnummer] = egenAnsatt
        personAdressebeskyttelseMap[fødselsnummer] = adressebeskyttelse

        assertTrue(harTilgangTilPersonGittGrupper(fødselsnummer, grupper))
    }

    @ParameterizedTest(name = "egenAnsatt={0}, adressebeskyttelse={1}, grupper={2}")
    @MethodSource("personKombinasjonerSomIkkeGirTilgang")
    fun `saksbehandler med visse grupper har ikke tilgang til visse personer`(
        egenAnsatt: Boolean,
        adressebeskyttelse: Adressebeskyttelse,
        grupper: List<SaksbehandlerGruppe>
    ) {
        personErEgenAnsattMap[fødselsnummer] = egenAnsatt
        personAdressebeskyttelseMap[fødselsnummer] = adressebeskyttelse

        assertFalse(harTilgangTilPersonGittGrupper(fødselsnummer, grupper))
    }

    @ParameterizedTest(name = "egenskaper={0}")
    @MethodSource("oppgaveKombinasjonerIngenHarTilgangTil")
    fun `saksbehandler har ikke tilgang til oppgave selv med kjente alle tilganger`(
        egenskaper: List<Egenskap>,
    ) {
        assertFalse(harTilgangTilOppgaveGittGrupper(egenskaper, SaksbehandlerGruppe.entries))
    }

    @ParameterizedTest(name = "egenskaper={0}, grupper={1}")
    @MethodSource("oppgaveKombinasjonerSomGirTilgang")
    fun `saksbehandler med visse kjente grupper har tilgang til visse oppgaver`(
        egenskaper: List<Egenskap>,
        grupper: List<SaksbehandlerGruppe>
    ) {
        assertTrue(harTilgangTilOppgaveGittGrupper(egenskaper, grupper))
    }

    @ParameterizedTest(name = "egenskaper={0}, grupper={1}")
    @MethodSource("oppgaveKombinasjonerSomIkkeGirTilgang")
    fun `saksbehandler med visse kjente grupper har ikke tilgang til visse oppgaver`(
        egenskaper: List<Egenskap>,
        grupper: List<SaksbehandlerGruppe>
    ) {
        assertFalse(harTilgangTilOppgaveGittGrupper(egenskaper, grupper))
    }

    @ParameterizedTest(name = "egenskaper={0}")
    @MethodSource("oppgaveKombinasjonerIngenHarTilgangTil")
    fun `saksbehandler har ikke tilgang til oppgave selv med oppslåtte alle tilganger`(
        egenskaper: List<Egenskap>,
    ) {
        assertFalse(harTilgangTilOppgaveGittGrupperSomSlåsOpp(egenskaper, SaksbehandlerGruppe.entries))
    }

    @ParameterizedTest(name = "egenskaper={0}, grupper={1}")
    @MethodSource("oppgaveKombinasjonerSomGirTilgang")
    fun `saksbehandler med visse oppslåtte grupper har tilgang til visse oppgaver`(
        egenskaper: List<Egenskap>,
        grupper: List<SaksbehandlerGruppe>
    ) {
        assertTrue(harTilgangTilOppgaveGittGrupperSomSlåsOpp(egenskaper, grupper))
    }

    @ParameterizedTest(name = "egenskaper={0}, grupper={1}")
    @MethodSource("oppgaveKombinasjonerSomIkkeGirTilgang")
    fun `saksbehandler med visse oppslåtte grupper har ikke tilgang til visse oppgaver`(
        egenskaper: List<Egenskap>,
        grupper: List<SaksbehandlerGruppe>
    ) {
        assertFalse(harTilgangTilOppgaveGittGrupperSomSlåsOpp(egenskaper, grupper))
    }

    companion object {
        @JvmStatic
        private fun personKombinasjonerIngenHarTilgangTil(): Stream<Arguments> =
            (Adressebeskyttelse.entries + null).flatMap { adressebeskyttelse ->
                // Ingen har tilgang til personer som har erEgenAnsatt == null
                listOf(Arguments.of(null, adressebeskyttelse)) +
                        if (adressebeskyttelse !in setOf(Adressebeskyttelse.Ugradert, Adressebeskyttelse.Fortrolig)) {
                            // Ingen har tilgang til noe annet enn (kjent) Adressebeskyttelse Ugradert eller Fortrolig
                            listOf(
                                Arguments.of(false, adressebeskyttelse),
                                Arguments.of(true, adressebeskyttelse)
                            )
                        } else emptyList()
            }.stream()

        @JvmStatic
        private fun personKombinasjonerSomGirTilgang(): Stream<Arguments> =
            Stream.of(
                Arguments.of(false, Adressebeskyttelse.Ugradert, emptyList<SaksbehandlerGruppe>()),
                Arguments.of(false, Adressebeskyttelse.Fortrolig, listOf(SaksbehandlerGruppe.KODE_7)),
                Arguments.of(true, Adressebeskyttelse.Ugradert, listOf(SaksbehandlerGruppe.SKJERMEDE_PERSONER)),
            )

        @JvmStatic
        private fun personKombinasjonerSomIkkeGirTilgang(): Stream<Arguments> = Stream.of(
            Arguments.of(
                false,
                Adressebeskyttelse.Fortrolig,
                emptyList<SaksbehandlerGruppe>()
            ),
            Arguments.of(
                false,
                Adressebeskyttelse.Fortrolig,
                SaksbehandlerGruppe.entries - SaksbehandlerGruppe.KODE_7
            ),
            Arguments.of(
                true,
                Adressebeskyttelse.Ugradert,
                emptyList<SaksbehandlerGruppe>()
            ),
            Arguments.of(
                true,
                Adressebeskyttelse.Ugradert,
                SaksbehandlerGruppe.entries - SaksbehandlerGruppe.SKJERMEDE_PERSONER
            ),
        )

        @JvmStatic
        private fun oppgaveKombinasjonerIngenHarTilgangTil(): Stream<Arguments> = Stream.of(
            Arguments.of(
                listOf(Egenskap.STRENGT_FORTROLIG_ADRESSE)
            )
        )

        @JvmStatic
        private fun oppgaveKombinasjonerSomGirTilgang(): Stream<Arguments> = Stream.of(
            Arguments.of(
                emptyList<Egenskap>(),
                emptyList<SaksbehandlerGruppe>()
            ),
            Arguments.of(
                listOf(Egenskap.FORTROLIG_ADRESSE),
                listOf(SaksbehandlerGruppe.KODE_7)
            ),
            Arguments.of(
                listOf(Egenskap.EGEN_ANSATT),
                listOf(SaksbehandlerGruppe.SKJERMEDE_PERSONER)
            ),
            Arguments.of(
                listOf(Egenskap.BESLUTTER),
                listOf(SaksbehandlerGruppe.BESLUTTER)
            ),
            Arguments.of(
                listOf(Egenskap.STIKKPRØVE),
                listOf(SaksbehandlerGruppe.STIKKPRØVE)
            ),
        )

        @JvmStatic
        private fun oppgaveKombinasjonerSomIkkeGirTilgang(): Stream<Arguments> =
            Stream.of(
                Arguments.of(
                    listOf(Egenskap.FORTROLIG_ADRESSE),
                    emptyList<SaksbehandlerGruppe>()
                ),
                Arguments.of(
                    listOf(Egenskap.EGEN_ANSATT),
                    emptyList<SaksbehandlerGruppe>()
                ),
                Arguments.of(
                    listOf(Egenskap.BESLUTTER),
                    emptyList<SaksbehandlerGruppe>()
                ),
                Arguments.of(
                    listOf(Egenskap.STIKKPRØVE),
                    emptyList<SaksbehandlerGruppe>()
                ),
                Arguments.of(
                    listOf(Egenskap.FORTROLIG_ADRESSE),
                    SaksbehandlerGruppe.entries - SaksbehandlerGruppe.KODE_7
                ),
                Arguments.of(
                    listOf(Egenskap.EGEN_ANSATT),
                    SaksbehandlerGruppe.entries - SaksbehandlerGruppe.SKJERMEDE_PERSONER
                ),
                Arguments.of(
                    listOf(Egenskap.BESLUTTER),
                    SaksbehandlerGruppe.entries - SaksbehandlerGruppe.BESLUTTER
                ),
                Arguments.of(
                    listOf(Egenskap.STIKKPRØVE),
                    SaksbehandlerGruppe.entries - SaksbehandlerGruppe.STIKKPRØVE
                ),
            )

        private val tilgangsgrupper = object : Tilgangsgrupper {
            override val kode7GruppeId: UUID = UUID.randomUUID()
            override val beslutterGruppeId: UUID = UUID.randomUUID()
            override val skjermedePersonerGruppeId: UUID = UUID.randomUUID()
            override val stikkprøveGruppeId: UUID = UUID.randomUUID()
            override fun gruppeId(gruppe: Gruppe) = error("Brukes bare i tester, bør ikke eksistere / bli kalt")
        }
    }

    private fun harTilgangTilPersonGittGrupper(fødselsnummer: String, grupper: List<SaksbehandlerGruppe>) =
        tilgangskontroll.harTilgangTilPerson(
            saksbehandlerTilganger = SaksbehandlerTilganger(
                gruppetilganger = grupper.tilUUIDer(),
                kode7Saksbehandlergruppe = tilgangsgrupper.kode7GruppeId,
                beslutterSaksbehandlergruppe = tilgangsgrupper.beslutterGruppeId,
                skjermedePersonerSaksbehandlergruppe = tilgangsgrupper.skjermedePersonerGruppeId,
            ),
            fødselsnummer = fødselsnummer
        )

    private fun harTilgangTilOppgaveGittGrupper(
        egenskaper: List<Egenskap>,
        grupper: List<SaksbehandlerGruppe>
    ) = tilgangskontroll.harTilgangTilOppgave(
        saksbehandler = lagLegacySaksbehandler(
            tilgangskontrollør = TilgangskontrollørForApi(
                saksbehandlergrupper = grupper.tilUUIDer(),
                tilgangsgrupper = tilgangsgrupper
            )
        ),
        egenskaper = egenskaper
    )

    private fun harTilgangTilOppgaveGittGrupperSomSlåsOpp(
        egenskaper: List<Egenskap>,
        grupper: List<SaksbehandlerGruppe>
    ): Boolean {
        val saksbehandler = lagSaksbehandler()
        saksbehandlerGruppeOppslagMap[saksbehandler.id().value] = grupper.tilUUIDer().toSet()
        return tilgangskontroll.harTilgangTilOppgave(
            saksbehandler = saksbehandler,
            egenskaper = egenskaper
        )
    }

    private fun lagLegacySaksbehandler(
        ident: String = "A123456",
        tilgangskontrollør: Tilgangskontroll
    ): LegacySaksbehandler {
        val navn = lagFornavn() + " " + lagEtternavn()
        return LegacySaksbehandler(
            epostadresse = lagEpostadresseFraFulltNavn(navn),
            oid = UUID.randomUUID(),
            navn = navn,
            ident = ident,
            tilgangskontroll = tilgangskontrollør,
        )
    }

    private fun lagSaksbehandler(ident: String = "A123456"): Saksbehandler {
        val navn = lagFornavn() + " " + lagEtternavn()
        return Saksbehandler(
            id = SaksbehandlerOid(UUID.randomUUID()),
            navn = navn,
            epost = lagEpostadresseFraFulltNavn(navn),
            ident = ident,
        )
    }

    private val personErEgenAnsattMap = mutableMapOf<String, Boolean?>()
    private val personAdressebeskyttelseMap = mutableMapOf<String, Adressebeskyttelse?>()
    private val saksbehandlerGruppeOppslagMap = mutableMapOf<UUID, Set<UUID>>()

    private val tilgangskontroll = NyTilgangskontroll(
        daos = object : PartialDaos {
            override val egenAnsattApiDao = object : EgenAnsattApiDao {
                override fun erEgenAnsatt(fødselsnummer: String) =
                    personErEgenAnsattMap[fødselsnummer]
            }
            override val personApiDao = object : PartialPersonApiDao {
                override fun personHarAdressebeskyttelse(
                    fødselsnummer: String,
                    adressebeskyttelse: Adressebeskyttelse
                ) = personAdressebeskyttelseMap[fødselsnummer] == adressebeskyttelse
            }
        },
        tilgangsgruppehenter = object : Tilgangsgruppehenter {
            override suspend fun hentTilgangsgrupper(oid: UUID, gruppeIder: List<UUID>) =
                saksbehandlerGruppeOppslagMap[oid].orEmpty()
        },
        tilgangsgrupper = tilgangsgrupper
    )

    private fun List<SaksbehandlerGruppe>.tilUUIDer(): List<UUID> =
        map {
            when (it) {
                SaksbehandlerGruppe.KODE_7 -> tilgangsgrupper.kode7GruppeId
                SaksbehandlerGruppe.BESLUTTER -> tilgangsgrupper.beslutterGruppeId
                SaksbehandlerGruppe.SKJERMEDE_PERSONER -> tilgangsgrupper.skjermedePersonerGruppeId
                SaksbehandlerGruppe.STIKKPRØVE -> tilgangsgrupper.stikkprøveGruppeId
            }
        }

    enum class SaksbehandlerGruppe {
        KODE_7,
        BESLUTTER,
        SKJERMEDE_PERSONER,
        STIKKPRØVE,
    }
}

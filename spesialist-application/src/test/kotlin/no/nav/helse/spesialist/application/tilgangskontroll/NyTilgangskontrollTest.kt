package no.nav.helse.spesialist.application.tilgangskontroll

import no.nav.helse.db.api.EgenAnsattApiDao
import no.nav.helse.db.api.PartialPersonApiDao
import no.nav.helse.modell.oppgave.Egenskap
import no.nav.helse.spesialist.api.person.Adressebeskyttelse
import no.nav.helse.spesialist.domain.Saksbehandler
import no.nav.helse.spesialist.domain.SaksbehandlerOid
import no.nav.helse.spesialist.domain.testfixtures.lagEpostadresseFraFulltNavn
import no.nav.helse.spesialist.domain.testfixtures.lagEtternavn
import no.nav.helse.spesialist.domain.testfixtures.lagFornavn
import no.nav.helse.spesialist.domain.testfixtures.lagFødselsnummer
import no.nav.helse.spesialist.domain.tilgangskontroll.SaksbehandlerTilganger
import no.nav.helse.spesialist.domain.tilgangskontroll.Tilgangsgruppe
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

        assertFalse(
            harTilgangTilPersonGittGrupper(
                fødselsnummer = fødselsnummer,
                grupper = tilgangsgrupper.alleGrupper().toSet()
            )
        )
    }

    @ParameterizedTest(name = "egenAnsatt={0}, adressebeskyttelse={1}, grupper={2}")
    @MethodSource("personKombinasjonerSomGirTilgang")
    fun `saksbehandler med visse grupper har tilgang til visse personer`(
        egenAnsatt: Boolean,
        adressebeskyttelse: Adressebeskyttelse,
        grupper: Set<Tilgangsgruppe>
    ) {
        personErEgenAnsattMap[fødselsnummer] = egenAnsatt
        personAdressebeskyttelseMap[fødselsnummer] = adressebeskyttelse

        assertTrue(
            harTilgangTilPersonGittGrupper(
                fødselsnummer = fødselsnummer,
                grupper = grupper
            )
        )
    }

    @ParameterizedTest(name = "egenAnsatt={0}, adressebeskyttelse={1}, grupper={2}")
    @MethodSource("personKombinasjonerSomIkkeGirTilgang")
    fun `saksbehandler med visse grupper har ikke tilgang til visse personer`(
        egenAnsatt: Boolean,
        adressebeskyttelse: Adressebeskyttelse,
        grupper: Set<Tilgangsgruppe>
    ) {
        personErEgenAnsattMap[fødselsnummer] = egenAnsatt
        personAdressebeskyttelseMap[fødselsnummer] = adressebeskyttelse

        assertFalse(
            harTilgangTilPersonGittGrupper(
                fødselsnummer = fødselsnummer,
                grupper = grupper
            )
        )
    }

    @ParameterizedTest(name = "egenskaper={0}")
    @MethodSource("oppgaveKombinasjonerIngenHarTilgangTil")
    fun `saksbehandler har ikke tilgang til oppgave selv med kjente alle tilganger`(egenskaper: Set<Egenskap>) {
        assertFalse(
            harTilgangTilOppgaveMedEgenskaperGittEksplisitteGrupper(
                egenskaper = egenskaper,
                saksbehandler = lagSaksbehandler(),
                grupper = tilgangsgrupper.alleGrupper().toSet()
            )
        )
    }

    @ParameterizedTest(name = "egenskaper={0}, grupper={1}")
    @MethodSource("oppgaveKombinasjonerSomGirTilgang")
    fun `saksbehandler med visse kjente grupper har tilgang til visse oppgaver`(
        egenskaper: Set<Egenskap>,
        grupper: Set<Tilgangsgruppe>
    ) {
        assertTrue(
            harTilgangTilOppgaveMedEgenskaperGittEksplisitteGrupper(
                egenskaper = egenskaper,
                saksbehandler = lagSaksbehandler(),
                grupper = grupper
            )
        )
    }

    @ParameterizedTest(name = "egenskaper={0}, grupper={1}")
    @MethodSource("oppgaveKombinasjonerSomIkkeGirTilgang")
    fun `saksbehandler med visse kjente grupper har ikke tilgang til visse oppgaver`(
        egenskaper: Set<Egenskap>,
        grupper: Set<Tilgangsgruppe>
    ) {
        assertFalse(
            harTilgangTilOppgaveMedEgenskaperGittEksplisitteGrupper(
                egenskaper = egenskaper,
                saksbehandler = lagSaksbehandler(),
                grupper = grupper
            )
        )
    }

    @ParameterizedTest(name = "egenskaper={0}")
    @MethodSource("oppgaveKombinasjonerIngenHarTilgangTil")
    fun `saksbehandler har ikke tilgang til oppgave selv med oppslåtte alle tilganger`(
        egenskaper: Set<Egenskap>,
    ) {
        assertFalse(
            harTilgangTilOppgaveMedEgenskaperGittGrupperSomSlåsOpp(
                egenskaper = egenskaper,
                saksbehandler = lagSaksbehandler(),
                grupper = tilgangsgrupper.alleGrupper().toSet()
            )
        )
    }

    @ParameterizedTest(name = "egenskaper={0}, grupper={1}")
    @MethodSource("oppgaveKombinasjonerSomGirTilgang")
    fun `saksbehandler med visse oppslåtte grupper har tilgang til visse oppgaver`(
        egenskaper: Set<Egenskap>,
        grupper: Set<Tilgangsgruppe>
    ) {
        assertTrue(
            harTilgangTilOppgaveMedEgenskaperGittGrupperSomSlåsOpp(
                egenskaper = egenskaper,
                saksbehandler = lagSaksbehandler(),
                grupper = grupper
            )
        )
    }

    @ParameterizedTest(name = "egenskaper={0}, grupper={1}")
    @MethodSource("oppgaveKombinasjonerSomIkkeGirTilgang")
    fun `saksbehandler med visse oppslåtte grupper har ikke tilgang til visse oppgaver`(
        egenskaper: Set<Egenskap>,
        grupper: Set<Tilgangsgruppe>
    ) {
        assertFalse(
            harTilgangTilOppgaveMedEgenskaperGittGrupperSomSlåsOpp(
                egenskaper = egenskaper,
                saksbehandler = lagSaksbehandler(),
                grupper = grupper
            )
        )
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
                Arguments.of(false, Adressebeskyttelse.Ugradert, emptySet<Tilgangsgruppe>()),
                Arguments.of(false, Adressebeskyttelse.Fortrolig, setOf(Tilgangsgruppe.KODE7)),
                Arguments.of(true, Adressebeskyttelse.Ugradert, setOf(Tilgangsgruppe.SKJERMEDE)),
            )

        @JvmStatic
        private fun personKombinasjonerSomIkkeGirTilgang(): Stream<Arguments> = Stream.of(
            Arguments.of(
                false,
                Adressebeskyttelse.Fortrolig,
                emptySet<Tilgangsgruppe>()
            ),
            Arguments.of(
                false,
                Adressebeskyttelse.Fortrolig,
                (tilgangsgrupper.alleGrupper() - Tilgangsgruppe.KODE7).toSet()
            ),
            Arguments.of(
                true,
                Adressebeskyttelse.Ugradert,
                emptySet<Tilgangsgruppe>()
            ),
            Arguments.of(
                true,
                Adressebeskyttelse.Ugradert,
                (tilgangsgrupper.alleGrupper() - Tilgangsgruppe.SKJERMEDE).toSet()
            ),
        )

        @JvmStatic
        private fun oppgaveKombinasjonerIngenHarTilgangTil(): Stream<Arguments> = Stream.of(
            Arguments.of(
                setOf(Egenskap.STRENGT_FORTROLIG_ADRESSE)
            )
        )

        @JvmStatic
        private fun oppgaveKombinasjonerSomGirTilgang(): Stream<Arguments> = Stream.of(
            Arguments.of(
                emptySet<Egenskap>(),
                emptySet<Tilgangsgruppe>()
            ),
            Arguments.of(
                setOf(Egenskap.FORTROLIG_ADRESSE),
                setOf(Tilgangsgruppe.KODE7)
            ),
            Arguments.of(
                setOf(Egenskap.EGEN_ANSATT),
                setOf(Tilgangsgruppe.SKJERMEDE)
            ),
            Arguments.of(
                setOf(Egenskap.BESLUTTER),
                setOf(Tilgangsgruppe.BESLUTTER)
            ),
            Arguments.of(
                setOf(Egenskap.STIKKPRØVE),
                setOf(Tilgangsgruppe.STIKKPRØVE)
            ),
        )

        @JvmStatic
        private fun oppgaveKombinasjonerSomIkkeGirTilgang(): Stream<Arguments> =
            Stream.of(
                Arguments.of(
                    setOf(Egenskap.FORTROLIG_ADRESSE),
                    emptySet<Tilgangsgruppe>()
                ),
                Arguments.of(
                    setOf(Egenskap.EGEN_ANSATT),
                    emptySet<Tilgangsgruppe>()
                ),
                Arguments.of(
                    setOf(Egenskap.BESLUTTER),
                    emptySet<Tilgangsgruppe>()
                ),
                Arguments.of(
                    setOf(Egenskap.STIKKPRØVE),
                    emptySet<Tilgangsgruppe>()
                ),
                Arguments.of(
                    setOf(Egenskap.FORTROLIG_ADRESSE),
                    (tilgangsgrupper.alleGrupper() - Tilgangsgruppe.KODE7).toSet()
                ),
                Arguments.of(
                    setOf(Egenskap.EGEN_ANSATT),
                    (tilgangsgrupper.alleGrupper() - Tilgangsgruppe.SKJERMEDE).toSet()
                ),
                Arguments.of(
                    setOf(Egenskap.BESLUTTER),
                    (tilgangsgrupper.alleGrupper() - Tilgangsgruppe.BESLUTTER).toSet()
                ),
                Arguments.of(
                    setOf(Egenskap.STIKKPRØVE),
                    (tilgangsgrupper.alleGrupper() - Tilgangsgruppe.STIKKPRØVE).toSet()
                ),
            )

        private val tilgangsgrupper = randomTilgangsgrupper()
    }

    private fun harTilgangTilPersonGittGrupper(fødselsnummer: String, grupper: Set<Tilgangsgruppe>) =
        tilgangskontroll.harTilgangTilPerson(
            saksbehandlerTilganger = SaksbehandlerTilganger(
                gruppetilganger = tilgangsgrupper.uuiderFor(grupper).toList(),
                kode7Saksbehandlergruppe = tilgangsgrupper.uuidFor(Tilgangsgruppe.KODE7),
                beslutterSaksbehandlergruppe = tilgangsgrupper.uuidFor(Tilgangsgruppe.BESLUTTER),
                skjermedePersonerSaksbehandlergruppe = tilgangsgrupper.uuidFor(Tilgangsgruppe.SKJERMEDE),
            ),
            fødselsnummer = fødselsnummer
        )

    private fun harTilgangTilOppgaveMedEgenskaperGittEksplisitteGrupper(
        egenskaper: Set<Egenskap>,
        saksbehandler: Saksbehandler = lagSaksbehandler(),
        grupper: Set<Tilgangsgruppe>
    ): Boolean =
        egenskaper.all {
            tilgangskontroll.harTilgangTilOppgaveMedEgenskap(
                egenskap = it,
                saksbehandler = saksbehandler,
                tilgangsgrupper = grupper
            )
        }

    private fun harTilgangTilOppgaveMedEgenskaperGittGrupperSomSlåsOpp(
        egenskaper: Set<Egenskap>,
        saksbehandler: Saksbehandler = lagSaksbehandler(),
        grupper: Set<Tilgangsgruppe>
    ): Boolean {
        saksbehandlerTilgangsgruppeOppslagMap[saksbehandler.id().value] = grupper
        return tilgangskontroll.harTilgangTilOppgaveMedEgenskaper(
            egenskaper = egenskaper,
            saksbehandler = saksbehandler
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
    private val saksbehandlerTilgangsgruppeOppslagMap = mutableMapOf<UUID, Set<Tilgangsgruppe>>()

    private val tilgangskontroll = NyTilgangskontroll(
        egenAnsattApiDao = object : EgenAnsattApiDao {
            override fun erEgenAnsatt(fødselsnummer: String) =
                personErEgenAnsattMap[fødselsnummer]
        },
        personApiDao = object : PartialPersonApiDao {
            override fun personHarAdressebeskyttelse(
                fødselsnummer: String,
                adressebeskyttelse: Adressebeskyttelse
            ) = personAdressebeskyttelseMap[fødselsnummer] == adressebeskyttelse
        },
        tilgangsgruppehenter = object : Tilgangsgruppehenter {
            override suspend fun hentTilgangsgrupper(oid: UUID, gruppeIder: List<UUID>) =
                tilgangsgrupper.uuiderFor(saksbehandlerTilgangsgruppeOppslagMap[oid].orEmpty())

            override suspend fun hentTilgangsgrupper(oid: UUID) =
                saksbehandlerTilgangsgruppeOppslagMap[oid].orEmpty()
        }
    )
}

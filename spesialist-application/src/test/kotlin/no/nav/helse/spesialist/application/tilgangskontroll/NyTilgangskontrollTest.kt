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
        grupper: Set<Gruppe>
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
        grupper: Set<Gruppe>
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
        grupper: Set<Gruppe>
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
        grupper: Set<Gruppe>
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
        grupper: Set<Gruppe>
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
        grupper: Set<Gruppe>
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
                Arguments.of(false, Adressebeskyttelse.Ugradert, emptySet<Gruppe>()),
                Arguments.of(false, Adressebeskyttelse.Fortrolig, setOf(Gruppe.KODE7)),
                Arguments.of(true, Adressebeskyttelse.Ugradert, setOf(Gruppe.SKJERMEDE)),
            )

        @JvmStatic
        private fun personKombinasjonerSomIkkeGirTilgang(): Stream<Arguments> = Stream.of(
            Arguments.of(
                false,
                Adressebeskyttelse.Fortrolig,
                emptySet<Gruppe>()
            ),
            Arguments.of(
                false,
                Adressebeskyttelse.Fortrolig,
                (tilgangsgrupper.alleGrupper() - Gruppe.KODE7).toSet()
            ),
            Arguments.of(
                true,
                Adressebeskyttelse.Ugradert,
                emptySet<Gruppe>()
            ),
            Arguments.of(
                true,
                Adressebeskyttelse.Ugradert,
                (tilgangsgrupper.alleGrupper() - Gruppe.SKJERMEDE).toSet()
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
                emptySet<Gruppe>()
            ),
            Arguments.of(
                setOf(Egenskap.FORTROLIG_ADRESSE),
                setOf(Gruppe.KODE7)
            ),
            Arguments.of(
                setOf(Egenskap.EGEN_ANSATT),
                setOf(Gruppe.SKJERMEDE)
            ),
            Arguments.of(
                setOf(Egenskap.BESLUTTER),
                setOf(Gruppe.BESLUTTER)
            ),
            Arguments.of(
                setOf(Egenskap.STIKKPRØVE),
                setOf(Gruppe.STIKKPRØVE)
            ),
        )

        @JvmStatic
        private fun oppgaveKombinasjonerSomIkkeGirTilgang(): Stream<Arguments> =
            Stream.of(
                Arguments.of(
                    setOf(Egenskap.FORTROLIG_ADRESSE),
                    emptySet<Gruppe>()
                ),
                Arguments.of(
                    setOf(Egenskap.EGEN_ANSATT),
                    emptySet<Gruppe>()
                ),
                Arguments.of(
                    setOf(Egenskap.BESLUTTER),
                    emptySet<Gruppe>()
                ),
                Arguments.of(
                    setOf(Egenskap.STIKKPRØVE),
                    emptySet<Gruppe>()
                ),
                Arguments.of(
                    setOf(Egenskap.FORTROLIG_ADRESSE),
                    (tilgangsgrupper.alleGrupper() - Gruppe.KODE7).toSet()
                ),
                Arguments.of(
                    setOf(Egenskap.EGEN_ANSATT),
                    (tilgangsgrupper.alleGrupper() - Gruppe.SKJERMEDE).toSet()
                ),
                Arguments.of(
                    setOf(Egenskap.BESLUTTER),
                    (tilgangsgrupper.alleGrupper() - Gruppe.BESLUTTER).toSet()
                ),
                Arguments.of(
                    setOf(Egenskap.STIKKPRØVE),
                    (tilgangsgrupper.alleGrupper() - Gruppe.STIKKPRØVE).toSet()
                ),
            )

        private val tilgangsgrupper = randomTilgangsgrupper()
    }

    private fun harTilgangTilPersonGittGrupper(fødselsnummer: String, grupper: Set<Gruppe>) =
        tilgangskontroll.harTilgangTilPerson(
            saksbehandlerTilganger = SaksbehandlerTilganger(
                gruppetilganger = tilgangsgrupper.tilUuider(grupper).toList(),
                kode7Saksbehandlergruppe = tilgangsgrupper.gruppeId(Gruppe.KODE7),
                beslutterSaksbehandlergruppe = tilgangsgrupper.gruppeId(Gruppe.BESLUTTER),
                skjermedePersonerSaksbehandlergruppe = tilgangsgrupper.gruppeId(Gruppe.SKJERMEDE),
            ),
            fødselsnummer = fødselsnummer
        )

    private fun harTilgangTilOppgaveMedEgenskaperGittEksplisitteGrupper(
        egenskaper: Set<Egenskap>,
        saksbehandler: Saksbehandler = lagSaksbehandler(),
        grupper: Set<Gruppe>
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
        grupper: Set<Gruppe>
    ): Boolean {
        saksbehandlerGruppeOppslagMap[saksbehandler.id().value] = grupper
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
    private val saksbehandlerGruppeOppslagMap = mutableMapOf<UUID, Set<Gruppe>>()

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
                tilgangsgrupper.tilUuider(saksbehandlerGruppeOppslagMap[oid].orEmpty())

            override suspend fun hentTilgangsgrupper(oid: UUID) =
                saksbehandlerGruppeOppslagMap[oid].orEmpty()
        }
    )
}

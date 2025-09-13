package no.nav.helse.spesialist.application.tilgangskontroll

import no.nav.helse.db.api.EgenAnsattApiDao
import no.nav.helse.db.api.PartialPersonApiDao
import no.nav.helse.spesialist.api.person.Adressebeskyttelse
import no.nav.helse.spesialist.domain.testfixtures.lagFødselsnummer
import no.nav.helse.spesialist.domain.tilgangskontroll.Tilgangsgruppe
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
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
                tilgangsgrupper = Tilgangsgruppe.entries.toSet()
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
                tilgangsgrupper = grupper
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
                tilgangsgrupper = grupper
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
                (Tilgangsgruppe.entries - Tilgangsgruppe.KODE7).toSet()
            ),
            Arguments.of(
                true,
                Adressebeskyttelse.Ugradert,
                emptySet<Tilgangsgruppe>()
            ),
            Arguments.of(
                true,
                Adressebeskyttelse.Ugradert,
                (Tilgangsgruppe.entries - Tilgangsgruppe.SKJERMEDE).toSet()
            ),
        )
    }

    private fun harTilgangTilPersonGittGrupper(fødselsnummer: String, tilgangsgrupper: Set<Tilgangsgruppe>) =
        tilgangskontroll.harTilgangTilPerson(
            tilgangsgrupper = tilgangsgrupper,
            fødselsnummer = fødselsnummer
        )

    private val personErEgenAnsattMap = mutableMapOf<String, Boolean?>()
    private val personAdressebeskyttelseMap = mutableMapOf<String, Adressebeskyttelse?>()

    private val tilgangskontroll = NyTilgangskontroll(
        egenAnsattApiDao = object : EgenAnsattApiDao {
            override fun erEgenAnsatt(fødselsnummer: String) =
                personErEgenAnsattMap[fødselsnummer]
        },
        personApiDao = object : PartialPersonApiDao {
            override fun hentAdressebeskyttelse(fødselsnummer: String) =
                personAdressebeskyttelseMap[fødselsnummer]
        }
    )
}

package no.nav.helse.spesialist.domain

import no.nav.helse.spesialist.domain.testfixtures.testdata.lagPerson
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagPersoninfo
import no.nav.helse.spesialist.domain.tilgangskontroll.Brukerrolle
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PersonKanSeesAvSaksbehandlerMedGrupperTest {
    @ParameterizedTest(name = "egenAnsatt={0}, adressebeskyttelse={1}")
    @MethodSource("personKombinasjonerIngenHarTilgangTil")
    fun `saksbehandler har ikke tilgang til person selv med alle tilganger`(
        egenAnsatt: Boolean?,
        adressebeskyttelse: Personinfo.Adressebeskyttelse?,
    ) {
        val person =
            lagPerson(
                erEgenAnsatt = egenAnsatt,
                info = adressebeskyttelse?.let { lagPersoninfo(adressebeskyttelse = it) },
            )

        assertFalse(person.kanSeesAvSaksbehandlerMedGrupper(Brukerrolle.entries.toSet()))
    }

    @ParameterizedTest(name = "egenAnsatt={0}, adressebeskyttelse={1}, roller={2}")
    @MethodSource("personKombinasjonerSomGirTilgang")
    fun `saksbehandler med visse grupper har tilgang til visse personer`(
        egenAnsatt: Boolean,
        adressebeskyttelse: Personinfo.Adressebeskyttelse,
        roller: Set<Brukerrolle>,
    ) {
        val person =
            lagPerson(
                erEgenAnsatt = egenAnsatt,
                info = lagPersoninfo(adressebeskyttelse = adressebeskyttelse),
            )

        assertTrue(person.kanSeesAvSaksbehandlerMedGrupper(roller))
    }

    @ParameterizedTest(name = "egenAnsatt={0}, adressebeskyttelse={1}, roller={2}")
    @MethodSource("personKombinasjonerSomIkkeGirTilgang")
    fun `saksbehandler med visse grupper har ikke tilgang til visse personer`(
        egenAnsatt: Boolean,
        adressebeskyttelse: Personinfo.Adressebeskyttelse,
        roller: Set<Brukerrolle>,
    ) {
        val person =
            lagPerson(
                erEgenAnsatt = egenAnsatt,
                info = lagPersoninfo(adressebeskyttelse = adressebeskyttelse),
            )

        assertFalse(person.kanSeesAvSaksbehandlerMedGrupper(roller))
    }

    companion object {
        @JvmStatic
        private fun personKombinasjonerIngenHarTilgangTil(): Stream<Arguments> =
            (Personinfo.Adressebeskyttelse.entries + null)
                .flatMap { adressebeskyttelse ->
                    // Ingen har tilgang til personer som har erEgenAnsatt == null
                    listOf(Arguments.of(null, adressebeskyttelse)) +
                        if (adressebeskyttelse !in
                            setOf(
                                Personinfo.Adressebeskyttelse.Ugradert,
                                Personinfo.Adressebeskyttelse.Fortrolig,
                            )
                        ) {
                            // Ingen har tilgang til noe annet enn (kjent) Adressebeskyttelse Ugradert eller Fortrolig
                            listOf(
                                Arguments.of(false, adressebeskyttelse),
                                Arguments.of(true, adressebeskyttelse),
                            )
                        } else {
                            emptyList()
                        }
                }.stream()

        @JvmStatic
        private fun personKombinasjonerSomGirTilgang(): Stream<Arguments> =
            Stream.of(
                Arguments.of(false, Personinfo.Adressebeskyttelse.Ugradert, emptySet<Brukerrolle>()),
                Arguments.of(false, Personinfo.Adressebeskyttelse.Fortrolig, setOf(Brukerrolle.Kode7)),
                Arguments.of(true, Personinfo.Adressebeskyttelse.Ugradert, setOf(Brukerrolle.EgenAnsatt)),
            )

        @JvmStatic
        private fun personKombinasjonerSomIkkeGirTilgang(): Stream<Arguments> =
            Stream.of(
                Arguments.of(
                    false,
                    Personinfo.Adressebeskyttelse.Fortrolig,
                    emptySet<Brukerrolle>(),
                ),
                Arguments.of(
                    false,
                    Personinfo.Adressebeskyttelse.Fortrolig,
                    (Brukerrolle.entries - Brukerrolle.Kode7).toSet(),
                ),
                Arguments.of(
                    true,
                    Personinfo.Adressebeskyttelse.Ugradert,
                    emptySet<Brukerrolle>(),
                ),
                Arguments.of(
                    true,
                    Personinfo.Adressebeskyttelse.Ugradert,
                    (Brukerrolle.entries - Brukerrolle.EgenAnsatt).toSet(),
                ),
            )
    }
}

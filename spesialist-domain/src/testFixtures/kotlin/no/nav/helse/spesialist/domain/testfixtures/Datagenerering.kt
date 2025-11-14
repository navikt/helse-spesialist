package no.nav.helse.spesialist.domain.testfixtures

import no.nav.helse.Varselvurdering
import no.nav.helse.modell.oppgave.Oppgave
import no.nav.helse.spesialist.domain.Behandling
import no.nav.helse.spesialist.domain.BehandlingUnikId
import no.nav.helse.spesialist.domain.Saksbehandler
import no.nav.helse.spesialist.domain.SaksbehandlerOid
import no.nav.helse.spesialist.domain.SpleisBehandlingId
import no.nav.helse.spesialist.domain.Varsel
import no.nav.helse.spesialist.domain.VarselId
import no.nav.helse.spesialist.domain.Varseldefinisjon
import no.nav.helse.spesialist.domain.VarseldefinisjonId
import no.nav.helse.spesialist.domain.Vedtaksperiode
import no.nav.helse.spesialist.domain.VedtaksperiodeId
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import kotlin.random.Random.Default.nextInt
import kotlin.random.Random.Default.nextLong

fun lagFødselsnummer() = nextLong(from = 10000_00000, until = 319999_99999).toString().padStart(11, '0')

fun lagDnummer() = nextLong(from = 40000_00000, until = 719999_99999).toString().padStart(11, '0')

fun lagAktørId() = nextLong(from = 1_000_000_000_000, until = 1_000_099_999_999).toString()

fun lagOrganisasjonsnummer() = nextLong(from = 800_000_000, until = 999_999_999).toString()

private val fornavnListe = listOf(
    "Måteholden",
    "Dypsindig",
    "Ultrafiolett",
    "Urettferdig",
    "Berikende",
    "Upresis",
    "Stridlynt",
    "Rund",
    "Internasjonal"
)
private val mellomnavnListe = listOf(
    "Lysende",
    "Spennende",
    "Tidløs",
    "Hjertelig",
    "Storslått",
    "Sjarmerende",
    "Uforutsigbar",
    "Behagelig",
    "Robust",
    "Sofistikert",
)

private val etternavnListe =
    listOf("Diode", "Flom", "Damesykkel", "Undulat", "Bakgrunn", "Genser", "Fornøyelse", "Campingvogn", "Bakkeklaring")

private val organisasjonsnavnDel1 = listOf("NEPE", "KLOVNE", "BOBLEBAD-", "DUSTE", "SKIHOPP", "SMÅBARN", "SPANIA")
private val organisasjonsnavnDel2 = listOf("AVDELINGEN", "SENTERET", "FORUM", "KLUBBEN", "SNEKKERIET")

fun lagOrganisasjonsnavn() = organisasjonsnavnDel1.random() + organisasjonsnavnDel2.random()

fun lagSaksbehandlerOid() = SaksbehandlerOid(UUID.randomUUID())
fun lagSaksbehandlerident() = ('A'..'Z').random() + "${nextInt(from = 100_000, until = 999_999)}"
fun lagSaksbehandlernavn() = listOfNotNull(lagFornavn(), lagMellomnavnOrNull(), lagEtternavn()).joinToString(separator = " ")
fun lagEpostadresseFraFulltNavn(fulltNavn: String) = fulltNavn.split(" ").joinToString(".").lowercase() + "@nav.no"
fun lagTilfeldigSaksbehandlerepost() = lagEpostadresseFraFulltNavn(lagSaksbehandlernavn())

fun fødselsdato(): LocalDate {
    val end = LocalDate.now().minusYears(18)
    val start = end.minusYears(100)
    val randomDayInEpoch = nextLong(start.toEpochDay(), end.toEpochDay())
    return LocalDate.ofEpochDay(randomDayInEpoch)
}

fun lagFornavn() = fornavnListe.random()

fun lagMellomnavnOrNull() = (mellomnavnListe  + List(10) { null }).shuffled().random()
fun lagMellomnavn() = mellomnavnListe.random()

fun lagEtternavn() = etternavnListe.random()

fun lagEnOppgave(behandlingId: UUID): Oppgave = Oppgave.ny(
    id = nextLong(),
    førsteOpprettet = LocalDateTime.now(),
    vedtaksperiodeId = UUID.randomUUID(),
    behandlingId = behandlingId,
    utbetalingId = UUID.randomUUID(),
    hendelseId = UUID.randomUUID(),
    kanAvvises = true,
    egenskaper = emptySet(),
)

fun lagEnSaksbehandler(): Saksbehandler {
    val navn = lagSaksbehandlernavn()
    return Saksbehandler(
        id = SaksbehandlerOid(UUID.randomUUID()),
        navn = navn,
        epost = lagEpostadresseFraFulltNavn(navn),
        ident = lagSaksbehandlerident()
    )
}

fun lagEnBehandling(
    id: UUID = UUID.randomUUID(),
    spleisBehandlingId: UUID = UUID.randomUUID(),
    vedtaksperiodeId: VedtaksperiodeId,
    tags: Set<String> = setOf("Innvilget"),
): Behandling = Behandling.fraLagring(
    id = BehandlingUnikId(id),
    spleisBehandlingId = SpleisBehandlingId(spleisBehandlingId),
    tags = tags,
    søknadIder = emptySet(),
    fom = 1.jan(2018),
    tom = 31.jan(2018),
    skjæringstidspunkt = 1.jan(2018),
    vedtaksperiodeId = vedtaksperiodeId,
)

fun lagEnVedtaksperiode(
    vedtaksperiodeId: UUID = UUID.randomUUID(),
    fødselsnummer: String = lagFødselsnummer(),
    organisasjonsnummer: String = lagOrganisasjonsnummer(),
): Vedtaksperiode = Vedtaksperiode(
    id = VedtaksperiodeId(vedtaksperiodeId),
    fødselsnummer = fødselsnummer,
    organisasjonsnummer = organisasjonsnummer
)

fun lagEtVarsel(
    id: UUID = UUID.randomUUID(),
    behandlingUnikId: BehandlingUnikId,
    spleisBehandlingId: SpleisBehandlingId?,
    status: Varsel.Status = Varsel.Status.AKTIV,
    kode: String = "RV_IV_1",
    opprettet: LocalDateTime = LocalDateTime.now(),
    vurdering: Varselvurdering? = null,
): Varsel = Varsel.fraLagring(
    id = VarselId(id),
    spleisBehandlingId = spleisBehandlingId,
    behandlingUnikId = behandlingUnikId,
    status = status,
    kode = kode,
    opprettetTidspunkt = opprettet,
    vurdering = vurdering
)

fun lagEnVarseldefinisjon(
    id: UUID = UUID.randomUUID(),
    kode: String = "RV_IV_1",
    tittel: String = "Dette er en tittel",
    forklaring: String? = "Dette er en forklaring",
    handling: String? = "Dette er en handling",
): Varseldefinisjon = Varseldefinisjon.fraLagring(
    id = VarseldefinisjonId(id),
    kode = kode,
    tittel = tittel,
    forklaring = forklaring,
    handling = handling
)

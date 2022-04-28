package no.nav.helse.mediator.api

import UtbetalingshistorikkElementApiDto
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.convertValue
import java.util.UUID
import no.nav.helse.arbeidsgiver.ArbeidsgiverApiDao
import no.nav.helse.arbeidsgiver.ArbeidsgiverApiDto
import no.nav.helse.arbeidsgiver.ArbeidsgiverDto
import no.nav.helse.measureAsHistogram
import no.nav.helse.mediator.api.PersonMediator.SnapshotResponse.SnapshotTilstand.FINNES_IKKE
import no.nav.helse.mediator.api.PersonMediator.SnapshotResponse.SnapshotTilstand.INGEN_TILGANG
import no.nav.helse.mediator.api.PersonMediator.SnapshotResponse.SnapshotTilstand.OK
import no.nav.helse.modell.SpeilSnapshotDao
import no.nav.helse.modell.egenansatt.EgenAnsattDao
import no.nav.helse.modell.utbetaling.UtbetalingDao
import no.nav.helse.modell.vedtak.snapshot.SpeilSnapshotRestClient
import no.nav.helse.objectMapper
import no.nav.helse.oppgave.OppgaveDao
import no.nav.helse.overstyring.OverstyringApiArbeidsforholdDto
import no.nav.helse.overstyring.OverstyringApiDagerDto
import no.nav.helse.overstyring.OverstyringApiDao
import no.nav.helse.overstyring.OverstyringApiInntektDto
import no.nav.helse.overstyring.OverstyrtArbeidsforholdApiDto
import no.nav.helse.overstyring.OverstyrtDagApiDto
import no.nav.helse.overstyring.OverstyrtInntektApiDto
import no.nav.helse.person.Adressebeskyttelse
import no.nav.helse.person.PersonApiDao
import no.nav.helse.person.PersonForSpeilDto
import no.nav.helse.person.PersonMetadataApiDto
import no.nav.helse.person.PersonsnapshotDao
import no.nav.helse.person.SnapshotDto
import no.nav.helse.risikovurdering.RisikovurderingApiDao
import no.nav.helse.tildeling.TildelingDao
import no.nav.helse.utbetaling.AnnullertAvSaksbehandlerApiDto
import no.nav.helse.utbetaling.OppdragApiDto
import no.nav.helse.utbetaling.UtbetalingApiDto
import no.nav.helse.utbetaling.UtbetalingslinjeApiDto
import no.nav.helse.vedtaksperiode.VarselDao

internal class PersonMediator(
    private val personsnapshotDao: PersonsnapshotDao,
    private val varselDao: VarselDao,
    private val personDao: PersonApiDao,
    private val egenAnsattDao: EgenAnsattDao,
    private val arbeidsgiverDao: ArbeidsgiverApiDao,
    private val overstyringDao: OverstyringApiDao,
    private val oppgaveDao: OppgaveDao,
    private val tildelingDao: TildelingDao,
    private val risikovurderingApiDao: RisikovurderingApiDao,
    private val utbetalingDao: UtbetalingDao,
    private val speilSnapshotDao: SpeilSnapshotDao,
    private val speilSnapshotRestClient: SpeilSnapshotRestClient
) {

    fun byggSpeilSnapshotForFnr(fødselsnummer: String, kanSeKode7: Boolean, kanSeSkjermede: Boolean) =
        measureAsHistogram("byggSpeilSnapshotForFnr") {
            byggSnapshot(fødselsnummer, kanSeKode7, kanSeSkjermede)
        }

    fun byggSpeilSnapshotForAktørId(aktørId: String, kanSeKode7: Boolean, kanSeSkjermede: Boolean) =
        measureAsHistogram("byggSpeilSnapshotForAktørId") {
            personsnapshotDao.finnFnrByAktørId(aktørId)?.let { byggSnapshot(it, kanSeKode7, kanSeSkjermede) }
                ?: SnapshotResponse(snapshot = null, tilstand = FINNES_IKKE)
        }

    private fun byggSnapshot(fødselsnummer: String, kanSeKode7: Boolean, kanSeSkjermede: Boolean): SnapshotResponse {
        if (!personDao.finnesPersonMedFødselsnummer(fødselsnummer)) {
            return SnapshotResponse(snapshot = null, tilstand = FINNES_IKKE)
        }

        val erFortrolig = personDao.personHarAdressebeskyttelse(fødselsnummer, Adressebeskyttelse.Fortrolig)
        val erUgradert = personDao.personHarAdressebeskyttelse(fødselsnummer, Adressebeskyttelse.Ugradert)
        val erUkjentEllerStrengtFortrolig = !erFortrolig && !erUgradert
        val erSkjermet = egenAnsattDao.erEgenAnsatt(fødselsnummer)

        return when {
            erSkjermet == null && !kanSeSkjermede -> {
                SnapshotResponse(snapshot = null, tilstand = FINNES_IKKE)
            }
            erSkjermet == true && !kanSeSkjermede -> {
                SnapshotResponse(snapshot = null, tilstand = INGEN_TILGANG)
            }
            erFortrolig && !kanSeKode7 -> {
                SnapshotResponse(snapshot = null, tilstand = INGEN_TILGANG)
            }
            erUkjentEllerStrengtFortrolig -> {
                SnapshotResponse(snapshot = null, tilstand = INGEN_TILGANG)
            }
            else -> {
                if (speilSnapshotDao.utdatert(fødselsnummer)) {
                    val nyttSnapshot = speilSnapshotRestClient.hentSpeilSnapshot(fødselsnummer)
                    speilSnapshotDao.lagre(fødselsnummer, nyttSnapshot)
                }
                val snapshot = personsnapshotDao.finnPersonByFnr(fødselsnummer)?.let(::byggSpeilSnapshot)
                    ?: return SnapshotResponse(null, FINNES_IKKE)
                SnapshotResponse(snapshot, OK)
            }
        }
    }

    private fun byggSpeilSnapshot(personsnapshot: Pair<PersonMetadataApiDto, SnapshotDto>) =
        measureAsHistogram("byggSpeilSnapshot") {
            val (personMetadata, speilSnapshot) = personsnapshot
            val infotrygdutbetalinger = measureAsHistogram("byggSpeilSnapshot_findInfotrygdutbetalinger") {
                personDao.finnInfotrygdutbetalinger(personMetadata.fødselsnummer)?.let { objectMapper.readTree(it) }
            }
            val utbetalinger = measureAsHistogram("byggSpeilSnapshot_findUtbetaling") {
                utbetalingDao.findUtbetalinger(personMetadata.fødselsnummer).map { utbetaling ->
                    UtbetalingApiDto(
                        type = utbetaling.type,
                        status = utbetaling.status.toString(),
                        arbeidsgiveroppdrag = utbetaling.arbeidsgiveroppdrag?.let {
                            OppdragApiDto(
                                mottaker = it.mottaker,
                                fagsystemId = it.fagsystemId,
                                utbetalingslinjer = it.linjer.map { linje ->
                                    UtbetalingslinjeApiDto(
                                        fom = linje.fom,
                                        tom = linje.tom,
                                        totalbeløp = linje.totalbeløp,
                                    )
                                }
                            )
                        },
                        personoppdrag = utbetaling.personoppdrag?.let {
                            OppdragApiDto(
                                mottaker = it.mottaker,
                                fagsystemId = it.fagsystemId,
                                utbetalingslinjer = it.linjer.map { linje ->
                                    UtbetalingslinjeApiDto(
                                        fom = linje.fom,
                                        tom = linje.tom,
                                        totalbeløp = linje.totalbeløp,
                                    )
                                }
                            )
                        },
                        annullertAvSaksbehandler = utbetaling.annullertAvSaksbehandler?.let {
                            AnnullertAvSaksbehandlerApiDto(
                                annullertTidspunkt = it.annullertTidspunkt,
                                saksbehandlerNavn = it.saksbehandlerNavn
                            )
                        },
                        totalbeløp = utbetaling.totalbeløp
                    )
                }
            }

            val arbeidsgivere = speilSnapshot.arbeidsgivere.map {
                val navn = arbeidsgiverDao.finnNavn(it.organisasjonsnummer)
                val bransjer = arbeidsgiverDao.finnBransjer(it.organisasjonsnummer)
                val overstyringer =
                    overstyringDao.finnOverstyringerAvTidslinjer(personMetadata.fødselsnummer, it.organisasjonsnummer)
                        .map { overstyring ->
                            OverstyringApiDagerDto(
                                hendelseId = overstyring.hendelseId,
                                begrunnelse = overstyring.begrunnelse,
                                timestamp = overstyring.timestamp,
                                saksbehandlerNavn = overstyring.saksbehandlerNavn,
                                saksbehandlerIdent = overstyring.saksbehandlerIdent,
                                overstyrteDager = overstyring.overstyrteDager.map { dag ->
                                    OverstyrtDagApiDto(
                                        dato = dag.dato,
                                        dagtype = dag.type,
                                        grad = dag.grad
                                    )
                                }
                            )
                        } + overstyringDao.finnOverstyringerAvInntekt(
                        personMetadata.fødselsnummer,
                        it.organisasjonsnummer
                    ).map { overstyring ->
                        OverstyringApiInntektDto(
                            hendelseId = overstyring.hendelseId,
                            begrunnelse = overstyring.begrunnelse,
                            timestamp = overstyring.timestamp,
                            saksbehandlerNavn = overstyring.saksbehandlerNavn,
                            saksbehandlerIdent = overstyring.saksbehandlerIdent,
                            overstyrtInntekt = OverstyrtInntektApiDto(
                                forklaring = overstyring.forklaring,
                                månedligInntekt = overstyring.månedligInntekt,
                                skjæringstidspunkt = overstyring.skjæringstidspunkt
                            )
                        )
                    } + overstyringDao.finnOverstyringerAvArbeidsforhold(
                        personMetadata.fødselsnummer,
                        it.organisasjonsnummer
                    ).map { overstyring ->
                        OverstyringApiArbeidsforholdDto(
                            hendelseId = overstyring.hendelseId,
                            begrunnelse = overstyring.begrunnelse,
                            timestamp = overstyring.timestamp,
                            saksbehandlerNavn = overstyring.saksbehandlerNavn,
                            saksbehandlerIdent = overstyring.saksbehandlerIdent,
                            overstyrtArbeidsforhold = OverstyrtArbeidsforholdApiDto(
                                forklaring = overstyring.forklaring,
                                deaktivert = overstyring.deaktivert,
                                skjæringstidspunkt = overstyring.skjæringstidspunkt
                            )
                        )
                    }
                ArbeidsgiverApiDto(
                    organisasjonsnummer = it.organisasjonsnummer,
                    navn = navn ?: "Ikke tilgjengelig",
                    id = it.id,
                    overstyringer = overstyringer,
                    vedtaksperioder = it.vedtaksperioder,
                    bransjer = bransjer,
                    utbetalingshistorikk = mapUtbetalingshistorikk(it),
                    generasjoner = it.generasjoner,
                    ghostPerioder = it.ghostPerioder
                )
            }
            measureAsHistogram("byggSpeilSnapshot_behovForVedtaksperiode_akkumulert") {
                speilSnapshot.arbeidsgivere.forEach { arbeidsgiver ->
                    arbeidsgiver.vedtaksperioder.forEach { vedtaksperiode ->
                        val vedtaksperiodeId = UUID.fromString(vedtaksperiode["id"].asText())
                        val oppgaveId = oppgaveDao.finnOppgaveId(vedtaksperiodeId)
                        val risikovurdering = risikovurderingApiDao.finnRisikovurdering(vedtaksperiodeId)
                        val varsler = varselDao.finnAktiveVarsler(vedtaksperiodeId)

                        vedtaksperiode as ObjectNode
                        vedtaksperiode.put("oppgavereferanse", oppgaveId?.toString())
                        risikovurdering?.let {
                            vedtaksperiode.set<ObjectNode>(
                                "risikovurdering",
                                objectMapper.convertValue(it, ObjectNode::class.java)
                            )
                        }
                        vedtaksperiode.set<ArrayNode>("varsler", objectMapper.convertValue<ArrayNode>(varsler))
                    }
                }
            }

            val enhet = measureAsHistogram("byggSpeilSnapshot_findEnhet") {
                personDao.finnEnhet(personMetadata.fødselsnummer)
            }

            val tildeling = tildelingDao.tildelingForPerson(personMetadata.fødselsnummer)

            val arbeidsforhold = arbeidsgivere
                .map { arbeidsgiverDao.finnArbeidsforhold(personMetadata.fødselsnummer, it.organisasjonsnummer) }
                .flatten()

            PersonForSpeilDto(
                aktørId = speilSnapshot.aktørId,
                fødselsnummer = speilSnapshot.fødselsnummer,
                dødsdato = speilSnapshot.dødsdato,
                personinfo = personMetadata.personinfo,
                arbeidsgivere = arbeidsgivere,
                infotrygdutbetalinger = infotrygdutbetalinger,
                enhet = enhet,
                utbetalinger = utbetalinger,
                arbeidsforhold = arbeidsforhold,
                inntektsgrunnlag = speilSnapshot.inntektsgrunnlag,
                tildeling = tildeling,
                vilkårsgrunnlagHistorikk = speilSnapshot.vilkårsgrunnlagHistorikk
            )
        }

    private fun mapUtbetalingshistorikk(it: ArbeidsgiverDto) =
        UtbetalingshistorikkElementApiDto.toSpeilMap(it.utbetalingshistorikk)

    fun erAktivOppgave(oppgaveId: Long) = oppgaveDao.venterPåSaksbehandler(oppgaveId)

    internal class SnapshotResponse(
        val snapshot: PersonForSpeilDto?,
        val tilstand: SnapshotTilstand
    ) {

        enum class SnapshotTilstand {
            FINNES_IKKE,
            INGEN_TILGANG,
            OK
        }
    }
}

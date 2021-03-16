package no.nav.helse.mediator.api

import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.measureAsHistogram
import no.nav.helse.mediator.FeatureToggle.REVURDERING_TOGGLE
import no.nav.helse.modell.OppgaveDao
import no.nav.helse.modell.SnapshotDao
import no.nav.helse.modell.VedtakDao
import no.nav.helse.modell.WarningDao
import no.nav.helse.modell.arbeidsforhold.ArbeidsforholdDao
import no.nav.helse.modell.arbeidsgiver.ArbeidsgiverDao
import no.nav.helse.modell.overstyring.OverstyringDao
import no.nav.helse.modell.person.PersonDao
import no.nav.helse.modell.risiko.RisikovurderingDao
import no.nav.helse.modell.tildeling.TildelingDao
import no.nav.helse.modell.utbetaling.UtbetalingDao
import no.nav.helse.modell.vedtak.Warning
import no.nav.helse.modell.vedtak.snapshot.ArbeidsgiverFraSpleisDto
import no.nav.helse.modell.vedtak.snapshot.PersonFraSpleisDto
import no.nav.helse.modell.vedtaksperiode.*
import no.nav.helse.objectMapper
import java.util.*

internal class VedtaksperiodeMediator(
    private val vedtakDao: VedtakDao,
    private val warningDao: WarningDao,
    private val personDao: PersonDao,
    private val arbeidsgiverDao: ArbeidsgiverDao,
    private val snapshotDao: SnapshotDao,
    private val overstyringDao: OverstyringDao,
    private val oppgaveDao: OppgaveDao,
    private val tildelingDao: TildelingDao,
    private val risikovurderingDao: RisikovurderingDao,
    private val utbetalingDao: UtbetalingDao,
    private val arbeidsforholdDao: ArbeidsforholdDao
) {
    fun byggSpeilSnapshotForFnr(fnr: String) =
        measureAsHistogram("byggSpeilSnapshotForFnr") {
            vedtakDao.findVedtakByFnr(fnr)?.let { byggSpeilSnapshot(it) }
        }

    fun byggSpeilSnapshotForAktørId(aktørId: String) =
        measureAsHistogram("byggSpeilSnapshotForAktørId") {
            vedtakDao.findVedtakByAktørId(aktørId)?.let { byggSpeilSnapshot(it) }
        }

    fun byggSpeilSnapshotForVedtaksperiodeId(vedtaksperiodeId: UUID) =
        measureAsHistogram("byggSpeilSnapshotForVedtaksperiodeId") {
            vedtakDao.findVedtakByVedtaksperiodeId(vedtaksperiodeId)?.let { byggSpeilSnapshot(it) }
        }

    private fun byggSpeilSnapshot(vedtak: VedtaksperiodeDto) =
        measureAsHistogram("byggSpeilSnapshot") {
            val infotrygdutbetalinger = measureAsHistogram("byggSpeilSnapshot_findInfotrygdutbetalinger") {
                personDao.findInfotrygdutbetalinger(vedtak.fødselsnummer)?.let { objectMapper.readTree(it) }
            }
            val speilSnapshot = measureAsHistogram("byggSpeilSnapshot_findSpeilSnapshot") {
                requireNotNull(snapshotDao.findSpeilSnapshot(vedtak.speilSnapshotRef)) { "Fant ikke speilSnapshot" }
                    .let { objectMapper.readValue<PersonFraSpleisDto>(it) }
            }
            val utbetalinger = measureAsHistogram("byggSpeilSnapshot_findUtbetaling") {
                utbetalingDao.findUtbetalinger(vedtak.fødselsnummer).map { utbetaling ->
                    UtbetalingForSpeilDto(
                        type = utbetaling.type,
                        status = utbetaling.status,
                        arbeidsgiverOppdrag = OppdragForSpeilDto(
                            organisasjonsnummer = utbetaling.arbeidsgiverOppdrag.organisasjonsnummer,
                            fagsystemId = utbetaling.arbeidsgiverOppdrag.fagsystemId,
                            utbetalingslinjer = utbetaling.arbeidsgiverOppdrag.linjer.map { linje ->
                                UtbetalingslinjeForSpeilDto(
                                    fom = linje.fom,
                                    tom = linje.tom
                                )
                            }
                        ),
                        annullertAvSaksbehandler = utbetaling.annullertAvSaksbehandler?.let {
                            AnnullertAvSaksbehandlerForSpeilDto(
                                annullertTidspunkt = it.annullertTidspunkt,
                                saksbehandlerNavn = it.saksbehandlerNavn
                            )
                        }
                    )
                }
            }

            val arbeidsgivere = speilSnapshot.arbeidsgivere.map {
                val arbeidsgiverDto = measureAsHistogram("byggSpeilSnapshot_findArbeidsgiver") {
                    arbeidsgiverDao.findArbeidsgiver(it.organisasjonsnummer)
                }
                val overstyringer = overstyringDao.finnOverstyring(vedtak.fødselsnummer, it.organisasjonsnummer)
                    .map { overstyring ->
                        OverstyringForSpeilDto(
                            hendelseId = overstyring.hendelseId,
                            begrunnelse = overstyring.begrunnelse,
                            timestamp = overstyring.timestamp,
                            saksbehandlerNavn = overstyring.saksbehandlerNavn,
                            overstyrteDager = overstyring.overstyrteDager.map { dag ->
                                OverstyringDagForSpeilDto(
                                    dato = dag.dato,
                                    dagtype = dag.type,
                                    grad = dag.grad
                                )
                            }
                        )
                    }
                ArbeidsgiverForSpeilDto(
                    organisasjonsnummer = it.organisasjonsnummer,
                    navn = arbeidsgiverDto?.navn ?: "Ikke tilgjengelig",
                    id = it.id,
                    overstyringer = overstyringer,
                    vedtaksperioder = it.vedtaksperioder,
                    bransjer = arbeidsgiverDto?.bransjer,
                    utbetalingshistorikk = if (REVURDERING_TOGGLE.enabled) mapUtbetalingshistorikk(it) else emptyList()
                )
            }
            measureAsHistogram("byggSpeilSnapshot_behovForVedtaksperiode_akkumulert") {
                speilSnapshot.arbeidsgivere.forEach { arbeidsgiver ->
                    arbeidsgiver.vedtaksperioder.forEach { vedtaksperiode ->
                        val vedtaksperiodeId = UUID.fromString(vedtaksperiode["id"].asText())
                        val oppgaveId = oppgaveDao.finnOppgaveId(vedtaksperiodeId)
                        val risikovurdering = risikovurderingDao.hentRisikovurdering(vedtaksperiodeId)
                        val varsler = Warning.meldinger(warningDao.finnWarnings(vedtaksperiodeId))

                        vedtaksperiode as ObjectNode
                        vedtaksperiode.put("oppgavereferanse", oppgaveId?.toString())
                        risikovurdering?.let {
                            vedtaksperiode.set<ObjectNode>(
                                "risikovurdering",
                                objectMapper.convertValue(it.speilDto(), ObjectNode::class.java)
                            )
                        }
                        vedtaksperiode.set<ArrayNode>("varsler", objectMapper.convertValue<ArrayNode>(varsler))
                    }
                }
            }

            val enhet = measureAsHistogram("byggSpeilSnapshot_findEnhet") {
                personDao.findEnhet(vedtak.fødselsnummer)
            }

            val tildeling = tildelingDao.tildelingForPerson(vedtak.fødselsnummer)

            val arbeidsforhold = arbeidsgivere
                .map { arbeidsgiver ->
                    arbeidsforholdDao
                        .findArbeidsforhold(vedtak.fødselsnummer, arbeidsgiver.organisasjonsnummer)
                        .map { arbeidsforhold ->
                            ArbeidsforholdForSpeilDto(
                                organisasjonsnummer = arbeidsgiver.organisasjonsnummer,
                                stillingstittel = arbeidsforhold.stillingstittel,
                                stillingsprosent = arbeidsforhold.stillingsprosent,
                                startdato = arbeidsforhold.startdato,
                                sluttdato = arbeidsforhold.sluttdato
                            )
                        }
                }
                .flatten()

            PersonForSpeilDto(
                aktørId = speilSnapshot.aktørId,
                fødselsnummer = speilSnapshot.fødselsnummer,
                dødsdato = speilSnapshot.dødsdato,
                personinfo = vedtak.personinfo,
                arbeidsgivere = arbeidsgivere,
                infotrygdutbetalinger = infotrygdutbetalinger,
                enhet = enhet,
                saksbehandlerepost = tildeling?.saksbehandlerepost,
                utbetalinger = utbetalinger,
                arbeidsforhold = arbeidsforhold,
                inntektsgrunnlag = speilSnapshot.inntektsgrunnlag,
                erPåVent = tildeling?.erPåVent ?: false
            )
        }

    private fun mapUtbetalingshistorikk(it: ArbeidsgiverFraSpleisDto) =
        it.utbetalingshistorikk?.let { utbetalingshistorikk ->
            UtbetalingshistorikkElementForSpeilDto.toSpeilMap(utbetalingshistorikk)
        } ?: emptyList()

    fun erAktivOppgave(oppgaveId: Long) = oppgaveDao.venterPåSaksbehandler(oppgaveId)
}

package no.nav.helse.spesialist.api.bootstrap

import no.nav.helse.spesialist.api.Avviksvurderinghenter
import no.nav.helse.spesialist.api.Dokumenthåndterer
import no.nav.helse.spesialist.api.Godkjenninghåndterer
import no.nav.helse.spesialist.api.Personhåndterer
import no.nav.helse.spesialist.api.Saksbehandlerhåndterer
import no.nav.helse.spesialist.api.StansAutomatiskBehandlinghåndterer
import no.nav.helse.spesialist.api.Totrinnsvurderinghåndterer
import no.nav.helse.spesialist.api.behandlingsstatistikk.IBehandlingsstatistikkService
import no.nav.helse.spesialist.api.oppgave.Oppgavehåndterer
import no.nav.helse.spesialist.api.snapshot.ISnapshotClient

data class ApiAvhengigheter(
    val saksbehandlerhåndtererProvider: () -> Saksbehandlerhåndterer,
    val oppgavehåndtererProvider: () -> Oppgavehåndterer,
    val totrinnsvurderinghåndterer: () -> Totrinnsvurderinghåndterer,
    val godkjenninghåndtererProvider: () -> Godkjenninghåndterer,
    val personhåndtererProvider: () -> Personhåndterer,
    val dokumenthåndtererProvider: () -> Dokumenthåndterer,
    val stansAutomatiskBehandlinghåndterer: () -> StansAutomatiskBehandlinghåndterer,
    val behandlingstatistikk: IBehandlingsstatistikkService,
    val snapshotClient: ISnapshotClient,
    val avviksvurderinghenter: Avviksvurderinghenter,
)

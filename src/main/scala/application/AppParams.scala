package es.eriktorr
package application

import com.monovore.decline.Opts

final case class AppParams(verbose: Boolean)

object AppParams:
  def opts: Opts[AppParams] = Opts
    .flag("verbose", short = "v", help = "Print extra metadata to the logs.")
    .orFalse
    .map(AppParams.apply)

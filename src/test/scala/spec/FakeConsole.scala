package es.eriktorr
package spec

import spec.FakeConsole.ConsoleState

import cats.Show
import cats.effect.std.Console
import cats.effect.{IO, Ref}
import cats.implicits.toShow

import java.nio.charset.Charset

final class FakeConsole(stateRef: Ref[IO, ConsoleState]) extends Console[IO]:
  override def readLineWithCharset(charset: Charset): IO[String] =
    IO.raiseError(IllegalArgumentException("not implemented"))

  override def print[A](a: A)(implicit S: Show[A]): IO[Unit] =
    IO.raiseError(IllegalArgumentException("not implemented"))

  override def println[A](a: A)(implicit S: Show[A]): IO[Unit] =
    stateRef.update(currentState => currentState.set(a.show :: currentState.lines))

  override def error[A](a: A)(implicit S: Show[A]): IO[Unit] =
    IO.raiseError(IllegalArgumentException("not implemented"))

  override def errorln[A](a: A)(implicit S: Show[A]): IO[Unit] =
    IO.raiseError(IllegalArgumentException("not implemented"))

object FakeConsole:
  final case class ConsoleState(lines: List[String]):
    def set(newLines: List[String]): ConsoleState = copy(newLines)

  object ConsoleState:
    val empty: ConsoleState = ConsoleState(List.empty)

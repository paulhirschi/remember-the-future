package com.shuttj.actors

import akka.actor.{Actor, Props}


class LoggingActor extends Actor {
  import com.shuttj.actors.LoggingActor._

  def receive: PartialFunction[Any, Unit] = {
    case LogFailedFuture(msg) => println(s"In LoggingActor.receive: [$msg]")
  }
}

object LoggingActor {
  def props: Props = Props[LoggingActor]

  case class LogFailedFuture(msg: String)
}
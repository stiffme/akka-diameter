package org.esipeng.akka.io.diameter

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.io.Tcp.CommandFailed
import akka.io.{IO, Tcp}

/**
  * Created by esipeng on 4/12/2016.
  */
class DiameterManager extends Actor with ActorLogging {

  def receive = {
    case Diameter.Connect(remote,settings) => {
      val listener = sender()
      val connection = context.actorOf(DiameterClientConnection.props(listener,settings))
      connection ! Tcp.Connect(remote)
    }
  }
}


private class DiameterClientConnection(listner:ActorRef,settings:DiameterSettings) extends Actor with ActorLogging  {
  implicit val system = this.context.system

  def receive = {
    case CommandFailed(t ) => {
      listner ! CommandFailed(t)
      listner ! ConnectionClosed()
      context stop self
    }

    case c @ Tcp.Connected(remote,local) => {
      //send CER
      val cer = DiameterBasicMessages.changeCapabilityRequest(settings)
      val tcpConnection = sender()
      tcpConnection ! Tcp.Register(self)
      //tcpConnection ! cer.encode()
    }
    case Tcp.Received(data) => {

    }
  }

}

private object DiameterClientConnection {
  def props(listner:ActorRef,settings:DiameterSettings) = Props(new DiameterClientConnection(listner,settings))
}

object DiameterBasicMessages  {
  def changeCapabilityRequest(settings:DiameterSettings):DiameterMessage = {
    null
  }
}
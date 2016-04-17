package org.esipeng.akka.io.diameter

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.io.Tcp.CommandFailed
import akka.io.{IO, Tcp}
import akka.util.ByteString

/**
  * Created by esipeng on 4/12/2016.
  */
class DiameterManager extends Actor with ActorLogging {

  def receive = {
    case Diameter.Connect(remote,settings) => {
      val listener = sender()
      val connection = context.actorOf(DiameterClientConnection.props(listener,settings))
      connection ! Diameter.Connect(remote,settings)
    }
  }
}


private class DiameterConnection(listener:ActorRef,settings:DiameterSettings) extends Actor with ActorLogging  {
  implicit val system = this.context.system
  val buffer = context.actorOf(DiameterMessageBufferDecoder.props(self),"Diameter-Buffer")
  def receive = {
    case Diameter.Connect(remote,settings) => {
      IO(Tcp) ! Tcp.Connect(remote)
    }
    case Tcp.CommandFailed( c:Tcp.Connect) => {
      log.warning("Can't establish connection to {}",c.remoteAddress)
      listener ! Diameter.Closed
      context stop self
    }
    case Tcp.Connected(remote, local) => {
      log.debug("Connection established, remote {}, local {}",remote,local)
      val connection  = sender()
      connection ! Tcp.Register(self)
    }
  }



  private def withDiameterDecodeAndEncode(receive:Receive):Receive = {
    //receive
    case data:ByteString => buffer ! data

  }

}

private object DiameterClientConnection {
  def props(listner:ActorRef,settings:DiameterSettings) = Props(new DiameterConnection(listner,settings))
}

object DiameterBasicMessages  {
  def changeCapabilityRequest(settings:DiameterSettings,hostIp:String):DiameterMessage = {
    val  header = DiameterHeader(true,true,false,false,Diameter.CapabilitiesExchangeRequest,0,0,0)
    null
  }
}
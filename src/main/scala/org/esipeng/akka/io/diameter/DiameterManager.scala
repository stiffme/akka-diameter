package org.esipeng.akka.io.diameter

import java.net.InetAddress

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.io.Tcp.CommandFailed
import akka.io.{IO, Tcp}
import akka.util.{ByteString, ByteStringBuilder}
import org.esipeng.akka.io.diameter.util.DiameterMessageBuilder

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
  var initialConnection = false
  def receive = {
    case Diameter.Connect(remote,settings) => {
      initialConnection = true
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
      if(initialConnection) { //send CER
        DiameterBasicMessages.changeCapabilityRequest(settings,local.getAddress)
      }
      connection ! Tcp.Register(self)
      context.become(capabilityExchangePhase(connection))
    }
  }

  def capabilityExchangePhase(connection:ActorRef):Receive = withDiameterDecodeAndEncode {
    case message:DiameterMessage => {
      if(message.header.commandCode != Diameter.CapabilitiesExchangeRequest)  {
        stopConnection(connection)
      } else  {
        //get result code
        val result = message.avps.filter( _.code == Diameter.ResultCode)
        if(result.size != 1)  {
          stopConnection(connection)
        } else  if(result(0).asInt == 2001) {
          log.debug("Capability exchange done from client")
        } else  {
          log.debug("capability exchange result is not successful {}",result(0).asInt)
          stopConnection(connection)
          context.become(monitoringPhase(connection))
        }
      }
    }
  }

  def monitoringPhase(connection:ActorRef):Receive = withDiameterDecodeAndEncode  {
    case message:DiameterMessage => {

    }

  }

  def stopConnection(connection:ActorRef) :Unit = {
    listener ! Diameter.Closed
    connection ! Tcp.Close
    context stop self
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
  def changeCapabilityRequest(settings:DiameterSettings,hostIp:InetAddress):DiameterMessage = {
    val cer = DiameterMessageBuilder.newRequest(Diameter.CapabilitiesExchangeRequest,Diameter.DiameterCommonMessages)
    //make Host-IP-Address
    cer.appendAvps(generateCapabilitiesExchangeContent(settings,hostIp))

    cer.makeMessage()

  }

  def changeCapabilityAnswer(settings:DiameterSettings,hostIp:InetAddress,request:DiameterMessage):DiameterMessage = {
    val cea = DiameterMessageBuilder.answerRequest(request)
    cea.appendAvps(generateCapabilitiesExchangeContent(settings,hostIp))
    cea.appendAvp(DiameterAvp(Diameter.ResultCode,false,true,false,None,2001))
    cea.makeMessage()
  }

  private def generateCapabilitiesExchangeContent(settings:DiameterSettings,hostIp:InetAddress):Seq[DiameterAvp] = {
    val builder = new ByteStringBuilder
    hostIp.getAddress.foreach( builder.putByte(_))
    val hostIpValue = builder.result

    val buffer = collection.mutable.ListBuffer.empty[DiameterAvp]
    buffer += DiameterAvp(Diameter.OriginHost,false,true,false,None,settings.originHost)
    buffer += DiameterAvp(Diameter.OriginRealm,false,true,false,None,settings.originRealm)
    buffer += DiameterAvp(Diameter.HostIPAddress,false,true,false,None,hostIpValue)
    buffer += DiameterAvp(Diameter.VendorId,false,true,false,None,settings.vendorId)
    buffer += DiameterAvp(Diameter.ProductName,false,true,false,None,"akka-diameter")
    settings.supportedVendorSpecificApps.foreach( t => {
      buffer += (DiameterAvp(Diameter.VendorSpecificApplicationId,false,true,false,None,Seq[DiameterAvp](
        DiameterAvp(Diameter.VendorId,false,true,false,None,t._1),
        DiameterAvp(Diameter.AuthApplicationId,false,true,false,None,t._2)
      )))
    })

    val vendorIds = settings.supportedVendorSpecificApps.map(_._1).toSet.foreach(t => {
      buffer += DiameterAvp(Diameter.SupportedVendorId,false,true,false,None,t)
    })
    buffer.toSeq

  }
}
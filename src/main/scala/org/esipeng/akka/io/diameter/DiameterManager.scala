package org.esipeng.akka.io.diameter

import java.net.InetAddress

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.io.{IO, Tcp}
import akka.util.{ByteString, ByteStringBuilder}
import org.esipeng.akka.io.diameter.util.DiameterMessageBuilder

/**
  * Created by esipeng on 4/12/2016.
  */
final case class SendDiameter(message:DiameterMessage)

class DiameterManager extends Actor with ActorLogging {

  def receive = {
    case Diameter.Connect(remote,settings) => {
      val listener = sender()
      val connection = context.actorOf(DiameterConnection.props(listener,settings))
      connection ! Diameter.Connect(remote,settings)
    }
  }
}


private class DiameterConnection(listener:ActorRef,settings:DiameterSettings) extends Actor with ActorLogging  {
  val internalH2H = Iterator from 10
  val internalE2E = Iterator from 10
  implicit val system = this.context.system
  val buffer = context.actorOf(DiameterMessageBufferDecoder.props(self),"Diameter-Buffer")
  val encoder = new DiameterMessageEncoder

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

      connection ! Tcp.Register(self)
      context.become(capabilityExchangePhase(connection))

      if(initialConnection) { //send CER
        self ! SendDiameter(DiameterBasicMessages.changeCapabilityRequest(settings,local.getAddress))
      }
    }
  }

  def capabilityExchangePhase(connection:ActorRef):Receive = withDiameterDecodeAndEncode(connection) orElse  {
    case message:DiameterMessage => {
      if(message.header.commandCode != Diameter.CapabilitiesExchangeRequest)  {
        stopConnection(connection)
      } else  {
        //get result code
        val result = message.avps.filter( _.code == Diameter.ResultCode)
        if(result.size != 1)  {
          stopConnection(connection)
        } else  if(result(0).asInt == 2001) {
          log.info("Capability exchange done")
          listener ! Diameter.Connected
          context.become(monitoringPhase(connection))
        } else  {
          log.warning("Capability exchange result is not successful {}",result(0).asInt)
          stopConnection(connection)
        }
      }
    }
  }

  def monitoringPhase(connection:ActorRef):Receive = withDiameterDecodeAndEncode(connection) orElse   {
    case dpr @ DiameterMessage(DiameterHeader(true,_,_,_,Diameter.DisconnectPeerRequest,_,_,_),_) => {
      log.info("DPR message received, answer success.")
      val dpa = DiameterMessageBuilder.answerRequest(dpr)
      dpa.appendAvp(DiameterAvp(Diameter.OriginHost,false,true,false,None,settings.originHost))
        .appendAvp(DiameterAvp(Diameter.OriginRealm,false,true,false,None,settings.originRealm))
        .appendAvp(DiameterAvp(Diameter.ResultCode,false,true,false,None,2001))
      val answer = dpa.makeMessage()
      self ! SendDiameter(answer)
    }
    case dwr @ DiameterMessage(DiameterHeader(true,_,_,_,Diameter.DeviceWatchdogRequest,_,_,_),_) => {
      log.debug("DWR message received, answer success.")
      val dwa = DiameterMessageBuilder.answerRequest(dwr)
      dwa.appendAvp(DiameterAvp(Diameter.OriginHost,false,true,false,None,settings.originHost))
        .appendAvp(DiameterAvp(Diameter.OriginRealm,false,true,false,None,settings.originRealm))
        .appendAvp(DiameterAvp(Diameter.ResultCode,false,true,false,None,2001))
      val answer = dwa.makeMessage()
      self ! SendDiameter(answer)
    }
    case message:DiameterMessage => {
      //send to listener
      listener ! message
    }

  }

  def stopConnection(connection:ActorRef) :Unit = {
    listener ! Diameter.Closed
    connection ! Tcp.Close
    context stop self
  }



  private def withDiameterDecodeAndEncode(connection: ActorRef):Receive = {
    //receive
    case data:ByteString => buffer ! data
    //send
    case SendDiameter(msg) => {
      if(msg.header.request == true)  {
        val encoded = encoder.encode(msg,internalH2H.next(),internalE2E.next())
        connection ! encoded
      } else  {
        val encoded = encoder.encode(msg)
        connection ! encoded
      }
    }

    case _:Tcp.ConnectionClosed => {
      stopConnection(connection)
    }
  }

}

private object DiameterConnection {
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
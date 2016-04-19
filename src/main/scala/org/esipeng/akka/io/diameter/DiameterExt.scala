package org.esipeng.akka.io.diameter

import java.net.InetSocketAddress

import akka.actor.{Actor, ActorLogging, ActorRef, ExtendedActorSystem, ExtensionKey, Props}
import akka.io.{IO, Tcp}

import scala.concurrent.duration.Duration

/**
  * Created by esipeng on 4/12/2016.
  */

class DiameterExt(system:ExtendedActorSystem) extends IO.Extension{
  val manager = system.actorOf(Props[DiameterManager],"IO-DIAMETER")
}

case class DiameterSettings(originHost:String,originRealm:String,vendorId:Int,supportedVendorSpecificApps:Set[(Int,Int)])

object Diameter extends ExtensionKey[DiameterExt] {

  //COMMANDS
  case class Connect(
                    remoteAddress:InetSocketAddress, //remote address
                    settings:DiameterSettings
                    )


  case class Bind(endpoint:InetSocketAddress,
                  settings:DiameterSettings)
  object Bind {
    def apply(settings:DiameterSettings,port:Int = 3868):Bind = Bind.apply(new InetSocketAddress(port),settings)
    def apply(settings:DiameterSettings,interface:String,port:Int):Bind = Bind.apply(new InetSocketAddress(interface,port),settings)
  }

  final case class Register(actorRef:ActorRef)

  case class Unbind(timeout:Duration)
  object Unbind extends Unbind(Duration.Zero)

  //EVENTS
  final case object Bound
  final case object Unbound
  final case object Connected
  final case object Closed

  //diameter commands
  final val CapabilitiesExchangeRequest = 257
  final val DisconnectPeerRequest = 282
  final val DeviceWatchdogRequest = 280

  //diameter avp code
  final val VendorId = 266
  final val HostIPAddress = 257
  final val SupportedVendorId = 265
  final val ProductName = 269
  final val FirmwareRevision = 267
  final val DestinationHost = 293
  final val DestinationRealm = 283
  final val DisconnectCause = 273
  final val ExperimentalResult = 297
  final val ExperimentalResultCode=298
  final val OriginHost = 264
  final val OriginRealm = 296
  final val ResultCode = 268
  final val SessionId=263
  final val VendorSpecificApplicationId = 260
  final val AuthApplicationId = 258

  //Appid
  final val DiameterCommonMessages = 0

}




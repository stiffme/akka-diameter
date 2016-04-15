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

case class DiameterSettings(originHost:String,originRealm:String,supportedVendorSpecificApps:Set[(Int,Int)])

object Diameter extends ExtensionKey[DiameterExt] {

  //COMMANDS
  case class Connect(
                    remoteAddress:InetSocketAddress, //remote address
                    settings:DiameterSettings
                    ) extends Tcp.Command

  object Connect  {
    def apply(host:String,settings:DiameterSettings,port:Int=3868):Connect = Connect.apply(new InetSocketAddress(host,port),settings)
  }

  case class Bind(listner:ActorRef,
                  endpoint:InetSocketAddress,
                  settings:DiameterSettings) extends Tcp.Command
  object Bind {
    def apply(listener:ActorRef,settings:DiameterSettings,port:Int = 3868):Bind = Bind.apply(listener,new InetSocketAddress(port),settings)
    def apply(listener:ActorRef,settings:DiameterSettings,interface:String,port:Int):Bind = Bind.apply(listener,new InetSocketAddress(interface,port),settings)
  }

  case class Unbind(timeout:Duration) extends Tcp.Command
  object Unbind extends Unbind(Duration.Zero)

  //EVENTS
  type Event = Tcp.Event
  type Connected = Tcp.Connected; val Connected = Tcp.Connected
  type Bound = Tcp.Bound; val Bound = Tcp.Bound
  val UnBound = Tcp.Unbound
  val Close
}




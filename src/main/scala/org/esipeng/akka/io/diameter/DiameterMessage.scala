package org.esipeng.akka.io.diameter

import akka.actor.{Actor, ActorLogging, ActorRef}
import akka.util.ByteString

/**
  * Created by esipeng on 4/12/2016.
  */
case class DiameterHeader(
                           request:Boolean,
                           proxyable:Boolean,
                           error:Boolean,
                           retransmission:Boolean,
                           commandCode:Int,
                           applicationId:Int)

case class DiameterAvp(code:Int,vendorSpecific:Boolean,mandatory:Boolean,proted:Boolean,vendorId:Option[Int],payload:ByteString)

case class DiameterMessage(header:DiameterHeader,avps:Seq[DiameterAvp]) {
  def encode():ByteString = null
}

class DiameterMessageBuffer(callback:ActorRef) extends Actor with ActorLogging {
  var buffer = ByteString.empty
  var currentLength = 0

  def receive = {
    case fragment @ ByteString => {
      if(currentLength == 0)
        peakMessageLength()

    }
  }

  private def peakMessageLength(): Unit = {
    val versionAndLength = buffer.slice(0,4).asByteBuffer.getInt
    val version = (versionAndLength & 0xff000000) >> 24
    val diameterLength = versionAndLength & 0x00ffffff
    if(version != 1)  {
      log.error("Diameter version is not {}, not expected 1",version)

    } else  {
      currentLength = diameterLength
    }

  }
  def decode(raw:ByteString):DiameterMessage = {
    null

  }

  def encode(message:DiameterMessage):ByteString = {
    null
  }
}



package org.esipeng.akka.io.diameter

import java.nio.{ByteBuffer, ByteOrder}

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.util.ByteString

/**
  * Created by stiff on 2016/4/15.
  */

class DiameterMessageBuffer(callback:ActorRef) extends Actor with ActorLogging {
  var buffer = ByteString.empty
  var currentLength = 0
  implicit val byteOrder = ByteOrder.BIG_ENDIAN
  def receive = {
    case fragment:ByteString => {
      buffer = buffer ++ fragment
      obtainMessage()
    }
  }

  private def obtainMessage() :Unit = {
    if(currentLength == 0)
      peakMessageLength()

    log.debug("currentLength is {}, buffer size is {}",currentLength,buffer.size)
    if(currentLength > 0 && buffer.size >= currentLength) {

      buffer = buffer.compact
      val message = decode(buffer.take(currentLength))
      if(message != null) callback ! message

      buffer = buffer.drop(currentLength)
      currentLength = 0
      if(buffer.size > 0)
        obtainMessage()
    }
  }

  private def peakMessageLength(): Unit = {
    val versionAndLength = buffer.iterator.getInt
    val version = (versionAndLength & 0xff000000) >> 24
    val diameterLength = versionAndLength & 0x00ffffff
    if(version != 1)  {
      log.error("Diameter version is not {}, not expected 1",version)

    } else  {
      log.debug("peak new currentLength {}",diameterLength)
      currentLength = diameterLength
    }

  }
  private def decode(raw:ByteString):DiameterMessage = {
    val source = raw.iterator
    val versionAndLength = source.getInt
    //skip version and Length, since it is already handled
    val flagsAndCommand = source.getInt
    val flags = (flagsAndCommand & 0xff000000) >> 24
    val request = (flags & 0x80) != 0
    val proxyable = (flags & 0x40) != 0
    val error = (flags & 0x20) != 0
    val retran = (flags & 0x10) != 0

    val cmd = flagsAndCommand & 0x00ffffff
    val appId = source.getInt
    val h2h = source.getInt
    val e2e = source.getInt

    val avps = DiameterAvp.splitAvps(source)
    DiameterMessage(
      DiameterHeader(request,proxyable,error,retran,cmd,appId,h2h,e2e),
      avps
    )
  }



  def encode(message:DiameterMessage):ByteString = {
    null
  }
}

object DiameterMessageBuffer  {
  def props(listner:ActorRef) = Props(new DiameterMessageBuffer(listner))
}


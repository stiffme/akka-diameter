package org.esipeng.akka.io.diameter

import java.nio.{ByteBuffer, ByteOrder}

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.util.{ByteString, ByteStringBuilder}
import org.esipeng.akka.io.diameter.util.ByteFlagUtil

/**
  * Created by stiff on 2016/4/15.
  */

class DiameterMessageBufferDecoder(callback:ActorRef) extends Actor with ActorLogging with ByteFlagUtil {
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
    val flags:Byte = ((flagsAndCommand & 0xff000000) >> 24).toByte
    val request = getFlagsFromHighest(flags,0)
    val proxyable = getFlagsFromHighest(flags,1)
    val error = getFlagsFromHighest(flags,2)
    val retran = getFlagsFromHighest(flags,3)

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
}

object DiameterMessageBufferDecoder  {
  def props(listner:ActorRef) = Props(new DiameterMessageBufferDecoder(listner))
}


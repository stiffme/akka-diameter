package org.esipeng.akka.io.diameter

import java.nio.{ByteBuffer, ByteOrder}

import akka.actor.{Actor, ActorLogging, ActorRef}
import akka.util.{ByteIterator, ByteString}

/**
  * Created by esipeng on 4/12/2016.
  */
case class DiameterHeader(
                           request:Boolean,
                           proxyable:Boolean,
                           error:Boolean,
                           retransmission:Boolean,
                           commandCode:Int,
                           applicationId:Int,
                           h2h:Int, e2e:Int)

case class DiameterAvp(code:Int,vendorSpecific:Boolean,mandatory:Boolean,proted:Boolean,vendorId:Option[Int],payload:ByteString)

case class DiameterMessage(header:DiameterHeader,avps:Seq[DiameterAvp])

object DiameterAvp  {
  implicit  val byteOrder = ByteOrder.BIG_ENDIAN

  def splitAvps(source:ByteIterator):Seq[DiameterAvp] =  {
    val avps:collection.mutable.ListBuffer[DiameterAvp] = collection.mutable.ListBuffer.empty[DiameterAvp]

    while(source.len > 0)  {
      avps += decodeAvp(source)
    }
    avps
  }

  private def decodeAvp(source:ByteIterator):DiameterAvp = {
    var usedLength = 0
    val avpCode = source.getInt; usedLength += 4
    val flagsAndLength = source.getInt
    usedLength += 4
    val flags = (flagsAndLength & 0xff000000) >> 24
    val vendorSpecific = (flags & 0x80) != 0
    val mandatory = (flags & 0x40) != 0
    val protect = (flags & 0x20) != 0

    val avpLength:Int = flagsAndLength & 0x00ffffff
    val vendorId = if(vendorSpecific) {

      usedLength += 4
      Some(source.getInt)
    } else None

    val payloadBytes = new Array[Byte](avpLength - usedLength)
    source.getBytes(payloadBytes)

    val mod = avpLength % 4
    if (mod != 0)
      for( i <- 0 until 4 - mod ) source.getByte

    DiameterAvp(avpCode,vendorSpecific,mandatory,protect,vendorId,ByteString(payloadBytes))
  }
}

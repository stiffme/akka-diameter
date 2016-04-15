package org.esipeng.akka.io.diameter

import java.nio.ByteBuffer

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
    case fragment:ByteString => {
      buffer = buffer ++ fragment
      obtainMessage()
    }
  }

  private def obtainMessage() :Unit = {
    if(currentLength == 0)
      peakMessageLength()

    if(currentLength > 0 && buffer.size >= currentLength) {
      buffer = buffer.compact
      val message = decode(buffer.take(currentLength))
      if(message != null) callback ! message

      buffer = buffer.drop(currentLength)
      currentLength = 0
      obtainMessage()
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
  private def decode(raw:ByteString):DiameterMessage = {
    val source = raw.asByteBuffer
    val versionAndLength = source.getInt
    //skip version and Length, since it is already handled
    val flags = source.get
    val request = (flags & 0x80) != 0
    val proxyable = (flags & 0x40) != 0
    val error = (flags & 0x20) != 0
    val retran = (flags & 0x10) != 0
    val cmd = source.get << 16 + source.get << 8 + source.get
    val appId = source.getInt
    val h2h = source.getInt
    val e2e = source.getInt

    val avps:collection.mutable.ListBuffer[DiameterAvp] = collection.mutable.ListBuffer.empty[DiameterAvp]

    while(source.hasRemaining)  {
      avps += decodeAvp(source)
    }
    DiameterMessage(
      DiameterHeader(request,proxyable,error,retran,cmd,appId),
      avps
    )
  }

  private def decodeAvp(source:ByteBuffer):DiameterAvp = {
    var usedLength = 0
    val avpCode = source.getInt; usedLength += 4
    val flags = source.get ; usedLength += 1
    val vendorSpecific = (flags & 0x80) != 0
    val mandatory = (flags & 0x40) != 0
    val protect = (flags & 0x20) != 0
    val avpLength = source.get << 16 + source.get << 8 + source.get ;usedLength += 3
    val vendorId = if(vendorSpecific) {

      usedLength += 4
      Some(source.getInt)
    } else None

    val payloadBytes = new Array[Byte](avpLength - usedLength)
    source.get(payloadBytes)

    val mod = avpLength % 4
    if (mod != 0)
      for( i <- 0 until 4 - mod ) source.get

    DiameterAvp(avpCode,vendorSpecific,mandatory,protect,vendorId,ByteString(payloadBytes))
  }

  def encode(message:DiameterMessage):ByteString = {
    null
  }
}



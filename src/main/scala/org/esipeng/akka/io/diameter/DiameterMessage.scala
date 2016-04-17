package org.esipeng.akka.io.diameter

import java.nio.{ByteBuffer, ByteOrder}

import akka.actor.{Actor, ActorLogging, ActorRef}
import akka.util.{ByteIterator, ByteString, ByteStringBuilder}
import org.esipeng.akka.io.diameter.util.ByteFlagUtil

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
                           h2h:Int=0, e2e:Int=0)

case class DiameterAvp(code:Int,vendorSpecific:Boolean,mandatory:Boolean,proted:Boolean,vendorId:Option[Int],payload:ByteString)  {
  lazy val asInt = payload.iterator.getInt
  lazy val asString = payload.utf8String
  lazy val children = DiameterAvp.splitAvps(payload.iterator)
}



case class DiameterMessage(header:DiameterHeader,avps:Seq[DiameterAvp]) {
  def prepareAnswer(answerAvps:Seq[DiameterAvp]): DiameterMessage =  {
    val answerHeader = DiameterHeader(false,header.proxyable,false,false,header.commandCode,header.applicationId,header.h2h,header.e2e)
    DiameterMessage(answerHeader,answerAvps)
  }
}

object DiameterAvp extends ByteFlagUtil{
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
    val flags = ((flagsAndLength & 0xff000000) >> 24).toByte
    val vendorSpecific = getFlagsFromHighest(flags,0)
    val mandatory = getFlagsFromHighest(flags,1)
    val protect = getFlagsFromHighest(flags,2)

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

  def encodeAvp(avp:DiameterAvp):ByteString = {
    val builder = new ByteStringBuilder
    builder.putInt(avp.code)
    val flags = generateFlagsFromHighest(avp.vendorSpecific,avp.mandatory,avp.proted)
    val length = if(avp.vendorSpecific == false) avp.payload.size + 4 /*avp code*/+ 1/*flags*/ + 3/*length*/
    else avp.payload.size + 4 /*avp code*/+ 1/*flags*/ + 3/*length*/ + 4 /*vendor id*/

    val flagsAndLength:Int = (flags << 24) | length
    builder.putInt(flagsAndLength)
    if(avp.vendorSpecific)
      builder.putInt(avp.vendorId.getOrElse(0))
    builder ++= avp.payload
    //padding handling
    val mod = length % 4
    if(mod != 0)
      for( i <- 0 until (4 - mod)) builder += 0x0

    builder.result()
  }

  def apply(code: Int, vendorSpecific: Boolean, mandatory: Boolean, proted: Boolean, vendorId: Option[Int], intValue:Int): DiameterAvp = {
    DiameterAvp(code,vendorSpecific,mandatory,proted,vendorId,(new ByteStringBuilder).putInt(intValue).result())
  }

  def apply(code: Int, vendorSpecific: Boolean, mandatory: Boolean, proted: Boolean, vendorId: Option[Int], stringValue:String): DiameterAvp = {
    DiameterAvp(code,vendorSpecific,mandatory,proted,vendorId,(new ByteStringBuilder).putBytes(stringValue.getBytes).result())
  }

  def apply(code: Int, vendorSpecific: Boolean, mandatory: Boolean, proted: Boolean, vendorId: Option[Int], children:Seq[DiameterAvp]): DiameterAvp = {
    val builder = new ByteStringBuilder
    children.foreach( c => builder ++= encodeAvp(c))
    DiameterAvp(code,vendorSpecific,mandatory,proted,vendorId,(new ByteStringBuilder).result())
  }




}

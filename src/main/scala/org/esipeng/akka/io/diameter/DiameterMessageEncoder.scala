package org.esipeng.akka.io.diameter

import akka.util.{ByteString, ByteStringBuilder}
import org.esipeng.akka.io.diameter.util.ByteFlagUtil

/**
  * Created by stiff on 2016/4/17.
  */
class DiameterMessageEncoder extends ByteFlagUtil{
  implicit val order = java.nio.ByteOrder.BIG_ENDIAN
  def encode(message:DiameterMessage,h2hOverride:Int,e2eOverride:Int):ByteString = {

    //encode all the avps
    val encodedAvps = message.avps.map( avp => {
      DiameterAvp.encodeAvp(avp)
    })
    val length =
      4 + /*version and length*/
        4 + /*flags and command*/
        4 + /* appid */
        4 + /* h2h */
        4 + /* e2e */
        encodedAvps.foldLeft(0)(_ + _.size)

    val versionAndLength = (0x01 << 24 ) | length
    val flags = generateFlagsFromHighest(message.header.request,
      message.header.proxyable,
      message.header.error,
      message.header.retransmission)

    val flagsAndCode = (flags << 24) | message.header.commandCode
    val builder = new ByteStringBuilder
    builder.putInt(versionAndLength)
      .putInt(flagsAndCode)
      .putInt(message.header.applicationId)
      .putInt(message.header.h2h)
      .putInt(message.header.e2e)
    //append all the avps
    encodedAvps.foreach(builder ++= _)
    builder.result()
  }

  def encode(message:DiameterMessage):ByteString = {
    encode(message,message.header.h2h,message.header.e2e)
  }
}

package org.esipeng.akka.io.diameter

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

object DiameterMessage  {
  def decode(raw:ByteString):DiameterMessage = {
    val rawBuffer = raw.asByteBuffer
    val versionAndLength = rawBuffer.getInt
    val version = (versionAndLength & 0xff000000) >> 24
    val diameterLength = versionAndLength & 0x00ffffff

    if()

  }

  def encode(message:DiameterMessage):ByteString = {
    null
  }
}



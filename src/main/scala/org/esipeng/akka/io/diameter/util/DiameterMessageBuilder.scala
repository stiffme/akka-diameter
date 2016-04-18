package org.esipeng.akka.io.diameter.util

import org.esipeng.akka.io.diameter.{DiameterAvp, DiameterHeader, DiameterMessage}

/**
  * Created by esipeng on 4/18/2016.
  */
protected class DiameterMessageBuilder(header:DiameterHeader) {
  private val avps = collection.mutable.ListBuffer.empty[DiameterAvp]
  def appendAvp(avp:DiameterAvp*):DiameterMessageBuilder = {
    avps ++= avp
    this
  }

  def appendAvps(trans:TraversableOnce[DiameterAvp]): DiameterMessageBuilder =  {
    trans.foreach(appendAvp(_))
    this
  }

  def makeMessage():DiameterMessage = {
    DiameterMessage(header,avps)
  }
}


object DiameterMessageBuilder {
  def newRequest(command:Int,appId:Int):DiameterMessageBuilder = {
    val header = DiameterHeader(true,true,false,false,command,appId,0,0)
    new DiameterMessageBuilder(header)
  }

  def answerRequest(request:DiameterMessage):DiameterMessageBuilder = {
    val header = DiameterHeader(false,true,false,false,request.header.commandCode,request.header.applicationId,request.header.h2h,request.header.e2e)
    new DiameterMessageBuilder(header)
  }
}
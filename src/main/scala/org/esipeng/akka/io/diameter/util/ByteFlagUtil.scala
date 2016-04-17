package org.esipeng.akka.io.diameter.util

import org.slf4j.LoggerFactory

/**
  * Created by stiff on 2016/4/17.
  */
trait ByteFlagUtil {
  //val log = LoggerFactory.getLogger(classOf[ByteFlagUtil])
  final val valuesFromHighest:Array[Byte] =Array(0x80,0x40,0x20,0x10,0x8,0x4,0x2,0x1).map(_.toByte)
  def generateFlagsFromHighest(switches:Boolean*):Byte = {
    var ret:Byte = 0
    if(switches.size > 8)
      throw new IllegalArgumentException("Switches count should be no greater than 8")

    var pos:Int = 0
    for(switch <- switches) {
      if(switch) ret = (ret | valuesFromHighest(pos)).toByte
      pos += 1
    }
    ret
  }

  def getFlagsFromHighest(value:Byte,switch:Int):Boolean = {
    if(switch >= 8) {
      throw new IllegalArgumentException("switch index is above 7, actually a byte has only 8 switches")
    } else  {
      (value & valuesFromHighest(switch)) != 0
    }
  }
}

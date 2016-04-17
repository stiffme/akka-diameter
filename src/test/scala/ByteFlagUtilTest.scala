/**
  * Created by stiff on 2016/4/17.
  */
import org.esipeng.akka.io.diameter.util.ByteFlagUtil
import org.junit.Test

class ByteFlagUtilTest {
  class Dummy extends ByteFlagUtil
  val util = new Dummy()
  @Test
  def generateFlags(): Unit = {
    assert(0x80.toByte == util.generateFlagsFromHighest(true))
    assert(0x40.toByte == util.generateFlagsFromHighest(false,true))
    assert(0x20.toByte == util.generateFlagsFromHighest(false,false,true))
    assert(0x10.toByte == util.generateFlagsFromHighest(false,false,false,true))
    assert(0x8.toByte == util.generateFlagsFromHighest(false,false,false,false,true))
    assert(0x4.toByte == util.generateFlagsFromHighest(false,false,false,false,false,true))
    assert(0x2.toByte == util.generateFlagsFromHighest(false,false,false,false,false,false,true))
    assert(0x1.toByte == util.generateFlagsFromHighest(false,false,false,false,false,false,false,true))
    assert(0x0.toByte == util.generateFlagsFromHighest(false,false,false,false,false,false,false,false))

    assert(0x24.toByte == util.generateFlagsFromHighest(false,false,true,false,false,true))

    assert(0xff.toByte == util.generateFlagsFromHighest(true,true,true,true,true,true,true,true))
    assert(0xAA.toByte == util.generateFlagsFromHighest(true,false,true,false,true,false,true,false))
  }

  @Test
  def getFlas():Unit = {
    val testValue = 0xAA.toByte
    val testValue2 = 0x80.toByte
    assert(util.getFlagsFromHighest(testValue2,0) == true)
    assert(util.getFlagsFromHighest(testValue2,1) == false)
    for( i <- 0 until 7)  {
      assert(util.getFlagsFromHighest(testValue,i) == (i % 2 == 0))
    }
  }
}

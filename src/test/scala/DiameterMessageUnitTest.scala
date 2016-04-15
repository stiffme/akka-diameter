/**
  * Created by esipeng on 4/15/2016.
  */
import akka.util.ByteString
import org.esipeng.akka.io.diameter.DiameterAvp
import org.junit.Test

class DiameterMessageUnitTest {

  @Test
  def decodeNormalSingleAvp(): Unit = {
    val raw = ByteString(HexUtil.hexStringToByte("00000128400000146572696373736f6e2e636f6d"))
    val iterator = raw.compact.iterator
    val avps = DiameterAvp.splitAvps(iterator)
    assert(avps.size == 1)
    val avp = avps(0)
    assert(avp.code == 296)
    assert(avp.mandatory == true)
    assert(avp.payload.size == 12)
    assert(avp.vendorId == None)
  }


  @Test
  def decodeNormalVendorAvp(): Unit = {
    val raw = ByteString(HexUtil.hexStringToByte("00000266c0000010000028af00000003"))
    val iterator = raw.compact.iterator
    val avps = DiameterAvp.splitAvps(iterator)
    assert(avps.size == 1)
    val avp = avps(0)
    assert(avp.code == 614)
    assert(avp.mandatory == true)
    assert(avp.payload.size == 4)
    assert(avp.vendorId == Some(10415))
  }


}

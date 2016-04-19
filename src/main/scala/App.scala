import java.net.{InetAddress, InetSocketAddress}

import akka.actor.ActorSystem
import akka.io.IO
import org.esipeng.akka.io.diameter.{Diameter, DiameterSettings}

/**
 * @author ${user.name}
 */
object App {
  
  def main(args : Array[String]) {
    implicit val system = ActorSystem("test")
    val settings = DiameterSettings("OriginTest.ericsson.se","ericsson.se",10415,Set[(Int,Int)]((10415,16777216)))
    IO(Diameter) ! Diameter.Connect(new InetSocketAddress("127.0.0.1",3872),settings)
  }

}

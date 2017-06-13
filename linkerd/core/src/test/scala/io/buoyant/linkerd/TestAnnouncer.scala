package io.buoyant.linkerd

import com.fasterxml.jackson.annotation.JsonIgnore
import com.twitter.finagle.{Announcement, Path}
import com.twitter.util.{Closable, Future, Time}
import java.net.InetSocketAddress

class TestAnnouncerInitializer extends AnnouncerInitializer {
  override def configClass: Class[_] = classOf[TestAnnouncerConfig]
  override def configId: String = "io.l5d.test"
}

object TestAnnouncerInitializer extends TestAnnouncerInitializer

class TestAnnouncerConfig extends AnnouncerConfig {

  @JsonIgnore
  override def defaultPrefix: Path = Path.read("/io.l5d.test")

  override def mk(): Announcer = new TestAnnouncer
}

class TestAnnouncer extends Announcer {
  override val scheme: String = "test"

  var services: Map[Path, Set[InetSocketAddress]] = Map.empty

  def announce(addr: InetSocketAddress, name: Path): Closable = {
    synchronized {
      services = services + (name -> (services(name) + addr))
    }
    new Announcement {
      override def unannounce(): Future[Unit] = synchronized {
        services = services + (name -> (services(name) - addr))
        Future.Unit
      }
    }
  }

  override def close(deadline: Time): Future[Unit] = Future.Unit
}

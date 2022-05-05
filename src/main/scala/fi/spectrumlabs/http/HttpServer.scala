package fi.spectrumlabs.http

import cats.effect.{Concurrent, ConcurrentEffect, ContextShift, Resource, Timer}
import cats.syntax.semigroupk._
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.server.middleware.CORS
import org.http4s.server.{Router, Server}
import sttp.tapir.server.http4s.Http4sServerOptions
import tofu.lift.Unlift

import scala.concurrent.ExecutionContext

import fi.spectrumlabs.AppConfig

object HttpServer {

  def make[
      I[_]: ConcurrentEffect: ContextShift: Timer,
      F[_]: Concurrent: ContextShift: Timer: Unlift[*[_], I]
  ](conf: AppConfig, ec: ExecutionContext)(
      implicit
      meta: MetadataService[F],
      opts: Http4sServerOptions[F, F]): Resource[I, Server] = {
    val metadataR = MetadataRoutes.make[F]
    val routes = unliftRoutes[F, I](metadataR)
    val corsRoutes = CORS.policy.withAllowOriginAll(routes)
    val api = Router("/" -> corsRoutes).orNotFound
    BlazeServerBuilder[I](ec)
      .bindHttp(conf.http.port, conf.http.host)
      .withHttpApp(api)
      .resource
  }
}

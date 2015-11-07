/*
 * Copyright (c) 2015 Brian Wawok <bwawok@gmail.com>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 *
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 *
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.wawok

/**
  * Created by Brian Wawok on 11/6/2015.
  */

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpResponse, HttpRequest}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route

import com.typesafe.config.ConfigFactory
import com.wawok.Models._
import slick.driver.H2Driver.api._
import org.slf4j.LoggerFactory
import spray.json._
import akka.stream.{Materializer, ActorMaterializer}

import scala.concurrent.{ExecutionContextExecutor, Future, ExecutionContext, Await}
import scala.concurrent.duration._


trait Service extends JsonSupport with DatabaseService {

  implicit val system: ActorSystem
  implicit def executor: ExecutionContextExecutor
  implicit val materializer: Materializer



  def routes() = {
    path("event") {
      post {
        entity(as[InboundRequest]) { inboundRequest =>
          complete {
              inboundRequest.`type` match {
                case InBoundRequestType.InboundText =>
                  "Success:" + inboundRequest.toString + ":" + inboundRequest.toJson
                case _ =>
                  "Success:" + inboundRequest.toString + ":" + inboundRequest.toJson
              }
          }


        }
      }
    } ~
      path("report") {
        get {
          complete {
            voteCount().map {
              case count => s"Report coming soon! Current count: $count"
            }
          }

        }
      }
  }
}

object PictureVoterService extends App with Service {


  override val logger = LoggerFactory.getLogger(this.getClass)

  implicit val system = ActorSystem("my-system")
  implicit val executor = system.dispatcher
  implicit val materializer = ActorMaterializer()
  override val db = Database.forConfig("h2-disk-config")

  logger.info("Waiting for database server to continue starting....")
  Await.result(setup(), 5.minutes)
  logger.info("Database service successfully started!")


  val config = ConfigFactory.load()
  val interface = config.getString("http.interface")
  val port = config.getInt("http.port")

  val bindingFuture = Http().bindAndHandle(routes, interface, port)

  logger.info(s"Server online at http://$interface:$port/")
  logger.info(s"Press <enter> to exit...")
  scala.io.StdIn.readLine() // for the future transformations
  bindingFuture
    .flatMap(_.unbind()) // trigger unbinding from the port
    .onComplete(_ ⇒ system.shutdown()) // and shutdown when done
}


/*
{ "type": "inboundText", "payload": "Hello", "fromNumber": "+14444444", "toNumber": "+15555555" }
{ "type": "inboundMedia", "payload": "", "fromNumber": "+14444444", "toNumber": "+15555555" }
{ "type": "voiceMail", "payload": "", "fromNumber": "+14444444", "toNumber": "+15555555" }
 */
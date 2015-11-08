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
import akka.http.scaladsl.server.Directives._
import akka.stream.{ActorMaterializer, Materializer}
import com.wawok.Models._
import org.slf4j.LoggerFactory
import slick.driver.H2Driver.api._
import spray.json._

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContextExecutor, Future}


trait Service extends JsonSupport with DatabaseService with DropboxSaver {

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
                processInboundText(inboundRequest)
              case InBoundRequestType.InboundMedia =>
                processInboundMedia(inboundRequest)
              case _ =>
                "Unknown request:" + inboundRequest.toString + ":" + inboundRequest.toJson
            }
          }
        }
      }
    } ~
      path("report") {
        get {
          entity(as[PhoneNumber]) { targetPhoneNumber =>
            complete {
              getAllVoteCounts(targetPhoneNumber).map {
                case result => result.toJson
              }
            }
          }

        }
      }
  }


  private[this] def processInboundText(inboundRequest: InboundRequest) = {
    val pictureName = inboundRequest.payload.split("/").last
    findPictureIdForNameAndTarget(pictureName, inboundRequest.toNumber).flatMap {
      case Some(pictureId) =>
        voteForPicture(pictureId, inboundRequest.fromNumber, inboundRequest.toNumber).map(voteSucces => {
          if (voteSucces) {
            s"Vote submitted for '$pictureName'"
          } else {
            s"Not able to submit vote for '$pictureName', perhaps you already voted for this image?"
          }
        })
      case _ =>
        Future.successful(s"Unable to find picture '$pictureName' to vote on")
    }
  }

  private[this] def processInboundMedia(inboundRequest: InboundRequest) = {
    val pictureName = inboundRequest.payload.split("/").last
    val newPicture = Picture(0, pictureName, inboundRequest.fromNumber, inboundRequest.toNumber)
    saveNewPicture(newPicture).flatMap {
      case true =>
        saveUrl(inboundRequest.payload).map(saveUrlSuccess => {
          if (saveUrlSuccess) {
            s"New picture '$pictureName' added to service"
          } else {
            s"Unable to save new picture '$pictureName'"
          }
        })
      case false =>
        Future.successful(s"Picture '$pictureName' already exists")
    }
  }

}

//noinspection FieldFromDelayedInit
object PictureVoterService extends App with Service with RealDropboxSaver with ConfigService {


  private[this] val logger = LoggerFactory.getLogger(this.getClass)

  implicit val system = ActorSystem("my-system")
  implicit val executor = system.dispatcher
  implicit val materializer = ActorMaterializer()
  override val db = Database.forConfig("h2-disk-config")


  logger.info("Waiting for database server to continue starting....")
  Await.result(setup(), 5.minutes)
  logger.info("Database service successfully started!")



  val bindingFuture = Http().bindAndHandle(routes(), INTERFACE, HTTP_PORT)

  logger.info(s"Server online at http://$INTERFACE:$HTTP_PORT/")
  logger.info(s"Press <enter> to exit...")
  scala.io.StdIn.readLine() // for the future transformations
  bindingFuture
    .flatMap(_.unbind()) // trigger unbinding from the port
    .onComplete(_ ⇒ system.shutdown()) // and shutdown when done
}


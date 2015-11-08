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
  * Created by Brian Wawok on 11/7/2015.
  */

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.RawHeader
import akka.stream.Materializer
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContextExecutor, Future}

trait DropboxSaver {
  def saveUrl(url: String): Future[Boolean]
}


object RealDropboxSaver {
  val SAVE_URL_PATH = "1/save_url/auto/"
}

trait RealDropboxSaver extends DropboxSaver with ConfigService {

  implicit val system: ActorSystem

  implicit def executor: ExecutionContextExecutor

  implicit val materializer: Materializer

  private[this] val logger = LoggerFactory.getLogger(this.getClass)

  override def saveUrl(url: String): Future[Boolean] = {


    val fileName = url.split("/").last


    val fullUrl = s"$DROPBOX_HOST/${RealDropboxSaver.SAVE_URL_PATH}/$DROPBOX_FOLDER/$fileName"
    logger.debug(s"Full url: $fullUrl, name: $fileName")

    val entity = s"""url=$url"""
    logger.debug(s"Entity: $entity")


    Http().singleRequest(request = HttpRequest(
      method = HttpMethods.POST,
      uri = fullUrl,
      entity = HttpEntity(
        ContentTypes.`application/x-www-form-urlencoded`,
        entity
      ),
      headers = RawHeader("Authorization", s"Bearer $DROPBOX_TOKEN") :: Nil)
    ).map(res => {
      logger.debug(s"****** Result=$res")
      res.status match {
        case StatusCodes.Success(code) =>
          true
        case _ =>
          false
      }
    })


  }

}

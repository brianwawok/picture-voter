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
import akka.stream.ActorMaterializer
import com.wawok.Models._
import com.wawok.PictureVoterService._
import org.scalatest.{FlatSpec, Matchers, WordSpec}
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.http.scaladsl.server._
import Directives._
import spray.json.JsString
import slick.driver.H2Driver.api._
import scala.concurrent.duration._

import scala.concurrent.Await


class PictureVoterServiceTest extends FlatSpec with Matchers with ScalatestRouteTest with JsonSupport {



  trait InMemoryDatabase extends HasDatabase{
    override val db = Database.forConfig("h2-test-config")
  }


  object serviceTest extends Service with InMemoryDatabase {
    implicit val system = ActorSystem("my-system")
    implicit val executor = system.dispatcher
    implicit val materializer = ActorMaterializer()
  }

  Await.result(serviceTest.setup(), 5.minutes)
  val route = serviceTest.routes()

  "The picture service" should "not try to handle a get request to root" in {
    Get("/") ~> route ~> check {
      handled shouldBe false
    }
  }

  it should "not respond to a get request to event either" in {
    Get("/event") ~> route  ~> check {
      handled shouldBe false
    }
  }

  it should "always respond to a get request to report" in {
    Get("/report") ~> route  ~> check {
      handled shouldBe true
    }
  }

  it should "give an unsupported media type exception if a good request was not sent as json" in {
    Post("/event", """{ "type": "inboundText", "payload": "Hello", "fromNumber": "+14444444", "toNumber": "+15555555" }""") ~> Route.seal(route) ~> check {
      status shouldEqual  StatusCodes.UnsupportedMediaType
      responseAs[String] shouldEqual "The request's Content-Type is not supported. Expected:\napplication/json"
    }
  }







}


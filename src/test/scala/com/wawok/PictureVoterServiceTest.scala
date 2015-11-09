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
import org.scalatest.{FlatSpec, Matchers}
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.http.scaladsl.server._
import slick.driver.H2Driver.api._
import scala.concurrent.duration._

import scala.concurrent.{Future, Await}


class PictureVoterServiceTest extends FlatSpec with Matchers with ScalatestRouteTest with JsonSupport {


  val targetPhoneNumber = PhoneNumber("+15551234567")

  val submitter1PhoneNumber = PhoneNumber("+15552345698")
  val submitter2PhoneNumber = PhoneNumber("+15552345699")
  val url_1 = "http://cat.jpg"
  val name_1 = "cat.jpg"
  val url_2 = "http://dog.jpg"
  val name_2 = "dog.jpg"

  trait FakeDropboxSaver extends DropboxSaver {
    override def saveUrl(url: String): Future[Boolean] = {
      Future.successful(true)
    }
  }

  object serviceTest extends Service with FakeDropboxSaver {
    implicit val system = ActorSystem("my-system")
    implicit val executor = system.dispatcher
    implicit val materializer = ActorMaterializer()
    override val db = Database.forConfig("h2-test-config")

  }

  Await.result(serviceTest.setup(), 5.minutes)
  val route = serviceTest.routes()

  "The picture service" should "not try to handle a get request to root" in {
    Get("/") ~> route ~> check {
      handled shouldBe false
    }
  }

  it should "not respond to a get request to event either" in {
    Get("/event") ~> route ~> check {
      handled shouldBe false
    }
  }

  it should "give an unsupported media type exception if a good request was not sent as json" in {
    Post("/event", """{ "type": "inboundText", "payload": "Hello", "fromNumber": "+14444444", "toNumber": "+15555555" }""") ~> Route.seal(route) ~> check {
      status shouldEqual StatusCodes.UnsupportedMediaType
      responseAs[String] shouldEqual "The request's Content-Type is not supported. Expected:\napplication/json"
    }
  }

  it should "our initial report should be an empty json string" in {
    Get("/report", targetPhoneNumber) ~> route ~> check {
      responseAs[String] shouldEqual "{\n\n}"
    }
  }


  val picture1 = InboundRequest(InBoundRequestType.InboundMedia, url_1, submitter1PhoneNumber, targetPhoneNumber)
  it should "happy allow us to add a new picture to the service" in {
    Post("/event", picture1) ~> route ~> check {
      handled shouldBe true
      responseAs[String] shouldEqual s"New picture '$name_1' added to service"
    }
  }

  it should "tell us the picture already exists if we try to add it again" in {
    Post("/event", picture1) ~> route ~> check {
      handled shouldBe true
      responseAs[String] shouldEqual s"Picture '$name_1' already exists"
    }
  }

  it should "still give us a blank report without any votes cast" in {
    Get("/report", targetPhoneNumber) ~> route ~> check {
      handled shouldBe true
      responseAs[String] shouldEqual "{\n\n}"
    }
  }

  val vote1 = InboundRequest(InBoundRequestType.InboundText, name_1, submitter1PhoneNumber, targetPhoneNumber)
  it should "save vote1 to the database" in {
    Post("/event", vote1) ~> route ~> check {
      handled shouldBe true
      responseAs[String] shouldEqual s"Vote submitted for '$name_1'"
    }
  }

  it should "now give us a report with image_1 in the lead" in {
    Get("/report", targetPhoneNumber) ~> route ~> check {
      handled shouldBe true
      responseAs[String] shouldEqual s"""{\n  "$name_1": 1\n}"""
    }
  }

  it should "not allow us to save the same vote1 to the database" in {
    Post("/event", vote1) ~> route ~> check {
      handled shouldBe true
      responseAs[String] shouldEqual s"Not able to submit vote for '$name_1', perhaps you already voted for this image?"
    }
  }

  val picture2 = InboundRequest(InBoundRequestType.InboundMedia, url_2, submitter1PhoneNumber, targetPhoneNumber)
  it should "happy allow us to add a second  to the service" in {
    Post("/event", picture2) ~> route ~> check {
      handled shouldBe true
      responseAs[String] shouldEqual s"New picture '$name_2' added to service"
    }
  }


  val vote2 = InboundRequest(InBoundRequestType.InboundText, name_2, submitter1PhoneNumber, targetPhoneNumber)
  it should "save vote2 to the database" in {
    Post("/event", vote2) ~> route ~> check {
      handled shouldBe true
      responseAs[String] shouldEqual s"Vote submitted for '$name_2'"
    }
  }

  val vote3 = InboundRequest(InBoundRequestType.InboundText, name_2, submitter2PhoneNumber, targetPhoneNumber)
  it should "save vote3 to the database" in {
    Post("/event", vote3) ~> route ~> check {
      handled shouldBe true
      responseAs[String] shouldEqual s"Vote submitted for '$name_2'"
    }
  }

  it should "now give us a report with image_2 in the lead and image_1 with 1 vote" in {
    Get("/report", targetPhoneNumber) ~> route ~> check {
      handled shouldBe true
      responseAs[String] shouldEqual s"""{\n  "$name_2": 2,\n  "$name_1": 1\n}"""
    }
  }


}


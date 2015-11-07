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

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import com.wawok.Models._
import spray.json._

/**
  * Created by Brian Wawok on 11/6/2015.
  */

trait JsonSupport extends SprayJsonSupport with DefaultJsonProtocol {

  implicit object phoneNumberFormat extends RootJsonFormat[PhoneNumber] {

    def write(p: PhoneNumber) = JsString(p.phoneNumber)

    def read(value: JsValue) = value match {
      case JsString(phoneNumber) =>
        new PhoneNumber(phoneNumber)
      case _ =>
        deserializationError("Phone Number expected")
    }
  }

  implicit object InboundRequestTypeFormat extends RootJsonFormat[InBoundRequestType.Value] {

    def write(ibr: InBoundRequestType.Value) = JsString(ibr.toString)

    private[this] def failed = deserializationError("InboundRequestTypeFormat expected")

    def read(value: JsValue) = value match {
      case JsString(inboundRequestTypeString) =>
        InBoundRequestType.fromString(inboundRequestTypeString).getOrElse(failed)
      case _ => failed
    }
  }


  implicit val inboundRequestFormat = jsonFormat4(InboundRequest)

}
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

import java.time.LocalDateTime

import com.sun.glass.ui.InvokeLaterDispatcher.InvokeLaterSubmitter


/**
  * Created by Brian Wawok on 11/6/2015.
  */

//only had a few so seemed silly to put in separate packages, eventually would migrate them out
object Models {

  object PhoneNumber {
    val PHONE_VALID_REGEX = "\\+\\d{11,15}".r
  }

  case class PhoneNumber(phoneNumber: String) {
    require(PhoneNumber.PHONE_VALID_REGEX.pattern.matcher(phoneNumber).matches, "Phone number must start with + and be 11-15 digits")

  }


  object InBoundRequestType extends Enumeration {

    val InboundText, InboundMedia, VoiceMail = Value

    def fromString(input: String) = values.find(_.toString.equalsIgnoreCase(input))

  }


  case class InboundRequest(`type`: InBoundRequestType.Value, payload: String, fromNumber: PhoneNumber, toNumber: PhoneNumber)


  case class Picture(pictureId : Long, name : String, submitterPhone : PhoneNumber, targetPhone: PhoneNumber)


  case class Vote(targetPhone: PhoneNumber, submitterPhone: PhoneNumber, pictureId : Long, voteTime : LocalDateTime)

}


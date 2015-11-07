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

import java.time.LocalDateTime

import com.wawok.Models._
import org.slf4j.LoggerFactory
import slick.driver.H2Driver.api._
import slick.jdbc.meta.MTable
import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent.{ExecutionContext, Future}


trait HasDatabase {
  val db : Database
}

trait OnDiskDatabase extends HasDatabase{
  override val db = Database.forConfig("h2-disk-config")
}

trait DatabaseService {
  this: HasDatabase =>

  val logger = LoggerFactory.getLogger(this.getClass)


  import Votes.votes
  import Pictures.pictures


  def setup(): Future[Unit] = {

    db.run(
      MTable.getTables("votes")
    ).flatMap(tables => {
      if (tables.isEmpty) {
        logger.info("Database tables do not exist, creating them...")
        db.run((votes.schema ++ pictures.schema).create)
      } else {
        Future.successful(
          logger.info("Database tables already exist, skipping....")
        )
      }
    })


  }

  def voteCount(): Future[Int] = {
    db.run(
      votes.length.result
    )
  }


  /*

  def voteForPicture(name : String, voterPhone: PhoneNumber) : Future[Boolean] = {
    db.run(
       pictures.filter(_.name === name).map(_.pictureId).take(1).result.headOption.flatMap(idOpt => {
         idOpt.map(id => {
           val vote = Vote(voterPhone, id, LocalDateTime.now())
           (votes += vote).map(insertResult => {
             insertResult > 0
           })
         })


       })

    )
  }
  */

}


object Votes extends DatabaseSupport {

  class Votes(tag: Tag) extends Table[Vote](tag, "votes") {

    def pictureId = column[Long]("picture_id")

    def phoneNumber = column[PhoneNumber]("phone_number")

    def voteTime = column[LocalDateTime]("vote_time")

    def pk = primaryKey("vote_pk", (pictureId, phoneNumber))

    def * = (pictureId, phoneNumber, voteTime) <>(Vote.tupled, Vote.unapply)

    def fk = foreignKey("picture_fk", pictureId, Pictures.pictures)(_.pictureId)

  }

  val votes = TableQuery[Votes]

}


object Pictures extends DatabaseSupport {

  class Pictures(tag: Tag) extends Table[Picture](tag, "pictures") {

    def pictureId = column[Long]("picture_id", O.PrimaryKey, O.AutoInc)

    def name = column[String]("name")

    def submitter = column[PhoneNumber]("submitter")


    def * = (pictureId, name, submitter) <>(Picture.tupled, Picture.unapply)


  }

  val pictures = TableQuery[Pictures]

}
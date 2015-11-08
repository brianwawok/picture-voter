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
import scala.concurrent.Future


trait DatabaseService extends DatabaseSupport {

  val db: Database

  private[this] val logger = LoggerFactory.getLogger(this.getClass)


  import Pictures.pictures
  import Votes.votes


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


  /*
  this is kind of absurd, go slick

  select p.name, count(*)
  from votes v, pictures p
  where v.picture_id = p.picture_id
  and v.target_phone = ?
  and p.target_phone = ?
  order by count(*) desc
*/
  def getAllVoteCounts(targetPhone: PhoneNumber): Future[Seq[(String, Int)]] = {
    db.run(
      (for {
        v <- votes.filter(_.targetPhone === targetPhone)
        p <- pictures.filter(_.targetPhone === targetPhone) if v.pictureId === p.pictureId
      } yield (v, p))
        .groupBy {
          case (v, p) => p.name
        }.map {
        case ((name), list) =>
          (name, list.length)
      }.sortBy(_._2.desc)
        .result
    )
  }


  //save new picture, return true if success or false if fail whale
  def saveNewPicture(picture: Picture): Future[Boolean] = {

    val cleanPicture = picture.copy(pictureId = 0)
    db.run(
      pictures += cleanPicture
    ).map(res => res > 0)
      .recover {
        //TODO maybe tease out this was a dupe FK exception and not something else
        case ex: java.sql.SQLException =>
          logger.warn("Save new picture exception", ex)
          false
      }
  }

  def findPictureIdForNameAndTarget(name: String, targetPhoneNumber: PhoneNumber): Future[Option[Long]] = {
    db.run(
      pictures
        .filter(_.name === name)
        .filter(_.targetPhone === targetPhoneNumber)
        .map(_.pictureId)
        .take(1)
        .result
        .headOption
    )
  }

  //insert a vote, return true if it was a success or false otherwise
  def voteForPicture(pictureId : Long, submitterPhone : PhoneNumber, targetPhone: PhoneNumber) : Future[Boolean] = {
    val vote = Vote(targetPhone, submitterPhone, pictureId, LocalDateTime.now())
    db.run(
      votes += vote
    ).map(_ > 0)
      .recover {
        //TODO maybe tease out this was a dupe FK exception and not something else
        case ex: java.sql.SQLException =>
          logger.warn("Vote for picture exception", ex)
          false
      }
  }



}


object Votes extends DatabaseSupport {

  class Votes(tag: Tag) extends Table[Vote](tag, "votes") {

    def targetPhone = column[PhoneNumber]("target_phone")

    def submitterPhone = column[PhoneNumber]("submitter_phone")

    def pictureId = column[Long]("picture_id")

    def voteTime = column[LocalDateTime]("vote_time")

    def pk = primaryKey("vote_pk", (targetPhone, submitterPhone, pictureId))

    def * = (targetPhone, submitterPhone, pictureId, voteTime) <>(Vote.tupled, Vote.unapply)

    def fk = foreignKey("picture_fk", pictureId, Pictures.pictures)(_.pictureId)

  }

  val votes = TableQuery[Votes]

}


object Pictures extends DatabaseSupport {

  class Pictures(tag: Tag) extends Table[Picture](tag, "pictures") {

    def pictureId = column[Long]("picture_id", O.PrimaryKey, O.AutoInc)

    def name = column[String]("name")

    def submitterPhone = column[PhoneNumber]("submitter_phone")

    def targetPhone = column[PhoneNumber]("target_phone")

    def uniqueNameForTarget = index("unique_name_for_target", (name, targetPhone), unique = true)


    def * = (pictureId, name, submitterPhone, targetPhone) <>(Picture.tupled, Picture.unapply)


  }

  val pictures = TableQuery[Pictures]

}
/*
* Copyright 2010-2011 WorldWide Conferencing, LLC
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/


package net.liftmodules
package restrecord

import net.liftweb.common._
import net.liftweb.json.JsonAST._
import net.liftweb.record.{MetaRecord, Record}
import net.liftweb.util.Props 

import dispatch._
import dispatch.oauth._
import com.ning.http.client.{RequestBuilder}

object RestRecordConfig {
  var host = "localhost"
  var context: Box[String] = Empty
  
  var ssl = false
  var oauth = false
  val requestToken = Props.get("twitter.oauthRequestToken")
  val tokenSecret =	Props.get("twitter.oauthTokenSecret")
  val consumerKey = Props.get("twitter.oauthConsumerKey")
  val consumerSecret = Props.get("twitter.oauthConsumerSecret") 

  def req = { 
    val _req = :/(host + (context.map("/" + _) openOr ""))
    if (ssl) _req.secure else _req
  }

  def webservice = new WebService(req)
}

trait RestRecord[MyType <: RestRecord[MyType]] extends JSONRecord[MyType] {

  self: MyType =>
  
  /** 
   *  Refine meta to require a RestMetaRecord 
   */
  def meta: RestMetaRecord[MyType]
  
  /** 
   *  Defines the RESTful endpoint for this resource 
   *  Examples: /foo or /foo/bar 
   */
  val uri: List[String]
  
  /** 
   *  Defines the RESTful id for this resource
   *  Empty implies this endoint does not use and id
   *  Used on Saves and Deletes 
   */
  def idPk: Box[Any] = Empty
 
  /** 
   *  Defines the RESTful suffix after id 
   *  Example: /uri/:id/suffix
   */
  val suffix: List[String] = Nil
  
  def uri(id: Any): List[String] = uri ::: List(id.toString) ::: suffix
  
  def create: Promise[Box[JValue]] = meta.create(this)

  def save: Promise[Box[JValue]] = meta.save(this)
  
  def delete: Promise[Box[JValue]] = meta.delete(this)

  def findEndpoint(id: Any) = uri(id)
  
  def findEndpoint = uri 

  def createEndpoint = uri // we should never have an id on creation

  private def _discoverEndpoint = idPk.map(uri(_)) openOr uri
  
  def saveEndpoint = _discoverEndpoint 

  def deleteEndpoint = _discoverEndpoint 

  // override this if you want to change this record's specific webservice
  def myWebservice: Box[WebService] = Empty

  def _discoverWebservice = myWebservice openOr RestRecordConfig.webservice

  def webservice = _discoverWebservice 
}

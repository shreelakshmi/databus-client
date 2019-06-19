package org.dbpedia.databus

import java.net.URL

import better.files.File
import org.apache.commons.io.FileUtils
import org.apache.jena.query._
import org.apache.jena.rdf.model.Model
import org.apache.jena.riot.{RDFDataMgr, RDFLanguages}
import org.dbpedia.databus.sparql.DataIdQueries

object QueryHandler {

  def executeSelectQuery(queryString:String) = {
    var query: Query = QueryFactory.create(queryString)
    var qexec: QueryExecution = QueryExecutionFactory.sparqlService("http://databus.dbpedia.org/repo/sparql", query)


    try {
      var results: ResultSet = qexec.execSelect
      var fileHandler = FileHandler

      while (results.hasNext()) {
        var resource = results.next().getResource("?file")
        fileHandler.downloadFile(resource.toString())
      }
    } finally qexec.close()
  }

  def getDataIdFile(url:String, dataIdFile: File) ={
    var queryString=s"""PREFIX dataid: <http://dataid.dbpedia.org/ns/core#>
                    PREFIX dcat: <http://www.w3.org/ns/dcat#>
                    SELECT DISTINCT ?dataset WHERE {
                    ?dataset dataid:version ?version .
                    ?dataset dcat:distribution ?distribution .
                    ?distribution dcat:downloadURL <$url> }"""

    var query: Query = QueryFactory.create(queryString)
    var qexec: QueryExecution = QueryExecutionFactory.sparqlService("http://databus.dbpedia.org/repo/sparql", query)


    try {
      var results: ResultSet = qexec.execSelect
      var fileHandler = FileHandler

      if (results.hasNext()) {
        var dataidURL = results.next().getResource("?dataset").toString()
        println(dataidURL)
        FileUtils.copyURLToFile(new URL(dataidURL),dataIdFile.toJava)
      }
    } finally qexec.close()

  }

  def executeDataIdQuery(dataIdFile:File):List[String] ={

    val dataidModel: Model = RDFDataMgr.loadModel(dataIdFile.pathAsString,RDFLanguages.NTRIPLES)

    //dir_structure : publisher/group/artifact/version
    var dir_structure = List[String]()

    var query: Query = QueryFactory.create(DataIdQueries.queryGetPublisher())
    var qexec = QueryExecutionFactory.create(query,dataidModel)

    try {
      val results = qexec.execSelect
      if (results.hasNext()) {
        //split the URI at the slashes and take the last cell
        var publisher = results.next().getResource("?o").toString().split("/").map(_.trim).last
        dir_structure = dir_structure :+ publisher
      }
    } finally qexec.close()

    query = QueryFactory.create(DataIdQueries.queryGetGroup())
    qexec = QueryExecutionFactory.create(query,dataidModel)

    try {
      val results = qexec.execSelect
      if (results.hasNext()) {
        var group = results.next().getResource("?o").toString().split("/").map(_.trim).last
        dir_structure = dir_structure :+ group
      }
    } finally qexec.close()

    query = QueryFactory.create(DataIdQueries.queryGetArtifact())
    qexec = QueryExecutionFactory.create(query,dataidModel)

    try {
      val results = qexec.execSelect
      if (results.hasNext()) {
        var artifact = results.next().getResource("?o").toString().split("/").map(_.trim).last
        dir_structure = dir_structure :+ artifact
      }
    } finally qexec.close()

    query = QueryFactory.create(DataIdQueries.queryGetVersion())
    qexec = QueryExecutionFactory.create(query,dataidModel)

    try {
      val results = qexec.execSelect
      if (results.hasNext()) {
        var version = results.next().getResource("?o").toString().split("/").map(_.trim).last
        dir_structure = dir_structure :+ version
      }
    } finally qexec.close()

    return dir_structure
  }



}

package org.dbpedia.databus.rdf_reader

import java.util.concurrent.{ExecutorService, Executors}

import better.files.File
import org.apache.jena.graph.Triple
import org.apache.jena.riot.RDFDataMgr
import org.apache.jena.riot.lang.{PipedRDFIterator, PipedRDFStream, PipedTriplesStream}
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.SparkSession

object RDF_Reader {

  def readRDF(spark: SparkSession, inputFile:File): RDD[Triple] = {

    val sc = spark.sparkContext
    val statements = RDFDataMgr.loadModel(inputFile.pathAsString).listStatements()
    var data = sc.emptyRDD[Triple]

    while (statements.hasNext()){
      val triple =statements.nextStatement().asTriple()
      val dataTriple = sc.parallelize(Seq(triple))
      data = sc.union(data, dataTriple)
    }

    return data
  }

}
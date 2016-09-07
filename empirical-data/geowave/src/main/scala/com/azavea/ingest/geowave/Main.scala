package com.azavea.ingest.geowave

//import com.typesafe.scalalogging.Logger
import mil.nga.giat.geowave.adapter.vector._
import mil.nga.giat.geowave.core.geotime.ingest._
import mil.nga.giat.geowave.core.{store => geowave}
import mil.nga.giat.geowave.core.store.index._
import mil.nga.giat.geowave.datastore.accumulo._
import mil.nga.giat.geowave.datastore.accumulo.index.secondary._
import mil.nga.giat.geowave.datastore.accumulo.metadata._
import mil.nga.giat.geowave.datastore.accumulo.operations.config.AccumuloOptions
import org.apache.spark.{SparkConf, SparkContext}
import org.geotools.data.{DataStoreFinder, FeatureSource}
import org.geotools.feature.FeatureCollection
import org.opengis.filter.Filter
import org.opengis.feature.simple.{SimpleFeature, SimpleFeatureType}
import org.geotools.data.simple.SimpleFeatureStore
import org.apache.spark.rdd._

import java.util.HashMap
import scala.collection.JavaConversions._
import scala.util.Try

import com.azavea.ingest.common._

object Main {
  def main(args: Array[String]) = {
    val params = CommandLine.parser.parse(args, Ingest.Params()) match {
      case Some(p) => p
      case None => {
        java.lang.System.exit(0)
        Ingest.Params()
      }
    }

    // Setup Spark environment
    val sparkConf = (new SparkConf).setAppName("GeoWave ingest")
    implicit val sc = new SparkContext(sparkConf)
    println("SparkContext created!")


    params.csvOrShp match {
      case Ingest.SHP => {
        val urls = HydrateRDD.getShpUrls(params.s3bucket, params.s3prefix)
      }
      case Ingest.CSV => {
        val urls = HydrateRDD.getCsvUrls(params.s3bucket, params.s3prefix, params.csvExtension)
        val csvRdd: RDD[SimpleFeature] = HydrateRDD.csvUrls2Rdd(urls, params.featureName, params.codec, params.dropLines, params.separator)

        Ingest.ingestRDD(params)(csvRdd, params.codec, params.featureName)
      }
    }

    //val urlRdd = sc.parallelize(params.urlList)

    //// Load shapefiles
    //urlRdd.foreach (Ingest.ingestShapefileFromURL(params))
  }
}
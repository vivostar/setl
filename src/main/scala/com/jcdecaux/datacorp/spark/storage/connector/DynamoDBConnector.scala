package com.jcdecaux.datacorp.spark.storage.connector

import com.jcdecaux.datacorp.spark.annotation.InterfaceStability
import com.jcdecaux.datacorp.spark.config.Conf
import com.jcdecaux.datacorp.spark.enums.Storage
import com.jcdecaux.datacorp.spark.util.TypesafeConfigUtils
import com.typesafe.config.Config
import org.apache.spark.sql._


/**
  * DynamoDB connector.
  *
  * {{{
  *   # Configuration
  *   dynamodb {
  *     region = ""
  *     table = ""
  *     saveMode = ""
  *   }
  * }}}
  *
  * @param spark      spark session
  * @param region     region of AWS
  * @param table      table name
  * @param saveMode   save mode
  * @param throughput the desired read/write throughput to use
  */
@InterfaceStability.Evolving
class DynamoDBConnector(val spark: SparkSession,
                        val region: String, // "eu-west-1"
                        val table: String,
                        val saveMode: SaveMode,
                        val throughput: String = "10000"
                       ) extends DBConnector {

  import com.audienceproject.spark.dynamodb.implicits._

  override val reader: DataFrameReader = {
    log.debug(s"DynamoDB connector read throughput $throughput")
    spark.read
      .option("region", region)
      .option("throughput", throughput)
  }

  override var writer: DataFrameWriter[Row] = _

  def this(spark: SparkSession, config: Config) = this(
    spark = spark,
    region = TypesafeConfigUtils.getAs[String](config, "region").get,
    table = TypesafeConfigUtils.getAs[String](config, "table").get,
    saveMode = SaveMode.valueOf(TypesafeConfigUtils.getAs[String](config, "saveMode").get)
  )

  def this(spark: SparkSession, conf: Conf) = this(
    spark = spark,
    region = conf.get("region").get,
    table = conf.get("table").get,
    saveMode = SaveMode.valueOf(conf.get("region").get)
  )

  @inline private[this] def initWriter(df: DataFrame): Unit = {
    if (df.hashCode() != lastWriteHashCode) {
      log.debug(s"DynamoDB connector write throughput $throughput")
      writer = df.write
        .mode(saveMode)
        .option("region", region)
        .option("throughput", throughput)

      lastWriteHashCode = df.hashCode()
    }
  }

  override val storage: Storage = Storage.DYNAMODB

  private[this] def writeDynamoDB(df: DataFrame, tableName: String): Unit = {
    initWriter(df)
    writer.dynamodb(tableName)
  }

  override def read(): DataFrame = {
    log.debug(s"Reading DynamoDB table $table in $region")
    reader.dynamodb(table)
  }

  override def write(t: DataFrame, suffix: Option[String]): Unit = {
    log.warn("Suffix will be ignore in DynamoDBConnector")
    write(t)
  }

  override def create(t: DataFrame, suffix: Option[String]): Unit = {
    log.warn("Create is not supported in DynamoDBConnector")
  }

  override def delete(query: String): Unit = {
    log.warn("Delete is not supported in DynamoDBConnector")
  }

  override def create(t: DataFrame): Unit = {
    log.warn("Create is not supported in DynamoDBConnector")
  }

  override def write(t: DataFrame): Unit = {
    writeDynamoDB(t, table)
  }
}
package com.jcdecaux.datacorp.spark.storage.v2.connector

import java.io.File

import com.jcdecaux.datacorp.spark.storage.SparkRepositorySuite
import com.jcdecaux.datacorp.spark.{SparkSessionBuilder, TestObject}
import org.apache.spark.sql.{Dataset, SaveMode, SparkSession}
import org.scalatest.FunSuite

class ParquetConnectorSuite extends FunSuite {

  import SparkRepositorySuite.deleteRecursively

  val spark: SparkSession = new SparkSessionBuilder().setEnv("dev").build().get()
  val path: String = "src/test/resources/test_parquet"
  val table: String = "test_table"

  val parquetConnector = new ParquetConnector(spark, path, table, SaveMode.Overwrite)


  test("IO") {

    import spark.implicits._
    val testTable: Dataset[TestObject] = Seq(
      TestObject(1, "p1", "c1", 1L),
      TestObject(2, "p2", "c2", 2L),
      TestObject(3, "p3", "c3", 3L)
    ).toDS()

    testTable.toDF.show()
    parquetConnector.write(testTable.toDF())
    parquetConnector.write(testTable.toDF())

    val df = parquetConnector.read()
    df.show()
    assert(df.count() === 3)
    deleteRecursively(new File(path))

  }
}

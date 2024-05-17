package io.h8.stages

import org.apache.spark.sql.{DataFrame, SparkSession}

package object spark {
  type Extract = Stage[SparkSession, DataFrame]
  type Transform = Stage[DataFrame, DataFrame]
  type Load = Stage[DataFrame, Unit]

  type ETL = Stage[SparkSession, Unit]
}

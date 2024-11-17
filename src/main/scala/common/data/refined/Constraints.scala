package es.eriktorr
package common.data.refined

import io.github.iltotore.iron.DescribedAs
import io.github.iltotore.iron.constraint.any.Not
import io.github.iltotore.iron.constraint.string.{Blank, StartWith}

object Constraints:
  type JdbcUrl = DescribedAs[StartWith["jdbc:"], "Should start with jdbc:"]

  type NonEmptyString =
    DescribedAs[Not[Blank], "Should contain at least one non-whitespace character"]

package scalan.linalgebra

/**
  * Created by Victor Smirnov on 3/12/15.
  */

import scalan._
import scalan.common.OverloadHack.Overloaded1
import scalan.common.Default
import scalan.linalgebra.Math

trait Matrices extends Vectors with Math { self: ScalanCommunityDsl =>

  type Matrix[T] = Rep[AbstractMatrix[T]]

  trait AbstractMatrix[T] extends Reifiable[AbstractMatrix[T]] {
    def numColumns: Rep[Int]
    def numRows: Rep[Int]
    implicit def elem: Elem[T]
    def rows: Rep[Collection[AbstractVector[T]]]
    def columns: Rep[Collection[AbstractVector[T]]]
    def rmValues: Rep[Collection[T]]

    @OverloadId("row")
    def apply(row: Rep[Int]): Vector[T]
    def apply(row: Rep[Int], column: Rep[Int]): Rep[T]

    def transpose: Matrix[T]

    //@OverloadId("vector")
    def *(vector: Vector[T])(implicit n: Numeric[T]): Vector[T] =
      DenseVector(rows.map { r => r.dot(vector) })
    @OverloadId("matrix")
    def *(mat: Rep[AbstractMatrix[T]])(implicit n: Numeric[T], o: Overloaded1): Rep[AbstractMatrix[T]] = {
      val resColumns = mat.columns.map { col: Rep[AbstractVector[T]] => this * col }
      companion.fromColumns(resColumns)
    }
    def companion: Rep[AbstractMatrixCompanion]
  }

  /*abstract class RowMajorMatrix[T](val rows: Rep[Collection[AbstractVector[T]]])(implicit val elem: Elem[T]) extends Matrix[T] {
    def companion = RowMajorMatrix
    def numRows: Rep[Int] = rows.length
    def numColumns = rows(0).length
    def columns =
      PArray(SArray.tabulate(numColumns) { j => DenseVector(rows.map(_(j))) })
    def rmValues = ???
  }

  trait RowMajorMatrixCompanion extends ConcreteClass1[RowMajorMatrix] with MatrixCompanion {
    override def defaultOf[T: Elem] = Default.defaultVal(RowMajorMatrix(element[PArray[DenseAbstractVector[T]]].defaultRepValue))
    override def fromColumns[T: Elem](cols: PA[AbstractVector[T]]): Matr[T] =
      RowMajorMatrix(PArray(SArray.tabulate(cols(0).length) { i => DenseVector(cols.map(_(i))) }))
  }*/

  abstract class RowMajorFlatMatrix[T](val rmValues: Rep[Collection[T]], val numColumns: Rep[Int])
                                      (implicit val elem: Elem[T])
    extends AbstractMatrix[T] {

    def items = rmValues
    //def companion = RowMajorFlatMatrix
    def numRows: Rep[Int] = rmValues.length /! numColumns
    def columns: Rep[Collection[AbstractVector[T]]] = {
      Collection.indexRange(numColumns).map { i =>
        DenseVector(Collection(rmValues.arr.stride(i, numRows, numColumns)))}
    }
    def rows: Coll[DenseVector[T]] = Collection(rmValues.arr.grouped(numColumns).map { row => DenseVector(Collection(row)) })

    @OverloadId("row")
    def apply(row: Rep[Int]): Vector[T] = DenseVector(rmValues.slice(row * numColumns, numColumns))
    def apply(row: Rep[Int], column: Rep[Int]): Rep[T] = items(toCellIndex(row, column))

    def fromCellIndex(iCell: Rep[Int]): Rep[(Int, Int)] = Pair(iCell /! numColumns, iCell % numColumns)
    def toCellIndex(iRow: Rep[Int], iCol: Rep[Int]): Rep[Int] = numColumns * iRow + iCol

    def blockCellIndices(top: Rep[Int], left: Rep[Int], height: Rep[Int], width: Rep[Int]): Coll[Int] = {
      for {
        i <- Collection.indexRange(height)
        j <- Collection.indexRange(width)
      } yield {
        toCellIndex(top + i, left + j)
      }
    }

    def transposeIndices(is: Coll[Int]): Coll[Int] = {
      for { i <- is } yield {
        val Pair(iRow, iCol) = fromCellIndex(i)
        val transM = RowMajorFlatMatrix(items, numRows)
        transM.toCellIndex(iCol, iRow)
      }
    }

    def getTranspositionOfBlocks(blocks: Coll[((Int, Int), (Int, Int))]): PairColl[Int, Int] = {
      val res = for { Pair(Pair(top, left), Pair(height, width)) <- blocks } yield {
        val bcis = blockCellIndices(top, left, height, width)
        val trans = transposeIndices(bcis)
        bcis zip trans
      }
      res.flatMap(c => c)
    }

    @OverloadId("block_size")
    def transpose(blockSize: Rep[Int]): Matrix[T] = transposeNested(this, blockSize)/*{
      val n = (numRows - 1) /! blockSize + 1
      val m = (numColumns - 1) /! blockSize + 1
      val bHeight = numRows % blockSize
      val rWidth = numColumns % blockSize
      val lastHeight = IF(bHeight === 0) THEN blockSize ELSE bHeight
      val lastWidth = IF(rWidth === 0) THEN blockSize ELSE rWidth

      val blocks = for {
        i <- Collection.indexRange(n)
        j <- Collection.indexRange(m)
      } yield {
        val height: Rep[Int] = IF(i === n - 1) THEN lastHeight ELSE blockSize
        val width = IF(j === m - 1) THEN lastWidth ELSE blockSize
        Pair(Pair(i * blockSize, j * blockSize), Pair(height, width))
      }

      val is = getTranspositionOfBlocks(blocks)
      val transposedItems = items.updateMany(is.bs, items gather is.as)

      RowMajorFlatMatrix(transposedItems, numRows)
    }*/
    def transpose: Matrix[T] = transpose(10)
  }

  abstract class RowMajorSparseMatrix[T](val rows: Rep[Collection[AbstractVector[T]]], val numColumns: Rep[Int])
                                        (implicit val elem: Elem[T])
    extends AbstractMatrix[T] {
    //def companion = RowMajorSparseMatrix
    def columns = ???
    def numRows = rows.length
    //def numColumns = rows(0).length
    def rmValues = ???

    @OverloadId("row")
    def apply(row: Rep[Int]): Vector[T] = rows(row)
    def apply(row: Rep[Int], column: Rep[Int]): Rep[T] = rows(row)(column)

    def transpose: Rep[AbstractMatrix[T]] = transposeJagged(this) /*{
      val nestedItems = sparseRows map {row => row.nonZeroItems}
      val newNestedItems = CompressedRowMatrix.transpose(nestedItems, numColumns)
      val newSparseRows = newNestedItems map { nonZeroItems => SparseVector(nonZeroItems, numRows)}
      CompressedRowMatrix(newSparseRows, numRows)
    }*/
  }

  trait AbstractMatrixCompanion extends TypeFamily1[AbstractMatrix] {

    def defaultOf[T: Elem]: Default[Rep[AbstractMatrix[T]]] =
      RowMajorFlatMatrix.defaultOf[T]
    def fromColumns[T: Elem](cols: Rep[Collection[AbstractVector[T]]]): Rep[AbstractMatrix[T]] =
      RowMajorFlatMatrix.fromColumns(cols)
    def apply[A: Elem](items: Coll[A], numColumns: Rep[Int])(implicit o: Overloaded1): Matrix[A] = RowMajorFlatMatrix(items, numColumns)
  }

  trait RowMajorFlatMatrixCompanion extends ConcreteClass1[RowMajorFlatMatrix] with AbstractMatrixCompanion {

    override def defaultOf[T: Elem] = Default.defaultVal(RowMajorFlatMatrix(element[Collection[T]].defaultRepValue, IntElement.defaultRepValue))
    override def fromColumns[T: Elem](cols: Coll[AbstractVector[T]]): Matrix[T] = {
      val numColumns = cols.length
      val numRows = cols(0).length
      val columnsArr: Coll[Collection[T]] = cols.map(col => col.items)
      val rmValues = Collection.indexRange(numRows * numColumns).map { i =>
        columnsArr(i % numColumns)(i /! numColumns)
      }
      RowMajorFlatMatrix(rmValues, numColumns)
    }
  }

  trait RowMajorSparseMatrixCompanion extends ConcreteClass1[RowMajorSparseMatrix] with AbstractMatrixCompanion {

    override def defaultOf[T: Elem] = Default.defaultVal(RowMajorSparseMatrix(element[Collection[AbstractVector[T]]].defaultRepValue, IntElement.defaultRepValue))
                                    //Default.defaultVal(SparseVector(element[Collection[Int]].defaultRepValue, element[Collection[T]].defaultRepValue, IntElement.defaultRepValue))
    override def fromColumns[T: Elem](cols: Coll[AbstractVector[T]]): Matrix[T] = ???
  }

  //implicit def matrixElem[T](implicit e: Elem[T]): Elem[Matrix[T]] = element[RowMajorFlatMatrix[T]].asInstanceOf[Elem[AbstractMatrix[T]]]
  implicit def matrixElem[T](implicit e: Elem[T]): Elem[AbstractMatrix[T]] = element[RowMajorFlatMatrix[T]].asElem[AbstractMatrix[T]]
}

trait MatricesDsl extends impl.MatricesAbs with VectorsDsl { self: ScalanCommunityDsl => }

trait MatricesDslSeq extends impl.MatricesSeq with VectorsDslSeq { self: ScalanCommunityDslSeq => }

trait MatricesDslExp extends impl.MatricesExp with VectorsDslExp { self: ScalanCommunityDslExp => }

package lms.transformation.tensor

import scala.annotation.implicitNotFound
import scala.collection._
import scala.collection.mutable.ListBuffer

import lms.core._
import lms.core.stub._
import lms.collection.mutable._
import lms.macros.SourceContext
import lms.thirdparty.{RandomDataTypeLess, NCCLTypeLess, MPIOps, NCCLOps, SIZE_TTypeLess, CUDNNOps,CUDNNTypeLess,CLibTypeLess}
import lms.thirdparty.array_computation.{ArrayCPUTypeLess, CUDATypeLess, CUBLASTypeLess, CudaOps, CudaLibs}
import lms.transformation.util.{DataStructure, CudnnUtils}

import Backend._


trait DistributeTensor2MPI_NCCLMiscs extends DistributeTensor2MPI_NCCLBase with CudnnUtils with CudaLibs {

  import BaseTypeLess._
  import PrimitiveTypeLess._
  import RangeTypeLess._
  import ArrayTypeLess._
  import ArrayCPUTypeLess._
  import FixedSizeDistributedTensorTypeLess._
  import CUDATypeLess._
  import RandomDataTypeLess._
  import NCCLTypeLess._
  import SIZE_TTypeLess._
  import CUBLASTypeLess._
  import CUDNNTypeLess._
  import CLibTypeLess._

  override def transform(n: Node): Backend.Exp = n match {
    case Node(s, "tensor_maskedfill", Backend.Const(tt: TensorType)::Backend.Const(anno:Anno)::(input:Backend.Sym)::(mask:Backend.Sym)::
      Backend.Const(value:Float)::_, _) =>
      implicit val pos = Adapter.oldSourceMap(s)

      val shape = tensor_shape(input, useOldMetadata = true)
      val input_tensor = get_operand(input, anno)
      val mask_tensor = get_operand(mask, anno)

      val size = numeral(shape)
      val output = withComment(s"allocating gpu array of size $size and type Float for the output of maskedfill") {
        gpu_array(size, manifest[Float], myNCCLRank)
      }

      withComment("calling masked fill kernel") {
        cudaMaskedFillWrap[Float](
          Wrap[Array[Float]](input_tensor),
          Wrap[Array[Float]](output.x),
          Wrap[Array[Int]](mask_tensor),
          shape, size, value)
      }
      output.x

    case Node(s, "tensor_maskedfill_bwd", Backend.Const(tt: TensorType)::Backend.Const(anno:Anno)::(doutput:Backend.Sym)::(mask:Backend.Sym)::_, _) =>
      implicit val pos = Adapter.oldSourceMap(s)

      val shape = tensor_shape(doutput, useOldMetadata = true)
      val doutput_tensor = get_operand(doutput, anno)
      val mask_tensor = get_operand(mask, anno)
      val size = numeral(shape)
      val dinput = withComment(s"allocating gpu array of size $size and type Float for the gradient input of masked fill") {
        gpu_array(size, manifest[Float], myNCCLRank)
      }

      withComment("calling masked fill gradient kernel") {
        cudaMaskedFillGradWrap[Float](
          Wrap[Array[Float]](doutput_tensor),
          Wrap[Array[Float]](dinput.x),
          Wrap[Array[Int]](mask_tensor),
          shape, size)
      }
      dinput.x

    case Node(s, "tensor_logsoftmax", Backend.Const(tt: TensorType)::Backend.Const(anno:Anno)::(input:Backend.Sym)::_, _) =>
      implicit val pos = Adapter.oldSourceMap(s)

      val shape = tensor_shape(input, useOldMetadata = true)
      val input_tensor = get_operand(input, anno)

      val size = numeral(shape)
      val output = withComment(s"allocating gpu array of size $size and type Float for the output of logsoftmax") {
        gpu_array(size, manifest[Float], myNCCLRank)
      }

      withComment("calling softmax kernel") {
        cudaLogSoftmaxWrap[Float](
          Wrap[Array[Float]](input_tensor),
          Wrap[Array[Float]](output.x),
          numeral(shape.init),
          shape.last)
      }
      output.x

    case Node(s, "tensor_logsoftmax_bwd", Backend.Const(tt: TensorType)::Backend.Const(anno:Anno)::(output:Backend.Sym)::(doutput:Backend.Sym)::_, _) =>
      implicit val pos = Adapter.oldSourceMap(s)

      val shape = tensor_shape(output, useOldMetadata = true)
      val output_tensor = get_operand(output, anno)
      val doutput_tensor = get_operand(doutput, anno)

      val size = numeral(shape)
      val dinput = withComment(s"allocating gpu array of size $size and type Float for the gradient input of logsoftmax") {
        gpu_array(size, manifest[Float], myNCCLRank)
      }

      withComment("calling softmax gradient kernel") {
        cudaLogSoftmaxGradWrap[Float](
          Wrap[Array[Float]](dinput.x),
          Wrap[Array[Float]](doutput_tensor),
          Wrap[Array[Float]](output_tensor),
          numeral(shape.init), shape.last)
      }
      dinput.x

    case Node(s, "tensor_transpose", Backend.Const(tt:TensorType)::Backend.Const(anno:Anno)::(operand:Backend.Sym)::_, _) =>
      val sourceTensor = new TENSOR(s, useOldMetadata = true)

      implicit val sc_ : SourceContext = sourceTensor.pos
      val m = sourceTensor.et

      val shape = tensor_shape(operand, useOldMetadata = true)
      val input_tensor = get_operand(operand, anno)
      
      val size = numeral(shape)
      val output = withComment(s"allocating gpu array of size $size and type Float for the output of transpose") {
        gpu_array(size, manifest[Float], myNCCLRank)
      }

      withComment("calling transpose kernel") {
        cudaTransposeWrap[Float](
          Wrap[Array[Float]](input_tensor),
          Wrap[Array[Float]](output.x),
          shape)
      }
      output.x

    case Node(s, "tensor_permute", Backend.Const(tt:TensorType)::Backend.Const(anno:Anno)::(operand:Backend.Sym)::Backend.Const(perm:List[Int])::_, _) =>
      val sourceTensor = new TENSOR(s, useOldMetadata = true)

      implicit val sc_ : SourceContext = sourceTensor.pos
      val m = sourceTensor.et

      val shape = tensor_shape(operand, useOldMetadata = true)
      val input_tensor = get_operand(operand, anno)

      val size = numeral(shape)
      val output = withComment(s"allocating gpu array of size $size and type Float for the output of permute") {
        gpu_array(size, manifest[Float], myNCCLRank)
      }

      withComment("calling permute kernel") {
        cudaPermuteWrap[Float](
          Wrap[Array[Float]](input_tensor),
          Wrap[Array[Float]](output.x),
          shape, size, perm)
      }
      output.x

    case Node(s, "tensor_embedding", Backend.Const(tt:TensorType)::Backend.Const(anno:Anno)::(input:Backend.Sym)::(indices:Backend.Sym)::_, _) =>
      val sourceTensor = new TENSOR(s, useOldMetadata = true)

      implicit val sc_ : SourceContext = sourceTensor.pos
      val m = sourceTensor.et
      val input_tensor = get_operand(input, anno)
      val indices_array = get_operand(indices, anno)

      val input_shape = tensor_shape(input, useOldMetadata = true)
      val indices_shape = tensor_shape(indices, useOldMetadata = true)
      val output_shape = tensor_shape(s, useOldMetadata = true)

      val indices_size = indices_shape(0)
      val embed_size = input_shape(1)
      val output_size = numeral(output_shape)

      val output = withComment(s"allocating gpu array of size $output_size and type Float for the output of embedding") {
        gpu_array(output_size, manifest[Float], myNCCLRank)
      }

      withComment("calling embedding kernel") {
        cudaEmbeddingWrap[Float](
          Wrap[Array[Float]](input_tensor),
          Wrap[Array[Float]](output.x),
          Wrap[Array[Int]](indices_array),
          unit[Int](embed_size),
          unit[Int](indices_size))
      }
      output.x

    case Node(s, "tensor_embedding_bwd", Backend.Const(tt:TensorType)::Backend.Const(anno:Anno)::(input:Backend.Sym)::(doutput:Backend.Sym)::(indices:Backend.Sym)::_, _) =>
      val sourceTensor = new TENSOR(s, useOldMetadata = true)

      implicit val sc_ : SourceContext = sourceTensor.pos
      val m = sourceTensor.et
      val doutput_tensor = get_operand(doutput, anno)
      val indices_array = get_operand(indices, anno)

      val doutput_shape = tensor_shape(doutput, useOldMetadata = true)
      val indices_shape = tensor_shape(indices, useOldMetadata = true)
      val dinput_shape = tensor_shape(s, useOldMetadata = true)


      val indices_size = indices_shape(0)
      val embed_size = doutput_shape(1)
      val dinput_size = numeral(dinput_shape)

      val dinput = withComment(s"allocating gpu array of size $dinput_size and type Float for the gradient input of embedding") {
        gpu_fixed_array(dinput_size, myNCCLRank, NUM(Backend.Const(0), m))
      }

      withComment("calling embedding gradient kernel") {
        cudaEmbeddingGradWrap[Float](
          Wrap[Array[Float]](doutput_tensor),
          Wrap[Array[Float]](dinput.x),
          Wrap[Array[Int]](indices_array),
          unit[Int](embed_size),
          unit[Int](indices_size))
      }
      dinput.x

    case _ => super.transform(n)
  }
}

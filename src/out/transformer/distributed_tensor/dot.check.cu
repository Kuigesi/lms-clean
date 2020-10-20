/*****************************************
Emitting C Generated Code
*******************************************/
#include <string.h>
#include <cblas.h>
#include <stdlib.h>
#include <cuda_header.h>
#include <stdio.h>
#include <stdint.h>
#include <stdbool.h>
/**************** Snippet ****************/
void Snippet(int x0) {
  module([&]() {
    float x1 = tensor_weight(TensorType(List(Size(Dim(1),32), Size(Dim(3),32)),Float,NAnno), SAnno(Dim(0),List(GPU(0), GPU(1)),true));
    float x2 = tensor_zeros(TensorType(List(Size(Dim(1),32), Size(Dim(3),32)),Float,NAnno), SAnno(Dim(0),List(GPU(0), GPU(1)),true));
    float x3 = tensor_zeros(TensorType(List(Size(Dim(1),32), Size(Dim(3),32)),Float,NAnno), SAnno(Dim(0),List(GPU(0), GPU(1)),true));
    int x4 = 0;
    while (x4 != 5) {
      float x5 = tensor_input(TensorType(List(Size(Dim(0),32), Size(Dim(1),32)),Float,NAnno), SAnno(Dim(0),List(GPU(0), GPU(1)),true));
      float x6 = tensor_ones(TensorType(List(Size(Dim(0),32), Size(Dim(3),32)),Float,NAnno), SAnno(Dim(0),List(GPU(0), GPU(1)),true));
      float x7 = tensor_transpose(TensorType(List(Size(Dim(1),32), Size(Dim(0),32)),Float,NAnno), SAnno(Dim(0),List(GPU(0), GPU(1)),true), x5);
      float x8 = tensor_dot(TensorType(List(Size(Dim(1),32), Size(Dim(3),32)),Float,NAnno), SAnno(Dim(0),List(GPU(0), GPU(1)),true), x7, x6);
      accum_tensor(SAnno(Dim(0),List(GPU(0), GPU(1)),true), x2, x8);
      optimize_tensor(SAnno(Dim(0),List(GPU(0), GPU(1)),true), x1, x2, x3);
      x4 = x4 + 1;
    }
    save(x1);
  })(());
  printf("compile");
}
/*****************************************
End of C Generated Code
*******************************************/
int main(int argc, char *argv[]) {
  if (argc != 2) {
    printf("usage: %s <arg>\n", argv[0]);
    return 0;
  }
  Snippet(atoi(argv[1]));
  return 0;
}

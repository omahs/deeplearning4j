/* ******************************************************************************
 *
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0.
 *
 *  See the NOTICE file distributed with this work for additional
 *  information regarding copyright ownership.
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 ******************************************************************************/

//
// @author Yurii Shyrma (iuriish@yahoo.com), created on 12.06.2019
//
#include <helpers/ConstantTadHelper.h>
#include <helpers/PointersManager.h>
#include <helpers/ShapeUtils.h>
#include <ops/declarable/helpers/prefix.h>
#include <ops/ops.h>

#include "execution/cuda/LaunchDims.h"


namespace sd {
namespace ops {
namespace helpers {

///////////////////////////////////////////////////////////////////
template <typename T>
static void prefix_(scalar::Ops op, const void* vx, LongType const* xShapeInfo, void* vz, LongType const* zShapeInfo, bool exclusive, bool reverse) {
  //TODO: note: this is the cpu implementation. The cuda implementation had too many edge cases.
  //this will be addressed at a later date.
  const auto x = reinterpret_cast<const T*>(vx);
  auto z = reinterpret_cast<T*>(vz);
  auto length = shape::length(xShapeInfo);

  T prevSum = op == scalar::Add ? (T)0 : (T)1;
  T sum = prevSum;

  if (reverse) {
    if (shape::elementWiseStride(xShapeInfo) == 1 && shape::elementWiseStride(zShapeInfo) == 1 &&
        shape::order(xShapeInfo) == 'c' && shape::order(zShapeInfo) == 'c') {
      for (LongType e = length - 1; e >= 0; --e) {
        sum = op == scalar::Add ? simdOps::Add<T, T, T>::op(sum, x[e]) : simdOps::Multiply<T, T, T>::op(sum, x[e]);
        if (!exclusive) prevSum = sum;

        z[e] = prevSum;

        prevSum = sum;
      }
    } else {
      for (LongType e = length - 1; e >= 0; --e) {
        auto xOffset = shape::getIndexOffset(e, xShapeInfo);
        auto zOffset = shape::getIndexOffset(e, zShapeInfo);
        sum = op == scalar::Add ? simdOps::Add<T, T, T>::op(sum, x[xOffset])
                                : simdOps::Multiply<T, T, T>::op(sum, x[xOffset]);

        if (!exclusive) prevSum = sum;

        z[zOffset] = prevSum;
        prevSum = sum;
      }
    }
  } else {
    if (shape::elementWiseStride(xShapeInfo) == 1 && shape::elementWiseStride(zShapeInfo) == 1 &&
        shape::order(xShapeInfo) == 'c' && shape::order(zShapeInfo) == 'c') {
      for (LongType e = 0; e < length; e++) {
        sum = op == scalar::Add ? simdOps::Add<T, T, T>::op(sum, x[e]) : simdOps::Multiply<T, T, T>::op(sum, x[e]);

        if (!exclusive) prevSum = sum;

        z[e] = prevSum;

        prevSum = sum;
      }
    } else {
      for (LongType e = 0; e < length; e++) {
        auto xOffset = shape::getIndexOffset(e, xShapeInfo);
        auto zOffset = shape::getIndexOffset(e, zShapeInfo);
        sum = op == scalar::Add ? simdOps::Add<T, T, T>::op(sum, x[xOffset])
                                : simdOps::Multiply<T, T, T>::op(sum, x[xOffset]);

        if (!exclusive) prevSum = sum;

        z[zOffset] = prevSum;
        prevSum = sum;
      }
    }
  }
};

template <typename T>
static void prefix_(scalar::Ops op, const NDArray* x, NDArray* z, const std::vector<LongType>& dims, bool exclusive,
                    bool reverse) {
  NDArray::preparePrimaryUse({z}, {x});
  auto xTads = x->allTensorsAlongDimension(dims);
  auto zTads = z->allTensorsAlongDimension(dims);
  auto t = xTads.size();

  for (int e = 0; e < t; e++) {
    auto tx = xTads.at(e);
    auto tz = zTads.at(e);

    prefix_<T>(op, tx->buffer(), tx->shapeInfo(), tz->buffer(), tz->shapeInfo(), exclusive, reverse);
  }

  NDArray::registerPrimaryUse({z}, {x});
};

///////////////////////////////////////////////////////////////////

template <typename T>
static void prefix_(scalar::Ops op, const NDArray* x, NDArray* z, bool exclusive, bool reverse) {
  prefix_<T>(op, x->buffer(), x->shapeInfo(), z->buffer(), z->shapeInfo(), exclusive, reverse);
};

void prefix(LaunchContext* context, scalar::Ops op, const NDArray* x, NDArray* z, bool exclusive, bool reverse) {
  BUILD_SINGLE_SELECTOR(x->dataType(), prefix_, (op, x, z, exclusive, reverse), SD_COMMON_TYPES);
}

void prefix(LaunchContext* context, scalar::Ops op, const NDArray* x, NDArray* z, const std::vector<LongType>& dims,
            bool exclusive, bool reverse) {
  BUILD_SINGLE_SELECTOR(x->dataType(), prefix_, (op, x, z, dims, exclusive, reverse), SD_COMMON_TYPES);
}

BUILD_SINGLE_TEMPLATE(template void prefix_,
                      (scalar::Ops op, const void* vx, sd::LongType const* xShapeInfo, void* vz,
                          sd::LongType const* zShapeInfo, bool exclusive, bool reverse),
                      SD_COMMON_TYPES);
BUILD_SINGLE_TEMPLATE(template void prefix_,
                      (scalar::Ops op, const NDArray* x, NDArray* z, const std::vector<sd::LongType>& dims, bool exclusive,
                          bool reverse),
                      SD_COMMON_TYPES);
BUILD_SINGLE_TEMPLATE(template void prefix_,
                      (scalar::Ops op, const NDArray* x, NDArray* z, bool exclusive, bool reverse), SD_COMMON_TYPES);

}  // namespace helpers
}  // namespace ops
}  // namespace sd

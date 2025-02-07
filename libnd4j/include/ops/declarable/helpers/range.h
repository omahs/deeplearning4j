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
// @author Yurii Shyrma (iuriish@yahoo.com), created on 27.08.2018
//

#ifndef LIBND4J_RANGE_H
#define LIBND4J_RANGE_H
#include <ops/declarable/helpers/helpers.h>

namespace sd {
namespace ops {
namespace helpers {

// be careful: outVector must have c-order and ews = 1 !!!
SD_LIB_HIDDEN void range(LaunchContext* context, const NDArray& start, const NDArray& delta, NDArray& outVector);

}  // namespace helpers
}  // namespace ops
}  // namespace sd

#endif  // LIBND4J_RANGE_H

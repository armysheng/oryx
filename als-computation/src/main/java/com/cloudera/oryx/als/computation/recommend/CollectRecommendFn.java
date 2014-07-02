/*
 * Copyright (c) 2013, Cloudera, Inc. All Rights Reserved.
 *
 * Cloudera, Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"). You may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * This software is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for
 * the specific language governing permissions and limitations under the
 * License.
 */

package com.cloudera.oryx.als.computation.recommend;

import com.cloudera.oryx.als.common.NumericIDValue;
import com.cloudera.oryx.als.common.StringLongMapping;
import com.cloudera.oryx.als.common.TopN;
import com.cloudera.oryx.als.computation.IDMappingState;
import com.cloudera.oryx.common.io.DelimitedDataUtils;
import com.cloudera.oryx.common.settings.ConfigUtils;
import com.cloudera.oryx.computation.common.fn.OryxReduceDoFn;
import com.google.common.base.Preconditions;
import org.apache.crunch.CrunchRuntimeException;
import org.apache.crunch.Emitter;
import org.apache.crunch.Pair;

import java.io.IOException;

public final class CollectRecommendFn extends OryxReduceDoFn<Long, Iterable<NumericIDValue>, String> {

  private int numRecs;
  private IDMappingState idMapping;
  private String idMappingPrefix;

  public CollectRecommendFn() { this(null); }

  public CollectRecommendFn(String idMappingPrefix) {
    this.idMappingPrefix = idMappingPrefix;
  }

  @Override
  public void initialize() {
    super.initialize();
    numRecs = ConfigUtils.getDefaultConfig().getInt("model.recommend.how-many");
    Preconditions.checkArgument(numRecs > 0, "# recommendations must be positive: %s", numRecs);
    try {
      idMapping = idMappingPrefix == null ? new IDMappingState(getConfiguration()) :
          new IDMappingState(idMappingPrefix);
    } catch (IOException e) {
      throw new CrunchRuntimeException(e);
    }
  }

  @Override
  public void process(Pair<Long, Iterable<NumericIDValue>> input, Emitter<String> emitter) {
    StringLongMapping mapping = idMapping.getIDMapping();
    Iterable<NumericIDValue> recs = TopN.selectTopN(input.second().iterator(), numRecs);
    String userID = mapping.toString(input.first());
    for (NumericIDValue rec : recs) {
      emitter.emit(DelimitedDataUtils.encode(',',
                                             userID,
                                             mapping.toString(rec.getID()),
                                             Float.toString(rec.getValue())));
    }
  }

}

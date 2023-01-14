#include "selfdrive/modeld/models/nav.h"

#include <cstdio>
#include <cstring>
#include <math.h>

#include "common/mat.h"
#include "common/modeldata.h"
#include "common/timing.h"


void fill_plan(cereal::NavModelData::Builder &framed, const NavModelOutputPlan &plan) {
  std::array<float, TRAJECTORY_SIZE> pos_x, pos_y;
  std::array<float, TRAJECTORY_SIZE> pos_x_std, pos_y_std;

  for (int i=0; i<TRAJECTORY_SIZE; i++) {
    pos_x[i] = plan.mean[i].x;
    pos_y[i] = plan.mean[i].y;
    pos_x_std[i] = exp(plan.std[i].x);
    pos_y_std[i] = exp(plan.std[i].y);
  }

  auto position = framed.initPosition();
  position.setX(to_kj_array_ptr(pos_x));
  position.setY(to_kj_array_ptr(pos_y));
  position.setXStd(to_kj_array_ptr(pos_x_std));
  position.setYStd(to_kj_array_ptr(pos_y_std));
}

void navmodel_publish(PubMaster &pm, uint32_t frame_id, const NavModelResult &model_res, float execution_time) {
  // make msg
  MessageBuilder msg;
  auto framed = msg.initEvent().initNavModel();
  framed.setFrameId(frame_id);
  framed.setModelExecutionTime(execution_time);
  framed.setDspExecutionTime(model_res.dsp_execution_time);
  framed.setFeatures(to_kj_array_ptr(model_res.features.values));
  framed.setDesirePrediction(to_kj_array_ptr(model_res.desire_pred.values));
  fill_plan(framed, model_res.plan);

  pm.send("navModel", msg);
}

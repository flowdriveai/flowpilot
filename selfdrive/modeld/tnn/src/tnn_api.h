#include <algorithm>
#include <iostream>
#include <string>
#include <vector>
#include <fstream>

#include "tnn/core/tnn.h"

class TNNAPI {
public:

    tnn::DeviceType io_dt = tnn::DeviceType::DEVICE_NAIVE;
    
    tnn::InputShapesMap input_shapes = {};
    tnn::InputShapesMap output_shapes = {};
    std::map<std::string, double> output_elements = {};
    tnn::MatMap output_mats = {};

    std::shared_ptr<tnn::TNN> net;
    std::shared_ptr<tnn::Instance> net_instance;

    tnn::MatConvertParam preprocessing_param;
    tnn::Status status;
    tnn::ModelConfig model_config;
    tnn::NetworkConfig network_config;

    bool init(std::string model_path, tnn::DeviceType device_type, tnn::DeviceType io_device_type=tnn::DeviceType::DEVICE_NAIVE);
    void create_input(std::string name, std::vector<int> shape);
    void create_output(std::string name, std::vector<int> shape);
    void forward(std::map<std::string, void*> input_container);
    float* get_output(std::string name);
    bool check_status();
};

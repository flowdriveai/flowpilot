#include "tnn_api.h"

// Helper functions
std::string loadFile(std::string path) {
    std::ifstream file(path, std::ios::binary);
    if (file.is_open()) {
        file.seekg(0, file.end);
        int size      = file.tellg();
        char* content = new char[size];
        file.seekg(0, file.beg);
        file.read(content, size);
        std::string fileContent;
        fileContent.assign(content, size);
        delete[] content;
        file.close();
        return fileContent;
    } else {
        return "";
    }
}

bool TNNAPI::check_status(){
    if (status != tnn::TNN_OK) {
        LOGE("model init failed %d \n", (int)status);
        return false;
    }
    return true;
}

bool TNNAPI::init(std::string model_path, tnn::DeviceType device_type, tnn::DeviceType io_device_type){
    std::string proto_content = loadFile(model_path + ".tnnproto");
    std::string model_content = loadFile(model_path + ".tnnmodel");

    this->io_dt = io_device_type;
    model_config.params = {proto_content, model_content};
    net = std::make_shared<tnn::TNN>();
    net->Init(model_config);
    
    network_config.device_type = device_type;
    network_config.precision = tnn::Precision::PRECISION_LOW;
    net_instance = net->CreateInst(network_config, status);

    return check_status();
}

void TNNAPI::forward(std::map<std::string, void*> input_container){
    std::shared_ptr<tnn::Mat> mat;
    for(auto it = input_container.begin(); it != input_container.end(); ++it){
        mat = std::make_shared<tnn::Mat>(io_dt, tnn::MatType::NCHW_FLOAT, input_shapes[it->first], it->second);
        net_instance->SetInputMat(mat, preprocessing_param, it->first);
    }
    net_instance->Forward();

    for(auto it = output_shapes.begin(); it != output_shapes.end(); ++it){
        mat = std::make_shared<tnn::Mat>(io_dt, tnn::MatType::NCHW_FLOAT, it->second);
        net_instance->GetOutputMat(mat, preprocessing_param, it->first, io_dt);
        output_mats[it->first] = mat;
    }
}

float* TNNAPI::get_output(std::string name){
    return reinterpret_cast<float*>(output_mats[name]->GetData());
}

void TNNAPI::create_input(std::string name, std::vector<int> shape){
    input_shapes.insert(std::pair<std::string, tnn::DimsVector>(name, shape));
}

void TNNAPI::create_output(std::string name, std::vector<int> shape){
    output_shapes.insert(std::pair<std::string, tnn::DimsVector>(name, shape));
    double elements = 1;
    for(int i:shape)
        elements *= i;
    output_elements[name] = elements;
}

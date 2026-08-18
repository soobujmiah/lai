#include "include/lai/backend.h"
#include "llama_session.h"

namespace lai {
namespace {

class LlamaCpuBackend final : public Backend {
public:
    std::string name() const override { return "cpu"; }
    bool available() const override { return true; }

    std::unique_ptr<BackendSession> open(
        const std::string& model_path,
        int context_size,
        std::string& error
    ) override {
        // 0 GPU layers: all weights stay in system RAM and run on the CPU backend.
        return build_llama_session(model_path, context_size, 0, error);
    }
};

}  // namespace

std::unique_ptr<Backend> create_llama_cpu_backend() {
    return std::make_unique<LlamaCpuBackend>();
}

}  // namespace lai

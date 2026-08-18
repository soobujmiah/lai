#pragma once

#include "include/lai/backend.h"

#include "llama.h"

#include <memory>
#include <mutex>
#include <string>
#include <vector>

namespace lai {

// LlamaSession is fully defined in llama_session.cpp (single TU). Other translation
// units only ever receive sessions from build_llama_session() and pass them around as
// std::unique_ptr<BackendSession>, so callers never need the complete type.

// Idempotent global initialization shared by every llama-based backend: installs the
// LAI-llama log tag, initializes llama.cpp and loads all ggml backends compiled into
// the artifact (CPU always; Vulkan when LAI_HAS_VULKAN). Safe to call repeatedly.
void initialize_llama_once();

// Shared session construction. `gpu_layers` is the number of model layers to offload:
// 0 keeps everything on CPU; a large value (999) offloads every layer to the GPU device
// registered by ggml (the Vulkan backend). Returns nullptr and fills `error` when the
// model cannot be loaded or the context cannot be allocated.
std::unique_ptr<BackendSession> build_llama_session(
    const std::string& model_path,
    int context_size,
    int gpu_layers,
    std::string& error);

}  // namespace lai

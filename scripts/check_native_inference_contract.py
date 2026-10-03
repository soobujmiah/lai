#!/usr/bin/env python3
"""Regression gate for ordering and the known fatal device boundary."""

from pathlib import Path

root = Path(__file__).resolve().parents[1]
sampler = (root / "runtime/llama/src/main/cpp/llama_session.cpp").read_text()
penalty = sampler.index("llama_sampler_chain_add(sampler, llama_sampler_init_penalties(")
nucleus = sampler.index("llama_sampler_chain_add(sampler, llama_sampler_init_top_p(")
temperature = sampler.index("llama_sampler_chain_add(sampler, llama_sampler_init_temp(")
distribution = sampler.index("llama_sampler_chain_add(sampler, llama_sampler_init_dist(")
assert penalty < nucleus < temperature < distribution, "Native sampler filter order regressed"

vulkan = (root / "runtime/llama/src/main/cpp/vulkan_backend.cpp").read_text()
assert vulkan.count("if (known_crashing_vulkan_device())") >= 2, (
    "Known native-crash device must be rejected during discovery and session creation"
)
assert 'std::string(device) == "onyx"' in vulkan
print("Native inference contract OK: sampler order and onyx crash guard")

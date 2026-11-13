#include "VirtualSpoof.h"
#include <sys/system_properties.h>
#include <android/log.h>
#include <atomic>
#include <cstring>
#include <mutex>
#include <string>
#include <unordered_map>
#include "./xdl.h"
#include "Dobby/dobby.h"

#define LOG_TAG "VirtualSpoof"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)

struct SpoofedProp {
    const char *key;
    const char *value;
};

// Keep OS version, SDK, SoC/EGL hardware, and CPU ABI genuine to prevent ART/driver crashes on Android 16+.
// Only mask emulator indicators and release build tags natively; per-user Build identity is synced via setProp.
static const SpoofedProp default_spoofed_props[] = {
    {"ro.build.type", "user"},
    {"ro.build.tags", "release-keys"},
    {"ro.kernel.qemu", "0"},
    {"ro.boot.qemu", "0"},
    {"ro.kernel.android.qemud", ""},
    {nullptr, nullptr}
};

static std::mutex g_prop_mutex;
static std::unordered_map<std::string, std::string> g_dynamic_props;
static std::atomic<bool> g_spoof_active{false};
static std::atomic<bool> g_hook_installed{false};
static int (*orig_system_property_get)(const char *name, char *value) = nullptr;

static int copy_prop_value(char *dst, const char *src) {
    if (!dst || !src) return 0;
    size_t len = strnlen(src, PROP_VALUE_MAX - 1);
    memcpy(dst, src, len);
    dst[len] = '\0';
    return static_cast<int>(len);
}

static int my_system_property_get(const char *name, char *value) {
    if (!name || !value) {
        return 0;
    }

    if (g_spoof_active.load(std::memory_order_acquire)) {
        {
            std::lock_guard<std::mutex> lock(g_prop_mutex);
            auto it = g_dynamic_props.find(name);
            if (it != g_dynamic_props.end()) {
                return copy_prop_value(value, it->second.c_str());
            }
        }

        for (int i = 0; default_spoofed_props[i].key != nullptr; ++i) {
            if (strcmp(name, default_spoofed_props[i].key) == 0) {
                return copy_prop_value(value, default_spoofed_props[i].value);
            }
        }
    }

    if (orig_system_property_get) {
        return orig_system_property_get(name, value);
    }
    value[0] = '\0';
    return 0;
}

void VirtualSpoof::setProp(const char *key, const char *value) {
    if (!key || !value || key[0] == '\0') {
        return;
    }
    std::lock_guard<std::mutex> lock(g_prop_mutex);
    g_dynamic_props[key] = value;
}

void VirtualSpoof::init() {
    g_spoof_active.store(true, std::memory_order_release);

    bool expected = false;
    if (!g_hook_installed.compare_exchange_strong(expected, true)) {
        return;
    }

    void *handle = xdl_open("libc.so", XDL_DEFAULT);
    if (!handle) {
        LOGD("VirtualSpoof: xdl_open failed for libc.so");
        return;
    }
    void *target = xdl_dsym(handle, "__system_property_get", nullptr);
    if (target) {
        if (DobbyHook(target, (void *) my_system_property_get, (void **) &orig_system_property_get) == 0) {
            LOGD("VirtualSpoof: __system_property_get hook installed");
        } else {
            LOGD("VirtualSpoof: __system_property_get hook failed");
        }
    }
    xdl_close(handle);
}

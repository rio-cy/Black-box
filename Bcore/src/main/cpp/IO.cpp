#include "IO.h"
#include "Log.h"

#include <algorithm>
#include <cstring>
#include <mutex>
#include <string>
#include <vector>

jmethodID getAbsolutePathMethodId;

static std::mutex g_rule_mutex;
static std::vector<IO::RelocateInfo> relocate_rule;
static std::vector<std::string> black_rules;

static bool is_prefix_match(const char *path, size_t pathLen, const std::string &rule) {
    const size_t ruleLen = rule.size();
    if (ruleLen == 0 || pathLen < ruleLen) {
        return false;
    }
    if (strncmp(path, rule.c_str(), ruleLen) != 0) {
        return false;
    }
    return path[ruleLen] == '\0' || path[ruleLen] == '/';
}

const char *IO::redirectPath(const char *__path, std::string &storage) {
    if (!__path || __path[0] == '\0') {
        return __path;
    }

    if (strstr(__path, "resource-cache")) {
        ALOGD("Blocking resource-cache path: %s", __path);
        return "/dev/null";
    }

    if (strstr(__path, "@idmap")) {
        ALOGD("Blocking idmap path: %s", __path);
        return "/dev/null";
    }

    if (strstr(__path, "systemui") &&
        (strstr(__path, ".frro") || strstr(__path, "-accent-") ||
         strstr(__path, "-dynamic-") || strstr(__path, "-neutral-"))) {
        ALOGD("Blocking systemui problematic path: %s", __path);
        return "/dev/null";
    }

    if (strstr(__path, "data@resource-cache@")) {
        ALOGD("Blocking data@resource-cache@ pattern: %s", __path);
        return "/dev/null";
    }

    if (strstr(__path, ".frro")) {
        ALOGD("Blocking .frro file: %s", __path);
        return "/dev/null";
    }

    if (strstr(__path, "systemui")) {
        ALOGD("Blocking systemui path: %s", __path);
        return "/dev/null";
    }

    if (strstr(__path, "/blackbox/") != nullptr) {
        return __path;
    }

    const size_t pathLen = strlen(__path);
    std::lock_guard<std::mutex> lock(g_rule_mutex);

    for (const auto &brule : black_rules) {
        if (is_prefix_match(__path, pathLen, brule)) {
            return __path;
        }
    }

    for (const auto &info : relocate_rule) {
        if (is_prefix_match(__path, pathLen, info.targetPath)) {
            const char *suffix = __path + info.targetPath.size();
            if (!info.relocatePath.empty() && info.relocatePath.back() == '/' && suffix[0] == '/') {
                suffix++;
            }
            storage.clear();
            storage.reserve(info.relocatePath.size() + strlen(suffix) + 1);
            storage.append(info.relocatePath);
            storage.append(suffix);
            return storage.c_str();
        }
    }

    return __path;
}

jstring IO::redirectPath(JNIEnv *env, jstring path) {
    return BoxCore::redirectPathString(env, path);
}

jobject IO::redirectPath(JNIEnv *env, jobject path) {
    return BoxCore::redirectPathFile(env, path);
}

void IO::addRule(const char *targetPath, const char *relocatePath) {
    if (!targetPath || !relocatePath || targetPath[0] == '\0' || relocatePath[0] == '\0') {
        return;
    }
    std::lock_guard<std::mutex> lock(g_rule_mutex);
    for (auto &existing : relocate_rule) {
        if (existing.targetPath == targetPath) {
            existing.relocatePath = relocatePath;
            return;
        }
    }
    IO::RelocateInfo info{std::string(targetPath), std::string(relocatePath)};
    relocate_rule.push_back(std::move(info));
    std::sort(relocate_rule.begin(), relocate_rule.end(),
              [](const IO::RelocateInfo &a, const IO::RelocateInfo &b) {
                  return a.targetPath.size() > b.targetPath.size();
              });
}

void IO::addBlackRule(const char *path) {
    if (!path || path[0] == '\0') {
        return;
    }
    std::lock_guard<std::mutex> lock(g_rule_mutex);
    for (const auto &existing : black_rules) {
        if (existing == path) {
            return;
        }
    }
    black_rules.emplace_back(path);
}

void IO::init(JNIEnv *env) {
    jclass tmpFile = env->FindClass("java/io/File");
    if (tmpFile) {
        getAbsolutePathMethodId = env->GetMethodID(tmpFile, "getAbsolutePath", "()Ljava/lang/String;");
        env->DeleteLocalRef(tmpFile);
    }
}

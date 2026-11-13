#ifndef VIRTUALM_IO_H
#define VIRTUALM_IO_H

#include <jni.h>
#include <string>
#include "BoxCore.h"

class IO {
public:
    static void init(JNIEnv *env);

    struct RelocateInfo {
        std::string targetPath;
        std::string relocatePath;
    };

    static void addRule(const char *targetPath, const char *relocatePath);

    static void addBlackRule(const char *path);

    static jstring redirectPath(JNIEnv *env, jstring path);

    static jobject redirectPath(JNIEnv *env, jobject path);

    static const char *redirectPath(const char *__path, std::string &storage);
};

#endif

#include <android/log.h>
#include <unistd.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <stdio.h>
#include <string.h>
#include <errno.h>
#include <dirent.h>
#include <stdarg.h>
#include "Dobby/dobby.h"
#include "xdl.h"

#define LOG_TAG "AntiDetection"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)

static const char* blocked_files[] = {
    // Root & Superuser
    "/system/xbin/su",
    "/system/bin/su",
    "/sbin/su",
    "/system/app/Superuser.apk",
    "/system/app/SuperSU.apk",
    "/system/etc/init.d/99SuperSUDaemon",
    "/system/xbin/daemonsu",
    "/system/xbin/sugote",
    "/system/bin/sugote-mksh",
    "/system/xbin/sugote-mksh",
    "/data/local/xbin/su",
    "/data/local/bin/su",
    "/data/local/tmp/su",
    "/system/bin/magisk",
    "/system/xbin/magisk",
    "/sbin/magisk",
    "/data/adb/magisk",
    
    // Virtualization & Sandbox artifacts
    "/data/virtual",
    "/data/data/com.benny.openlauncher",
    "/data/data/io.va.exposed",
    "/data/data/com.lody.virtual",
    "/data/data/com.excelliance.dualaid",
    "/data/data/com.lbe.parallel",
    "/data/data/com.dual.dualspace",
    "/data/data/com.ludashi.superboost",
    "/data/data/top.niunaijun.blackboxa",
    "/blackbox",
    "/virtual",
    
    // Emulator artifacts
    "/dev/vboxguest",
    "/dev/vboxuser",
    "/dev/qemu_pipe",
    "/dev/goldfish_pipe",
    "/dev/socket/qemud",
    "/dev/socket/baseband_genyd",
    "/dev/socket/genyd",
    "/system/lib/libc_malloc_debug_qemu.so",
    "/sys/qemu_trace",
    "/system/bin/qemu-props",
    "/system/bin/nox-prop",
    "/sys/module/goldfish_audio",
    "/sys/module/goldfish_sync",
    "/proc/tty/drivers/goldfish",
    "/dev/goldfish_events",
    "/system/lib/libdroid4x.so",
    "/system/bin/windroyed",
    "/system/lib/libnoxspeedup.so",
    "/system/lib/libmemu.so",
    "/system/lib/libbluelog.so",
    
    // Hooking Frameworks
    "/system/xposed.prop",
    "/system/framework/XposedBridge.jar",
    "/data/data/de.robv.android.xposed.installer",
    "/data/data/org.meowcat.edxposed.manager",
    "/data/data/top.canyie.dreamland.manager",
    
    nullptr
};

static const char* blocked_packages[] = {
    "com.noshufou.android.su",
    "com.noshufou.android.su.elite", 
    "eu.chainfire.supersu",
    "com.koushikdutta.superuser",
    "com.thirdparty.superuser",
    "com.yellowes.su",
    "com.koushikdutta.rommanager",
    "com.koushikdutta.rommanager.license",
    "com.dimonvideo.luckypatcher",
    "com.chelpus.lackypatch",
    "com.ramdroid.appquarantine",
    "com.ramdroid.appquarantinepro",
    "com.devadvance.rootcloak",
    "com.devadvance.rootcloakplus",
    "de.robv.android.xposed.installer",
    "com.saurik.substrate",
    "com.zachspong.temprootremovejb",
    "com.amphoras.hidemyroot",
    "com.amphoras.hidemyrootadfree",
    "com.formyhm.hiderootPremium",
    "com.formyhm.hideroot",
    "me.phh.superuser",
    "eu.chainfire.supersu.pro",
    "com.kingouser.com",
    "com.topjohnwu.magisk",
    "com.lody.virtual",
    "io.va.exposed",
    "com.benny.openlauncher",
    nullptr
};

static bool is_blocked_file(const char* path) {
    if (!path) return false;
    for (int i = 0; blocked_files[i]; ++i) {
        if (strstr(path, blocked_files[i])) {
            return true;
        }
    }
    return false;
}

static bool is_blocked_package(const char* path) {
    if (!path) return false;
    for (int i = 0; blocked_packages[i]; ++i) {
        if (strstr(path, blocked_packages[i])) {
            return true;
        }
    }
    return false;
}

static bool is_safe_path(const char* path) {
    if (!path) return false;
    if (strstr(path, "/proc/net/")) return true;
    if (strstr(path, "/dev/socket/")) return true;
    return false;
}

static bool is_maps_path(const char* path) {
    if (!path) return false;
    return strstr(path, "/proc/") != nullptr && (strstr(path, "/maps") != nullptr || strstr(path, "/smaps") != nullptr);
}

static int (*orig_access)(const char *pathname, int mode) = nullptr;
static int (*orig_stat)(const char *pathname, struct stat *buf) = nullptr;
static int (*orig_lstat)(const char *pathname, struct stat *buf) = nullptr;
static FILE* (*orig_fopen)(const char *pathname, const char *mode) = nullptr;
static int (*orig_open)(const char *pathname, int flags, ...) = nullptr;
static int (*orig_openat)(int dirfd, const char *pathname, int flags, ...) = nullptr;
static ssize_t (*orig_readlink)(const char *pathname, char *buf, size_t bufsiz) = nullptr;
static DIR* (*orig_opendir)(const char *name) = nullptr;

static FILE* create_sanitized_maps_file(const char* real_path) {
    FILE* real_fp = orig_fopen ? orig_fopen(real_path, "r") : fopen(real_path, "r");
    if (!real_fp) return nullptr;

    FILE* tmp_fp = tmpfile();
    if (!tmp_fp) {
        fclose(real_fp);
        return nullptr;
    }

    char line[1024];
    while (fgets(line, sizeof(line), real_fp)) {
        if (strstr(line, "libblackbox.so") != nullptr ||
            strstr(line, "top.niunaijun.blackbox") != nullptr ||
            strstr(line, "blackboxa") != nullptr ||
            strstr(line, "/blackbox/") != nullptr ||
            strstr(line, "/virtual/") != nullptr ||
            strstr(line, "libblackbox") != nullptr) {
            continue; // Purge BlackBox framework references from memory map
        }
        fputs(line, tmp_fp);
    }
    fclose(real_fp);
    rewind(tmp_fp);
    return tmp_fp;
}

static int create_sanitized_maps_fd(const char* real_path) {
    FILE* tmp_fp = create_sanitized_maps_file(real_path);
    if (!tmp_fp) return -1;
    int raw_fd = fileno(tmp_fp);
    if (raw_fd < 0) {
        fclose(tmp_fp);
        return -1;
    }
    int dup_fd = dup(raw_fd);
    fclose(tmp_fp);
    return dup_fd;
}

static int my_access(const char *pathname, int mode) {
    if (pathname && !is_safe_path(pathname) && (is_blocked_file(pathname) || is_blocked_package(pathname))) {
        errno = ENOENT;
        return -1;
    }
    return orig_access ? orig_access(pathname, mode) : -1;
}

static int my_stat(const char *pathname, struct stat *buf) {
    if (pathname && !is_safe_path(pathname) && (is_blocked_file(pathname) || is_blocked_package(pathname))) {
        errno = ENOENT;
        return -1;
    }
    return orig_stat ? orig_stat(pathname, buf) : -1;
}

static int my_lstat(const char *pathname, struct stat *buf) {
    if (pathname && !is_safe_path(pathname) && (is_blocked_file(pathname) || is_blocked_package(pathname))) {
        errno = ENOENT;
        return -1;
    }
    return orig_lstat ? orig_lstat(pathname, buf) : -1;
}

static FILE* my_fopen(const char *pathname, const char *mode) {
    if (pathname && is_maps_path(pathname)) {
        FILE* fp = create_sanitized_maps_file(pathname);
        if (fp) {
            LOGD("my_fopen: Sanitized memory map for %s", pathname);
            return fp;
        }
    }
    if (pathname && !is_safe_path(pathname) && (is_blocked_file(pathname) || is_blocked_package(pathname))) {
        errno = ENOENT;
        return nullptr;
    }
    return orig_fopen ? orig_fopen(pathname, mode) : nullptr;
}

static int my_open(const char *pathname, int flags, ...) {
    if (pathname && is_maps_path(pathname)) {
        int fd = create_sanitized_maps_fd(pathname);
        if (fd >= 0) {
            LOGD("my_open: Sanitized memory map fd for %s", pathname);
            return fd;
        }
    }
    if (pathname && !is_safe_path(pathname) && (is_blocked_file(pathname) || is_blocked_package(pathname))) {
        errno = ENOENT;
        return -1;
    }
    if (orig_open) {
        if (flags & O_CREAT) {
            va_list args;
            va_start(args, flags);
            mode_t mode = va_arg(args, mode_t);
            va_end(args);
            return orig_open(pathname, flags, mode);
        } else {
            return orig_open(pathname, flags);
        }
    }
    return -1;
}

static int my_openat(int dirfd, const char *pathname, int flags, ...) {
    if (pathname && is_maps_path(pathname)) {
        int fd = create_sanitized_maps_fd(pathname);
        if (fd >= 0) {
            LOGD("my_openat: Sanitized memory map fd for %s", pathname);
            return fd;
        }
    }
    if (pathname && !is_safe_path(pathname) && (is_blocked_file(pathname) || is_blocked_package(pathname))) {
        errno = ENOENT;
        return -1;
    }
    if (orig_openat) {
        if (flags & O_CREAT) {
            va_list args;
            va_start(args, flags);
            mode_t mode = va_arg(args, mode_t);
            va_end(args);
            return orig_openat(dirfd, pathname, flags, mode);
        } else {
            return orig_openat(dirfd, pathname, flags);
        }
    }
    return -1;
}

static ssize_t my_readlink(const char *pathname, char *buf, size_t bufsiz) {
    if (pathname && !is_safe_path(pathname) && (is_blocked_file(pathname) || is_blocked_package(pathname))) {
        errno = ENOENT;
        return -1;
    }
    return orig_readlink ? orig_readlink(pathname, buf, bufsiz) : -1;
}

static DIR* my_opendir(const char *name) {
    if (name && !is_safe_path(name) && (is_blocked_file(name) || is_blocked_package(name))) {
        errno = ENOENT;
        return nullptr;
    }
    return orig_opendir ? orig_opendir(name) : nullptr;
}

static void install_file_hooks() {
    void* handle = xdl_open("libc.so", XDL_DEFAULT);
    if (!handle) {
        LOGD("xdl_open failed for libc.so");
        return;
    }

    void* target_access = xdl_dsym(handle, "access", nullptr);
    if (target_access) {
        DobbyHook(target_access, (void*)my_access, (void**)&orig_access);
    }

    void* target_stat = xdl_dsym(handle, "stat", nullptr);
    if (target_stat) {
        DobbyHook(target_stat, (void*)my_stat, (void**)&orig_stat);
    }

    void* target_lstat = xdl_dsym(handle, "lstat", nullptr);
    if (target_lstat) {
        DobbyHook(target_lstat, (void*)my_lstat, (void**)&orig_lstat);
    }

    void* target_fopen = xdl_dsym(handle, "fopen", nullptr);
    if (target_fopen) {
        DobbyHook(target_fopen, (void*)my_fopen, (void**)&orig_fopen);
    }

    void* target_open = xdl_dsym(handle, "open", nullptr);
    if (target_open) {
        DobbyHook(target_open, (void*)my_open, (void**)&orig_open);
    }

    void* target_openat = xdl_dsym(handle, "openat", nullptr);
    if (target_openat) {
        DobbyHook(target_openat, (void*)my_openat, (void**)&orig_openat);
    }

    void* target_readlink = xdl_dsym(handle, "readlink", nullptr);
    if (target_readlink) {
        DobbyHook(target_readlink, (void*)my_readlink, (void**)&orig_readlink);
    }

    void* target_opendir = xdl_dsym(handle, "opendir", nullptr);
    if (target_opendir) {
        DobbyHook(target_opendir, (void*)my_opendir, (void**)&orig_opendir);
    }

    xdl_close(handle);
    LOGD("File system and /proc/self/maps memory cloaking hooks installed");
}

__attribute__((constructor)) void install_antidetection_hooks() {
    LOGD("Installing anti-detection hooks...");
    install_file_hooks(); 
    LOGD("Anti-detection hooks installation complete");
}

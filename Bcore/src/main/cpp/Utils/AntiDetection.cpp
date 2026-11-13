#include "AntiDetection.h"
#include "IO.h"

#include <android/log.h>
#include <atomic>
#include <dirent.h>
#include <errno.h>
#include <fcntl.h>
#include <limits.h>
#include <stdarg.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <string>
#include <sys/stat.h>
#include <sys/syscall.h>
#include <unistd.h>
#include "Dobby/dobby.h"
#include "xdl.h"

#ifndef MFD_CLOEXEC
#define MFD_CLOEXEC 0x0001U
#endif

#ifndef __NR_memfd_create
#if defined(__aarch64__)
#define __NR_memfd_create 279
#elif defined(__arm__)
#define __NR_memfd_create 385
#endif
#endif

#define LOG_TAG "AntiDetection"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)

static std::atomic<bool> g_hooks_installed{false};

static const char *blocked_files[] = {
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

    // Virtualization & Sandbox artifacts (never match /blackbox/data/* virtual app files)
    "/data/virtual",
    "/data/data/com.benny.openlauncher",
    "/data/data/io.va.exposed",
    "/data/data/com.lody.virtual",
    "/data/data/com.excelliance.dualaid",
    "/data/data/com.lbe.parallel",
    "/data/data/com.dual.dualspace",
    "/data/data/com.ludashi.superboost",
    "/data/data/top.niunaijun.blackboxa",
    "/data/user/0/top.niunaijun.blackboxa",

    // Emulator & SELinux-denied proc artifacts
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
    "/proc/tty/drivers",
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

static const char *blocked_packages[] = {
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

static bool is_virtual_sandbox_path(const char *path) {
    if (!path) return false;
    return strstr(path, "/blackbox/data/") != nullptr ||
           strstr(path, "/blackbox/proc/") != nullptr ||
           strstr(path, "/blackbox/profiles/") != nullptr ||
           strstr(path, "/blackbox/cache/") != nullptr;
}

static bool is_safe_path(const char *path) {
    if (!path) return false;
    if (strstr(path, "/proc/net/")) return true;
    if (strstr(path, "/dev/socket/")) return true;
    return false;
}

static bool is_blocked_file(const char *path) {
    if (!path || is_virtual_sandbox_path(path)) return false;

    if (strcmp(path, "/blackbox") == 0 || strcmp(path, "/blackbox/") == 0 ||
        strcmp(path, "/virtual") == 0 || strcmp(path, "/virtual/") == 0) {
        return true;
    }

    // Block direct probing of the host package directories unless it points into /blackbox/data|proc|profiles|cache
    if ((strstr(path, "/data/data/top.niunaijun.blackbox") != nullptr ||
         strstr(path, "/data/user/0/top.niunaijun.blackbox") != nullptr ||
         strstr(path, "/data/app/") != nullptr) &&
        strstr(path, "top.niunaijun.blackbox") != nullptr) {
        return true;
    }

    for (int i = 0; blocked_files[i]; ++i) {
        if (strstr(path, blocked_files[i])) {
            return true;
        }
    }
    return false;
}

static bool is_blocked_package(const char *path) {
    if (!path || is_virtual_sandbox_path(path)) return false;
    for (int i = 0; blocked_packages[i]; ++i) {
        if (strstr(path, blocked_packages[i])) {
            return true;
        }
    }
    return false;
}

static bool ends_with(const char *str, const char *suffix) {
    if (!str || !suffix) return false;
    size_t str_len = strlen(str);
    size_t suf_len = strlen(suffix);
    if (str_len < suf_len) return false;
    return strcmp(str + str_len - suf_len, suffix) == 0;
}

static bool is_maps_path(const char *path) {
    if (!path) return false;
    if (strncmp(path, "/proc/", 6) != 0) return false;
    return ends_with(path, "/maps") ||
           ends_with(path, "/smaps") ||
           ends_with(path, "/smaps_rollup") ||
           ends_with(path, "/numa_maps");
}

static bool is_mountinfo_path(const char *path) {
    if (!path) return false;
    if (strncmp(path, "/proc/", 6) != 0) return false;
    return ends_with(path, "/mountinfo") || ends_with(path, "/mounts");
}

static int (*orig_access)(const char *pathname, int mode) = nullptr;
static int (*orig_faccessat)(int dirfd, const char *pathname, int mode, int flags) = nullptr;
static int (*orig_stat)(const char *pathname, struct stat *buf) = nullptr;
static int (*orig_lstat)(const char *pathname, struct stat *buf) = nullptr;
static int (*orig_fstatat)(int dirfd, const char *pathname, struct stat *buf, int flags) = nullptr;
static FILE *(*orig_fopen)(const char *pathname, const char *mode) = nullptr;
static FILE *(*orig_fopen64)(const char *pathname, const char *mode) = nullptr;
static int (*orig_open)(const char *pathname, int flags, ...) = nullptr;
static int (*orig_open64)(const char *pathname, int flags, ...) = nullptr;
static int (*orig_openat)(int dirfd, const char *pathname, int flags, ...) = nullptr;
static int (*orig_openat64)(int dirfd, const char *pathname, int flags, ...) = nullptr;
static ssize_t (*orig_readlink)(const char *pathname, char *buf, size_t bufsiz) = nullptr;
static ssize_t (*orig_readlinkat)(int dirfd, const char *pathname, char *buf, size_t bufsiz) = nullptr;
static DIR *(*orig_opendir)(const char *name) = nullptr;

// Rewrite virtual sandbox paths like:
//   /data/(data|user/<n>)/top.niunaijun.blackbox/blackbox/data/app/<pkg>/... -> /data/app/<pkg>/...
//   /data/(data|user/<n>)/top.niunaijun.blackbox/blackbox/data/user/<n>/<pkg>/... -> /data/user/0/<pkg>/...
//   /data/(data|user/<n>)/top.niunaijun.blackbox/blackbox/data/user_de/<n>/<pkg>/... -> /data/user_de/0/<pkg>/...
// Returns false if the string contains BlackBox host references that should be hidden completely.
static bool sanitize_virtual_text(const char *in, char *out, size_t out_size) {
    if (!in || !out || out_size == 0) return false;

    if (strstr(in, "libblackbox") != nullptr ||
        strstr(in, "libdobby") != nullptr ||
        strstr(in, "blackboxa") != nullptr ||
        strstr(in, "/virtual/") != nullptr) {
        return false;
    }

    const char *bb_data_app = strstr(in, "/blackbox/data/app/");
    const char *bb_data_user = strstr(in, "/blackbox/data/user/");
    const char *bb_data_user_de = strstr(in, "/blackbox/data/user_de/");

    if (bb_data_app || bb_data_user || bb_data_user_de) {
        const char *marker = bb_data_app ? bb_data_app : (bb_data_user ? bb_data_user : bb_data_user_de);
        const char *host_data = strstr(in, "/data/");
        if (!host_data || host_data > marker) {
            return false;
        }

        const char *replacement = nullptr;
        const char *after_marker = nullptr;
        if (marker == bb_data_app) {
            replacement = "/data/app/";
            after_marker = marker + strlen("/blackbox/data/app/");
        } else if (marker == bb_data_user) {
            replacement = "/data/user/0/";
            after_marker = marker + strlen("/blackbox/data/user/");
            while (*after_marker >= '0' && *after_marker <= '9') after_marker++;
            if (*after_marker == '/') after_marker++;
        } else {
            replacement = "/data/user_de/0/";
            after_marker = marker + strlen("/blackbox/data/user_de/");
            while (*after_marker >= '0' && *after_marker <= '9') after_marker++;
            if (*after_marker == '/') after_marker++;
        }

        size_t prefix_len = static_cast<size_t>(host_data - in);
        size_t rep_len = strlen(replacement);
        size_t tail_len = strlen(after_marker);
        if (prefix_len + rep_len + tail_len >= out_size) {
            return false;
        }
        memcpy(out, in, prefix_len);
        memcpy(out + prefix_len, replacement, rep_len);
        memcpy(out + prefix_len + rep_len, after_marker, tail_len);
        out[prefix_len + rep_len + tail_len] = '\0';

        // Recursively strip if any unexpected blackbox reference remains on the line
        if (strstr(out, "top.niunaijun.blackbox") != nullptr || strstr(out, "/blackbox/") != nullptr) {
            return false;
        }
        return true;
    }

    if (strstr(in, "top.niunaijun.blackbox") != nullptr || strstr(in, "/blackbox/") != nullptr) {
        return false;
    }

    size_t len = strlen(in);
    if (len >= out_size) len = out_size - 1;
    memcpy(out, in, len);
    out[len] = '\0';
    return true;
}

static bool write_all(int fd, const char *buf, size_t len) {
    size_t written = 0;
    while (written < len) {
        ssize_t n = write(fd, buf + written, len - written);
        if (n < 0) {
            if (errno == EINTR) continue;
            return false;
        }
        written += static_cast<size_t>(n);
    }
    return true;
}

static int create_memfd_file() {
#ifdef __NR_memfd_create
    int mfd = static_cast<int>(syscall(__NR_memfd_create, "jit-cache", MFD_CLOEXEC));
    if (mfd >= 0) {
        return mfd;
    }
#endif
    return -1;
}

static int create_sanitized_maps_fd(const char *real_path, bool is_mount) {
    FILE *real_fp = orig_fopen ? orig_fopen(real_path, "r") : fopen(real_path, "r");
    if (!real_fp) return -1;

    int mfd = create_memfd_file();
    if (mfd < 0) {
        fclose(real_fp);
        return -1;
    }

    char line[4096];
    char clean_line[4096];
    while (fgets(line, sizeof(line), real_fp)) {
        if (is_mount) {
            if (strstr(line, "blackbox") != nullptr || strstr(line, "top.niunaijun.blackbox") != nullptr) {
                continue;
            }
            write_all(mfd, line, strlen(line));
        } else {
            if (sanitize_virtual_text(line, clean_line, sizeof(clean_line))) {
                write_all(mfd, clean_line, strlen(clean_line));
            }
        }
    }
    fclose(real_fp);
    lseek(mfd, 0, SEEK_SET);
    return mfd;
}

static FILE *create_sanitized_maps_file(const char *real_path, bool is_mount) {
    int fd = create_sanitized_maps_fd(real_path, is_mount);
    if (fd < 0) return nullptr;
    FILE *fp = fdopen(fd, "r");
    if (!fp) {
        close(fd);
        return nullptr;
    }
    return fp;
}

static int my_access(const char *pathname, int mode) {
    if (pathname && !is_safe_path(pathname) && (is_blocked_file(pathname) || is_blocked_package(pathname))) {
        errno = ENOENT;
        return -1;
    }
    std::string storage;
    const char *final_path = IO::redirectPath(pathname, storage);
    return orig_access ? orig_access(final_path, mode) : -1;
}

static int my_faccessat(int dirfd, const char *pathname, int mode, int flags) {
    if (pathname && !is_safe_path(pathname) && (is_blocked_file(pathname) || is_blocked_package(pathname))) {
        errno = ENOENT;
        return -1;
    }
    std::string storage;
    const char *final_path = IO::redirectPath(pathname, storage);
    return orig_faccessat ? orig_faccessat(dirfd, final_path, mode, flags) : -1;
}

static int my_stat(const char *pathname, struct stat *buf) {
    if (pathname && !is_safe_path(pathname) && (is_blocked_file(pathname) || is_blocked_package(pathname))) {
        errno = ENOENT;
        return -1;
    }
    std::string storage;
    const char *final_path = IO::redirectPath(pathname, storage);
    return orig_stat ? orig_stat(final_path, buf) : -1;
}

static int my_lstat(const char *pathname, struct stat *buf) {
    if (pathname && !is_safe_path(pathname) && (is_blocked_file(pathname) || is_blocked_package(pathname))) {
        errno = ENOENT;
        return -1;
    }
    std::string storage;
    const char *final_path = IO::redirectPath(pathname, storage);
    return orig_lstat ? orig_lstat(final_path, buf) : -1;
}

static int my_fstatat(int dirfd, const char *pathname, struct stat *buf, int flags) {
    if (pathname && !is_safe_path(pathname) && (is_blocked_file(pathname) || is_blocked_package(pathname))) {
        errno = ENOENT;
        return -1;
    }
    std::string storage;
    const char *final_path = IO::redirectPath(pathname, storage);
    return orig_fstatat ? orig_fstatat(dirfd, final_path, buf, flags) : -1;
}

static FILE *handle_fopen_common(FILE *(*orig_fn)(const char *, const char *), const char *pathname, const char *mode) {
    if (pathname) {
        if (is_maps_path(pathname)) {
            FILE *fp = create_sanitized_maps_file(pathname, false);
            if (fp) return fp;
        } else if (is_mountinfo_path(pathname)) {
            FILE *fp = create_sanitized_maps_file(pathname, true);
            if (fp) return fp;
        }
        if (!is_safe_path(pathname) && (is_blocked_file(pathname) || is_blocked_package(pathname))) {
            errno = ENOENT;
            return nullptr;
        }
    }
    std::string storage;
    const char *final_path = IO::redirectPath(pathname, storage);
    return orig_fn ? orig_fn(final_path, mode) : nullptr;
}

static FILE *my_fopen(const char *pathname, const char *mode) {
    return handle_fopen_common(orig_fopen, pathname, mode);
}

static FILE *my_fopen64(const char *pathname, const char *mode) {
    return handle_fopen_common(orig_fopen64, pathname, mode);
}

static inline bool has_mode_arg(int flags) {
#ifdef O_TMPFILE
    return (flags & O_CREAT) != 0 || (flags & O_TMPFILE) == O_TMPFILE;
#else
    return (flags & O_CREAT) != 0;
#endif
}

static int handle_open_common(int (*orig_fn)(const char *, int, ...), const char *pathname, int flags, mode_t mode, bool use_mode) {
    if (pathname) {
        if (is_maps_path(pathname)) {
            int fd = create_sanitized_maps_fd(pathname, false);
            if (fd >= 0) return fd;
        } else if (is_mountinfo_path(pathname)) {
            int fd = create_sanitized_maps_fd(pathname, true);
            if (fd >= 0) return fd;
        }
        if (!is_safe_path(pathname) && (is_blocked_file(pathname) || is_blocked_package(pathname))) {
            errno = ENOENT;
            return -1;
        }
    }
    if (!orig_fn) return -1;
    std::string storage;
    const char *final_path = IO::redirectPath(pathname, storage);
    return use_mode ? orig_fn(final_path, flags, mode) : orig_fn(final_path, flags);
}

static int my_open(const char *pathname, int flags, ...) {
    mode_t mode = 0;
    bool use_mode = has_mode_arg(flags);
    if (use_mode) {
        va_list args;
        va_start(args, flags);
        mode = static_cast<mode_t>(va_arg(args, int));
        va_end(args);
    }
    return handle_open_common(orig_open, pathname, flags, mode, use_mode);
}

static int my_open64(const char *pathname, int flags, ...) {
    mode_t mode = 0;
    bool use_mode = has_mode_arg(flags);
    if (use_mode) {
        va_list args;
        va_start(args, flags);
        mode = static_cast<mode_t>(va_arg(args, int));
        va_end(args);
    }
    return handle_open_common(orig_open64, pathname, flags, mode, use_mode);
}

static int handle_openat_common(int (*orig_fn)(int, const char *, int, ...), int dirfd, const char *pathname, int flags, mode_t mode, bool use_mode) {
    if (pathname) {
        if (is_maps_path(pathname)) {
            int fd = create_sanitized_maps_fd(pathname, false);
            if (fd >= 0) return fd;
        } else if (is_mountinfo_path(pathname)) {
            int fd = create_sanitized_maps_fd(pathname, true);
            if (fd >= 0) return fd;
        }
        if (!is_safe_path(pathname) && (is_blocked_file(pathname) || is_blocked_package(pathname))) {
            errno = ENOENT;
            return -1;
        }
    }
    if (!orig_fn) return -1;
    std::string storage;
    const char *final_path = IO::redirectPath(pathname, storage);
    return use_mode ? orig_fn(dirfd, final_path, flags, mode) : orig_fn(dirfd, final_path, flags);
}

static int my_openat(int dirfd, const char *pathname, int flags, ...) {
    mode_t mode = 0;
    bool use_mode = has_mode_arg(flags);
    if (use_mode) {
        va_list args;
        va_start(args, flags);
        mode = static_cast<mode_t>(va_arg(args, int));
        va_end(args);
    }
    return handle_openat_common(orig_openat, dirfd, pathname, flags, mode, use_mode);
}

static int my_openat64(int dirfd, const char *pathname, int flags, ...) {
    mode_t mode = 0;
    bool use_mode = has_mode_arg(flags);
    if (use_mode) {
        va_list args;
        va_start(args, flags);
        mode = static_cast<mode_t>(va_arg(args, int));
        va_end(args);
    }
    return handle_openat_common(orig_openat64, dirfd, pathname, flags, mode, use_mode);
}

static ssize_t sanitize_readlink_result(ssize_t len, char *buf, size_t bufsiz) {
    if (len <= 0 || !buf || bufsiz == 0 || static_cast<size_t>(len) >= PATH_MAX) {
        return len;
    }
    char raw[PATH_MAX];
    char clean[PATH_MAX];
    memcpy(raw, buf, static_cast<size_t>(len));
    raw[len] = '\0';

    if (strstr(raw, "/blackbox/data/") != nullptr) {
        if (sanitize_virtual_text(raw, clean, sizeof(clean))) {
            size_t new_len = strlen(clean);
            if (new_len > bufsiz) new_len = bufsiz;
            memcpy(buf, clean, new_len);
            return static_cast<ssize_t>(new_len);
        }
    }
    return len;
}

static ssize_t my_readlink(const char *pathname, char *buf, size_t bufsiz) {
    if (pathname && !is_safe_path(pathname) && (is_blocked_file(pathname) || is_blocked_package(pathname))) {
        errno = ENOENT;
        return -1;
    }
    if (!orig_readlink) return -1;
    std::string storage;
    const char *final_path = IO::redirectPath(pathname, storage);
    ssize_t ret = orig_readlink(final_path, buf, bufsiz);
    return sanitize_readlink_result(ret, buf, bufsiz);
}

static ssize_t my_readlinkat(int dirfd, const char *pathname, char *buf, size_t bufsiz) {
    if (pathname && !is_safe_path(pathname) && (is_blocked_file(pathname) || is_blocked_package(pathname))) {
        errno = ENOENT;
        return -1;
    }
    if (!orig_readlinkat) return -1;
    std::string storage;
    const char *final_path = IO::redirectPath(pathname, storage);
    ssize_t ret = orig_readlinkat(dirfd, final_path, buf, bufsiz);
    return sanitize_readlink_result(ret, buf, bufsiz);
}

static DIR *my_opendir(const char *name) {
    if (name && !is_safe_path(name) && (is_blocked_file(name) || is_blocked_package(name))) {
        errno = ENOENT;
        return nullptr;
    }
    std::string storage;
    const char *final_path = IO::redirectPath(name, storage);
    return orig_opendir ? orig_opendir(final_path) : nullptr;
}

static void hook_unique_symbol(void *handle, const char *name, void *replacement, void **orig,
                               void **seen, int &seen_count, int max_seen) {
    void *target = xdl_dsym(handle, name, nullptr);
    if (!target) return;
    for (int i = 0; i < seen_count; ++i) {
        if (seen[i] == target) {
            return;
        }
    }
    if (DobbyHook(target, replacement, orig) == 0 && seen_count < max_seen) {
        seen[seen_count++] = target;
    }
}

void AntiDetection::init() {
    bool expected = false;
    if (!g_hooks_installed.compare_exchange_strong(expected, true)) {
        return;
    }

    void *handle = xdl_open("libc.so", XDL_DEFAULT);
    if (!handle) {
        LOGD("xdl_open failed for libc.so");
        return;
    }

    void *seen[24] = {nullptr};
    int seen_count = 0;
    constexpr int max_seen = 24;

    hook_unique_symbol(handle, "access", (void *) my_access, (void **) &orig_access, seen, seen_count, max_seen);
    hook_unique_symbol(handle, "faccessat", (void *) my_faccessat, (void **) &orig_faccessat, seen, seen_count, max_seen);
    hook_unique_symbol(handle, "stat", (void *) my_stat, (void **) &orig_stat, seen, seen_count, max_seen);
    hook_unique_symbol(handle, "lstat", (void *) my_lstat, (void **) &orig_lstat, seen, seen_count, max_seen);
    hook_unique_symbol(handle, "fstatat", (void *) my_fstatat, (void **) &orig_fstatat, seen, seen_count, max_seen);
    hook_unique_symbol(handle, "fstatat64", (void *) my_fstatat, (void **) &orig_fstatat, seen, seen_count, max_seen);
    hook_unique_symbol(handle, "fopen", (void *) my_fopen, (void **) &orig_fopen, seen, seen_count, max_seen);
    hook_unique_symbol(handle, "fopen64", (void *) my_fopen64, (void **) &orig_fopen64, seen, seen_count, max_seen);
    hook_unique_symbol(handle, "open", (void *) my_open, (void **) &orig_open, seen, seen_count, max_seen);
    hook_unique_symbol(handle, "open64", (void *) my_open64, (void **) &orig_open64, seen, seen_count, max_seen);
    hook_unique_symbol(handle, "openat", (void *) my_openat, (void **) &orig_openat, seen, seen_count, max_seen);
    hook_unique_symbol(handle, "openat64", (void *) my_openat64, (void **) &orig_openat64, seen, seen_count, max_seen);
    hook_unique_symbol(handle, "readlink", (void *) my_readlink, (void **) &orig_readlink, seen, seen_count, max_seen);
    hook_unique_symbol(handle, "readlinkat", (void *) my_readlinkat, (void **) &orig_readlinkat, seen, seen_count, max_seen);
    hook_unique_symbol(handle, "opendir", (void *) my_opendir, (void **) &orig_opendir, seen, seen_count, max_seen);

    xdl_close(handle);
    LOGD("Native IO redirection, anti-detection, and /proc/self/maps memfd cloaking hooks installed (%d unique)", seen_count);
}

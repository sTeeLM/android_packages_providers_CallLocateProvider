LOCAL_PATH:= $(call my-dir)
#include $(CLEAR_VARS)

#LOCAL_MODULE_TAGS := user

#LOCAL_SRC_FILES := $(call all-subdir-java-files)

#LOCAL_MODULE := com.liwen.calllocate
#include $(BUILD_STATIC_JAVA_LIBRARY)

include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := eng

LOCAL_SRC_FILES := $(call all-subdir-java-files)

LOCAL_PACKAGE_NAME := CallLocateProvider
LOCAL_CERTIFICATE := shared

LOCAL_STATIC_JAVA_LIBRARIES += android-common

include $(BUILD_PACKAGE)

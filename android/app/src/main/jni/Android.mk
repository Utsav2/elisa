MY_LOCAL_PATH := $(call my-dir)

include $(call all-subdir-makefiles)

LOCAL_PATH := $(MY_LOCAL_PATH)

include $(CLEAR_VARS)
LOCAL_MODULE := libavcodec
LOCAL_SRC_FILES := video/libavcodec.so
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := libavdevice
LOCAL_SRC_FILES := video/libavdevice.so
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := libavfilter
LOCAL_SRC_FILES := video/libavfilter.so
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := libavformat
LOCAL_SRC_FILES := video/libavformat.so
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := libavutil
LOCAL_SRC_FILES := video/libavutil.so
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := libjniavcodec
LOCAL_SRC_FILES := video/libjniavcodec.so
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := libjniavdevice
LOCAL_SRC_FILES := video/libjniavdevice.so
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := libjniavfilter
LOCAL_SRC_FILES := video/libjniavfilter.so
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := libjniavformat
LOCAL_SRC_FILES := video/libjniavformat.so
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := libjniavutil
LOCAL_SRC_FILES := video/libjniavutil.so
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := libjnicvkernels
LOCAL_SRC_FILES := video/libjnicvkernels.so
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := libjniopencv_calib3d
LOCAL_SRC_FILES := video/libjniopencv_calib3d.so
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := libjniopencv_core
LOCAL_SRC_FILES := video/libjniopencv_core.so
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := libjniopencv_dnn
LOCAL_SRC_FILES := video/libjniopencv_dnn.so
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := libjniopencv_face
LOCAL_SRC_FILES := video/libjniopencv_face.so
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := libjniopencv_features2d
LOCAL_SRC_FILES := video/libjniopencv_features2d.so
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := libjniopencv_flann
LOCAL_SRC_FILES := video/libjniopencv_flann.so
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := libjniopencv_highgui
LOCAL_SRC_FILES := video/libjniopencv_highgui.so
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := libjniopencv_imgcodecs
LOCAL_SRC_FILES := video/libjniopencv_imgcodecs.so
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := libjniopencv_imgproc
LOCAL_SRC_FILES := video/libjniopencv_imgproc.so
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := libjniopencv_ml
LOCAL_SRC_FILES := video/libjniopencv_ml.so
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := libopencv_objdetect
LOCAL_SRC_FILES := video/libopencv_objdetect.so
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := libjniopencv_objdetect
LOCAL_SRC_FILES := video/libjniopencv_objdetect.so
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := libjniopencv_optflow
LOCAL_SRC_FILES := video/libjniopencv_optflow.so
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := libjniopencv_photo
LOCAL_SRC_FILES := video/libjniopencv_photo.so
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := libjniopencv_shape
LOCAL_SRC_FILES := video/libjniopencv_shape.so
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := libjniopencv_stitching
LOCAL_SRC_FILES := video/libjniopencv_stitching.so
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := libjniopencv_superres
LOCAL_SRC_FILES := video/libjniopencv_superres.so
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := libjniopencv_video
LOCAL_SRC_FILES := video/libjniopencv_video.so
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := libjniopencv_videostab
LOCAL_SRC_FILES := video/libjniopencv_videostab.so
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := libjniopencv_xfeatures2d
LOCAL_SRC_FILES := video/libjniopencv_xfeatures2d.so
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := libjniopencv_ximgproc
LOCAL_SRC_FILES := video/libjniopencv_ximgproc.so
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE :=libjnipostproc
LOCAL_SRC_FILES := video/libjnipostproc.so
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := libjniswresample
LOCAL_SRC_FILES := video/libjniswresample.so
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := libjniswscale
LOCAL_SRC_FILES := video/libjniswscale.so
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := libopencv_calib3d
LOCAL_SRC_FILES := video/libopencv_calib3d.so
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := libopencv_core
LOCAL_SRC_FILES := video/libopencv_core.so
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := libopencv_dnn
LOCAL_SRC_FILES := video/libopencv_dnn.so
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := libopencv_face
LOCAL_SRC_FILES := video/libopencv_face.so
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := libopencv_features2d
LOCAL_SRC_FILES := video/libopencv_features2d.so
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := libopencv_flann
LOCAL_SRC_FILES := video/libopencv_flann.so
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := libopencv_highgui
LOCAL_SRC_FILES := video/libopencv_highgui.so
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := libopencv_imgcodecs
LOCAL_SRC_FILES := video/libopencv_imgcodecs.so
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := libopencv_imgproc
LOCAL_SRC_FILES := video/libopencv_imgproc.so
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := libopencv_ml
LOCAL_SRC_FILES := video/libopencv_ml.so
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := libopencv_optflow
LOCAL_SRC_FILES := video/libopencv_optflow.so
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := libopencv_photo
LOCAL_SRC_FILES := video/libopencv_photo.so
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := libopencv_shape
LOCAL_SRC_FILES := video/libopencv_shape.so
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := libopencv_stitching
LOCAL_SRC_FILES := video/libopencv_stitching.so
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := libopencv_superres
LOCAL_SRC_FILES := video/libopencv_superres.so
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := libopencv_video
LOCAL_SRC_FILES := video/libopencv_video.so
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := libjniopencv_videoio
LOCAL_SRC_FILES := video/libjniopencv_videoio.so
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := libopencv_videoio
LOCAL_SRC_FILES := video/libopencv_videoio.so
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := libopencv_videostab
LOCAL_SRC_FILES := video/libopencv_videostab.so
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := libopencv_xfeatures2d
LOCAL_SRC_FILES := video/libopencv_xfeatures2d.so
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := libopencv_ximgproc
LOCAL_SRC_FILES := video/libopencv_ximgproc.so
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := libpostproc
LOCAL_SRC_FILES := video/libpostproc.so
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := libswresample
LOCAL_SRC_FILES := video/libswresample.so
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := libswscale
LOCAL_SRC_FILES := video/libswscale.so
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)

ifeq ($(TARGET_ARCH_ABI),armeabi-v7a)
    LOCAL_ARM_NEON := true
endif
LOCAL_MODULE		:= elisa
LOCAL_SRC_FILES	 	:= imtoolbox.cc elisa.cc uiuc_bioassay_elisa_ELISAApplication.cc
LOCAL_C_INCLUDES 	+= $(LOCAL_PATH)/libjpeg $(LOCAL_PATH)/libpng $(LOCAL_PATH)
LOCAL_STATIC_LIBRARIES 	:= libpng libjpeg 
#LOCAL_CPP_FEATURES 	+= exceptions
LOCAL_CFLAGS            := -Wall -Wextra -funroll-loops -O3 -DANDROID
LOCAL_LDLIBS 		:= -lz -llog

include $(BUILD_SHARED_LIBRARY)



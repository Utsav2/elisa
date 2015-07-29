#ifndef ELISA_H
#define ELISA_H
#include <string>
#include "imtoolbox.h"
namespace elisa {
constexpr size_t left_off = 1100;
constexpr size_t right_off = 1100;
constexpr uint8_t thr_noise = 20;

//
constexpr const char *BB_FOLDER = "bb/";
constexpr const char *AVG_FILE_NAME = "avg.jpg";
constexpr const char *BB_DATA = "bb.bin";
constexpr size_t MAX_PICTURE = 4;

int process_bb(const std::string &path) noexcept;
inline int process_bb(const char *path) noexcept {
  return process_bb(std::string(path));
}

int process_sample(const std::string &path, std::vector<imtoolbox::fp_t> &s,
                   std::vector<imtoolbox::fp_t> &bg) noexcept;
inline int process_sample(const char *path, std::vector<imtoolbox::fp_t> &s,
                          std::vector<imtoolbox::fp_t> &bg) noexcept {
  return process_sample(std::string(path), s, bg);
}
} // namespace elisa
#endif

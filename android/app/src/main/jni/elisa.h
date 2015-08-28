#ifndef ELISA_H
#define ELISA_H
#include <string>
#include "imtoolbox.h"
namespace elisa {
using namespace imtoolbox;
constexpr size_t left_off = 1100;
constexpr size_t right_off = 1100;
constexpr uint8_t thr_noise = 20;

//
constexpr const char *BB_FOLDER = "bb/";
constexpr const char *AVG_FILE_NAME = "avg.jpg";
constexpr const char *BB_DATA = "bb.bin";
constexpr const char *RES = "res.bin";
constexpr const char *RGB_SPEC = "rgb.bin";
constexpr size_t MAX_PICTURE = 4;

int process_bb(const std::string &path) noexcept;
inline int process_bb(const char *path) noexcept {
  return process_bb(std::string(path));
}

int process_sample(const std::string &path) noexcept;
inline int process_sample(const char *path) noexcept {
  return process_sample(std::string(path));
}

inline std::vector<fp_t> read_rgb_spec(const char* path) {
  std::fstream ifs_rgb_spec(path, std::ios::in | std::ios::binary);
  if (!ifs_rgb_spec.good()) {
    println_e("Couldn't open broadband file to read");
    return std::vector<fp_t>{};
  }
  size_t col_end;
  ifs_rgb_spec.read(reinterpret_cast<char *>(&col_end), sizeof(col_end));
  std::vector<fp_t> rgb_spec(3 * col_end);
  for (auto first = rgb_spec.begin(), last = rgb_spec.end(); first != last;
       ++first) {
    ifs_rgb_spec.read(reinterpret_cast<char *>(&(*first)), sizeof(*first));
  }
  ifs_rgb_spec.close();
  return rgb_spec;
}

inline std::vector<fp_t> read_res(const char* path, bool is_bb) {
  std::fstream ifs_res(path, std::ios::in | std::ios::binary);
  if (!ifs_res.good()) {
    println_e("Couldn't open result file to read");
    return std::vector<fp_t>{};
  }
  size_t col_end;
  ifs_res.read(reinterpret_cast<char *>(&col_end), sizeof(col_end));
  std::vector<fp_t> res(col_end);
  for (auto first = res.begin(), last = res.end(); first != last;
       ++first) {
    ifs_res.read(reinterpret_cast<char *>(&(*first)), sizeof(*first));
  }
  ifs_res.close();
  if (is_bb) {
    for (size_t i = 0; i < res.size(); ++i) {
      if (!almost_equal(res[i], static_cast<fp_t>(0))) {
        res[i] = 1;
      } else {
        res[i] = 0;
      }
    }
  }
  return res;
}
} // namespace elisa
#endif

#include "elisa.h"
using namespace imtoolbox;
int main(int argc, char *argv[]) {
  if (argc != 2) {
    std::cout << "Usage: " << argv[0] << " folder\n";
    return -1;
  }

  if (elisa::process_bb("test/bb/") != 0) {
    return -1;
  }
  elisa::process_sample("test/sample/");
  return 0;
}

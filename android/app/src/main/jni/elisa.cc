#include "elisa.h"
#include "imtoolbox.h"
namespace elisa {
    using namespace imtoolbox;

    template <typename T = fp_t> struct Point {
        T x;
        T y;
        Point(T _x, T _y) : x(_x), y(_y) {}
    };

    struct Circle {
        fp_t xc;
        fp_t yc;
        fp_t r;
    };

    template <typename ForwardIterator>
    std::pair<size_t, size_t> minmax_index(ForwardIterator first,
                                           ForwardIterator last,
                                           decltype(*first) thr) {
        size_t idx = 0;
        while (first != last && *first <= thr) {
            ++first;
            ++idx;
        }
        if (first == last) {
            return std::make_pair(-1, -1);
        }

        size_t idx_smallest = idx;
        size_t idx_largest = idx;

        while (++first != last) {
            ++idx;
            if (*first <= thr) {
                continue;
            }
            idx_largest = idx;
        }
        return std::make_pair(idx_smallest, idx_largest);
    }

// Maybe I should add backward search
    template <typename M>
    std::vector<Point<fp_t>>
    find_points(const M &m, const std::pair<size_t, size_t> &idxs, fp_t pct) {
        static_assert(is_matrix<M>() && M::order == 3,
                      "Operation requires 3D matrix");
        std::vector<Point<fp_t>> v;
        for (size_t i = idxs.first; i <= idxs.second; ++i) {
            auto data = m(i, slice::all, 0);
            auto p = imtoolbox::max_element(data.begin(), data.end());
            auto thr = (*p.first) * pct;
            while (*p.first >= thr) {
                ++p.first;
                ++p.second;
            }
            fp_t a1 = data(0, p.second, 0) - data(0, p.second - 1, 0);
            fp_t a2 = data(0, p.second, 0) - a1 * p.second;
            v.emplace_back((thr - a2) / a1, i);
        }
        return v;
    }

// n-std filter
    void filter_points(std::vector<Point<fp_t>> &points, int n) {
        fp_t mean_x = std::accumulate(points.begin(), points.end(), 0.0,
                                      [](fp_t total, const auto &point) {
                                          return total + point.x;
                                      }) /
                      points.size();
        fp_t var_x =
                std::accumulate(points.begin(), points.end(), 0.0,
                                [&mean_x](fp_t total, const auto &point) {
                                    return total + (point.x - mean_x) * (point.x - mean_x);
                                }) /
                (points.size() - 1);
        fp_t std_x = sqrt(var_x);
        points.erase(remove_if(points.begin(), points.end(),
                               [&n, &mean_x, &std_x](const auto &point) {
                                   return (point.x > mean_x + n * std_x) ||
                                          (point.x < mean_x - n * std_x);
                               }),
                     points.end());
    }

    template <typename T>
    fp_t fit_circle(const std::vector<Point<T>> &p, Circle &circle) {
        fp_t sum_x[3]{};  // Hold sum of x, sum of x^2, sum of x^3
        fp_t sum_y[3]{};  // Hold sum of y, sum of y^2, sum of y^3
        fp_t sum_xy = 0;  // Hold sum of x*y
        fp_t sum_xy2 = 0; // Hold sum of x*y*y
        fp_t sum_x2y = 0; // Hold sum of x*x*y
        fp_t res = 0;
        size_t n = p.size();
        for (size_t i = 0; i < n; ++i) {
            fp_t prod_x = p[i].x;
            fp_t prod_y = p[i].y;
            for (int j = 0; j < 3; ++j) {
                sum_x[j] += prod_x;
                sum_y[j] += prod_y;
                prod_x *= p[i].x;
                prod_y *= p[i].y;
            }
        }

        for (size_t i = 0; i < n; ++i) {
            sum_xy += p[i].x * p[i].y;
        }

        for (size_t i = 0; i < n; ++i) {
            sum_xy2 += p[i].x * p[i].y * p[i].y;
        }

        for (size_t i = 0; i < n; ++i) {
            sum_x2y += p[i].x * p[i].x * p[i].y;
        }

        fp_t A = n * sum_x[1] - sum_x[0] * sum_x[0];
        fp_t B = n * sum_xy - sum_x[0] * sum_y[0];
        fp_t C = n * sum_y[1] - sum_y[0] * sum_y[0];
        fp_t D = 0.5 * (n * sum_xy2 - sum_x[0] * sum_y[1] + n * sum_x[2] -
                        sum_x[0] * sum_x[1]);
        fp_t E = 0.5 * (n * sum_x2y - sum_y[0] * sum_x[1] + n * sum_y[2] -
                        sum_y[0] * sum_y[1]);
        circle.xc = (D * C - B * E) / (A * C - B * B);
        circle.yc = (A * E - B * D) / (A * C - B * B);
        circle.r = 0;
        for (size_t i = 0; i < n; ++i) {
            circle.r += sqrt((p[i].x - circle.xc) * (p[i].x - circle.xc) +
                             (p[i].y - circle.yc) * (p[i].y - circle.yc));
        }
        circle.r /= n;
        for (size_t i = 0; i < n; ++i) {
            fp_t err = circle.r - sqrt((p[i].x - circle.xc) * (p[i].x - circle.xc) +
                                       (p[i].y - circle.yc) * (p[i].y - circle.yc));
            res += err * err;
        }
        return res / n;
    }

    template <typename ForwardIterator, typename OutputIt1, typename OutputIt2>
    inline void
    get_normalized_coeffs_and_mask(ForwardIterator first, ForwardIterator last,
                                   OutputIt1 mask_first, OutputIt2 d_first) {
        while (first != last) {
            auto r = *first;
            auto g = *++first;
            auto b = *++first;
            bool is_noise = (r < thr_noise) && (g < thr_noise) && (b < thr_noise);
            *mask_first = !is_noise;
            fp_t denom = r * r + g * g + b * b;
            *d_first = is_noise ? 0 : (r / denom);
            *(++d_first) = is_noise ? 0 : (g / denom);
            *(++d_first) = is_noise ? 0 : (b / denom);
            ++first;
            ++mask_first;
            ++d_first;
        }
    }

    template <typename ForwardIterator1, typename ForwardIterator2,
            typename OutputIt>
    void get_normalized_data(ForwardIterator1 first1, ForwardIterator1 last1,
                             ForwardIterator2 first2, OutputIt d_first) {
        while (first1 != last1) {
            auto r = *first1;
            auto g = *++first1;
            auto b = *++first1;
            auto r_coeff = *first2;
            auto g_coeff = *++first2;
            auto b_coeff = *++first2;
            bool is_noise = (r < thr_noise) && (g < thr_noise) && (b < thr_noise);
            *d_first = is_noise ? 0 : (r * r_coeff + g * g_coeff + b * b_coeff);
            ++first1;
            ++first2;
            ++d_first;
        }
    }

    template <typename ForwardIterator1, typename OutputIt>
    void copy_and_remove_noise(ForwardIterator1 first1, ForwardIterator1 last1, OutputIt d_first) {
        while (first1 != last1) {
            auto r = *first1;
            auto g = *++first1;
            auto b = *++first1;
            bool is_noise = (r < thr_noise) && (g < thr_noise) && (b < thr_noise);
            *d_first = is_noise ? 0 : (r + g + b);
            ++first1;
            ++d_first;
        }
    }


    int process_bb(const std::string &path) noexcept {
        matrix3<uint8_t> bb_im;
        bb_im = avg_folder<MAX_PICTURE>(
                path, AVG_FILE_NAME, Margin{left_off, right_off, 0, 0});
        if (is_empty(bb_im)) {
            println_e("Couldn't load sample image");
            LOGD("%d", __LINE__);
            return -1;
        }

        auto bb_gray = rgb2gray(bb_im);
        auto h = imhist(bb_gray.begin(), bb_gray.end(), 256, [](auto x) {
            return std::min(255 * x, static_cast<fp_t>(255));
        });
        auto thresh = pctl_hist_thresh(h, 0.7) / 255;

        auto mask = bb_gray >= thresh;
        std::vector<Rect> rects;
        bwlabel(mask, rects);
        sort(rects.begin(), rects.end(), [](const auto &a, const auto &b) {
            return (a.x2 - a.x1 + 1) * (a.y2 - a.y1 + 1) >
                   (b.x2 - b.x1 + 1) * (b.y2 - b.y1 + 1);
        });

        auto bb_roi = bb_im(slice{rects[0].y1, rects[0].y2}, slice::all, slice::all);
        auto s = sum(sum(bb_roi, 2), 1);

        auto tmp = s.clone();
        sort(tmp.begin(), tmp.end());

        auto thr = tmp(std::round(tmp.size() * 0.5) - 1);

        auto pair_idx = minmax_index(s.begin(), s.end(), thr);

        auto points = find_points(bb_roi(slice::all, slice::all, 0), pair_idx, 0.6);

        filter_points(points, 2);

        Circle circle;
        auto err = fit_circle(points, circle);
        size_t top_off = rects[0].y1;
        size_t bottom_off = bb_im.size(0) - 1 - rects[0].y2;
        circle.xc += left_off;
        circle.yc += top_off;
        fp_t r_start = sqrt((left_off - circle.xc) * (left_off - circle.xc) +
                            (top_off - circle.yc) * (top_off - circle.yc));
        size_t col_start = floor(
                sqrt(r_start * r_start - (0 - circle.yc) * (0 - circle.yc)) + circle.xc);
        fp_t r_end = sqrt((left_off + bb_roi.size(1) - 1 - circle.xc) *
                          (left_off + bb_roi.size(1) - 1 - circle.xc) +
                          (top_off - circle.yc) * (top_off - circle.yc));
        size_t col_end =
                ceil(sqrt(r_end * r_end - (0 - circle.yc) * (0 - circle.yc)) + circle.xc);

        size_t height = bb_roi.size(0);
        size_t width = bb_roi.size(1);
        size_t depth = bb_roi.size(2);

        matrix3<fp_t> coeffs(bb_roi.descriptor());
        mask = matrix2<logical>(coeffs.size(0), coeffs.size(1));

        get_normalized_coeffs_and_mask(bb_roi.begin(), bb_roi.end(), mask.begin(),
                                       coeffs.begin());

        // Calculate broadband
        col_end += 1;
        std::vector<fp_t> bg(col_end);
        std::vector<fp_t> red(col_end);
        std::vector<fp_t> green(col_end);
        std::vector<fp_t> blue(col_end);

        for (size_t i = 0; i < col_start; ++i) {
            bg[i] = 0;
            red[i] = 0;
            green[i] = 0;
            blue[i] = 0;
        }
        size_t y_start = top_off;
        size_t y_end = top_off + height - 1;
        for (size_t i = col_start; i < col_end; ++i) {
            fp_t R = sqrt((i - circle.xc) * (i - circle.xc) +
                          (0 - circle.yc) * (0 - circle.yc));
            fp_t bg_val = 0;
            fp_t red_val = 0;
            fp_t green_val = 0;
            fp_t blue_val = 0;
            for (size_t y = y_start; y <= y_end; ++y) {
                fp_t x = sqrt(R * R - (y - circle.yc) * (y - circle.yc)) + circle.xc -
                         left_off;
                if (x >= 0 && x < width - 1) {
                    auto x1 = floor(x);
                    auto p = x - x1;
                    bg_val +=
                            (1 - p) * mask(y - top_off, x1) + p * mask(y - top_off, x1 + 1);
                    red_val += (1 - p) * bb_roi(y - top_off, x1, 0) +
                               p * bb_roi(y - top_off, x1 + 1, 0);
                    green_val += (1 - p) * bb_roi(y - top_off, x1, 1) +
                                 p * bb_roi(y - top_off, x1 + 1, 1);
                    blue_val += (1 - p) * bb_roi(y - top_off, x1, 2) +
                                p * bb_roi(y - top_off, x1 + 1, 2);
                }
            }
            bg[i] = bg_val;
            red[i] = red_val;
            green[i] = green_val;
            blue[i] = blue_val;
        }

        // Write rgb spectrum to file
        std::fstream ofs_rgb_spec(path + RGB_SPEC, std::ios::out | std::ios::binary);
        if (!ofs_rgb_spec.good()) {
            println_e("Couldn't open broadband rgb spectrum file to write");
            LOGD("%d", __LINE__);
            return -1;
        }

        ofs_rgb_spec.write(reinterpret_cast<char *>(&col_end), sizeof(col_end));
        auto first_r = red.begin();
        auto last_r = red.end();
        auto first_g = green.begin();
        auto first_b = blue.begin();

        while (first_r != last_r) {
            ofs_rgb_spec.write(reinterpret_cast<char *>(&(*first_r)), sizeof(*first_r));
            ofs_rgb_spec.write(reinterpret_cast<char *>(&(*first_g)), sizeof(*first_g));
            ofs_rgb_spec.write(reinterpret_cast<char *>(&(*first_b)), sizeof(*first_b));
            ++first_r;
            ++first_g;
            ++first_b;
        }
        ofs_rgb_spec.close();

        // Write result to file
        std::fstream ofs_res(path + RES, std::ios::out | std::ios::binary);
        if (!ofs_res.good()) {
            println_e("Couldn't open broadband result file to write");
            LOGD("%d", __LINE__);
            return -1;
        }
        ofs_res.write(reinterpret_cast<char *>(&col_end), sizeof(col_end));
        for (auto first = bg.begin(), last = bg.end(); first != last; ++first) {
            ofs_res.write(reinterpret_cast<char *>(&(*first)), sizeof(*first));
        }
        ofs_res.close();

        // Write to file
        std::fstream ofs(path + BB_DATA, std::ios::out | std::ios::binary);
        if (!ofs.good()) {
            println_e("Couldn't open broadband file to write");
            LOGD("%d", __LINE__);
            return -1;
        }
        ofs.write(reinterpret_cast<char *>(&top_off), sizeof(top_off));
        ofs.write(reinterpret_cast<char *>(&bottom_off), sizeof(bottom_off));
        ofs.write(reinterpret_cast<char *>(&col_start), sizeof(col_start));
        ofs.write(reinterpret_cast<char *>(&col_end), sizeof(col_end));
        ofs.write(reinterpret_cast<char *>(&(circle.xc)), sizeof(circle.xc));
        ofs.write(reinterpret_cast<char *>(&(circle.yc)), sizeof(circle.yc));
        ofs.write(reinterpret_cast<char *>(&height), sizeof(height));
        ofs.write(reinterpret_cast<char *>(&width), sizeof(width));
        ofs.write(reinterpret_cast<char *>(&depth), sizeof(depth));
        for (auto first = coeffs.begin(), last = coeffs.end(); first != last;
             ++first) {
            ofs.write(reinterpret_cast<char *>(&(*first)), sizeof(*first));
        }
        ofs.close();
        return 0;
    }

    int process_fluoroscent(const std::string &path) noexcept {
        for (size_t i = 0; i < 8; ++i) {
            int ret = process_sample(path + std::to_string(i+1) + "/", 2, false);
            if (ret < 0) {
                return ret;
            }
        }
        return 0;
    }

    int process_sample(const std::string &path, int action, bool average) noexcept {
        LOGD("Path: %s", path.c_str());
        size_t pos = path.length() - 1;
        while (action > 0) {
            if (path[pos] == '/') {
                --pos;
            }
            while (path[pos] != '/') {
                --pos;
            }
            --action;
        }

        auto root = path.substr(0, pos + 1);
        LOGD("Root: %s", root.c_str());
        // Read broadband data file
        std::fstream ifs(root + BB_FOLDER + BB_DATA, std::ios::in | std::ios::binary);
        if (!ifs.good()) {
            println_e("Couldn't open broadband file to read");
            std::string s = root + BB_FOLDER + BB_DATA + " Couldn't open broadband file to read";
            LOGD("%s\n", s.c_str());
            return -1;
        }

        size_t top_off;
        size_t bottom_off;
        size_t col_start;
        size_t col_end;

        ifs.read(reinterpret_cast<char *>(&top_off), sizeof(top_off));
        ifs.read(reinterpret_cast<char *>(&bottom_off), sizeof(bottom_off));
        ifs.read(reinterpret_cast<char *>(&col_start), sizeof(col_start));
        ifs.read(reinterpret_cast<char *>(&col_end), sizeof(col_end));

        LOGD("%d", __LINE__);
        Circle circle;
        ifs.read(reinterpret_cast<char *>(&(circle.xc)), sizeof(circle.xc));
        ifs.read(reinterpret_cast<char *>(&(circle.yc)), sizeof(circle.yc));

        size_t height;
        size_t width;
        size_t depth;
        ifs.read(reinterpret_cast<char *>(&height), sizeof(height));
        ifs.read(reinterpret_cast<char *>(&width), sizeof(width));
        ifs.read(reinterpret_cast<char *>(&depth), sizeof(depth));
        LOGD("%d height: %d, width: %d, depth: %d", __LINE__, height, width, depth);

        matrix3<fp_t> coeffs(height, width, depth);
        for (auto first = coeffs.begin(), last = coeffs.end(); first != last;
             ++first) {
            ifs.read(reinterpret_cast<char *>(&(*first)), sizeof(*first));
        }

        ifs.close();
        LOGD("%d", __LINE__);

        // Read broadband result file
        std::fstream ifs_res(root + BB_FOLDER + RES, std::ios::in | std::ios::binary);
        if (!ifs_res.good()) {
            println_e("Couldn't open broadband result file to read");
            LOGD("Couldn't open broadband result file to read");
            LOGD("%d", __LINE__);
            return -1;
        }
        ifs_res.read(reinterpret_cast<char *>(&col_end), sizeof(col_end));
        std::vector<fp_t> bg(col_end);
        for (auto first = bg.begin(), last = bg.end(); first != last; ++first) {
            ifs_res.read(reinterpret_cast<char *>(&(*first)), sizeof(*first));
        }

        ifs_res.close();
        matrix3<uint8_t> sample_im;

        if (average) {
            sample_im = avg_folder<MAX_PICTURE>(
                    path, AVG_FILE_NAME, Margin{left_off, right_off, top_off, bottom_off});
        } else {
            sample_im = imread<uint8_t, 3>(path + "image.jpg");
        }

        if (is_empty(sample_im)) {
            LOGD("%s", ("no image: " + path).c_str());
            return -1;
        }

        LOGD("sample %d, width: %d, height: %d, depth: %d", __LINE__, sample_im.size(1), sample_im.size(0), sample_im.size(2));

        matrix2<fp_t> f(height, width);

        if (average) {
            get_normalized_data(sample_im.begin(), sample_im.end(), coeffs.begin(),
                                f.begin());
        } else {
            copy_and_remove_noise(sample_im.begin(), sample_im.end(), f.begin());
        }

        std::vector<fp_t> s(col_end);
        std::vector<fp_t> red(col_end);
        std::vector<fp_t> green(col_end);
        std::vector<fp_t> blue(col_end);
        for (size_t i = 0; i < col_start; ++i) {
            s[i] = 0;
            red[i] = 0;
            green[i] = 0;
            blue[i] = 0;
        }

        size_t min_width = width > sample_im.size(1) ? sample_im.size(1) : width;
        size_t min_height = height > sample_im.size(0) ? sample_im.size(0): height;
        LOGD("%d", __LINE__);
        size_t y_start = top_off;
        size_t y_end = top_off + min_height - 1;

        LOGD("%d, min_height: %d min_width: %d", __LINE__, min_height, min_width);

        for (size_t i = col_start; i < col_end; ++i) {
            fp_t R = sqrt((i - circle.xc) * (i - circle.xc) +
                          (0 - circle.yc) * (0 - circle.yc));
            fp_t s_val = 0;
            fp_t red_val = 0;
            fp_t green_val = 0;
            fp_t blue_val = 0;

            for (size_t y = y_start; y <= y_end; ++y) {

                fp_t x = sqrt(R * R - (y - circle.yc) * (y - circle.yc)) + circle.xc -
                         left_off;
                if (x >= 0 && x < min_width - 1) {

                    auto x1 = floor(x);
                    auto p = x - x1;
                    s_val += (1 - p) * f(y - top_off, x1) + p * f(y - top_off, x1 + 1);
                    red_val += (1 - p) * sample_im(y - top_off, x1, 0) +
                               p * sample_im(y - top_off, x1 + 1, 0);
                    green_val += (1 - p) * sample_im(y - top_off, x1, 1) +
                                 p * sample_im(y - top_off, x1 + 1, 1);
                    blue_val += (1 - p) * sample_im(y - top_off, x1, 2) +
                                p * sample_im(y - top_off, x1 + 1, 2);

                }
            }

            s[i] = s_val;
            red[i] = red_val;
            green[i] = green_val;
            blue[i] = blue_val;
        }


        // Write rgb spectrum to file
        std::fstream ofs_rgb_spec(path + RGB_SPEC, std::ios::out | std::ios::binary);
        if (!ofs_rgb_spec.good()) {
            println_e("Couldn't open broadband rgb spectrum file to write");
            return -1;
        }


        LOGD("%d", __LINE__);

        ofs_rgb_spec.write(reinterpret_cast<char *>(&col_end), sizeof(col_end));
        auto first_r = red.begin();
        auto last_r = red.end();
        auto first_g = green.begin();
        auto first_b = blue.begin();

        LOGD("%d", __LINE__);

        while (first_r != last_r) {
            ofs_rgb_spec.write(reinterpret_cast<char *>(&(*first_r)), sizeof(*first_r));
            ofs_rgb_spec.write(reinterpret_cast<char *>(&(*first_g)), sizeof(*first_g));
            ofs_rgb_spec.write(reinterpret_cast<char *>(&(*first_b)), sizeof(*first_b));
            ++first_r;
            ++first_g;
            ++first_b;
        }

        ofs_rgb_spec.close();

        for (size_t i = 0; i < bg.size(); ++i) {
            if (!almost_equal(bg[i], static_cast<fp_t>(0))) {
                s[i] = s[i] / bg[i];
                bg[i] = 1;
            } else {
                s[i] = 0;
                bg[i] = 0;
            }
        }

        LOGD("%d", __LINE__);

        println_i(std::fixed,
                  std::accumulate(s.begin(), s.end(), static_cast<fp_t>(0)));
        // LOGD("%f", std::accumulate(s.begin(), s.end(), static_cast<fp_t>(0)));
        println_i(std::fixed,
                  std::accumulate(bg.begin(), bg.end(), static_cast<fp_t>(0)));
        // LOGD("%f", std::accumulate(bg.begin(), bg.end(), static_cast<fp_t>(0)));

        LOGD("%d", __LINE__);
        // Write result to file
        LOGD("Writing to file %s", (path + RES).c_str());
        std::fstream ofs_res(path + RES, std::ios::out | std::ios::binary);
        if (!ofs_res.good()) {

            LOGD("%d", __LINE__);
            println_e("Couldn't open broadband result file to write");
            return -1;
        }

        ofs_res.write(reinterpret_cast<char *>(&col_end), sizeof(col_end));

        for (auto first = s.begin(), last = s.end(); first != last; ++first) {
            ofs_res.write(reinterpret_cast<char *>(&(*first)), sizeof(*first));
        }

        ofs_res.close();
        return 0;
    }
} // namespace elisa

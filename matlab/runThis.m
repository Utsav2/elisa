clear;
close all;

imdir='test';
if ~exist(fullfile(imdir, 'bb', 'avg.png'), 'file') 
    averagePhotos(fullfile(imdir, 'bb'), 4);
end
I = im2double(imread(fullfile(imdir, 'bb', 'avg.png')));
left_off = 1100;
right_off = 1100;
I = I(:, (left_off + 1):(size(I, 2) - right_off), :);

% Find the data region
gray = rgb2gray(I);
h = imhist(gray);
thr = 0.7 * sum(h(:));
s = 0;
for i=1:length(h)
    s = s + h(i);
    if (s > thr)
        break;
    end
end
thresh = (i - 1) / 255;
mask = gray >= thresh;
figure; imshow(mat2gray(mask));
%mask = imopen(mask, strel('disk', 9));
%figure; imshow(mat2gray(mask));
%figure; imshow(I);

[labels, num] = bwlabel(mask);

% find the largest component
maxIdx = 0;
nElems = 0;
for i=1:num
    [y, ~] = find(labels == i);
    if (length(y) > nElems) 
        nElems = length(y);
        maxIdx = i;
    end
end

[y, x] = find(labels == maxIdx);
yMax = max(y);
yMin = min(y);
xMax = max(x);
xMin = min(x);
top_off = yMin - 1;
bottom_off = size(I, 1) - yMax;

I1 = I((top_off + 1):(size(I, 1) - bottom_off), :, :); 

figure; imshow(I1);
J = sum(I1, 3);
vSum = sum(J, 2);
temp = sort(vSum);
thresh = temp(round(length(temp)*0.5));
figure;
plot(vSum);
hold on;
plot(1:length(vSum), thresh);
hold off;
index = find(vSum > thresh);
minIndex = min(index);
maxIndex = max(index);
[px, py] = findPoints1(I1(:, :, 1), [minIndex, maxIndex], 0.6);
[px, py] = filterPoints(px, py, 2);
%[c, R] = fitCircle_ransac(px, py);
[c, R] = circleFitMLS(px, py);
t = zeros(length(vSum), 1);
for i=1:length(vSum)
    t(i) = sqrt(R*R - (i-c(2))*(i-c(2))) + c(1);
end
mse = (1/length(px))*sum((t(py) - px).*(t(py) - px));
figure;
plot(py, px);
hold on;
plot(py, t(py), 'r');
figure; imshow(I1);
hold on;
plot(t, 1:length(t), 'r');

c(1) = c(1) + left_off;
c(2) = c(2) + top_off;
R_start = sqrt((left_off + 1 - c(1)) * (left_off + 1 - c(1)) + (top_off + 1 - c(2)) * (top_off + 1 - c(2)));
col_start = floor(sqrt(R_start * R_start - (1 - c(2)) * (1 - c(2))) + c(1)); 
R_end = sqrt((left_off + size(I1, 2) - c(1)) * (left_off + size(I1, 2) - c(1)) + (top_off + 1 - c(2)) * (top_off + 1 - c(2)));
col_end = ceil(sqrt(R_end * R_end - (1 - c(2)) * (1 - c(2))) + c(1)); 

if ~exist(fullfile(imdir, 'sample', 'avg.png'), 'file') 
    averagePhotos(fullfile(imdir, 'sample'), 4);
end
I2 = im2double(imread(fullfile(imdir, 'sample', 'avg.png')));
I2 = I2((top_off + 1):(size(I2, 1) - bottom_off),(left_off + 1):(size(I2, 2) - right_off), :);
figure; imshow(I2)

coeffs = zeros(size(I1));
thr = 20/255;
mask = I1(:, :, 1) >= thr | I1(:, :, 2) >= thr | I1(:, :, 3) >= thr;
for i=1:3
    sumsqr = I1(:, :, 1) .^ 2 + I1(:, :, 2) .^ 2 + I1(:, :, 3) .^ 2;
    coeffs(:, :, i) = (mask .* I1(:, :, i)) ./ (sumsqr);
end
coeffs(isnan(coeffs)) = 0;
mask1 = I2(:, :, 1) >= thr | I2(:, :, 2) >= thr | I2(:, :, 3) >= thr;
f = mask1 .* (I2(:, :, 1).*coeffs(:, :, 1) + I2(:, :, 2).*coeffs(:, :, 2) + I2(:, :, 3).*coeffs(:, :, 3));

s = zeros(col_end, 1);
bg = zeros(col_end, 1);
s(1:(col_start - 1)) = 0;
bg(1:(col_start - 1)) = 0;
for i=col_start:col_end
    R = sqrt((i - c(1)) * (i - c(1)) + (1 - c(2)) * (1 - c(2)));
    for y=(1 + top_off):(size(f, 1) + top_off)     
        x = sqrt(R*R - (y-c(2))*(y-c(2))) + c(1) - left_off;
        if (x >= 1 && x < size(f, 2))
            x1 = floor(x);
            p = x - x1;
            s(i) = s(i) + (1-p) * f(y - top_off, x1) + p * f(y - top_off, x1 + 1);
            bg(i) = bg(i) + (1-p) * mask(y - top_off, x1) + p * mask(y - top_off, x1 + 1);
        end
    end
end

for i=1:length(bg)
    if (bg(i) > eps) 
        s(i) = s(i) / bg(i);
        bg(i) = 1;
    else 
        s(i) = 0;
        bg(i) = 0;
    end
end

red_laser_peak = 1016.037828;
green_laser_peak = 1443.327951;
red_laser_nm = 656.26;
green_laser_nm = 532.1;

nmScale = (red_laser_nm - green_laser_nm) / (red_laser_peak - green_laser_peak);

nmOff = green_laser_nm - nmScale * green_laser_peak;

nm = zeros(size(s));
for i=1:length(nm)
    nm(i) = nmScale * i + nmOff;
end

figure;
plot(nm, s, nm, bg);
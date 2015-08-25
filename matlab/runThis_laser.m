clear;
close all;

I = im2double(imread('redlaser.jpg'));
left_off = 1100;
right_off = 1100;
I = I(:, (left_off + 1):(size(I, 2) - right_off), :);
figure;
imshow(I);

% Find the data region
gray = rgb2gray(I);
h = imhist(gray);
thr = 0.99 * sum(h(:));
s = 0;
for i=1:length(h)
    s = s + h(i);
    if (s > thr)
        break;
    end
end
thresh = (i - 1) / 255;
mask = gray >= thresh;
mask = imopen(mask, strel('disk', 5));
figure; imshow(mat2gray(mask));

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
left_off = left_off + xMin - 1;
right_off = right_off + size(I, 2) - xMax;

I1 = I((top_off + 1):(size(I, 1) - bottom_off), xMin:xMax, :); 

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
plot(t, 1:length(t), 'g');

c(1) = c(1) + left_off;
c(2) = c(2) + top_off;
R_start = sqrt((left_off + 1 - c(1)) * (left_off + 1 - c(1)) + (top_off + 1 - c(2)) * (top_off + 1 - c(2)));
col_start = floor(sqrt(R_start * R_start - (1 - c(2)) * (1 - c(2))) + c(1)); 
R_end = sqrt((left_off + size(I1, 2) - c(1)) * (left_off + size(I1, 2) - c(1)) + (top_off + 1 - c(2)) * (top_off + 1 - c(2)));
col_end = ceil(sqrt(R_end * R_end - (1 - c(2)) * (1 - c(2))) + c(1)); 

f = I1(:, :, 1);
s = zeros(col_end - col_start + 1, 1);
for i=col_start:col_end
    R = sqrt((i - c(1)) * (i - c(1)) + (1 - c(2)) * (1 - c(2)));
    for y=(1 + top_off):(size(f, 1) + top_off)     
        x = sqrt(R*R - (y-c(2))*(y-c(2))) + c(1) - left_off;
        if (x >= 1 && x < size(I1, 2))
            x1 = floor(x);
            p = x - x1;
            s(i - col_start + 1) = s(i - col_start + 1) + (1-p) * f(y - top_off, x1) + p * f(y - top_off, x1 + 1);
        end
    end
end
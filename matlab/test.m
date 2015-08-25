clear;
close all;
I = im2double(imread('IMG_20141113_174858.jpg'));
gray = rgb2gray(I);
h = imhist(gray);
thr = 0.85 * sum(h(:));
s = 0;
for i=1:length(h)
    s = s + h(i);
    if (s > thr)
        break;
    end
end
thresh = (i - 1) / 255;
mask = gray > thresh;
figure; imshow(mat2gray(mask));
mask = imclose(mask, strel('disk', 51));
figure; imshow(mat2gray(mask));
figure; imshow(I);

[labels, num] = bwlabel(mask);

% find the largest component
maxIdx = 0;
nElems = 0;
for i=1:num
    [y, x] = find(labels == i);
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

[px, py] = findPoints1(I(:, :, 1), [yMin + 400, yMin + 500], 0.5);
[px, py] = filterPoints(px, py, 2);
[c, R] = circleFitMLS(px, py);

 
% px = zeros(yMax - yMin + 1, 1);
% px(:) = xMin;
% py = (yMin:yMax)';
% 
% for i=yMin:yMax
%     row_data = gray(i, xMin:xMax);
%     thr = 0.6 * max(row_data);
%     j = 2;
%     while (j <= length(row_data)) 
%         if (row_data(j - 1) < thr && row_data(j) > thr) 
%             break;
%         end
%         j = j + 1;
%     end
%     if (j > length(row_data))
%         px(i - yMin + 1) = -1;
%     else 
%         px(i - yMin + 1) = px(i - yMin + 1) + j - 2;
%     end
% end
% 
% idxs = px ~= -1;
% px = px(idxs);
% py = py(idxs);
% 
% px = px(100:1000);
% py = py(100:1000);
% [ c, R ] = circleFitMLS( px, py );
% 
% pz = zeros(size(py));
% for i=1:length(px);
%     pz(i) = sqrt(R * R - (px(i) - c(1)) * (px(i) - c(1))) + yMin;
% end
% 

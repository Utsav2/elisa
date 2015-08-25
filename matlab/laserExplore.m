clear;
close all;
I = imread('greenlaser1.jpg');
[height, width, ~] = size(I);
if (height < width)
    I = imrotate(I, -90);
end
J = sum(I, 3);
vSum = sum(J, 1);
temp = sort(vSum);
thresh = temp(round(length(temp)*0.7));
figure;
plot(vSum);
% hold on;
% plot(1:length(vSum), thresh);
% hold off;
% index = find(vSum > thresh);
% minIndex = min(index);
% %minIndex = 1200;
% maxIndex = max(index);
index = find(vSum > thresh);
[end1, end2] = findLargestInterval(index, 100);
minIndex = index(end1);
maxIndex = index(end2);
[px, py] = findPoints(I, 2, [minIndex, maxIndex], 0.9, true);
%[px, py] = findPoints(I, 1, [minIndex, maxIndex], 0.8, true);
[px, py] = filterPoints(px, py, 2);
t = zeros(length(vSum), 1);
[c, R] = circleFitMLS(px, py);
t = zeros(length(vSum), 1);
for i=1:length(vSum)
    t(i) = sqrt(R*R - (i-c(1))*(i-c(1))) + c(2);
end
mse = (1/length(px))*sum((t(px) - py).*(t(px) - py));
figure;
plot(px, py);
hold on;
plot(px, t(px), 'g');
hold off;

figure;
imshow(I);
hold on;
[m, n, ~] = size(I);
s = zeros(m, 1);
for i=1:m
    r = sqrt((1 - c(1)).^2 + (i - c(2)).^2);
    yr = zeros(n, 1);
    for j=1:n
        yr(j) = sqrt(r.^2 - (j - c(1)).^2) + c(2);
    end
    if (i == 1000 || i == 1200) 
        plot(yr);
    end
    for j=1:n
        y = floor(yr(j));
        if (y >= 1 && y <= m) 
            s(i) = s(i) + double(I(y, j, 1));
        end
    end
end
figure;
plot(s, 'g');
% figure;
% imshow(I);
% % scatter(px, py);
% hold on;
% plot(t, 'y');
% rMin = R - 50;
% yrMin = zeros(2448, 1);
% for i=1:2448
%     yrMin(i) = sqrt(rMin*rMin - (i-c(1))*(i-c(1))) + c(2);
% end
% plot(yrMin);
% 
% rMax = R + 50;
% yrMax = zeros(2448, 1);
% for i=1:2448
%     yrMax(i) = sqrt(rMax*rMax - (i-c(1))*(i-c(1))) + c(2);
% end
% plot(yrMax);
% 
% r = rMin;
% s = zeros(rMax - rMin + 1, 1);
% i = 1;
% while (r <= rMax)
%     yr =zeros(2448, 1);
%     for j=1:2448
%         yr(j) = sqrt(r*r - (j-c(1))*(j-c(1))) + c(2);
%     end
%     for j=1:2448
%         s(i) = s(i) + double(I(floor(yr(j)), j, 2));
%     end
%     i = i + 1;
%     r = r + 1;
% end
% figure;
% plot(s, 'g');
% x = (38:48)';
% y = s(x);
% figure;
% plot(x, y);
% p = polyfit(x, y, 3);
% y1 = polyval(p, x);
% hold on;
% plot(x, y1);
% mse = (1/length(y))*sum((y - y1).*(y - y1));
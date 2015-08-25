clear;
close all;
I = imread('avg.jpg');
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
hold on;
plot(1:length(vSum), thresh);
hold off;
index = find(vSum > thresh);
minIndex = min(index);
%minIndex = 1200;
maxIndex = max(index);
[px, py] = findPoints1(rgb2gray(I), [minIndex, maxIndex], 0.8);
[px, py] = filterPoints(px, py, 2);
[c, R] = circleFitMLS(px, py);
t = zeros(length(vSum), 1);
for i=1:length(vSum)
    t(i) = sqrt(R*R - (i-c(1))*(i-c(1))) + c(2);
end
mse = (1/length(px))*sum((t(px) - py).*(t(px) - py));
figure;
plot(px, py);
hold on;
plot(px, t(px), 'r');
figure; imshow(I);
hold on;
plot(t, 'r');



[px1, py1] = findPoints(I, 2, [minIndex, maxIndex], 0.8, true);
[px1, py1] = filterPoints(px1, py1, 2);
R1 = calculateRadius(px1, py1, c);
t1 = zeros(length(vSum), 1);
for i=1:length(vSum)
    t1(i) = sqrt(R1*R1 - (i-c(1))*(i-c(1))) + c(2);
end
mse1 = (1/length(px1))*sum((t1(px1) - py1).*(t1(px1) - py1));
figure;
plot(px1, py1);
hold on;
plot(px1, t1(px1), 'g');

[c2, R2] = circleFitMLS(px1, py1);
t2 = zeros(length(vSum), 1);
for i=1:length(vSum)
    t2(i) = sqrt(R2*R2 - (i-c2(1))*(i-c2(1))) + c2(2);
end
mse2 = (1/length(px1))*sum((t2(px1) - py1).*(t2(px1) - py1));
figure;
plot(px1, py1);
hold on;
plot(px1, t1(px1), 'r');
plot(px1, t2(px1), 'g');



[px2, py2] = findPoints(I, 3, [minIndex, maxIndex], 0.9, true);
[px2, py2] = filterPoints(px2, py2, 2);
R3 = calculateRadius(px2, py2, c);
t3 = zeros(length(vSum), 1);
for i=1:length(vSum)
    t3(i) = sqrt(R3*R3 - (i-c(1))*(i-c(1))) + c(2);
end
mse3 = (1/length(px2))*sum((t3(px2) - py2).*(t3(px2) - py2));
figure;
plot(px2, py2);
hold on;
plot(px2, t3(px2), 'b');

[c4, R4] = circleFitMLS(px2, py2);
t4 = zeros(length(vSum), 1);
for i=1:length(vSum)
    t4(i) = sqrt(R4*R4 - (i-c4(1))*(i-c4(1))) + c4(2);
end
mse4 = (1/length(px2))*sum((t4(px2) - py2).*(t4(px2) - py2));
figure;
plot(px2, py2);
hold on;
plot(px2, t3(px2), 'r');
plot(px2, t4(px2), 'b');

figure;
imshow(I);
% scatter(px, py);
hold on;
plot(minIndex:maxIndex, t(minIndex:maxIndex), 'r');
plot(minIndex:maxIndex, t1(minIndex:maxIndex), 'r');
plot(minIndex:maxIndex, t2(minIndex:maxIndex), 'y');
plot(t2, 'y');
plot(minIndex:maxIndex, t3(minIndex:maxIndex), 'r');
plot(minIndex:maxIndex, t4(minIndex:maxIndex), 'y');


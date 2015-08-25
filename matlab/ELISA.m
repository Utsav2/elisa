clear;
close all;
I = imread('IMG_20141107_125925.jpg');
[c R] = circleFit([337 920 1636], [1438 1521 1488]);
y = zeros(2448, 1);
for i=1:2448
    y(i) = sqrt(R*R - (i-c(1))*(i-c(1))) + c(2);
end
imshow(I);
hold on;
plot(y);

rMin = R - 800;
yrMin = zeros(2448, 1);
for i=1:2448
    yrMin(i) = sqrt(rMin*rMin - (i-c(1))*(i-c(1))) + c(2);
end
plot(yrMin);

rMax = R + 800;
yrMax = zeros(2448, 1);
for i=1:2448
    yrMax(i) = sqrt(rMax*rMax - (i-c(1))*(i-c(1))) + c(2);
end
plot(yrMax);

r = rMin;
s = zeros(rMax - rMin + 1, 3);
i = 1;
while (r <= rMax)
    yr =zeros(2448, 1);
    for j=1:2448
        yr(j) = sqrt(r*r - (j-c(1))*(j-c(1))) + c(2);
    end
    for j=1:2448
        for k=1:3
            s(i, k) = s(i, k) + double(I(floor(yr(j)), j, k));
        end
        if (j == 1275)
        end
    end
    i = i + 1;
    r = r + 1;
end
figure;
plot(s(:, 1), 'r');
hold on;
plot(s(:, 2), 'g');
plot(s(:, 3), 'b');

I1 = imread('IMG_20141107_130119.jpg');
[c1 R1] = circleFit([357 1170 1909], [1438 1531 1441]);
y1 = zeros(2448, 1);
for i=1:2448
    y1(i) = sqrt(R1*R1 - (i-c1(1))*(i-c1(1))) + c1(2);
end
figure;
imshow(I1);
hold on;
plot(y1);
% z = zeros(2448, 1);
% for i=1:2448
%     z(i) = sqrt(R*R - (i-c(1))*(i-c(1))) + c(2);
% end
% hold on;
% plot(z, 'r');
rMin1 = R1 - 800;
yrMin1 = zeros(2448, 1);
for i=1:2448
    yrMin1(i) = sqrt(rMin1*rMin1 - (i-c1(1))*(i-c1(1))) + c1(2);
end
plot(yrMin1);

rMax1 = R1 + 800;
yrMax1 = zeros(2448, 1);
for i=1:2448
    yrMax1(i) = sqrt(rMax1*rMax1 - (i-c1(1))*(i-c1(1))) + c1(2);
end
plot(yrMax1);

r1 = rMin1;
s1 = zeros(rMax1 - rMin1 + 1, 3);
i = 1;
while (r1 <= rMax1)
    yr =zeros(2448, 1);
    for j=1:2448
        yr(j) = sqrt(r1*r1 - (j-c1(1))*(j-c1(1))) + c1(2);
    end
    for j=1:2448
        for k=1:3
            s1(i, k) = s1(i, k) + double(I(floor(yr(j)), j, k));
        end
        if (j == 1275)
        end
    end
    i = i + 1;
    r1 = r1 + 1;
end
figure;
plot(s1(:, 1), 'r');
hold on;
plot(s1(:, 2), 'g');
plot(s1(:, 3), 'b');

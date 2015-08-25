clear;
close all;
I = imread('IMG_20141107_125925.jpg');
px = [300; 900; 1500; 500; 800; 1100];
py = zeros(length(px), 1);
for i=1:length(px)
    col = I(:, px(i), :);
    figure;
    plot(col(:, 1), 'r');
    hold on;
    plot(col(:, 2), 'g');
    plot(col(:, 3), 'b');
    maxRed = max(max(col(:, 1)));
    threshRed = 0.2 * maxRed;
    plot(1:length(col(:, 1)), threshRed:threshRed);
    hold off;
    j = 1;
    while (col(j, 1) < threshRed)
        j = j + 1;
    end
    while (col(j, 1) >= threshRed)
        j = j + 1;
    end
    py(i) = j;
end

[c, R] = circleFit(px(1:3), py(1:3));
[~, n, ~] = size(I);
y = zeros(n, 1);
for i=1:2448
    y(i) = sqrt(R*R - (i-c(1))*(i-c(1))) + c(2);
end
figure;
imshow(I);
hold on;
plot(y, 'r');

[c1, R1] = circleFit(px(4:6), py(4:6));
z = zeros(n, 1);
for i=1:2448
    z(i) = sqrt(R1*R1 - (i-c1(1))*(i-c1(1))) + c1(2);
end
plot(z, 'c');
diff = abs(round(y(200:1700)) - round(z(200:1700)));
[c2, R2] = circleFitMLS(px, py);
t = zeros(n, 1);
for i=1:2448
    t(i) = sqrt(R2*R2 - (i-c2(1))*(i-c2(1))) + c2(2);
end
plot(t, 'b');



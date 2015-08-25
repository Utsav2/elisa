function [ c, R ] = fitCircle_ransac( px, py )
    K = 2000;
    n = length(px);
    maxCount = 0;
    px_in = [];
    py_in = [];
    for i=1:K
        rp = randperm(n);
        x = px(rp(1:3));
        y = py(rp(1:3));
        [c, R] = circleFitMLS(x, y);
        t = zeros(n, 1);
        for j=1:n
            t(j) = sqrt(R*R - (py(j)-c(2))*(py(j)-c(2))) + c(1);
        end
        mask = abs(px - t) < 0.05;
        count = sum(mask);
        if (count > maxCount)
            maxCount = count;
            px_in = px(mask);
            py_in = py(mask);
        end
    end
    [c, R] = circleFitMLS(px_in, py_in);
end


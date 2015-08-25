function [ px, py ] = findPoints1( I, range, thresh )
    py = (range(1):range(2))';
    px = zeros(length(py), 1);
    for i=1:length(px)
        col = I(py(i), :);
        [maxVal, iMax] = max(col(:));
        minVal = min(col(:));
        threshVal = double(thresh * (maxVal - minVal) + minVal);
        j = iMax;
        while (j < length(col) && col(j) >= threshVal) 
            j = j + 1;
        end
        x1 = j - 1;
        val1 = double(col(x1));
        x2 = j;
        val2 = double(col(x2));
        a = [x1 1; x2 1] \ [val1; val2];
        px(i) = (threshVal - a(2)) / a(1);
    end
end

